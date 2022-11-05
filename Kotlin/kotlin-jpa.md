# 코틀린과 JPA (Hibernate)

Kotlin과 Java는 비슷하면서도 다른 구석이 많다. 특히 JPA 엔티티를 정의할 때는 적절한 설계와 성능 확보를 위해 이런 차이점들을 잘 숙지해야 한다.

## Kotlin에서의 JPA Entity 정의

### @Id
Hibernate는 엔티티 생성 시, reflection으로 값을 넣어주기 때문에 setter를 사용하지 않는다. 또한 pk값은 변경될 여지가 없기 때문에 val로 선언하는 것이 좋다.

-   식별자에서 @GeneratedValue를 사용하게 되면 DB insert 전에는 id가 null이다. 따라서 nullable하게 정의해줘야 한다.
-   id 값은 비즈니스 로직에서 넣어줄 필요가 없기 때문에 생성자에서는 제거한다. 필드 선언 시, default 값은 null을 넣어준다.

```kotlin
@Entity
class Entity() {
    @Id
	@GeneratedValue
    val id: Long? = null
}
```

### @GeneratedValue

위에서 @Id 필드에 정의한 @GeneratedValue는 다음의 문제들이 있다.

- isNew (merge or persiste 결정)을 위해 id에 null값 혹은 default 값을 배정해야 한다.
	- non-null 선언하고, default 값 배정 -> 영속화 전 엔티티 식별이 안됨
	- nullable 선언하고, null 값 배정 -> DB PK는 항상 non-null, 식별자가 nullable인 것은 어색하다.
- AUTO / SEQUENCE / TABLE 중 어떤 모드를 사용하더라도 DB에 책임을 전가하고 부하를 유발한다.

모든 문제를 해결하기 위해서는 직접 UUID를 생성하고 Persistable을 구현하는 방법이 있다. (이에 대한 설명은 생략한다)

### Property

```kotlin
@Entity
class Entity() {
	@Id
	val id: Long? = null
	@Column
	val field1: String = ""
	@Column
	var field2: String = ""
}
```

**코틀린에서 nullable하지 않은 필드를 선언하는 경우 초기화를 반드시 해주어야 한다**. 그리고 이러한 제약 사항 때문에 Entity 필드 정의 시, default 값으로 무의미한 값을 넣을 수 밖에 없다.

불필요한 default 값은 가독성을 헤치고, 오류를 유발할 수 있다. primary constructor에서 생성자 매개변수를 지정하면 이런 문제를 해결할 수 있다.

```kotlin
class Entity(
	@Column
	val field1: String,
	@Column
	var field2: String
) {
  @Id
  val id: Long? = null
}
```

### Property 접근 제어

위와 같이 primary constructor를 사용하면 새로운 문제가 있다. var로 선언한 mutable한 필드에 대해 public setter가 생성되고, 이를 이용한 무분별한 외부 수정이 가능해진다는 것이다. 문제를 대응하기 위해서 필드 선언 및 접근 제어하는 setter를 생성하고, 일회성 (var도 val도 아닌) 생성자 인수를 함께 사용하는 방법이 있다.
- 이렇게 하면 필드에 불필요한 default 값을 선언하지 않아도 된다.
- setter를 private이 아닌 protected로 정의한 이유는 private setter에 대해 open property가 허용되지 않기 때문이다.

```kotlin
class Entity(
	@Column
	val field1: String,
	field2: String
) {
	@Id
	val id: Long? = null	

	@Column
	var field2: String = field2
		protected set
}
```

### data class 지양

JPA 영속성 컨텍스트는 Entity를 식별할 때 Id만 사용한다. 따라서 equals나 hashcode를 override할 때도 Id 필드만 있으면 된다.

- data class를 사용하는 경우 primary constructor에 명시된 필드를 모두 이용하기 때문에 문제가 된다.
- data class는 open이 불가능하기 때문에 Hibernate Lazy Loading등 이용이 불가능하다.

## 연관 관계

### 외부에 노출되는 Collection

```kotlin
@Entity
class User (
	@Column
	val field1: String,
	field2: String
) {
  @Id
	val id: Long? = null	

	@Column
	var field2: String = field2
		protected set

	@OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL], mappedBy = "user")
	val mutableBoards: MutableList<Board> = mutableListOf()
}

// 중략
val findUser = userRepository.findById(user.id).get()

val board3 = Board("게시판", "내용", BoardInformation(null, 1), user, setOf())
findUser.mutableBoards.add(board3)
// 
```

JPA에서 연관 관계의 요소 변경은 DB 변경을 유발한다. Property를 불변(val)으로 선언하더라도 언제든지 외부에서 연관관계를 변경할 수 있다.

문제를 해결하기 위해서는 어떻게 해야할까?

일단, 연관 관계에 수정이 발생하는 것은 당연하기 때문에 mutableList를 유지한다. 대신 외부에 노출되는 collection을 immutableList로 사용할 수 있다.

```kotlin
@Entity
class User (
	@Column
	val field1: Long,
	field2: Long
) {
	@Id
	val id: Long? = null	

	@Column
	var field2: Long = field2
		protected set

	@OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL], mappedBy = "user")
  val mutableBoards: MutableList<Board> = mutableListOf()
	val boards: List<Board> get() = mutableBoards.toList()
}
```

## Kotlin 지원 plug-in

### all-open plugin

Hibernate는 지연로딩 시 Entity를 프록시 객체로 조회한다. final 클래스의 경우 상속이 제한되기 때문에 프록시를 생성할 수 없고, 이는 지연로딩을 불가능하게 한다.

**Technically Hibernate can persist final classes or classes with final persistent state accessor (getter/setter) methods. However, it is generally not a good idea as doing so will stop Hibernate from being able to generate proxies for lazy-loading the entity.**

Kotlin의 모든 클래스, 프로퍼티, 함수가 기본적으로 final이기 때문에 @Entity로 사용하기 위해서는 반드시 open 키워드를 명시해야 한다. fetch Type만 적용하고 해당 @Entity에 open을 까먹는다면? 서비스 성능에 심각한 영향을 미칠 수 있다.

이런 문제를 방지하기 위해 나온 것이 바로 `all-open plugin`이다. 문서의 설명을 읽어보면 기본적으로 final로 작동하는 Kotlin의 불편함을 해결하기 위해 나왔다고 한다. @Component, @Async, @Transactional, @Cacheable 등의 기본적인 Annotation 및 @RestController 등의 Meta annotation 이 명시된 클래스를 자동적으로 open 처리 해준다.

Spring AOP는 Bean proxy를 기반으로 동작하기 때문에 스프링 사용을 위해서는 거의 필수라고 볼 수 있다. 그리고 이런 이유 때문인지 기본적으로 spring initionalizer 등을 이용해 kotlin 기반의 Spring 프로젝트를 생성하면 해당 플러그인이 포함된다.

다만, @Entity는 해당 플러그인의 자동 open 적용 대상이 아니기 때문에 위의 Lazy loading 문제 해결을 위해서는 별도 명시를 해줘야 한다.

```groovy
// Gradle
allOpen {
    annotation("javax.persistence.Entity")
}
```

```xml
<!-- Maven-->
<configuration>
    <pluginOptions>
        <option>all-open:annotation=javax.persistence.Entity</option>
    </pluginOptions>
</configuration>
```

### no-arg plugin

Hibernate는 reflection을 통해 entity 인스턴스화 한다. 기본 생성자가 없다면 reflection을 통한 생성자 인수 획득이 불가능하다. 따라서 JPA의 스펙은 protected / public 기본 생성자를 강제한다.

kotlin에서는 no-args 플러그인을 통해 @Entity, @Enbeddable 등 어노테이션이 붙은 클래스에 기본 생성자를 추가해준다.

---
참고

- https://docs.jboss.org/hibernate/orm/5.2/userguide/html_single/Hibernate_User_Guide.html#entity-pojo
- https://spoqa.github.io/2022/08/16/kotlin-jpa-entity.html
- https://techblog.woowahan.com/2675/
- https://velog.io/@eastperson/Kotlin-Spring-%EC%8B%9C%EC%9E%91%ED%95%98%EA%B8%B03-Entity#%EC%8B%9D%EB%B3%84%EC%9E%90



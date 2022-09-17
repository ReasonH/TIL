# 번외, 기타 매핑

## 상속관계 매핑

객체에는 상속이 있지만, 관계형 DB에는 상속관계가 없으며 그 대신 유사한 모델링 기법으로 슈퍼타입-서브타입 모델링이 있다. 상속관계 매핑이란 이런 객체의 구조와 DB의 슈퍼타입-서브타입 관계를 매핑한다.

슈퍼-서브 논리모델을 물리 모델로 구현하는 방법

1. 각각 테이블로 변환 → 조인 전략
2. 통합 테이블로 변환 → 단일 테이블
3. 서브타입 테이블로 변환 → 구현 클래스마다 테이블 생성

JPA의 기본 전략은 싱글테이블 전략(**모든 상속 객체의 컬럼들이 합쳐져서 단일 테이블로 생성**)

이는 InheritanceType 통해 변경 가능하다.

### JOINED 전략

```java
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn
public abstract class Item {

    @Id
    @GeneratedValue
    private Long id;

    private String name;
}

@Entity
@DiscriminatorValue("BK")
public class Book extends Item {

		private String author;
}
```

- 위와 같이 JOINED로 사용하는 경우 각각을 테이블로 변환해주며 객체 생성 및 조회 시 관련 처리들을 대신 수행
- @DiscriminatorColumn으로 슈퍼타입에 필드 생성 가능
- @DiscriminatorValue로 타입 값 명시 가능 (기본값은 테이블명)

### SINGLE_TABLE 전략

```java
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class Item {

    @Id
    @GeneratedValue
    private Long id;

    private String name;
}

@Entity
@DiscriminatorValue("BK")
public class Book extends Item {

		private String author;
}
```

- 성능이 잘나옴 (JOIN 불필요)
- 쿼리가 간편해짐, 추가적으로 하위클래스 조회해도 JPA가 알아서 DTYPE 등을 필터링해서 조회
- @DiscriminatorColumn이 없어도 DTYPE이 필수로 생성된다. 단일 테이블에서는 해당 컬럼이 없으면 구분이 안되기 때문이다.

### TABLE_PER_CLASS

```java
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class Item {

    @Id
    @GeneratedValue
    private Long id;

    private String name;
}

@Entity
@DiscriminatorValue("BK")
public class Book extends Item {

		private String author;
}
```

- 슈퍼타입 테이블은 생성하지 않으며 서브타입 테이블만 생성
- 조회 시 성능 문제가 발생

```java
Book book = new Book();
book.setAuthor("작가");
book.setName("책");

em.persist(book);
em.flush();
em.clear();

Item item = em.find(Item.class, book.getId());
```

이렇게 슈퍼타입 객체로 테이블을 조회하는 경우 모든 서브타입 테이블을 전부 조회해서 UNION 연산을 수행한다.

### 조인 전략

가장 정석적인 설계라고 볼 수 있음

장점

- 테이블 정규화
- 외래키 참조 무결성 제약조건 활용 가능 (타 테이블에서 참조 시 슈퍼타입 객체만 참조하면 됨)
- 저장공간 효율화

단점

- 조회 시 JOIN 다수 사용, 성능 저하 및 복잡성 증가
- 데이터 저장 시 INSERT 2회 호출
- ~~사실 위 두개는 성능상으로는 크게 이슈가 되지 않는다.~~

### 단일테이블 전략

장점

- 조회 성능
- 단순한 쿼리

단점

- 자식 엔티티가 매핑한 컬럼은 모두 nullable이 된다.
- 테이블이 커질 수 있으며 상황에 따라 성능이 느려질 수도 있음 (임계점을 넘는경우)

### 구현클래스마다

- 그냥 사용하지 말자

## @MappedSuperclass

- 주로 엔티티에서 사용되는 공통 필드들을 추출하기 위해 사용

```java
@MappedSuperclass
public class BaseEntity {

    private String createdBy;
    private LocalDateTime createdDate;
}
```

- 테이블과 매핑되지 않음
- 이 클래스(부모클래스)를 상속받는 자식클래스에 매핑 정보만 제공
- 추상 클래스 권장

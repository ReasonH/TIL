# 코틀린에서의 OOP

## 클래스

### 클래스와 프로퍼티

코틀린에서는 생성자를 만들어줄 때 동시에 프로퍼티를 선언할 수 있다.

```kotlin
class Person(
    val name: String,
    var age: Int
)
```

프로퍼티 선언 시, **getter와 setter가 자동**으로 만들어지며 이를 호출할 때는 `.`을 사용한다.

```kotlin
person.name = "Lee"
println(person.name)
```

### 생성자와 init

생성자에서 프로퍼티 검증은 어떻게 하지? → init 사용

```kotlin
class Person(
    val name: String,
    var age: Int
) {
    init {
        if (age <= 0) {
            throw IllegalArgumentException("")
        }
    }
}
```

다른 생성자를 만드는 방법

```kotlin
class Person(
    val name: String,
    var age: Int
) {
    init {
        if (age <= 0) {
            throw IllegalArgumentException("")
        }
    }

    constructor(name: String) : this(name, 1)
}
```

- 클래스명 옆에 바로 정의하는 생성자를 주생성자라 한다. (constructor 생략 가능)
- class block 내부의 생성자를 부생성자라 한다. 부생성자는 최종적으로 주생성자를 호출해야 한다.
- 주생성자가 없는 경우 기본 생성자를 자동으로 만들어준다.
- 부생성자는 내부에 body를 가질 수 있다.

**그러나 코틀린은 부생성자 정의보다는 default parameter를 권장한다.**

```kotlin
class Person(
    val name: String = "Lee",
    var age: Int = 1
)
```

### 커스텀 getter, setter

```kotlin
class Person(
    val name: String = "Lee",
    var age: Int = 1
) {
		// (1)
    fun isAdult(): Boolean {
        return this.age >= 20
    }
		// (2)
    val isAdult: Boolean
        get() = this.age >= 20
}
```

getter를 커스텀하게 정의할 때도 두 가지 방식이 있다.

1. 메서드 정의 → Java와 유사한 사용방법
2. custom getter → Kotlin 고유 방식, 좀 더 간결하고 마치 property와 같이 접근, 사용 가능하다.

그렇다면 둘을 어떻게 사용해야 할까? (사실 명확한 기준은 없다.)

다만, 추천하는 방식은 **속성을 나타내는 경우 custom getter를 사용하는 것이다.**

### backing field

#### custom getter / setter 재정의

custom getter / setter로 기존 프로퍼티를 재정의 할 수도 있다.

```kotlin
class Person(
    name: String = "Lee",
    var age: Int = 1
) {

    var name = name
        get() = field.uppercase()
        set(value) {
            field = value.uppercase
        }

    val isAdult: Boolean
        get() = this.age >= 20
}
```

위는 name 필드의 getter / setter 를 호출했을 때, 항상 대문자를 반환하도록 수정한 코드이다.

- getter / setter 자동 생성을 막기 위해 주생성자에서 `val` 키워드를 제거했다.
- setter 재정의를 확인하기 위해 body에서 필드를 `var`로 정의했다.

**field 키워드는 무엇인가?**

만약 아래와 같이 정의하는 경우

```kotlin
var name: String = name
	get() = name.uppercase()
```

외부에서 `person.name()` 호출 시, `get()`이 호출된다. 그러나 `get()`에서 name에 접근할 때는 다시 `get()`을 호출한다. 즉 무한 루프에 들어가는 것이다. 코틀린에서는 이를 방지하기 위해 `field`라는 키워드를 제공하고 있다.

#### Tip1.

물론 실제로는 아래와 같이 추가 프로퍼티를 제공하는 방법을 더 많이 사용한다.

```kotlin
class Person(
    val name: String = "Lee",
    var age: Int = 1
) {

		val upperCaseName: String
				get() = this.name.uppercase()

    val isAdult: Boolean
        get() = this.age >= 20
}
```

#### Tip2.

사실 setter 자체를 지양하기 때문에 custom setter 또한 자주 사용할 일이 없다.

## 상속

### 추상 클래스

```kotlin
abstract class Animal(
    protected val species: String,
    protected open val legCount: Int
) {

    abstract fun move()
}
```

위를 상속한 클래스를 만들어보면 아래와 같다.

```kotlin
class Penguin(
    species: String
) : Animal(species, 2) {

    private val wingCount: Int = 2

    override fun move() {
        println("움직임")
    }

    override val legCount: Int
        get() = super.legCount + this.wingCount
}
```

- 프로퍼티를 상속받아 재정의 하기 위해서는 open 키워드를 사용해야 한다.
    - 이를 통해 추상 클래스에서 만들어진 legCount의 getter를 override할 수 있다.
- 상위 클래스 접근은 자바와 동일하게 `super` 키워드를 사용한다.

### 인터페이스

```kotlin
interface Flyable {

    fun act() {
        println("비행")
    }
}

interface Swimable {

    fun act() {
        println("수영")
    }
}
```

Kotlin에서는 default 키워드 없이 인터페이스에서 메서드 구현이 가능하다.

```kotlin
class Penguin(
    species: String
) : Animal(species, 2), Flyable, Swimable {

    private val wingCount: Int = 2
    
    override fun act() {
        super<Flyable>.act()
        super<Swimable>.act()
    }
	// 생략
}
```

중복되는 인터페이스를 특정할 때는 super<타입>.함수를 사용한다.

### 상속 시 주의점

상위 클래스 설계 시, 생성자 또는 초기화 블록에 사용되는 프로퍼티에는 open을 피해야 한다.

### 상속 키워드

- final: 기본 상태, override 불가능하게 한다.
- open: override 가능하게 한다.
- abstract: 반드시 override 해야 한다.
- override: 상위 타입을 override 하고 있음을 나타낸다. (override 시 필수 사용해야함)
    - 추상 멤버가 아니라면 기본적으로 override 불가능하기 때문에 open을 사용해야 한다.

## 접근 제어

### 가시성 제어

1. public: 모든 곳에서 접근 가능
2. protected: 선언된 클래스 or 하위 클래스에서 접근 가능
3. internal: 같은 모듈에서만 접근 가능 (gradle / maven등의 모듈 단위를 생각하면 된다)
4. private: 선언된 클래스 내에서만 접근 가능

### 파일 접근 제어

1. public: 어디서든 접근 가능
2. protected: 파일(최상단)에는 사용 불가능
3. internal: 같은 모듈에서만 접근 가능
4. private: 같은 파일 내에서만 접근 가능

### 구성요소 접근 제어

#### 생성자

가시성 제어와 동일하다. 다만 생성자에서 가시성 제어를 사용하기 위해서는 constructor 키워드를 반드시 사용해야 한다.

#### Tip

유틸성 클래스를 만들 때, 단순 파일에 함수를 정의하는 방식이 편리하다.

```kotlin
// StringUtil.kt

fun isDirectoryPath(path: String) {
    return path.endsWith("/")
}
```

이렇게 정의하는 경우 마치 정적 메서드처럼 사용할 수 있다.

#### 프로퍼티

프로퍼티 정의 시 가시성 제어를 사용하면 getter / setter 모두에 적용된다.

만약 setter에만 private을 적용하고 싶다면?

```kotlin
class Car(
    internal val name: String,
    private var owner: String,
    _price: Int
) {
    var price = _price
        private set
}
```

### Java와 사용 시 주의점

- Kotlin의 internal은 Java에서 public이 된다. 따라서 Java 코드에서 접근할 수 있다.
- 비슷하게, Java는 같은 패키지에 존재하는 Kotlin protected 클래스에 접근이 가능하다.

## object 키워드

### static 함수와 변수

코틀린에는 static 키워드가 없다. 대신 companion object를 사용한다.

클래스라는 설계 템플릿과 세트로 붙어있는 유일한 object라고 생각하면 된다.

```kotlin
class Person private constructor(
	var name: String,
	var age: Int
) {

   companion object Factory : Log {
      private const val MIN_AGE = 1
      fun newBaby(name: String): Person {
         return Person(name, MIN_AGE)
      }

      override fun log() {
         println("log")
      }
   }
}
```

특징

- companion 또한 이름을 지을 수 있으며 인터페이스 구현도 가능하다.
- companion object 내의 상수는 const 키워드를 사용 가능하다.
    - 이 키워드가 있으면 값이 컴파일 타임에 할당된다. (없다면 런타임에 할당)
    - 즉 진짜 상수를 나타낸다.
    - 기본 타입과 String에만 사용 가능하다.

### 싱글톤

```kotlin
object Singleton {
   var a: Int = 0
}
```

단순히 위와 같이 사용하면 된다.

### 익명 클래스

```kotlin
fun main() {
   moveSomething(object : Moveable {
      override fun move() {
         // something
      }

      override fun fly() {
         // something
      }
   })
}
```

익명클래스 사용 시 new 를 통해 인터페이스를 구현하는 대신 `object`를 사용한다.

## 중첩 클래스

```kotlin
class House (
   var address: String,
   var livingRoom: LivingRoom = LivingRoom(10.0)
) {
   class LivingRoom(
      private var area: Double
   )
}
```

단순히 내부에 class를 정의하면 내부 static class를 정의한 것처럼 동작한다. (LivingRoom은 외부 참조 X)

## 다양한 클래스를 다루는 방법

### Data class

```kotlin
data class PersonDto(
   var name: String,
   var age: Int
)
```

equals(), hashCode(), toString() 등을 알아서 정의해준다.

### Enum

```kotlin
 enum class Country(
   private val code: String
) {
   KOREA("K0"),
   AMERICA("US"),
   ;
}
```

Java와 거의 차이 없다.

#### when과의 결합

```kotlin
fun handleCountry(country: Country) {
   when (country) {
      Country.KOREA -> TODO()
      Country.AMERICA -> TODO()
   }
}

enum class Country(
   private val code: String
) {
   KOREA("K0"),
   AMERICA("US"),
   ;
}
```

- 분기처리 가독성이 늘어난다.
- else 처리에 대한 고민이 필요없다.
- IDE 단에서 Enum에 변화가 있다면 알 수 있다.

### Sealed class

상속이 가능하도록 하되, 외부에서 마음대로 사용하지 못하도록 만들기 위해 사용한다.

- 컴파일 타임에 하위 클래스 타입을 모두 기억한다.
- 런타임에서 클래스 타입 추가가 불가능
- 하위 클래스는 같은 패키지에 있어야 한다.
- 추상화가 필요한 Entity / DTO에서 사용하면 좋다

Abstract class와는 무슨 차이지?

물론 유사하다. 그러나 그 가짓수를 확실히 고정하고 싶을 때 사용한다.

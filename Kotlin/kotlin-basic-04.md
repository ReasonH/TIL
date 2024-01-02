# 함수형 프로그래밍

## 배열과 컬렉션

### 배열

보고만 지나가자

### 컬렉션

#### List

코틀린에서는 collection을 만들 때 **가변, 불변 여부를 설정**해야 한다.

```kotlin
// 불변 리스트
fun main() {
    val mumbers = listOf(100, 200)
    val emptyList = emptyList<Integer>()
}

// 가변 리스트
fun main() {
    val mumbers = mutableListOf(100, 200)
}
```

코틀린에서는 collection 인자를 가져올 때 배열과 같이 접근이 가능하다.

```kotlin
println(mumbers[0])

for (number in numbers) {
    println(number)
}

for ((intdex, number) in numbers.withIndes) {
    println("$index $number")
}
```

#### Set

list와 유사하다.

#### Map

```kotlin
fun main() {
    val oldMap = mutableMapOf<Int, String>()
		
		// 가변 map, put과 동일한 기능이다
    oldMap[1] = "MONDAY"
    oldMap[2] = "TUESDAY"

		// 불변 map
    mapOf(1 to "MONDAY", 2 to "TUESDAY")
}
```

### Java와 함께 사용하기

#### Kotlin 코드를 Java에서 사용

Java는 읽기 전용 컬렉션과 변경 가능 컬렉션을 구분하지 않는다.

ex) Java에서 kotlin 읽기 전용 컬렉션을 호출하고 원소 추가

Java는 nullable 타입과 non-nullable 타입을 구분하지 않는다.

ex) Java에서 Kotlin non-nullable 컬렉션을 호출하고 null 추가

따라서 사용에 주의가 필요하다. (non-nullable 컬렉션의 경우 Collections.unmodifiableXXX 등 활용)

#### Java 코드를 Kotlin에서 사용

맥락을 파악하고 별도의 Wrapping을 작성하자

## 다양한 함수

### 확장함수

```kotlin
fun String.lastChar(): Char {
    return this[this.length - 1]
}
```

코틀린에서는 클래스 밖에서 클래스 멤버함수처럼 동작하는 함수를 만들 수 있다.

위 함수에서 부르고 있는 객체 `this`를 코틀린에서는 **수신객체**라 한다.

특징

1. 확장 함수는 클래스 내의 private / protected에 접근할 수 없다. this에 의한 수신 객체의 캡슐화가 깨지는 것을 방지하기 위한 것이다.
2. 확장함수와 멤버함수의 시그니쳐가 동일하면 멤버함수가 호출된다.
3. 확장함수가 오버라이드 된다면? 정적인 현재 타입에 의해 결정된다.
    - ex) val srt: Train = Srt() 정의 후, 확장함수 사용 시 Train 확장함수가 호출됨

### infix 함수 (중위함수)

변수.함수이름(argument) 대신 변수 함수이름 argument 형식 사용 가능

```kotlin
infix fun Int.add(other: Int): Int {
    return this + other
}

// 이와 같이 사용 가능하다.
3 add 4
```

- infix는 멤버 함수에도 사용 가능하다.

### inline 함수

```kotlin
fun main() {
    3.add(4)
}

inline fun Int.add(other: Int): Int {
    return this + other
}
```

컴파일 시점에 함수 내용 자체가 호출부로 들어간다.

→ 함수 call chain overhead를 줄이기 위해 사용한다.

### 지역함수

함수 내에 함수를 선언할 수 있다.

컨벤션 상 권장되지는 않음.

## 람다

### 코틀린에서의 람다

코틀린에서는 함수가 그 자체로 값이 될 수 있다. 변수로 넣을 수도 있으며 파라미터로 넘길 수도 있다.

람다를 만드는 방법은 다음과 같이 두 가지가 있다.

```kotlin
val isApple = fun(fruit: Fruit): Boolean {
    return fruit.name == "사과 "
}

val isApple2 = { fruit: Fruit -> fruit.name == "사과" }
```

다음과 같이 사용 가능하다.

```kotlin
private fun filterFruits(
    fruits: List<Fruit>, filter: (Fruit) -> Boolean
): List<Fruit> {
    val results = mutableListOf<Fruit>()
    for (fruit in fruits) {
        if (filter(fruit)) {
            results.add(fruit)
        }
    }
    return results
}
```

함수를 파라미터로 받을 때는 **변수명: 입력 타입 → 반환 타입** 형태로 전달 가능하다.

호출 시에는 다음과 같이 사용 가능하다.

```kotlin
filterFruits(fruits, isApple2)
// 또는 
filterFruits(fruits, { fruit: Fruit -> fruit.name == "사과" })
```

만약 함수형 파라미터가 메서드의 마지막 파라미터라면 다음과 같이 메서드를 소괄호 밖으로 뺄 수 있다.

```kotlin
filterFruits(fruits) { fruit: Fruit -> fruit.name == "사과" }
```

또한, 함수의 타입을 추론 가능하기 때문에 타입을 생략할 수 있다.

```kotlin
filterFruits(fruits) { fruit -> fruit.name == "사과" }
```

람다의 파라미터가 한 개(여기서는 fruit) 인 경우 이를 다음과 같이 it으로 축약할 수도 있다.

```kotlin
filterFruits(fruits) { it.name == "사과" }
```

### Clousure

코틀린에서는 람다가 시작하는 지점에 참조하고 있는 변수들을 모두 포획하여 그 정보를 가지고 있다. → 람다 내에서 Effectively final 제한 등이 없다. 가변 변수도 사용 가능하다.
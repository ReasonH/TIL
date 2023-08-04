# 범위 지정 함수

특정 객체에 대한 작업을 블록 안에 넣어서 실행할 수 있도록 하는 함수이다. 블록은 특정 객체에 대한 작업 범위가 된다. 코틀린에서는 5종의 범위 지정함수를 지원한다.

### 수신 객체 지정 함수

범위 지정 함수와 동의어로 사용된다. 람다 안에서는 수신 객체를 명시하지 않거나, it 호출 만으로도 수신객체 메서드를 호출할 수 있도록 하기 때문이다. 이것이 가능한 이유는 수신 객체를 람다 입력 파라미터 또는 수신 객체로 사용하였기 때문이다.

```kotlin
data class Person(
	var name: String = "",
	var age: Int = 0,
	var temperature: Float = 36.5f
) {
	fun isSick(): Boolean = temperature > 37.5f
}
```

### apply

```kotlin
public inline fun <T> T.apply(block: T.() -> Unit): T
```

apply는 수신객체 내부 프로퍼티를 변경하고, 수신객체 자체를 반환하기 위해 사용한다.

일반적으로 객체 생성 시 다양한 프로퍼티를 설정해야 하는 경우 자주 사용된다.

```kotlin
val person = Person().apply {
	name = "Lee"
	age = 29
	temperature = 36.3f
}
```

### run

```kotlin
public inline fun <T, R> T.run(block: T.() -> R): R
```

run은 apply와 똑같이 동작하지만, 수신객체가 아닌 블럭의 마지막 라인을 return한다.

수신 객체에 대해 특정 작업을 한 이후 결과값을 반환해야 하는 경우 자주 사용된다.

```kotlin
val isPersonSick = person.run {
	temperature = 37.3f
	isSick() // 반환
}
```

### with

```kotlin
public inline fun <T,R> with(receiver: T, block: T.() -> R): R
```

run과 완전히 동일하지만, with은 수신객체를 파라미터로 받아 사용한다. 보통은 그냥 run을 사용한다.

```kotlin
val isPersonSick = with(person) {
	temperature = 37.3f
	isSick() // 반환
}
```

### let

```kotlin
public inline fun <T, R> T.let(block: (T) -> R): R
```

run, with과 유사하게 수신객체 작업 이후 마지막 줄을 반환한다. 블럭에서 수신객체 접근 시, it을 사용해야 한다.

일반적으로 nullable한 객체 + elvis와 함께 사용해서 블럭 내 구문이 수신객체가 null이 아닌 경우에만 실행되도록 한다.

### also

```kotlin
public inline fun <T> T.also(block: (T) -> Unit): T
```

apply와 유사하게 수신객체 자신을 반환한다. also는 프로퍼티 세팅 뿐 아니라 객체에 추가적인 작업을 한 후 반환할 때 사용한다.

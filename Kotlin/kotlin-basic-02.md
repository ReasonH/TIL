# 코드 제어

## 제어문

### if

자바의 if는 Statement지만 코틀린에서의 if는 Expression 취급된다.

- 자바에서는 if문을 Expression처럼 사용하기 위해 3항 연산자를 사용했다.
- 코틀린에서는 if문을 바로 return 하거나 변수에 대입 가능하다.

범위에 대한 논리 연산이 필요할 때

`if (score in 0..100)`과 같이 사용할 수 있다.

### switch 와 when

코틀린에서는 switch 대신 when을 사용한다. when 또한 하나의 expression이기 때문에 return 값 등에 바로 사용 가능하다.

```kotlin
fun getGrade(score : Int): String {
        return when(score / 10) {
        9 -> "A"
        8 -> "B"
        7 -> "C"
        else -> "D"
    }
}
```

조건부에는 어떤 expression이라도 들어갈 수 있다.

- `is String -> XXX`
- `in 0..100 -> XXX`

조건부에서는 여러 조건을 동시에 검사할 수 있다.

- `1, 0, -1 → “많이 본 숫자”`

## 반복문

### for each문

```java
for (long number : numbers) {
    // xxx
}
```

코틀린에서는 다음과 같이 사용

```kotlin
for (number in numbers) {
    // ...
}
```

### 전통적인 for 문

```kotlin
// 증가하는 경우
for (i in 1..3) {
    // ...
}

// 감소하는 경우
for (i in 3 downto 1) {
    // ...
}

// 2씩 증가하는 경우
for (i in 1..5 step 2) {
    // ...
}
```

### Progression과 Range

위의 for 문은 사실 등차 수열을 만드는 코드이다.

- downTo와 step 또한 함수이다. (중위함수)

## 예외 처리

### try catch finally 구문

문법적으로는 동일하다.

특이한 차이점은 try-catch 구문을 expression처럼 사용할 수 있다는 점이다.

```kotlin
fun parseToInt(str: String): Int? {
    return try {
        str.toInt()
    } catch (e: NumberFormatException) {
        null
    }
}
```

### Checked Exception과 Unchecked Exception

코틀린에서는 모든 Exception이 Unchecked Exception이다.

### try with resources

코틀린에서는 삭제되었다. 대신 `use`라는 인라인 확장함수를 사용한다.

## 함수를 다루는 방법

### 함수 선언 문법

```java
public int max(int a, int b) {
    if (a > b) {
        return a;
    }
    return b;
}
```

```kotlin
fun max(a: Int, b: Int) = if (a > b) a else b
```

- public 접근 지시자는 생략 가능하다.
- body가, 하나의 값으로 간주되는 경우 block을 없앨 수 있다.
- block이 없다면 결과 타입 추론을 사용 가능하다.
- if와 else의 중괄호를 없애고 한 줄로 만들 수 있다.

### default parameter

오버로딩을 대체하는 느낌으로 사용 가능하다.

자주 쓰이는 기본 파라미터를 정의한다.

### named parameter

마치 builder처럼 사용할 수 있게 된다. 파라미터를 직접 지정함으로써 가독성이 좋아진다. 또한, default parameter가 있는 경우 해당 값을 사용하고 원하는 parameter만 지정해서 넣어줄 수 있다. (Java 함수 사용 시 사용 불가능)

### 가변 인자

varargs로 정의하는 것을 제외하면 기본적인 사용 방식은 동일하다.

단, 만약 배열을 가변 인자 파라미터로 전달할 시에는 spread 연산자(\*)를 사용해야 한다.

```java
fun main() {
    val array = arrayOf("A", "B", "C")
    printAll(*array)
}

fun printAll(varargs strings: String) {
    // ...
}
```
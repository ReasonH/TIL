# 변수, 타입, 연산자

## Null을 다루는 방법

### null 체크

- String과 String?는 아예 다른 타입이다.
- 코틀린은 문맥상 null체크를 컴파일러가 인지한다.

```kotlin
fun startwith(str: String?) {
    return str.startsWith("A") // Error
}

fun startwith(str: String?) {
    if (str == null) {
        return false
    }

    return str.startsWith("A")
}
```

### Safe Call

```kotlin
val str: String? = "ABC"
str.length // 불가능
str?.length // 가능
```

safe call을 이용하면 변수가 null이 아닌 경우 후속 메서드를 실행한다. (null인 경우 그냥 null)

### Elvis 연산자

```kotlin
val str: String? = "ABC"
str?.length ?: 0
```

Elvis 연산자 `?:`를 이용하면 null인 경우 후속 값을 사용한다 (또는 예외 처리)

```kotlin
fun A(number: Long?): Long {
		number ?: return 0
}
```

위와 같이 early return 방식으로도 사용 가능하다.

### null 아님 단언

```kotlin
fun startWith(str: String?): Boolean {
    return str!!.startWith("A")
}
```

`!!`를 사용하면 null아 아님을 단언할 수 있다. 이를 사용해 nullable type에서 컴파일 경고를 무시할 수 있다. 이 경우 잘못된 값이 들어오면 Runtime에 NPE가 발생할 수 있다.

**정말 확실한 경우에만 사용**

### 플랫폼 타입

코틀린은 자바의 어노테이션에 대한 정보를 이해한다. (일부 패키지 한정)

즉 자바 코드의 함수에 포함된 @NotNull / @Nullable 등을 사용 가능하다.

그러나 이런 어노테이션이 없는 경우 코틀린은 Nullable 여부를 알 수 없다.

- 이를 플랫폼 타입이라 하며, 이런 변수/메서드 사용 시 Runtime NPE 발생 가능성이 있음

## Type 처리

### 기본 타입

자바는 더 큰 타입으로의 형변환이 암시적으로 수행된다.

코틀린에서는 이런 암시적 형변환이 수행되지 않기 때문에 `to기본타입()`메서드를 사용해야 한다.

P.S) 추가 팁

만약 두 개의 정수 나눗셈 연산을 한다면 실수 결과를 얻기 위해 다음과 같이 사용한다.

`val result = number1 / number2.toDouble()`

### 일반 타입

```kotlin
public static void printAgeIfPerson(Object obj) {
    if (obj instanceof Person) {
        Person person = (Person) obj;
        System.out.println(person.getAge());    
    }
}
```

```kotlin
fun printAgeIfPerson(obj: Any) {
    if (obj is Person) {
        val person = obj as Person
        println(person.age)
    }
}
```

- 코틀린에서의 instanceof는 is이다.
- as는 타입 간주이다. (생략 가능, if문에서 이미 타입 체크를 하기 때문에 스마트캐스팅 된다)

**해당 타입이 아닌 경우를 체크**

```kotlin
fun printAgeIfPerson(obj: Any) {
    if (obj !is Person) { // 부정형 사용
        val person = obj as Person
        println(person.age)
    }
}
```

**obj가 nullable인 경우는?**

```kotlin
fun printAgeIfPerson(obj: Any?) {
    val person = obj as? Person
    println(person?.age)
}
```

`as` 대신 `as?`를 사용한다.

- 이 경우 person 또한 nullabel 객체가 되기에 사용시 safe call이 필요하다.
- **obj가 Person이 아닌 경우**
    - `as`를 사용하면 예외가 발생한다.
    - `as?`를 사용하면 null이 반환된다.

### Kotlin의 특이한 타입 3가지 - Any

- 자바의 Object 역할
- 모든 Primitive Type의 최상위 타입
- equals / hashCode / toString 존재

### Kotlin의 특이한 타입 3가지 - Unit

- 자바의 void와 동일한 역할

### Kotlin의 특이한 타입 3가지 - Notihing

함수가 정상적으로 끝나지 않았다는 사실을 표현

```kotlin
fun fail(message: String): Nothing {
		throw IllegalArgumentException(message)
}
```

항상 예외를 반환 / 무한 루프 등의 함수에 사용

### String Interpolation / String indexing

```kotlin
val person = Person("이름")
println("이름: ${person.name}")
```

위와 같이 문자열 처리가 가능하다.

```kotlin
val str = """
   ABC
   BBB
   CCC
""".trinIndent()
```

여러 줄 문자열은 “””를 사용하면 편리하게 작성 가능하다.

```kotlin
val str = "HELLO"
println(str[0])
```

인덱스를 활용해 특정 문자를 가져올 수 있다.

## 연산자

### 일반 연산자

단항 연산자, 산술 연산자, 산술대입 연산자 모두 Java와 동일

### 비교 연산자

- ≤ / ≥ / < / > 등의 연산을 이용해 객체를 비교하면 자동으로 `compareTo()`를 호출한다.
- Java는 동일성에서 == , 동등성에서 equals를 호출했다면 Kotlin에서는 동일성에 ===을, 동등성에 ==을 사용한다.
    - == 사용 시, 자동으로 `equals()`를 호출한다.

### 논리 연산자

- && / || / ! 등은 Java와 동일하다. (lazy연산 또한 수행됨)

### 특이한 연산자

Kotlin에서 생긴 연산자

`**in` 과 `!in`**: 컬렉션이나 범위에 포함 여부 확인

`**a..b`:** a부터 b까지의 범위 객체 생성

`**a[i]**`: 특정 인덱스 조회 및 값 대입

### 연산자 오버로딩

Kotlin에서는 객체마다 연산자를 직접 정의할 수 있다.
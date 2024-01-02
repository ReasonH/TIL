# 기타 특성

## ETC

### Type Alias와 as import

**Type alias**

타입 정의를 축약할 수 있는 기능이다.

```kotlin
typealias FruitFilter = (Fruit) -> Boolean

fun filterFruits(fruits: List<Fruit>, filter: FruitFilter) {}
```

**as import**

클래스나 함수를 import할 때 명칭을 변경할 수 있다.

동일한 이름의 메서드를 여러 패키지에서 import하는 경우 사용 가능하다.

### 구조 분해와 componentN 함수

```kotlin
data class Person(
    val name: String,
    val age: Int
)

val person = Person("이유혁", 100)
// 이건
val (name, age) = person

// 이것과 동일
val name = person.component1()
val age = person.component2()
```

구조 분해는 복합적인 값을 분해하여 여러 변수를 한 번에 초기화하는 것이다.

위와같이 data class를 통해 여러 변수를 초기화할 수 있으며 이 과정에서 component N 함수가 사용된다. (component N 함수는 data class가 자동으로 field 기반으로 생성)

초기화 순서를 바꿔도 component N은 순서대로 호출된다.

```kotlin
// 이건
val (age, name) = person

// 이것과 동일
val age = person.component1()
val name = person.component2()
```

**만약 data class가 아닌데 구조 분해를 사용하고 싶다면 component N을 직접 정의할 수 있음**

### Jump와 Label

**Jump**

return: 기본적으로 가장 가까운 enclosing function 또는 익명 함수로 값이 반환된다.

break: 가장 가까운 루프가 제거된다.

continue: 가장 가까운 루프를 다음 step으로 보낸다.

**주의점**

foreach에서는 continue나 break 등을 사용할 수 없다. label을 사용해야만 가능

**Label**

- 특정 expression에 붙여 라벨로 간주, return / break / continue 등 사용 가능
- 특정 조건에서 특정 라벨을 컨트롤할 수 있다.

label을 활용한 jump는 비추천한다. 복잡도 증가..

### TakeIf와 TakeUnless

주어진 조건 만족 시 null / 만족하지 않을 시 null 반환하는 구문

```kotlin
return number.takeUnless {it <= 0 }
```

위 코드는 number가 0이상이면 값 반환, 아닌 경우 null 반환한다.
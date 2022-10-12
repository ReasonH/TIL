# 키워드

### Open
Kotlin은 기본적으로 클래스의 상속과 메서드 오버라이딩을 막아놓았다. (추상 클래스 제외)
Open 키워드는 클래스와 메서드에 적용해서 상속과 오버라이딩을 가능하게 한다.
다음과 같이 abstract class에서 사용하는 경우 메서드의 오버라이딩을 선택적으로 할 수 있게 만든다.

```kotlin
abstract class Animal {

    // 추상 메서드는 반드시 override 해야 함
    abstract fun bark()

    // 이 메서드는 하위 클래스에서 선택적으로 override 할 수 있다. (하거나 안하거나 자유)
    open fun running() {
        println("animal running!")
    }
}

class Dog() : Animal() {

    override fun bark() {
        println("멍멍")
    }

    // 이 메서드는 override 하거나 하지 않거나 자유.
    override fun running() {
        println("dog's running!")
    }
}
```

### 접근제어자
private, public은 JAVA와 동일하다. 아무것도 없으면 기본 public이다.

##### Internal
Internal은 default 가시성 제어자와는 그 성격이 다르다. 먼저 코틀린은 패키지를 네임스페이스를 관리하기 위한 용도로만 사용하며 가시성 제어에 사용하지 않는다. 따라서 이를 위한 별도의 가시성 제어자도 없다. Internal은 같은 모듈 내에서만 볼 수 있으며, 모듈은 한번에 컴파일되는 코틀린 파일들을 의미한다.

##### protected
위에 기술했듯 코틀린은 패키지를 가시성 제어에 이용하지 않기 때문에 protected도 조금 다르게 동작한다. 이는 오직 클래스와 이 클래스를 상속한 클래스에서만 보인다.


### lateinit
나중에 초기화 하고싶은 변수를 정의하기 위해 사용한다.
초기화를 위해 임의의 값을 넣고 싶지 않을 때 사용한다. 값이 없는 경우 사용 시점에 Exception을 발생시킨다.
- Primitive Type만 사용 가능하다.
- var만 사용 가능하다. (당연히)

### by lazy
나중에 사용될 **상수**를 정의할 때 사용한다. 다음과 같이 사용할 수 있다.
```kotlin
val x : Int by lazy { inputValue.length }
inputValue = "Initialized!"
println(x)
```
이 경우 inputValue.length는 나중에 알 수 있으면서 x를 사용하기 전에는 알 수 있다.
항상 inputValue가 정해진 이후 x를 사용하는 상황에 사용 가능한 키워드이다.

lateinit과 by lazy는 기본적으로 나중에 초기화한다는 목적은 동일하지만, val과 var라는 큰 차이가 있다. 따라서 작업 특성에 맞게 적절히 활용해야 한다.
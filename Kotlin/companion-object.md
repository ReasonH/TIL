코틀린의 Companion Object는 과연 자바의 static과 동일할까?

### 코틀린의 class 키워드

```kotlin
class WhoAmI(private val name:String){
    fun myNameIs() = "나의 이름은 ${name}입니다."
}
fun main(args: Array<String>){
    val m1 = WhoAmI("영수")
    val m2 = WhoAmI("미라")
    println(m1.myNameIs()) //나의 이름은 영수입니다.
    println(m2.myNameIs()) //나의 이름은 미라입니다.
}
```

코틀린에서는 클래스를 정의하자마자 바로 생성자 및 속성까지 정의할 수 있도록 설계되었다. 객체를 생성할 때는 new 키워드 없이 (마치 함수 사용처럼) 괄호를 붙여서 사용한다.

### 코틀린의 object 키워드

object 키워드는 코틀린의 독특한 singleton 선언 방식이다.

```kotlin
object MySingleton{
    val prop = "나는 MySingleton의 속성이다."
    fun method() = "나는 MySingleton의 메소드다."
}
fun main(args: Array<String>){
    println(MySingleton.prop);    //나는 MySingleton의 속성이다.
    println(MySingleton.method());   //나는 MySingleton의 메소드다.
}
```

object는 특정 클래스나 인터페이스를 확장(**var obj = object:MyClass(){}**
 또는 **var obj = object:MyInterface{}**)해 만들 수 있으며 선언문이 아닌 표현식(**var obj = object{}**
)으로 생성할 수 있다.

### Companion object ≠ static

코틀린 companion object는 static처럼 동작하는 것으로 보일 뿐 static이 아니다.

```kotlin
class MyClass2{
    companion object{
        val prop = "나는 Companion object의 속성이다."
        fun method() = "나는 Companion object의 메소드다."
    }
}
fun main(args: Array<String>) {
    //사실은 MyClass2.맴버는 MyClass2.Companion.맴버의 축약표현이다.
    println(MyClass2.Companion.prop)
    println(MyClass2.Companion.method())
}
fun main(args: Array<String>) {
    //사실은 MyClass2.맴버는 MyClass2.Companion.맴버의 축약표현이다.
    println(MyClass2.prop)
    println(MyClass2.method())
}
```

method와 변수에 직접 접근이 되는 것은 축약 표현일 뿐이다.

### Companion object는 객체이다.

중요한 것은 companion object가 객체라는 점이다.

companion object는 객체이기 때문에 변수에 할당할 수 있으며 멤버에도 접근이 가능하다. 이는 자바의 static 키워드에서는 불가능한 일이다.

```kotlin
class MyClass2{
    companion object{
        val prop = "나는 Companion object의 속성이다."
        fun method() = "나는 Companion object의 메소드다."
    }
}
fun main(args: Array<String>) {
    println(MyClass2.Companion.prop)
    println(MyClass2.Companion.method())

    val comp1 = MyClass2.Companion  //--(1)
    println(comp1.prop)
    println(comp1.method())

    val comp2 = MyClass2  //--(2)
    println(comp2.prop)
    println(comp2.method())
}
```

(1)과 (2)를 보면 companion object가 바로 변수에 할당되는 것을 확인할 수 있다.

### 그 외 특징

-   companion object는 이름을 지을 수 있다. `companion object xxx{}`
-   클래스 내 companion object는 딱 하나만 쓸 수 있다.
-   인터페이스 내에도 companion object를 정의할 수 있다.
-   상속 관계에서 companion object 멤버는 같은 이름일 경우 가려진다. (shadowing)

책임은 객체가 다른 객체와 협력하기 위해 수행하는 행동이며 역할은 대체 가능한 책임의 집합이다. 책임은 객체지향 설계의 핵심이며 책임을 할당하는 작업은 응집도와 결합도 같은 설계 품질과 깊이 연관돼 있다.

## 1. 데이터 중심의 영화 예매 시스템

훌륭한 객체지향 설계는 데이터가 아닌 책임에 초점을 맞춰야 한다. 객체의 상태는 구현에 속한다. 데이터에 초점을 맞추는 경우 객체의 상태 변경이 인터페이스 변경을 초래하므로 변경에 취약해지게 된다. 이에 반해 객체의 책임은 인터페이스에 속한다. 따라서 상대적으로 변경에 안정적인 설계를 얻을 수 있게 된다.

<코드 중략>

## 2. 설계 트레이드 오프

캡슐화, 응집도, 결합도라는 세 가지 품질 척도의 의미를 살펴본다.

### 캡슐화

변경 가능성이 높은 부분 (구현)을 객체 내부로 숨기는 추상화 기법이다. 변경될 수 있는 어떤 것이라도 캡슐화해야 한다.

### 응집도와 결합도

-   응집도는 모듈에 포함된 내부 요소들의 연관도이다. 클래스에 얼마나 관련이 높은 책임들을 할당했는지를 나타낸다.
-   결합도는 의존성의 정도를 나타낸다. 객체지향 관점에서 객체가 협력에 필요한 적절 수준의 관계만을 유지하는지를 나타낸다.

높은 응집도와 낮은 결합도를 추구하는 이유는 한 가지다. 그것이 설계를 변경하기 쉽게 만들기 때문이다. 캡슐화의 정도는 응집도와 결합도에 영향을 미치기에 응집도와 결합도를 고려하기 전 먼저 캡슐화를 향상시키기 위해 노력해야 한다.

## 3. 데이터 중심 영화 예매 시스템의 문제점

데이터 중심의 설계는 캡슐화를 위반하고, 객체의 내부 구현을 인터페이스의 일부로 만든다.

### 캡슐화 위반

```java
class Movie {
	int fee;

	public int getFee() {...}
	public void setFee(int fee) {...}
}
```

데이터 중심으로 설계한 Movie클래스의 getter와 setter는 인스턴스 변수를 퍼블릭 인터페이스에 노골적으로 드러낸다. 이런 캡슐화 위반은 객체의 설계에서 책임이 아닌 내부 데이터에 초점을 맞췄기때문에 발생한다.

설계할 때 협력에 관해 고민하지 않으면 과도한 접근자와 수정자를 가지게 되는 경향이 있다. 이는 객체의 캡슐화를 위반하는 변경에 취약한 설계를 만들어낸다.

### 높은 결합도

객체 내부의 구현이 인터페이스에 드러난다는 것은 클라이언트가 구현에 강하게 결합된다는 것을 의미한다. 위의 getFee() 메서드 등은 사실상 인스턴스 변수 fee의 가시성을 public으로 변경하는 것과 동일하다.

결합도 측면에서의 데이터 중심 설계가 갖는 단점은 하나의 제어 객체가 다수의 데이터 객체에 강하게 결합된다는 것이다. 이 때문에 **제어 객체는 데이터 객체의 수정에 큰 영향을 받게 된다.**

### 낮은 응집도

서로 다른 이유로 변경되는 코드가 하나의 모듈에 공존할 때 모듈 응집도가 낮다고 말한다. 낮은 응집도는 두 가지 측면에서 설계에 문제를 일으킨다.

1.  변경과 아무런 상관없는 코드들이 영향을 받게 된다.
2.  하나의 요구사항 변경을 반영하기 위해 동시에 여러 모듈을 수정해야 한다. 다른 모듈에 위치해야 할 책임의 일부가 엉뚱한 곳에 위치하게 되기 때문이다.

> 이는 단일 책임 원칙을 위배한 것과도 같다.

## 4. 자율적인 객체를 향해

### 캡슐화를 지켜라

객체는 스스로 상태를 책임져야 하며 외부에서는 인터페이스에 정의된 메서드를 통해서만 상태에 접근할 수 있어야 한다. 이 때 메서드는 단순 속성 접근/수정자를 의미하는 것이 아니다. 속성의 가시성을 private으로 설정했어도 접근자와 수정자를 통해 외부로 제공하고 있다면 캡슐화를 위반하는 것이다. 접근자 메서드로 속성을 노출하기 전 책임을 객체에게 이동시켜서 캡슐화를 강화해야 한다.

### 스스로 자신의 데이터를 책임지는 객체

객체는 내부의 데이터보다 협력에서 수행할 책임을 정의하는 오퍼레이션이 더 중요하다.

-   이 객체가 어떤 데이터를 포함해야 하는가?
-   이 객체가 데이터에 대해 수행해야 하는 오퍼레이션은 무엇인가?

두 질문을 조합하면 객체 내부 상태를 저장하는 방식과 오퍼레이션 집합을 얻을 수 있다.

## 5. 하지만 여전히 부족하다

다음은 데이터 중심의 객체를 수정한 사례에서 문제점을 살펴본다.

### 캡슐화 위반

```java
public class DiscountCondition {
	private DiscountConditionType type;
	private int sequence;
	...

	public DiscountConditionType getType() {...}
	public boolean isDiscountable(int sequence) {...}
	public boolean isDiscountable(...) {...}
}
```

위 객체는 얼핏 보면 객체가 자신의 데이터를 스스로 처리하는 것으로 보일 수 있다. 그러나 오퍼레이션의 파라미터를 살펴보면 내부의 속성을 외부에 노출하고 있음을 알 수 있다. getType 메서드 또한 내부에 type을 포함하고 있음을 외부로 노출하고 있다. 이 상태에서 객체 내부 속성을 변경해야 한다면 파급효과가 외부로 영향을 줄 수 밖에 없다. 즉 캡슐화 실패로 볼 수 있다.

```java
public class Movie {
	...

	public MovieType getType() {...}
	public Money calculateAmountDiscountPolicy() {...}
	public Money calculatePercentDiscountPolicy() {...}
	public Money calculateNoneDiscountPolicy() {...}
}
```

이 경우도 동일하다. 파라미터와 반환값이 내부 속성을 노출하고 있지 않지만, 메서드 명에서 세 가지 할인 정책이 존재한다는 것을 노출하고 있다. 새로운 할인 정책의 추가나 제거가 발생할 때 외부 클라이언트가 영향을 받을 수 밖에 없다.

> 캡슐화는 내부 속성을 감추는 것만이 아닌 변경될 수 있는 어떤 것이라도 감추는 것을 의미한다.

위와 같이 제대로 캡슐화되지 않은 객체를 사용하는 클라이언트는 해당 객체의 내부 구현만 변경되더라도 동시에 변경이 이루어져야 한다. 이는 캡슐화 위반으로 인한 높은 결합도, 낮은 응집도 사례라고 할 수 있다. 데이터 중심을

위 설계는 여전히 데이터 중심 설계가 갖는 문제점을 지니고 있다.

## 6. 데이터 중심 설계의 문제점

데이터 중심 설계가 변경에 취약한 이유는 두 가지이다.

1.  본질적으로 너무 이른 시기에 데이터에 관해 결정하도록 강요
    -   데이터에 관한 지식이 객체 인터페이스에 고스란히 드러나게 된다.
2.  협력을 고려하지 않고, 객체를 고립시킨 채 오퍼레이션을 결정
    -   다른 객체는 이미 구현된 인터페이스에 억지로 끼워맞추게 된다.
    -   인터페이스에 구현이 노출되고, 구현이 변경되면 협력하는 객체도 영향을 받게 된다.
# 조건부 로직 간소화

조건부 로직은 프로그램의 힘을 강화하는 동시에 프로그램을 복잡하게 만드는 주요 원인이다.

### 10.1 조건문 분해하기

조건을 검사하고 그 결과에 따른 동작을 표현한 코드는 ‘왜' 그 일이 일어나는지 제대로 말해주지 않을 때가 많다. 거대한 코드 블록을 부위별로 분해한 다음 해체된 코드 덩어리들을 함수 호출로 바꿔주면 그 의도가 더 확실히 드러난다. 이렇게 하면 해당 조건이 무엇이며 무엇을 분기했는지와 분기한 이유 역시 명확해진다.

**절차**

1. 조건식과 그 조건식에 딸린 조건절 각각을 함수로 추출

- 취향에 따라 전체 조건문을 3항 연산자로 바꿀수도 있다.

### 10.2 조건식 통합하기

비교하는 조건은 다르지만 그 결과로 수행하는 동작은 동일한 코드들이 더러 있다.

이런 로직들을 하나로 합치면 다음의 이점을 얻는다.

1. 하려는 일이 더 명확해진다.
2. 작업을 **함수 추출하기**로 이어갈 수 있다. → 이를 통해 ‘무엇'을 ‘왜'로 바꿀 수 있다.

<aside>
💡 반면, 하나의 검사라고 생각할 수 없는, 진짜 독립된 검사라고 판단되면 이 리팩터링을 행해서는 안된다.

</aside>

**절차**

1. 해당 조건식 모두에 부수효과가 없는지 확인
2. 조건문 두 개를 선택해 조건식들을 논리 연산자로 결합

   → 같은 계층인 경우 or 결합, 중첩 조건인 경우 and 결합

3. 테스트
4. 조건이 하나만 남을 때까지 반복
5. 하나로 합쳐진 조건식을 함수로 추출할지 고려

### 10.3 중첩 조건문을 보호 구문으로 바꾸기

조건문은 참인 경로와 거짓인 경로 모두 정상 동작으로 이어지는 형태와 한쪽만 정상인 형태 두 가지로 쓰인다.

두 경로 모두가 정상 동작이라면 if와 else절을 사용하지만, 한 쪽만 정상 동작한다면 비정상 조건을 if에서 검사하고, 참인 경우 함수에서 빠져나올 수 있다. 이를 **보호 구문**이라 한다.

보호 구문의 핵심은 리팩터링의 의도를 부각하는 데 있다. 비정상 조건인 경우는 함수의 핵심이 아니며 이런 일이 발생했을 때 조치를 취한 후 함수에서 빠져나온다고 이야기할 수 있다.

**절차**

1. 교체해야 할 조건 중 가장 바깥 것을 선택하여 보호 구문으로 변경
2. 테스트
3. 위 과정을 필요만큼 반복
4. 모든 보호 구문이 같은 결과를 반환한다면 보호 구문들의 조건식을 통합

```jsx
function payAmount(employee) {
  let result;
  if (employee.isSeparated) {
    result = { amount: 0, reasonCode: "SEP" };
  } else {
    if (employee.isRetired) {
      result = { amount: 0, reasonCode: "RET" };
    } else {
      // 급여 계산 로직
      result = someFinalComputation();
    }
  }
  return result;
}
```

이 코드가 진짜 의도한 일은 모든 조건이 거짓일 때만 실행된다. 보호 구문을 사용하면 이런 코드의 의도를 더 잘 드러낼 수 있다.

1. 최상위 조건을 보호구문으로 변경

   ```jsx
   function payAmount(employee) {
     let result;
     if (employee.isSeparated) return { amount: 0, reasonCode: "SEP" };
     if (employee.isRetired) {
       result = { amount: 0, reasonCode: "RET" };
     } else {
       // 급여 계산 로직
       result = someFinalComputation();
     }
     return result;
   }
   ```

2. 테스트하고 다음 조건으로 넘어간다.

   ```jsx
   function payAmount(employee) {
     if (employee.isSeparated) return { amount: 0, reasonCode: "SEP" };
     if (employee.isRetired) return { amount: 0, reasonCode: "RET" };

     // 급여 계산 로직
     result = someFinalComputation();
     return result;
   }
   ```

   여기에서는 result 변수도 역할이 없기에 지워질 수 있다.

이 리팩터링은 조건식을 반대로 만들어 적용하는 경우도 많다.

### 10.4 조건부 로직을 다형성으로 바꾸기

복잡한 조건부 로직은 더 높은 수준의 개념을 도입해 분리해낼 수 있다.

**예시로, 타입을 여러 개 만들고 각 타입이 조건부 로직을 자신만의 방식으로 처리하도록 구성하는 방법이 있다. 타입을 기준으로 분기하는 switch 문이 많이 보인다면 분명 이런 상황이다. 이런 경우 case별로 클래스를 하나 씩 만들어 공통 switch 로직의 중복을 없앨 수 있다.**

다른 예로, 기본 동작을 위한 case 문과 변형 동작으로 구성된 로직도 있다. 가장 일반적이거나 직관적인 동작을 슈퍼클래스로 넣어서 기본에 집중하게 하고, 변형 동작을 뜻하는 case들을 서브클래스로 만드는 것이다.

조건부 로직을 모두 다형성으로 대체해야하는 것은 아니지만, 복잡한 조건부 로직인 경우 다형성이 막강한 도구가 될 수 있다.

**절차**

1. 다형성 동작을 표현하는 클래스들이 없다면 만들어준다. 적합한 인스턴스를 반환하는 팩터리 함수도 같이 만든다.
2. 호출 코드에서 팩터리 함수를 사용하게 한다.
3. 조건부 로직 함수를 슈퍼클래스로 옮긴다. (조건부 로직이 온전함 함수로 분리되어있지 않다면 먼저 함수로 추출한다.)
4. 서브클래스 중 하나를 선택한다. 서브클래스에서 슈퍼클래스의 조건부 로직 메서드를 오버라이드 한다.
5. 같은 방식으로 각 조건절을 해당하는 서브클래스에서 메서드로 구현한다.
6. 슈퍼클래스는 기본 동작만 남긴다. 슈퍼클래스가 추상 클래스여야 한다면 이 메서드를 추상으로 선언하거나 서브클래스에서 처리해야함을 알리는 에러를 던진다.

### 10.4 - 예시

다음의 예제외 함께 보자.

```jsx
function plumages(birds) {
  return new Map(birds.map((b) => [b.name, plumage(b)]));
}

function plumgae(bird) {
  switch (bird.type) {
    case "유럽 제비":
      return "보통이다.";
    case "아프리카 제비":
      return bird.numberOfCoconuts > 2 ? "지쳤다" : "보통이다";
    default:
      return "알 수 없다.";
  }
}
```

가장 먼저 plumage()를 Bird라는 클래스로 묶어본다.

```jsx
function plumages(birds) {
  return new Map(birds.map((b) => [b.name, plumage(b)]));
}

function plumage(bird) {
  return new Bird(bird).plumage;
}

class Bird {
  constructor(birdObject) {
    Object.assign(this, birdObject);
  }

  get plumage() {
    switch (this.type) {
      case "유럽 제비":
        return "보통이다.";
      case "아프리카 제비":
        return this.numberOfCoconuts > 2 ? "지쳤다" : "보통이다";
      default:
        return "알 수 없다.";
    }
  }
}
```

이제 종 별 서브클래스를 만들고, 객체를 얻을 때 팩터리 함수를 사용하도록 변경한다.

```jsx
function plumage(bird) {
	return createBird(bird).plumage;
}

function createBird(bird) {
		switch (bird.type) {
		case '유럽 제비':
			return new EuropeanSwallow(bird);
		case '아프리카 제비':
			return new AfricaSwallow(bird);
		default:
			return new Bird(bird);
}

class EuropeanSwallow extends Bird {
}

class AfricaSwallow extends Bird {
}
```

switch 문의 절을 서브클래스에서 오버라이드한다.

```jsx
class EuropeanSwallow extends Bird {
  get plumage() {
    return "보통이다";
  }
}

class AfricaSwallow extends Bird {
  get plumage() {
    return this.numberOfCoconuts > 2 ? "지쳤다" : "보통이다";
  }
}

class Bird {
  constructor(birdObject) {
    Object.assign(this, birdObject);
  }

  get plumage() {
    return "알 수 없다.";
  }
}
```

최종 코드는 다음과 같다. (plumage는 plumages로 인라인했다.)

```jsx
function plumages(birds) {
	return new Map(birds
									.map(b => createBird(b))
									.map(bird => [bird.name, bird.plumage]));
}

function createBird(bird) {
		switch (bird.type) {
		case '유럽 제비':
			return new EuropeanSwallow(bird);
		case '아프리카 제비':
			return new AfricaSwallow(bird);
		default:
			return new Bird(bird);
}

class Bird {
	constructor(birdObject) {
		Object.assign(this, birdObject);
	}

	get plumage() {
		return "알 수 없다.";
	}
}

class EuropeanSwallow extends Bird {
	get plumage() {
		return "보통이다";
	}
}

class AfricaSwallow extends Bird {
	get plumage() {
		return (this.numberOfCoconuts > 2) ? "지쳤다" : "보통이다";
	}
}
```

<aside>
💡 위의 예에서는 계층 구조를 정확히 새의 종 분류에 맞게 구성했다. 상속은 거의 똑같지만 다른 부분이 있음을 표현할 때도 사용된다.

</aside>

### 10.5 특이 케이스 추가하기 → 뭔 개소린지 ㄹㅇ 이해가안됨…

코드베이스에서 특정 값에 대해 똑같이 반응하는 코드가 여러 고시알면 이를 한 데로 모으는 게 효율적이다.

특수한 경우의 공통 동작을 요소 하나에 모아서 사용하는 특이 케이스 패턴이라는 것이 있는데, 이럴 때 적용하면 좋은 메커니즘이다.

- 특이 케이스 객체에서 단순히 데이터를 읽기만 한다면 반환할 값들을 담은 리터럴 객체 형태로 준비한다.
- 특이 케이스 객체는 캡슐화한 클래스가 반환하도록 만들 수도, 변환을 거쳐 데이터 구조에 추가하는 형태도 될 수 있다.

**절차**

이 절차에서는 리팩터링의 대상이 될 속성을 담은 데이터 구조(클래스)를 컨테이너라 지칭한다. 컨테이너가 가질 수 있는 값 중 특별히 다뤄야 할 값을 특이 케이스 클래스로 대체하고자 한다.

1. 컨테이너에 특이 케이스인지 검사하는 속성을 추가하고, false를 반환하게 한다.
2. 특이 케이스 객체를 만든다. 특이 케이스인지 검사하는 속성만 포함하며, 이는 true를 반환하게 한다.
3. 클라이언트에서 특이 케이스인지 검사하는 코드를 함수로 추출한다. 모든 클라이언트가 이 함수를 사용하도록 고친다.
4. 코드에 새로운 특이 케이스 대상을 추가한다. 함수 반환 값으로 받거나 변환 함수를 적용하면 된다.
5. 특이 케이스를 검사하는 함수 본문을 수정하여 특이 케이스 객체의 속성을 사용하도록 한다.
6. 테스트
7. 여러 함수를 클래스로 묶기나 변환 함수로 묶기를 적용하여 특이 케이스를 처리하는 공통 동작을 새로운 요소로 옮긴다.
8. 특이 케이스 검사를 이용하는 곳이 남아 있다면 검사 함수를 인라인한다.

### 10.5 예제

```jsx
class Site {
	get customer() {
		return this._customer;
	}
}

class Customer {
	get name() {...}
	get billingPlan() {...}
	get billingPlan(arg) {...}
	get paymentHistory() {...}
}

// 클라1
const aCustomer = site.customer;
...
let customerName;
if (aCustomer === "미확인 고객") customerName = "거주자";
else customerName = aCustomer.name;

// 클라2
const plan = (aCustomer === "미확인 고객") ?
			registry.billingPlans.basic : aCustomer.billingPaln;

// 클라3
if (aCustomer != "미확인 고객") aCustomer.billingPlan = newPlan;

// 클라4
const weeksDelinquent = (aCustomer === "미확인 고객") ?
			 0 : aCustomer.paymentHistory.weeksDelinquent;
```

코드베이스에서 미확인 고객을 처리해야 하는 클라이언트가 여러 개 발견됐고, 대부분 동일한 처리를 수행했다고 가정한다.

- 고객 이름(customerName): “거주자”
- 요금제(billingPlan): 기본 요금제
- 연체 기간(weeksDelinquent) : 0주

1. 먼저 미확인 고객인지를 나타내는 메서드를 고객 클래스에 추가한다.

   ```jsx
   class Customer {
   	get name() {...}
   	get billingPlan() {...}
   	get billingPlan(arg) {...}
   	get paymentHistory() {...}
   	**get isUnknown() {return false;} // --- 추가**
   }

   ```

2. 미확인 고객 전용 클래스를 만든다.

   ```jsx
   **class UnknownCustomer {
   	get isUnknown() {return true;}
   }**
   ```

이제 미확인 고객을 사용하는 모두에 새로 만든 `UnknownCustomer`를 반환하도록 하고, 값이 “**미확인 고객**"인지 검사하는 곳 모두에서 새로운 `isUnknown()` 메서드를 사용하도록 고쳐야 한다.

그러나 `Customer`를 수정하여 “미확인 고객" 문자열 대신 `UnknownCustomer`를 반환하도록 하는 경우 클라이언트들 각각이 `isUnknown()`호출을 위해 코드 수정작업을 해야한다.

이 때 여러 곳에서 똑같이 수정해야만 하는 코드를 별도 함수로 추출하여 한데로 모을 수 있다.

```jsx
function isUnknown(arg) {
  if (!(arg instanceof Customer || arg === "미확인 고객")) throw new Error("잘못된 값과 비교: <$(arg)>");
  return arg === "미확인 고객";
}
```

이제 이 isUnknown() 함수를 이용해 미확인 고객인지를 확인할 수 있다.

```jsx
// 클라1
const aCustomer = site.customer;
...
let customerName;
**if (isUnknown(aCustomer)) customerName = "거주자"; // -- 수정**
else customerName = aCustomer.name;

// 클라2
const plan = (**isUnknown(aCustomer)**) ? **// -- 수정**
			registry.billingPlans.basic : aCustomer.billingPaln;

// 클라3
if (!**isUnknown(aCustomer)**) aCustomer.billingPlan = newPlan; **// -- 수정**

// 클라4
const weeksDelinquent = (**isUnknown(aCustomer)**) ? **// -- 수정**
			 0 : aCustomer.paymentHistory.weeksDelinquent;
```

1. 모든 부분이 수정되었다면 `Site` 클래스가 `UnknownCustomer`객체를 반환하도록 수정한다.

   ```jsx
   class Site {
   	get customer() {
   		**return (this._customer === "미확인 고객") ? new UnknownCustomer() : this._customer;**
   	}
   }
   ```

2. `isUnknown()`을 수정하여 고객 객체 속성을 사용하도록 하면 “미확인 고객” 문자열을 사용하던 코드는 사라진다.

   ```jsx
   function isUnknown(arg) {
   	if (!(arg instanceof Customer || **arg instanceof UnknownCustomer**)) // -- 수정
   		throw new Error('잘못된 값과 비교: <$(arg)>');
   	return arg.isUnknown;
   }
   ```

3. 테스트
4. 각 클라이언트에서 수행하는 특이 케이스 검사를 일반적인 기본값으로 대체 가능하다면 검사 코드에 여러 함수를 클래스로 묶기를 적용한다.

   ```jsx
   class UnknownCustomer {
   	get isUnknown() {return true;}
   	**get name() {return "거주자";} // -- 추가
   	get billingPlan() {return registry.billingPlans.basic;} // -- 추가
   	get billingPlan(arg) {/* 무시 */} // -- 추가
   	get paymentHistory() {return new NullPaymentHistory();}**
   }

   class NullPaymentHistory() {
   	get weeksDelinquentInLastYear() {return 0;}
   }

   // 클라1
   const aCustomer = site.customer;
   ...
   // name을 결정하는 조건부 로직이 삭제됨
   **const** customerName = aCustomer.name;

   // 클라2
   **const plan = aCustomer.billingPaln;**

   // 클라3
   **aCustomer.billingPlan = newPlan;**

   // 클라4
   **const weeksDelinquent = aCustomer.paymentHistory.weeksDelinquent;**
   ```

5. 계속해서 모든 클라이언트 코드를 다형적 행위로 대체할 수 있는지 살펴본다.

   - ex) 미확인 고객의 이름으로 “**거주자**"가 아닌 “**미확인 거주자**”를 사용하는 클라이언트가 있을 수 있음
     - `const name = !isUnKnown(aCustomer) ? aCustomer.name : “미확인 거주자”`
   - 이런 경우엔 원래의 특이 케이스 검사 코드를 유지하면서 `isUnknown()`을 인라인한다.
     - `const name = aCustomer.isUnKnown ? aCustomer.name : “미확인 거주자”`

   모든 클라이언트를 수정했다면 호출하는 곳이 없어진 전역 `isUnknown()`을 없앤다.

### 10.5 예제: 객체 리터럴 이용하기

앞의 예와 다르게 데이터 구조를 읽기만 한다면 클래스 대신 리터럴 객체(java의 경우 VO)를 사용해도 된다.

### 10.6 어서션 추가하기

특정 조건이 참일 때만 제대로 동작하는 코드 영역을 명시적으로 표시하기 위해서는 어서션을 사용한다.

어서션은 항상 참이라고 가정하는 조건부 문장으로, 오류 찾기는 물론 프로그램이 어떤 상태를 가정한 채 실행되는지를 다른 개발자에게 알려주는 소통 도구도 될 수 있다.

**절차**

1. 참이라고 가정하는 조건이 보이면 그 조건을 명시하는 어서션을 추가한다.

> 참이라고 생각하는 모든 가정에 어서션을 달지는 않는다. 이는 미세하게 자주 조정되기 때문에 중복 코드가 있다면 큰 문제가 될 수 있다. 따라서 이런 조건들에서 중복은 반드시 남김없이 제거해야 한다.
> ex) 데이터를 외부에서 읽어올 때 그 값을 검사하는 작업은 어서션이 아닌 예외 처리로 대응해야 한다.

개인적으로 이 부분은 공감하지 못하겠다 …

### 10.7 제어 플래그를 탈출문으로 바꾸기

제어플래그란 코드 동작을 변경하는 데 사용되는 변수를 말한다. 어딘가에서 값을 계산해 설정한 후 다른 어딘가의 조건문에서 검사하는 형태로 쓰인다. 이는 리팩터링으로 충분히 간소화할 수 있는 코드 악취이다.

이는 반복문에서 주로 발견된다.

**절차**

1. 제어 플래그를 갱신하는 코드 각각을 적절한 제어문으로 바꾼다. 하나 바꿀 때마다 테스트한다.
2. 모두 수정했다면 제어 플래그를 제거한다.

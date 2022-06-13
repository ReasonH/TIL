# API 리팩터링

모듈과 함수는 소프트웨어를 구성하는 빌딩 블록이며, API는 이 블록을 끼워 맞추는 연결부다. 좋은 API는 데이터를 갱신하는 함수와 조회만 하는 함수를 명확히 구분한다. 두 기능이 섞여 있다면 갈라놔야 한다.

### 11.1 질의 함수와 변경 함수 분리하기

외부에서 관찰할 수 있는 겉보기 부수효과가 전혀 없이 값을 반환해주는 함수를 추구해야 한다. 이는 어디서 얼마나 호출해도 된다. 그렇기에 테스트하기 또한 쉽다. 부수효과가 있는 함수와 없는 함수는 명확히 구분하는 것이 좋다.

값을 반환하며 부수효과도 있는 함수가 있다면 변경 부분과 질의 부분을 분리해야 한다.

**절차**

1. 대상 함수를 복제하고 질의 목적에 충실한 이름을 짓는다.
2. 새 질의 함수에서 부수효과를 모두 제거한다. (객체 수정 작업이 있는 부분을 제거한다.)
3. 정적 검사
4. 원래 함수를 호출하는 곳을 모두 찾아낸다. 반환 값을 사용하고 있다면 질의함수를 호출하도록 바꾸고, 원래 함수를 호출하는 코드를 바로 아래 새로 추가한다. (테스트)
5. 원래 함수에서 질의 관련 코드를 제거한다.
6. 테스트

### 11.2 함수 매개변수화하기

두 함수의 로직이 아주 비슷하고 리터럴 값만 다르다면, 다른 값만 매개변수로 받아 처리함으로써 중복을 없앨 수 있다.

```jsx
function tenPercentRaise(aPerson) {
  aPerson.salary = aPerson.salary.multiply(1.1);
}

function fivePercentRaise(aPerson) {
  aPerson.salary = aPerson.salary.multiply(1.05);
}
```

**절차**

1. 비슷한 함수 중 하나를 선택한다.
2. 함수 선언 바꾸기로 리터럴들을 매개변수롤 추가한다.
3. 이 함수를 호출하는 곳 모두에 적절한 리터럴 값을 추가한다.
4. 테스트
5. 매개변수로 받은 값을 사용하도록 함수 본문을 수정한다.
6. 비슷한 다른 함수를 호출하는 코드를 찾아 매개변수화된 함수를 호출하도록 하나씩 수정한다.

### 11.3 플래그 인수 제거하기

플래그 인수가 있으면 함수들의 기능 차이가 잘 드러나지 않으며 이로인해 함수에 대한 이해를 어렵게 만든다. 특히boolean은 읽는 이에게 뜻을 온전히 전달하지 못하기 때문에 더욱 좋지 못하다. 이보다는 특정한 기능 하나만 수행하는 함수를 제공하는 편이 훨씬 깔끔하다.

함수 하나에서 플래그 인수를 두 개 이상 사용한다면 플래그 인수를 사용해야하는 합당한 근거가 될 수 있다. 그러나 다른 관점에서 보면 이는 함수가 너무 많은 일을 처리하고 있다는 신호이기도 하다.

**절차**

1. 매개변수로 주어질 수 있는 값 각각에 명시적 함수 생성
2. 원래 함수를 호출하는 코드들을 찾아서 각 플래그 리터럴에 대응되는 명시적 함수 호출하도록 수정

### 11.4 객체 통째로 넘기기

레코드 (VO)를 통째로 넘기면 변화에 대응하기 쉽다. 함수가 더 다양한 데이터를 사용하도록 바뀌어도 매개변수 목록은 수정할 필요가 없고, 함수 사용법을 이해하기 쉬워진다.

그러나 레코드와 함수가 서로 다른 모듈에 속한 상황처럼 함수가 레코드 자체에 의존하기를 원치 않을 때는 이를 수행하지 말아야 한다.

- 특정 객체로부터 값 몇 개를 얻은 후 그 값들만으로 무언가를 하는 로직이 있다면 로직을 객체 안으로 집어넣어야 한다.
- 한 객체가 제공하는 기능 중 똑같은 일부만을 사용하는 코드가 많다면 이를 따로 묶어 클래스로 추출하라는 신호일 수 있다.

**절차**

1. 매개변수들을 원하는 형태로 받는 빈 함수를 만든다.
2. 새 함수 본문에서는 원래 함수를 호출하도록 하며, 새 매개변수와 원래 함수의 매개변수를 매핑한다.
3. 정적검사
4. 모든 호출자가 새 함수를 사용하게 수정한다.
5. 호출자를 모두 수정했다면 원래 함수 새 함수에 인라인한다.
6. 새 함수 이름을 적절히 수정한다.

### 11.5 매개변수를 질의 함수로 바꾸기

매개변수 목록은 함수의 변동 요인을 모아놓은 곳이다. 이 목록에서도 중복은 피하는게 좋으며 짧을수록 이해하기 쉽다. **피호출 함수가 스스로 쉽게 결정할 수 있는 값을 매개변수로 전달하는 것도 일종의 중복이다.**

매개변수가 있다면 결정 주체가 호출자가 되고, 없다면 피호출 함수가 된다. 피호출 함수가 역할을 수행하기에 충분하다면 책임 소재를 피호출 함수로 옮긴다.

매개변수를 제거하면 피호출 함수에 원치 않는 의존성이 생길 때는 매개변수를 질의 함수로 바꾸지 말아야 한다.

<aside>
💡 대상 질의 함수는 참조 투명성이 보장되어야 한다. (똑같은 값을 건네면 똑같은 값이 반환)

</aside>

**절차**

1. 대상 매개변수 값을 계산하는 코드를 별도 함수로 추출
2. 함수 본문에서 대상 매개변수로의 참조를 모두 찾아서 매개변수 값을 만들어주는 표현식을 참조하도록 변경
3. 함수 선언 바꾸기로 대상 매개변수 삭제

### 11.6 질의 함수를 매개변수로 바꾸기

함수 안에 두지 않고 싶은 참조의 경우 참조를 매개변수로 바꿔 해결할 수 있다. 참조를 풀어내는 책임을 호출자로 옮기는 것이다.

이는 대부분 코드 의존 관계를 바꾸려 할 때 벌어진다. 함수들끼리 많은 것을 공유하여 결합을 만들어 내거나, 모든 것을 매개변수로 바꿔 반복적인 매개변수 목록을 만들 수도 있다. 특정 시점의 결정이 영원히 옳다고 할 수 없기 때문에 개선하기 쉽도록 설계하는게 중요하다.

**절차**

1. 변수 추출하기로 질의 코드A와 나머지 코드B로 분리
2. 함수 본문 중 해당 질의를 호출하지 않는 B 코드들을 별도 새 함수로 추출
3. 방금 만든 변수 질의 코드를 새 함수 호출 매개변수로 인라인하여 제거
4. 원래 함수도 인라인
5. 새 함수의 이름을 원래 함수의 이름으로 고쳐준다.

```jsx
get targetTemperature() {
	if (thermostat.selectedTemperature > this._max) return this._max;
	else if (thermostat.selectedTemperature < this._min) return this._min;
	else return thermostat.selectedTemperature;
}

if (thePlan.targetTemperature > thermostat.currentTemperature) setToHeat();
else if (thePlan.targetTemperature < thermostat.currentTemperature) setToCool();
else setOff();
```

1. 변수 추출하기로 메서드에서 사용할 매개변수를 준비

   ```jsx
   get targetTemperature() {
   	const selectedTemperature = thermostat.selectedTemperature;
   	if (selectedTemperature > this._max) return this._max;
   	else if (selectedTemperature < this._min) return this._min;
   	else return selectedTemperature;
   }
   ```

2. 매개변수 값 구하는 코드를 제외한 나머지를 메서드로 추출

   ```jsx
   get targetTemperature() {
   	const selectedTemperature = thermostat.selectedTemperature;
   	return this.xxNEWtargetTemperature(selectedTemperature);
   }

   xxNEWtargetTemperature(selectedTemperature) {
   	if (selectedTemperature > this._max) return this._max;
   	else if (selectedTemperature < this._min) return this._min;
   	else return selectedTemperature;
   }
   ```

3. 변수 인라인

   ```jsx
   get targetTemperature() {
   	return this.xxNEWtargetTemperature(thermostat.selectedTemperature);
   }
   ```

4. 메서드를 인라인

   ```jsx
   if (thePlan.xxNEWtargetTemperature(thermostat.selectedTemperature) > thermostat.currentTemperature) setToHeat();
   else if (thePlan.xxNEWtargetTemperature(thermostat.selectedTemperature) < thermostat.currentTemperature) setToCool();
   else setOff();
   ```

5. 메서드를 원래 메서드 이름으로 변경

   ```jsx
   if (thePlan.targetTemperature(thermostat.selectedTemperature) >
   		thermostat.currentTemperature) setToHeat();
   else if (thePlan.targetTemperature(thermostat.selectedTemperature) <
   		thermostat.currentTemperature) setToCool();
   else setOff();

   targetTemperature(selectedTemperature) {
   	if (selectedTemperature > this._max) return this._max;
   	else if (selectedTemperature < this._min) return this._min;
   	else return selectedTemperature;
   }
   ```

### 11.7 세터 제거하기

객체 생성 후에는 수정되지 않길 원하는 필드라면 세터를 제거하여 의도를 명확하게 전달하는 게 좋다.

**절차**

1. 설정해야 할 값을 생성자에서 받지 않는다면 그 값을 받을 매개변수를 생성자에 추가한다. 그런 다음 생성자 안에서 적절한 세터를 호출한다.
2. 생성자 밖에서 세터를 호출하는 곳을 찾아 제거하고, 새로운 생성자를 사용하도록 한다.
3. 세터 메서드를 인라인한다. 가능하다면 필드를 불변으로 만든다.
4. 테스트

### 11.8 생성자를 팩터리 함수로 바꾸기

생성자는 일반 함수에는 없는 제약이 따라붙기도 한다. 가령 생성자는 반드시 생성자를 정의한 클래스의 인스턴스를 반환해야 한다(서브클래스의 인스턴스나 프록시를 반환할 수 없음). 생성자를 호출하려면 특별한 연산자를 사용해야해서 일반 함수가 오길 기대하는 자리에는 사용하기 어렵다. 팩터리 함수는 이러한 제약이 없다.

**절차**

1. 팩터리 함수 생성, 본문에서는 원래의 생성자 호출
2. 생성자 호출 코드를 팩터리 함수 호출로 변경
3. 하나씩 수정할 때마다 테스트
4. 생성자의 가시 범위가 최소가 되도록 제한

### 11.9 함수를 명령으로 바꾸기

함수를 그 함수만의 객체 안으로 캡슐화하면 더 유용해지는 상황이 있다. 이런 객체를 가리켜 **명령 객체** 혹은 단순히 명령이라 한다. 이는 대부분 메서드 하나로 구성되며, 이를 요청해 실행하는 것이 이 객체의 목적이다.

명령은 평범한 함수보다 훨씬 유연하게 함수를 제어하고 표현할 수 있다. 물론 이는 복잡성을 키우기 때문에 일급 함수가 지원된다면 일급 함수를 사용한다. 명령 객체를 사용하는 것은 이보다 더 간단한 방식으로 얻을 수 없는 기능이 필요할 때 뿐이다.

**절차**

1. 대상 함수의 기능을 옮길 빈 클래스를 만든다. 클래스 이름은 함수 이름에 기초해 짓는다.
2. 방금 생성한 빈 클래스로 함수를 옮긴다.
   1. 이 함수를 호출하는 클래스 메서드는 execute(), call() 등 일반적인 이름으로 지정한다.
3. 함수의 인수들 각각은 명령의 필드로 만들어 생성자를 통해 설정할지 고민해본다.

```jsx
function score(candidate, medicalExam, scoringGuide) {
  let result = 0;
  let healthLevel = 0;
  // 기타 코드 생략
}
```

```jsx
class Scorer {
  constructor(candidate, medicalExam, scoringGuide) {
    this._candidate = candidate;
    this._medicalExam = medicalExam;
    this._scoringGuild = scoringGuild;
  }

  execute() {
    this._result = 0;
    this._helthLevel = 0;
    // 코드 생략
  }
}
```

### 11.10 명령을 함수로 바꾸기

명령 객체(명령 class)는 복잡한 연산을 다룰 수 있는 강력한 메커니즘을 제공한다. 그러나 로직이 크게 복잡하지 않다면 단점보다 단점이 클 수도 있다. 이럴 때는 명령(객체)을 다시 일반 함수로 바꾼다.

**절차**

1. 명령 객체를 생성하고 실행 메서드를 호출하는 코드를 함수로 추출
2. 명령의 실행 함수가 호출하는 보조 메서드들 각각을 인라인
   1. 값을 반환하는 메서드라면 반환할 값을 먼저 변수로 추출
3. 함수 선언 바꾸기를 적용해 명령 객체의 생성자 매개변수 모두를 실행 메서드 매개변수로 이동
4. 실행 메서드에서 참조하는 필드들 대신 매개변수를 사용하게끔 변경
5. 생성자 호출과 명령의 실행 메서드 호출을 호출자 안으로 인라인
6. 테스트
7. 죽은 코드 제거하기로 명령 클래서 제거

### 11.11 수정된 값 반환하기

데이터가 어떻게 수정되는지를 추적하는 일은 코드에서 이해하기 가장 어려운 부분 중 하나다. 데이터가 수정된다면 이 사실을 명확히 알려주어서, 어느 함수가 무슨 일을 하는지 쉽게 알 수 있게 하는 일이 대단히 중요하다.

이를 위한 좋은 방법은 변수를 갱신하는 함수가 수정된 값을 반환하여 호출자가 그 값을 변수에 담아두도록 하는 것이다. 이를 통해 호출자 코드를 읽을 때 변수가 갱신될 것임을 분명히 인지하게 된다.

이는 값 여러개를 갱신하는 함수에는 효과적이지 않다.

**절차**

1. 함수가 수정된 값을 반환하게 하여 호출자가 그 값을 자신의 변수에 저장하게 한다.
2. 테스트
3. 피호출 함수 안에 반환할 값을 가리키는 새로운 변수를 선언한다.
4. 테스트
5. 계산이 변수 선언과 동시에 이뤄지도록 통합한다.
6. 테스트
7. 피호출 함수의 변수 이름을 새 역할에 어울리도록 바꿔준다.
8. 테스트

### 11.12 오류 코드를 예외로 바꾸기

예외를 사용하면 오류 코드를 일일이 검사하거나 오류를 식별해 콜스택 위로 던지는 일을 신경쓰지 않아도 된다. 예외에는 독자적인 흐름이 있어서 프로그램 나머지에서는 오류 발생에 따른 복잡한 상황에 대처하는 코드를 작성하거나 읽을 일이 없게 해준다.

예외는 정확히 사용할 때만 최고의 효과가 나온다. 이를 판단하기 위해서 예외를 던지는 코드를 종료 코드로 바꿔도 프로그램이 정상 동작하는지 여부를 따져볼 수 있다. 정상동작하지 않는다면 예외를 사용하는 대신 오류를 검출해서 프로그램을 정상 흐름으로 돌리게끔 처리해야 한다.

**절차**

1. 콜스택 상위에 해당 예외를 처리할 핸들러를 작성한다.
2. 테스트
3. 해당 오류코드를 대체할 예외와 그 밖의 예외를 구분할 식별 방법을 찾는다.
4. 정적 검사
5. catch 절을 수정하여 직접 처리할 수 있는 예외는 적절히 대처하고 그렇지 않은 예외는 다시 던진다.
6. 테스트
7. 오류코드를 반환하는 곳 모두에서 예외를 던지도록 수정한다.
8. 모두 수정했다면 오류 코드를 콜스택 위로 전달하는 코드를 모두 제거한다. 하나 수정할 때마다 테스트

다음과 같은 예시를 가정한다.

```jsx
// 피호출 함수
function localshippingRules(country) {
  const data = countryData.shippingRules[country];
  if (data) return new ShippingRules(data);
  else return -23;
}

// 호출자
function calculateShippingCosts(anOrder) {
  const shippingRules = localShippingRules(anOrder.country);
  if (shippingRules < 0) return shippingRules;
  // 생략
}

// 최상위
const status = calculateShippingCosts(orderData);
if (status < 0) errorList.push({ order: orderData, errorCode: status });
```

`localShippingRules`함수는 배송 규칙들이 countryData에 제대로 반영되어 있다고 가정해도 되는지, country 인수가 전역 데이터에 저장된 키들과 일치하는 곳에서 가져온 것인지, 앞서 검증을 받았는지 등에 대한 답이 긍정적이라면 오류 코드를 예외로 바꾸는 리팩터링을 적용할 준비가 된 것이다.

1. 가장 먼저 최상위 예외 핸들러를 갖춘다. 예외 처리 로직 (errorList.push …)은 포함하고 싶지 않기 때문에 status의 선언과 초기화를 분리해야 한 뒤, try / catch 블록으로 감싼다.

   ```jsx
   // 최상위
   let status;
   try {
     status = calculateShippingCosts(orderData);
   } catch (e) {
     throw e;
   }
   if (status < 0) errorList.push({ order: orderData, errorCode: status });
   ```

   우선 잡은 예외는 모두 다시 던진다. 호출부의 다른 부분에 오류 목록에 대한 처리가 이미 구비되어 있다면 해당 try문을 수정해서 calculateShippingCosts() 호출을 포함시킨다.

2. 다른 예외와 구별할 수 있는 작업 수행 (서브클래스 생성 등) → OrderProcessingError 정의
3. 이제 준비된 예외 클래스를 처리하는 로직을 추가한다.

   ```jsx
   // 최상위
   let status;
   try {
     status = calculateShippingCosts(orderData);
   } catch (e) {
     if (e instanceof OrderProcessingError) errorList.push({ order: orderData, errorCode: e.code });
     else throw e;
   }
   if (status < 0) errorList.push({ order: orderData, errorCode: status });
   ```

4. 피호출 함수가 오류코드 대신 이 예외를 던지도록 한다.

   ```jsx
   function localShippingRules(country) {
     const data = countryData.shippingRules[country];
     if (data) return new ShippingRules(data);
     else throw new OrderProcessingError(-23);
   }
   ```

5. 이제 calculateShippingCosts()에서 오류 코드를 전파하는 부분을 제거할 수 있다.

   ```jsx
   // 호출자
   function calculateShippingCosts(anOrder) {
     const shippingRules = localShippingRules(anOrder.country);
     // 생략
   }
   ```

6. 마지막으로 오류코드에 대한 처리를 하던 부분 및 status 변수도 지운다.

   ```jsx
   // 최상위
   try {
     calculateShippingCosts(orderData);
   } catch (e) {
     if (e instanceof OrderProcessingError) errorList.push({ order: orderData, errorCode: e.code });
     else throw e;
   }
   ```

### 11.13 예외를 사전 확인으로 바꾸기

예외는 뜻밖의 오류라는 말 그대로 예외적으로 동작할 때만 쓰여야 한다. 함수 수행 시 문제가 될 수 있는 조건을 함수 **호출 전에 검사할 수 있다면**, 예외 대신 호출하는 곳에서 조건을 검사하도록 해야한다.

**절차**

1. 예외를 유발하는 상황을 검사할 수 있는 조건문을 추가한다. catch 블록의 코드를 조건문의 조건절 중 하나로 옮기고, 남은 try 블록의 코드를 다른 조건절로 옮긴다.
2. catch 블록에 어서션을 추가하고 테스트한다.
3. try문과 catch 블록을 제거한다.
4. 테스트

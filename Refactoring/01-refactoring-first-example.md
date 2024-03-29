# 리팩터링

### 1.2 예시 프로그램 소감

리팩터링에 본격적으로 들어가기 전, 수백 줄짜리 코드를 수정할 때는 프로그램의 작동 방식을 쉽게 파악할 수 있도록 코드를 여러 함수와 프로그램 요소로 재구성한다. 프로그램이 새로운 기능을 추가하기 편한 구조가 아니라면, 먼저 기능을 추가하기 쉬운 형태로 리팩터링하고 나서 원하는 기능을 추가한다.

리팩터링이 필요한 이유는 변경 때문이다. 잘 작동하고 나중에 변경할 일이 절대 없다면 코드를 현상태로 나둬도 문제가 없다. 하지만 모든 코드는 변경점이 생기기 마련이다. 그리고 이는 항상 원작자가 맡게되지 않는다. 누군가가 코드를 읽고 이해해야 할 때 로직을 파악하기 어렵다면 대책을 마련해야 한다.

### 1.3 테스트

리팩터링의 첫 단계는 테스트 코드를 마련하는 절차이다. 프로그램 규모가 클 수록 수정에서 예상치 못하는 문제가 발생할 수 있기 때문이다. 테스트는 항시 자가진단 테스트로 만든다. (그리고 테스트 최신 테스트 프레임워크는 이를 위한 모든 기능을 지원한다.

### 1.4 함수 쪼개기

긴 함수를 리팩터링할 때는 먼저 전체 동작을 각 부분으로 나눌 지점을 찾는다.

`statement()`에서 switch문은 공연 한 번에 대한 요금을 계산하고 있다. 이는 코드를 분석해서 얻은 정보로써 잊지 않도록 재빨리 코드에 반영할 필요가 있다. 이를 통해 다음번에 코드를 볼 때, 코드를 분석하지 않아도 코드 스스로가 자신이 하는 일이 무엇인지 알려줄 것이다. 여기에서는 코드 조각을 별도 함수로 추출한 후 코드가 하는 일을 설명하는 이름을 지어준다.

**이를 함수 추출하기 절차라고 한다.**

이 과정에서는 유효범위를 벗어나는 변수가 있는지 확인한 후 이에 대해 적절히 처리한다. 여기에서 유효변수에 벗어나는 변수는 perf, play, thisAmount이다.

**perf, play 는 값을 변경하지 않기에 매개변수로 전달한다.**

**thisAmount는 함수 안에서 값이 변경되기에 함수 내에서 초기화 하고 반환 값으로 설정한다.**

```jsx
function amountFor(play, perf) {
    let thisAmount = 0;

    switch (play.type) {
        case "tragedy":
            thisAmount = 40000;
            if (perf.audience > 30) {
                thisAmount += 1000 * (perf.audience - 30);
            }
            break;
        case "comedy":
            thisAmount = 30000;
            if (perf.audience > 20) {
                thisAmount += 10000 + 500 * (perf.audience - 20);
            }
            thisAmount += 300 * perf.audience;
            break;
        default:
            throw new Error(`unknown type: ${play.type}`);
    }

    return thisAmount;
}
```

`statement()`에서는 이제 `ammountFor()`를 통해 thisAmount 값을 채워준다.

아무리 작은 수정이라도 곧바로 컴파일하고 테스트해서 실수가 있는지 판단한다. 피드백 주기를 짧게 가져가는 것은 결과적으로 작업시간을 줄일 수 있도록 돕는다. 리팩터링은 프로그램 수정을 작은 단걔로 나눠 진행함으로써 중간에 실수하더라도 쉽게 버그를 잡도록 돕는다.

함수를 추출하고 난 뒤에는 가장 먼저 변수 이름을 더 명확하게 바꾼다. 여기에서는 thisAmount를 result로 변경한다. 함수의 반환 값에 result라는 이름을 쓴다면 변수의 역할을 쉽게 알 수 있다. 이제 파라미터들도 명확하게 변경한다 perf를 performance로 변경한다.

-   사람이 이해하도록 작성하는 프로그래머가 실력자다
-   좋은 코드는 하는 일이 명확히 드러나야 한다. 이 때 변수는 커다란 역할을 한다. 명확성을 위한 이름 변경을 망설이지 말 것

**다음으로는 play 매개변수를 리팩터링한다.**

1. 임시 변수를 질의 함수로 변경

    `statment()`의 루프에서 수행하는 다음의 코드 `const play = plays[perf.playID];`의 우변을 함수로 추출한다.

    ```jsx
    function playFor(performance) {
        return plays[performance.playID];
    }
    ```

2. 컴파일 - 테스트 - 커밋
3. 변수 인라인으로 play가 사용되는 곳을 함수로 대체
4. 컴파일 - 테스트 - 커밋

이를 통해 매개변수와 불필요한 지역 변수 play를 삭제할 수 있게 됐다.

동일한 원리로 `amountFor()`의 결과 값을 저장하는 `statement()`의 지역 변수 thisAmount도 변수 인라인을 적용해서 삭제한다.

**지역 변수를 제거해서 얻는 가장 큰 장점은 추출 작업이 쉬워진다는 점이다. 유효범위를 신경써야 할 대상이 줄어들기 때문이다.**

**이번에는 volumeCredits 변수를 처리한다.**

```jsx
function volumeCreditsFor(performance) {
    let result = 0;
    result += Math.max(performance.audience - 30, 0);
    if ("comedy" === playFor(aPerformance).type) {
        result += Math.floor(performance.audience / 5);
    }
    return result;
}
```

**format 변수를 제거한다.**

앞에 설명햇듯 임시 변수는 자신이 속한 루틴에서만 의미가 있어서 루틴이 길고 복잡해질 우려가 있다. 이 또한 함수로 변경한다. 다만 format이라는 이름은 함수가 하는 일을 충분히 설명하지 못한다.

**함수 선언 바꾸기**를 통해 이름은 usd로 지정한다.

```jsx
function usd(number) {
    return new Intl.NumberFormat("en-US", {
        style: "currency",
        currency: "USD",
        minimumFractionDigits: 2,
    }).format(number / 100); // USD는 부동소수점 문제를 피하기 위해 센트 단위 정수로 저장한다.
    // 따라서 화면에 노출할 때는 달러 단위로 변환해준다.
}
```

이름 바꾸기는 중요하지만 쉽지 않은 작업이다. 단번에 좋은 이름을 짓기는 쉽지 않으므로 떠오르는 이름을 사용하다가 나중에 좋은 이름이 떠오를 때 변경하도록 한다.

**volumeCredits 변수 제거**

이 변수는 반복문을 돌 때마다 값을 누적하기 때문에 리팩터링하기가 더 까다롭다.

1. **반복문 쪼개기**로 해당 값이 누적되는 부분을 빼낸다.
2. 이후 문장 슬라이드 하기를 통해 volumeCredits 변수를 선언하는 문장을 반복문 바로 앞으로 옮긴다.

```jsx
for (let perf of invoice.performances) {
    // print line for this order
    result += `  ${playFor(perf).name}: ${format(amountFor(perf) / 100)} (${
        perf.audience
    } seats)\n`;
    totalAmount += amountFor(perf);
}

let volumeCredits = 0;
for (let perf of invoice.performances) {
    volumeCredits += volumeCreditsFor(perf);
}
```

이렇게 volumeCredits 값 갱신 관련 문장을 한 군데 모아두면 **임시 변수를 질의 함수로 바꾸기**가 수월해진다.

1. 이제 volumeCredits 값 갱신부분을 질의 함수로 변경한다.
2. 마지막으로 `statement()`의 지역 변수를 방금 만든 함수로 변경한다.

```jsx
function totalVolumeCredit() {
    let volumeCredits = 0;
    for (let perf of invoice.performances) {
        volumeCredits += volumeCreditsFor(perf);
    }

    return volumeCredits;
}
```

반복문 쪼개기는 성능에 큰 대부분 영향을 끼치지 않는다. 다만 항상 그런 것은 아니다. 그럼에도 이런 작업을 해야하는 이유는 잘 다듬어진 코드여야 성능 개선이 수월하기 때문이다. 리팩터링 과정에서 성능이 크게 떨어진다면 시간을 내어 성능을 개선한다. 대체로 리팩터링 덕분에 성능 개선을 효과적으로 수행 가능하다.

또한, 리팩터링은 위와 같이 단계를 나누어 진행할 수록 테스트 실패 시 원인을 찾기가 쉽고, 복잡한 코드일수록 작업 속도를 높여준다.

마지막으로 totalAmount 변수 계산에도 동일한 작업을 진행한다.

### 1.6 계산 단계와 포맷팅 단계 분리

지금까지는 프로그램의 논리 요소를 파악하기 쉽도록 구조 보강에 중점을 뒀다. 이제 본격적으로 기능 변경을 진행한다. `statement` 함수를 HTML 버전으로 만드는 작업을 가정한다.

우리는 HTML 버전의 `statement` 생성 함수가 기존과 동일한 계산 함수들을 사용하게 만들고 싶다. 이를 위해 단계 쪼개기를 수행한다. 첫 번째 단계에서는 `statement`에 필요한 데이터를 처리하고, 다음 단계에서는 결과를 텍스트나 HTML로 표현한다.

이하 1.7까지 코드를 수정하는 부분은 생략한다. (정리를 위한 정리가 될 것 같아서 ...)

코드 총량이 늘더라도 간결함보다는 명료함이 중요하다는 것을 항상 생각하자.

### 1.8 다형성 적용

연극 장르를 추가하고 장르마다 공연료와 적립 포인트를 다르게 지정하도록 하도록 수정해보자. 현재는 `amountFor()`함수에서 연극 장르에 따라 계산 방식이 달라질텐데, 이런 형태의 로직은 코드 수정 횟수가 늘어날수록 골칫거리가 된다. 여기에서는 다형성을 활용해 구조를 보완하도록 한다.

**공연료 계산기 만들기**

먼저 공연료와 적립 포인트 계산 함수를 담을 클래스가 필요하다. 기존에는 `amountFor()`와 `volumeCreditsFor()`에서 타입에 따른 공연료와 적립 포인트를 계산했다. 이제 이 기능들은 전용 클래스로 옮겨서 작업할 수 있도록 전용 클래스 `PerformanceCalculator`를 생성한다.

**함수들을 계산기로 옮기기**

다음으로 공연 정보를 클래스 메서드에서 사용할 수 있도록 생성자로부터 받도록 한다. 그 다음 계산 함수들을 클래스 내부로 복사한다. 문제가 없다면 기존 함수에서 사용하던 계산 함수 호출 부분을 객체 호출 → 메서드 함수 호출로 대체한다.

**공연료 계산기를 다형성 버전으로 만들기**

이제 타입(장르 코드, type)대신 서브 클래스를 사용하도록 변경한다. 팩터리 메서드를 만들고, 필요한 부분에서는 이를 호출해 타입 관련 계산 로직을 갖고있는 서브 클래스 타입 객체를 받아온다.

서브 클래스에서는 슈퍼 클래스의 조건부 로직을 오버라이드 해서 각각에 맞는 로직으로 수정한다. 각 서브 클래스 간 차이가 크지 않거나, 일부 타입에서만 특수한 동작을 하는 경우라면 일반적인 메서드를 슈퍼 클래스에 남겨두고 필요한 곳에서만 오버라이드 하도록 한다.

### 1.9 상태 점검

구조를 보강하며 코드는 늘어났지만, 이제 연극 장르별 계산 코드를 한 곳으로 묶을 수 있게 되었다. 수정 대부분이 이 코드에서만 이뤄질 것 같다면 명확한 분리가 득이된다. 같은 타입의 다형성을 기반으로 실행되는 함수가 많을수록 이런 구성 방식이 유리하다. (ex. 이번 예에서는 타입에 따라 `amountFor()`, `volumeCreditsFor()`이 조건부로 동작한다.)

### 1.10 요약

이번 장에서는

-   함수 추출하기
-   변수 인라인하기
-   함수 옮기기
-   조건부 로직을 다형성으로 바꾸기

등을 사용했다.

-   좋은 코드를 가늠하는 척도는 얼마나 수정하기 쉬운가이다.
-   건강한 코드는 생산성을 극대화하며 필요한 기능을 더 빠르고 저렴하게 제공하도록 한다.
-   리팩터링 단계를 나누는 것을 통해 더 빠르고, 견고하게 큰 변화를 만들 수 있다.

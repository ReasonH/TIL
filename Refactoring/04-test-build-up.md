# 테스트 구축

### 4.1 자가 테스트 코드의 가치

자가 테스트를 만들고 이를 리팩토링 과정에서 짧은 간격으로 수행한다면 버그를 빠르게 검출하는 데 도움을 준다.

테스트가 갖춰지지 않은 코드를 리팩터링할 때는, 먼저 자가 테스트 코드부터 작성한다.

### 4.2 샘플 코드 ~ 4.3 첫 번째 테스트

정리는 생략한다.

### 4.4 테스트 추가

테스트에서는 문제가 의심되는 영역을 집중적으로 테스트한다. 불필요한 부분에 매몰되지 말자.

테스트 코드에서도 중복은 피해야 한다. 하지만, 중복을 제거하기 위해 테스트끼리 상호작용하는 공유 픽스쳐를 생성해서는 안된다. 다른 테스트에서 공유 텍스쳐의 값을 사용한다면 또다른 테스트에 영향을 주게 된다.

따라서 `beforeEach()`등을 통해 테스트의 독립성을 보장한다. 각각의 테스트 내에 `beforeEach()`에 해당하는 작업을 할당할 수 있지만, `beforeEach()`를 사용함으로써 각 테스트가 표준 픽스쳐를 이용한다는 사실을 손쉽게 알 수 있다.

### 4.5 픽스처 수정

복잡한 동작을 하는 setter 작업은 테스트해볼 필요가 있다.

-   되도록이면 테스트 하나에서 여러가지를 검사하지 말자.

### 4.6 경계 조건 검사하기

의도대로 흘러가는 상황의 범위를 벗어나는 경계 지점에 대한 테스트도 함께 작성하면 좋다. 예를 들어 컬렉션을 사용하는 테스트에서 컬렉션이 비어있다면? 연산에 들어가는 숫자가 0이거나 음수인 경우는? 등등

물론 이런 케이스들 자체가 말이 안되는 상황일 수 있지만, 경계를 확인하는 테스트는 프로그램이 이런 특이 상황들을 어떻게 처리하는게 좋을지 생각해볼 수 있게 만든다. 의식적으로 프로그램을 망가뜨리는 방법을 모색함으로써 생산성을 끌어올리는 것이다.

> 어차피 모든 버그를 잡아낼 수 없다고 생각하여 테스트를 작성하지 않는다면 대다수의 버그를 잡을 기회를 날리는 것이다.

테스트 작성은 위험한 부분, 코드 처리 과정이 복잡한 부분에 집중한다. 리팩터링 과정에서 테스트를 더 보충한다.

### 4.7 끝나지 않은 여정

테스트 용이성 자체가 아키텍쳐의 평가 기준이 될 정도로 중요하다.

테스트 스위트가 충분한지를 평가하는 기준은 주관적이다. 가령 ‘누군가 결함을 심으면 테스트가 발견할 수 있다는 믿음'이 기준이 될 수 있다. 자가 테스트 코드는 이 믿음을 갖게 해준다.

> 테스트 코드의 초록불을 보고 리팩터링 과정에서 버그가 하나도 없다고 확신할 수 있다면 충분히 좋은 테스트 스위트라 할 수 있다.

<aside>
💡 테스트가 과한 경우보다 너무 적은 경우가 훨씬 많다. 많은건 걱정하지 말자..

</aside>
### HikariPool Connection 고갈 이슈

기본적인 parallelStream을 사용하던 로직 내부에 log 추가가 필요한 상황이 생겼다. Spring sleuth의 traceId가 parallelStream에 전파되지 않았기 때문에 trace 전파가 가능하도록 Custom한 thread pool (이하 `traceableThreadPool`을 정의하고, CompletableFuture에 pool을 적용해 사용하기로 했다.

작업은 다음과 같이 진행했다.
1. thread pool의 크기는 기존 parallelStream을 사용할 때와 동일하게 생성 (=ForkJoinPool.commonPool()의 사이즈)
2. parallelStream를 `traceableThreadPool`이 적용된 CompletableFuture로 변경
3. 잠재적으로 로깅 추가 가능성이 있는 CompletableFuture에 `traceableThreadPool`을 적용
4. 적용된 부분들에 대해 기능 / 로깅 테스트 진행 및 통과 확인 후 배포

#### 이슈
개발망에서도 정상 동작하는 것을 확인한 후, Release 망에 배포되면서 이슈가 발생했다. 간헐적으로 모든 API가 먹통이 되었고, `Connection is not available, request timed out after` 로그가 발생했다.

#### 원인 파악

1. Grafana 지표를 살펴봤을 때, Hikari slave pool이 가득 차서 줄어들지 않았다.
2. slave connection 사용치가 올라가는 시점을 기준으로 호출된 API들을 파악했으나, 모두 다른 API였다.
3. 누수 일어나는 정확한 지점 파악 위해 `leak-detection-threshold` 옵션 추가 후 재배포했다.
4. 로그 트래킹 결과 Connection을 물고 있는 서비스들이 공통적으로 A라는 서비스를 호출하는 것을 확인했다.
5. A는 내부적으로 `traceableThreadPool`이 적용된 CompletableFuture를 사용하고 있었다.

여기에서 원인이 내가 했던 작업 때문이었음을 깨달았다. 문제는 2가지 였는데

1. 기존부터 중첩된 비동기 호출이 있었다. 스레드 안에서 스레드를 호출하는 로직이 있었다.
2. 이는 서로다른 thread pool을 사용하고 있었으나, 내가 작업하며 동일한 thread pool을 적용했다.

이 상태에서는 thread마다 새로운 트랜잭션이 열리면서 Connection을 소모하게 되고, Thread에 deadlock이 걸리면서 Connection을 지속적으로 점유하게 된 것이다.

> 결국 과거의 잘못된 코드 + 현재의 잘못된 작업으로 이슈가 발생했다.

#### 해결 방안
중첩 비동기 호출 제거, 추후 메트릭 추이 보며 pool size 재설정 검토
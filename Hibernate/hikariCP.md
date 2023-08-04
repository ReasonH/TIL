모호하게 사용하고 있던 Hikari Connection Pool 주요 설정에 대해 정리해본다.

### maxLifeTime

커넥션이 풀에서 최대 생존할 수 있는 시간
-   DBMS의 wait_timeout보다 짧게 설정해야, 불필요한 오버헤드를 줄일 수 있다.
-   HikariCP 내부 스케쥴러에 의해 설정된 시간마다 CP 내의 connection이 제거된다.
    -   사용중인 connection은 반환 이후 제거된다.

### idleTimeout

idle 상태의 커넥션을 유지하는 시간, 초과하는 경우 커넥션을 제거한다.
-   minimumIdle 설정값이 maximumPoolSize보다 작을때만 유효함

### connectionTimeout

CP에서 커넥션을 가져올 때 걸리는 최대 시간이다.
-   잘못된 설정으로 가장 장애가 발생하기 쉬운 지점
-   대부분의 경우 설정보다는 타임아웃이 발생하는 원인을 해결해야한다.

### validationTimeout

커넥션의 유효성을 검사하는 데 걸리는 최대 시간이다.
connectionTimeout보다 반드시 작아야 한다. 만약 connectionTimeout보다 긴 경우 validation 실패 이후 connection 획득 실패로까지 이어질 수 있다. 반대로, validationTimeout이 connectionTimout보다 짧은 경우, timeout 도달 시 검증을 취소하고, 즉시 새로운 connection 획득을 시도한다.

### keepaliveTime

idle 상태의 커넥션 유효성을 검증하는 빈도를 조절한다. DB또는 network인프라에 의한 타임아웃을 방지한다.

#### 번외, 추천설정 leakDetectionThreshold

커넥션 leak을 체크하기 위한 스레시홀드를 설정한다. 설정한 시간을 초과할 경우 누수 감지 메시지가 기록된다. 기본값은 0인데, 웬만하면 해당 값을 활성화 해두는 것을 추천한다. 이를 설정하지 않아 문제 원인 파악이 오래걸린 경험이 있다...


참고
- https://github.com/brettwooldridge/HikariCP
- [https://perfectacle.github.io/2022/09/25/hikari-cp-time-config/#more](https://perfectacle.github.io/2022/09/25/hikari-cp-time-config/#more)
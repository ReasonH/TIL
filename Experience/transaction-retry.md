### Transaction 예외 Retry 적용 관련 문제

#### 이슈
`ObjectOptimisticLockingFailureException`  예외 발생 시, 재시도 처리를 위한 `@Retryable`이 동작하지 않는 현상을 파악

#### 원인
코드를 살펴보면 외부 트랜잭션 메서드 A가 다른 트랜잭션 메서드 B를 호출하는 구조였다. B는 `@Retryable`이 적용되어 있었으며 전파레벨로 `REQUIRED`를 사용 중이었다.
`ObjectOptimisticLockingFailureException` 은 트랜잭션의 커시 시점 (메서드 A 완료 시점)에 발생하는 예외이기 때문에 B에 적용한 `@Retryable`에서 캐치되지 않았던 것이다.

#### 해결 방안
1.  외부 트랜잭션에 `@Retryable` 설정 → 커밋 시점에 해당 예외를 잡아서 재시도 처리 가능
2.  내부 트랜잭션을 `REQUIRES_NEW`로 설정 → 독립적으로 커밋하기에 예외 발생 시 즉시 재시도
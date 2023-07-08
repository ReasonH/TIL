# RedisTemplate과 Transaction 통합

웹 서비스에서 캐시 시스템을 적용하는 이유는 다양하지만, 아마 가장 큰 이유는 DB통신 비용을 줄이기 위함일 것이다. 우리 팀이 Redis를 사용하는 주 목적 또한 쿼리 발생을 줄이는 것이었다. 그리고 이런 용도로 캐시를 사용할 때 신경써야할 문제가 바로 캐시와 DB의 상태를 동기화하는 부분이다.

일시적으로 상태 불일치를 허용하는 시스템도 있겠지만, 우리의 경우는 달랐다. 메시징 시스템의 특성 상 메시지가 생성될 때마다 수신 대상을 조회해야 했고, 이 부하를 줄이기 위해 캐시를 통하도록 했기 때문이다. 캐시 정보가 잘못 업데이트된 경우 잘못된 대상에게 메시지가 전송되거나, 정상적인 대상이 메시지를 수신할 수 없는 문제가 발생할 수 있기에, 수신 대상 목록이 DB와 정확하게 싱크되는 것이 무엇보다 중요했다.

## 문제 상황

문제 상황은 캐시가 업데이트된 이후 트랜잭션이 실패했을 때 발생했다.

**예시. 유저 채팅방 입장**
1. 채팅방 수신자로 유저 등록하는 비즈니스 로직 (Transaction + 캐시 Put) 수행
2. 트랜잭션 실패 -> 롤백
3. 유저는 방 입장에 실패했으나, 캐시는 등록됨

**예시. 유저 채팅방 탈퇴**
1. 채팅방 수신자에서 유저 삭제하는 비즈니스 로직 (Transaction + 캐시 Evict) 수행
2. 트랜잭션 실패 -> 롤백
3. 유저는 방 퇴장에 실패했으나, 캐시는 삭제됨

결국 트랜잭션 실패 시, 캐시에 대한 롤백이 이루어질 수 없기 때문에 DB 트랜잭션과 캐시 로직의 원자성을 확보할 방법을 찾아야 했다.

## 트랜잭션 통합 방법

### transactionAware

Spring과 Redis를 함께 사용할 때, 아마 우선적으로 spring-boot-starter-data-redis 의존성을 추가하고 사용할 것이다. 이는 Spring의 Cache 추상화와 Redis를 결합하여 기존 코드와 Redis를 손쉽게 결합할 수 있도록 돕는다. 대표적으로 사용하는 `@Cacheable` / `@CachePut` / `@CacheEvict` 등의 Annotation이 예시이다. 이를 사용하고 있다면 `transactionAware()` 옵션을 통해 간단하게 **커밋 시, 캐시 작업 수행**을 보장할 수 있다.

#### 한계

그러나 Annotation 기반의 캐시 관리는 아주 단순한 put / get / evict 정도만 처리할 수 있다. 만약 Redis Hash + redisTemplate 등으로 특정 자료구조와 커맨드를 이용하는 경우 추상화 적용이 불가능하기 때문에 이 방식은 사용할 수 없었다.

### setEnableTransactionSupport

상기 옵션은 redisTemplate에서 지원하는 Transaction Support옵션이다. 이 옵션을 사용하는 경우 redisTemplate을 이용하는 캐시 작업은 Spring의 @Transactional과 자연스럽게 통합된다.
> 내부적으로 정의된 PlatformTransactionManager Bean이 없다면 추가가 필요하다.

```java
@Bean  
public RedisTemplate<String, Object> redisTemplateTransactional(RedisConnectionFactory redisConnectionFactory) {  
    RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();  
    redisTemplate.setConnectionFactory(redisConnectionFactory);  
    redisTemplate.setKeySerializer(new StringRedisSerializer());  
    redisTemplate.setEnableTransactionSupport(true);  // 트랜잭션 통합 활성화
    return redisTemplate;  
}
```

이를 이용하면 @Transactional 메서드 내의 모든 redisTemplate 조작을 redis 트랜잭션(MULTI / EXEC)으로 묶게 된다. 서비스 내의 redisTemplate 갱신 작업은 Queue에 적재되며 서비스 메서드에서 예외 발생 시 버려진다. 반대로, DB Transaction이 정상적으로 수행되고, Commit된다면 미리 Queue된 커맨드들이 캐시 수정 작업을 전송한다.

### 한계

자 이제 설정으로 아주 간단하게 DB 트랜잭션과 레디스 작업을 원자적으로 묶을 수 있게 됐다... 라고 생각했으나 이 또한 다음의 문제가 있었다.

문제점
1. 일부 키 삽입 로직의 경우 트랜잭션 커밋 이전에 예외적으로 즉시 반영이 되어야 하는 경우가 있었다. 일괄적으로 옵션을 적용하는 경우 이를 제어할 수 없다는 문제가 있다.
2. Lettuce의 경우 CLUSTER MODE에서 MULTI 커맨드를 사용하는 경우 에러가 발생한다. (해당 이슈는 Redisson을 사용하는 경우 발생하지 않는다.)

### TransactionSupport를 위한 다른 방법은 없을까?

결국 `CacheManager`의 `transactionAware` 옵션과 `RedisTemplate`의 `setEnableTransactionSupport` 모두 현재 문제를 해결하기에 적절하지 않았다. 아쉽지만, 다른 방법을 찾아야 했다.

먼저 위 옵션들을 적용하며 파악한 한계점을 토대로 캐시 로직에 필요한 요건을 정의했다.

1. 필요한 캐시 로직에만 선택적으로 옵션을 적용할 수 있어야 한다.
2. @Transactional로 묶인 서비스가 커밋된 이후 실행될 수 있도록 한다.
3. 메서드에 @Transactional이 없는 경우 즉시 실행되어야 한다.
4. 쉽게, 범용적으로 현재 코드를 최대한 변경하지 않으면서 적용될 수 있어야 한다.

## TransactionTemplate을 이용한 문제 해결

1/2/3번 고민을 한 번에 해결할 수 있는 방법이 있었는데, 이는 `TransactionTemplate`을 이용하는 방법이었다. 해당 클래스를 사용하는 경우 현재 실행되고 있는 트랜잭션의 커밋 시점에 특정 코드가 수행되도록 등록하는데, 이는 지금 상황에 알맞은 기능이었다.

> 번외로, 해당 클래스는 위에서 언급했던 transactionAware()옵션 활성화 시 사용되는 클래스였다. 해당 옵션이 켜져 있는 경우 Annotation 기반의 캐시로직에 자동으로 Decorator가 감싸지는데, 이 Decorator는 내부적으로 TransactionTemplate을 이용해 캐시 삽입/삭제 동작을 커밋 시점 이후로 조정한다.

사용법은 아래와 같다.
```java
transactionTemplate.execute(new TransactionCallbackWithoutResult() {
	@Override
	protected void doInTransactionWithoutResult(TransactionStatus status) {
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				try {
					// 실제 동작시킬 캐시 코드
				} catch (Throwable throwable) {
					log.error("After commit process error: {}", throwable.getMessage());
				}
			}
		});
	}
});
```
`afterCommit()` 메서드 내부에서 실행되는 로직은 Transaction의 커밋 이후에 수행된다. 이는 필요한 곳에 선택적으로 적용이 가능했으며 실제 테스트 결과 의도대로 동작하는 것을 확인했다.

### 기존 코드에 적용

기존의 코드에 TransactionTemplate을 적용하려하니 상당히 많은 수정이 필요했다. 단순히 생각했을 때 캐시 로직을 호출하는 곳 또는 캐시 로직 내부의 구현을 모두 바꿔야 했는데 이는 실수할 가능성이 크고, 너무 많은 부수 코드가 생긴다는 문제점도 있었다.

#### AOP 적용
이에 대한 대안으로 생각한 것은 AOP 적용이었다.
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RunAfterCommit {
}
```

```java
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class RunAfterCommitAspect {

    private final TransactionTemplate transactionTemplate;

    @Around("@annotation(com.apiserver.aop.annotation.RunAfterCommit)")
    public void registerRunnable(final ProceedingJoinPoint joinPoint) throws Throwable {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            joinPoint.proceed();
                        } catch (Throwable throwable) {
                            log.error("After commit process error: {}", throwable.getMessage());
                        }
                    }
                });
            }
        });
    }
}
```

위와같이 Annotation을 기반으로 Aspect 내에서 afterCommit 처리하도록 만들었다. 이를 이용해 기존 코드의 수정 없이, 메서드에 @RunAfterCommit을 붙이는 것 만으로도 트랜잭션 커밋 이후 실행됨을 보장할 수 있게 됐다.


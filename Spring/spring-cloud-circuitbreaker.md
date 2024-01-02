# CircuitBreaker

## 1.1 R4J Circuit Breaker 설정

- `org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j`로 적용
- 기본적으로 Autoconfiguration 동작, 비활성화 가능

### 1.1.3 기본 설정

`Resilience4JCircuitBreakerFactory`를 반환하는 Customizer bean 정의하고, `configureDefault`를 사용함으로써 애플리케이션 내 모든 서킷 브레이커에 적용되는 기본 설정을 제공할 수 있다.

```java
@Bean
public Customizer<Resilience4JCircuitBreakerFactory> defaultCustomizer() {
    return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
            .timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(4)).build())
            .circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
            .build());
}
```

#### ExecutorService 커스터마이징

서킷브레이커를 실행하기 위한 ExecutorService를 설정할 수도 있다.

```java
@Bean
public Customizer<Resilience4JCircuitBreakerFactory> defaultCustomizer() {
    return factory -> {
        ContextAwareScheduledThreadPoolExecutor executor = ContextAwareScheduledThreadPoolExecutor.newScheduledThreadPool().corePoolSize(5)
            .build();
        factory.configureExecutorService(executor);
    };
}
```

### 1.1.4 세부 설정
생략 (하단 링크 참조)

### 1.1.5 Circuit Breaker 속성 설정

properties file을 통해 CircuitBreaker와 TimeLimiter를 설정할 수 있다. 이 설정은 Java로 정의한 Customizer보다 우선 순위를 갖는다.

설정의 우선순위는 다음과 같다.

- Method (id)
- Service(group)
- Global default config

다음은 각 설정의 예시이다.

### **Global Default Properties Configuration**

```java
resilience4j.circuitbreaker:
    configs:
        default:
            registerHealthIndicator: true
            slidingWindowSize: 50
```

### **Configs Properties Configuration**

```java
resilience4j.circuitbreaker:
    configs:
        groupA:
            registerHealthIndicator: true
            slidingWindowSize: 200
```

### **Instances Properties Configuration**

```java
resilience4j.circuitbreaker:
 instances:
     backendA:
         registerHealthIndicator: true
         slidingWindowSize: 100
     backendB:
         registerHealthIndicator: true
         slidingWindowSize: 10
         permittedNumberOfCallsInHalfOpenState: 3
         slidingWindowType: TIME_BASED
         recordFailurePredicate: io.github.robwin.exception.RecordFailurePredicate
```

위의 예시로 다음의 Circuit Breaker를 만든다면?

`Resilience4JCircuitBreakerFactory.create("backendA")`

- instances backendA 속성이 적용된다.

`Resilience4JCircuitBreakerFactory.create("backendA", “groupA”)`

- instances backendA 속성이 적용된다.
- configs groupA 속성이 적용된다.

**configs default 설정은 항시 적용된다.**

### 1.1.6 Bulkhead 패턴 지원

`resilience4j-bulkhead`의존성이 classpath에 존재하는 경우, 서킷 브레이커는 모든 메서드를 bulkhead로 감싼다. 이 또한 설정으로 비활성화가 가능하다.

Spring Cloud CircuitBreaker Resilience4j는 2개의 bulkhead 구현을 제공한다.

- `SemaphoreBulkhead`: Semaphore 사용
- `FixedThreadPoolBulkhead`: bounded queue 및 고정 스레드 풀 사용

기본적으로, SCCR은 `FixedThreadPoolBulkhead`를 사용한다. 만약 기본으로 `SemaphoreBulkhead`를 사용하기 위해서는 `**spring**.cloud.circuitbreaker.resilience4j.enableSemaphoreDefaultBulkhead` 를 `true`로 설정하면 된다.

`Customizer<Resilience4jBulkheadProvider>`로 기본 `Bulkhead`와 `ThreadPoolBulkhead` 설정을 제공할 수 있다.

```java
@Bean
public Customizer<Resilience4jBulkheadProvider> defaultBulkheadCustomizer() {
    return provider -> provider.configureDefault(id -> new Resilience4jBulkheadConfigurationBuilder()
        .bulkheadConfig(BulkheadConfig.custom().maxConcurrentCalls(4).build())
        .threadPoolBulkheadConfig(ThreadPoolBulkheadConfig.custom().coreThreadPoolSize(1).maxThreadPoolSize(1).build())
        .build());
}
```

### 1.1.7 상세 설정

> 서킷, 벌크헤드 둘 다 상세 설정은 유사하다. 또한, 실제 사용 시점에 호출되는 이벤트 리스너 등록도 둘 다 가능하다.

### 1.1.8 Bulkhead 속성 설정

circuit breaker와 유사하게 bulkhead도 properties 파일을 통해 설정이 가능하다.

```yaml
resilience4j.thread-pool-bulkhead:
    instances:
        backendA:
            maxThreadPoolSize: 1
            coreThreadPoolSize: 1
resilience4j.bulkhead:
    instances:
        backendB:
            maxConcurrentCalls: 10
```

## 이벤트 리스너 설정

`RegistryEventConsumer`를 사용해서 TimeLimiter / CircuitBreaker / Retry 등에 대한 이벤트를 처리할 수 있다. Spring에서 사용 시, Bean 선언을 통해 서킷 브레이커 관련 인스턴스 생성 시 마다 컨슈머를 추가할 수 있다.

---
참고
- Docs: [https://docs.spring.io/spring-cloud-circuitbreaker/docs/current/reference/html/#usage-documentation](https://docs.spring.io/spring-cloud-circuitbreaker/docs/current/reference/html/#usage-documentation)
- 설정 참조: [https://resilience4j.readme.io/docs/getting-started-3#configuration](https://resilience4j.readme.io/docs/getting-started-3#configuration)
- [https://clack2933.tistory.com/48](https://clack2933.tistory.com/48)
- [https://sabarada.tistory.com/206](https://sabarada.tistory.com/206)

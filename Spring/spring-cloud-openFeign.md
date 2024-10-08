시스템 아키텍쳐가 MSA로 변해가면서 생긴 트레이드 오프 중 하나는 API 호출의 증가이다. 분산된 시스템에서는 서비스간 API를 호출하는 코드를 반복적으로 만들어야하는 번거로움이 생겼다. Feign은 이런 문제점을 해결해줄 수 있는 프로젝트이다. 이는 RestTemplate 호출 등을 JPA Repository처럼 단순 interface로 추상화한 프로젝트이다. 간단한 사용법은 예제와 함께 빠르게 살펴본다.

## Getting Started

### 1. 의존성

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
    <version>{version}</version>
</dependency>
```

### 2. MainApplication

```java
@EnableFeignClients // Feign Client를 사용할 것임을 알려준다.
@SpringBootApplication
public class FeignTestApplication {
	public static void main(String[]~ args) {
		SpringApplication.run(FeignTestApplication.class, args);
	}
}
```

- base package에서 FeignClient를 사용할것임을 명시한다.

### 3. Client

```java
@FeignClient(name="feign", url="http://localhost:8080")
public interface TestClient {

    @GetMapping("/testfeign")
    String testFeign(@RequestHeader, @RequestParam, @RequestBody 등 사용 가능);
}
```

1. 인터페이스에 어노테이션을 적용함으로써 FeignClient로 만든다.
2. 메소드 위의 annotation을 통해 요청 url path를 넣어준다.

@FeignClient 속성 설명

- `name`: feignclient의 서비스 이름으로 필수 속성이다. 서비스 탐색 시 필요
- `url`: 요청의 base url
- `qualifier` : beanName
- `configuration`: 커스터마이징한 configuration을 넣을 수 있음
fallback 관련 옵션은 아래에서 다룬다.

### 4. Service / Controller

```java
@Service
public class TestService {
  	//@Autowired를 통해 방금 작성한 client 의존성 주입
    @Autowired
    TestClient testClient;

  	// client의 기능을 사용할 메소드 testFeign 작성
    public String testFeign() {
        return testClient.testFeign();
    }
}
```

```java
@RestController
@RequiredArgsConstructor
public class TestController {

    private final TestService testService;

    // 1) 메인 페이지로 접근
    // 2) Feign Client가 /testfeign 으로 get 호출
    // 3) 반환값 받고 메인에서 출력
    @GetMapping("/")
    public String main() {
        return testService.testFeign();
    }

    // Feign Client 요청에 응답을 주기 위한 컨트롤러
    @GetMapping("/testfeign")
    public String testFeign() {
        return "Hello Feign Cleint~ 찡긋";
    }
}
```

MSA 테스트를 위해 서버가 2개여야하지만, 간단한 테스트를 위해 1개의 서버 내에서 2개의 컨트롤러로 테스트한다. /로 요청 시 FeignClient를 통해 /testfeign으로 요청이 전달된다.

## Basic Configuration

FeignClient는 기본적으로 제공하는 Configuration이 있기에 별도 설정 없이도 사용이 가능하다. 제공되는 Configuration은 FeignClientsConfiguration.class이다. 내부적으로 설정되어 있는 Bean들을 살펴보면 `@ConditionalOnMissingBean`이 붙어있다. 이는 해당 Bean은 해당 옵션으로 사용하는 별도 Bean이 없을 때 적용되는 Default라는 의미이다. 별도 Bean이 있다면 Override된다.

#### 설정 관련 Bean (일부 정리)

**`Decoder feignDecoder`**
Feign호출 이후 http 응답에 대한 디코딩 처리 설정

**`Encoder feignEncoder`**
Feign호출에서 인코딩 처리 설정, 기본적으로 SpringEncoder로 인코딩한다.

**`Logger feignLogger`**
```java
@Autowired(required = false)
private Logger logger;

@Bean
@ConditionalOnMissingBean(FeignLoggerFactory.class)
public FeignLoggerFactory feignLoggerFactory() {
    return new DefaultFeignLoggerFactory(this.logger);
}
```

기본적으로 통신에 대한 로깅은 NONE으로 잡혀있기 때문에 Logger.Level.FULL로 설정해도 로깅이 되지 않는다. 실제 로깅을 위해서는 application.yml에서 FeignClient가 있는 패키지의 서버 로깅 레벨을 DEBUG로 변경해야한다.

실제 환경에서 Feign을 사용하기 위해서는 커스터마이징이 필수적이다. ex) full로그가 필요, Hystrix나 Retryer과 함께 제공되는 기능이 필요 등

## Spring Cloud CircuitBreaker와의 통합

Spring cloud openfeign은 circuit breaker에 대한 통합도 지원한다. 만약 Spring Cloud CircuitBreadker가 classpath에 존재하고, `spring.cloud.openfeign.circuitbreaker.enabled=true`인 경우 Feign은 모든 메서드를 circuit breaker로 wrapping한다. 각 Feign client 기반의 서킷 지원을 비활성화하려면 Feign.Builder를 prototype scope Bean으로 생성하면 된다.
> 구버전의 경우 서킷 활성화를 위해 `feign.circuitbreaker.enabled=true` 설정을 사용해야 한다.

이 때 생성된 서킷 브레이커의 네이밍 패턴은 `<feignClientName>_<calledMethod>` 형식이다.

아래의 Resolver를 제공함으로써 네이밍 패턴을 변경할 수 있다.
```java
@Configuration
public class FooConfiguration {
    @Bean
    public CircuitBreakerNameResolver circuitBreakerNameResolver() {
        return (String feignClientName, Target<?> target, Method method) -> feignClientName + "_" + method.getName();
    }
}
```

### Configuration Properties를 활용한 CircuitBreaker 설정

서킷은 아래와 같이 properties 파일을 이용해서 설정 가능하다.

```yaml
spring:
  cloud:
    openfeign:
      circuitbreaker:
        enabled: true
        alphanumeric-ids:
          enabled: true
resilience4j:
  circuitbreaker:
    instances:
      DemoClientgetDemo:
        minimumNumberOfCalls: 69
  timelimiter:
    instances:
      DemoClientgetDemo:
        timeoutDuration: 10s
```

### Feign Spring cloud CircuitBreaker의 Fallbacks

FeignClient와 Circuit Breaker를 통합하여 fallback 패턴을 사용할 수 있다. fallback은 다음과 같이 사용 가능하며 서킷이 열리는 경우 외부 서버에 대한 응답을 대체한다.

기본적으로 아래와 같이 `fallbakck`을 사용해서 설정 가능하다.

```java
@FeignClient(name = "example-service", fallback = ExampleServiceFallback.class)
public interface ExampleService {
    @GetMapping("/someEndpoint")
    String getSomeData();
}

public class ExampleServiceFallback implements ExampleService {
    @Override
    public String getSomeData() {
        return "Fallback Data";
    }
}
```

만약 fallback을 트리거한 throwable에 접근하려면 다음과 같이 `fallbackFactory`를 사용할 수 있다. 더 구체적인 예외 원인을 파악하고 알맞은 처리를 하기 위해서는 factory를 사용하는 것이 권장된다.

```java
@FeignClient(name = "example-service", fallbackFactory = ExampleServiceFallbackFactory.class)
public interface ExampleService {
    @GetMapping("/someEndpoint")
    String getSomeData();
}

@Component
public class ExampleServiceFallbackFactory implements FallbackFactory<ExampleService> {
    @Override
    public ExampleService create(Throwable cause) {
        return new ExampleService() {
            @Override
            public String getSomeData() {
                if (cause instanceof FeignException) {
                    // Customize the fallback logic based on the specific cause
                    // For example, you can inspect the FeignException to determine the error.
                }
                return "Fallback Data";
            }
        };
    }
}
```
---

참고
- https://sabarada.tistory.com/115?category=822738
- https://resilience4j.readme.io/docs/feign
- https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/#spring-cloud-feign-circuitbreaker

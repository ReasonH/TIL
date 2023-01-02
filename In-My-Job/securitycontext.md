# SecurityContext 이슈와 회고

Spring Security는 Spring 기반의 서버 애플리케이션에서 인증, 인가를 처리해주는 프레임워크다. Spring을 API 백엔드 개발 용도로 사용한다면 대부분의 경우 적용하게 되는 프레임워크이다. 물론, 우리 파트에서 담당하는 API 서버도 이를 사용하고 있었다. 오늘은 Spring Security의 SecurityContextHolder를 사용하며 겪은 이슈를 회고해보려고 한다.

## SecurityContextHolder와 기본 전략

SecurityContextHolder는 Spring Security에서 가장 핵심적인 객체라고 할 수 있다. 이는 사용자의 요청을 바탕으로 보안 처리를 위한 정보를 구성한다.

SecurityContextHolder는 어디서든 참조할 수 있지만 실제 SecurityContext를 참조할 때는 공유 레벨에 대한 이해가 필요하다. 공유 레벨에 관해서는 다양한 자료가 있으니 여기에서는 간단하게만 살펴보고 넘어간다.

- MODE_THREADLOCAL: ThreadLocal을 사용한 Context 공유, 동일 스레드 내에서만 공유 가능
- MODE_INHERITABLETHREADLOCAL: 자식 스레드까지 Context 공유 가능
- MODE_GLOBAL: JVM 내 인스턴스 모두 (애플리케이션 범위)에서 공유 가능

Spring Security에서는 기본적으로 MODE_THREADLOCAL을 사용하기 때문에 별도의 스레드에서 SecurityContext를 참조할 수 없는 문제가 있다.

### 비동기 공유 전략

#### MODE_INHERITABLETHREADLOCAL
그렇다면 비동기 처리를 위해서는 MODE_INHERITABLETHREADLOCAL을 사용하면 될까? 아쉽게도 이는 만능 해결책 이아니다

대부분의 Java Application이 그렇듯이 우리 또한 TaskExecutor Bean을 만들어서 사용하고 있었다. 이는 스레드 생성으로 발생하는 부하를 줄이기 위해 스레드를 **재사용** 하도록 만든다. 이런 Pool 기반의 Thread를 사용할 때 MODE_INHERITABLETHREADLOCAL을 사용한다면 잘못된 SecurityContext가 채워진 Thread를 다른 유저가 그대로 재사용할 수 있는 것이다. 이 문제는 Spring Security의 [이슈](https://github.com/spring-projects/spring-security/issues/6856#issuecomment-493585232)로도 등록되어 있다.

### Spring Concurrency Support

팀에서 Security 전파를 위해 MODE_INHERITABLETHREADLOCAL대신 사용하는 것은 바로 `DelegatingSecurityContextRunnable`이다.

Spring Security는 `DelegatingSecurityContextRunnable`, `DelegatingSecurityContextExecutor`등과 같이 멀티 스레드 환경에서 Security를 이용하기 위한 추상화 또한 제공하고 있다. 이런 추상화 방식은 스레드 호출과 반환 시점에 SecurityContext를 설정, 해제 처리해준다. 이를 통해 Thread pool을 사용하는 환경에서도 신뢰할 수 있는 SecurityContext 전파가 이루어지게 돕는다.

다음은 `DelegatingSecurityContextRunnable` 코드의 일부와 이를 적용한 TaskExecutor Bean 정의 코드 예시이다.

##### `DelegatingSecurityContextRunnable`
```java
public DelegatingSecurityContextRunnable(Runnable delegate, SecurityContext securityContext) {  
   Assert.notNull(delegate, "delegate cannot be null");  
   Assert.notNull(securityContext, "securityContext cannot be null");  
   this.delegate = delegate;  
   this.delegateSecurityContext = securityContext;  
}  
  
/**  
 * Creates a new {@link DelegatingSecurityContextRunnable} with the  
 * {@link SecurityContext} from the {@link SecurityContextHolder}.  
 * @param delegate the delegate {@link Runnable} to run under the current  
 * {@link SecurityContext}. Cannot be null.  
 */public DelegatingSecurityContextRunnable(Runnable delegate) {  
   this(delegate, SecurityContextHolder.getContext());  
}  
  
@Override  
public void run() {  
   this.originalSecurityContext = SecurityContextHolder.getContext();  
   try {  
      SecurityContextHolder.setContext(this.delegateSecurityContext);  
      this.delegate.run();  
   }  
   finally {  
      SecurityContext emptyContext = SecurityContextHolder.createEmptyContext();  
      if (emptyContext.equals(this.originalSecurityContext)) {  
         SecurityContextHolder.clearContext();  
      }  
      else {  
         SecurityContextHolder.setContext(this.originalSecurityContext);  
      }  
      this.originalSecurityContext = null;  
   }  
}
```
DelegatingSecurityContextRunnable은 Runnable과 SecurityContext를 받아 생성되며 Runnable 실행 전-후로 SecurityContextHolder를 불러와 Context를 설정-해제해준다.

##### `dataSyncThreadPool` 정의
```java
public TaskExecutor dataSyncThreadPool() {  
    ThreadPoolTaskExecutor taskExecutor = buildThreadPoolTaskExecutor(5, "ds-pool");  
    taskExecutor.setTaskDecorator(runnable -> new DelegatingSecurityContextRunnable(runnable, SecurityContextHolder.getContext()));  
  
    return taskExecutor;  
}  
  
private ThreadPoolTaskExecutor buildThreadPoolTaskExecutor(int coreSize, String threadNamePrefix) {  
    ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();  
    taskExecutor.setCorePoolSize(coreSize);  
    taskExecutor.setMaxPoolSize(coreSize);  
    taskExecutor.setThreadNamePrefix(threadNamePrefix);  
    return taskExecutor;  
}
```
위와 같은 Runnable 객체는 TaskExecutor 정의 시점에 Decorator로 적용하는 방식으로 사용이 가능하다.


## 이슈

앞에서는 SecurityContext 전파에 현재 팀에서 사용하는 방식을 적어봤다. 이미 내가 입사했을 때 이런 코드 형태를 사용하고 있었고, 꽤 긴 시간(?) 문제없이 운영해왔던 코드이다. 그러던 어느 날 유저로부터 자신의 캐릭터 정보가 보이지 않는다는 문의가 접수되었다. 이는 유저의 데이터 보정 로직이 잘못 동작한 상황이었다.

특정 API를 호출했을 때, 서버에서는 현재 유저 정보를 외부 서버 데이터와 동기화 시킨다. 이 흐름은 다음과 같다.
1. 유저의 API 호출
2. 서비스 내에서 @Async한 데이터 보정 함수 `void dataSync()` 호출
	- @Async는 Delegating 설정된 Thread Pool을 사용
	- 전파받은 SecurityContext를 내부에서 참조해 유저 데이터를 외부와 동기화
4. API 응답

문제가 생겼던 것은 2번의 `dataSync()` 로직 내부였다. `dataSync()`를 호출한 상위 스레드에서와 전혀 다른 SecurityContext를 불러와서 사용하고 있었다. 그리고 이 SecurityContext는 동시간대(0.1초 이내)에 같은 API를 호출했던 **다른 유저**의 SecurityContext였다.

### 원인 파악
의심가는 정황은 다음 두 개였다.

##### 1. 외부 서버 이슈
이슈가 발생한 시간대에는 외부 서버의 이슈가 발생해서, 데이터 동기화 처리에 상당한 시간이 소요됐다. 
- 그만큼 스레드를 더 오래 붙잡고 있었다.

##### 2. 모두 MessageBroker로부터 도착한 요청
서버 구조 상 동일한 API라도 요청은 2가지 경로로 올 수 있었다. 첫 번째는 일반적인 REST 요청 (http) 요청, 두 번째는 Message Broker를 통한 요청이다. 문제가 발생한 케이스들을 살펴보니 모두 Message Broker를 통한 요청이었다. 
- 서버의 Tomcat thread가 max 512로 잡혀있는 것에 반해, Message Broker를 처리하기 위한 thread는 5개에 불과했다. 
- 실제로 이슈 케이스를 확인해보니 동시간 대에 이슈가 발생한 두 호출이 동일한 thread를 사용하고 있었다.

위 사항들을 종합해봤을 때, **외부 서버에 이슈가 발생했고 해당 시간대에 비동기 요청이 길어지면서 상대적으로 적은 수의 스레드를 사용하고 있던 Message Broker API 처리 과정에서 동일한 스레드를 가져오면서 문제가 발생했다.**  라는 가설을 세웠다.

### 재현

다음은 위 가설을 기반으로 만든 재현 케이스이다.

##### Thread Pool Config
```java
    @Bean  
    public TaskExecutor executor1() {  
        ThreadPoolTaskExecutor taskExecutor = buildThreadPoolTaskExecutor(1, "e-1");  
  
        return taskExecutor;  
    }  
  
    @Bean  
    public TaskExecutor executor2() {  
        ThreadPoolTaskExecutor taskExecutor = buildThreadPoolTaskExecutor(1, "e-2");  
        taskExecutor.setTaskDecorator(runnable -> new DelegatingSecurityContextRunnable(runnable, SecurityContextHolder.getContext()));  
  
        return taskExecutor;  
    }  
  
    private ThreadPoolTaskExecutor buildThreadPoolTaskExecutor(int coreSize, String threadNamePrefix) {  
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();  
  
        taskExecutor.setCorePoolSize(coreSize);  
        taskExecutor.setMaxPoolSize(coreSize);  
        taskExecutor.setThreadNamePrefix(threadNamePrefix);  
        return taskExecutor;  
    }
```
- executor1은 cloud stream에 의해 생성된 Message Broker 스레드 풀이라고 가정한다.
- executor2은 dataSync()에 사용할 사용자 정의 스레드 풀이라 가정한다. 이는 DelegatingSecurity 관련 Decorator 설정이 된 스레드 풀이다.

##### Service
```java
@Slf4j  
@RequiredArgsConstructor  
@Service  
public class TestService {  
  
    @Qualifier("executor2")  
    private final TaskExecutor taskExecutor;  
  
    @Async("executor2")  
    void async1() {  
        log.error("async start");  
        try {  
            Thread.sleep(20000);  
        } catch (Exception e) {  
        }  
        log.error("async end");  
    }  
  
    @Async("executor1")  
    void async2() {  
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("key", "pc1", Collections.singleton(getRole())));  
  
        log.error("async 2 current session {}", SecurityContextHolder.getContext().getAuthentication().getPrincipal());  
        taskExecutor.execute(() -> {  
            log.error("async 2 inner start");  
            log.error("async 2 inner current session {}", SecurityContextHolder.getContext().getAuthentication().getPrincipal());  
            log.error("async 2 inner end");  
        });  
        log.error("async 2 end");  
    }  
  
    @Async("executor1")  
    void async3() {  
        log.error("async 3 start");  
  
        // 얘가 갱신하려고 하는 애는 이미 Delegation에서 세팅해준 SecurityContextHolder의 context이다.  
        // Wrapping이 없을 경우 원본이 갱신되지만  
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("key", "pc2", Collections.singleton(getRole())));  
  
        log.error("async 3 end");  
    }  
  
    private JaasGrantedAuthority getRole() {  
        return new JaasGrantedAuthority("role", new Principal() {  
            @Override  
            public String getName() {  
                return "hello";  
            }  
        });  
    }  
}
```
1. async1: **executor2** 스레드를 사용해 시작된다. --- 20초 동작
2. async2: **executor1** 스레드를 사용해 시작된다.
3. async2: SecurityContext에 **pc1** 설정 후 executor2 스레드를 호출하지만 Pool에 여유 스레드가 없다.
4. async2: 종료된다.
5. async3: **executor1** 스레드를 사용해 시작된다.
6. async3: SecurityContext에 **pc2** 설정
7. async3: 종료된다.

마지막으로 async2의 taskExecutor가 executor2의 스레드를 얻어 동작을 시작한다. 이 때 SecurityContext에 pc1이 있을 것을 예상했지만 pc2가 들어있다.

```shell
2022-11-30 00:22:19.548 ERROR  : async start
2022-11-30 00:22:21.664 ERROR  : async 2 start
2022-11-30 00:22:21.665 ERROR  : async 2 current session pc1
2022-11-30 00:22:21.667 ERROR  : async 2 end
2022-11-30 00:22:23.991 ERROR  : async 3 start
2022-11-30 00:22:23.992 ERROR  : async 3 end
2022-11-30 00:22:39.554 ERROR  : async end
2022-11-30 00:22:39.558 ERROR  : async 2 inner start
2022-11-30 00:22:39.559 ERROR  : async 2 inner current session pc2
2022-11-30 00:22:39.559 ERROR  : async 2 inner end
```

## 왜 이런 일이 발생하는가

먼저 DelegatingSecurityContextRunnable의 전파 방식을 이해해야 한다. 

TaskExecutor 정의하는 코드를 보면 DelegatingSecurityContextRunnable 생성 시점에 SecurityContext.getContext()를 전달함으로써 Context를 전파하는 방식을 사용하고 있다. 이는 Context의 참조값을 전달하는 방식이기에 외부에서 SecurityContext.getContext().setAuthentication(...) 등을 이용해 SecurityContext 내부의 값을 수정하면 전파 받은 스레드에도 그대로 영향이 가게 된다.

DelegatingSecurityContextRunnable의 코드를 보면 Runnable.run()을 호출하기 직전에 
`SecurityContextHolder.setContext(this.delegateSecurityContext);`를 이용해 SecurityContextHolder의 값을 설정하고 있다.

대부분의 경우 setContext 직후에 스레드를 얻고 비동기 로직이 수행됐기 때문에 문제가 없었을 것이다. 하지만,비동기 호출의 지연이 발생하고, 스레드 대기로 이어진다면? 외부 스레드의 재사용과 SecurityContext 갱신이 무차별적으로 발생하며 이슈가 생기게 되는 것이다. (그게 이번 사례였다.)

## 개선

결국 Delegating 관련 추상화를 이용한다고 Thread 간 Security 공유 문제가 모두 해결되는게 아니었다. cloud stream binding 스레드 풀, tomcat 스레드 풀 등 사용자에 의해 커스텀되지 않은 풀에서는 동일한 문제가 일어나지 않으리라는 보장이 없다.

결과적으로 팀에서 채택한 해결 방법은 AOP를 활용해서 스레드 시작 시점에 SecurityContextHolder 자체를 새로 초기화하고, 스레드 종료 시점에 clearContext() 해주는 것이었다. 이렇게만 해도 하위 스레드에 SecurityContext 간섭은 방지할 수 있기 때문이다.

---
**PS**
매우 드문 상황에 발생하는 케이스였지만, 2차 검증 로직이 없었다면 단순 오류가 아닌 치명적인 보안문제로 이어질 수 있는 케이스였다. 이렇게라도 문제를 찾아서 다행이고, 단순히 사용하고 있던 DelegatingSecurityContextRunnable의 내부 동작을 공부할 기회였다.

참고
- https://docs.spring.io/spring-security/site/docs/5.0.x/reference/html/concurrency.html#concurrency
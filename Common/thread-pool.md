# 이상적인 Thread Pool의 갯수는?

Java의 스레드 생성에는 비용이 들어간다. 스레드 생성에는 시간이 걸리고, 요청 처리에 지연을 발생시킨다. 스레드 풀은 현재 task를 실행을 위해 이전에 생성된 스레드를 재사용한다. 이는 스레드 사이클의 오버헤드에 대한 솔루션이 된다.

### 왜 스레드 풀 제한을 둬야할까?

Executors, newChachedThreadPool 등과 같이 java에서 기본으로 설정되어 있는 스레드 풀을 왜 사용하면 안될까?

```java
/** Thread Pool constructor */
public ThreadPoolExecutor(int corePoolSize,
              int maximumPoolSize,
              long keepAliveTime,
              TimeUnit unit,
              BlockingQueue workQueue) {...}

/** Cached Thread Pool */
public static ExecutorService newCachedThreadPool() {
              return new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                                                      60L, TimeUnit.SECONDS,
                                                      new SynchronousQueue());
}
```

코드를 보면 SynchronousQueue를 확인할 수 있다. 이는 모든 스레드가 바쁜 상태에서 새로운 작업에 대해 새로운 스레드를 만든다는 것을 의미한다. high load(혹은 버스트) 상황에서, 기아(starvation) 상태에 빠질 수 있으며 최악의 경우 OOM을 맞이하게 될 것이다.

### 스레드 풀의 제한

스레드 풀의 사이즈를 조절하기 전에 우리는 제한 사항을 이해해야 한다. 이는 하드웨어만을 뜻하는 것은 아니다.

1. 워커 스레드가 DB에 의존하고 있다면, 스레드 풀은 DB CP 사이즈에 의해 제한된다. DB CP사이즈가 100인데, 워커 스레드가 1000개라면 말이 안된다.
2. 워커 스레드가 동시에 제한된 수의 요청만 처리할 수 있는 외부 서비스를 호출한다면, 스레드풀은 이 서비스의 처리량(Throughput)에 종속된다.
3. 물론, 스레드 풀에 대한 중요한 요소 중 하나는 CPU이다. 우리는 다음 코드를 통해 CPU core 수를 얻을 수 있다.

   ```java
   int numOfCores = Runtime.getRuntime().availableProcessors();
   ```

   (위 코드는 CPU 수를 얻어오는 고전적인 방법이다. 주의할 점은 컨테이너 환경에서 구체적인 제한을 설정하지 않는다면 프로세스는 호스트 OS의 하드웨어를 보게 된다.)

4. 기타 메모리, 소켓, 파일 핸들링 등등도 고려요소이다.

### 공식

일반적으로 스레드 풀의 사이즈를 결정하는 공식은 다음과 같다.

```java
Number of threads = Number of Available Cores * (1 + Waiting time / Service time)
```

**Waiting Time**

I/O 작업 완료까지 대기하는데 사용되는 시간 (다른 서비스의 HTTP 응답 등)

- 스레드가 대기 상태로 있는 시간, lock을 모니터링하는 시간 등도 될 수 있음

**Service Time**

HTTP 응답 처리, marshalling/unmarshalling, 기타 변환 등에 소모되는 시간

**Blocking Coefficient** (**Waiting Time / Service Time)**

계산 집약적인 작업은 **Blocking Coefficient**가 0에 가깝다. 이 경우, 스레드 수는 코어 수와 같다. 모든 작업이 계산 집약적이라면, 더 많은 스레드는 도움이 전혀 되지 않는다.

예시

- 워커스레드는 마이크로 서비스를 호출, 응답을 JSON으로 직렬화하고 특정 작업을 수행한다.
- 마이크로 서비스의 응답 시간은 50ms, 처리 시간은 5ms이다
- 서비스는 2core CPU 서버에 배포된다.

이 경우 `2 * (1 + 50 / 5) = 22`가 적절한 스레드 풀 사이즈이다.

...이 예시는 너무 간단하다. 애플리케이션은 HTTP CP외에 JMS와 JDBC CP 요청도 있을 수 있다.

만약 작업의 클래스가 다르다면 각각의 워크로드에 알맞은 여러 스레드 풀을 사용하는게 가장 좋은 방법이다. 이 경우 위의 공식에 스레드 풀의 사용률만 추가하면 된다. (0~1)

```java
Number of threads = Number of Available Cores * Target CPU utilization * (1 + Wait time / Service time)
```

### Little's Law

앞의 단계를 통해 이제 최적의 스레드 풀 사이즈를 찾을 수 있고, 이론적으로 계산 가능한 상한과 몇 가지 메트릭을 알게 되었다. 하지만 스레드의 수가 latency, throughput를 얼마나 바꿀 수 있을까?

리틀의 법칙은 이 질문의 해답이 될 수 있다. 이 법칙은 다음과 같이 말한다.

**시스템의 요청 수 = 요청 비율 \* 개별 요청을 서비스하는 데 걸리는 평균 시간을 곱한 값**

이 공식은 특정 latency 수준에서 미리 정의된 throughput을 핸들링 하기 위한 스레드 수를 계산하는 데 사용할 수 있다.

```java
L = λ * W

L - the number of requests processed simultaneously
λ – long-term average arrival rate (RPS)
W – the average time to handle the request (latency)
```

이 공식을 사용하여 정해진 수의 요청을 안정적으로 처리할 수 있는 시스템 용량(또는 병렬로 실행되는 인스턴스 수)를 계산할 수 있다.

위의 예에서 평균 서비스 응답 시간은 55ms(50 + 5)였고, 스레드 풀 사이즈는 22였다. 여기에 리틀의 법칙 공식을 적용하면 다음과 같다.

```java
22 / 0.055 = 400 // 안정적인 응답을 제공할 수 있는 초당 요청 수
```

### 결론

이러한 공식은 만능이 아니며 모든 프로젝트에 적합하지는 않다. 공식은 시스템의 평균 요청 수에만 초점을 맞췄지만 실제로는 다양한 트래픽 버스트 패턴이 존재하기 때문이다. 그러나 이런 공식이 프로젝트의 시작점이 될 수는 있다. 계산된 값으로 시작한 뒤, 테스트 후 스레드 풀을 조절하자.

출처: [https://engineering.zalando.com/posts/2019/04/how-to-set-an-ideal-thread-pool-size.html](https://engineering.zalando.com/posts/2019/04/how-to-set-an-ideal-thread-pool-size.html)\

**의역 많음**

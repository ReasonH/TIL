# Redisson의 Local Cache
로컬 캐시란 무엇일까? 쉽게 말하자면 애플리케이션 런타임에 heap을 사용해 데이터를 관리하는 방법이다. 적절한 퇴거 정책과 업데이트 로직만 있다면 단순 변수 선언도 로컬캐시라고 할 수 있다. 물론 대부분의 경우 Caffeine이나 Guava 등 외부 라이브러리를 사용한다.

로컬 캐시는 DB는 물론 외부 캐시시스템을 접근하는 것보다도 훨씬 빠른 속도를 보장한다. 타 시스템에 대한 의존은 물론 **네트워크 트래픽 자체**가 발생하지 않기 때문이다.

그러나 이는 단점도 존재한다. 그건 바로 데이터가 애플리케이션 단위로만 관리된다는 점이다. 프로덕션 환경의 서비스라면 동일한 애플리케이션을 여러 인스턴스로 구동할 것이다. 그리고 여러 인스턴스 간 로컬 캐시의 데이터를 동기화하려 한다면, 이는 매우 복잡한 작업이 될 것이다.

로컬 캐시의 이점을 누리면서 모든 인스턴스 간 실시간으로 데이터를 동기화시킬 방법은 없을까?

### Redisson Local Cache

Redisson이 제공하는 local cache는 이럴 때 좋은 대안이 된다. Redisson의 로컬 캐시는 크게 두 가지 용도로 사용할 수 있는데

1.  기존 캐시 데이터 access가 빈번해서 성능 향상을 누리기 위해
2.  기존 로컬 캐시를 여러 instance간 동기화 하기 위해

앞서 어급한 사례는 2번에 해당할 것이다.

### How to work?

사용법은 간단하다.
`redissonClient.getMapCache("myCache");` 와 같이 호출하고,
`myCache.put(key, value);` 와 같이 데이터를 넣으면 끝이다.
Spring 서버 등에서 사용할 경우 아래와 같이 Bean 초기화 시 로컬 캐시를 초기화해두고 런타임 내에 계속해서 사용한다.
```java

@Service
@RequiredArgsConstructor
public class Test {
	private final RedissonClient redissonClient;
	private RMapCache<String, TestCache> myCache;

	@PostConstruct
	public void init() {
		myCache = redissonClient.getMapCache("myCache");
	}

	public update(String key, TestCache value) {
		myCache.put(key, value);
	}
}

```

자세한 동작 방식은 다음과 같다.

1.  캐시 초기화
    
    인스턴스 A 및 인스턴스 B는 Redisson 구성을 기반으로 로컬 캐시를 시작하고 초기화
    
2.  인스턴스 A의 캐시 업데이트
    

-   인스턴스 A는 로컬 캐시의 특정 항목을 업데이트하라는 요청을 수신
-   인스턴스 A는 항목을 추가, 수정 또는 제거하여 캐시 업데이트 작업을 수행

3.  캐시 업데이트 전파

-   Redisson은 인스턴스 A에서 캐시 업데이트 작업을 가로채고, 캐시 업데이트 이벤트 + 영향을 받는 데이터를 나타내는 캐시 업데이트 메시지를 Redis 채널 또는 항목에 게시

4.  인스턴스 B에서 받은 캐시 업데이트

-   Redis 채널 또는 항목에 가입된 인스턴스 B는 캐시 업데이트 메시지를 수신

5.  인스턴스 B의 캐시 무효화

-   캐시 업데이트 메시지를 수신하면 인스턴스 B는 로컬 캐시의 해당 항목에 인스턴스 A의 변경 사항을 반영하거나 항목을 무효화 (무효화된 항목은 다시 요청할 때 Redis에서 다시 로드)
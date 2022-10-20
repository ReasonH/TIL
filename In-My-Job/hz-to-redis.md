# from Hazelcast to Redis

회사에서 사용 중인 캐시 시스템을 변경하게 됐다. Redis로 넘어가게 된 이유는 여러가지였다.

## Hazelcast에서의 문제점

#### 1. 캐시 클러스터 용량
3대의 노드로 운용중이던 Hazelcast 캐시의 메모리 용량이 한계에 다달았다. 단순히 scale-out을 하면 되는거 아닌가? 라고 생각할 수 있지만, 문제가 간단하지 않았다. 기존에 사용하던 Hazelcast mancenter의 경우 완전 오픈소스가 아니라, 일정 갯수의 노드까지만 무료로 사용할 수 있는 툴이었다. 노드가 늘어나면 이를 유료로 사용해야 한다는 문제가 있었다.

#### 2. 부족한 생태계
다른 오픈소스 웹 콘솔은 없었나? 내가 못 찾은건지는 모르겠지만 적어도 내가 찾은 것 중에는 쓸만한 오픈소스가 없었다. 기존에 개발하면서도 느낀점이지만 hazelcast는 사용자 커뮤니티가 비교적 활성화되지 않은 편이었다. 단순히 stackoverflow에 올라오는 이슈들도 오래된 구 버전의 이슈를 다루고 있었고, 뭔가 찾아보려 하면 실 사용자의 이야기보다는 docs만 올라오는 것도 아쉬운 점이었다.

#### 3. 마지막으로...
hazelcast여야만 하는 이유가 없었다. 초기에 hazelcast를 선택했던 기준은 모르겠으나, 현재 팀 내에는 hazelcast라는 시스템을 잘 아는 사람이 없었다. (관성에 의해 사용되는?) 시스템이었기에 조금이라도 장점이 있고 편리한 대체제가 있다면 모두가 변경에 찬성하는 상태였다.

## Redis 도입
Reids를 선택한 이유는 다음과 같다.

#### 1. 성숙도
![](hz-to-redis-1.png)
압도적인 커뮤니티의 활성도와 그에 따른 다양한 경험들이 이미 축적되어 있었다. 시스템 어드민 용도로 사용할 수 있는 다양한 웹 콘솔, Java Client, 벤치마크 자료와 트러블 슈팅 자료까지 이미 캐시 시스템의 트렌드가 되면서 구축된 탄탄한 생태계가 있다.

#### 2. 영속화(백업) 지원
기본적으로 제공하는 RDB / AOF 기능은 데이터 영속화에 대한 걱정을 덜어준다. 새로운 기능 개발을 준비하면서 Look Aside용도가 아닌, Persistence 용도로의 캐시 사용을 검토하던 중에 Redis가 가진 이런 백업 지원이 적절하다고 생각했다.

#### 3. 개발 편의성
Redis하면 가장 먼저 특징으로 거론되는게 다양한 자료구조와 Single Thread 패턴이다. 개발에 있어서 이런 부분은 많은 수고를 덜어준다. 특히 원자성 보장은 기존 race condition 관리를 위한 수고를 눈에 띄게 덜어줬다.


## How to?

### Redis Cluster 구축
먼저 Redis 운영 방식을 결정해야 했다. Redis하면 많이 거론되는 Sentinel 방식은 scale-up만 가능했기에 단점이 명확했다. 프로덕션 환경에서는 구축이나 운영이 복잡하더라도 scale-out을 위해 Cluster 방식으로 사용하는게 유리했다. 물론 자동 Fail over는 둘 다 제공된다.

### 백업 전략
Redis에서 지원하는 백업 전략은 RDB와 AOF가 있다. 각각의 특징은 다음과 같다.

#### RDB
특정 시점에 메모리에 있는 데이터 스냅샷을 생성하는 기능이다.
-  로딩 속도가 AOF 보다 빠르다.
-  그러나 스냅샷 추출이 오래걸린다. → 이 시간 동안 다른 요청이 block 된다.
-  스냅샷 간격이 길기 때문에 그 사이 요청은 유실된다.

#### AOF
명령 단위의 데이터를 파일에 기록한다.
-  데이터 손실이 거의 없고, 쓰기 속도가 빠르다.
-  데이터 양이 크고, 복구 시 명령어를 처음부터 수행하기 때문에 재시작 속도가 느리다.
-  fsync everysec 정도로 사용 / no의 경우 OS에 맡긴다.
-  특정 시점에 데이터 전체를 다시 쓴다. 커맨드를 압축한다고 보면 된다. (rewrite)

RDB의 경우 라이브 서비스에서 block을 만들 수 있다는게 치명적이었다. Failover에 대한 복구 시, 시간이 더 걸리더라도 AOF 옵션만을 활용하는게 유리하다고 판단해 RDB는 사용하지 않았다.

	save ""
	appendfsync everysec

#### Max-Clients
Redis 전용 서버였기 때문에 소켓을 50,000 정도로 크게 잡아도 상관은 없었다. 그러나 Redis를 사용하는 Spring Server와 Connection Pool에 대한 최대 수치가 명확했기 때문에 다음과 같이 계산해서 약 10,000 으로 설정했다.
(참고로 여기서 Subscribable Channel Connection을 미리 파악하지 못해서 후에 이슈의 원인이 된다.)

	((Max Connection + Subscribable Channel Connection) * 서버의 기본 Pod 수) * 2 


### Redis Client 설정
현재 Java 기반 서버에서 Redis에 대한 클라이언트 선택지는 3개였다.
- Jedis: 사용하기 쉽지만, 클러스터에서의 동기 처리로 인한 성능 이슈가 있다.
- Lettuce: Spring이 기본 채택하는 클라이언트, 다양한 처리 지원 복잡하지만 높은 성능을 낼 수 있다.
- Redisson: 가장 다양한 자료구조 제공, near cache 기능을 지원한다.

우리는 Redisson을 이용했는데, 기존 시스템에 쓰이던 분산 락을 pub-sub 방식으로 제공하고 있었고, Hazelcast에서 사용하던 near cache 기능을 사용 가능했기 때문이다.

### Api Server
작업은 다음과 같이 진행됐다.
1. Redis Cache 설정 추가, Connection pool size 등은 우선 Hazelcast와 동일하게 설정
2. Hazelcast에 대치되는 Redis 서비스 로직 작성
3. 캐시 제거 시, 부하가 한 번에 몰릴 것을 대비해 일정 기간 마이그레이션 되도록 로직 수정
	- Hazelcast Hit -> Redis insert 프로세스 추가 
4. Migration 기간 (2주) 후 Hazelcast 로직 제거


## Issue
간헐적으로 `Unable to send PING command over channel`  로그가 다수 발생하며 , 동시간 대에 lock 사용되는 API들이 동작하지 않는 이슈가 있었다. 

**증상은 다음과 같았다.**
1. Redis cluster의 로그를 살펴봤을 때 failover나 이상 상태에 대한 로깅은 없음
2. 동시간 대 redis lock을 사용하는 API에서 오류 발생
3. 동시간 대 Can't update lock, only 0 of 1 slaves were synced 로그 발생

#### 조사
증상들을 보자마자 우선 lock 문제를 짐작했다. 관련해서 찾아보던 중 Redisson Pub/Sub 모델의 lock이 독자적인 Connection을 유지한다는 점과 이게 네트워크 channel을 점유하며 부하를 발생시킬 수 있다는 것을 알게 됐다.

설명은 의외로 Spin lock에 되어 있었다. (이런 문제를 예방하려면 Spin lock을 써라)
*결국 은탄은 없다는 것이다...*
![](hz-to-redis-2.png)
![](hz-to-redis-3.png)
pub/sub 형식이 좋구나 싶어서 바로 도입하고, 정작 그로인해 발생할 수 있는 문제는 제대로 알아보지 않은게 패착이었다. 이를 해결하기 위한 대안은 다음과 같았다.

1. Subscription Channel size를 늘린다.
2. Spin lock 방식으로 전환한다.
**3. ... 그런데 우리의 코드에는 문제가 없나??**

#### 응? 코드 개선
해결방법을 찾으며 우리의 코드를 보다가 문득 생각이 들었다. Hazelcast를 Redis로 Migration하며 Distrubuted lock을 사용하는 곳을 당연히 Redisson Lock으로 모두 대체했다. 그런데, 정말 여기에 모두 lock이 필요한게 맞나? 이걸 검토를 했던가?

회사의 API 중 초당 호출횟수가 가장 높은 것은 당연 채팅 메시지를 처리하는 API였다. 여기에는 채팅방에서 발생한 메시지의 순서를 결정하기 위해 sequence라는 변수를 캐싱하고 있었고, 이에 대한 경합을 관리하기 위해 lock을 사용했다.

```java
// For instance
public void send() {

	// 각종 처리 로직
	try {
		if (lock.tryLock(2, 1, TimeUnit.SECONDS)) {
			int sequence = messageSequenceCache.get("<chatting room ID>") + 1;
			messageSequenceCache.set("<chatting room ID>", sequence);
		}
	} catch (~~~) {
		~~~~
	}
	// 각종 처리 로직
}
```

이는 Hazelcast를 사용하던 시절의 코드를 거의 그대로 매핑한 것인데, Redisson에는  `RAtomicLong`과 같은 변수 증분용 캐시 오브젝트가(+ 자동으로 원자성 보장이 되는) 있다.

결국 내가 바꾼 코드는 다음과 같다.
```java
// For instance
public void send() {

	// 각종 처리 로직
	int sequence = messageSequenceCache.incrementAndGet("<chatting room ID>");
	// 각종 처리 로직
}
```
가장 사용률 (경합률)이 높은 API를 이렇게 바꾸게 됨으로써 모든 문제는 해결되었다.

#### 결론
쉬운 길을 돌아온 감이 정말 크지만, 잘못한 것들을 많이 느꼈다.

1. 어떤 기술의 장점이 우리 시스템에서도 정답이라고 생각하면 안된다.
2. 다는 아니어도 사용하는 함수에 관해서라도 docs는 읽자, 설정부터 문제까지 docs에 다 있다.
3. 특성을 파악하자
	- Redis의 Singleton 특징은, 이런 증분용 캐시의 경합 관리에 이점이 된다.
	- 그런데, 이를 위해 별도의 lock을 걸다니...

---
부록으로  [redis-tip](/Common/redis-tip)

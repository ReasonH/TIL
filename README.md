# TIL
정리했던 것들 기록

### Spring
- [Spring Security](Spring/Spring-Security.md)
- [Spring Cloud Stream](Spring/Spring-Cloud-Stream.md)
- [Spring Cloud OpenFeign](Spring/Spring-Cloud-OpenFeign.md)
- [Spring Cache](Spring/Spring-Cache.md)
- [JDK와 CGLIB](Spring/jdk-cglib.md)
- [각종 Spring Annotation](Spring/annotaton.md)

### Test
- [테스트 DateTime 고정](Spring/test-datetime.md)
- [MockMvc StanaloneSetup vs webAppContextSetup](Spring/mockmvc.md)
- [테스트 문서화에서 발생한 PayloadHandlingException 처리](Spring/rest-docs-PayloadHandlingException.md)
- [테스트 메서드에서 @Transactional 사용 시 주의점](Spring/test-transactional.md)

### JPA, Hibernate
- [JPA](JPA/README.md)
- [트랜잭션 내에서의 각종 동작 정리](JPA/transactional-test.md)
- [Hibernate](Hibernate/hibernate.md)
- [Hibernate 캐시](Hibernate/hibernate-cache.md)
- [주요 Hikari CP 설정](Hibernate/hikariCP.md)
- [왜 @Transactional noRollbackFor이 적용되지 않는가](Hibernate/no-rollback-for.md)

### Java
- [Annotation Retention](Java/retention.md)
- [안정적인 스레드 풀의 수는 얼마일까?](Java/thread-pool.md)
- [Generic](Java/generic.md)
- [동기화 키워드](Java/sync-keyword.md)
- [람다 캡쳐링과 Final](Java/effectively-final.md)
- [병렬 스트림](Java/parallelStream.md)
- [Virtual Thread](Java/virtual-thread.md)

#### Kotlin
- [Companion object란?](Kotlin/companion-object.md)
- [코틀린의 범위 지정 함수](Kotlin/scope-function.md)
- [각종 키워드](Kotlin/keyword.md)
- [코틀린과 JPA Entity](Kotlin/kotlin-jpa)

#### MySQL
- [mySQL 팁](MySQL/mySQL.md)
- [mySQL의 쿼리분석](MySQL/explain.md)
- [인덱스에 관한 각종 정리](MySQL/index.md)

#### Cassandra
- [Cassandra](Cassandra/cassandra.md)
- [Cassandra TTL](Cassandra/cassandra-ttl.md)
- [Cassandra Component](Cassandra/cassandra-component.md)

#### Redis
- [Cacheable](Redis/cacheable.md)
- [Redis와 Transaction 결합, 그리고 TransactionTemplate](Redis/redisTemplate.md)
- [redisson local cache](Redis/redisson-localcache.md)
- [redissonRegionFactory는 왜 EVAL을 수없이 보낼까](Redis/redisson-2lv.md)
- [레디스 팁](Redis/redis-tip.md)

#### Docker / K8s
- [Docker](Docker/Docker-Concept.md)
- [Kubernetes](Kubernetes/README.md)

#### Protocol
- [Websocket](Protocol/websocket.md)
- [gRPC](Protocol/gRPC.md)

#### ETC
- [Message Queue vs Pub/Sub 차이점](ETC/mq-pub-sub.md)
- [Maven](ETC/maven.md)
- [Kafka 간단 개념 정리](Kafka/README.md)

#### Issue & Experience
- [캐시 전환 작업 회고](Experience/hz-to-redis.md)
- [SecurityContext 이슈](Experience/securitycontext.md)
- [hikariCP 고갈 이슈](Experience/hikariCP-starvation.md)
- [@Transactional과 @Retry 사용시 주의점](Experience/transaction-retry.md)

### Book
1. [Refactoring](Refactoring/README.md)
2. [HTTP 완벽 가이드](Http/README.md)
3. [오브젝트](Object/README.md)

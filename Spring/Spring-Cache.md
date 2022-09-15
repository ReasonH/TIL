# Spring과 Hibernate에서의 Cache 지원

## Spring Cache 추상화

스프링 프레임워크는 Cache 추상화를 지원한다. 이를 통해 다양한 Cache 솔루션을 편리하게 도입/변경 가능하다. 대부분의 로직은 Spring AOP 기반으로 동작하기 때문에 public method 제약, 동일 클래스 내의 method 호출 제약 등이 있다.

### Cacheable + Optional

@Cacheable 사용 시, 캐시가 조회되지 않는 경우 실제 메서드가 호출되며 반환 값이 캐시된다. 이 때 반환 값으로 Optional이 오는 경우 자동으로 Unwrapped 된 값이 캐시된다.

- Optional 값이 present ⇒ 캐시 저장
- Optional 값이 not present ⇒ 캐시에 null 이 저장, @Cacheable 필드에 null unless 처리된 경우는 예외

**HIbernate 캐시와의 차이점**

Spring Caching은 메서드 호출 결과 전체가 캐시된다. 반면 Hibernate는 캐시되는 엔티티를 더 세밀하게 조정할 수 있다.

ex) Transient 필드는 Spring에 의해 캐시되지만 Hibernate에 의해서는 캐시되지 않음

- `@EnableCaching` 기능 활성화
- `@Cacheable` 캐시를 적용할 메서드 지정, 1회 호출 시 캐시 저장 → 이후 캐시 즉시 반환
  - javax.persistence.Cacheable (JPA @Cacheable과 다름)
- `@CacheEvict` 캐시된 데이터 삭제
- `@CachePut` 캐시 업데이트
- `@CacheConfig` 클래스 레벨에서 캐시 설정 공유

스프링 캐시 추상화는 실제 저장소를 제공하주지 않고 구체화된 추상에 의존한다. 사용자가 `CacheManager`나 `CacheResolver` 타입의 빈을 정의하지 않는다면 스프링 부트는 캐시 제공자를 정해진 순서대로 탐지한다. ([https://docs.spring.io/spring-boot/docs/2.1.6.RELEASE/reference/html/boot-features-caching.html](https://docs.spring.io/spring-boot/docs/2.1.6.RELEASE/reference/html/boot-features-caching.html))

## Hibernate Cache

### 1차 캐시 (JPA 1차 캐시)

영속성 컨텍스트(Persistence Context)의 내부에는 엔티티를 보관하는 저장소가 있는데 이것을 1차 캐시(First Level Cache)라고 부른다. 1차 캐시는 트랜잭션이 시작하고 종료할 때까지만 유효하다. 즉, 트랜잭션 단위의 캐시이며 Commit이나 Flush 하게 되면 1차 캐시 내용을 DB에 반영한다.

스프링 프레임워크에서 JPA를 실행하는 경우 트랜잭션 시작/종료에 따라 영속성 컨텍스트가 실행/종료된다. 1차 캐시는 비활성화가 불가능하다. 따라서 영속성 컨텍스트 자체가 1차 캐시라고 할 수 있다.

### 2차 캐시 (JPA 2차 캐시)

2차 캐시는 공유 캐시라고도 불리며 애플리케이션 단위의 캐시이다. 애플리케이션 종료까지 캐시가 유지된다. 분산 캐시나 클러스터링 환경 캐시는 애플리케이션보다 오래 유지될 수 있다.

데이터 조회 시 2차 캐시에서 우선적으로 이루어지므로 DB 접근 횟수를 획기적으로 낮출 수 있다.

- 조회한 객체의 복사본을 만들어서 반환

JPA는 2.0에 와서 캐시 표준을 정의했다. 이는 여러 구현체가 공통으로 사용하는 부분을 표준화했으며 세밀한 설정을 위해서는 구현체별 기능을 사용해야 한다.

## Hibernate 2차 캐시

대표적인 JPA 구현체인 하이버네이트의 2차 캐시는 3가지 형태의 캐시를 지원한다.

1. 엔티티 캐시
   - 엔티티 단위의 캐시
   - 식별자로 엔티티 조회 or 컬렉션이 아닌 연관 엔티티 조회 시 사용
2. 컬렉션 캐시
   - 엔티티와 연관된 컬렉션을 캐시, 컬렉션이 엔티티를 담고 있으면 식별자 값만 캐시
3. 쿼리 캐시
   - 쿼리와 파라미터 정보를 키로 사용해 캐시
   - 결과가 엔티티인 경우 식별자 값만 캐시

### 엔티티 캐시, 컬렉션 캐시

@Cache로 설정 가능하다.

### 쿼리 캐시

쿼리 캐시는 쿼리와 파라미터 정보를 키로 사용해서 쿼리 결과를 캐시하는 방법이다. 쿼리 캐시를 적용하려면 영속성 유닛을 설정에 hibernate.cache.use_query_cache 옵션을 꼭 true로 설정해야 한다. 이후 캐시를 적용할 대상 쿼리에 cacheable을 true 설정한다.

### 쿼리 캐시, 컬렉션 캐시 주의사항

엔티티 캐시는 엔티티 정보를 모두 캐시하지만, 이외에는 식별자만 캐시해두고 사용 시점에 실제 값을 조회하게 된다. 따라서 쿼리/컬렉션 캐시를 사용하는 대상 엔티티에 엔티티 캐시가 적용되어 있지 않은 경우 심각한 성능 문제가 발생한다.

### @Cache 설정

- usage: 캐시 동시성 전략
- region: 캐시 지역 설정
- include: 연관 객체 캐시 포함 여부 선택, 기본값은 all

**동시성 전략**

- `NONE` : 캐시를 설정하지 않는다.
- `READ_ONLY` : 읽기 전용으로 설정한다. 등록, 삭제는 가능하지만 수정은 불가능하다.
  - 자주 조회하고 수정이 없는 경우
- `NONSTRICT_READ_WRITE` : 엄격하지 않은 읽고 쓰기 전략이다.
  - 자주 조회하고 거의 수정하지 않는 경우
- `READ_WRITE` : 읽고 쓰기가 가능하도록 READ_COMMITTED 정도의 격리 수준을 보장한다. Ehcache는 데이터를 수정하면 캐시 데이터도 같이 수정한다.
  - 조회와 수정이 비슷하게 일어나는 경우
- `TRANSACTIONAL` : 컨테이너 관리 환경에서 사용할 수 있다. 설정에 따라 REPEATABLE_READ 정도의 격리 수준을 보장받을 수 있다.
- 상세 설명
  - **_READ_ONLY_**: Used only for entities that never change (exception is thrown if an attempt to update such an entity is made). It is very simple and performant. Very suitable for some static reference data that don't change
  - **_NONSTRICT_READ_WRITE_**: Cache is updated after a transaction that changed the affected data has been committed. Thus, strong consistency is not guaranteed and there is a small time window in which stale data may be obtained from cache. This kind of strategy is suitable for use cases that can tolerate eventual consistency
    캐시 데이터를 수정하면 해당 수정사항이 **커밋된 이후** 캐시가 업데이트 된다. 커밋 전 짧은 순간 데이터 불일치 발생 가능
  - **_READ_WRITE_**: This strategy guarantees strong consistency which it achieves by using ‘soft' locks: When a cached entity is updated, a soft lock is stored in the cache for that entity as well, which is released after the transaction is committed. All concurrent transactions that access soft-locked entries will fetch the corresponding data directly from database
    캐시된 엔티티가 업데이트되면 해당 엔티티 캐시에 소프트 락 저장, 소프트락 항목에 접근하는 모든 동시트랜잭션은 DB에서 직접 조회 수행
  - **_TRANSACTIONAL_**: Cache changes are done in distributed XA transactions. A change in a cached entity is either committed or rolled back in both database and cache in the same XA transaction

### **NONSTRICT_READ_WRITE**

**Read Through**: 조회 시 캐시가 비어있는 경우 DB를 읽으며 캐시를 동기화 (여기에서 Read Through 방식은 쓰기 작업이 발생하는 경우 캐시를 무효화 한다.)

**Write Through**: 쓰기 작업 시 항상 캐시를 동기화

가장 자주 사용되는 캐시 전략인 NONSTRICT_READ_WRITE의 경우 Read Through 방식의 캐시 전략을 취한다고 볼 수 있다. 이 경우 다음의 동시성 문제가 발생할 수 있다.

1. A가 **Entity(v1)**을 캐시에서 조회한 뒤 수정 → 여기서 캐시 evict
2. 그 사이에 B가 DB 스냅샷의 **Entity(v1)**을 로딩 → 여기서 캐시 put
3. A의 트랜잭션 커밋 → 여기서 캐시에는 **Entity(v1)**인 상태
4. **이때, C가 캐시를 조회하는 경우 Entity(v1)이 로드됨.**
5. A의 트랜잭션 커밋 이후 Cache evict

### 기타

**spring의 @Cacheable과 Hibernate의 @Cache**

- @Cacheable 또한 @Cache와 같이 추상화된 캐시를 사용하기 위한 어노테이션이다.
- 엔티티나 함수에 적용 가능
- 엔티티에 이미 Hibernate @Cache 적용했다면 굳이 쓸필요는 없다.

**regionFactory란?**

- L2 캐시에서 실제 캐시 프로바이더 (구현체)를 추상화된 인터페이스
- Hibernate와 Cache 구현체의 브릿지 역할 수행

**region이란?**

- 하이버네이트는 각각의 엔티티 클래스에 대해 각 인스턴스 상태를 저장하기 위한 Cache region을 생성한다.
- region의 이름은 정규화된 클래스 경로이다.

---

출처

[https://hazelcast.com/glossary/hibernate-second-level-cache/](https://hazelcast.com/glossary/hibernate-second-level-cache/)

[https://joosjuliet.github.io/hibernate_structure/](https://joosjuliet.github.io/hibernate_structure/)

[https://www.baeldung.com/hibernate-second-level-cache](https://www.baeldung.com/hibernate-second-level-cache)

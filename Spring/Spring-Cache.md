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

스프링 캐시 추상화는 실제 저장소를 제공하주지 않고 구체화된 추상에 의존한다. 사용자가 `CacheManager`나 `CacheResolver` 타입의 빈을 정의하지 않는다면 스프링 부트는 캐시 제공자를 정해진 순서대로 탐지한다.

---

참고
- https://docs.spring.io/spring-boot/docs/2.1.6.RELEASE/reference/html/boot-features-caching.html
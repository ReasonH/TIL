### 테스트 코드에서의 @Transactional 주의사항

#### 이슈
Repository, Service layer 등을 테스트할 때 무의식적으로 @Transactional을 붙이고 테스트하는 경우가 있다. 이는 테스트의 원자성을 보장하기 위해 롤백을 활성화 시키는 일반적인 작업이지만 다음의 이슈를 만든다.

##### 1. 실패해야하는 테스트를 성공시켜 가짜 음성(false negative)을 만들어낸다.
예를 들어 Lazy loading을 테스트 한다고 가정하자. 이 경우 @Transactional이 명시되어있지 않은 컴포넌트에서 영속성 객체에 Lazy loading을 시도하면 Exception이 발생해야 한다.

그러나 같은 서비스를 @Transactional Test로 호출하면 얘기가 달라진다. 이 테스트는 성공하며 아무런 문제를 일으키지 않는다. 이유는 테스트 코드에서 준 @Transactional 때문에 영속성 컨텍스트가 유지되고 있기 때문이다.

##### 2. Auto-Commit으로 인한 DB 반영 문제
@Transactional은 Auto-commit을 활성화시킨다. 이는 트랜잭션 내에서 변경된 데이터가 모두 커밋 후 DB에 저장된다는 의미이다.

**트랜잭션이 아닌** Controller에서 엔티티를 setter등으로 업데이트 한 경우 테스트에서는 변경된 데이터가 DB에 저장되지만 실제 서비스에서는 DB에 반영되지 않는 문제가 생긴다.

#### 결론
위와 같은 문제를 피하며 테스트의 원자성을 보장하기 위해선 @BeforeEach / @AfterEach 등에서 수동으로 DB를 롤백해주는 선택지를 사용해야 한다.

그렇다면 테스트에서 @Transactional은 언제 사용해야 하는가?

> @DataJpaTest의 메타 어노테이션을 확인하면 @Transactional이 포함되어 있는 것을 확인할 수 있다. 즉, JPA Repository를 테스트할 때 기본적으로 해당 어노테이션을 사용하게 된다는 것이다. 이는 단순히 Repository의 기능을 테스트하는 것이기 때문에 다른 컴포넌트에서 Lazy loading의 작동 유무에 대해 신경 쓸 필요가 없다. 즉 단순 DAO 계층 (Repository 계층)을 테스트할 때는 @Transactional을 사용해도 문제가 없다.

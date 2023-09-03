## Concept  
spring transaction manager 및 JPA가 다양한 상황에서 어떻게 동작하는지 확인한다.
모든 테스트 코드는 [여기](../example/transactional-test-integration/README.md)에서 참고 가능하다.

### 트랜잭션 전파 레벨에 따른 RuntimeException 롤백 처리  
`RuntimeExceptionServiceTest` 참고  
  
부모-자식 관계의 트랜잭션은 자식 트랜잭션 전파레벨에 따라 다음과 같이 동작한다.  
- propagation = `REQUIRES` 인 경우  
  - 부모 트랜잭션에서 catch한 경우 → rollback 마킹에 의해 전역롤백  
  - 자식 트랜잭션에서 catch한 경우 → 롤백하지 않음  
- propagation = `REQUIRES_NEW`를 적용한 경우  
  - 부모 트랜잭션에서 catch한 경우 → 자식 트랜잭션은 롤백됨  
  - 자식 트랜잭션에서 catch한 경우 → 롤백하지 않음  
- 트랜잭션이 아닌 경우  
  - 부모/자식 트랜잭션 구분없이 예외가 잡히기만 하면 롤백하지 않음  
  
### 다른 스레드에 전달된 영속성 객체  
`UpdateAsyncServiceTest` 참고  
  
트랜잭션이 진행중인 메인 스레드에서 비동기 함수로 영속성 객체를 전달하고, 해당 함수 내에서 영속성 객체를 수정하는 경우를 가정해보자.   
이 때는 비동기 함수의 `@Transactional` 여부와 관계없이 dirty-checking이 이루어지지 않는다.   
영속성 객체가 다른 스레드로 넘어가는 순간 detached 상태가 되기 때문이다.  
  
    메인 스레드의 함수가 종료(커밋)되기 전, 비동기 함수의 객체 수정이 먼저 수행되는 경우 변경 사항이 commit될 수 있다.   
그러나 함수 호출마다 동작이 달라질 수 있기에 비동기 호출 내에서 객체 갱신 시 주의해야 한다.  
  
메서드가 호출한 곳과 별도의 스레드라면 전파 레벨과 관계 없이 무조건 별도의 트랜잭션을 생성한다.   
(REQUIRES_NEW와 동일) Spring은 트랜잭션 정보를 ThreadLocal 변수에 저장하기에 다른 스레드로 트랜잭션 전파가 일어나지 않는다.  
  
### 트랜잭션이 종료된 영속성 객체 반환 시 동작  
`UpdateServiceTest` 참고  
  
- 일반 함수 A  
- `@Transactional` 함수 B  
  
가 있을 때, B에서 조회한 엔티티를 A가 반환받는 경우 해당 엔티티는 영속성 컨텍스트에서 detached 된다.   
  
- `@Transactional` 함수 A  
- `@Transactional(propagation = REQUIRES_NEW)` 함수 B  
  
인 경우에도 동일하게 동작한다. 즉 A에서 B의 필드를 수정해도 dirty-checking은 동작하지 않는다.  
  
### 이벤트 리스너에서의 영속성  
`EventListenerServiceTest`  
  
- @EventListener 내에서의 영속성 동작은 일반 함수와 동일하다.  
- @TransactionalEventListener는 기본적으로 트랜잭션 커밋 이후 수행된다.  
  - 이벤트 리스너의 @Transactional 여부와 관계없이 지연 로딩은 동작한다.  
  - 커밋 이후 동작하기 때문에 쓰기 작업은 REQUIRES_NEW를 통해 새로운 트랜잭션을 얻었을 때만 가능하다.  
  
### 1차 캐시와 Flush  
`FirstCacheServiceTest` 참고  
  
@Transactional에서는 다음과 같은 상황이 벌어질 수 있다.  
  
> 1. A라는 유니크 컬럼에 a라는 값이 지정되어 있는 영속성 객체 X가 존재  
> 2. X의 A컬럼 값을 a → b로 수정  
> 3. Y라는 엔티티를 생성해서 A컬럼 값을 a로 지정  
> 4. Y를 영속화하려 하면 unique column duple로 인한 Exception 발생  
  
이는 변경감지가 반영되지 않은 상태에서 이미 DB에 존재하는 유니크 값으로 엔티티를 저장하려 해서 발생하는 예외이다.   
dirty-checking은 기본적으로 flush 단계에서 수행되며 flush는 commit시 자동으로 호출된다.  
flush 내에서도 쿼리가 반영되는 순서가 다른데, Hibernate에서는 다음과 같은 순서를 따르고 있다.  
  
- `OrphanRemovalAction`  
- `AbstractEntityInsertAction`  
- `EntityUpdateAction`  
- `QueuedOperationCollectionAction`  
- `CollectionRemoveAction`  
- `CollectionUpdateAction`  
- `CollectionRecreateAction`  
- `EntityDeleteAction`  
  
순서를 확인해보면 UpdateAction은 InsertAction의 뒤에 있는 것을 확인할 수 있다.   
즉, 기존 유니크 컬럼을 업데이트하는 동작이 insert 구문보다 뒤에 수행되기 때문에 SQL Exception이 발생한 것이다.  
  
참고: https://vladmihalcea.com/hibernate-facts-knowing-flush-operations-order-matters/  
  
이를 방지하기 위해서는 기존 객체의 유니크 컬럼 수정 후, 해당 내용을 즉시 DB에 동기화시키는 saveAndFlush()를 호출해야 한다.    
  
### 1차 캐시와 JPQL  
`FirstCacheServiceTest` 참고  
  
- JPQL 쿼리는 수행 직전 flush를 호출한다.  
- JPQL 쿼리 결과를 application에 로딩할 때, 현재 1차 캐시에 동일한 ID 값이 있다면 이 값을 버린다.  
따라서 **연관 조작 시** 다음의 문제가 발생할 수 있다.  
  
  1. 팀-멤버의 1:N 양방향 관계에서 신규 Team, Member를 만들고, Member에 Team 정보 삽입  
      2. fetch join을 통해 팀-멤버 정보 조회  
  3. Team.members.size() 조회 결과는 0  필드 갱신의 경우 flush 시점에 이미 영속성 객체의 값 또한 갱신된 상태이기 때문에 조회된 값이 버려져도 상관이 없다.  
그러나 양방향 연관관계가 설정된 상태에서 한 쪽만 객체 설정을 해준 경우 문제가 발생할 수 있다.   
이 때의 해결 방법은 연관관계 편의 메서드를 통해 Member의 Team 삽입 시, Team의 Member도 추가하는 것이다. 
# Hibernate

하이버네이트 사용 관련 정리

## Hibernate
간단히 말하자면 Java ORM tool이자 JPA의 구현체이다. 

### Hibernate Architecture
![](hibernate-architecture.png)

Hibernate는 Application과 DB 사이에서 연결과 매핑 등 구성 정보를 로드하며 영속 객체 생성을 담당한다.

### Hibernate Element
하위의 Java API를 이용헤 영속성 조작을 래핑하는 구성요소들을 알아본다.

#### SessionFactory
- 2lv 캐시를 보유한다.
- Session 객체를 생성하는 팩토리 메서드를 제공한다.
- ConnectionProvider로부터 Connection을 얻어와서 Session을 open한다.

#### Session
- 애플리케이션과 DB 데이터 간 JDBC 연결을 래핑하는 인터페이스를 제공한다.
- Transaction, Criteria, Query 등에 대한 메서드를 제공한다.
- 1lv 캐시를 보유한다. (영속성 컨텍스트)
- 객체 삽입, 삭제, 갱신 등의 메서드를 제공한다.

#### Transaction
- 원자 단위 트랜잭션 작업을 지정한다.

#### ConnectionProvider
- JDBC 커넥션을 만들어낸다.
- DataSource / DriverManager 등을 추상화한다.


## Hibernate 팁

### IN 쿼리 사용
JPA에서 IN 쿼리를 사용할 때, 해당 쿼리의 IN 사이즈가 가변적인 경우 종류별 SQL이 생성 및 호출된다. 이는 두 가지 문제를 야기하는데, 다음과 같다.
1. DB 관점에서의 preparedStatement 이점을 누릴 수 없음
2. Hibrernate Execution Plan Cache에 각 종류별 query 데이터 적재로 인한 힙 메모리 점유

해결 방법은 다음과 같다.
1. `hibernate.query.in_clause_parameter_padding`  옵션 설정
2. 직접 패딩 처리 구현
3. `hibernate.query.plan_cache_max_size`  옵션 설정

일반적인 경우 1의 설정으로 적절히 튜닝이 가능하다. 이 옵션은 2의 제곱 수만큼 IN 쿼리를 패딩 처리한다.

```sql
select from Sample where id in (1 ,2 ,3);
-- After Padding 2^2 Padding
select from Sample where id in (1 ,2 ,3, 3);

select from Sample where id in (1 ,2 ,3, 4, 5);
-- After Padding 2^3 Padding
select from Sample where id in (1 ,2 ,3, 4, 5, 5, 5, 5);
```


### save() 사용

JPA save는 두 가지 방식으로 동작한다. 내부 소스 코드를 보면 다음과 같이 생겼다.
```java
@Transactional
@Override
public <S extends T> S save(S entity) {

    if (entityInformation.isNew(entity)) {
        em.persist(entity);
        return entity;
    } else {
        return em.merge(entity);
    }
}
```

#### isNew()
이는 persist와 merge 방식을 구분하는 기준이 된다. 다음의 방식 등으로 구분한다.
- @Id - 식별자가 Wrapper-null 또는 Primitive-0인 경우 new로 판단
- @Version - 필드가 null이면 new로 간주, 필드에 @Version이 있는 경우 @Id는 판단 기준에서 제외됨
- Persistable 구현 - isNew 메서드 직접 구현, @Id와 @Version은 판단 기준에서 제외됨

#### Persist()
업데이트가 아닌 새로운 객체를 만들어서 반환한다.
- DB에 entity 새로 삽입
- 전달받은 entity를 영속성 컨텍스트 managed 상태로 만든다.

#### Merge()
영속성 컨텍스트로부터 동일한 ID를 갖는 managed 객체를 찾는다.
- 존재하는 경우 업데이트 (UPDATE), 존재하지 않는 경우 DB에 entity 새로 삽입 (INSERT)
- 전달받은 entity가 아닌 새로운 managed 객체를 반환한다.

사실 둘의 용도는 다르다. merge의 경우 detached 상태의 entity를 다시 managed 상태로 복사한 객체를 반환하는 것이다. update의 경우 dirty checking을 사용해도 되기 때문이다. 둘의 용도와 쿼리 발생에 따른 차이를 이해해야 한다. 추가적으로, save에서 항상 반환된 객체를 사용하면 영속성 관련 문제들을 방지할 수 있다.

###  saveAll() 사용

JPA의 saveAll을 사용할 경우 INSERT 쿼리가 반복적으로 발생한다. 이 때 hibernate 다음 옵션을 통해 INSERT 쿼리를 한 묶음으로 보낼 수 있다.
- `hibernate.jdbc.batch_size`
이 옵션은 다건의 쿼리를 묶어서 DB에 보내 NW 통신을 줄이는 용도이다.
실제 multi-value insert로 재작성하는 기능이 아니기 때문에 multi-value 최적화까지 하기 위해서는 connector 레벨의 구문 재작성이 필요하다. 이는 jdbc url에 다음 옵션을 추가해 사용 가능하다.
- jdbc:mysql://localhost:3306/jpa-test?useSSL=false&**rewriteBatchedStatements=true**

#### 영속성 컨텍스트 OOM 문제

사이즈가 너무 큰 경우 영속성 컨텍스트의 메모리 점유율이 높아지고, OOM 발생 가능하므로 적당히 조절해야 한다.

#### 쿼리 혼용

도중에 다른 INSERT가 발생할 경우 Batch 묶음이 끊긴다. 
```java
em.persist(new Entity()); // 1  
em.persist(new Entity()); // 2  
em.persist(new Entity()); // 3    
em.persist(new Orders()); // 1-1, 다른 SQL이 추가 되었기 때문에  SQL 배치를 다시 시작
em.persist(new Entity()); // 1  
em.persist(new Entity()); // 2
```

다음의 옵션으로 문제를 피할 수 있다.
- `hibernate.order_updates: true`
- `hibernate.order_inserts: true`

#### GenerationType.IDENTITY 문제

IDENTITY는 auto increment 방식으로 ID를 자동 증분해 사용한다. 이는 DB insert 이전에 ID 값을 알 수 없다는 특징이 있다. 이에 따라 다음의 문제가 발생한다.
1. ID를 모르면 영속성 컨텍스트에 엔티티를 보관할 수 없다.
2. 따라서 IDENTITY 방식에서는 save 쿼리가 바로 DB로 전송되어야 한다.
3. 이는 saveAll에서도 동일하다. 루프를 돌며 각각의 save가 즉시 전송되어야 한다.
4. 따라서 트랜잭션 쓰기 지연은 동작하지 않는다.
이 결과로 Batch Insert 또한 적용되지 못한다. 

---
- https://cheese10yun.github.io/jpa-batch-insert/#null
- https://joosjuliet.github.io/hibernate_structure/
- https://meetup.toast.com/posts/211
- https://docs.spring.io/spring-data/jdbc/docs/current/reference/html/#is-new-state-detection
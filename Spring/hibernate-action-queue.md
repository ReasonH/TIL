# Hibernate ActionQueue

1:N 관계의 엔티티 예제를 통해 Hibernate batch size 옵션 및 ActionQueue의 동작 방식에 대해 이해한다.

### 사전 지식

먼저 다음의 두 가지를 알아야 한다.
Hibernate는 insert / update / delete 작업 발생 시, ActionQueue 내에 추가하고 flush 호출 시 이를 순차적으로 실행한다.
- hibernate option으로 해당 실행 순서에 대해 정렬을 사용할 수 있으나 기본값은 false임

Hiberante는 Batch 작업 실행 시, 엔티티 이름으로 BatchKey를 만들고 PreparedStatement에 쿼리를 추가한다. 만약 다음 배치 작업에 새로운 BatchKey를 넘겨준다면 이전 쿼리를 실행하고 새로운 PreparedStatement에 쿼리를 추가한다.

#### Insert
```java
    void saveAll() throws Exception {
        long childId = 1;
        for (long i = 1; i <= 5; i++) {
            final ParentEntity parent = ParentEntity.of(i);
            for (long j = 0; j < 3; j++) {
                final ChildEntity child = ChildEntity.of(childId, parent);
                parent.addChild(child);
                childId++;
            }
            parentRepository.save(parent);
        }
        flush();
    }
```

위 쿼리는 ActionQueue 내에 
Parent Insert 1개 + Child Insert 3개를 5회 반복적으로 쌓는다. 이 상태에서 flush 수행 시 다음과 같이 쿼리가 발생한다.

```log
[main] MySQL : [QUERY] insert into parent
[main] MySQL : [QUERY] insert into child 
[main] MySQL : [QUERY] insert into parent
[main] MySQL : [QUERY] insert into child 
[main] MySQL : [QUERY] insert into parent
[main] MySQL : [QUERY] insert into child 
[main] MySQL : [QUERY] insert into parent
[main] MySQL : [QUERY] insert into child 
[main] MySQL : [QUERY] insert into parent
[main] MySQL : [QUERY] insert into child 
```

이 때 hibernate 옵션을 사용하면 Hibernate가 Action Queue를 정렬하고, 아래와 같이 쿼리가 발생한다.
```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          order_inserts: true
```

```
[main] MySQL : [QUERY] insert into parent ... x 5개
[main] MySQL : [QUERY] insert into child ... x 15개
```

#### update

```java
    void updateAll() throws Exception {
        final int parentSize = 5;
        insertTestValues(INSERT_PARENT, parentParameters(parentSize));
        insertTestValues(INSERT_CHILD, childParameters(parentSize, 4));

        final List<ParentEntity> parents = parentRepository.findAll();
        parents.forEach(ParentEntity::plus);

        flush();
    }

@Entity
class Parent {
//...
    public void plus() {
        //...
        children.forEach(ChildEntity::plus);
    }
}
```

이 쿼리는 Parent를 조회 후 update한다. Parent는 내부적으로 1:N 자식 엔티티인 child를 업데이트한다.
여기에서는 별도의 hibernate 옵션을 설정하지 않아도 쿼리가 아래와 같이 실행된다.

```log
[main] MySQL : [QUERY] update parent set ... update parent set ... (총 5개 batch)
[main] MySQL : [QUERY] update child set ... update child set ... (총 20개 batch)
```

그 이유는 다음과 같다.
1. Parent가 우선적으로 영속성 컨텍스트에 등록
2. 이후 각 plus 호출 시 Child가 LAZY loading되어 영속성 컨텍스트에 등록
3. 여기에서 영속성 컨텍스트에는 Parent 5개, Child 20개 엔티티가 순서대로 등록된 상태
4. 애플리케이션에서 각 엔티티 수정
5. flush 시점에 dirty checking
6. 정렬된 엔티티 순서대로 dirty checking되며 Action Queue에 update 액션 등록
7. BatchKey가 순서대로 만들어지기 때문에 2번의 쿼리만 수행

만약 fetch Join을 사용해 엔티티를 한 번에 로딩한다면?
```java
@Query("SELECT DISTINCT p FROM ParentEntity p join fetch p.children ORDER BY p.id")
```

```
[main] MySQL : [QUERY] update parent set 
[main] MySQL : [QUERY] update child set ... update child set ... (총 4개 batch)
[main] MySQL : [QUERY] update parent set 
[main] MySQL : [QUERY] update child set ... update child set ... (총 4개 batch)
[main] MySQL : [QUERY] update parent set 
[main] MySQL : [QUERY] update child set ... update child set ... (총 4개 batch)
[main] MySQL : [QUERY] update parent set 
[main] MySQL : [QUERY] update child set ... update child set ... (총 4개 batch)
[main] MySQL : [QUERY] update parent set 
[main] MySQL : [QUERY] update child set ... update child set ... (총 4개 batch)
```

Parent와 Child가 한 번에 로딩되고, 이에 대한 dirty checking이 수행되며 쿼리가 따로 실행된다.

```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          order_updates: true
```
이 또한, hibernate 옵션을 사용하면 정렬할 수 있다.

```log
[main] MySQL : [QUERY] update parent set ... update parent set ... (총 5개 batch)
[main] MySQL : [QUERY] update child set ... update child set ... (총 20개 batch)
```

---
참고
- https://docs.jboss.org/hibernate/orm/5.4/userguide/html_single/Hibernate_User_Guide.html#batch-jdbcbatch
- https://techblog.woowahan.com/2695/
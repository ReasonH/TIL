# JPQL2

### 경로 표현식

- 상태필드: 단순히 값을 지정하기 위한 필드 → `m.username`
  - 상태 필드는 경로 탐색의 끝이며 더 이상 탐색이 되지 않는다.
- 연관 필드: 연관관계를 위한 필드
  - 단일 값 연관 필드: 대상이 단일 값, ManyToOne, OneToOne → `m.team`
    - 묵시적인 내부 조인 발생, 탐색을 더 진행할 수 있음
  - 컬렉션 값 연관 필드: 대상이 컬렉션 OneToMany, ManyToMany → `m.orders`
    - 묵시적인 내부 조인 발생, **탐색 불가능 → 탐색 위헤서는 명시적 조인 사용**

연관필드는 그냥 다 명시적 조인 사용할 것을 추천한다.

### 패치조인

JPA에서 성능 최적화를 위해 제공하는 JPQL 전용 조인

- inner join처럼 동작, 연관 엔티티가 비어있는 경우 조회 안함
- 엔티티를 조인해서 **함께 1차캐시에 로딩**한다. → 기본 join은 select 절에 명시하지 않으면 join을 수행하더라도 조인된 엔티티 데이터를 로딩하지 않는다.

N:1 관계 fetch join 조회, `select m From Member m join fetch m.team`

1:N 관계 fetch join 조회, `select t From Team t join fetch t.members`

Team을 조회할 때 Member를 fetch join 하면 데이터 중복이 생길 수 있다. 1개의 팀과 N명의 멤버가 연관관계를 맺고 있으면 팀까지 N개가 조회된다.

→ 문제 해결을 위해 JPQL에서 distinct 제공, 이는 SQL에 distinct를 추가할 뿐 아니라 가져오는 **엔티티에도 distinct를 적용**해서 애플리케이션 레벨에서 같은 식별자를 가진 Team 엔티티를 제거한다.

### 패치조인 한계

- 패치 조인 대상에는 별칭을 줄 수 없음
  ⇒ fetch를 연쇄적으로 사용해야할 경우 아니면 딱히 사용할 필요가 없다.
- 둘 이상의 **컬렉션**은 패치 조인 할 수 없다. (1:N)
- 컬렉션을 페치 조인 하는경우 페이징 불가능하다.

### **컬렉션 fetch join 문제**

1(Team):N(Member)를 가정한다.

컬렉션 패치 조인 + 페이징 쿼리를 사용하는 경우, 1:N 조인으로인해 중복이 생긴 데이터들을 페이징 해야하기 때문에 문제가 생긴다. 아래와 같은 조회 결과는 어떻게 페이징 해야할까?

```java
--> Team(1), Member(1)
--> Team(1), Member(2)
--> Team(1), Member(3)
--> Team(2), Member(4)
--> Team(2), Member(5) ...
```

- 결국 정확한 페이징을 위해서는 Team만을 조회해야 한다.
- 만약 각 Team의 멤버를 조회할 필요가 있다면? → 필연적으로 N+1 문제가 발생하게 된다.

@BatchSize를 사용한다면 이 문제를 1+1 쿼리로 해결할 수 있게 되는 것이다.

**@BatchSize**

여러 개의 프록시 객체를 조회할 때 `WHERE` 절이 같은 여러 개의 `SELECT` 쿼리들을 하나의 `IN` 쿼리로 만든다. → LAZY 로딩 대상 객체를 한번(최대 배치 사이즈) 조회에 모두 긁어온다.

tip. 이 때, **영속성 컨텍스트에 존재하는** 모든 team의 id를 이용한다.

```sql
SELECT * FROM member WHERE member.team_id = 1
SELECT * FROM member WHERE member.team_id = 2
to
SELECT * FROM member WHERE member.team IN (1, 2)
```

### JPQL에서의 엔티티 사용

- JPQL에서 엔티티를 직접 사용하면 SQL에서는 자동으로 엔티티 기본 키 값을 사용
  - `select m From Member m where m = :member`
  - 실행된 SQL ⇒ `select m From Member m where m.id = ?`
- 이는 외래키에서도 적용된다.
  - `select m From Member m where m.team = :team`
  - 실행된 SQL ⇒ `select m From Member m where m.team_id = ?`

### 조회 결과의 DTO 매핑

여러 테이블을 조회해서 엔티티가 가진 모양이 아닌 전혀 다른 결과를 내야 하는 경우, 패치 조인 보다 일반 조인을 사용하고 필요한 데이터들만 조회해서 DTO로 반환하거나 애플리케이션에서 조합해서 사용한다.

### 벌크연산

JPA가 기본 제공하는 saveAll, deleteAll 등의 함수들은 기본적으로 loop를 돌며 수행되기 때문에 다건의 DML쿼리에 대한 성능 저하 요소가 다분하다. 이를 극복하기 위해 한 번에 여러 row를 변경하는 벌크 연산을 사용한다.

- @Modifying 사용 필수
- UPDATE, DELETE 지원
- 벌크 연산은 영속성 컨텍스트를 무시하고 DB에 직접 쿼리한다. 이로인해 문제가 생길 수 있음
  1. Member 조회 → 1차 캐시 로딩
  2. 업데이트 쿼리 수행 전 FLUSH (AUTO FLUSH 모드)
  3. Member의 age 벌크 업데이트 쿼리 DB에 직접 수행
  4. Member 재조회 → 1차 캐시에서 로딩 (db조회 발생하지 않음)
  5. Member에서 age 조회하면 업데이트 되지 않은 값이 조회됨
- 해결 방법 (2가지)
  - 영속성 컨텍스트에 값 넣지 않은 상태에서 벌크 연산 먼저 실행 → 애초에 1차 캐시 문제없음
  - 벌크 연산 수행 후 영속성 컨텍스트 초기화
    - 자동 옵션: `@Modifying(clearAutomatically = true)`

잠깐, 기본 함수를 사용하는 것은 안되나?

```java
deleteInBatch(Iterable<T> entities)
```

물론, 선택할 수 있는 옵션이지만 삭제 대상 엔티티가 모두 메모리에 올라와야 하기 때문에 문제가 될 수 있다.

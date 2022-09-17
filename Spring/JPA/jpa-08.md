# JPQL

JPA는 SQL을 추상화한 JPQL이라는 객체지향 쿼리언어를 제공한다. 이는 SQL과 유사한 문법을 제공하며 엔티티 객체를 대상으로 하는 쿼리이다.

한계점: JPQL은 단순 String이기 때문에 동적 쿼리를 만들 수 없음

**Criteria**

```java
CriteriaBuilder cb = em.getCriteriaBuilder();
CriteriaQuery<Member> query = cb.createQuery(Member.class);
Root<Member> m = query.from(Member.class);

CriteriaQuery<Member> cq = query.select(m).where(cb.equal(m.get("username"), "kim"));
List<Member> resultList = em.createQuery(cq).getResultList();
```

이는 동적쿼리를 짜기 훨씬 편리하며 컴파일 타임에 오류체크를 해주어 안정성이 보장된다.

한계점: 복잡해서 알아보기가 힘들며, SQL스럽지 않다.

**QueryDSL**

문자가 아닌 자바코드로 JPQL을 작성한다. 위의 한계점들을 극복

**네이티브 SQL**

- JPA가 제공하는 SQL을 직접 사용하는 기능
- JPQL로 해결 불가능한 DB 의존적 기능 (ex. 특정 DB만 사용하는 SQL 힌트)

**JDBC 직접 사용**

- JDBC 커넥션이나 스프링 JdbcTemplate, 마이바티스 등 함께 사용 가능
- JPA 우회하는 경우 영속성 컨텍스트를 적절한 시점에 직접 flush 해야함

### JPQL 프로젝션

- `SELECT m FROM Member m` 엔티티 프로젝션: 엔티티 조회
- `SELECT m.team FROM Member m` 연관 엔티티 프로젝션: 연관 엔티티 조회 (join 쿼리 발생)
- `SELECT o.address FROM Order o` 임베디드 타입 프로젝션: 임베디드 타입 해당 필드만 조회
- `SELECT m.username, m.age FROM Member m` 스칼라 타입 프로젝션: 선택 값 필드만 조회

### JPQL 서브쿼리

- EXIST, ALL, ANY, SOME, IN 등등.. 지원
- JPA는 WERE, HAVING 절에서만 서브쿼리 가능 + 하이버네이트에서 SELECT절도 지원
- **FROM 절의 서브쿼리는 JPQL에서 불가능**
- 조인으로 풀 수 있으면 풀어서 해결한다.
  - 정 안되면 쿼리를 두번날리거나 애플리케이션에서 조작하거나 방법은 여러가지 있다.

### JPQL 조건식

- CASE, COALESCE, NULLIF 등 존재

### JPQL 함수

JPQL은 기본 함수 및 사용자 함수를 제공한다. 또한, Dialect별 DB함수들도 제공되고 있다.

**사용자 함수란?**

Hibernate에서 제공하는 방언(H2Dialect, MySQLDialect...)은 특정 DB에 종속된 함수는 따로 제공되지 않는다. 따라서 특정 DB에 종속된 함수를 사용하기 위해서는 사용자 함수를 등록해야 한다.

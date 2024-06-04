Repository 개선 작업을 진행하며 예상치 못한 쿼리가 발생하는 케이스가 있었다. 관련 내용을 찾아보고 정리했다.

### 예제

JPA에서 ManyToOne의 연관 관계가 있는 Member와 Team이 있다고 가정하자.

```java
@Entity
@Getter
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;
}

@Entity
@Getter
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;
}
```

이 때 JPA의 method query를 사용해 특정 Team에 속한 Member를 조회하는 경우, 다음의 두 가지 방식을 사용할 수 있다.

```java
interface MemberRepository extends JpaRepository<Member, Long> {

	Optional<Member> findByTeam(Team team);
	
	Optional<Member> findByTeamId(Long id);
}
```

데이터가 정확한 상황이라면 두 쿼리는 항상 같은 값을 반환할 것이다. 그러나 실제 발생하는 쿼리에는 차이가 있다.

#### 첫 번째 쿼리 `Optional<Member> findByTeam(Team team);`

```java
Hibernate: 
    select
        member0_.id as id1_1_,
        member0_.name as name2_1_,
        member0_.team_id as team_id3_1_ 
    from
        member member0_ 
    where
        member0_.team_id=?
```

#### 두 번째 쿼리 `Optional<Member> findByTeamId(Long id);`

```java
Hibernate: 
    select
        member0_.id as id1_1_,
        member0_.name as name2_1_,
        member0_.team_id as team_id3_1_ 
    from
        member member0_ 
    left outer join
        team team1_ 
            on member0_.team_id=team1_.id 
    where
        team1_.id=?
```

얼핏 보면 `findByTeamId`도 첫 번째와 동일한 쿼리가 나갈 거라고 예상했을 것이다. (내가 그랬다.) 그러나 예상과 다르게 LEFT OUTER JOIN이 발생한다. id 값을 이용해 WHERE 절을 사용할 수 있을 것 같다. 그럼에도 LEFT OUTER JOIN 쿼리가 만들어지는 이유는 무엇일까?

### 다른 쿼리가 발생하는 이유

그 이유는 spring-data의 Query Method에 관한 Docs를 살펴보면 알 수 있다.

**Property expressions can refer only to a direct property of the managed entity,** as shown in the preceding example. At query creation time, you already make sure that the parsed property is a property of the managed domain class. However, **you can also define constraints by traversing nested properties.** Consider the following method signature:

`List<Person> findByAddressZipCode(ZipCode zipCode);`

Assume a Person has an Address with a ZipCode. In that case, the method creates the x.address.zipCode property traversal. The resolution algorithm starts by interpreting the entire part (AddressZipCode) as the property and checks the domain class for a property with that name (uncapitalized). If the algorithm succeeds, it uses that property. If not, the algorithm splits up the source at the camel-case parts from the right side into a head and a tail and tries to find the corresponding property — in our example, AddressZip and Code. If the algorithm finds a property with that head, it takes the tail and continues building the tree down from there, splitting the tail up in the way just described. If the first split does not match, the algorithm moves the split point to the left (Address, ZipCode) and continues.

강조한 부분만 요약을 하자면 다음과 같다.

- 쿼리 메소드는 managed entity (**일반적으로 @Entity 주석이 표시된 클래스**)의 직접 정의된 속성만 사용할 수 있다.
- 또한, 중첩된 속성을 탐색하여 사용할 수 있다. (결국 이 또한 직접 정의된 속성을 탐색하여 사용하는 방식이다.)

`List<Person> findByAddressZipCode(ZipCode zipCode);` 이 예시에서 `Address`는 `Person`에 정의된 연관 관계 맵핑 속성이고 `ZipCode`는 `Address` 내부의 속성이라 생각하면 된다.

이런 개념을 기반으로 우리의 예제로 돌아와 보자.

`findByTeamId` 쿼리를 사용하는 경우, JPA는 Team entity를 조회하고, 여기에서 id 값이 일치하는 항목을 찾는 쿼리를 만들게 된다. 우리가 사용하는 id라는 속성이 Team entity에 정의되어 있기 때문이다. 이를 위해서는 결국 JOIN을 사용할 수 밖에 없다.

### Summary

- DB에 teamId라는 컬럼을 지정했다고. `findByTeamId`이 teamId를 where 절로 조회할 거라는 생각은 오산이다. JPA는 entity에 정의된 속성만 사용할 수 있다.
- JPA를 이용할 때는 항상 실제 쿼리가 예상대로 나가는지 확인해야 한다.
- P.S) `@ManyToOne(optional = false)` 인 경우 LEFT OUTER JOIN 대신 INNER JOIN이 발생한다.
---
참고

- [https://docs.spring.io/spring-data/commons/reference/repositories/query-methods-details.html#repositories.query-methods.query-property-expressions](https://docs.spring.io/spring-data/commons/reference/repositories/query-methods-details.html#repositories.query-methods.query-property-expressions)
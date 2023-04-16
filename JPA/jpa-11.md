# 번외, 값 타입

### 엔티티 타입

데이터가 변해도 식별자로 지속해서 추적 가능

### 값 타입

식별자가 없고 값만 있으므로 변경시 추적 불가

- 기본값 타입: 기본 타입래퍼 클래스 및 String
  - 생명주기를 엔티티에 의존
  - 기본 타입은 값을 복사하기 때문에 어차피 공유되지 않음
  - 래퍼클래스 및 String은 값을 참조하기 때문에 공유는 되지만 변경이 불가능
    - Integer.setValue 이런게 없음
- 임베디드 타입: 복합 값 타입 (주소 정보와 같은)
- 컬렉션 값 타입

### 임베디드 타입

- 임베디드 타입 또한 기본적으로 값 타입

```java
@Entity
public class Member {

    @Id
    @GeneratedValue
    @Column(name = "MEMBER_ID")
    private Long id;

    @Column(name = "USERNAME")
    private String name;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private String city;
    private String street;
    private String zipcode;
}
/*==================================================================*/
@Entity
public class Member {

    @Id
    @GeneratedValue
    @Column(name = "MEMBER_ID")
    private Long id;

    @Column(name = "USERNAME")
    private String name;

		@Embedded
    private Period period;

		@Embedded
    private Address address;
}

@Getter
@Setter
@NoArgsConstructor
@Embeddable
public class Period {
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}

@Getter
@Setter
@NoArgsConstructor
@Embeddable
public class Address {
    private String city;
    private String street;
    private String zipcode;
}
```

ㄴ**코드에서 주석 위 아래의 테이블 매핑은 동일하다.**

장점

- 재사용성, 높은 응집도
- 도메인 모델링 편의성
- 값 타입만 사용하는 의미 있는 메소드 생성 가능

@AttributeOverrides 사용하면 중복된 임베디드 값 타입도 사용가능

### 값 타입과 불변객체

값 타입은 항상 불변객체로 생성해야만 참조로 인한 사이드이펙트를 피할 수 있다.

- 생성 시점 이후 변경 불가능하도록 수정자를 만들지 않음 → like Integer, String

### 값 타입 컬렉션

RDB에서는 기본적으로 컬렉션을 테이블 안에 넣을 수 없다. 따라서 컬렉션 값 타입을 사용하는 경우RDB에서는 이를 위한 별도의 테이블을 만들어 매핑해야한다.

```java
@Entity
public class Member {

    @Id
    @GeneratedValue
    @Column(name = "MEMBER_ID")
    private Long id;

    @Column(name = "USERNAME")
    private String name;

    @Embedded
    private Address homeAddress;

    @ElementCollection
    @CollectionTable(name = "FAVORITE_FOOD", joinColumns =
            @JoinColumn(name = "MEMBER_ID")
    )
    @Column(name = "FOOD_NAME")
    private Set<String> favoriteFoods = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "ADDRESS", joinColumns =
            @JoinColumn(name = "MEMBER_ID")
    )
    private List<Address> addressesHistory = new HashSet<>();
}
```

위와같이 하는 경우 알아서 값 타입 컬렉션을 참조해 FAVORITE_FOOD, ADDRESS 테이블을 만든다.

```java
Member member = new Member();
member.setName("유혁");
member.setHomeAddress(new Address("city", "street", "zipcode"));
member.getFavoriteFoods().add("치킨");
member.getFavoriteFoods().add("피자");
member.getAddressesHistory().add(new Address("oldcity", "street", "zipcode"));
member.getAddressesHistory().add(new Address("oldcity2", "street", "zipcode"));

em.persist(member);
```

- 저장: 별도의 persist하지 않아도 생명주기는 Member에 의존된다. 즉 영속성 전이 + 고아객체 제거 기능을 필수로 가지게 된다.
- 조회: 컬렉션 테이블들은 기본적으로 지연로딩이다.

```java
member.getFavoriteFoods().remove("치킨");
member.getFavoriteFoods().add("한식");

member.getAddressesHistory().remove(new Address("oldcity", "street", "zipcode"));
member.getAddressesHistory().add(new Address("newcity", "street", "zipcode"));
```

- 수정: 컬렉션 요소 지우고 추가해야함
  - 컬렉션 요소가 값 타입 객체인 경우 equals가 구현되어 있어야 remove 가능하다.
  - 값 타입 객체로 이루어진 컬렉션에 변경 사항이 생기는 경우 주인 엔티티와 연관된 모든 데이터를 지우고 다시 생성함

### 결론

- 수정 과정에서 복잡함이 다소 생긴다.
- 결국 실무에서는 값 타입 컬렉션 보다는 값 타입의 엔티티 승급을 통한 일대다 관계를 고려해야 한다.
- 엔티티 내부에 값 타입 객체를 하나 두어서 활용 → 범용성이 높아진다.
- 영속성 전이 및 고아 객체 제러를 사용해서 값 타입 컬렉션처럼 사용한다.
- 그러면 값 타입 컬렉션을 사용하는 경우는? A. **매우 단순해서 단독 조회가 필요없는 수준인 경우**

### 추가 팁

- 값 타입의 equals 메서드 재정의 시, getter를 활용한다. 필드접근을 사용하는 경우 프록시 객체가 들어왔을 때 문제가 될 수 있음

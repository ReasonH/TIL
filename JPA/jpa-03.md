# 엔티티 매핑

### 필드와 컬럼 매핑

@Column / @Id / @Entity / @Table… → 생략

@Enumerated: Enum 타입 필드에 사용, 항상 string 타입 지향

@Temporal: Date 타입

- TemporalType 종류: Date, Time, TimeStamp

@Transient: DB매핑하지 않는 필드

### 기본 키 매핑

- IDENTITY
  - 기본 키 생성을 DB에 위임
  - ex) MySQL의 AUTO\_ INCREMENT
  - JPA는 보통 트랜잭션 커밋 시점에 INSERT SQL 실행
  - AUTO\_ INCREMENT는 데이터베이스에 INSERT SQL을 실행한 이후에 ID 값을 알 수 있음
  - IDENTITY 전략은 em.persist() 시점에 즉시 INSERT SQL 실행한 후 DB에서 식별자를 조회 → 그 이후 커밋
- SEQUENCE / TABLE
  - SEQUENCE - DB 시퀸스 객체 사용 (@SequenceGenerator 필요)
  - TABLE - 키 생성 전용 테이블을 사용 (@TableGenerator 필요)
  - 이 둘은 allocationSize가 중요하다. 매번 DB 통신하는 비용을 줄이기 위해 한번에 시퀸스를 올려놓고 메모리에서 해당 시퀸스 값까지 사용
- AUTO
  - 방언에 따라 매핑 전략 선택한다. ex) MySQL dialect인 경우 IDENTITY

### Hibernate가 지원하는 스키마 자동 생성

실무에서는 validate 정도 사용한다.

hibernate.hbm2ddl.auto 지원 기능

- create: 기존 테이블 삭제 후 재생성
- create-drop: 종료시점에 테이블 drop
- update: 변경분만 반영
- validate: 엔티티 - 테이블 정상매핑 확인
- none: 사용x

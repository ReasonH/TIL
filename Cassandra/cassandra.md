# Apache Cassandra

Cassandra 관련, 필요할 때마다 공부했던 부분들을 정리

## 1. Cassandra Concept
### 특징
- Key-Value 형태의 NoSQL DB
- 자유로운 노드 장비 추가, 제거로 확장성 및 HA에 최적화된 분산 데이터 저장소
- 복구 정책으로 안정성 확보
- 조회 쿼리에 비교적 제약이 존재 (ex. Partition Key 사용하지 않을 시 성능 이슈) 

### 구성
![](cassandra-1.png)

Cassandra는 기본적으로 여러 노드로 구성된 Ring 구조를 갖고 있다. 각 노드는 설정을 통해 고유의 hash값 범위를 부여받는다. 여기에 데이터 Partition Key (Cassandra Data Layer의 Row Key)의 Hash값을 기준으로 데이터를 분산시킨다. 같은 Partition Key를 갖는 Row들은 같은 디스크에 저장된다.

### 데이터 저장 방식
- Memtable 저장 → commit logging → 가득 차면 → 디스크의 Sorted String Table에 저장
- 즉 Memtable 저장 시점부터 정렬되어있는 Append only 방식

## 2. CDL / CQL

![](cassandra-2.png)
 
 위는 Cassandra Data Layer를 나타낸다.
1. Keyspace
2. Table
3. Row
4. Column Name - Column Value

형태로 마치 일반적인 RDBMS 구조와 유사해 보이지만, Column Name과 Value는 일반적인 RDBMS의 컬럼과 의미가 다르다. Cassandra에서의 Row는 특정 스키마에 종속되지 않으며 Column이 계속 늘어날 수 있다. 이는 Cassandra가 Key-Value NoSQL 저장소인 이유이기도 하다.

![](cassandra-3.png)

그림은 CQL 테이블로 CDL을 매핑한 결과이다.

CQL은 Cassandra Query Language의 약어로 CDL을 추상화한다. 이 그림에서의 Column은 CDL과 달리 RDB Table의 Attribute와 매치된다. 애플리케이션 레벨에서 개발하는 경우 대부분 CQL을 이용하는데, 둘은 분명한 차이가 있기 때문에 이애 대해 인지할 필요가 있다.

### CQL Key 용어 정리
둘을 비교하기 전 CQL에서 지칭하는 Key 개념들을 먼저 익혀야 한다.
1.  Partition key
    - CQL / CDL에서 Cassandra에 데이터를 분산 저장하기 위한 unique한 key
    - table 구성 시 반드시 1개 이상이 지정되어야 함
    - CDL의 Row key는 Partition Key를 통해 결정된다.
2.  Cluster key
	- CDL에서 Row에 속한 Column들을 정렬시키기 위한 기준이 되는 키
4.  Primary key
    - CQL 테이블에서의 각 row를 각자 unique하게 결정해주는 기준을 담당
    - 최소 1개 이상의 partition key와 0개 이상의 cluster key로 구성된다.
5.  Composite key
    - 2개 이상의 CQL Column으로 이루어진 primary key
6.  Composite partition key
    - 2개 이상의 CQL Column으로 이루어진 partition key


### 데이터 조회 결과 비교
테이블을 생성해서 둘의 차이를 확인해본다.
- 여기에서 code는 Partition Key가 되며 location은 Cluster Key가 된다.
```sql
CREATE TABLE test_keyspace.test_table_ex1 ( 
    code text, 
    location text, 
    sequence text, 
    description text, 
    PRIMARY KEY (code, location)
);

INSERT INTO test_keyspace.test_table_ex1 (code, location, sequence, description ) VALUES ('N1', 'Seoul', 'first', 'AA');
INSERT INTO test_keyspace.test_table_ex1 (code, location, sequence, description ) VALUES ('N1', 'Gangnam', 'second', 'BB');
INSERT INTO test_keyspace.test_table_ex1 (code, location, sequence, description ) VALUES ('N2', 'Seongnam', 'third', 'CC');
INSERT INTO test_keyspace.test_table_ex1 (code, location, sequence, description ) VALUES ('N2', 'Pangyo', 'fourth', 'DD');
INSERT INTO test_keyspace.test_table_ex1 (code, location, sequence, description ) VALUES ('N2', 'Jungja', 'fifth', 'EE');
```

##### CQL
```sql
Select * from test_keyspace.test_table_ex1;
```
먼저 CQL로 조회를 수행하면

![](cassandra-5.png)

5개의 Row와 4개의 Column으로 이루어진 데이터들이 출력된다.

##### CDL
```sql
use test_keyspace;
list test_table_ex1;

Using default limit of 100
Using default cell limit of 100
```
cli를 통해 CDL 상태를 확인해본다.
```sql
RowKey: N1
=> (name=Gangnam:, value=, timestamp=1452481808817684)
=> (name=Gangnam:description, value=4242, timestamp=1452481808817684)
=> (name=Gangnam:sequence, value=7365636f6e64, timestamp=1452481808817684)
=> (name=Seoul:, value=, timestamp=1452481808814357)
=> (name=Seoul:description, value=4141, timestamp=1452481808814357)
=> (name=Seoul:sequence, value=6669727374, timestamp=1452481808814357)
-------------------
RowKey: N2
=> (name=Jungja:, value=, timestamp=1452481808833644)
=> (name=Jungja:description, value=4545, timestamp=1452481808833644)
=> (name=Jungja:sequence, value=6669667468, timestamp=1452481808833644)
=> (name=Pangyo:, value=, timestamp=1452481808829751)
=> (name=Pangyo:description, value=4444, timestamp=1452481808829751)
=> (name=Pangyo:sequence, value=666f75727468, timestamp=1452481808829751)
=> (name=Seongnam:, value=, timestamp=1452481808823137)
=> (name=Seongnam:description, value=4343, timestamp=1452481808823137)
=> (name=Seongnam:sequence, value=7468697264, timestamp=1452481808823137)

2 Rows Returned.
Elapsed time: 67 ms.
```
`value=,`으로 표시되는 Column은 잘못된 데이터가 아니라, CQL을 내부적으로 변환하여 사용되는 데이터이다. 이를 무시하고 다른 부분을 살펴본다.

1. CQL Row와 CDL Row는 의미가 다르다. Cassandra는 실제 데이터를 Row Key 기준으로 분산한다.
2. CDL Row는 Column의 길이가 각각 다르다.
3. CDL에서의 Column name은 CQL Cluster Key의 value와 Primary Key에 속하지 않는 Column **Name**의 ":" 조합이다. 
4. 하나의 CDL Row 내에서는 Cluster Key를 통해 데이터가 정렬된다.


## 3. 데이터 삭제 프로세스
Cassandra에서의 삭제는 INSERT 또는 UPSERT로 취급된다. DELETE 명령을 수행 시 파티션에는 tombstone이라는 삭제 마킹용 데이터가 추가되기 때문이다. tombstone 데이터는 만료 시간을 갖는데, 만료 이후에는 Cassandra의 표준 압축 프로세스에 의해 제거된다.

### 분산 시스템에서의 삭제

클러스터링 환경에서 Cassandra는 두 개 이상의 노드에 데이터 복제본을 저장할 수 있다. 이는 데이터 손실을 방지하지만, 동시에 삭제 프로세스의 복잡도를 높이는 원인이 된다.

#### 좀비 문제

**Tombstone이 없이 삭제하는 경우**

일부 노드에서 삭제가 실패한 경우에도 QUORUM을 통해 삭제는 성공한 것으로 간주된다. 삭제가 실패한 노드에 읽기 요청이 오는 경우 이미 사라진 데이터 (aka 좀비)가 반환될 수 있다.

![](cassandra-4.png)

**Tombstone을 이용해 삭제하는 경우**

물론 Tombstone을 이용하더라도 좀비 데이터 문제는 발생한다.

1.  특정 노드 A에 문제가 발생
2.  다른 노드로부터 Tombstone 수신 실패, 이전 버전 레코드를 유지
3.  나머지 노드들에서 Tombstone과 레코드가 삭제
4.  노드 A 복구와 함께 이전 버전 레코드를 다른 노드들에 전파
5.  좀비 레코드 발생

이런 문제를 방지하기 위해 Tombstone은 자체적으로 expire time을 갖고 있다. 이 시간 동안 문제가 발생한 노드는 Tombstone을 정상적으로 복구하고, 처리할 수 있게 된다.

### 삭제 후 동작

노드 읽기는 항상 최신 값을 우선으로 한다. 일부 노드에 Tombstone이 있더라도 다른 노드에 더 최근 변경사항이 있다면 최종 응답은 최신 값이다.

### 삭제 추가 정보

-   삭제 표시의 만료 시간은 생성 시간에 table_options의 gc_grace_seconds을 더한 값
-   테이블 전체에 대해 TTL을 설정할 수 있으며 이는 위의 삭제 처리 프로세스를 따른다. TTL이 초과된 레코드는 별도의 Tombstone 처리 없이 DB에서 즉시 지워진다.
-   `DROP KEYSPACE`, `DROP TABLE` 등은 tombstone 없이 즉시 제거된다.
-   Partition 단위로 제거하는 경우 tombstone은 1개만 생성된다. (row 갯수만큼 생성되지 않음)

---
##### 참고
- https://meetup.toast.com/posts/58](https://meetup.toast.com/posts/58)
- https://nicewoong.github.io/development/2018/02/11/cassandra-internal/
- https://meetup.toast.com/posts/65
- https://thelastpickle.com/blog/2016/07/27/about-deletes-and-tombstones.html
- https://docs.datastax.com/en/cassandra-oss/3.0/cassandra/dml/dmlAboutDeletes.html
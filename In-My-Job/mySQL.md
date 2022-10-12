# MySQL
MySQL 사용 중 봤던 이것 저것 정리


### 인덱스 설정 TIP
-   적절한 길이의 키를 설정
-   여러 컬럼으로 구성 시, 카디널리티 (희소성) 높은 순서대로
-   인덱스를 타기 위해서는 인덱스의 첫 번째 컬럼은 필수적으로 쿼리에 포함되어야 함
-   Between, like 등 범위 조건을 사용하는 경우 해당 컬럼 이후의 인덱스 컬럼은 조회에 사용되지 않는다.

##### 범위 조회 관련
```sql
SELECT * FROM Item
WHERE order_date BETWEEN 123 AND 145 
	AND user_no = 23512;
```
위와 같은 쿼리를 사용할 때
1.  인덱스가 order_date, user_no로 구성된 경우 user_no의 인덱스 컬럼 효과를 볼 수 없다.
	1.  order_date로 정렬되어 있는 항목에 대해 범위 조회
	2.  user_no로 일치 조회를 해도 조회 범위를 줄이지 못함
2.  인덱스가 user_no, order_date로 구성된 경우 인덱스 컬럼 효과를 본다.
	1.  user_no로 정렬, 하위에 order_date별로 정렬이 되어있기 때문에 범위가 줄어든다.

    

### 서브쿼리 vs JOIN
MySQL 5.6 미만 버전에서는 서브쿼리 최적화에 상당히 문제가 많았다. 5.6 버전 이후로 문제가 개선되었지만, 여전히 제약조건이 많다. 회사 시스템의 경우 8버전 이상을 사용하기 때문에 많은 최적화가 이루어졌을테지만, 모든 부분을 인지하지 못한다면 JOIN을 우선적으로 검토하자.
- 8.0+에서 최적화가 제한되는 서브쿼리 조건에 대해 알아보자 (TBD)


### 가끔 사용하는 MySQL 쿼리 및 커맨드

테이블 복사 (constraint 포함)
```sql
CREATE TABLE newtable LIKE oldtable; 
INSERT INTO newtable SELECT * FROM oldtable;
```

프로세스 리스트 확인
```sql
show processlist;
```
현재 커넥션 리스트들과 각 커넥션에서 수행되는 process의 sql 및 상태를 보여준다.
문제 상황에서는 해당 프로세스에 `kill`  을 이용해 프로세스를 죽인다. (주의 필요)


INFORMATION_SCHEMA
```sql
SELECT * FROM information_schema.`TABLES`
WHERE information_schema.`TABLES`.TABLE_SCHEMA = 'test';
```
DB 메타정보를 읽어온다. 다양한 메타정보를 포함하기에 유용하게 사용 가능하다. 
나의 경우 배치 서버 개발 시, 테스트를 진행하며 대량의 데이터를 INSERT / DELETE하는 테스트를 수행할 때 DB 상태 체크를 위해 유용하게 사용했다.
(다만 싱크가 일부 맞지 않아서 정확한 실시간 통계가 제공되지는 않는 것 같다)

### EXPLAIN 쿼리
[[explain | 상세 설명 ]]

### INSTANT ADD
MySQL 8.0버전 (InnoDB) 이상에서는 Instant ADD column을 지원한다.
https://dev.mysql.com/blog-archive/mysql-8-0-innodb-now-supports-instant-add-column/

Online DDL 작업에서 사용할 수 있는 기능인데, Long Transaction 없이도 실시간으로 meta data만을 변경해 column 추가를 가능할 수 있게 한다. 다만 마지막 열이 아니라 중간에 넣는 경우 INPLACE 방식으로 동작한다고 한다. 이는 잠시동안 배타적 메타 데이터 잠금을 발생시킬 수 있다.

ALGORITHM을 따로 설정하지 않는 경우 자동으로 INSTANT -> INPLACE -> COPY 순으로 선택한다.

- https://nomadlee.com/mysql-explain-sql/
- https://jojoldu.tistory.com/520
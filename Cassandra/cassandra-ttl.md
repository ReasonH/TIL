# Cassandra TTL

Cassandra 데이터 제거 작업 관련 TTL에 대해 알아보았다. out dated 및 제거 처리가 필요한 데이터들은 TTL을 적용하면 편리하게 처리할 수 있을 것 같다.

## 개요

카산드라에서는 테이블 또는 컬럼에 TTL을 설정할 수 있다.

TTL 적용 시, 레코드는 스토리지에서 즉시 지워지지 않는다. 이는 Delete 쿼리를 사용할 때와 동일하게 tombstone 마킹이 먼저 발생하며, gc_period에 도달해 compaction이 발생할 때만 물리적으로 지워진다.
-   삭제 대상 데이터는 grace period 계산을 위한 데이터를 더 사용한다. 즉 compaction 발생 전까지 리소스를 더 소모할 수 있다.
-   데이터는 즉시 지워지지 않기 때문에 grace period 전까지 계속 조회에 사용된다. (쿼리 결과에 포함되지 않더라도 조회 범위에 포함되기 때문에 생각지 못한 성능 이슈가 생길 수 있다.)

### 사용 방법

테이블 단위의 적용, 컬럼 단위의 적용 모두 가능하다.

- 컬럼 단위 적용: INSERT / UPDATE 가능
- 테이블 단위 적용: CREATE / ALTER 가능

테이블 단위로 TTL을 적용하는 경우 모든 열에 대해 default_time_to_live 만큼의 TTL이 적용된다. 이 때 ALTER Table을 사용하는 경우 **새로 적재되는 레코드에 대해서만 TTL이 적용된다.**

### 실습

cql에서는 `tracing on`을 통해 쿼리에 대한 history를 자세히 볼 수 있다.

준비된 temp table은 데이터 조회 시 기본적으로 5개 row가 확인된다. channelid와 temp1은 row key (partition key)이며 나머지 temp2, temp3는 일반 컬럼이다.

```shell
cqlsh:mtalk> select * from temp where channelId = 100;

 channelid | temp1 | temp2 | temp3
-----------+-------+-------+-------
       100 |   100 |   100 |   100
       100 |   200 |   200 |   200
       100 |   300 |   300 |   300
       100 |   400 |   400 |   400
       100 |   500 |   500 |   500
```

테이블에 TTL을 적용한다. 지금부터 이 테이블에 삽입되는 데이터는 TTL이 적용된다.

```yaml
alter table TEMP with default_time_to_live = 30;
```

2개의 row를 추가 삽입한다. (실제로는 column 데이터지만 cql row를 의미한다)

```yaml
insert into TEMP(channelId, temp1, temp2, temp3) values (100, 800, 1, 1);
insert into TEMP(channelId, temp1, temp2, temp3) values (100, 900, 1, 1);
```

데이터 및 TTL 확인

```shell
cqlsh:mtalk> select * from temp where channelId = 100;

 channelid | temp1 | temp2 | temp3
-----------+-------+-------+-------
       100 |   100 |   100 |   100
       100 |   200 |   200 |   200
       100 |   300 |   300 |   300
       100 |   400 |   400 |   400
       100 |   500 |   500 |   500
       100 |   800 |     1 |     1
       100 |   900 |     1 |     1

cqlsh:mtalk> select TTL(temp2), TTL(temp3) from temp where channelId = 100;

 ttl(temp2) | ttl(temp3)
------------+------------
       null |       null
       null |       null
       null |       null
       null |       null
       null |       null
         23 |         23
         26 |         26

... 30초 뒤 확인

 ttl(temp2) | ttl(temp3)
------------+------------
       null |       null
       null |       null
       null |       null
       null |       null
       null |       null
```

이제 select 쿼리를 날려본다.

```shell
cqlsh:mtalk> select * from temp where channelId = 100;

 channelid | temp1 | temp2 | temp3
-----------+-------+-------+-------
       100 |   100 |   100 |   100
       100 |   200 |   200 |   200
       100 |   300 |   300 |   300
       100 |   400 |   400 |   400
       100 |   500 |   500 |   500

Read 5 live rows and 4 tombstone cells [ReadStage-3] | 2022-10-28 16:29:37.709000 | 127.0.0.1 |           1166 |
```

5개의 live row와 4개의 tombstone 셀이 확인된다.

여기에서 tombstone이 4개인 이유는 각 컬럼 별로 tombstone이 유지되기 때문이다.

row를 기준으로는 2개가 삭제되었지만 실제 컬럼은 key column인 `channelid`, `temp1`을 제외한 총 4개가 삭제된 것이다.

---
참고
-  https://docs.datastax.com/en/cql-oss/3.3/cql/cql_using/useExpire.html

# EXPLAIN
EXPLAIN 쿼리는 간단한 쿼리 분석에 매우 유용한 명령어다. 특정 쿼리의 맨 앞에 explain만 붙이면 간단하게 동작한다.
```sql
EXPLAIN <쿼리>
```

##### 항목 별 의미
| 항목          | 설명                                                   |
| ------------- | ------------------------------------------------------ |
| id            | select를 구분하는 번호                                 |
| table         | 참조하는 테이블                                        |
| select_type   | select의 타입                                          |
| type          | 조인, 조회 타입                                        |
| possible_keys | 데이터를 조회할 때 DB에서 사용할 수 있는 인덱스 리스트 |
| key           | 실제 사용할 인덱스                                     |
| key_len       | 사용할 인덱스 길이                                     |
| ref           | key안의 인덱스와 비교하는 컬럼                         |
| filtered      | 필터율                                                 |
| rows          | 쿼리 수행 시 검색해야 할 행                            |
| extra         | 추가 정보                                              |

##### id
- explain의 결과로 나온 각 행이 실제 쿼리에서 몇 번째 select 쿼리에 대한 분석인지 나타낸다.
- 수행 순서에 따라 id가 부여된다.

##### select_type
| 항목                 | 설명                                                                       |
| -------------------- | -------------------------------------------------------------------------- |
| SIMPLE               | 단순 SELECT (UNION, Sub Query 없음)                                        |
| PRIMARY              | UNION의 첫 번째 SELECT 쿼리 또는 Sub Query의 외부 쿼리                     |
| UNION                | UNION의 나머지 쿼리                                                        |
| DEPENDENT_UNION      | UNION과 동일하지만, 외부 쿼리에 영향받는 쿼리                              |
| UNION_RESULT         | UNION 쿼리의 결과                                                          |
| SUBQUERY             | FROM절 이외에서 사용되는 Sub Query 여러 Sub Query 결합인 경우 첫 번째 쿼리 |
| DEPENDENT_SUBQUERY   | Sub Query와 동일하나, 외부 쿼리에 의존적인 쿼리                            |
| DERIVED              | FROM절의 서브쿼리 또는 Inlin View                                          |
| UNCACHEABLE SUBQUERY | 캐시가 불가능한 서브쿼리                                                   |
| UNCACHEABLE UNION    | 캐시가 불가능한 UNION 쿼리                                                 |

추가
- DERIVED는 쿼리로 발생한 임시 테이블이다. 크기가 커서 디스크에 저장된다면 이슈가 될 수 있다.
- DEPENDENT 키워드는 내부 쿼리 수행 시, 외부 쿼리가 반드시 수행되어야 한다. 다수의 경우 성능 문제를 야기하기 때문에 가능한 JOIN으로 처리한다.

##### type (join type)
아래로 갈 수록 Cost가 높아진다.
| 항목            | 설명                                                                    |
| --------------- | ----------------------------------------------------------------------- |
| system          | 테이블에 단 한개의 데이터만 있는 경우                                   |
| const           | PK 혹은 Unique key를 상수로 조회, 많아야 한 건                          |
| eq_ref          | JOIN을 할 때 PK로 매칭하는 경우                                         |
| ref             | JOIN을 할 때 PK 혹은 Unique가 아닌 key로 매칭하는 경우                  |
| fulltext        | JOIN을 할 때 FULLTEXT 인덱스를 사용하는 경우                            |
| ref_or_null     | ref + null이 추가되어 검색                                              |
| index_merge     | 두 개의 인덱스가 병합되어 검색                                          |
| unique_subquery | IN Subquery + eq_ref                                                    |
| index_subquery  | IN Subquery + PK가 아닌 인덱스                                          |
| range           | 특정 범위 내에서 인덱스를 사용해서 데이터 추출                          |
| index           | 인덱스를 처음부터 끝까지 검색, 인덱스 일부 열만 사용하는 경우 발생 가능 |
| all             | 테이블 풀스캔                                                           |

---
##### 참고
- https://dev.mysql.com/doc/refman/8.0/en/correlated-subqueries.html
- https://dev.mysql.com/doc/refman/8.0/en/explain-output.html
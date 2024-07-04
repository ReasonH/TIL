헷갈리는 Lock에 대해 정리해보려 한다.

아래 예제들은 모두 아래의 DB 테이블을 기준으로 한다.

```sql
CREATE TABLE departments (
    id INT PRIMARY KEY,
    name VARCHAR(50) NOT NULL
);

CREATE TABLE employees (
    id INT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    department_id INT,
    FOREIGN KEY (department_id) REFERENCES departments(id)
);
```

S-Lock: 읽기 락, 공유 락

- 레코드에 s lock이 걸려있는 경우에도 획득 가능하다.
- 레코드에 x lock이 걸려있는 경우 획득 불가능하다.

X-Lock: 쓰기 락, 배타적 락

- 레코드에 어떤 종류의 lock이라도 걸려 있다면 획득이 불가능하다.

### Lock을 획득하는 방법

SELECT 상황에서의 명시적인 S-Lock 획득

```sql
SELECT * FROM employees WHERE department = 'Sales' LOCK IN SHARE MODE;
```

SELECT 상황에서의 명시적인 X-Lock 획득

```sql
SELECT * FROM employees WHERE department = 'Sales' FOR UPDATE;
```

UPDATE 상황에서의 X-Lock 획득 케이스

```sql
UPDATE departments SET name = 'new' WHERE id = 2;
```

- UPDATE 쿼리는 항상 X-Lock을 획득한 이후 수행된다.

UPDATE 상황에서의 X-Lock, S-Lock 동시 획득 케이스

```sql
INSERT INTO employees (id, name, department_id) VALUES (1, 'John Doe', 2);
```

- 외래 키를 포함한 레코드를 삽입하는 경우, MySQL은 참조 무결성을 확인하기 위해 연관된 테이블의 참조 대상 레코드에 대해 S-Lock을 건다.

#### TIP

- 모든 Lock의 해제 시점은 트랜잭션이 종료되는 때이다. (단일 쿼리인 경우 즉시 해제된다)
- 혼동하기 쉽지만 SELECT 쿼리가 S-Lock을 걸지는 않는다. 즉 X-Lock이 걸린 레코드를 얼마든 조회할 수 있다.

## 데드락

Lock 간 경합이 일어나서 대기 상태에 빠지는 것을 데드락이라 한다.

케이스별로 살펴보자.

### 단일 트랜잭션

트랜잭션 내에서는 명령어가 순차적으로 처리된다. 즉 첫 번째 명령어가 완료된 이후 두 번째 명령어가 실행되는 것을 보장한다. 따라서 단일 트랜잭션 만으로 데드락에 빠지는 경우는 없다. 다음 예시를 확인해보자.

**한 트랜잭션에서 동일한 레코드에 여러 번의 Lock을 건다고 가정한다.**

**X-Lock 획득 → S-Lock 획득**

X-Lock을 이미 획득한 경우 S-Lock을 얻을 필요가 없다. 따라서 데드락은 생기지 않는다.

**X-Lock 획득 → X-Lock 획득**

X-Lock을 이미 획득한 경우 X-Lock을 얻을 필요가 없다. 따라서 데드락은 생기지 않는다.

그렇다면 아래의 케이스는 어떨까?

**S-Lock 획득 → X-Lock 획득**

```sql
START TRANSACTION;

SELECT * FROM departments WHERE id = 2 LOCK IN SHARE MODE; # S-Lock
UPDATE departments SET name = 'new' WHERE id = 2; # X-Lock

COMMIT;
```

**이 경우 MySQL에서는 이미 얻은 S-Lock이 X-Lock으로 승격된다. 따라서 트랜잭션은 정상적으로 수행된다.**

결과적으로 단일 트랜잭션에서의 데드락은 걱정하지 않아도 된다.

### 다중 트랜잭션

일반적인 케이스에서 발생하는 데드락은 분산 환경에서 발생한다. 특히 대부분의 경우 UPDATE 등의 작업으로 X-Lock이 필요한 케이스에서 데드락이 자주 발생한다.

대표적인 예시는 아래와 같다.

```sql
-- Transaction 1
START TRANSACTION;
SELECT * FROM departments WHERE id = 1 LOCK IN SHARE MODE;
DO SLEEP(5);
UPDATE departments SET name = 'part1' WHERE id = 1;
COMMIT;

-- Transaction 2
START TRANSACTION;
SELECT * FROM departments WHERE id = 1 LOCK IN SHARE MODE;
DO SLEEP(5);
UPDATE departments SET name = 'part2' WHERE id = 1;
COMMIT;
```

1. 두 트랜잭션은 동일한 레코드에 대해 S-Lock을 획득한다.
2. 테스트를 위한 sleep 단계이다 (둘 다 S-Lock을 얻고 난 뒤 UPDATE 구문을 실행시키기 위함)
3. 두 트랜잭션은 각각 X-Lock을 얻으려 하지만 다음 상황이다.
    1. Transaction 1에서 X-Lock을 얻으려 하나 Transaction 2에서 S-Lock을 가지고 있다.
    2. Transaction 2에서 X-Lock을 얻으려 하나 Transaction 1에서 S-Lock을 가지고 있다.

이런 상황이 발생하는 경우 MySQL은 다음 예외를 던지며 강제적으로 하나의 트랜잭션을 실패시킨다.

> \[40001]\[1213] Deadlock found when trying to get lock; try restarting transaction

### 번외 (JPA)

- \[40001]\[1205] Lock wait timeout exceeded; try restarting transaction는 단순히 Lock 대기 시간이 길어져 `innodb_lock_wait_timeout`이 초과했을 때 발생한다.
- 경합 상태가 너무 많다면 상황에 따라 비관적 락을 검토해보는게 도움이 될 수 있다.
- 낙관적 락을 사용하는 경우 롤백이 자주 발생하고, 리트라이 커넥션이 많아지는 등 골치아파질 수 있다.

---

참고
- [https://dololak.tistory.com/446](https://dololak.tistory.com/446)
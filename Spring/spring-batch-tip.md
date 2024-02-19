Spring Batch에서는 다양한 Reader를 제공하고 있다. 이 중 JpaPagingItemReader는 JPA를 기반으로 만들어져 기존의 Application에서 JPA ORM을 다루는 방식으로 배치 작업을 할 수 있게 돕는다. (개인적인 경험으로는) JDBC 방식에 비해 가독성이 좋고, 부수코드의 양도 줄어들어 실무에서도 활용 중이다.

이 글에서는 JpaPagingItemReader에서 조심해야 할 부분과 문제를 해결한 방법에 대해 작성해보려 한다.

## 1. chunk와 page의 크기 통일

`JpaPagingItemReader`는 기본 page size로 10을 사용한다. Step 정의 시 chunk size가 page size를 초과하는 경우 Reader는 chunk size를 채우기 위해 여러 번의 트랜잭션을 수행한다.

이 때 트랜잭션이 여러 번 수행되는 것보다 큰 문제는 바로 페이지를 읽을 때마다 이전 트랜잭션 세션이 초기화된다는 점이다. 이렇게 되는 경우 마지막 이전의 entity들은 모두 영속성 컨텍스트가 끊긴 상태가 되며 `LazyInitializationException` 등을 발생시킬 여지가 있다.

따라서 Paging기반의 ItemReader 사용 시 두 사이즈를 통일 시키는 것을 권장한다.

```kotlin
@JobScope
@Bean
fun testStep(testReader: JpaPagingItemReader<TestEntity>,
							testProcessor: ItemProcessor<TestEntity, TestEntity>,
							testWriter: RepositoryItemWriter<TestEntity>)
: Step {
    return stepBuilderFactory["testStep"]
        .chunk<TestEntity, TestEntity>(100) // <--- 100
        .reader(testReader)
        .processor(testProcessor)
        .writer(testWriter)
        .listener(StepListener())
        .listener(WriterListener())
        .build()
}
```

```kotlin
@Bean
@StepScope
fun testReader(): JpaPagingItemReader<TestEntity> {
    val reader: JpaPagingItemReader<TestEntity> = object : JpaPagingItemReader<TestEntity>() {}
		reader.apply {
            pageSize = 100 // <--- 100
            setName("testReader")
            setEntityManagerFactory(entityManagerFactory)
            setQueryString("중략")
        }

    return reader
}
```

## 2. Reader의 조건 컬럼을 Processor(Writer)에서 가공하는 경우

Paging 기반의 ItemReader 사용 시 주의해야 하는 또다른 점은 바로 데이터 조회에 대한 보장이다. 만약 Reader에서 조회되는 entity가 Processor / Writer를 통해 조작된다면 페이지의 균일성이 깨질 수 있다.

### 예시

간단한 예시는 다음과 같다.

|id|status|content|
|---|---|---|
|1|true|…|
|2|true|…|
|3|true|…|
|4|true|…|
|5|true|…|
|6|true|…|
|7|true|…|
|8|true|…|
|9|true|…|
|10|true|…|
|11|true|…|
|12|true|…|
|13|true|…|
|14|true|…|
|15|true|…|

테이블에서 pageSize가 5로 지정된 JpaPagingItemReader를 이용해 데이터를 읽어온다고 가정한다.

쿼리는 다음과 같다.

```sql
SELECT * FROM example WHERE status = true ORDER BY id ;
```

별도 설정을 하지 않았다면 Reader의 pageSize 설정을 통해 최초 조회 시 다음과 같은 쿼리가 발생할 것이다.

```sql
SELECT * FROM example WHERE status = true ORDER BY id OFFSET 0 LIMIT 5;
```

이후 Processor에서 읽어온 데이터들의 `status`컬럼을 false로 갱신한다면 테이블의 상태는 다음과 같아진다.

|id|status|content|
|---|---|---|
|1|false|…|
|2|false|…|
|3|false|…|
|4|false|…|
|5|false|…|
|6|true|…|
|7|true|…|
|8|true|…|
|9|true|…|
|10|true|…|
|11|true|…|
|12|true|…|
|13|true|…|
|14|true|…|
|15|true|…|

이제 Reader는 다음 페이지를 읽기 위해 아래 쿼리를 수행한다.

```sql
SELECT * FROM example WHERE status = true ORDER BY id OFFSET 6 LIMIT 5;
```

이 때 읽어오게 되는 데이터는 당연하게도 11~15번의 row이다.

기존에 조회되던 데이터 자체가 변질됐기 때문에 페이징이 균일하게 적용되지 않은 것이다. where절로 인해 실제 쿼리가 조회하는 대상은 6번 row부터가 되며 이 상태에서 OFFSET과 LIMIT을 적용했기 때문에 중간 데이터를 의도치 않게 건너뛰게 된다.

즉 이런 방식을 사용한다면 처리가 누락되는 데이터가 생길 것이다.

### 해결책

1. Cursor 기반 Item Reader사용
2. Reader에서의 page 고정

1번 해결책은 당연한 사항이므로 생략하도록 한다. 주목할 것은 2번이다. (개인적으로 좀 더 실용적인 방안이라 생각한다.) 이 방법은 대상이 Update(또는 Delete)되는 경우를 감안해서 page 사이즈를 항상 0으로 고정시키는 것이다. 쿼리에서 매번 첫번째 페이지만 조회하도록 한다면 대상이 모두 업데이트 될 때까지 대상 데이터를 빠짐없이 조회 가능하다.

아래는 JpaPagingItemReader에서 0 page 고정을 사용하는 예시이다.

```kotlin
@Bean
@StepScope
fun testReader(): JpaPagingItemReader<TestEntity> {
    val reader: JpaPagingItemReader<TestEntity> = object : JpaPagingItemReader<TestEntity>() {
				{
        override fun getPage(): Int {
            return 0
        }
    }.apply {
            pageSize = 100
            setName("testReader")
            setEntityManagerFactory(entityManagerFactory)
            setQueryString("SELECT * FROM example WHERE status = true ORDER BY id")
        }

    return reader
}
```

Reader를 초기화 시 `getPage()` 메서드를 override 해주면 된다.

## 3. JpaPagingItemReader 성능 최적화 - Zero Offset

JpaPagingItemReader에서 LIMIT과 OFFSET으로 인한 성능 악화는 아주 유명한 문제다.

간단하게 설명하자면 Reader가 페이지를 읽어나갈 수록 테이블을 스캔하는 범위가 늘어나기에 배치 후반부에서 Reader 성능이 매우 느려진다.

zero offset이란 OFFSET을 계속 0으로 고정하는 방법을 뜻한다. 그 대신 Reader가 읽기를 시작할 PK를 매번 전달해서 테이블 풀스캔을 막고 성능을 높인다.

이를 구현하기 위해 커스텀한 ItemReader를 만들 수도 있지만, 간단한 예시를 위해 JpaPagingItemReader에 설정을 추가하는 방법을 알아보도록 한다.

```kotlin

private var startId = 0L

@Bean
@StepScope
fun testReader(): JpaPagingItemReader<TestEntity> {
    val reader: JpaPagingItemReader<TestEntity> = object : JpaPagingItemReader<TestEntity>() {
				{
        override fun getPage(): Int {
            return 0
        }

        override fun doReadPage() {
            val parameterValues: MutableMap<String, Any> = HashMap()
            parameterValues["startId"] = startId
            setParameterValues(parameterValues)
            super.doReadPage()
        }
    }.apply {
            pageSize = 100
            setName("testReader")
            setEntityManagerFactory(entityManagerFactory)
            setQueryString("SELECT * FROM example WHERE status = true and id > :startId ORDER BY id")
        }

    return reader
}
```

```kotlin
@Bean
@StepScope
fun testProcessor(): ItemProcessor<TestEntity, TestEntity> {
    return ItemProcessor { entity ->
        startId = entity.id
        entity
    }
}
```

1. `doReadPage()`메서드를 override한다.
    - 여기에서는 `parameterValues`를 통해 Reader 쿼리에서 사용할 수 있는 parameter를 주입할 수 있다. 예시에서는 startId를 주입했다.
2. queryString 부분에서는 `parameterValues`에서 주입한 key값으로 id 조건 쿼리를 추가했다.
3. ItemProcessor에서는 각 entity를 처리할 때마다 startId를 갱신한다.
    - 이렇게 한 페이지의 처리가 완료된 경우 다음 페이지 처리 시, 직전 id 값을 이용해 테이블 스캔 범위를 줄일 수 있다.

---

참고한 글들

- [https://jojoldu.tistory.com/146](https://jojoldu.tistory.com/146)
- [https://jojoldu.tistory.com/337](https://jojoldu.tistory.com/337)
- [https://tech.kakaopay.com/post/ifkakao2022-batch-performance-read/#zerooffsetitemreader](https://tech.kakaopay.com/post/ifkakao2022-batch-performance-read/#zerooffsetitemreader)
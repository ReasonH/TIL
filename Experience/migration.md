Spring 버전 업그레이드 작업 중 겪었던 이슈 정리
## Maven

- 사용하는 라이브러리 및 java 버전 변경에 따라 mvnw 버전도 업그레이드 해주어야 한다.
- 주의: dependency management를 사용하지 않는 경우와 사용하는 경우 특정 의존성의 하위 의존성이 다르게 불려질 수 있다.
    
    ```bash
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.2</version>
        <relativePath />
    </parent>
        
    설정이 존재하는 경우    
    
    [INFO] +- org.hibernate.validator:hibernate-validator:jar:6.2.3.Final:compile
    [INFO] |  +- jakarta.validation:jakarta.validation-api:jar:3.0.2:compile
    [INFO] |  +- org.jboss.logging:jboss-logging:jar:3.5.3.Final:compile
    [INFO] |  \\- com.fasterxml:classmate:jar:1.7.0:compile
    ```
    
    ```bash
    별다른 의존성 매니지먼트가 없는 경우
    
    [INFO] +- org.hibernate.validator:hibernate-validator:jar:6.2.3.Final:compile
    [INFO] |  +- jakarta.validation:jakarta.validation-api:jar:2.0.2:compile
    [INFO] |  +- org.jboss.logging:jboss-logging:jar:3.4.1.Final:compile
    [INFO] |  \\- com.fasterxml:classmate:jar:1.5.1:compile
    ```
    
>우리는 기존에 pom.xml에서 maven-surefire plugin 설정을 관리했는데, 이 설정 값은 실행 시 파라미터로도 전달이 가능하다는 걸 알게 되어서 이참에 정리했다.

```
./mvnw test -Dsurefire.useSystemClassLoader=false -DargLine='-Dfile.encoding=UTF-8 -Xms1024m -Xmx2048m' -Dtest='*ControllerTest,CommonDocumentationTest'
```

## javax -> jakarta

javax 패키지는 사라지고 jakarta 패키지로 대체되었다. IDE를 이용한다면 일괄 교체가 가능해서 대부분은 무리 없이 변경이 가능하다.
다만 공통 의존하고 있는 프로젝트가 있는 경우 이를 꼭 함께 업그레이드 해주어야 한다. 만약 외부 모듈의 method 인자 등에 javax 패키지 하위 클래스를 전달받는다면 문제가 된다.

## jasypt

프로퍼티 암호화를 위해 jasypt 사용하는 경우, 신규 버전 이용 시 다음과 같은 레거시 호환 설정을 추가해야만 암호화가 정상적으로 동작한다.

```yml
jasypt:
  encryptor:
    iv-generator-classname: org.jasypt.iv.NoIvGenerator
```

## Redis

ObjectMapper의 동작 방식이 수정되면서 직렬화 알고리즘에 영향이 생겼다. 우리의 경우 RedisTemplate Bean 정의에서 직렬화 용도로 `GenericJackson2JsonRedisSerializer`를 사용 중이었으며 이는 내부적으로 ObjectMapper를 사용 중이었다.

별다른 설정을 추가하지 않았음에도 다음과 같은 변경 사항이 생겼다.

**As-is**

```json
{
  "@class": "com.ncsoft.mtalk.lime.presenceserver.model.OnlineSession",
  "pubServerId": "lime-pub-server-purple-deploy-595cfb7dff-s4ssw:19081",
  "webSessionId": "e2f451c0-39d8-70a0-a651-fa088120b548",
  "topic": "/mchat/at2tnwbiZq2846TlV49k",
  "locale": [
    "java.util.Locale",
    "ko_KR"
  ],
  "clientType": "PURPLE_WEB",
  "deviceId": "5ca3bbc4-f3d9-498c-9197-2a3ef1af45b9"
}
```

**To-be**

```json
{
  "pubServerId": "lime-pub-server-purple-deploy-595cfb7dff-s4ssw:19081",
  "webSessionId": "12418cb7-819b-5ad5-8a10-913d32d2cbbd",
  "topic": "/mchat/f9huaYV2lO0BvTGTNvHc",
  "locale": "ko_KR",
  "clientType": "PURPLE_WEB",
  "deviceId": "5ca3bbc4-f3d9-498c-9197-2a3ef1af45b9"
}
```

@class 필드의 포함 여부가 업데이트 된 듯 하다. 이로인해 구버전 캐시 조회 시, 역직렬화 오류가 발생했다.

대안으로 생각해 본 방식은 다음과 같다.

1. 직렬화 / 역직렬화 커스텀
2. redisson-spring-boot-starter 버전 강제 롤백
3. 캐시 버전 업그레이드 후 새로운 규격으로 적재되도록 설정

2번 → 결국 업그레이드 해야하기 때문에 불가능

3번 → 사용자가 메시지를 수신하지 못하는 등, 배포 시점에 영향도 발생해서 불가능

결국 1번에서 방법을 찾아보기로 결정했다. 보통은 이런 이슈때문에 `GenericJackson2JsonRedisSerializer`를 잘 사용하지 않는다. 때문에 겸사겸사 Serializer 전환 작업도 병행하기로 했다.

#### 작업방향

1. serializer 전환 - `StringRedisSerializer`
2. 구버전 신규버전 모두 정상 조회 가능하도록 deserialize 로직 추가 및 objectMapper 커스텀
3. 신규 버전의 캐시 추가
4. 신규 버전 마이그레이션
    - 새롭게 적재되는 캐시는 신규 버전 이용
    - 조회 시에는 구버전 + 신규버전을 모두 보도록 코드 수정

#### ObjectMapper 커스텀

```java
@Bean
public ObjectMapper objectMapper() {
    SimpleModule module = new SimpleModule();
    module.addDeserializer(Locale.class, new JsonDeserializer<>() {
        @Override
        public Locale deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

            try {
                String[] localeArray = ctxt.readValue(p, String[].class);
                if (localeArray != null && localeArray.length > 1) {
                    return Locale.of(localeArray[1]);
                } else {
                    return null;
                }
            } catch (InvalidDefinitionException e) {
                return Locale.of(ctxt.readValue(p, String.class));
            }
        }
    });

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(module);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return objectMapper;
}
```

#### 캐시 추상화 메서드 수정

Redis 조회 시, 구버전/신버전 동시 지원하도록 적용

- reflection을 이용해 조회하려는 클래스 타입을 유연하게 지정
- Redis 데이터 내에 정의된 json 타입을 읽어와 클래스로 조합한다.

```java
public List<V> getHashValues(String cacheName, String key, Class<V> vClass) {
    HashOperations<String, String, Object> hashOps = redisTemplate.opsForHash();
    return hashOps.values(getCombinedKey(cacheName, key)).stream()
            .map(
                    jsonString -> {
                        try {
                            return deserialize((String) jsonString, vClass);
                        } catch (JsonProcessingException e) {
                            log.error("deserialize error {}", e);
                            return null;
                        }
            })
            .filter(Objects::nonNull)
            .toList();
}

private V deserialize(String json, Class<V> vClass) throws JsonProcessingException {
    if (json == null) {
        return null;
    }
    if (vClass == String.class) {
        if (json.startsWith("\\"") && json.endsWith("\\"")) {
            return vClass.cast(json.substring(1, json.length() - 1));
        } else {
            return vClass.cast(json);
        }
    }

    if (json.contains("\\"@class\\"")) {
        return objectMapper.readValue(json, vClass);
    } else {
        Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {});
        return convertMapToClass(map, vClass);
    }
}

private V convertMapToClass(Map<String, Object> map, Class<V> vClass) {
    try {
        V instance = vClass.getDeclaredConstructor().newInstance();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String fieldName = entry.getKey();
            Object fieldValue = entry.getValue();

            try {
                Field field = vClass.getDeclaredField(fieldName);
                field.setAccessible(true);

                if (fieldValue != null) {
                    if (field.getType().isEnum()) {
                        fieldValue = Enum.valueOf((Class<Enum>) field.getType(),
                                (String) fieldValue);
                    } else if (field.getType().equals(Locale.class)) {
                        fieldValue = Locale.of((String) fieldValue);
                    }
                }
                field.set(instance, fieldValue);
            } catch (NoSuchFieldException e) {
                log.error("convertMapToClass error {}", e);
            }
        }

        return instance;
    } catch (Exception e) {
        throw new RuntimeException("Failed to convert map to " + vClass.getName(), e);
    }
}
```

## Sleuth

spring-cloud-sleuth는 더 이상 관리되지 않으며 기존에 사용하던 분산 추적의 경우 micrometer를 사용하도록 변경되었다.

우리의 경우, 스레드 풀로의 traceid 전파를 위해 TraceableExecutorService라는 ExecutorService 구현체를 사용했는데, 이는 sleuth에서 관리되던 클래스였으며 이번 라이브러리 전환으로 인해 더 이상 사용이 불가능해졌다.

따라서 trace 전파를 위해 `ContextExecutorService`를 사용하도록 전환했다.

```java
@Bean
public ExecutorService publishExecutorService() {
    ExecutorService executorService = Executors.newFixedThreadPool(32);

    return ContextExecutorService.wrap(executorService, ContextSnapshotFactory.builder().build()::captureAll);
}
```

## Hibernate

### Id 이슈

기존에는 @GeneratedValue 사용 시, 정상 동작했으나, Hibernate 업그레이드 이후

_**Caused by: org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing table [XXX_SEQ]**_ 오류가 발생했다.

우리의 환경은 다음과 같았다.

- MySQL 8
- Hibernate 5.2

오류를 확인해보니 @GeneratedValue에서 사용될 sequence table이 존재하지 않아 발생하는 문제로 보인다.

우리는 AUTO 방식을 사용하기 때문에 IDENTITY 방식으로 id 채번이 이루어질 것을 기대했다. 그러면 sequence table missing 예외는 왜 등장한걸까?

뭔가 버전을 건너뛰며 놓친 부분이 있을 것이라 생각하고 문제를 명확하게 확인해보기로 했다.

먼저 우리가 사용하던 legacy hibernate (5.2)의 AUTO 모드에서 id 값 채번 방식은 아래와 같다.

1. @Id annotated된 필드의 타입 확인
    
2. Numeric인 경우 `new_generator_mappings`설정을 확인
    
3. false로 지정한 경우 native Generator를 사용
    
4. Dialect class가 지원하는 식별자 생성 방식(`getNativeIdentifierGeneratorStrategy`)을 사용
    
5. 우리는 MySQLDialect를 사용하는데, 이는 다음 코드에 따라 `identity` 방식을 적용하고 있다.
    
    ```java
    Dialect.class
    
    public abstract class Dialect implements ... {
    		...
    		public String getNativeIdentifierGeneratorStrategy() {
    		    return this.getIdentityColumnSupport().supportsIdentityColumns() ? "identity" : "sequence";
    		}
    		...
    }
    
    public class MySQLDialect extends Dialect {
    		...
    		public IdentityColumnSupport getIdentityColumnSupport() {
    		    return MySQLIdentityColumnSupport.INSTANCE;
    		}
    		...
    }
    
    public class MySQLIdentityColumnSupport extends IdentityColumnSupportImpl {
    		...
        public boolean supportsIdentityColumns() {
            return true;
        }
    		...
    }
    ```
    

현재의 6.5 버전에서는 위 방식이 적용되지 않는다. docs를 확인해보니 Hibernate 6.X 이후로 `new_generator_mappings`옵션이 사라졌다는 것을 확인했다.

또한, 이제 AUTO 방식은 다음과 같이 동작한다는 것을 알 수 있었다.

If the identifier type is numeric (e.g. `Long`, `Integer`), then Hibernate will use its `SequenceStyleGenerator` which resolves to a SEQUENCE generation if the underlying database supports sequences and a table-based generation otherwise.

AUTO 모드에서 식별자 유형이 숫자형인 경우 Hibernate는 SequenceStyleGenerator를 사용한다. 이는 다음과 같이 동작한다.

DB에서 sequence가 지원되는 경우 SEQUENCE 생성 → SEQUENCE 전략

- DB에서 sequence가 지원되지 않는 경우 테이블 기반 생성 → TABLE 전략

그렇다면 MySQL은 어디에 해당할까? 관련된 [이슈](https://discourse.hibernate.org/t/generated-value-strategy-auto/6481)가 이미 질문되어 있다.

논의를 보면 알 수 있듯이 5.3 버전부터 MySQL은 hibernate_sequence 테이블을 만드는 방식을 선택하고 있다. (즉, TABLE 전략) 따라서 기존 버전 프로젝트에서 호환을 맞추기 위해서는 명시적으로 GenerationType.IDENTITY 방식을 사용해야 한다. (추후 migration 방안을 찾을 필요는 있다)

## CircuitBreakerNameResolver 오류

openFeign 4.0.0 이후 버전 부터는 다음 옵션을 추가해야만 CircuitBreakerBuilder가 정상 동작한다.

```jsx
feign:
  circuitbreaker:
    enabled: true
```

### 출처
---
전체 참고

- [https://techblog.lycorp.co.jp/ko/how-to-migrate-to-spring-boot-3](https://techblog.lycorp.co.jp/ko/how-to-migrate-to-spring-boot-3)

Hibernate

- [https://medium.com/@132262b/spring-boot-3-hibernate-6-enum-문제-55bd3711a35d](https://medium.com/@132262b/spring-boot-3-hibernate-6-enum-%EB%AC%B8%EC%A0%9C-55bd3711a35d)
- [https://stackoverflow.com/questions/78299542/segregate-identity-generator-types-for-different-data-source-in-hibernate-6](https://stackoverflow.com/questions/78299542/segregate-identity-generator-types-for-different-data-source-in-hibernate-6)
- [https://velog.io/@power0080/JPA의-GeneratedValue의-AUTO-전략-이건-뭐죠](https://velog.io/@power0080/JPA%EC%9D%98-GeneratedValue%EC%9D%98-AUTO-%EC%A0%84%EB%9E%B5-%EC%9D%B4%EA%B1%B4-%EB%AD%90%EC%A3%A0)
- [https://bryceyangs.github.io/study/2023/02/15/Spring-spring-6-버전-hibernate-6-변경/#google_vignette](https://bryceyangs.github.io/study/2023/02/15/Spring-spring-6-%EB%B2%84%EC%A0%84-hibernate-6-%EB%B3%80%EA%B2%BD/#google_vignette)
- [https://discourse.hibernate.org/t/how-to-selectively-specify-an-id-creation-strategy-depending-on-the-connected-dbms-mariadb-10-3-or-above-and-below-and-oracle/10211/4](https://discourse.hibernate.org/t/how-to-selectively-specify-an-id-creation-strategy-depending-on-the-connected-dbms-mariadb-10-3-or-above-and-below-and-oracle/10211/4)
- [https://docs.jboss.org/hibernate/orm/6.5/userguide/html_single/Hibernate_User_Guide.html#identifiers-generators-auto](https://docs.jboss.org/hibernate/orm/6.5/userguide/html_single/Hibernate_User_Guide.html#identifiers-generators-auto)

CircuitBreaker

- [https://medium.com/@taesulee93/spring-with-resilience4j-circuit-breaker-%EC%A0%81%EC%9A%A9%ED%95%98%EA%B8%B0-a6102e8bbc7c](https://medium.com/@taesulee93/spring-with-resilience4j-circuit-breaker-%EC%A0%81%EC%9A%A9%ED%95%98%EA%B8%B0-a6102e8bbc7c)
- [https://mangkyu.tistory.com/289](https://mangkyu.tistory.com/289)
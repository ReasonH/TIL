# 각종 Spring Annotation 정리

### @Conditional

정의된 조건에 따라 Bean을 등록 여부를 결정하는 데 사용되는 Annotation이다.

```java
@Service
@Conditional(IsDevEnvCondition.class)
class LoggingService {
    // ...
}
```

### @ConfigurationProperties: 프로퍼티 설정값 기반

```java
@Service
@ConditionalOnProperty(
  value="logging.enabled",
  havingValue = "true",
  matchIfMissing = true)
class LoggingService {
    // ...
}
```

### @ConditionalOnExpression: 표현식 기반

```java
@Service
@ConditionalOnExpression(
  "${logging.enabled:true} and '${logging.level}'.equals('DEBUG')"
)
class LoggingService {
    // ...
}
```

그 외 다양한 종류가 존재한다.

### Bean 생성 조건으로 Or Condtion 사용

`AnyNestedCondition` 을 상속받아서 사용한다.

```java
class ProfileCondition extends AnyNestedCondition {

    public ProfileCondition() {
        super(ConfigurationPhase.PARSE_CONFIGURATION);
    }

    @ConditionalOnExpression("'${spring.profiles.active}'.indexOf('local') > -1")
    static class LocalL2MCondition {
    }

    @ConditionalOnExpression("'${spring.profiles.active}'.indexOf('live') > -1")
    static class LivePurpleCondition {
    }

    @ConditionalOnExpression("'${spring.profiles.active}'.indexOf('sandbox') > -1")
    static class StPurpleCondition {
    }
}
```

- 내부 클래스들의 @ConditionalOnExpression을 통해 조건 평가
- OR로 결합하여 조건 참 거짓 여부 결정 → 하나라도 매치되는 경우 ProfileCondition Bean 생성

ConfigurationPhase.PARSE_CONFIGURATION

- `Condition`이 `@Configuration` 파싱 이후 평가됨
- `Condition`이 regular bean 등록 이후 평가됨

### @Import

@Configuration으로 설정한 설정 파일을 두 개 이상 사용하는 경우 사용한다. 나의 경우는 테스트를 위해 특정 Configuration들만 불러오고 싶을 때 사용했다.

```java
@Configuration
public class DataSourceConfig {
    @Bean
    public DataSource dataSource() {
        return new DriverManagerDataSource(...);
    }
}

@Configuration
@AnnotationDrivenConfig
@Import(DataSourceConfig.class) // <-- AppConfig imports DataSourceConfig
public class AppConfig extends ConfigurationSupport {
    @Autowired DataSourceConfig dataSourceConfig;

    @Bean
    public void TransferService transferService() {
        return new TransferServiceImpl(dataSourceConfig.dataSource());
    }
}
```

물론 @Import를 한 번에 여러개 하는 것도 가능하다.

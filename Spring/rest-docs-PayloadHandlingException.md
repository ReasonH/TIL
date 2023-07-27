RestDocs를 이용한 API 문서화 작업 중 겪은 이슈 정리

### 이슈 상황

Maven기반의 프로젝트를 Gradle로 전환하는 과정에서 RestDocs를 이용한 문서화 수행 시 일부 API 스니펫이 만들어지지 않는 케이스가 생겼다.

다음은 문서화 코드의 예시이다. 단일 컨트롤러를 문서화 하는 일반적인 테스트이다.

```java
@ExtendWith(RestDocumentationExtension.class)
class ControllerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;
    private final UseCase useCase = mock(UseCase.class);

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        this.mockMvc = MockMvcBuilders.standaloneSetup(new Controller(useCase))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .addFilter(new CharacterEncodingFilter(StandardCharsets.UTF_8.name(), true))
                .apply(
                        documentationConfiguration(restDocumentation)
                                .operationPreprocessors()
                                .withResponseDefaults(prettyPrint())
                )
                .apply(new TestMockMvcConfigurer())
                .build();
    }

    @Test
    void inventory() throws Exception {
        final String url = ApiInfo.TEST_URL;

        FieldDescriptor[] requestField = new FieldDescriptor[]{
                fieldWithPath("id").type(JsonFieldType.NUMBER).description("아이디 값")
                , fieldWithPath("name").type(JsonFieldType.STRING).description("이름")
        };

        Request request = new Request(123123, "이유혁");
        when(useCase.foo(request)).thenReturn(null);

        this.mockMvc.perform(post(url)
                .header(HttpHeaders.AUTHORIZATION, TestAccountHelper.getJWT())
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(print())
                .andDo(document(url.replaceFirst("/", "")
                        , getCommonRequestPreprocessor()
                        , getCommonResponsePreprocessor()
                        , requestFields(requestField)
                ));
    }
}
```

기존에는 정상 작동 했지만, Gralde 전환 후`/gradlew test --tests ControllerTest` 수행 시 다음과 같이 BUILD FAILED가 발생했다.

```bash
2023-07-26T11:06:54.720+0900 [ERROR] [org.gradle.internal.buildevents.BuildResultLogger] BUILD FAILED in 20s
```
에러 로그에는 

파싱 관련 에러가 발생한다.
```java
org.springframework.restdocs.payload.PayloadHandlingException: Cannot handle application/json;charset=UTF-8 content as it could not be parsed as JSON or XML
```

### 원인 파악

우선 해당 에러를 검색했을 때는 Content-Type에 관한 이슈가 의심된다고 나왔다. 하지만, 분명 모든 문서 테스트는 공유하는 상황이었으며 테스트 코드는 수정도 없던 상태였다. 
- 단순히 빌드 툴만 변경한 상태였으며
- IntelliJ에서의 테스트 실행이나 maven을 이용한 테스트 빌드에서는 정상적으로 성공한 것을 확인했다.

이런 이유로 뭔가 다른 원인이 있을 것이라 생각했다.

조금 더 정보를 얻기 위해 --debug로 테스트를 수행했다.
`/gradlew test --tests ControllerTest --debug`

```java
2023-07-26T11:06:52.626+0900 [DEBUG] [TestEventLogger] Body = {"id":123123,"name":"����"...
```

수많은 로그들 중 위와 같은 내용을 확인했다. IntelliJ 테스트 콘솔에서는 잘 보이던 한글이 gradle 테스트에서는 깨져서 나오고 있다. 아무래도 인코딩 문제가 의심되는 상황이었다.

### 해결

Request Dto의 필드값을 다음과 같이 영어로 교체하고 동일한 graldlew test를 수행하니, 성공하는 것을 확인할 수 있었다.
```java
        Request request = new Request(123123, "Lee");
```
```java
2023-07-26T11:23:33.199+0900 [LIFECYCLE] [org.gradle.internal.buildevents.BuildResultLogger] BUILD SUCCESSFUL in 20s
```

예상했던대로 한글 인코딩의 문제라는 것을 알 수 있었다. 임시 조치로 테스트 Request / Response 필드를 모두 영어로 수정할 수 있지만, 아무래도 정답은 아닌 것 같아 gradle의 인코딩 지정을 바꾸는 방법을 찾아봤다.

다음의 파일을 추가하고, properties를 지정한다.
> gradle wrapper를 사용하는 spring boot 프로젝트라면, 파일을 build.gradle과 같은 레벨에 위치시킨다.

`gradle.properties`
```java
org.gradle.jvmargs=-Dfile.encoding=UTF-8
```
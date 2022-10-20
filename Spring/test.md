# 테스트 관련

### Test LocalDateTime 고정

실무에서는 현재 시스템 시간을 이용한 로직이 심심치 않게 사용되는데, 이는 테스트 시 외부 변인으로 작용할 수 있다. **좋은 테스트는 언제 실행하더라도 같은 결과를 만들어야 한다.**

물론 가장 좋은 방법은 구조 개선을 통해 시간 관련 변수를 mocking하는 것이다. 그러나 이런 개선이 제한될 경우 다음과 같이 static mocking을 통해 테스트 환경의 시스템 시간을 고정할 수 있다.

```java

class Test {
	final static String FIX_DATE_TIME = "2023-01-01T00:00:00.00Z";
	
	@BeforeEach
	void setUp() {
		LocalDateTime dateTime = LocalDateTime.now(Clock.fixed(
				Instant.parse(FIX_DATE_TIME),
				ZoneOffset.UTC));
		mockStatic(LocalDateTime.class);
		when(LocalDateTime.now(ZoneOffset.UTC)).thenReturn(dateTime);
	}
}
```

---

### MockMvc의 StanaloneSetup과 webAppContextSetup

MockMvc는 실제 네트워크에 연결하지 않고 웹 API 테스트가 가능하도록 모킹된 객체이다. MockMvcBuilder를 이용한 파라미터 세팅이 필수인데, 이 때 `StanaloneSetup`과 `webAppContextSetup`중 하나를 적용한다.

#### **`webAppContextSetup(WebApplicationContext context)`**

- 파라미터로 주어진 완전히 초기화된 web application context를 사용한다.
- MVC 설정이 적용된 DI 컨테이너를 사용해, 실제 서버에 배포된 것처럼 MVC 동작을 재현한다.

```java
@WebAppConfiguration
public class WelcomeControllerTest {

    @Autowired
    WebApplicationContext context;

    MockMvc mockMvc;

    @Test
    public void setUpMockMvc() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }
    // ..
}
```

`@WebAppConfiguration`:  테스트를 위해 부트스트랩된 ApplicationContext 가 _WebApplicationContext_ 의 인스턴스여야 함을 나타내는 데 사용한다. (중요)
`WebApplicationContext`: 테스트할 Application Context를 DI 받는다. 이 구성이 캐시되기 후속 테스트에서 재사용된다.

#### **`standaloneSetup(Object... controllers)`**

- @Controller 인스턴스를 하나 이상 등록하고, Spring MVC 인프라를 프로그래밍 방식으로 구성하여 MockMvc인스턴스를 만든다.
- 단위 테스트 관점에서 컨트롤러를 테스트한다.

```java
@ExtendWith(MockitoExtension.class)
public class WelcomeControllerTest {
    MockMvc mockMvc;
	
	@InjectMocks
	MessageRestController controller;
	
	@Mock
	MessageService mockMessageService;

    @Before
    public void setUpMockMvc() {
        this.mockMvc = MockMvcBuilders
		        .standaloneSetup(controller)
				.addFilter(new CharacterEncodingFilter("UTF-8"))
		        .build();
    }
}
```

addFilter등을 사용해 서블릿 필터가 추가 가능하다

> 참고1: https://itmore.tistory.com/entry/MockMvc-%EC%83%81%EC%84%B8%EC%84%A4%EB%AA%85
> 참고2: https://www.baeldung.com/spring-webappconfiguration

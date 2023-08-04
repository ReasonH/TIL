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
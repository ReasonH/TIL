신규 서버 개발 중 일부 API 호출이 400 예외를 반환하는 현상이 발생했다. 이에 대해 원인을 파악하고 문제를 해결한 과정을 정리한다.

### 현재 환경

- 클라이언트에서 서버로의 모든 요청은 게이트웨이를 통하고 있다.
- 게이트웨이에서는 JWT 토큰을 해석해 추가적인 헤더를 주입한 뒤 서버로 요청을 전달한다.
- 서버에서는 CommonRequestLoggingFilter를 통해 모든 요청과 응답에 대해 로깅하고 있다.
- 서버에서는 모든 예외에 대해 @RestControllerAdvice를 통해 전역적 예외 처리 및 로깅을 수행하고 있다.

### 문제 상황

- 클라이언트에서 서버로의 모든 API에 대해 400응답이 전달되고 있었다.
- 일부 계정에서만 이런 현상이 발생했으며 정상적으로 동작하는 계정도 있었다.
- Request / Response에 대한 로그는 물론 전역 예외 로그도 남고 있지 않았다.

### 문제 파악

우선 게이트웨이에서 API가 실패했을 가능성을 살펴봤다. 서버에서는 400 에러를 직접적으로 내려주는 케이스가 없었으며 문제가 되는 로그 또한 발견되지 않았기 때문이다. 일부 요청이 성공하는 것을 보면 헤더 파싱에 문제가 있어서 Gateway에서 요청이 거절되는게 아닐까 생각했다.

확인을 위해 서버에 tomcat access log를 추가하고 유입을 살펴본 결과, 서버에는 실제로 요청이 전달되고 있었다. 요청 실패에 대한 로그가 안남아있는 이유는 CommonRequestLoggingFilter 및 예외 처리에 의한 오류 로그가 동작하기도 전 앞단에서 요청이 거절됐기 때문이었다.

### 로그 확인

명확한 원인을 찾기 위해 전역 로그 설정을 DEBUG 모드로 수정하고, API 요청에 대한 로그를 찾아봤다. `org.apache.coyote.http11.Http11InputBuffer` 에서 남기고 있는 다음과 같은 로그를 볼 수 있었다.

```
Received [POST ***/testAPI HTTP/1.1
Accept-Encoding: identify,gzip,deflate
Content-Type: application/json
...중략
test-header: 1$...중략...;:ëì¹;...이하 생략 // 헤더는 가칭으로 대체
...중략
content-length: 0
```

눈에 들어온 것은 깨진 문자열이 포함된 헤더였다. `test-header`은 게이트웨이에서 생성되는 헤더였다.

조금 더 명확한 비교를 위해 요청이 성공하는 계정의 로그를 살펴봤다.

```
test-header: 1$...중략...;:reason96;...이하 생략
```

깨진 문자가 없이 정상적으로 들어오고 있다. 여기까지 두 가지 사실을 알 수 있었다.

- `Http11InputBuffer` 까지는 헤더가 정상적으로 들어오고 있다.
- 깨진 문자열이 있는 위치는 계정에 설정된 닉네임에 해당한다. 깨진 문자열을 가진 계정은 닉네임이 한글이다.

따라서 _test-header라는 헤더를 만들며 한글 인코딩이 정상적으로 되지 않았고, 이를 전달받은 Spring 내부적으로 문자를 처리하며 문제가 생겼을 것이다._ 라는 가설을 세웠다.

조금 더 살펴보니 `HttpStatusRequestRejectedHandler`클래스에 남아있는 다음의 로그를 찾을 수 있었다.

```
Rejecting request due to: The request was rejected because the header: 
"test-header" has a value "<생략>" that is not allowed.
org.springframework.security.web.firewall.RequestRejectedException
```

친절하게도 RequestRejectedException로 인해 요청이 거절되었다고 적혀있다. 여기에서 요청이 거절되고 400응답이 반환된 것으로 보인다.

### 원인 파악

`RequestRejectedException`에 대해 찾아보니 원인은 금방 찾을 수 있었다. Spring Security에서 기본으로 적용되어 있는 StrictHttpFirewall는 악성으로 의심되는 요청을 차단한다. (대표적인 예시는 MIME header 인코딩으로 처리되지 않은 문자열 또는 non-ASCII 문자가 포함된 요청이다.)

이 동작은 [Spring Security 문서]([https://docs.spring.io/spring-security/site/docs/5.4.2/reference/html5/#servlet-httpfirewall-headers-parameters](https://docs.spring.io/spring-security/site/docs/5.4.2/reference/html5/#servlet-httpfirewall-headers-parameters)) 에도 언급되어 있다.

	By default the `StrictHttpFirewall` is used. This implementation rejects requests that appear to be malicious. If it is too strict for your needs, then you can customize what types of requests are rejected. However, it is important that you do so knowing that this can open your application up to attacks. For example, if you wish to leverage Spring MVC’s Matrix Variables, the following configuration could be used:

이 문제는 StrictHttpFirewall가 헤더 문자열을 UTF-8로 구문 분석하도록 해 문제를 해결할 수 있다.

```java
@Bean
public StrictHttpFirewall httpFirewall() {
    StrictHttpFirewall firewall = new StrictHttpFirewall();
    firewall.setAllowedHeaderValues((header) -> {
        Pattern allowed = Pattern.compile("[\\\\p{IsAssigned}&&[^\\\\p{IsControl}]]*");
        String parsed = new String(header.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        return allowed.matcher(parsed).matches();
    });
    return firewall;
}
```

위 Bean을 통해 StrictHttpFirewall의 allowedHeaderValues를 수정할 수 있다.

### 결론

- 개발 환경에서는 logging 설정 시, root DEBUG를 기본적으로 사용하자. 문제를 훨씬 더 빨리 발견할 수 있었을 것 같다.
- Spring에서는 왜 HttpStatusRequestRejectedHandler의 handle 로그를 DEBUG 레벨로 설정했을까? ERROR는 아니더라도 WARN 수준이라면 좋을 것 같은데, 한 번 이슈업 해볼까 싶다.

---

참고

- [https://stackoverflow.com/questions/67128524/spring-boot-behind-nginx-reverse-proxy-rejected-header-value](https://stackoverflow.com/questions/67128524/spring-boot-behind-nginx-reverse-proxy-rejected-header-value)
- [https://velog.io/@semi-cloud/Spring-Security](https://velog.io/@semi-cloud/Spring-Security)
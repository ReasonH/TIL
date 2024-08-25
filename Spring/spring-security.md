## 개요

Spring Security flow를 파악하고, 각 객체간 역할을 직접 공부하며 예시와 함께 작성했습니다. Authentication 객체와 Provider의 동작을 중점적으로 정리합니다.

## Security 흐름도

![](./img/security.png)

## Security 구성요소 설명

### 0. Authentication

- 인증 정보를 의미하는 인터페이스
- **Security에서 제공하는 대부분 토큰은 이를 구현한 클래스이다.**
- `Provider`에서 구현하는 `authenticate()`의 파라미터 타입으로 사용된다.

### 1. SecurityConfig

1. 어떤 자원을 필터링할것이며 인증할것인가 `configure()`로 설정한다.
2. 필터 생성 및 등록한다.
    - 필터에 `Handler` 및 `AuthenticationManager` 등록)

    ```java
    protected OrganizationLoginFilter organizationLoginFilter() throws Exception {
            OrganizationLoginFilter filter = new OrganizationLoginFilter("/organization/login", 
    						loginAuthenticationSuccessHandler, loginAuthenticationFailureHandler);
            filter.setAuthenticationManager(super.authenticationManagerBean());
            return filter;
        }
    ```

3. `AuthenticationManagerManagerBuilder`로 `AuthenticationManager`에 `Provider`를 등록한다.

    AuthenticationManagerBuilder는 스프링의 ApplictaionContext가 만들어지는 과정에서 생성되고, 클래스 내부 메소드가 실행되면서 ProviderManager 클래스를 생성하여 add한 Provider들이 주입된다.

### 2. Provider (implements AuthenticationProvider)

`AuthenticationManagerBuilder`에 등록된다. `AuthenticationManager`로부터 실제 인증 작업을 할당 받고 실제 처리를 수행하는 class이다.

구현해야하는 함수는 아래의 두 개이다.

- `authenticate(Authentication authentication)` - 실제 인증 구현
- `support(Class<?> authentication)` - authenticate 메서드에 전달되는 인자가 지원되는 타입의 인자인지 확인, supports에서 통과된 타입이 authenticate 메서드의 파라미터로 전달된다.

    ```java
    @Override
    public boolean supports(Class<?> authentication) {
        return LoginPreAuthorizationToken.class.isAssignableFrom(authentication);
    }
    // isAssignableFrom의 의미
    // LoginPreAuthorizationToken은 authentication으로 assign할 수 있다.
    // ==> authentication은 LoginPreAuthorizationToken을 구현했다.
    ```

**`authenticate()` 상세 동작**

1. `**PreToken**`을 전달받아 아이디 비밀번호를 분리하고 이를 DB에서 조회하여 비밀번호 일치 여부를 검사한다.
2. 문제가 있는 경우 `AuthenticationException`을 상속받은 `AuthenticationServiceException`등을 throw 한다.
    - 필터로 결과가 전달되며, 필터는 등록된 `FailureHandler`를 동작시킨다.
3. 정상적으로 인증된 경우 `**PostToken**`을 만들어 반환한다.
    - 필터로 결과가 전달되며, 필터는 등록된 `SuccessHandler`로 이를 전달한다.

**Provider는 파라미터와 반환값으로 항상 토큰 타입이 아닌 `Authentication`타입을 사용한다.**

### 2-1. PreAuthorizationToken (extends UsernamePasswordAuthenticationToken)

- `Provider`에 인증 시 전달할 토큰
- `UsernamePasswordAuthenticationToken`등의 클래스를 사용하거나 이들을 활용해 커스텀 토큰을 만든다.

### 2-2. PostAuthorizationToken (extends UsernamePasswordAuthenticationToken)

- `Provider`에 인증 후 `SuccessHandler`에 전달할 토큰
- `UsernamePasswordAuthenticationToken`등의 클래스를 사용하거나 이들을 활용해 커스텀 토큰을 만든다.

### 3. Filter (extends AbstractAuthenticationProcessingFilter)

1. `SecurityConfig`에 등록할 필터
2. `attemptAutentication()`을 구현한다.
    - `PreToken`을 생성하고 `AuthenticationManager`의 `authenticate()`에 전달해 인증을 진행한다.
3. `successfulAuthentcation()`과 `unsuccessfulAuthentcation()`을 구현하여 인증 여부에 따라 핸들러의 `onAuthentication...` 메서드를 동작시킨다.

### 4. Handler

`Filter`에 등록되어 인증의 성공이나 실패에 따라 맡은 작업을 수행한다.

### 4-1. FailureHandler (implements AuthenticationFailureHandler)

`Provider`의 인증 실패 시, `HttpServletResponse`를 처리하여 적절한 응답을 반환하는 핸들러이다.

### 4-2 SuccessHandler (implements AuthenticationSuccessHandler)

`Provider`의 인증 성공 시, 이후 처리를 담당하는 핸들러이다. `Filter`에 등록된다.

- `onAuthenticationSuccess()`를 구현해야한다.
- 파라미터로 전달받는 `Authentication`은 실제로는 `PostToken`값이다. 따라서 해당 토큰 타입으로 캐스팅을 진행한 뒤 사용한다.

    ⇒ 토큰은 Authentication의 구현체를 상속받았기 때문이다.

- 토큰의 값으로부터 jwt토큰 등을 만들고, `HttpServletResponse`를 처리하여 적절한 응답을 반환한다.

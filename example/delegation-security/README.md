# SecurityContext 갱신 문제 테스트

업무 중 발생했던 이슈에 대해 원인을 파악하면서 진행했던 테스트이다. 자세한 내용은 다음 문서에 정리되어 있다.

### TEST
다음 절차로 진행된다.

- 비동기 작업을 위한 스레드 풀(TaskExecutor) @Bean 정의
  - 테스트를 위해 max-thread 1개로 생성한다.
  - SecurityContext를 전파시키기 위해 `DelegatingSecurityContextRunnable`를 설정한다.

1. 특정 작업이 스레드 사용 (스레드 풀 고갈)
2. API1 수행, SecurityContext 설정 후 종료
   - 내부에서 작업 스레드 호출되며 -> 스레드풀 고갈 상태이기 때문에 task 할당 후 종료
3. API2 수행, SecurityContext 설정 후 종료
4. API1 내부 스레드에 전달된 SecurityContext는 API2에서 갱신된 유저 정보임을 확인.
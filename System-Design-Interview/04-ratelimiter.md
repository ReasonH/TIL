# 4. 처리율 제한 장치 설계

## 1. 문제 이해 및 설계 범위 확정

질의 응답을 통해 먼저 확인할 수 있는 요구 사항은 다음과 같다.

- 어떤 종류의 제한 장치를 설치해야 하는가? 클라 or 서버? → 서버 측 장치
- 어떤 기준의 호출 제한을 해야하는가? IP / ID / ETC? → 다양하게 정의 가능하도록 유연한 설계 필요
- 시스템 규모는 어느정도인가? → 대규모 처리 필요
- 분산 환경 고려 필요한가? → 그렇다
- 독립 서비스인가? 기존 Application에 포함되는가? → 자유
- 제한에 걸린 경우 사용자에게 알려줘야 하는가? → 그렇다

## 2. 개략적 설계안 제시 및 동의 구하기

보통은 안정성을 위해 클라이언트에서 처리율 제한을 구현하기 보다는 서버나 GW등의 미들웨어를 이용한다. 둘 중 어느 것을 선택할지는 자유이다.

일반적인 기준은?

- 기술 스택: 현재 사용하는 언어나 기술이 서버측 구현을 지원하기 충분한지 확인하라
- 필요한 알고리즘: GW를 사용하는 경우 알고리즘 선택에 제한이 생길 수 있다. 필요한 알고리즘을 찾아라
- 현재 구조: 이미 GW를 사용하는 경우 처리율 제한 장치 또한 GW에 포함시키는게 좋을 수 있음
- 인력: 인력이 없다면 상용 GW를 쓰는게 나을 수 있음

### 처리율 제한 알고리즘

#### 1. 토큰 버킷 알고리즘
    
특정 capacity가 정의된 버킷에 시간당 N개의 토큰이 누적되며 각 요청은 해당 토큰을 소모한다. 버킷에 토큰이 없는 경우 요청은 무시된다.

장: 구현, 메모리 사용량, 트래픽 버스트도 견딜 수 있음

단: 튜닝하기 까다로울 수 있음


#### 2. 누출 버킷 알고리즘
    
특정 capa가 정의된 큐(버킷)에 요청을 누적시키며 시간당 N개만큼 처리한다. 버킷이 가득 찬 경우 요청은 무시한다.

장: 메모리 사용량, 고정 처리율을 통한 안정적 출력

단: 단시간 버스트에 취약, 튜닝하기 까다로울 수 있음
    
#### 3. 고정 윈도 카운터 알고리즘
    
타임라인을 고정 간격 윈도우로 나누고, 윈도우 내에서 N개의 요청만 처리되도록 한다. 초과 요청은 무시한다.

장: 메모리 사용량, 이해가 쉬움

단: 경계 부분에 요청이 몰리는 경우 시간당 기대보다 처리량이 늘어날 수 있다.
    
#### 4. 이동 윈도 로깅 알고리즘
    
Redis 캐시(ZSET) 등을 사용해 요청 타임스탬프를 추적한다. 새 요청이 올 때 시간을 기준으로 윈도우를 계산하고, 만료된 타임스탬프를 제거한다.

타임스탬프 로그를 매번 저장하며 로그 크기가 허용치보다 작다면 요청을 시스템에 전달한다.

장: 매우 정교

단: 메모리 사용량 높음 (거부된 요청까지 보관하기 때문에)
    
#### 5. 이동 윈도 카운터 알고리즘
    
현재 윈도우 내에서의 요청 수 + 직전 윈도우에서의 요청 수 X 이동 중인 윈도우와 직전 윈도우가 겹치는 비율 (%)를 계산해서 사용한다.

장: 단시간 버스트 대응 가능, 메모리 효율

단: 직전 시간대에 도착한 요청이 균등하게 분포되어 있다고 가정한다. 따라서 다소 느슨 (큰 문제아님)
    

#### 개략적인 설계
클라이언트 → 미들 웨어 (레디스 참조) → API 서버

## 3. 상세 설계

위의 설계에서는 다음 사항은 알 수 없다.

- 처리율 제한 규칙은 어떻게 만들어지고 어디에 저장?
- 처리 제한된 요청은 어떻게 되는지?

책의 예시는 다음과 같다.

1. **규칙 → 설정 파일 형태로 생성**
2. **저장 → 디스크 + 캐시 사용**
3. 한도 초과 트래픽 → 429 status 처리 or 큐를 이용한 재처리
4. 클라이언트의 한도 감지 → 429 status 응답에 다음의 헤더들을 추가
    - X-Ratelimit-Remaining: 윈도 내 처리 가능한 남은 요청 수
    - X-Ratelimit-Limit: 윈도마다 클라이언트가 전송할 수 있는 요청 수
    - X-Ratelimit-Retry-After: 한도 제한에 걸리지 않으려면 언제 요청을 다시 보내야 하는지?

### 분산 환경에서의 처리

우리의 시스템이 분산 환경에 있을 경우 다음과 같은 추가 고려 사항이 필요하다.

- 경쟁 조건: GET → SET 과정에서 스레드 경쟁으로 인한 데이터 불일치
- 동기화 이슈: 동일 사용자 요청이 다른 세션(서버)로 들어오는 경우
- 성능 최적화: 데이터 센터가 여러 개인 경우 latency
- 모니터링: 처리율 제한 알고리즘과 규칙에 대한 검증
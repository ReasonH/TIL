# 9. 웹 크롤러 설계

웹 크롤러는 웹에 새로 올라오거나 갱신된 콘텐츠를 찾는 것이 주된 목적이다. 웹 크롤러는 몇 개 웹 페이지에서 시작하여 그 링크를 따라 나가면서 새로운 콘텐츠를 수집한다.

크롤러 이용처

- 검색 엔진 인덱싱 - 로컬 인덱스를 만드는 용도
- 웹 아카이빙
- 웹 마이닝 (데이터 마이닝)
- 웹 모니터링

## 문제 이해 및 설계 범위 확정

웹 크롤러의 기본 알고리즘

1. URL 집합이 입력으로 주어지면, 해당 URL들이 가리키는 모든 웹 페이지를 다운로드한다.
2. 다운받은 웹 페이지에서 URL들을 추출한다.
3. 추출된 URL들을 다운로드할 URL 목록에 추가하고 위 과정들을 반복한다.

설계 범위 확정

- 크롤러의 용도 → 검색 엔진 인덱싱
- 수집 대상 → 월 10억 개
- 새로 만들어지거나 수정된 웹페이지도 수집 대상
- 수집한 웹페이지 저장 → 5년 간 저장
- 중복 콘텐츠를 갖는 페이지 무시

웹 크롤러가 만족해야할 속성

- 규모 확장성 → 병행성 고려
- 안정성 → 비정상적 입력과 환경애 대응 필요
- 예절 → 대상 사이트에 단시간 많은 요청하지 않도록 제어
- 확장성 → 손쉽게 새로운 형태의 콘텐츠 지원

### 개략적 규모 추정

- 월 10억 개 페이지 수집 → 400QPS (피크 시 800)
- 웹 페이지 크기 평균 500k 가정 → 5년 보전에 약 30PB 저장용량 필요

## 개략적 설계안 제시 및 동의 구하기

크롤러의 구성 요소에 대해 알아본다.

#### 시작 URL 집합

웹 크롤러가 크롤링을 시작하는 출발점이다. 전체 웹을 탐색하는 경우 크롤러가 가능한 많은 링크를 탐색할 수 있도록 하는 URL을 고르는게 바람직하다.

예시

- 전체 URL 공간을 부분집합으로 나누기
- URL 공간을 주제별로 세분화하고 각기 다른 시작 URL 사용하기

#### 미수집 URL 저장소

아직 다운로드가 되지 않은 URL을 저장 관리하는 컴포넌트, FIFO큐라고 생각할 수 있다.

#### HTML 다운로더

웹 페이지를 다운로드하는 컴포넌트

#### 도메인 이름 변환기

웹 페이지를 다운로드하기 위해 URL을 IP주소로 변환하는 컴포넌트

#### 콘텐츠 파서

웹 페이지를 다운로드한 뒤 파싱과 검증을 수행하는 컴포넌트

#### 중복 콘텐츠 판별기

동일한 콘텐츠를 여러번 저장하는 문제를 막기 위한 컴포넌트, 두 웹 페이지의 해시를 비교하여 중복을 줄이고 데이터 처리 속도를 높인다.

#### 콘텐츠 저장소

HTML 문서를 보관하는 컴포넌트, 대부분의 콘텐츠는 디스크에 저장하며 인기있는 콘텐츠는 메모리에 둔다.

#### URL 추출기

HTML 페이지를 파싱하여 링크를 골라내는 컴포넌트

#### URL 필터

크롤링 대상 URL을 필터링하는 컴포넌트

#### 방문한 URL 판별기

이미 방문한 URL or 미수집 URL 저장소에 이미 보관된 URL인지 확인하는 컴포넌트, 블룸 필터나 해시 테이블 등을 이용한다.

#### URL 저장소

이미 방문한 URL을 보관하는 저장소

### 웹 크롤러 작업 흐름

1. 시작 URL들을 미수집 URL 저장소에 저장
2. HTML 다운로더는 미수집 URL 저장소에서 URL 목록을 가져온다.
3. HTML 다운로더는 도메인 이름 변환기를 사용해 URL IP주소를 알아내고, 해당 IP에서 웹 페이지를 다운받는다.
4. 콘텐츠 파서는 HTML 페이지를 파싱해 올바른 페이지인지 검증한다.
5. 파싱과 검증이 끝나면 중복 콘텐츠인지 확인한다.
    - 이미 콘텐츠 저장소에 있는 경우 처리하지 않는다.
    - 저장소에 없는 경우 콘텐츠 저장소에 저장 후 URL 추출기로 전달한다.
6. URL 추출기는 HTML 페이지 내 링크를 골라낸다.
7. 골라낸 링크를 필터로 전달한다.
8. 필터링이 끝난 URL들은 중복 URL 판별 단계로 전달한다.
    - 이미 URL 저장소에 보관된 URL은 버린다.
    - 저장소에 없는 경우 URL 저장소에 저장 후 미수집 URL 저장소에 전달한다.

## 상세 설계

### DFS vs BFS

DFS: 어느정도로 깊숙이 가게 될지 가늠하기 어려움 → 보통 BFS를 사용

그러나, BFS에도 문제는 있다. 바로 한 페이지에서 나오는 링크의 상당수가 같은 서버로 되돌아간다는 것이다.

[wikipedia.com](http://wikipedia.com) → [wikipedia.com/a](http://wikipedia.com/a), [wikipedia.com/b](http://wikipedia.com/b), [wikipedia.com/c](http://wikipedia.com/c)

[wikipedia.com/a](http://wikipedia.com/a) → [wikipedia.com/a-1](http://wikipedia.com/a-1), [wikipedia.com/a-2](http://wikipedia.com/a-2), [wikipedia.com/a-3](http://wikipedia.com/a-3)

[wikipedia.com/](http://wikipedia.com/a)b → [wikipedia.com/b-1](http://wikipedia.com/b-1), [wikipedia.com/b-2](http://wikipedia.com/a-2), [wikipedia.com/b-3](http://wikipedia.com/b-3)

…

식으로 Queue 안에 저장될 탐색 대상 URL 중 대부분이 wikipedia가 될 확률이 높다.

### 미수집 URL 저장소

미수집 URL 저장소를 활용하면 BFS를 사용한 URL 수집에서 발생하는 요청 과부하 문제를 해결할 수 있다.

#### 예의

크롤러가 짧은 시간에 서버로 많은 요청을 보내는 경우 사이트를 마비시킬 수도 있다. 이런 문제를 방지하기 위한 원칙은 동일 웹 사이트에 대해서는 한 번에 한 페이지만 요청하도록 하는 것이다.

이를 만족시키기 위해서는 호스트명-작업 스레드 간의 관계를 유지하면 된다.

각 스레드는 별도의 FIFO 큐를 가지고 있어서 해당 큐에서 꺼낸 URL만 다운로드 한다.

- 큐 라우터: 같은 호스트에 속한 URL을 언제나 같은 큐로 가도록 보장한다.
- 매핑 테이블: 호스트 이름 - 큐 간의 관계를 보관한다. 큐 라우터는 이를 통해 URL이 삽입될 큐를 보장한다.
- 큐 선택기: 각 큐를 순회하며 큐에서 URL을 꺼내고, 지정된 작업 스레드에 전달한다.
- 작업 스레드: 전달된 URL을 다운로드한다. 작업들은 순차 처리되며 별도의 지연시간을 둘 수 있다.

#### 우선순위

크롤러 입장에서는 중요도가 높은 페이지를 먼저 수집하는 것이 바람직하다. 이 때 페이지 랭크, 트래픽 양, 갱신 빈도 등 다양한 척도를 사용할 수 있다.

- 순위 결정 장치: URL을 입력으로 받아 우선순위를 계산한다.
- 큐: 우선순위별로 큐가 하나씩 할당된다.
- 큐 선택기: 임의 큐에서 처리할 URL을 꺼낸다. 순위가 높은 큐에서 더 자주 URL을 꺼낸다.

2개의 큐를 이용해 예의 + 우선순위를 갖춘 설계를 할 수 있다.

- 전면 큐: 우선순위 결정 과정을 처리
- 휴면 큐: 예의바르게 동작하도록 (페이지 부하가 쏠리지 않도록) 처리

#### 신선도

웹 페이지는 주기적으로 변경되기 때문에 신선도를 유지하기 위해 재수집이 필요하다.

이를 위해 다음 두 가지 전략을 활용 가능하다.

- 변경 이력 활용
- 우선순위를 이용해, 중요 페이지를 자주 재수집

#### 미수집 URL 저장소를 위한 지속성 저장장치

처리해야 하는 URL 대부분은 디스크에 두지만 IO 비용을 줄이기 위해 메모리 버퍼에 큐를 둔다.

### HTML 다운로더

#### Robots.txt

웹사이트가 크롤러와 소통하는 표준적 방법이다. 크롤러는 페이지를 다운로드 하기 전 이 규칙을 확인해서 수집이 가능한 페이지를 판단해야 한다. 파일은 주기적으로 다시 다운받아 캐시에 보관한다.

#### 성능 최적화

HTML 다운로더에 사용 가능한 성능 최적화 기법

1. 분산 크롤링
2. 도메인 이름 변환 결과 캐시 - 도메인과 IP 관계를 캐시 보관, 크론을 통한 주기적 갱신으로 성능 향상
3. 지역성 - 지역별 분산으로 레이턴시 감소
4. 짧은 타임아웃 - 최대 대기 시간 제한

#### 안전성

다음은 다운로더 설계 시 시스템 안정성을 향상하기 위한 고려 사항이다.

- 안정 해시를 이용해 다운로드 서버 간 부하를 분산하고, 서버의 추가 삭제를 쉽게 한다.
- 크롤링 상태 및 수집 데이터를 저장해서 장애를 쉽게 복구할 수 있도록 한다.
- 예외가 발생해도 전체 시스템이 중단되지 않도록 한다.
- 시스템 오류를 방지하기 위해 데이터를 검증한다.

#### 확장성

새로운 형태의 콘텐츠 지원이 손쉽게 가능해야 한다.

#### 문제있는 콘텐츠 감지 및 회피

1. 중복 콘텐츠 → 앞서 설명했듯 해시 or 체크섬을 이용
2. 거미 덫 (크롤러 무한루프 함정) → 자동 알고리즘은 거의 불가능, 덫을 확인한 경우 수작업으로 제외해야함
3. 데이터노이즈 → 가능하다면 제외한다.

## 마무리

추가로 논의해볼만 한 사항

- 서버 측 렌더링
    
    웹사이트가 js등을 이용해 동적으로 링크를 만들어내는 경우, 페이지를 파싱하기 전 서버 측 렌더링을 적용할 수 있다.
    
- 원치 않는 페이지 필터링
    
    스팸 방지 컴포넌트를 두어 스팸성 페이지를 걸러내면 좋다.
    
- DB 다중화 및 샤딩 / 수평적 규모 확장성 / CAP / 데이터 분석 등
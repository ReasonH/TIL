# 6. 키-값 저장소 설계

put / get을 지원하는 키-값 저장소를 설계해본다.

## 문제 이해 및 설계 범위 확정

- 키-값 쌍의 크기는 10KB 이하
- 큰 데이터 저장 가능
- 높은 가용성 제공, 장애가 있더라도 빠른 응답
- 높은 규모 확장성 제공, 트래픽 양에 따른 자동 서버 증설/삭제
- 데이터 일관성 수준 조절 가능
- 짧은 레이턴시

## 단일 서버 키-값 저장소

단일 서버에 저장하는 경우 메모리의 한계가 분명하다. 데이터 압축, 자주 쓰이지 않는 데이터를 디스크 저장하는 방식으로 문제 개선이 가능하지만, 결국 서버 용량이 부족해지는 때가 온다. 따라서 분산 키-값 저장소가 필요하다.

## 분산 키-값 저장소

분산 시스템 설계 시에는 CAP 정리를 이해해야 한다.

### CAP 정리: 일관성, 가용성, 분할내성을 동시에 만족시킬 수 없음

1. Consistency
    
    모든 클라이언트는 분산 시스템의 어떤 노드에서도 같은 데이터를 보아야한다.
    
2. Availability
    
    일부 노드에 장애가 발생하더라도 항상 응답을 받을 수 있어야 한다.
    
3. Partition tolerance
    
    노드 간 네트워크 단절이 생기더라도 시스템은 계속 동작하여야 한다.
    

이 중 어떤 두 가지를 충족하려면 하나는 반드시 희생되어야 한다.

#### 이상적 상태의 분산 시스템

n1, n2, n3 노드가 있는 경우 네트워크 단절은 일어나지 않으며 데이터는 항상 정상 복제됨

#### 실세계의 분산 시스템

n3에 장애가 발생한 경우 n1, n2 데이터는 n3에 복사되지 않으며 n3에서 갱신된 데이터가 n1, n2에 복제되지 않았을 수 있다.

#### 문제 사례

- 일관성을 얻기 위해 n1, n2에 대해 쓰기 연산을 중단 시킨다. → 가용성이 깨진다.
- 가용성을 얻기 위해 쓰기 연산을 허용하는 경우 n3에서는 낡은 데이터를 반환할 수 있다. → 일관성이 개진다.

은행권의 시스템의 경우 보통 일관성을 양보하지 않는다. 이렇듯 케이스에 따른 CAP 정리를 적용해야 한다.

### 시스템 컴포넌트

#### 데이터 파티션

대규모 애플리케이션의 전체 데이터를 한 대 서버에 욱여넣는 것은 불가능하다. 따라서 데이터를 파티션으로 분할해 여러 서버에 저장한다.

서버에 데이터를 고르게 분산하고, 노드 추가/삭제 시 데이터 이동을 최소화 하기 위해 안정 해시를 사용할 수 있다.

#### 데이터 다중화

높은 가용성과 안정성을 확보하기 위해서는 데이터의 다중화가 필요하다. 안정 해시와 같은 경우 링을 순회하며 만나는 N개의 노드에 데이터 사본을 보관하는 방법을 사용한다.

- 가상 노드를 사용하는 경우 안정성을 확보하기 위해 같은 물리장비를 선택하지 않도록 해야한다.

#### 데이터 일관성

데이터 동기화를 위해서는 Quorum Consensus 프로토콜을 사용한다.

- N = 사본 개수
- W = 쓰기 연산을 성공으로 간주하기 위한 기준 Quorum 수 (정족수)
- R = 읽기 연산을 성공으로 간주하기 위한 기준 Quorum 수

W와 R은 Coordinator가 받는 응답의 기준이 된다.

ex) W가 1인 경우 Coordinator는 쓰기 연산 시 한 대의 서버로부터 성공 응답을 받으면 연산을 성공으로 판단

- R=1, W=N : 빠른 읽기
- R=N, W=1: 빠른 쓰기
- W + R > N: 강한 일관성
- W + R ≤ N: 강한 일관성 보장 X

> 일관성 모델

- 강한 일관성: 모든 읽기 연산은 항상 최신 데이터를 반환
- 약한 일관성: 항상 최신 데이터를 반환하지는 못할 수 있음
- 최종 일관성: 갱신 결과가 결국에는 모든 사본에 반영

고가용성 시스템의 경우 대부분 최종 일관성을 채택한다. 이 경우 병렬 쓰기 연산에서 발생하는 일관성 깨짐 문제는 클라이언트에서 해결해야 하는데, 이에 대한 기법은 다음과 같다.

#### 데이터 버저닝

동일한 데이터를 읽고 기록하는 서버1과 서버2가 있다. 두 서버가 동시에 데이터를 기록하는 경우 우리는 충돌하는 두 값을 갖게된다. 우리는 이 두 값의 충돌을 발견하고, 자동으로 해결할 버저닝 시스템이 필요하다.

**백터 시계는** 이런 문제를 해결하는 보편적인 기술이다. 이는 데이터에 [서버, 버전]구조의 순서쌍을 메단 것이다. 선행 버전과 후행 버전을 구분하거나 다른 버전과의 충돌을 판별할 때 사용한다.

데이터 D를 서버 S0에 기록하는 경우, 시스템은 아래 작업 가운데 하나를 수행한다.

1. D[S0, vi]이 있는 경우 → D[S0, vi + 1]로 버전을 1개 증가
2. 그렇지 않은 경우 → D[S0, v0]을 생성

벡터 시계를 사용하면 동일한 두 데이터 중 어떤게 이전 버전인지 쉽게 판단할 수 있다. 동일한 데이터 Dx와 Dy가 있을 때 Dx에 포함된 모든 구성요소의 버전(v)들이 Dy의 모든 구성요소들의 버전(v)보다 낮은 경우 Dx는 Dy의 이전 버전이다.

그렇지 않은 경우 (특정 요소들은 낮고, 특정 요소들은 높은 경우) 두 데이터 사이에 충돌이 있는 상황으로 이를 클라이언트에서 해결해야 한다.

#### 장애 감지

보통 분산 시스템에서는 두 대 이상의 서버가 동일한 서버에 대해 장애를 보고할 때 해당 서버를 장애로 간주한다. 이를 위해 모든 노드 사이에 멀티캐스팅 채널을 구축할 수도 있지만, 이는 서버가 많은 경우 분명 비효율적이다.

가십 프로토콜은 보다 효율적인 대안이 된다.

가십 프로토콜 동작 과정

1. 각 노드는 멤버ID와 박동 카운터 쌍의 목록으로 이루어진 멤버십 목록을 갖는다.
    
    ```jsx
    memberID | counter | time
    0        | 1110    | 12:00:01
    1        | 1211    | 12:00:01
    2        | 1000    | 11:59:55
    3        | 1110    | 12:00:01
    4        | 1210    | 12:00:01
    ```
    
2. 각 노드는 주기적으로 자신의 박동 카운터를 증가시킨다.
    
3. 각 노드는 주기적으로 무작위 노드들에 자신이 가진 박동 카운터 목록을 보낸다.
    
4. 이를 수신받은 노드들은 자신의 멤버십 목록에서 박동 카운터 값들을 최신으로 갱신한다.
    
5. 특정 멤버의 박동 카운터 값이 일정 시간 갱신되지 않으면 장애로 간주한다.
    

#### 일시적 장애 처리(자동 장애 복구)

네트워크나 서버 문제로 장애 상태인 서버로 가는 요청은 다른 서버가 잠시 맡아 처리한다.

이 때 변경 사항을 반영할 수 있도록 임시로 쓰기 연산을 처리한 서버는 hint를 남겨둔다. 장애 서버는 복구되는 시점에 해당 서버로부터 갱신된 데이터를 인계받아 일괄 반영한다. 이를 hinted handoff 기법이라 부른다.

#### 영구 장애 처리(수동 장애 복구)

영구 장애로 인해 수동으로 복구가 필요한 경우 사본들의 동기화는 어떻게 처리해야 할까?

사본 간 일관성이 망가진 상태를 탐지하고, 전송 데이터의 양을 줄이기 위해서는 **머클 트리(해시 트리)**를 사용한다. 이를 이용하면 대규모 자료 구조 내용을 효과적이면서도 안전한 방법으로 검증할 수 있다.

머클 트리 구성 과정

1. 키 공간을 버킷으로 나눈다.
2. 버킷 내 각 키에 균등 분포 해시를 적용해 해시 값을 계산한다.
3. 버킷별 해시 값을 계산해 해당 값을 레이블로 갖는 노드를 만든다.
4. 자식 노드의 레이블로부터 새로운 해시 값을 계산하여 이진 트리를 상향식으로 구성한다.

이 방법으로 서버 별 머클 트리를 구성한 뒤, 각 트리의 루트부터 비교하며 동기화 대상을 찾는다. 만약 루트 노드의 해시값이 같다면 두 서버는 같은 데이터를 갖는 것이다. 값이 다른 경우에는 각 서버의 왼쪽 자식 노드의 해시값을 비교 → 각 서버의 오른 쪽 자식 노드의 값을 비교하면서 아래쪽으로 탐색한다. 이 과정을 통해 최종적으로 다른 데이터를 갖는 버킷을 찾을 수 잇으므로 해당 버킷만 동기화하면 된다.

이를 통해 두 서버에 보관된 데이터 총량과 무관하게 동기화할 데이터의 양을 제한할 수 있다.

#### 데이터 센터 장애 처리

재난 상황에 대비해 데이터 센터를 다중화한다.

### 시스템 아키텍쳐 다이어그램

- 클라이언트는 키-값 저장소가 제공하는 API와 통신한다.
- 키-값 저장소에 대한 프락시 역할을 하는 노드를 둔다.
- 각 노드는 안정 해시의 링 위에 분포된다.
- 시스템은 완전히 분산되며 노드 추가/삭제가 가능하다.
- 데이터는 여러 노드에 다중화된다.
- 모든 노드가 같은 책임을 지므로 SPOF가 존재하지 않는다.

노드 내부 동작은 다음과 같다. (예시)

#### 쓰기 경로

1. 쓰기 요청을 commit log파일에 기록
2. 메모리에 저장
3. 메모리가 임계치가 넘는 경우 SSTable에 키, 값 쌍을 저장

#### 읽기 경로

1. 메모리에서 읽기
2. 메모리에 존재하지 않는 경우 bloom filter 검사 → 어떤 SSTable에 키가 보관되어 있는지 확인
3. SSTable에서 데이터 조회 및 반환
# Redis

## 개요
일반적인 캐시 사용 사례

**Look Aside**:  캐시 먼저 확인 → 없으면 DB 확인 → 캐시 갱신 후 결과 반환

**Write Back**:   쓰기가 빈번한 경우 데이터를 캐시에 먼저 저장하고 이를 모아서 주기적으로 DB 저장, 로그와 같은 정보
- 단점은 메모리에 먼저 저장하기 때문에 문제 발생 시 데이터 날아간다는 것


### Redis의 특징

-  개발의 편의성
	- 랭킹 시스템을 만든다고 가정한다. DB에 저장하고 Order By를 쓰면? → 너무 느림    
	- Redis Sorted Set을 쓴다면? → 빠르고 간편하다

-  개발의 난이도
	- 2개의 독립적인 트랜잭션이 동시에 일어났을 때 (Race Condition), 읽기 쓰기 작업 간 불일치가 발생 가능하다. Redis는 Atomic하기 때문에 Race Condition 자체를 피할 수 있다.


### Redis Collection
-   String: K-V
    -   key를 어떻게 잡을지 고민해야함
-   List: 중간 데이터 삽입 필요시 사용X
-   Set: 중복 데이터 방지
-   Sorted Set: 순서 있는 Set
    -   Sorted Set의 Score는 실수형이다 → 이에 관해 문제 발생 가능
-   Hash: 하위 계층 존재


### 주의사항
-   하나의 컬렉션에 수천개~10000개 정도만 유지하는게 좋다
-   Expire는 Collection 전체에 걸린다. 일부 아이템에 사용하는게 아님


## Redis 운영

### 메모리 관리
-   Redis는 In memory data store
-   물리적 메모리 이상 사용하면 문제 발생
    -   Swap 사용으로 해당 메모리 page 접근시 마다 늦어짐 (디스크를 사용하게 되어서)
-   Maxmemory 설정하더라도 이보다 더 사용할 가능성이 크다.
    -   Jemalloc으로 관리함에도 정확한 메모리 사용량을 알 수 없음 → 메모리 파편화때문에
-   메모리 사용해서 Swap을 쓰고 있다는 것을 모를때가 많다.
-   큰 메모리를 사용하는 instance 하나보다 적은 메모리를 사용하는 instance 여러개가 안전
-   다양한 사이즈를 갖는 데이터보다 유사한 크기의 데이터를 갖는 경우가 유리하다
-   Ziplist 자료구조로 메모리 절약이 가능하다
    -   List, Hash, Sorted Set 등을 Ziplist로 대체해서 처리하는 설정들이 존재
    -   인메모리이기 때문에 선형탐색 하더라도 빠름

### O(N) 관련 명령어 주의
-   Redis는 싱글스레드 → 결국 한 번에 한 개의 명령어만 처리 가능
-   단순한 get/set은 초당 10만 TPS 이상 가능하지만 O(N)은 주의 필요

#### 대표적인 O(N) 명령어
1.  KEYS
2.  FLUSHALL, FLUSHDB
3.  Delete Collections → Collection 내부 데이터가 100만개라면? 매우느림
4.  Get All Collections

#### 대표적인 실수 사례
-   Key가 100만 개 이상인데, 모니터링을 위해 KEYS 명령을 주기적 호출
-   ex) 예전 버전의 Spring Security Oauth Redis Token Stroe

#### KEYS를 대체할 방법은?
-   scan 명령을 사용하는 것으로 하나의 긴 명령을 짧은 여러번의 명령으로 변경
-   scan 명령 사이사이에 get/set 등을 처리할 수 있음

#### Collection의 모든 item을 가져와야할 때는?
-   일부만 가져오거나
-   여러개의 작은 Collection으로 나눠서 저장한다.
    -   하나당 몇천개 안쪽으로 조절하는게 좋음


## Redis Replication
-   Async Replication임 → Replication Lag이 발생할 수 있음 (일시적 데이터 불일치)
-   Replicaof 명령으로 설정 가능
-   DBMS의 statement replication과 유사
    -   DB에 저장할 때 시각에 now() 등을 사용하면 Primary와 Secondary의 데이터가 다를 수 있음
    -   이런 점들을 인지해야함
-   Replication 설정 과정
    1.  Secondary에 replicaof 명령 전달
    2.  Secondary는 Primary에 sync 명령 전달
    3.  Primary는 현재 메모리 상태를 Fork해서 저장
    4.  Fork한 메모리 정보를 disk에 dump
    5.  해당 정보를 secondary 전달
    6.  Fork 이후 데이터를 secondary에 계속 전달


### 주의할 점
-   Fork 발생으로 메모리 부족 발생 가능
-   Redis-cli —rdb 명령은 현재 상태 메모리 스냅샷을 가져오기에 동일 문제를 발생시킴
-   많은 Redis 서버가 Replica를 두고 있다면?
    -   동시에 replication 재시도 시 문제 발생 가능


### 권장 설정
-   Maxclient 50000
-   RDB / AOF off
-   KEYS 커맨드 disable
-   대부분의 장애가 KEYS와 SAVE (RDB 옵션) 사용에서 발생
-   클러스터 모드로 사용하는 경우 RDB / AOF 필요 시 Secondary에서만 구동
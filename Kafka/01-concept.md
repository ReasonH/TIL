## 카프카 구성요소

### 기본구조

주키퍼 클러스터

- 카프카 클러스터 관리

카프카 클러스터

- 메세지 저장 역할
- 여러 대의 브로커(서버)로 구성

프로듀서와 컨슈머

### 토픽, 파티션

토픽

- 메세지 구분의 단위 FS의 폴더와 유사
- 한 개의 토픽은 하나 이상의 파티션으로 구성

파티션

- 저장하는 물리적인 파일

프로듀서와 컨슈머는 토픽을 기준으로 메세지를 주고받음

### 파티션과 오프셋, 메시지 순서

- 파티션은 append-only 파일이며 각각의 메세지가 저장되는 위치를 offset 이라함
- 프로듀서가 넣은 메세지는 파티션의 맨 뒤에 추가
- 컨슈머는 offset 기준으로 메세지를 순서대로 읽음
- 메세지는 삭제되지 않음(설정에 따라 일정 시간 이후 삭제)

### 여러 파티션과 프로듀서

동일한 토픽에 여러 파티션이 있는 경우 프로듀서는 라운드로빈 or 키를 통해 파티션을 선택한다. 프로듀서가 메세지를 보낼 때 토픽과 함께 키를 지정할 수 있는데, 해당 키의 해시 값으로 파티션을 찾아서 저장한다.

### 여러 파티션과 컨슈머

- 컨슈머는 컨슈머 그룹이라는 단위에 속함
- 한 개의 파티션은 컨슈머 그룹 당 하나의 컨슈머에만 연결 가능
    - 같은 그룹에 속한 컨슈머들 간 파티션을 공유할 수 없음
    ⇒ 이를 통해 컨슈머 그룹을 기준으로 파티션 메세지들이 순서대로 읽힘을 보장

### 성능

- 파티션 파일은 OS 페이지 캐시 사용
- zero copy
- 브로커의 역할 단순화
- 프로듀서/컨슈머가 묶어서 보내고, 묶어서 받을 수 있음 (배치 작업 가능) → 높은 처리량
- 손쉬운 수평 확장

### 리플리카

- 파티션의 복제본, 복제 수만큼 파티션의 복제본이 각 브로커에 생성
- 리더와 팔로워 구성으로 프로듀서/컨슈머는 리더를 통해서 메세지 처리, 팔로워는 리더의 메세지를 복제
- 리더 장애 시 다른 팔로워가 리더로 승격
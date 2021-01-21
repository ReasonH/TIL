## 1. 컨테이너로 애플리케이션 실행

도커 이미지?

컨테이너를 구성하는 FS와 실행할 애플리케이션을 하나로 합친 것, 컨테이너 생성하는 템플릿

도커 컨테이너

이미지를 기반으로 FS와 애플리케이션이 구체화되 실행되는 상태

### 도커 이미지와 도커 컨테이너

```bash
$docker image pull gihyodocker/echo:latest
```

```bash
$docker container run -t -p 9000:8080 gihyodocker/echo:latest
```

- 옵션을 통한 port forwarding 지정되어 있음

```bash
$curl http://localhost:9000/
```

```bash
$docker stop $(docker container ls -q)
```

### 간단한 애플리케이션과 도커이미지

```go
package main

import (
	"fmt"
	"log"
	"net/http"
)

func main() {
	http.HandleFunc("/", func(w http.ResponseWriter, r * http.Request) {
		log.Println("received request")
		fmt.Fprintf(w, "Hello Docker!!")
	})

	log.Println("start server")
	server:= & http.Server {
		Addr: ":8080",
	}

	if err:= server.ListenAndServe(); err != nil {
		log.Println(err)
	}
}
```

```docker
FROM golang:1.9 # 일반적으로 태그로 버전 구분

RUN mkdir /echo
COPY main.go /echo

CMD ["go", "run", "/echo/main.go"]
```

FROM 인스트럭션

- 베이스 이미지 지정, 이미지 빌드 전 지정 이미지 다운로드
- 베이스 이미지는 docker hub에서 참조

RUN

- 컨테이너 내에서 실행할 명령을 정의하는 인스트럭션
- 실행할 명령을 그대로 기술

COPY 인스트럭션

- 도커가 동작 중인 호스트 머신의 파일이나 디렉터리를 도커 컨테이너 안으로 복사하는 인스트럭션
    - 예제에서는 호스트에서 작성한 main.go 를 컨테이너 내부로 복사

CMD 인스트럭션

- 컨테이너 안에서 실행할 프로세스 지정
- RUN은 이미지를 빌드할 때 실행, 어플리케이션 업데이트 및 배치
- CMD는 컨테이너를 시작할 때 한 번 실행, 어플리케이션 자체 실행
- 쉘 스크립트의 `$ go run /echo/main.go` ?

추가) ENTRYPOINT 인스트럭션

- 컨테이너 안에서 실행할 프로세스 지정 (CMD와 유사하다.)
- 그러나 ENTRYPOINT는 불변 값으로 무조건 실행된다.

    이는 CMD가 실행시 전달받은 인자로 대체되는 것과 대비됨

- ENTRYPOINT를 지정하고 인자를 잘못 전달하면 에러 발생 가능

**ENTRYPOINT와 CMD의 올바른 사용**

1. 컨테이너 수행시 변경되지 않을 명령은 ENTRYPOINT 사용
2. 메인 명령어 실행 시 default option 인자값은 CMD 정의

    *이렇게하여금 인자는 대체 가능하도록 만든다.*

또한, ENTRYPOINT와 CMD는 성능상 리스트 포맷으로 전달이 유리

### 도커 이미지 빌드하기

`docker image build -t 이미지명[:태그명] Dockerfile경로`

- - t : 이미지명, 태그명 지정/태그 생략시 latest생성
    - 태그는 생성하는 것이 좋음

`docker image build -t example/echo:latest .`

확인 `docker image ls`

 

그 외에 LABEL, ENV, ARG 등의 인스트럭션이 있다

LABEL: 이미지 만든이

ENV: 컨테이너 환경변수

ARG: 빌드 환경변수

- bg 실행

```bash
$docker container run -d example/echo:latest
# -d 옵션은 백그라운드 실행
```

`main.go` 에서 지정한 포트는 컨테이너 내부 포트이다.

따라서 외부에서 접근이 불가능하기 때문에

컨테이너 포트에서 포트포워딩이 필요하다. 다시 해보자

- 컨테이너 종료

```bash
$docker container stop $(docker container ls --filter "ancestor=example/echo" -q)
# -q 옵션은 컨테이너 아이디만 출력
```

- 포트 포워딩 및 테스트

```bash
# 포트 지정
$docker container run -d -p 9000:8080 example/echo:latest
# 포트 비지정 (에페메랄로 포워딩)
$docker container run -d -p 8080 example/echo:latest

# 컨테이너 외부
$curl http://localhost:9000/
```

## 2. 도커 이미지 다루기

도커 이미지

- 도커 컨테이너를 만들기 위한 템플릿
- ubuntu와 같은 운영체제로 이루어진 파일 시스템
- 컨테이너위에서 실행하기 위한 애플리케이션, 의존 lib, 실행 환경 설정정보도 포함
- Dockerfile은 이미지가 아니다. 이미지 순서만 구성해놓은 것

### docker image build - 이미지 빌드

`$docker image build -t 이미지명[:태그명] Dockerfile경로`

- -f 옵션

이름이 'Dockerfile'이 아닌 도커파일을 찾을 때 사용

```bash
$docker image build -f Dockerfile-test -t example/echo:latest .
# 이름이 Dockerfile-test인 도커파일 사용
```

- --pull 옵션

```bash
$docker image build --pull=true -t example/echo:latest
# 베이스 이미지를 항상 새로 받아옴
# 캐시를 사용하지 확실한 최신 베이스 이미지를 사용하도록 함
```

실무에서는 latest 지양, 태그 사용한다.

### docker search - 이미지 검색

```bash
$docker search [options] 검색어
# --limit 5 등 옵션 선택
```

기본적으로 star 순 정렬하며 네임스페이스가 생략된 레포지터리는 공식레포

### docker image pull - 이미지 내려받기

```bash
$docker image pull [options] 레포지토리명[:태그명]
# 태그 생략시 기본값으로 받아옴, 일반적으로 기본값은 latest
```

### docker image ls - 보유 도커 이미지 목록 보기

- 호스트에 저장된 도커 이미지 목록, pull / build 모두 포함

### docker image tag - 이미지에 태그부여

image id 는 도커 이미지의 버전 넘버 역할을 한다. dockerfile 편집 뿐 아니라 copy 대상이 되는 파일 내용이 바뀌어도 image id 값이 바뀜 따라서 이미지 id는 곧 버전이 된다.

`$docker image tag 기반이미지명[:태그] 새이미지명[:태그]`

### docker image push - 이미지 외부 공개

```bash
$docker image tag example/echo:latest stormcattest/echo:latest
# 도커 허브는 자신 혹은 소속 기관이 소유한 리포지토리에만 이미지 등록 가능
# 따라서 네임스페이스 example -> stormcattest(docker hub id)로 변경
```

## 3. 도커 컨테이너 다루기

### 도커 컨테이너 라이프사이클

- 실행 중, 정지, 파기 3가지 상태를 가짐
- 같은 이미지로 생성해도 별개의 상태를 갖는다

실행중 상태

- Dockerfile에 포함된 CMD및 ENTRYPOINT 인스트럭션에 정의된 애플리케이션이 실행됨. 이 에플리케이션이 실행 중인 상태
- 실행이 끝나면 정지 상태가 됨

정지 상태

- 사용자가 컨테이너 정지 or 실행된 에플리케이션이 종료된 경우
- 가상환경 동작 X, 디스크 종료 시점의 컨테이너 상태 저장됨 → 재실행 가능

파기 상태

- 정지 컨테이너의 명시적 삭제 상태

### docker container run - 컨테이너 생성 및 실행

```bash
$docker container run [options] 이미지명[:태그] [명령] [명령인자]
$docker container run [options] 이미지 ID [명령] [명령인자]
```

docker container run 명령 인자

```bash
$docker image pull alpine:3.7
$docker container run -it alpine:3.7 uname -a

# docker container run 명령에 명령 인자 전달시 인스트럭션을 
# 다른 명령으로 오버라이드 가능
```

컨테이너 이름 붙이기

```bash
$docker container run -t -d --name gihyo-echo example/echo:latest
# --name [컨테이너명] [이미지명][:태그]
```

**자주 사용하는 옵션**

- --it
- -i 옵션은 컨테이너 실행시 컨테이너 쪽 표준 입력과 연결 유지
- -t: 옵션은 컨테이너 실행시 유사 터미널 실행 ⇒ 붙여서 사용함

### docker container ls - 컨테이너 목록 보기

아이디만 추출

-> `$docker container ls -q`

컨테이너 목록 필터링

- 컨테이너 이름 기준

   -> `$docker container ls --filter "name=echo1"`

- 컨테이너 생성 이미지 기준

   -> `$docker container ls --filter "ancestor=example/echo1"`

이미 종료된 컨테이너 포함 목록

-> `$docker container ls -a` 

#### docker container stop - 컨테이너 정지

`$docker container stop 컨테이너ID또는 컨테이너명`

#### docker container restart - 컨테이너 재시작

`$docker container restart 컨테이너ID또는 컨테이너명`

#### docker container rm - 컨테이너 파기

`$docker container rm 컨테이너ID또는 컨테이너명`

실행중 삭제하려면 -f 옵션

컨테이너 유지가 필요없는 경우 run 시점에 —rm 옵션

#### docker container logs - 표준 출력 연결

`$docker container logs [options] 컨테이너ID또는 컨테이너명`

- 컨테이너 표준 출력 제공, -f 사용시 새로운 표준 출력 계속 업뎃해줌

### docker container exec - 실행중 컨테이너에서 명령 실행

`$docker container exec [options] 컨테이너ID_또는_컨테이너명 컨테이너에서_실행할_명령`

```bash
$docker container run -t -d --name echo --rm example/echo:latest
# hash값 출력
$docker container exec echo pwd
/go

# -it 옵션 조합시
$docker container exec -it echo sh
# 컨테이너에서 쉘 사용 가능
# ex)
pwd
/go
```

### docker container cp - 파일 복사

```bash
# 컨테이너 안의 /echo/main.go 파일을 호스트의 현재 작업 디렉토리로 복사
$docker container cp echo:/echo/main.go .

# 호스트에서 컨테이너로 파일 복사
$docker container cp dummy.txt echo:/tmp
$docker container exec echo ls /tmp | grep dummy
dummy.txt
```

- 파기되지 않은 정지상태 컨테이너에 대해서도 수행가능
- 디버깅 중 컨테이너 안에서 생성된 파일을 호스트에서 확인 목적으로 사용

## 4. 운영과 관리를 위한 명령

### prune - 컨테이너 및 이미지 파기

실행 중이 아닌 모든 컨테이너 삭제 명령

-> `$docker container prune`

태그가 붙지 않은 모든 이미지 삭제

-> `$docker image prune`

사용하지 않는 모든 도커 리소스 일괄 삭제

-> `$docker system prune`

### docker container stats - 사용 현황 확인

-> `$ docker container stats [options] [대상컨테이너ID]`

## 5. 도커 컴포즈로 여러 컨테이너 실행

도커 컨테이너는 단일 애플리케이션이다. 따라서 애플리케이션 연동 없이는 시스템 구축 불가능! ⇒ 도커 컴포즈 사용 이유

### docker-compose 명령으로 컨테이너 실행

`$ docker-compose up` -> 컨테이너 실행

`$ docker-compose down` -> yml의 모든 컨테이너 정지 or 삭제

```yaml
version: "3" # grammer version
services:
    echo: # container name
        build: . # using relative path, --build option explictly re-build the image.
        # image: example/echo:latest # docker image
        ports: # port forwarding
            - 9000:8080
```

## 6. 컴포즈로 여러 컨테이너 실행

```yaml
version: "3"
services:    
    master:
        container_name: master
        image: jenkinsci/jenkins:2.142-slim
        ports:
            - 8080:8080
        volumes: # 컨테이너간 파일 공유
            - ./jenkins_home:/var/jenkins_home # 마운팅
```

마스터 컨테이너 접속 후 SSH 생성

-> `docker container exec -it master ssh-keygen -t rsa -C ""`

### 슬레이브 젠킨스 컨테이너 생성

```yaml
version: "3"
services:    
    master:
        container_name: master
        image: jenkinsci/jenkins:latest
        ports:
            - 8080:8080
        volumes: # 컨테이너간 파일 공유
            - ./jenkins_home:/var/jenkins_home # 마운팅
        links: # master에서 slave를 찾아갈 수 있음
            - slave01

slave01:
    container_name: slave01
    image: jenkinsci/ssh-slave # ssh 접속하는 슬레이브 용도의 도커 이미지
    environment:
        - JENKINS_SLAVE_SSH_PUBKEY=ssh-rsa ~~~
        # ssh 접속하는 상대가 마스터 젠킨스임을 식별하기 위한 키
        # 키는 반드시 외부 환경 변수로 받아와야함
```

중간정리

1. 마스터 컨테이너 생성 후, 마스터의 SSH 공개키 생성
2. docker-compose.yml 파일에 슬레이브 컨테이너 추가 후, 1에서 만든 SSH 공개키를 환경 변수에 설정
3. links 요소를 사용해 마스터 컨테이너가 슬래이브 컨테이너 통신하도록 설정
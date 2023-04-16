도커 이미지 추가 방법에는 크게 세 가지가 있다.

1. Pull을 사용해 이미지 가져오기
2. 컨테이너의 변경사항으로부터 이미지 생성
3. Dockerfile을 빌드

이전 실습에서는 2번을 수행했다. 그러나 이는 실제로 자주 사용하지 않는다.

### Dockerfile로 Git이 설치된 우분투 이미지 정의

1. 디렉토리 생성
2. Dockerfile 작성

    ```docker
    FROM ubuntu:bionic
    RUN apt-get update
    RUN apt-get install -y git
    ```

3. Dockerfile로 이미지 빌드

    ```bash
    $ docker build -t ubuntu:git-from-dockerfile .
    ```

### 모니위키 도커 파일 작성

웹 애플리케이션 서버 실행을 위한 도커 이미지를 작성한다. 애플리케이션 실행을 위해 도커 이미지를 만드는 작업은 도커라이징이라 한다.

```docker
FROM ubuntu:14.04

RUN apt-get update &&\
  apt-get -qq -y install git curl build-essential apache2 php5 libapache2-mod-php5 rcs

WORKDIR /tmp
RUN \
  curl -L -O https://github.com/wkpark/moniwiki/archive/v1.2.5p1.tar.gz &&\
  tar xf /tmp/v1.2.5p1.tar.gz &&\
  mv moniwiki-1.2.5p1 /var/www/html/moniwiki &&\
  chown -R www-data:www-data /var/www/html/moniwiki &&\
  chmod 777 /var/www/html/moniwiki/data/ /var/www/html/moniwiki/ &&\
  chmod +x /var/www/html/moniwiki/secure.sh &&\
  /var/www/html/moniwiki/secure.sh

RUN a2enmod rewrite

ENV APACHE_RUN_USER www-data
ENV APACHE_RUN_GROUP www-data
ENV APACHE_LOG_DIR /var/log/apache2

EXPOSE 80
CMD bash -c "source /etc/apache2/envvars && /usr/sbin/apache2 -D FOREGROUND"
```

맨 윗줄부터 살펴보면 다음과 같다.

```docker
FROM ubuntu:14.04
```

**FROM**으로 기반 이미지를 지정한다.

```docker
RUN apt-get update &&\
  apt-get -qq -y install git curl build-essential apache2 php5 libapache2-mod-php5 rcs
```

**RUN**으로 직접 명령어를 실행한다. 여러 패키지들을 여기서 설치하게 된다.

```docker
WORKDIR /tmp
```

**WORKDIR**로 이후 실행되는 모든 작업의 디렉터리를 변경한다. RUN으로 cd를 매번 앞에 사용하는 것은 작업 위치가 매번 초기화되기에 해당 방법이 더 편리하다.

```docker
RUN \
  curl -L -O https://github.com/wkpark/moniwiki/archive/v1.2.5p1.tar.gz &&\
  tar xf /tmp/v1.2.5p1.tar.gz &&\
  mv moniwiki-1.2.5p1 /var/www/html/moniwiki &&\
  chown -R www-data:www-data /var/www/html/moniwiki &&\
  chmod 777 /var/www/html/moniwiki/data/ /var/www/html/moniwiki/ &&\
  chmod +x /var/www/html/moniwiki/secure.sh &&\
  /var/www/html/moniwiki/secure.sh

RUN a2enmod rewrite
```

이제 모니위키를 셋업한다.

첫 번째 **RUN**

1. 깃허브에서 다운로드
2. 압축 해제
3. 디렉터리 변경
4. 접근 권한 설정

두 번째 **RUN**

- 아파치2의 모듈 활성화

```docker
ENV APACHE_RUN_USER www-data
ENV APACHE_RUN_GROUP www-data
ENV APACHE_LOG_DIR /var/log/apache2
```

**ENV**로 컨테이너 실행 환경에 적용될 환경변수 기본값을 지정한다.

```docker
EXPOSE 80
CMD bash -c "source /etc/apache2/envvars && /usr/sbin/apache2 -D FOREGROUND"
```

**EXPOSE**로 가상머신에 오픈할 포트를 지정한다.

**CMD**로 컨테이너에서 실행될 명령어를 지정해준다. 이 명령어는 기본값이며 컨테이너 실행시 덮어쓸 수 있다.

이제 도커파일을 빌드시킨 뒤 실행시킨다.

```bash
$ docker build -t 생성할이미지명 .
...
$ docker run -d -p 9999:80 이미지명
```

-d 플래그로 컨테이너를 백그라운드 실행시키며 -p를 통해 포트포워딩을 지정한다. (외부포트:내부포트)

컨테이너에서는 아파치가 80포트로 실행되고 있기 때문에 여기에서는 9999포트로 들어오는 연결을 컨테이너 80포트에서 실행중인 아파치 서버로 보낸다.

이제 페이지를 요청해본다.

![](In-My-Job/Docker/img/2.png)

### 도커 이미지 빌드 과정

Dockerfile의 한 줄 한 줄은 레이어라는 형태로 저장된다. 레이어를 줄이면 캐시 또한 효율적으로 관리된다. 따라서 && 및 \를 사용해 여러 명령어 적절하게 붙여 사용한다.

도커 빌드 과정

- 지시자 하나(한 스텝)씩 빌드하게 된다.
1. 컨테이너 생성
2. 지시자 실행
3. 임시 이미지 생성

→ 지시자 수(스텝 수) 만큼 반복한다.

`docker history`명령어로 과정 중 생성된 중간 이미지들을 확인 가능하다.
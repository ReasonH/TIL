## 1. 애플리케이션과 시스템 내 단일 컨테이너의 적정 비중

실제 운영에서는 애플리케이션을 컨테이너 안에 어떻게 배치하는지가 매우 중요하다.

단일 컨테이너의 적정 비중은 책임과 복잡도를 고려해야한다.

### 컨테이너 1개 = 프로세스 1개?

웹 애플리케이션과 워커형 상주 애플리케이션 등 프로세스 하나를 하나의 컨테이너로 만드는 방식?

**정기적으로 작업을 실행하는 에플리케이션**

- 스케쥴러 + 작업 = 1 애플리케이션인 경우 컨테이너 1개로 가능
- 스케쥴러를 갖추지 않았다면? → 외부 기능 의존 (cron job 등)
    - 이 경우 프로세스 2개 → 컨테이너 2개가 됨

즉 다음과 같은 방식으로 구성 가능

1. 컨테이너 2개가 API로 통신하는 방식
2. Cron 컨테이너 위에 Work 컨테이너를 실행하는 방식

이는 너무 복잡하며 실제로 하나로 합쳐서 구동이 가능하다.

즉, 프로세스 하나를 컨테이너 하나로 억지로 만드는 것은 생산성이 높지 않다.

### 컨테이너 1개에 하나의 관심사

컨테이너 하나는 한 가지 역할이나 문제 영역(도메인)에 집중해야 한다.

그러나 지나치게 세세하게 나누는 것은 좋지 않음.

각 컨테이너가 맡을 역할을 적절히 나누고, 그 역할에 따라 배치한 컨테이너를 복제해도 전체 구조에서 부작용이 일어나지 않는가?

## 2. 컨테이너의 이식성

### 커널 및 아키텍쳐의 차이

모든 환경에서 동작하는 컨테이너는 없다.

### 라이브러리와 동적 링크 문제

- 정적 링크: 애플리케이션에 라이브러리를 포함하기 때문에 이식성은 뛰어남
- 동적 링크: 애플리케이션 크기는 작아지지만 호스트가 라이브러리를 갖춰야함

ADD, COPY instruction 등 호스트에서 파일을 복사해오는 기능을 통해 애플리케이션을 컨테이너 외부에서 주입하는 경우도 적지 않다.

해결법

1. 네이티브 라이브러리를 정적 링크해 빌드
2. 모든 빌드 프로세스를 수행하는 도커 컨테이너를 만들고 그 안에서 애플리케이션 빌드
    - 다른 컨테이너에서 이를 사용
3. 컨테이너를 빌드용과 실행용으로 분리해서 사용

## 3. 도커 친화적인 애플리케이션

### 환경 변수 활용

도커 컨테이너 형태로 실행되는 애플리케이션을 제어하는 방법

1. 실행 시 인자 사용

    실행시 인자가 많아지면 내부 변수 매핑이 복잡해지거나 CMD, ENTRYPOINT 내용 관리가 어려워질 수 있다

2. 설정 파일

    실행할 애플리케이션에 환경 이름을 부여하고 그에따라 설정 파일을 바꿔가며 사용하는 방식

    JAVA Gradle, Maven, Ruby on rails 등이 있음

    각 설정별로 도커 이미지를 만든다? → 다른 환경에서 애플리케이션 실행하려면 이미지를 새로 빌드해야함

    호스트에 위치한 환경별 설정 파일을 컨테이너에 마운트? → 호스트 의존성이 생기게됨

3. 애플리케이션 동작을 환경 변수로 제어

    환경 변수 값을 바꿔도 컨테이너만 다시 시작하면 된다. → 시간 절약

    JSON, XML 등 계층적 구조를 갖는 설정 파일보다 매핑 처리에 수고가 더 많이든다.

4. 설정 파일에 환경 변수를 포함

    설정 파일 + 환경 변수 장점을 합친다 (모든 프레임워크가 지원하는 방식은 아님)

## 4. 퍼시스턴스 데이터를 다루는 방법

컨테이너 실행 중 작성, 수정된 파일은 호스트 파일 시스템에 마운트되지 않는 한 컨테이너 파기 시 함께 삭제된다. Stateful 유형은 파기된 컨테이너를 동일하게 재현하는게 어려움

컨테이너를 이용해 상태를 갖는 애플리케이션을 운영하기 위해서는 이전 버전의 컨테이너에서 사용하던 파일, 디렉토리를 그대로 이어받아 사용할 수 있어야한다. ⇒ 이 때 **데이터 볼륨을 사용한다.**

### 데이터 볼륨

도커 컨테이너 안의 디렉터리를 디스크에 퍼시스턴스 데이터로 남기기 위한 메커니즘, 호스트와 컨테이너 사이의 디렉터리 공유 및 재사용 기능을 제공한다.

이미지를 수정하고 새로 컨테이너를 생성해도 같은 데이터 볼륨이 사용 가능하다. 데이터 볼륨은 컨테이너를 파기해도 디스크에 그대로 남기에 Stateful 애플리케이션 실행에 적합하다.

```bash
docker container run [options] -v host_directory:container_directory repo_name[:tag][명령][명령인자]
```

jenkins 예제에서 사용한 것이 -v 옵션이다.

### 데이터 볼륨 컨테이너

컨테이너 - 호스트가 아닌 컨테이너 - 컨테이너 데이터 공유, 퍼시스턴스 데이터를 볼륨으로 만들어 다른 컨테이너에 공유

데이터 볼륨 컨테이너가 저장한 데이터 또한 호스트의 스토리지에 저장된다는 점은 동일하다

- 데이터 볼륨 컨테이너는 도커가 관리하는 영역에만 영향을 미친다
- 즉, 호스트 머신이 컨테이너에 미치는 영향을 최소한으로 억제
- 볼륨을 필요로 하는 컨테이너가 사용할 호스트 디렉토리를 알 필요가 없음 → 데이터 제공 볼륨 컨테이너만 지정하면 된다
- 데이터 볼륨이 데이터 볼륨 컨테이너로 캡슐화되어 호스트에 대해 아는 것이 없어도 데이터 사용 가능, 결합이 느슨해져서 애플리케이션 컨테이너와 데이터 볼륨 교체 가능

**데이터 볼륨에 MySQL 데이터 저장**

데이터 볼륨 컨테이너 역할을 할 이미지 생성을 위한 Dockerfile

```docker
FROM busybox

VOLUME /var/lib/mysql

CMD ["bin/true"]
```

Mysql-data 이미지 생성 및 컨테이너 실행

```bash
$ docker image build -t example/mysql-data:latest
$ docker container run -d --name mysql-data example/mysql-data:latest
```

Mysql 컨테이너 실행, 볼륨 컨테이너에 마운트

```bash
$ docker cotainer run -d --rm --name mysql \
> -e "MYSQL_ALLOW_EMPTY_PASSWORD=yes" \
> -e "MYSQL_DATABASE=volume_test" \
> -e "MYSQL_USER=example" \
> -e "MYSQL_PASSWORD=example" \
> --volumes-from mysql-data \
> mysql:5.7
```

```bash
$ docker container exec -it mysql mysql -u root -p volume_test

Enter password:
Welcome to the MySQL monitor.  Commands end with ; or \g.
Your MySQL connection id is 2
Server version: 5.7.29 MySQL Community Server (GPL)

Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.

Oracle is a registered trademark of Oracle Corporation and/or its
affiliates. Other names may be trademarks of their respective
owners.

Type 'help;' or '\h' for help. Type '\c' to clear the current input statement.

mysql> create table user(
    -> id int primary key auto_increment,
    -> name varchar(255)
    -> ) engine=InnoDB default charset=utf8mb4 collate utf8mb4_unicode_ci;
Query OK, 0 rows affected (0.03 sec)

mysql> insert into user (name) values ('gihyo'), ('docker'), ('Solomon Hykes');
Query OK, 3 rows affected (0.02 sec)
Records: 3  Duplicates: 0  Warnings: 0

mysql> select * from user
    -> ;
+----+---------------+
| id | name          |
+----+---------------+
|  1 | gihyo         |
|  2 | docker        |
|  3 | Solomon Hykes |
+----+---------------+
3 rows in set (0.00 sec)
```

이제 컨테이너를 정지(rm옵션이기 때문에 자동삭제) 한 뒤 다른 컨테이너를 실행해서 데이터를 확인하면 데이터가 남아있다.

**데이터 익스포트 및 복원**

데이터 볼륨은 범위가 결국 같은 도커 호스트 안으로 제한됨

```bash
$ docker container run -v ${PWD}:/tmp \
--volumes-from mysql-data \
busybox \
tar cvzf /tmp/mysql-backup.tar.gz /var/lib/mysql
```

busy박스 이미지를 이용해 컨테이너를 만든다. 컨테이너는 호스트 볼륨 pwd와 마운트한다.

동시에, 데이터 볼륨 컨테이너 mysql-data에서 데이터를 압축하여 tmp 폴더로 가져온다.
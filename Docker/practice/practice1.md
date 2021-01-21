### 우분투 설치 및 컨테이너 실행

```bash
$ docker pull ubuntu:bionic
...
$ docker run -it ubuntu:bionic bash
```

### 컨테이너에 깃 설치

**설치 전, 다른 터미널에서 diff 명령어를 통해 버전을 확인한다. 이는 부모 이미지와 파생된 컨테이너의 파일 시스템 간 변경사항을 확인할 수 있다.**

```bash
$ docker diff 컨테이너ID
-> 아무런 결과도 나오지 않는다.
```

이 컨테이너는 이미지의 파일 시스템 그대로이다. 아무것도 출력되지 않는다.

다음으로 git을 설치한다.

```bash
/# 
```

다시 diff를 실행한다.

```bash
C /var
C /var/lib
C /var/lib/apt
C /var/lib/apt/lists
A /var/lib/apt/lists/archive.ubuntu.com_ubuntu_dists_bionic-backports_main_binary-amd64_Packages.lz4
A /var/lib/apt/lists/archive.ubuntu.com_ubuntu_dists_bionic-updates_restricted_binary-amd64_Packages.lz4
A /var/lib/apt/lists/archive.ubuntu.com_ubuntu_dists_bionic_multiverse_binary-amd64_Packages.lz4
A /var/lib/apt/lists/security.ubuntu.com_ubuntu_dists_bionic-security_InRelease
A /var/lib/apt/lists/security.ubuntu.com_ubuntu_dists_bionic-security_universe_binary-amd64_Packages.lz4
A /var/lib/apt/lists/archive.ubuntu.com_ubuntu_dists_bionic-backports_InRelease
```

A는 ADD, C는 Change, D는 Delete를 의미한다. 패키지 하나를 설치한 것 만으로도 많은 파일들이 추가 및 변경되었다. 이제 해당 컨테이너는 git이 설치된 상태이지만 원래의 이미지로 새로운 컨테이너를 실행 시 git은 존재하지 않는다.

### 깃이 설치된 컨테이너로 새로운 이미지 생성

이제 **git이 설치된 ubuntu:bionic** 이미지로 새로운 이미지를 생성한다. 도커에서는 이 작업을 commit이라 한다.

```bash
$ docker commit 컨테이너ID 생성할이미지이름:태그
$ docker images
REPOSITORY   TAG       IMAGE ID       CREATED         SIZE
ubuntu       git       b54a088c7956   5 seconds ago   98.2MB
centos       latest    300e315adb2f   6 weeks ago     209MB
ubuntu       bionic    2c047404e52d   7 weeks ago     63.3MB
```

이제 새로운 이미지로 컨테이너를 실행시키면 git이 설치되어 있음을 확인할 수 있다.

**도커 이미지의 생명주기**

1. 이미지 pull
2. commit으로 새로운 이미지 생성
3. rmi를 통한 이미지 삭제
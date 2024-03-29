# 1. HTTP 개관

### 1.1 HTTP

HTTP는 전 세계의 웹 서버로부터 대량의 정보를 빠르고, 간편하고, 정확하게 사람들의 PC에 설치된 웹 브라우저로 옮긴다. HTTP 통신은 신뢰성을 보장하는 프로토콜을 사용한다.

### 1.2 웹 클라이언트와 서버

HTTP 요청을 보내고 받는 HTTP 클라이언트 / 서버는 WWW의 기본 요소이다.

웹 브라우저가 요청을 보내면 서버는 응답과 응답 정보를 함께 내려준다.

### 1.3 리소스

웹 리소스는 웹 콘텐츠의 원천이다. 리소스는 텍스트, 이미지 등 정적 파일일 수도 있고, 라이브 콘텐츠 등의 동적 콘텐츠일 수도 있다.

**1.3.1 미디어 타입**

웹 서버는 전송되는 모든 HTTP 객체에 MIME 타입이라는 데이터 포맷 라벨을 붙인다. 이는 멀티미디어 콘텐츠를 기술하고 라벨을 붙이기 위해 사용된다. 웹 브라우저는 서버로부터 객체를 돌려받을 때 다룰 수 있는 객체인지 MIME 타입을 통해 확인한다.

MIME:`주 타입/부 타입` 형태로 이루어진 문자열 라벨 ex) `text/plain`

**1.3.2 URI**

웹 서버 리소스는 각자 이름을 갖는다. URI는 이런 정보 리소스를 고유하게 식별하는 통합 자원 식별자이다. URI에는 URL과 URN이 있다.

**1.3.3 URL**

리소스 식별자의 가장 흔한 형태, 특정 서버의 리소스에 대한 구체적인 위치를 알려준다.

다음과 같은 구성을 갖는다.

1. scheme: 리소스에 접근하기 위해 사용되는 프로토콜 서술
2. 서버의 인터넷 주소
3. 리소스

⇒ http:://www.naver.com/index.html

책에서는 URL == URI로 사용한다.

**1.3.4 URN**

한 리소스에 대해 리소스의 위치에 영향받지 않는 유일무이한 이름, 리소스가 이름을 유지하는 한 여러 종류의 네트워크 접속 프로토콜로 접근해도 문제없다. 다만 많이 채택되지 않았다.

### 1.4 트랜잭션

클라이언트와 웹 서버가 리소스를 주고받기 위해 사용하는 HTTP 트랜잭션은 요청 명령과 응답 결과로 구성된다.

**1.4.1 메서드**

모든 HTTP 요청 메세지는 서버에서 취할 동작을 지정하는 한 개의 메서드를 갖는다.

**1.4.2 상태 코드**

모든 HTTP 응답 메세지는 상태 코드와 함께 반환된다. 이는 요청 성공, 실패 등에 대해 클라이언트에 알려준다.

**1.4.3 웹페이지는 여러 객체로 이루어질 수 있다.**

웹 페이지는 HTML 페이지 레이아웃, 그래픽, 이미지 등 여러 리소스의 모음으로 이루어진다.

### 1.5 메시지

HTTP는 단순한 줄 단위의 문자열로 요청 메시지 / 응답 메시지만 존재한다. 메시지의 구성 요소는 다음과 같다.

1. 시작줄: 요청인 경우 무엇을 해야하는지, 응답인 경우 무엇이 일어났는지 서술
2. 헤더: 구문 분석을 위한 Key: Value 모음, 마지막 줄은 공백
3. 본문: 텍스트, 이진 데이터 등 요청과 응답의 본문

### 1.6 TCP 커넥션

커넥션을 통한 이동 방식

**1.6.1 TCP/IP**

HTTP가 신경쓰지 않는 통신 세부사항을 담당, 신뢰성 보장하는 패킷 교환 네트워크 프로토콜 집합

커넥션이 연결되면 메시지의 손실, 손상, 순서변경이 없음을 보장

**1.6.2 접속, IP 주소와 포트번호**

HTTP 클라이언트가 서버에 메세지 전송을 할 수 있으려면 IP 주소(URL 주소)와 포트 번호를 이용해 TCP/IP 커넥션을 먼저 맺어야 한다.

### 1.7 프로토콜 버전

생략

### 1.8 웹 구성요소

**1.8.1 프록시**

클라이언트와 서버 사이에서 요청/응답 필터링 및 중개, 사용자를 대신해 서버에 요청, 성능 최적화 및 보안

**1.8.2 캐시**

자주 사용되는 리소스의 사본을 저장해두는 특별한 종류의 프록시, 성능 및 효율성

**1.8.3 게이트웨이**

클라이언트와 서버 간 중개자, 프로토콜 변환 등

**1.8.4 터널**

주로 비 HTTP 데이터를 HTTP연결을 이용해 raw 상태로 전송하기 위해 사용, SSL 통신에서 사용

**1.8.5 에이전트**

사용자를 위해 HTTP 요청을 만드는 클라이언트 프로그램 (웹브라우저, 모바일 OS 등)

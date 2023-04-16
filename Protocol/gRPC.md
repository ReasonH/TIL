## gRPC

google에서 개발한 RPC 통신 방식, proto buf를 이용한 byte stream 통신

RPC에서는 클라이언트가 서버의 메서드를 직접 호출해서 통신한다.

### protobuf (protocol buffer)

proto buffer는 아래 요소들의 조합을 의미한다.

- (.proto 파일로 생성되는) 정의언어
- 데이터를 포함한 인터페이스를 만드는 proto 컴파일러
- 언어별 런타임 라이브러리
- 직렬화 포맷

### proto compile 과정

1. IDL(Interface Definition Language)로서 data structure를 정의(.proto 파일)
2. protocol buffer compiler(protoc)를 이용해 compile → java의 경우 class / class builder 생성
3. Complie된 소스 코드를 사용하여 다양한 데이터 스트림에서 다양한 언어로 다양한 구조의 데이터 처리

```protobuf
syntax = "proto3";

package proto.hello;

option java_package = "proto.hello";
option java_multiple_files = true;

service HelloService {
  rpc Hello (HelloRequest) returns (HelloResponse) {}
}

message HelloRequest {
  string name = 1;
}

message HelloResponse {
  string response = 1;
}
```

**protoc가 생성하는 코드**

1. 요청, 응답 모델 (Request, Response) → 본문의 `message` → proto buf
2. service 인터페이스 (MVC 패턴에서는 Controller라고 불린다) → 본문의 `service` → gRPC
3. stub

### Server side

Service에 정의돈 method 구현 및 gRPC server 실행

decode incoming request ⇒ excute service method ⇒ encode response

### Client side

stub (=각자의 언어로 service와 같은 메서드를 구현하고 있는 객체)에 method 호출

⇒ server에

### 관리

- 서비스 API를 투명하게 관리하기 위해 IDL을 위한 독립 레포지터리 생성
- protoc를 이용해 생성된 generated 파일 (gen 폴더)까지 포함
  - protobuf와 gRPC는 protoc 버전, plugin 버전에 따라 생성되는 결과물이 다를 수 있는데 이를 통일하지 않고 각자 generate 해 사용하면 문제가 될 수 있기 때문

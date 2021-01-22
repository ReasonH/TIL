k8s에서 볼륨이란 Pod에 종속되는 디스크이다. Pod 단위이기에 Pod에 속해 있는 여러개의 컨테이너가 공유해서 사용될 수 있다.

## 볼륨 종류

로컬 디스크, NFS 등 일반적인 외장 디스크 인터페이스, Ceph 등의 오픈소스 파일 시스템, 퍼블릭 클라우드, 프라이빗 클라우드 등 다양한 볼륨을 지원한다. 크게 **임시, 로컬, 네트워크** 디스크로 나뉨

### emptyDir

Pod가 생성될때 생성되고, Pod가 삭제될 때 같이 삭제되는 임시 볼륨.

Pod 내의 컨테이너가 크래쉬되어 삭제 및 재시작되어도 emptyDir의 생명주기는 Pod 단위이기에 삭제되지 않는다.

emptyDir의 내용은 물리적 디스크(로컬, 네트워크 등)에 저장이 된다. emptyDir.medium 필드에 "Memory"라고 지정 시, emptyDir 내용은 물리 디스크 대신 메모리에 저장이 된다.

다음은 하나의 Pod에 nginx와 redis 컨테이너를 가동 시키고, emptyDir 볼륨을 생성하여 이를 공유하는 설정이다.

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: shared-volumes 
spec:
  containers:
  - name: redis
    image: redis
    volumeMounts:
    - name: shared-storage
      mountPath: /data/shared # 여기에 마운트한다.
  - name: nginx
    image: nginx
    volumeMounts:
    - name: shared-storage
      mountPath: /data/shared # 여기에 마운트한다.
  volumes: # emptyDir 기반 볼륨 생성
  - name : shared-storage
    emptyDir: {}
```
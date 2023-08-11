## 카프카 cli
cli 툴 사용 시, 필수 / 선택 옵션을 어느정도 알고 있는게 좋다. 이를 모르고 사용할 경우, 문제가 생길 수 있다.

### kafka-topics.sh 
- 토픽 생성
- 토픽 조회
- 토픽의 상태 (config 정보 등)
- 파티션 개수 증가

#### 토픽 생성
```sh
kafka_2.12-2.5.0  bin/kafka-topics.sh --create --bootstrap-server m
y-kafka:9092 --topic hello.kafka

WARNING: Due to limitations in metric names, topics with a period ('.') or underscore ('_') could collide. To avoid issues it is best to use either, but not both.
Created topic hello.kafka.

➜  kafka_2.12-2.5.0  bin/kafka-topics.sh --bootstrap-server my-kafka:9092 --topic hello.kafka --describe
Topic: hello.kafka	PartitionCount: 1	ReplicationFactor: 1	Configs: segment.bytes=1073741824
	Topic: hello.kafka	Partition: 0	Leader: 0	Replicas: 0	Isr: 0
```

#### 옵션으로 토픽 생성
```sh
➜  kafka_2.12-2.5.0  bin/kafka-topics.sh --create --bootstrap-server my-kafka:9092 --partitions 10 --replication-factor 1 --topic hello.kafka2 --config retention.ms=172800000

➜  kafka_2.12-2.5.0  bin/kafka-topics.sh --bootstrap-server my-kafka:9092 --topic hello.kafka2 --describe
Topic: hello.kafka2	PartitionCount: 10	ReplicationFactor: 1	Configs: segment.bytes=1073741824,retention.ms=172800000
	Topic: hello.kafka2	Partition: 0	Leader: 0	Replicas: 0	Isr: 0
	Topic: hello.kafka2	Partition: 1	Leader: 0	Replicas: 0	Isr: 0
	Topic: hello.kafka2	Partition: 2	Leader: 0	Replicas: 0	Isr: 0
	Topic: hello.kafka2	Partition: 3	Leader: 0	Replicas: 0	Isr: 0
	Topic: hello.kafka2	Partition: 4	Leader: 0	Replicas: 0	Isr: 0
	Topic: hello.kafka2	Partition: 5	Leader: 0	Replicas: 0	Isr: 0
	Topic: hello.kafka2	Partition: 6	Leader: 0	Replicas: 0	Isr: 0
	Topic: hello.kafka2	Partition: 7	Leader: 0	Replicas: 0	Isr: 0
	Topic: hello.kafka2	Partition: 8	Leader: 0	Replicas: 0	Isr: 0
	Topic: hello.kafka2	Partition: 9	Leader: 0	Replicas: 0	Isr: 0
```

#### 토픽 생성 및 파티션 수 변경
```sh
➜  kafka_2.12-2.5.0  bin/kafka-topics.sh --create --bootstrap-server my-kafka:9092 --topic test
Created topic test.

➜  kafka_2.12-2.5.0  bin/kafka-topics.sh --describe --bootstrap-server my-kafka:9092 --topic test
Topic: test	PartitionCount: 1	ReplicationFactor: 1	Configs: segment.bytes=1073741824
	Topic: test	Partition: 0	Leader: 0	Replicas: 0	Isr: 0

➜  kafka_2.12-2.5.0  bin/kafka-topics.sh --alter --partitions 10 --bootstrap-server my-kafka:9092 --topic test

➜  kafka_2.12-2.5.0  bin/kafka-topics.sh --describe --bootstrap-server my-kafka:9092 --topic test
Topic: test	PartitionCount: 10	ReplicationFactor: 1	Configs: segment.bytes=1073741824
	Topic: test	Partition: 0	Leader: 0	Replicas: 0	Isr: 0
	Topic: test	Partition: 1	Leader: 0	Replicas: 0	Isr: 0
	Topic: test	Partition: 2	Leader: 0	Replicas: 0	Isr: 0
	Topic: test	Partition: 3	Leader: 0	Replicas: 0	Isr: 0
	Topic: test	Partition: 4	Leader: 0	Replicas: 0	Isr: 0
	Topic: test	Partition: 5	Leader: 0	Replicas: 0	Isr: 0
	Topic: test	Partition: 6	Leader: 0	Replicas: 0	Isr: 0
	Topic: test	Partition: 7	Leader: 0	Replicas: 0	Isr: 0
	Topic: test	Partition: 8	Leader: 0	Replicas: 0	Isr: 0
	Topic: test	Partition: 9	Leader: 0	Replicas: 0	Isr: 0
```

### kafka-configs.sh
- 토픽 옵션 중 일부는 configs 쉘을 통해 설정해야 한다. --alter와 --add-config를 사용해 min.insync.replicas를 토픽별로 설정할 수 있다.
- 또한, `server.properties`에 설정된 브로커 기본 옵션을 조회하는 데도 활용 가능하다.

#### 토픽 생성 후 min.insync.replicas 적용
```sh
➜  kafka_2.12-2.5.0  bin/kafka-topics.sh --describe --bootstrap-server my-kafka:9092 --topic test
Topic: test	PartitionCount: 10	ReplicationFactor: 1	Configs: segment.bytes=1073741824
	Topic: test	Partition: 0	Leader: 0	Replicas: 0	Isr: 0
	Topic: test	Partition: 1	Leader: 0	Replicas: 0	Isr: 0
	Topic: test	Partition: 2	Leader: 0	Replicas: 0	Isr: 0
	Topic: test	Partition: 3	Leader: 0	Replicas: 0	Isr: 0
	Topic: test	Partition: 4	Leader: 0	Replicas: 0	Isr: 0
	Topic: test	Partition: 5	Leader: 0	Replicas: 0	Isr: 0
	Topic: test	Partition: 6	Leader: 0	Replicas: 0	Isr: 0
	Topic: test	Partition: 7	Leader: 0	Replicas: 0	Isr: 0
	Topic: test	Partition: 8	Leader: 0	Replicas: 0	Isr: 0
	Topic: test	Partition: 9	Leader: 0	Replicas: 0	Isr: 0

➜  kafka_2.12-2.5.0  bin/kafka-configs.sh --bootstrap-server my-kafka:
9092 --alter --add-config min.insync.replicas=2 --topic test
Completed updating config for topic test.

➜  kafka_2.12-2.5.0  bin/kafka-topics.sh --describe --bootstrap-server my-kafka:9092 --topic test
Topic: test	PartitionCount: 10	ReplicationFactor: 1	Configs: min.insync.replicas=2,segment.bytes=1073741824
	Topic: test	Partition: 0	Leader: 0	Replicas: 0	Isr: 0
	Topic: test	Partition: 1	Leader: 0	Replicas: 0	Isr: 0
	Topic: test	Partition: 2	Leader: 0	Replicas: 0	Isr: 0
	Topic: test	Partition: 3	Leader: 0	Replicas: 0	Isr: 0
	Topic: test	Partition: 4	Leader: 0	Replicas: 0	Isr: 0
	Topic: test	Partition: 5	Leader: 0	Replicas: 0	Isr: 0
	Topic: test	Partition: 6	Leader: 0	Replicas: 0	Isr: 0
	Topic: test	Partition: 7	Leader: 0	Replicas: 0	Isr: 0
	Topic: test	Partition: 8	Leader: 0	Replicas: 0	Isr: 0
	Topic: test	Partition: 9	Leader: 0	Replicas: 0	Isr: 0
```

#### 브로커 0번의 모든 기본 설정 조회
```sh
➜  kafka_2.12-2.5.0  bin/kafka-configs.sh --bootstrap-server my-kafka:9092 --broker 0 --all --describe
All configs for broker 0 are:
... 중략
```

### kafka-console-producer.sh
- 특정 토픽에 데이터를 넣는 용도 (일반적으로 테스트 용도로 사용)
- key 관련 옵션 설정을 통해 메시지 키를 설정할 수 있다.

#### 메시지 키?
- 메시지 키는 일반적으로 같이 처리되어야 하는 레코드, 순서가 보장되어야 하는 레코드 등에 사용한다. 
- 메시지 키를 설정하지 않는 경우 (null) 레코드는 라운드 로빈으로 파티션에 전송된다. 반면 동일한 메시지 키를 갖는 레코드는 동일한 파티션에 적재된다.

#### produce
```sh
➜  kafka_2.12-2.5.0  bin/kafka-console-producer.sh --bootstrap-server my-kafka:9092 --topic hello.kafka
>hello
>kafka
>0
>1
>2
>3
>4
>5
>6

➜  kafka_2.12-2.5.0  bin/kafka-console-producer.sh --bootstrap-server my-kafka:9092 --topic hello.kafka --property "parse.key=true" --proper
ty "key.separator=:"
>k1:msg
>k2:new
>k3:msg~
>k1:no3
```

### kafka-console-consumer.sh
- 브로커에 전송된 데이터 확인
- --from-beginning: 처음 데이터부터 조회 가능
- --property: 레코드를 k-v 형태로 조회 가능
- --max-messages: 최대 consume 갯수
- --partition: 특정 파티션만 consume
- --group: consumer group을 기반으로 console-consumer가 동작
	- consumer group이 있어야만 토픽에 대한 offset 관리가 된다.

```sh
➜  kafka_2.12-2.5.0  bin/kafka-console-consumer.sh --bootstrap-server my-kafka:9092 --topic hello.kafka --from-beginning --property key.separator=":" --property print.key=true --max-messages 10 --group hello-group
null:hello
null:kafka
null:0
null:1
null:2
null:3
null:4
null:5
null:6
k1:msg
Processed a total of 10 messages

➜  kafka_2.12-2.5.0  bin/kafka-topics.sh --bootstrap-server my-kafka:9092 --list
__consumer_offsets # group consume을 통해 offset을 관리하는 토픽이 새로 생김
hello.kafka
hello.kafka2
test
```


```sh
➜  kafka_2.12-2.5.0  bin/kafka-console-producer.sh --bootstrap-server my-kafka:9092 --topic test
>1
>2
>3

➜  kafka_2.12-2.5.0  bin/kafka-console-consumer.sh --bootstrap-server my-kafka:9092 --topic test
1
2
3
```

### kafka-consumer-groups.sh
컨슈머 그룹의 상태 조회
- 파티션 번호, 현재 레코드 오프셋, 파티션 마지막 레코드 오프셋
- 컨슈머 랙 (최종 오프셋 - 현재 오프셋) 등
	- 컨슈머 랙은 컨슈머 지연 정도를 나타낸다.

오프셋 리셋
- 토픽에 대한 파티션 데이터를 재처리하기 위해 사용
- 다양한 리셋 종류 제공
```sh
➜  kafka_2.12-2.5.0  bin/kafka-consumer-groups.sh --bootstrap-server my-kafka:9092 --group hello-group --describe

Consumer group 'hello-group' has no active members.

GROUP           TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID     HOST            CLIENT-ID
hello-group     hello.kafka     0          10              10              0               -               -               -

➜  kafka_2.12-2.5.0  bin/kafka-consumer-groups.sh --bootstrap-server my-kafka:9092 --group hello-group --topic hello.kafka --reset-offsets -
-to-earliest --execute

GROUP                          TOPIC                          PARTITION  NEW-OFFSET
hello-group                    hello.kafka                    0          0

➜  kafka_2.12-2.5.0  bin/kafka-console-consumer.sh --bootstrap-server my-kafka:9092 --topic hello.kafka --group hello-group
hello
kafka
0
1
2
3
4
5
6
msg
```

### 그 외
kafka-consumer-perf-test.sh
- 프로듀서로 퍼포먼스를 측정할 때 사용, 프로듀서와 브로커 간 네트워크 체크에 사용

kafka-producer-perf-test.sh
- 컨슈머로 퍼포먼스 측정 시 사용, 브로커와 컨슈머 간 네트워크 체크에 사용

kafka-reassign-partitions.sh
- 클러스터 내 파티션 재분산
- auto.leader.rebalance.enable 옵션은 클러스터 내 리더 파티션 위치를 주기적으로 파악하고 자동으로 리밸런싱하도록 도움

kafka-delete-record.sh
- 특정 offset 이전 레코드에 delete 플래그

### 특성에 맞는 토픽 생성
- cli 툴을 이용한 직접 토픽 생성 외에 프로듀서 / 컨슈머를 통해 토픽이 생성되도록 할 수도 있다.
- 그러나 관리를 위해 명시적으로 레코드 특성에 맞게 생성하는게 낫다.

---
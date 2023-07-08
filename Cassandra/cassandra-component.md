# Cassandra Cluster

Cassandra의 컴포턴트 그리고 데이터 복제 전략에 대해 알아본다.

## Cassandra Component

먼저 Cassandra DB에서 주로 알아야 할 5가지 컴포넌트는 다음과 같다.
1. Cluster
2. Data Center (DC)
3. Rack
4. Node
5. V-Node

### Node / VNode

#### Node
노드는 Cassandra의 가장 기본 구성요소이다. 클러스터 내의 각 노드 모두 읽기 / 쓰기가 가능한 독립적 시스템이며 클러스터 내 모든 노드의 역할은 동일하다. 노드 간에는 Gossip protocol을 통해 통신하며 Ring topology를 유지한다.

#### V-Node
V Node는 각 노드 하위의 가상 노드이다. [이전 글](cassandra.md)에서 언급했듯이, 각 노드는 파티션 키를 기반으로 하는 고유의 토큰 범위를 갖는다. V Node는 하나의 노드 내에서 이를 다시 분배 및 할당함으로써 시스템에 유연성을 제공한다. 
> 클러스터 내 노드 추가가 발생할 경우 토큰 값의 재분배, 즉 데이터 재할당이 필요하다. 가상노드를 활용하면 이런 상황에서 더 많은 streaming peers를 사용해 오버헤드를 제한하며 클러스터를 확장할 수 있다.

cassandra 4.0에서 기본 v node의 갯수는 16개이다. 1로 설정하는 경우 vNode를 사용하지 않는 것과 다름없다.

### Rack
Node의 Ring topology 내 논리적 그룹을 Rack이라한다. 복제본이 가능한 다른 렉에 분산되도록 함으로써 가용성을 확보할 수 있다. Snitch를 설정함으로써 특정 노드가 속한 Rack, DC를 결정하고 복제본이 어디에 위치할 지 지정할 수 있다. 특정 Rack이 죽을 경우 다른 Rack으로 지속적으로 데이터 접근이 가능하다.

### Data Center
데이터 센터는 복제 목적으로 클로스터에서 구성되는 노드 그룹이다. 최소 한 개 이상의 Rack을 보유하고 있다. DC를 유사한 노드(ex. 인접 위치)로 구성함으로써 지연을 최소화시킬 수 있다. 또한, 각 DC마다 별도의 replication factor를 구성하여 상황에 맞는 구성을 선택할 수 있다.

### Cluster
클러스터는 1개 이상의 DC로 구성되는 Cassandra 컴포넌트의 최상위 요소이다. DC / Rack / Node / VNode 등의 설정을 통해 최대한의 가용성을 확보한다.

## Replication Strategy

위에 계속 언급했지만, 클러스터 내 노드는 자신의 토큰 범위 데이터 뿐 아니라, 다른 노드의 데이터 복제본을 함께 소유한다. 따라서 노드 다운 시, 다른 노드를 통해 데이터에 접근이 가능하다. Cassandra는 Keyspace 생성 시 필수적으로 Replication Factor를 설정해야 한다.

Cassandra는 기본적으로 _SimpleStrategy_, _NetworkTopologyStrategy_ 등 몇 가지 복제 전략을 제공한다.

### SimpleStrategy
이 전략은 replication factor만큼 링 노드를 순회하며 데이터를 복제 저장한다.

### NetworkTopologyStrategy
이를 이용하면 각 DC에 대해 서로 다른 복제 요소를 지정할 수 있다. DC내 가용성을 극대화하기 위해 서로 다른 랙의 노드에 복제본을 할당한다. 프로덕션 환경에 배포한다면 단일 DC로 사용하더라도 이 전략을 사용하는 것을 권고하고 있다.

---
참고
- https://www.youtube.com/watch?v=3UPXw4cVMMs
- https://www.datastax.com/blog/distributed-database-things-know-cassandra-datacenter-racks
- https://www.baeldung.com/cassandra-replication-partitioning


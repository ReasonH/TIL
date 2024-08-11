현재 운영하는 플랫폼에 신규 게임이 론칭하게 되며 주기적으로 Deadlock 로그가 발생하는 현상이 있었다.

```
Deadlock found when trying to get lock; try restarting transaction
```

별도의 재시도 로직이 있어서 결과적으로는 정상 동작했겠지만, 로그를 그냥 둘 수 없으니 조치하기로 했다.

## Deadlock 탐색

_'Deadlock이 무엇인지, 왜 발생하는지 등에 대해서는 생략한다'_

먼저 Deadlock을 찾기 위해 MySQL 서버에서 다음 쿼리를 수행해봤다. (만약 Master-Slave 구조를 사용 중이라면 당연히 쓰기 작업이 수행됐던 마스터 노드에서 확인해야 한다.)

```java
show engine innodb status;
```

### 출력

```
*** (1) TRANSACTION:
TRANSACTION 92465172, ACTIVE 0 sec fetching rows
mysql tables in use 1, locked 1
LOCK WAIT 3 lock struct(s), heap size 360, 6 row lock(s), undo log entries 1
MySQL thread id 949396, OS thread handle 0x7fef8cfb6700, query id 28309277 127.0.0.1 root updating

<대기 중인 업데이트 쿼리를 표시> # --------- (1)

*** (1) HOLDS THE LOCK(S):
<어떤 락을 홀드하고 있는지 정보 제공>
*** (1) WAITING FOR THIS LOCK TO BE GRANTED:
<어떤 락을 대기하고 있는지 정보 제공>

*** (2) TRANSACTION:
TRANSACTION 92465171, ACTIVE 0 sec starting index read
mysql tables in use 36, locked 36
5 lock struct(s), heap size 1184, 3 row lock(s)
MySQL thread id 949674, OS thread handle 0x7fee7a600700, query id 28309288 localhost root Sending data

<대기 중인 업데이트 쿼리를 표시> # --------- (2)

*** (2) HOLDS THE LOCK(S):
<어떤 락을 홀드하고 있는지 정보 제공>
*** (2) WAITING FOR THIS LOCK TO BE GRANTED:
<어떤 락을 대기하고 있는지 정보 제공>

*** WE ROLL BACK TRANSACTION (1) # --------- (3)
```

db status는 다양한 정보를 제공하주는데, 여기에 최근 데드락에 대한 정보도 기록되어 있다. 언뜻 복잡해 보이지만 차근차근 보면 중요한 것은 주석이 표시된 세 부분이다.

(1), (2)는 데드락을 발생시키는 2개의 쿼리가 표시되어 있다. 보통은 **HOLDS THE LOCK**과 **WAITING FOR THIS LOCK TO BE GRANTED** 두 종류의 상세 로그가 출력되는데, 이 부분은 현재 트랜잭션이 어떤 Lock을 소유하고 있으며 어떤 Lock을 대기하고 있는지를 나타낸다. 이를 통해 대략적으로 어떤 서비스 메서드들이 경합을 발생시키는지 파악이 가능하다.

(3)에서는 서버가 데드락 해결을 위해 어떤 트랜잭션을 롤백시켰는지를 나타낸다. 예시에서는 (1)번 트랜잭션이 롤백되었음을 알 수 있다. 위의 `Deadlock found ...`로그는 여기에서 롤백된 트랜잭션에 출력된다.

### 시나리오 예상

상세 구조를 나타낼 순 없으나 대략 다음과 같은 과정으로 데드락이 발생했다.

1. 게임 클라이언트에서 유저가 특정 동작 수행을 한 경우, 서버로 각기 다른 API인 A와 B를 동시에 호출한다.
2. A와 B는 JPA @Transactional 메서드를 호출하며 메서드는 다음 과정을 수행한다.
    1. A: TestA entity 업데이트, TestB entity 업데이트
    2. B: TestB entity 업데이트, TestA entity 업데이트
3. 동시에 커밋 요청 후 Deadlock 발생

## Deadlock 처리(?)

### 해결 방법

일단 클라이언트의 호출 구조를 변경하는게 가장 쉬운 방식이지만, 이는 현재 불가능했다. 서버 쪽에서 수정할 수 있는 가장 쉬운 방식은 호출의 순서를 일관성 있게 바꾸는 것이다.

Deadlock은 근본적으로 서로 자원을 점유한 상태에서 자원을 추가 점유하려 하기에 발생하는 문제이다. A와 B의 Lock 획득 순서를 동일하게 만드는 경우, 점유 상태에서 추가 대기가 발생하지 않으며 이에 따라 Deadlock은 구조적으로 발생할 수 없다.

```
As-is

Tx1가 t1 X lock 획득 -> t2 X lock 대기 -> 데드락
Tx2가 t2 X lock 획득 -> t1 X lock 대기 -> 데드락

To-be

Tx1가 t1 X lock 획득 -> t2 X lock 획득 -> 커밋
-> Tx2가 t1 X lock 획득 -> t2 X lock 획득 -> 커밋
```

### @Transactional에서의 처리

먼저 @Transactional 메서드의 내부적인 갱신 순서를 맞추기로 했다. 이렇게 하면 flush되는 쿼리 순서가 보장될 것이라 생각했다.

**…이후 문제가 깔끔하게 해결될 줄 알았으나 재현 과정에서 여전히 데드락이 발생하고 있었다.**

이유가 뭘까? 실제 쿼리 로그를 찍어보니, 로직의 순서를 바꿔 놨음에도 두 @Transactional 메서드는 각기 다른 순서로 갱신 쿼리를 날리고 있었다. 더티체킹에 의한 flush는 @Transactional 메서드 내에서의 entity의 갱신 순서와는 전혀 상관이 없었다. 어떤 과정에서 Dirty checking flush가 발생하는지 상세히 알아보고자, 내부 구현을 살펴봤다.

## Flush 동작 확인

Transactional 메서드가 flush되는 시점에는 다음의 동작이 이뤄진다.

#### DefaultFlushEventListener.onFlush()

```java
public void onFlush(FlushEvent event) throws HibernateException {
    EventSource source = event.getSession();
    PersistenceContext persistenceContext = source.getPersistenceContext();
    if (persistenceContext.getNumberOfManagedEntities() > 0 || persistenceContext.getCollectionEntries().size() > 0) {
        try {
            source.getEventListenerManager().flushStart();
            this.flushEverythingToExecutions(event); // ------------------- (1)
            this.performExecutions(source); // ------------------- (2)
            this.postFlush(source);
        } finally {
            source.getEventListenerManager().flushEnd(event.getNumberOfEntitiesProcessed(), event.getNumberOfCollectionsProcessed());
        }

        this.postPostFlush(source);
        if (source.getFactory().getStatistics().isStatisticsEnabled()) {
            source.getFactory().getStatistics().flush();
        }
    }

}
```

저 부분에서 볼 수 있는 메서드들은 명칭을 볼 때 다음과 같은 역할을 할 것으로 추측된다.

1. `flushEverythingToExecutions`: event 기반으로 execution을 위해 모든 내용을 flush한다.
2. `performExecutions`: 실제 수행 단계 → DB에 쿼리를 날린다.

먼저 준비 단계로 보이는 `flushEverythingToExecutions`부터 알아보자.

### Flsuh 준비

#### AbstractFlushingEventListener.flushEverythingToExecutions()

→ `flushEntities`

```java
private int flushEntities(FlushEvent event, PersistenceContext persistenceContext) throws HibernateException {
    LOG.trace("Flushing entities and processing referenced collections");
    EventSource source = event.getSession();
    Iterable<FlushEntityEventListener> flushListeners = ((EventListenerRegistry)source.getFactory().getServiceRegistry().getService(EventListenerRegistry.class)).getEventListenerGroup(EventType.FLUSH_ENTITY).listeners();
    Entry<Object, EntityEntry>[] entityEntries = persistenceContext.reentrantSafeEntityEntries(); 
		// ------------------- (1)
    int count = entityEntries.length;
    Entry[] var7 = entityEntries;
    int var8 = entityEntries.length;

    for(int var9 = 0; var9 < var8; ++var9) { // ------------------- (2)
        Entry<Object, EntityEntry> me = var7[var9];
        EntityEntry entry = (EntityEntry)me.getValue();
        Status status = entry.getStatus();
        if (status != Status.LOADING && status != Status.GONE) {
        // ------------------- (3)
            FlushEntityEvent entityEvent 1= new FlushEntityEvent(source, me.getKey(), entry); 
            Iterator var14 = flushListeners.iterator();

            while(var14.hasNext()) {
                FlushEntityEventListener listener = (FlushEntityEventListener)var14.next();
                listener.onFlushEntity(entityEvent);
            }
        }
    }

    source.getActionQueue().sortActions();
    return count;
}

```

1. 여기에서는 `persistenceContext.reentrantSafeEntityEntries()`를 이용해 `entityEntries`를 저장한다.
2. `entityEntries`의 엔트리를 루프돌며
3. `FlushEntityEvent`를 만들어 `onFlushEntity`를 호출한다.

#### DefaultFlushEntityEventListener.onFlushEntity()

→ `scheduleUpdate`

```java
private boolean scheduleUpdate(FlushEntityEvent event) {
    EntityEntry entry = event.getEntityEntry();
    EventSource session = event.getSession();
    Object entity = event.getEntity();
    Status status = entry.getStatus();
    EntityPersister persister = entry.getPersister();
    Object[] values = event.getPropertyValues();
    // 중략

    (new Nullability(session)).checkNullability(values, persister, true);
    session.getActionQueue().addAction(new EntityUpdateAction(entry.getId(), values, dirtyProperties, event.hasDirtyCollection(), status == Status.DELETED && !entry.isModifiableEntity() ? persister.getPropertyValues(entity) : entry.getLoadedState(), entry.getVersion(), nextVersion, entity, entry.getRowId(), persister, session));
		// ------------------- (4)
    return intercepted;
}
```

4. `ActionQueue`에 `EntityUpdateAction`을 추가한다.

#### ActionQueue.addAction()

```java
public void addAction(EntityUpdateAction action) {
    this.addAction(EntityUpdateAction.class, action);
}

private <T extends Executable & Comparable & Serializable> void addAction(Class<T> executableClass, T action) {
		// ------------------- (5)
    ((ActionQueue.ListProvider)EXECUTABLE_LISTS_MAP.get(executableClass)).getOrInit(this).add(action);
}
```

5. 메서드는 `*EXECUTABLE_LISTS_MAP*`의 Value에 현재 action을 추가한다.

요약하자면 현재까지의 flush 내용을 이벤트로 만들고, 하위로 전달하며 해당 이벤트를 기반으로 updateAction을 만들어서 Hibernate가 커밋 시점에 수행할 작업 목록을 관리하는 ActionQueue라는 곳에 이를 저장한다.

## Flush 실행

이번에는 `performExecutions`를 살펴보자.

#### AbstractFlushingEventListener.performExecutions()

```java
protected void performExecutions(EventSource session) {
    LOG.trace("Executing flush");

    try {
        session.getJdbcCoordinator().flushBeginning();
        session.getPersistenceContext().setFlushing(true);
        session.getActionQueue().prepareActions();
        session.getActionQueue().executeActions(); // ------------------- (1)
    } finally {
        session.getPersistenceContext().setFlushing(false);
        session.getJdbcCoordinator().flushEnding();
    }

}
```

1. Queue의 액션 실행

#### ActionQueue.executeActions

```java
public void executeActions() throws HibernateException {
    if (this.hasUnresolvedEntityInsertActions()) {
        throw new IllegalStateException("About to execute actions, but there are unresolved entity insert actions.");
    } else {
        Iterator var1 = EXECUTABLE_LISTS_MAP.values().iterator();

        while(var1.hasNext()) {
            ActionQueue.ListProvider listProvider = (ActionQueue.ListProvider)var1.next();
            ExecutableList<?> l = listProvider.get(this);
            if (l != null && !l.isEmpty()) {
                this.executeActions(l); // ------------------- (2)
            }
        }

    }
}
```

1. `*EXECUTABLE_LISTS_MAP*`를 순회하며 각 액션 타입별 저장된 액션들을 모두 수행한다.

결국 action Queue는 `*EXECUTABLE_LISTS_MAP*`를 순회하며 action을 순서대로 수행할 뿐이다. flush 순서를 알기 위해서는 이 작업을 넣는 순서를 알아야 한다.

### Flush 순서 결정

위에서 이미 다뤘지만 플러시 액션을 생성하는데, 가장 결정적인 코드는 아래 코드였다.

```java
Entry<Object, EntityEntry>[] entityEntries = persistenceContext.reentrantSafeEntityEntries();
```

이걸 좀 더 살펴보도록 하자.

```java
public Entry<Object, EntityEntry>[] reentrantSafeEntityEntries() {
    if (this.dirty) {
        this.reentrantSafeEntries = new EntityEntryContext.EntityEntryCrossRefImpl[this.count];
        int i = 0;
				// ------------------- (1)
        for(ManagedEntity managedEntity = this.head; managedEntity != null; managedEntity = managedEntity.$$_hibernate_getNextManagedEntity()) {
            this.reentrantSafeEntries[i++] = new EntityEntryContext.EntityEntryCrossRefImpl(managedEntity.$$_hibernate_getEntityInstance(), managedEntity.$$_hibernate_getEntityEntry());
        }

        this.dirty = false;
    }

    return this.reentrantSafeEntries;
}
```

managedEntity를 조회해서 reentrantSafeEntries를 만들어주고 있다. managedEntity가 정의되는 곳은 아래의 addEntityEntry 이다.

```java
public void addEntityEntry(Object entity, EntityEntry entityEntry) {
    this.dirty = true;

    assert AbstractEntityEntry.class.isInstance(entityEntry);

    assert !entityEntry.getPersister().isMutable() || ((AbstractEntityEntry)AbstractEntityEntry.class.cast(entityEntry)).getPersistenceContext() == this.persistenceContext;

    ManagedEntity managedEntity = this.getAssociatedManagedEntity(entity);
    boolean alreadyAssociated = managedEntity != null;
    if (!alreadyAssociated) {
        if (ManagedEntity.class.isInstance(entity)) {
            if (entityEntry.getPersister().isMutable()) {
                managedEntity = (ManagedEntity)entity;
                this.checkNotAssociatedWithOtherPersistenceContextIfMutable((ManagedEntity)managedEntity);
            } else {
                managedEntity = new EntityEntryContext.ImmutableManagedEntityHolder((ManagedEntity)entity);
                if (this.immutableManagedEntityXref == null) {
                    this.immutableManagedEntityXref = new IdentityHashMap();
                }

                this.immutableManagedEntityXref.put((ManagedEntity)entity, (EntityEntryContext.ImmutableManagedEntityHolder)managedEntity);
            }
        } else {
            if (this.nonEnhancedEntityXref == null) {
                this.nonEnhancedEntityXref = new IdentityHashMap();
            }

            managedEntity = new EntityEntryContext.ManagedEntityImpl(entity);
            this.nonEnhancedEntityXref.put(entity, managedEntity);
        }
    }

    ((ManagedEntity)managedEntity).$$_hibernate_setEntityEntry(entityEntry);
    if (!alreadyAssociated) {
        if (this.tail == null) {
            assert this.head == null;

            ((ManagedEntity)managedEntity).$$_hibernate_setPreviousManagedEntity((ManagedEntity)null);
            ((ManagedEntity)managedEntity).$$_hibernate_setNextManagedEntity((ManagedEntity)null);
            this.head = (ManagedEntity)managedEntity;
            this.tail = this.head;
            this.count = 1;
        } else {
            this.tail.$$_hibernate_setNextManagedEntity((ManagedEntity)managedEntity);
            ((ManagedEntity)managedEntity).$$_hibernate_setPreviousManagedEntity(this.tail);
            ((ManagedEntity)managedEntity).$$_hibernate_setNextManagedEntity((ManagedEntity)null);
            this.tail = (ManagedEntity)managedEntity;
            ++this.count;
        }

    }
}
```

- managedEntity가 추가될 때마다 선 후 관계 정의를 해주고 있다.

addEntityEntry는 `org.hibernate.loader.Loader`에서 doQuery를 통해 Entity가 로드될 때 해당 Entity를 파싱하며 사용된다. 즉 지금 managed되는 순서는 쿼리로 불려온 뒤 1차 캐시에 attached 되는 entity 순서라는 것을 알 수 있다. flush 시점에 dirty checking 및 update 쿼리가 생성되는 순서는 이를 따라간다.

### 결론

결론이 조금 허무하지만(?) 두 가지 해결 방법이 있다.

1. 갱신 순서를 맞추기 위해 로딩 순서를 맞추거나
2. 원하는 시점에 강제 flush를 날리거나

사실은

_어? 쿼리가 왜 안날라가지? → saveAndFlush() 한번 호출하지 뭐

한 번이면 끝날 문제였지만, 어떤 원리로 더티체킹이 수행되는건지 궁금해서 조금 깊게 파봤다. 이로써 Deadlock 로그는 말끔히 사라지게 되었다.

## 번외

### JPQL은 flush를 항상 발생시키는게 아니다?

처음 JPA를 접할 때, 외우듯이 알게 되는 것이 바로 JPQL을 사용하는 경우 자동으로 1차 캐시 flush가 이루어진다는 것이다. 데드락 이슈를 해결하기 위해 다양한 디버깅을 해보던 중 JPQL을 직접 날려도 이전 entity의 dirty checking 내역 flush가 수행되지 않는 현상을 발견했다.

그리고 아래와 같은 이유를 찾았다.

	JPQL로 조회나 갱신하는 대상 중 현재 쓰기 지연 저장소에 저장된 entity와 일치하는 타입이 있을 때만 업데이트가 발생한다.

#### 예시

1. EntityA, EntityB, EntityC 에 대한 갱신 내역이 쓰기 지연 저장소에 존재
2. JPQL로 EntityA이 조회되는 경우
3. Flush 수행

#### 예시2

1. EntityA, EntityB, EntityC 에 대한 갱신 내역이 쓰기 지연 저장소에 존재
2. JPQL로 EntityD이 조회되는 경우
3. Flush 수행하지 않음
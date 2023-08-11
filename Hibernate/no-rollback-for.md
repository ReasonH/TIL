서버 배포 이후, 특정 API가 동작하지 않는다는 문의를 받았다. 확인 결과 API 호출 시 ClassCastException로 인한 롤백 (UnexpectedRollbackException) 예외가 발생하고 있었고, 이유도 금방 찾을 수 있었다.

### 원인 파악

우리는 Entity에 대한 Second level cache(2lv cache)를 사용 하고 있다. 팀원 분께서 작업 중 Entity class에서 미사용 필드를 제거한 뒤 관련된 2lv cache의 버전을 올리지 않았는데, 이 상태로 배포가 되며 캐시와 Entity 객체의 맵핑이 깨져 오류가 발생한 것이었다.
- **JPA query가 수행 시, 2lv cache를 조회하며 ClassCastException발생**

### 또 다른 문제?

해당 Entity의 2lv cache 버전을 올리면서 (키 값 수정) 오류는 금방 해결됐지만, 또 다른 문제가 있었다.

문제가 되는 API는 내부적으로 여러 데이터를 조회해서 리스트 형태의 결과를 만들어주는데, 일부 항목에서 예외가 발생하더라도 정상적인 데이터는 반환이 되도록 설계된 상태였다. 그러나 위에서 발생한 이슈에서는 한 건이라도 예외가 발생하면 UnexpectedRollbackException을 반환하며 API 호출 자체가 실패하고 있었다. 

나는 이를 개선해서 일부 항목 오류가 있어도 정상 조회된 항목들은 클라이언트에 전달을 해야 한다고 생각했다.

### noRollbackFor를 사용한 이슈 처리

API 서비스 메서드는 대략 다음과 같은 호출 구조를 띄고 있다. 하위 트랜잭션에서 발생한 예외를 외부에서 try - catch하는 구조였는데, 이게 문제라는 건 금방 눈치챌 수 있었다. 

```java
@Service
public class Service {

	@Autowired
	private InnerService innerService;

	@Transactional(readOnly = true)
	public ResultListDto getResult(List<Long> ids) {
		// ... some
		for (Long id : ids) {
		try {
				resultList.pushResult(innerService.getResult(id));
		} catch (Exception e) {
				
		}				
		// ... some
		return resultList;
	}
}
```

```java
@Service
public class InnerService {
	@Transactional(readOnly = true)
	public Result getResult(Long id) {
	  // ...
	}
}
```

내부에서 발생한 예외가 global rollback을 marking하기 때문이다. (이 부분은 아마 누군가의 실수로 작업된 레거시 코드가 오랫동안 남아있던 게 아닐까 추측한다.)
- 관련 글: [https://techblog.woowahan.com/2606/](https://techblog.woowahan.com/2606/)

문제를 고치는 것은 금방 할 수 있다. 하위 메서드 트랜잭션 선언부에 `noRollbackFor = ClassCastException.class`을 추가했다.

그러나, 잘 동작할 줄 알았던 API는 여전히 동일한 예외를 반환하고 있었다.
```bash
org.springframework.transaction.UnexpectedRollbackException: Transaction silently rolled back because it has been marked as rollback-only
```

noRollbackFor 옵션이 동작을 안 하는 건가 싶어 `noRollbackFor = RuntimeException.class` 로 모든 런타임 예외를 처리하도록 수정했음에도 동일한 이슈가 발생했다. 특이한 점은, 문제가 되는 쿼리 수행 전 직접 `throw RuntimeException()` 를 통해 예외를 발생시키는 경우에는 noRollbackFor 옵션이 정상 동작했다 것이다. 이 차이는 어디서 오는것일까? 

내부 구조를 살펴보자.

### Case1) 쿼리에서 발생한 RuntimeException

JPA method name query를 통해 엔티티를 조회하면 다음과 같은 과정을 통해 Exception을 처리한다. 중요 코드만 짚어봤다.

#### `SessionImpl`

```java
public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockModeType, Map<String, Object> properties) {
	this.checkOpen();
	LockOptions lockOptions = null;
	
	String entityName;
	try {
		this.getLoadQueryInfluencers().getEffectiveEntityGraph().applyConfiguredGraph(properties);
		Boolean readOnly = properties == null ? null : (Boolean)properties.get("org.hibernate.readOnly");
		this.getLoadQueryInfluencers().setReadOnly(readOnly);
		IdentifierLoadAccess<T> loadAccess = this.byId(entityClass);
		loadAccess.with(this.determineAppropriateLocalCacheMode(properties));
		if (lockModeType != null) {
			if (!LockModeType.NONE.equals(lockModeType)) {
				this.checkTransactionNeededForUpdateOperation();
			}
	
			lockOptions = this.buildLockOptions(lockModeType, properties);
			loadAccess.with(lockOptions);
		}
	
		if (this.getLoadQueryInfluencers().getEffectiveEntityGraph().getSemantic() == GraphSemantic.FETCH) {
			this.setEnforcingFetchGraph(true);
		}
	
		Object var25 = loadAccess.load((Serializable)primaryKey);
		return var25;
	} catch (EntityNotFoundException var17) {
			// 중략
	} catch (TypeMismatchException | ClassCastException | MappingException var20) {
		throw this.getExceptionConverter().convert(new IllegalArgumentException(var20.getMessage(), var20));
	// 이하 생략
```

find method에서 Entity를 로드할 때 `ClassCastException` 등의 특정 예외가 발생하는 경우 `this.getExceptionConverter().convert()`를 호출한다.

#### `ExceptionConverterImpl`

```java
public RuntimeException convert(RuntimeException e) {
    RuntimeException result = e;
    if (e instanceof HibernateException) {
        result = this.convert((HibernateException)e);
    } else {
        this.sharedSessionContract.markForRollbackOnly();
    }

    return result;
}
```

`convert()` 메서드에서는 `HibernateException`이 아닌 경우 `markForRollbackOnly()`라는 메서드를 호출한다. 코드를 더 따라가보면

#### `JdbcResourceLocalTransactionCoordinatorImpl`

```java
public void markRollbackOnly() {
    if (this.getStatus() != TransactionStatus.ROLLED_BACK) {
        if (JdbcResourceLocalTransactionCoordinatorImpl.log.isDebugEnabled()) {
            JdbcResourceLocalTransactionCoordinatorImpl.log.debug("JDBC transaction marked for rollback-only (exception provided for stack trace)", new Exception("exception just for purpose of providing stack trace"));
        }

        this.rollbackOnly = true;
    }
}
```

현재 트랜잭션이 롤백 상태가 아닌 경우 rollbackOnly = true를 마킹한다.

#### `AbstractPlatformTransactionManager`

```java
private void processCommit(DefaultTransactionStatus status) throws TransactionException {
    try {
        boolean beforeCompletionInvoked = false;

        try {
            boolean unexpectedRollback = false;
            this.prepareForCommit(status);
            this.triggerBeforeCommit(status);
            this.triggerBeforeCompletion(status);
            beforeCompletionInvoked = true;
            if (status.hasSavepoint()) {
                if (status.isDebug()) {
                    this.logger.debug("Releasing transaction savepoint");
                }

                unexpectedRollback = status.isGlobalRollbackOnly();
                status.releaseHeldSavepoint();
            } else if (status.isNewTransaction()) {
                if (status.isDebug()) {
                    this.logger.debug("Initiating transaction commit");
                }

                unexpectedRollback = status.isGlobalRollbackOnly();
                this.doCommit(status);
            } else if (this.isFailEarlyOnGlobalRollbackOnly()) {
                unexpectedRollback = status.isGlobalRollbackOnly();
            }

            if (unexpectedRollback) {
                throw new UnexpectedRollbackException("Transaction silently rolled back because it has been marked as rollback-only");
            }
        } catch (UnexpectedRollbackException var17) {
            this.triggerAfterCompletion(status, 1);
            throw var17;
        } catch (TransactionException var18) {
            if (this.isRollbackOnCommitFailure()) {
                this.doRollbackOnCommitException(status, var18);
            } else {
                this.triggerAfterCompletion(status, 2);
            }

            throw var18;
        } catch (Error | RuntimeException var19) {
            if (!beforeCompletionInvoked) {
                this.triggerBeforeCompletion(status);
            }

            this.doRollbackOnCommitException(status, var19);
            throw var19;
        }

        try {
            this.triggerAfterCommit(status);
        } finally {
            this.triggerAfterCompletion(status, 0);
        }
    } finally {
        this.cleanupAfterCompletion(status);
    }

}
```

최종 커밋을 처리하는 `processCommit` 메서드는 `unexpectedRollback = status.isGlobalRollbackOnly();`부분에서 unexpectedRollback을 마킹하며 `UnexpectedRollbackException`예외를 던진다.

##### 결론
아무래도 Hibernate의 DB 조작 과정에서 오류가 발생하는 경우 일괄적으로 rollback 마킹을 하고, 트랜잭션 커밋 시점에 예외를 발생시키는 것으로 보인다. 쿼리가 아닌 다른 곳에서 발생한 예외(사용자가 직접 발생시키는 예외)는 어떻게 처리되는지 살펴보자.

### Case2) 유저가 발생시킨 RuntimeException

#### `TransactionAspectSupport`

```java
protected void completeTransactionAfterThrowing(@Nullable TransactionAspectSupport.TransactionInfo txInfo, Throwable ex) {
    if (txInfo != null && txInfo.getTransactionStatus() != null) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace("Completing transaction for [" + txInfo.getJoinpointIdentification() + "] after exception: " + ex);
        }

        if (txInfo.transactionAttribute != null && txInfo.transactionAttribute.rollbackOn(ex)) {
            try {
                txInfo.getTransactionManager().rollback(txInfo.getTransactionStatus());
            } catch (TransactionSystemException var6) {
                this.logger.error("Application exception overridden by rollback exception", ex);
                var6.initApplicationException(ex);
                throw var6;
            } catch (Error | RuntimeException var7) {
                this.logger.error("Application exception overridden by rollback exception", ex);
                throw var7;
            }
        } else {
            try {
                txInfo.getTransactionManager().commit(txInfo.getTransactionStatus());
            } catch (TransactionSystemException var4) {
                this.logger.error("Application exception overridden by commit exception", ex);
                var4.initApplicationException(ex);
                throw var4;
            } catch (Error | RuntimeException var5) {
                this.logger.error("Application exception overridden by commit exception", ex);
                throw var5;
            }
        }
    }

}
```

메서드 내 예외 발생 시, 위 함수가 호출된다. 눈여겨 보아야 할 부분은 `txInfo.transactionAttribute.rollbackOn()` 메서드이다.

#### `RuleBasedTransactionAttribute`

```java
public boolean rollbackOn(Throwable ex) {
	    RollbackRuleAttribute winner = null;
	    int deepest = 2147483647;
	    if (this.rollbackRules != null) {
	        Iterator var4 = this.rollbackRules.iterator();
	
	        while(var4.hasNext()) {
	            RollbackRuleAttribute rule = (RollbackRuleAttribute)var4.next();
	            int depth = rule.getDepth(ex);
	            if (depth >= 0 && depth < deepest) {
	                deepest = depth;
	                winner = rule;
	            }
	        }
	    }
	
	    if (winner == null) {
	        return super.rollbackOn(ex);
	    } else {
	        return !(winner instanceof NoRollbackRuleAttribute);
	    }
}
```

`RuleBasedTransactionAttribute`는 noRollbackFor 옵션 등을 통해 지정된 rollback rule을 `rollbackRules`라는 리스트로 관리한다. 예외가 발생하는 경우 현재 예외와 기존 정의된 rule을 비교해 rollback 여부를 판단한다. `completeTransactionAfterThrowing`는 rollbackOn의 결과값으로 true가 반환될 때 rollback을 false가 반환될 때 커밋을 진행한다.

만약 true가 반환되어 rollback이 시작된다면

Case1에서 나온 `JdbcResourceLocalTransactionCoordinatorImpl`의 `markRollbackOnly`가 호출되며 rollback 마킹이 수행된다. 그 이후의 과정은 동일하다.

### 결론

`@Transactional`처리된 메서드에서는 DB쿼리가 아닌 외부 API 호출, 캐시 접근, 파일 I/O 등 다양한 케이스에서 RuntimeException이 발생할 수 있다. 이런 경우 noRollbackFor 옵션을 이용한다면 롤백 예외를 피해서 트랜잭션 완료를 보장할 수 있다. 그러나 쿼리에서 발생하는 일부 오류(ex. `MappingException`, `ClassCastException`, `JDBCException` 등)들은 사용자의 롤백 무시 설정과 관계없이 즉시 전역 롤백을 마킹하고 예외를 전달한다.

이에 대해 혹시 설정이 가능한지 [문의](https://discourse.hibernate.org/t/why-is-the-hibernate-orm-marking-a-rollback-even-in-the-read-only-state/8055)를 해보았으나 다음과 같은 답변이 달렸다.
> What exceptions are we talking about here? Hibernate has to obey the rules of the JPA specification regarding rollback when exceptions occur and it’s also not easy to maintain a consistent internal state when exceptions happen somewhere during result processing, which is why a session usually should not be used anymore after an exception happens.

일반적으로 Hibernate 세션에서 오류가 발생한 경우 해당 세션을 재사용하지 않아야 한다고 한다. 즉 실패처리는 의도된 것이기 때문에 rollback을 피해가려 하는 것은 옳지 않다는 뜻으로 보인다. 읽어보니 맞는 말이다. 세션 오류는 발생하지 않도록 철저히 대비하는게 답이다.

PS. `ObjectDeletedException`, `ObjectNotFoundException` 등의 예외에서는 rollback 마킹을 하지 않는다. 이 기준은 뭘까?
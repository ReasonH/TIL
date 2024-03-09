애플리케이션에서spring-data-redis 의존성을 사용해 Redis를 통합한 경우 @Cacheable, @CachePut, @CacheEvict와 같은 annotation 등을 사용해 캐시를 관리할 수 있다. 이 annotation들을 사용하기 위해서는 key와 별개로 cache name이 필요한데, 이는 각각의 key들을 그룹화하는 개념으로 사용된다.

실제로 Redis client가 annotation을 통해 Redis String Cache를 저장할 때에는 `<CacheName>::<key>` 형태로 key가 만들어진다.

## 캐시 제거의 필요성

라이브 환경에서는 최대한 피해야겠지만, 어쩔 수 없이 특정 캐시를 제거해야하는 경우가 생기기도 한다. 팀에서 과거에 사용하던 Hazelcast의 경우, map.destroy 등 하나의 그룹화된 캐시 객체를 제거할 수 있는 명령어들이 존재했다. 그러나 Redis는 Cache Name등을 기준으로 제거하는 명령어가 따로 존재하지 않았기에 다른 방법이 필요했다.

### shell script 사용 검토

아래 stack over flow를 보면 redis-cli를 사용하는 다양한 제거 방법이 명시되어 있다.
https://stackoverflow.com/questions/4006324/how-to-atomically-delete-keys-matching-a-pattern-using-redis

이 중 성능 이슈가 되지 않는 것을 적당히 사용해도 되지만 다음의 두 가지 문제점이 있었다.
- 별도의 shell script 관리가 필요
- 사용 방식이 직관적이지 않음
- 잘못된 key를 입력하는 경우에 대한 처리 필요

### RedisCacheManager 사용 검토

반면에 Redis Cache Manager를 사용한 방법은 비교적 간단하다.
Spring의 캐시 추상화를 사용하기 위한 CacheManager를 정의하고, 미리 선언된 Cache Name들을 관리할 수 있도록 `cacheKeyMap`으로 등록해 놓았다.

```java
@Bean
public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
    return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(redisCacheConfig())
            .withInitialCacheConfigurations(cacheKeyMap())
            .build();
}

private Map<String, RedisCacheConfiguration> cacheKeyMap() {
    Map<String, RedisCacheConfiguration> cacheMap = new HashMap<>();
    // String
    cacheMap.put(CACHE_NAME_01, redisCacheConfig().entryTtl(Duration.ofDays(15)));

		return cacheMap;
}
```

이제 키 제거가 필요하다면 다음과 같이 사용할 수 있다.
```java

public void clearCache() {
	cacheManager.getCache(CACHE_NAME_01).clear();
}

```
cacheManager를 주입받아 `getCache()` 메서드를 통해 정의해놓은 cache name을 조작할 수 있다. 우리는 단순히 `clear()`만 호출하여 모든 캐시를 탐색하고 지울 수 있다. shell script의 단점들이 보완된 것처럼 보인다. 그러나 이를 사용하면 치명적인 문제점이 있다.

### CacheManager 사용 시, 문제점

위의 `clear()` 메서드는 `RedisCache` 클래스에 속해있다. 내부적으로는 다음과 같은 코드로 동작한다.

#### RedisCache.class
```java
public void clear() {
	byte[] pattern = (byte[])this.conversionService.convert(this.createCacheKey("*"), byte[].class);
	this.cacheWriter.clean(this.name, pattern);
}
```
createCacheKey로 와일드 카드 패턴을 만들고 Cache를 지우는 것으로 보인다. cacheWriter 내부를 봐보자.

#### DefaultRedisCacheWriter.class
```java
public void clean(String name, byte[] pattern) {
    Assert.notNull(name, "Name must not be null!");
    Assert.notNull(pattern, "Pattern must not be null!");
    this.execute(name, (connection) -> {
        boolean wasLocked = false;

        try {
            if (this.isLockingCacheWriter()) {
                this.doLock(name, connection);
                wasLocked = true;
            }

            long deleteCount;
            for(deleteCount = this.batchStrategy.cleanCache(connection, name, pattern); deleteCount > 2147483647L; deleteCount -= 2147483647L) {
                this.statistics.incDeletesBy(name, 2147483647);
            }

            this.statistics.incDeletesBy(name, (int)deleteCount);
            return "OK";
        } finally {
            if (wasLocked && this.isLockingCacheWriter()) {
                this.doUnlock(name, connection);
            }

        }
    });
}
```
여기에서는 `batchStrategy.cleanCache()` 메서드를 실행시켜 캐시 제거를 수행하는 것을 알 수 있다. 이 메서드의 내부를 확인해보면

#### Keys
```java
static class Keys implements BatchStrategy {
    static BatchStrategies.Keys INSTANCE = new BatchStrategies.Keys();

    Keys() {
    }

    public long cleanCache(RedisConnection connection, String name, byte[] pattern) {
        byte[][] keys = (byte[][])((Set)Optional.ofNullable(connection.keys(pattern)).orElse(Collections.emptySet())).toArray(new byte[0][]);
        if (keys.length > 0) {
            connection.del(keys);
        }

        return (long)keys.length;
    }
}
```
pattern에 일치하는 키들을 제거하기 위해 `connection.keys`를 호출하고 있다. 해당 커맨드는 운영환경에서 사용하기에는 굉장히 위험하다. 이 상태로 캐시 제거를 시도한다면 Redis가 block되고 시스템 장애로 이어질 수 있다. 공식 문서에서는 해당 커맨드를 아래와 같이 설명한다. 간단히 말하자면 KEYS 보다는 SCAN을 권장하고 있다.

**Warning**: consider `KEYS` as a command that should only be used in production environments with extreme care. It may ruin performance when it is executed against large databases. This command is intended for debugging and special operations, such as changing your keyspace layout. Don't use `KEYS` in your regular application code. If you're looking for a way to find keys in a subset of your keyspace, consider using [`SCAN`](https://redis.io/commands/scan) or [sets](https://redis.io/topics/data-types#sets).

### RedisCache의 clear에서 KEYS 대신 SCAN 사용하기

그렇다면 여기서 해야할 일은 RedisCache clear() method가 SCAN을 사용하도록 설정하는 것이다.
위의 `Keys` 클래스가 구현하고 있는 인터페이스 `BatchStrategy`를 확인하면 다음과 같다.
```java

/**
 * A {@link BatchStrategy} to be used with {@link RedisCacheWriter}.
 * <p>
 * Mainly used to clear the cache.
 * <p>
 * Predefined strategies using the {@link BatchStrategies#keys() KEYS} or {@link BatchStrategies#scan(int) SCAN}
 * commands can be found in {@link BatchStrategies}.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @since 2.6
 */
public interface BatchStrategy {

	/**
	 * Remove all keys following the given pattern.
	 *
	 * @param connection the connection to use. Must not be {@literal null}.
	 * @param name The cache name. Must not be {@literal null}.
	 * @param pattern The pattern for the keys to remove. Must not be {@literal null}.
	 * @return number of removed keys.
	 */
	long cleanCache(RedisConnection connection, String name, byte[] pattern);

}
```

 *Predefined strategies using the {@link BatchStrategies#keys() KEYS} or {@link BatchStrategies#scan(int) SCAN}* 를 읽어보면 우리가 이미 사용했던 구현체인 Keys외에 Scan이라는 것도 준비되어 있는 것으로 보인다.
 
 이 구현체만 바꿔주면 손쉽게 SCAN을 통한 제거를 할 수 있을 것으로 보인다.
일단 `DefaultRedisCacheWriter`는 생성자에서 batchStrategy를 결정하고 있다. 이외에 배치전략이 설정되는 메서드는 보이지 않는다. 그리고 `DefaultRedisCacheWriter`는 `RedisCacheWriter.class`에서 생성되고 있다.

#### RedisCacheWriter
```java
public interface RedisCacheWriter extends CacheStatisticsProvider {
    static RedisCacheWriter nonLockingRedisCacheWriter(RedisConnectionFactory connectionFactory) {
        return nonLockingRedisCacheWriter(connectionFactory, BatchStrategies.keys());
    }

    static RedisCacheWriter nonLockingRedisCacheWriter(RedisConnectionFactory connectionFactory, BatchStrategy batchStrategy) {
        Assert.notNull(connectionFactory, "ConnectionFactory must not be null!");
        Assert.notNull(batchStrategy, "BatchStrategy must not be null!");
        return new DefaultRedisCacheWriter(connectionFactory, batchStrategy);
    }
...
```
여기서 이용하고 있는 생성자는 첫 번째 `nonLockingRedisCacheWriter(RedisConnectionFactory connectionFactory)`이다. 이는 내부적으로 static 생성자를 호출해 DefaultRedisCacheWriter를 생성해주고 있다. 여기에서는 기본적으로 BatchStrategies.keys()를 사용해 CacheWriter를 생성하기 때문에 batchStrategy를 설정할 수가 없다. batchStrategy를 설정하기 위해서는 `nonLockingRedisCacheWriter(RedisConnectionFactory connectionFactory, BatchStrategy batchStrategy)`를 사용해야 한다.
#### RedisCacheManager

이는 `RedisCacheWriter`를 호출하는 상위 메서드이다.

`builder`는 `fromConnectionFactory`를 호출한다. `fromConnectionFactory`는 다시 `nonLockingRedisCacheWriter`를 사용해 CacheWriter를 만들어주고 있다. BatchStrategy 를 변경하기 위해서는  `fromCacheWriter`를 사용하면 될 것 같다.

```java
public static RedisCacheManager.RedisCacheManagerBuilder builder(RedisConnectionFactory connectionFactory) {
    Assert.notNull(connectionFactory, "ConnectionFactory must not be null!");
    return RedisCacheManager.RedisCacheManagerBuilder.fromConnectionFactory(connectionFactory);
}

...

public static RedisCacheManager.RedisCacheManagerBuilder fromConnectionFactory(RedisConnectionFactory connectionFactory) {
    Assert.notNull(connectionFactory, "ConnectionFactory must not be null!");
    return new RedisCacheManager.RedisCacheManagerBuilder(RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory));
}

...

public static RedisCacheManager.RedisCacheManagerBuilder fromCacheWriter(RedisCacheWriter cacheWriter) {
    Assert.notNull(cacheWriter, "CacheWriter must not be null!");
    return new RedisCacheManager.RedisCacheManagerBuilder(cacheWriter);
}
```

그리고 이를 위해서는 우리가 CacheManager를 정의하는 Bean을 수정하면 된다.

#### CacheManager Bean
```java
@Bean
public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
    return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(redisCacheConfig())
            .withInitialCacheConfigurations(cacheKeyMap())
            .build();
}
```

현재는 builder를 호출하고 있어 Batch 전략 설정이 불가능하다. 이를 다음과 같이 수정한다.

```java
@Bean
public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
    return RedisCacheManager.RedisCacheManagerBuilder.
    fromCacheWriter(RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory, BatchStrategies.scan(1000)))
            .cacheDefaults(redisCacheConfig())
            .withInitialCacheConfigurations(cacheKeyMap())
            .build();
}
```

builder 대신 RedisCacheManagerBuilder.fromCacheWriter를 사용하여 SCAN 전략을 지정한다. scan단위는 적절하게 지정하며 (여기에서는 1000), 나머지 설정은 유지한다. 이제 호출하고 디버깅을 해보면 SCAN을 사용하여 key를 조회하고 제거하는 것을 알 수 있다.
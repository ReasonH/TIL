# 왜 Redisson은 주기적으로 2차캐시 EVAL을 수행할까?

Hibernate second level cache (이하 2lv라고 함)를 Redis로 전환하며 RedissonRegionFactory를 사용했다. 2lv 캐시가 정상적으로 적용된 것은 확인했지만, 주기적으로 수많은 lua script 로그가 발생하고 있어 이에 대한 이유를 알아보았다.

### 상황

수많은 EVAL 로그가 몇 초 단위로 수십개씩 생기고 있다. 로그를 자세히보면 각 Entity마다 별개의 zrangebyscore 커맨드를 lua script로 전송하고 있다.

```text
2023-07-05T15:27:12.319+09:00 DEBUG [lime-api-server-purple,,,] 53976 --- [netty-8-15] [      org.redisson.command.RedisExecutor: 615] : acquired connection for command (EVAL) and params [if redis.call('setnx', KEYS[6], ARGV[4]) == 0 then return -1;end;redis.call('expire', KEYS[6], ARGV[3]); local expiredKeys1 = redis.call('zrangebyscore', KEYS[2], 0, ARGV[1], 'limit', 0, ARGV[2]); for i, key in ipairs(expiredKeys1) do local v = redis.call('hget', KEYS[1], key); if v ~= false then local t, val = struct.unpack('dLc0', v); local msg = struct.pack('Lc0Lc0', string.len(key), key, string.len(val), val); local listeners = redis.call('publish', KEYS[4], msg); if (listeners == 0) then break;end; end;end;for i=1, #expiredKeys1, 5000 do redis.call('zrem', KEYS[5], unpack(expiredKeys1, i, math.min(i+4999, table.getn(expiredKeys1)))); redis.call('zrem', KEYS[3], unpack(expiredKeys1, i, math.min(i+4999, table.getn(expiredKeys1)))); redis.call('zrem', KEYS[2], unpack(expiredKeys1, i, math.min(i+4999, table.getn(expiredKeys1)))); redis.call('hdel', KEYS[1], unpack(expiredKeys1, i, math.min(i+4999, table.getn(expiredKeys1)))); end; local expiredKeys2 = redis.call('zrangebyscore', KEYS[..., 6, AccountV1, redisson__timeout__set:{AccountV1}, redisson__idle__set:{AccountV1}, redisson_map_cache_expired:{AccountV1}, redisson__map_cache__last_access__set:{AccountV1}, redisson__execute_task_once_latch:{AccountV1}, 1688538432319, 100, ...] from slot NodeSource [slot=846, addr=null, redisClient=null, redirect=null, entry=null] using node 172.19.65.84/172.19.65.84:6300... RedisConnection@307628211 [redisClient=[addr=redis://172.19.65.84:6300], channel=[id: 0xcafb1b5a, L:/172.18.96.144:55491 - R:172.19.65.84/172.19.65.84:6300], currentCommand=null, usage=1]
2023-07-05T15:27:12.319+09:00 DEBUG [lime-api-server-purple,,,] 53976 --- [netty-8-13] [      org.redisson.command.RedisExecutor: 615] : acquired connection for command (EVAL) and params [if redis.call('setnx', KEYS[6], ARGV[4]) == 0 then return -1;end;redis.call('expire', KEYS[6], ARGV[3]); local expiredKeys1 = redis.call('zrangebyscore', KEYS[2], 0, ARGV[1], 'limit', 0, ARGV[2]); for i, key in ipairs(expiredKeys1) do local v = redis.call('hget', KEYS[1], key); if v ~= false then local t, val = struct.unpack('dLc0', v); local msg = struct.pack('Lc0Lc0', string.len(key), key, string.len(val), val); local listeners = redis.call('publish', KEYS[4], msg); if (listeners == 0) then break;end; end;end;for i=1, #expiredKeys1, 5000 do redis.call('zrem', KEYS[5], unpack(expiredKeys1, i, math.min(i+4999, table.getn(expiredKeys1)))); redis.call('zrem', KEYS[3], unpack(expiredKeys1, i, math.min(i+4999, table.getn(expiredKeys1)))); redis.call('zrem', KEYS[2], unpack(expiredKeys1, i, math.min(i+4999, table.getn(expiredKeys1)))); redis.call('hdel', KEYS[1], unpack(expiredKeys1, i, math.min(i+4999, table.getn(expiredKeys1)))); end; local expiredKeys2 = redis.call('zrangebyscore', KEYS[..., 6, ChatGroupV1, redisson__timeout__set:{ChatGroupV1}, redisson__idle__set:{ChatGroupV1}, redisson_map_cache_expired:{ChatGroupV1}, redisson__map_cache__last_access__set:{ChatGroupV1}, redisson__execute_task_once_latch:{ChatGroupV1}, 1688538432319, 100, ...] from slot NodeSource [slot=790, addr=null, redisClient=null, redirect=null, entry=null] using node 172.19.65.84/172.19.65.84:6300... RedisConnection@2045226730 [redisClient=[addr=redis://172.19.65.84:6300], channel=[id: 0xb01cf7bd, L:/172.18.96.144:55487 - R:172.19.65.84/172.19.65.84:6300], currentCommand=null, usage=1]
2023-07-05T15:27:12.319+09:00 DEBUG [lime-api-server-purple,,,] 53976 --- [netty-8-32] [      org.redisson.command.RedisExecutor: 615] : acquired connection for command (EVAL) and params [if redis.call('setnx', KEYS[6], ARGV[4]) == 0 then return -1;end;redis.call('expire', KEYS[6], ARGV[3]); local expiredKeys1 = redis.call('zrangebyscore', KEYS[2], 0, ARGV[1], 'limit', 0, ARGV[2]); for i, key in ipairs(expiredKeys1) do local v = redis.call('hget', KEYS[1], key); if v ~= false then local t, val = struct.unpack('dLc0', v); local msg = struct.pack('Lc0Lc0', string.len(key), key, string.len(val), val); local listeners = redis.call('publish', KEYS[4], msg); if (listeners == 0) then break;end; end;end;for i=1, #expiredKeys1, 5000 do redis.call('zrem', KEYS[5], unpack(expiredKeys1, i, math.min(i+4999, table.getn(expiredKeys1)))); redis.call('zrem', KEYS[3], unpack(expiredKeys1, i, math.min(i+4999, table.getn(expiredKeys1)))); redis.call('zrem', KEYS[2], unpack(expiredKeys1, i, math.min(i+4999, table.getn(expiredKeys1)))); redis.call('hdel', KEYS[1], unpack(expiredKeys1, i, math.min(i+4999, table.getn(expiredKeys1)))); end; local expiredKeys2 = redis.call('zrangebyscore', KEYS[..., 6, GameUserV1, redisson__timeout__set:{GameUserV1}, redisson__idle__set:{GameUserV1}, redisson_map_cache_expired:{GameUserV1}, redisson__map_cache__last_access__set:{GameUserV1}, redisson__execute_task_once_latch:{GameUserV1}, 1688538432319, 100, ...] from slot NodeSource [slot=15826, addr=null, redisClient=null, redirect=null, entry=null] using node 172.19.65.84/172.19.65.84:6302... RedisConnection@1272665815 [redisClient=[addr=redis://172.19.65.84:6302], channel=[id: 0x60d919ee, L:/172.18.96.144:55530 - R:172.19.65.84/172.19.65.84:6302], currentCommand=null, usage=1]
2023-07-05T15:27:12.320+09:00 DEBUG [lime-api-server-purple,,,] 53976 --- [netty-8-17] [      org.redisson.command.RedisExecutor: 615] : acquired connection for command (EVAL) and params [if redis.call('setnx', KEYS[6], ARGV[4]) == 0 then return -1;end;redis.call('expire', KEYS[6], ARGV[3]); local expiredKeys1 = redis.call('zrangebyscore', KEYS[2], 0, ARGV[1], 'limit', 0, ARGV[2]); for i, key in ipairs(expiredKeys1) do local v = redis.call('hget', KEYS[1], key); if v ~= false then local t, val = struct.unpack('dLc0', v); local msg = struct.pack('Lc0Lc0', string.len(key), key, string.len(val), val); local listeners = redis.call('publish', KEYS[4], msg); if (listeners == 0) then break;end; end;end;for i=1, #expiredKeys1, 5000 do redis.call('zrem', KEYS[5], unpack(expiredKeys1, i, math.min(i+4999, table.getn(expiredKeys1)))); redis.call('zrem', KEYS[3], unpack(expiredKeys1, i, math.min(i+4999, table.getn(expiredKeys1)))); redis.call('zrem', KEYS[2], unpack(expiredKeys1, i, math.min(i+4999, table.getn(expiredKeys1)))); redis.call('hdel', KEYS[1], unpack(expiredKeys1, i, math.min(i+4999, table.getn(expiredKeys1)))); end; local expiredKeys2 = redis.call('zrangebyscore', KEYS[..., 6, GameUserNaturalIdV1, redisson__timeout__set:{GameUserNaturalIdV1}, redisson__idle__set:{GameUserNaturalIdV1}, redisson_map_cache_expired:{GameUserNaturalIdV1}, redisson__map_cache__last_access__set:{GameUserNaturalIdV1}, redisson__execute_task_once_latch:{GameUserNaturalIdV1}, 1688538432320, 100, ...] from slot NodeSource [slot=9972, addr=null, redisClient=null, redirect=null, entry=null] using node 172.19.65.84/172.19.65.84:6301... RedisConnection@936088229 [redisClient=[addr=redis://172.19.65.84:6301], channel=[id: 0xea79682c, L:/172.18.96.144:55473 - R:172.19.65.84/172.19.65.84:6301], currentCommand=null, usage=1]
2023-07-05T15:27:12.320+09:00 DEBUG [lime-api-server-purple,,,] 53976 --- [netty 8-16] [      org.redisson.command.RedisExecutor: 615] : acquired connection for command (EVAL) and params [if redis.call('setnx', KEYS[6], ARGV[4]) == 0 then return -1;end;redis.call('expire', KEYS[6], ARGV[3]); local expiredKeys1 = redis.call('zrangebyscore', KEYS[2], 0, ARGV[1], 'limit', 0, ARGV[2]); for i, key in ipairs(expiredKeys1) do local v = redis.call('hget', KEYS[1], key); if v ~= false then local t, val = struct.unpack('dLc0', v); local msg = struct.pack('Lc0Lc0', string.len(key), key, string.len(val), val); local listeners = redis.call('publish', KEYS[4], msg); if (listeners == 0) then break;end; end;end;for i=1, #expiredKeys1, 5000 do redis.call('zrem', KEYS[5], unpack(expiredKeys1, i, math.min(i+4999, table.getn(expiredKeys1)))); redis.call('zrem', KEYS[3], unpack(expiredKeys1, i, math.min(i+4999, table.getn(expiredKeys1)))); redis.call('zrem', KEYS[2], unpack(expiredKeys1, i, math.min(i+4999, table.getn(expiredKeys1)))); redis.call('hdel', KEYS[1], unpack(expiredKeys1, i, math.min(i+4999, table.getn(expiredKeys1)))); end; local expiredKeys2 = redis.call('zrangebyscore', KEYS[..., 6, GameUserDetailV1, redisson__timeout__set:{GameUserDetailV1}, redisson__idle__set:{GameUserDetailV1}, redisson_map_cache_expired:{GameUserDetailV1}, redisson__map_cache__last_access__set:{GameUserDetailV1}, redisson__execute_task_once_latch:{GameUserDetailV1}, 1688538432320, 100, ...] from slot NodeSource [slot=6025, addr=null, redisClient=null, redirect=null, entry=null] using node 172.19.65.84/172.19.65.84:6301... RedisConnection@1027379397 [redisClient=[addr=redis://172.19.65.84:6301], channel=[id: 0x3f311100, L:/172.18.96.144:55476 - R:172.19.65.84/172.19.65.84:6301], currentCommand=null, usage=1]
```

### 스크립트 분석

주기적으로 보내는 스크립트는 어떤 기능을 하고있을까? 디버깅을 통해 실제 스크립트 전문을 확인해보기로 한다.

`org.redisson.eviction.**MapCacheEvictionTask**`

```java
RFuture<Integer> execute() {
    int latchExpireTime = Math.min(delay, 30);
    return executor.evalWriteNoRetryAsync(name, LongCodec.INSTANCE, RedisCommands.EVAL_INTEGER,
            "if redis.call('setnx', KEYS[6], ARGV[4]) == 0 then "
             + "return -1;"
          + "end;"
          + "redis.call('expire', KEYS[6], ARGV[3]); "
           +"local expiredKeys1 = redis.call('zrangebyscore', KEYS[2], 0, ARGV[1], 'limit', 0, ARGV[2]); "
            + "for i, key in ipairs(expiredKeys1) do "
                + "local v = redis.call('hget', KEYS[1], key); "
                + "if v ~= false then "
                    + "local t, val = struct.unpack('dLc0', v); "
                    + "local msg = struct.pack('Lc0Lc0', string.len(key), key, string.len(val), val); "
                    + "local listeners = redis.call('publish', KEYS[4], msg); "
                    + "if (listeners == 0) then "
                        + "break;"
                    + "end; "
                + "end;"  
            + "end;"
            + "for i=1, #expiredKeys1, 5000 do "
                + "redis.call('zrem', KEYS[5], unpack(expiredKeys1, i, math.min(i+4999, table.getn(expiredKeys1)))); "
                + "redis.call('zrem', KEYS[3], unpack(expiredKeys1, i, math.min(i+4999, table.getn(expiredKeys1)))); "
                + "redis.call('zrem', KEYS[2], unpack(expiredKeys1, i, math.min(i+4999, table.getn(expiredKeys1)))); "
                + "redis.call('hdel', KEYS[1], unpack(expiredKeys1, i, math.min(i+4999, table.getn(expiredKeys1)))); "
            + "end; "
          + "local expiredKeys2 = redis.call('zrangebyscore', KEYS[3], 0, ARGV[1], 'limit', 0, ARGV[2]); "
          + "for i, key in ipairs(expiredKeys2) do "
              + "local v = redis.call('hget', KEYS[1], key); "
              + "if v ~= false then "
                  + "local t, val = struct.unpack('dLc0', v); "
                  + "local msg = struct.pack('Lc0Lc0', string.len(key), key, string.len(val), val); "
                  + "local listeners = redis.call('publish', KEYS[4], msg); "
                  + "if (listeners == 0) then "
                      + "break;"
                  + "end; "
              + "end;"  
          + "end;"
          + "for i=1, #expiredKeys2, 5000 do "
              + "redis.call('zrem', KEYS[5], unpack(expiredKeys2, i, math.min(i+4999, table.getn(expiredKeys2)))); "
              + "redis.call('zrem', KEYS[3], unpack(expiredKeys2, i, math.min(i+4999, table.getn(expiredKeys2)))); "
              + "redis.call('zrem', KEYS[2], unpack(expiredKeys2, i, math.min(i+4999, table.getn(expiredKeys2)))); "
              + "redis.call('hdel', KEYS[1], unpack(expiredKeys2, i, math.min(i+4999, table.getn(expiredKeys2)))); "
          + "end; "
          + "return #expiredKeys1 + #expiredKeys2;",
          Arrays.<Object>asList(name, timeoutSetName, maxIdleSetName, expiredChannelName, lastAccessTimeSetName, executeTaskOnceLatchName), 
          System.currentTimeMillis(), keysLimit, latchExpireTime, 1);
}
```

위의 lua script를 해석해보면 대략 다음과 같은 동작을 하고 있다.

1.  **redisson__execute_task_once_latch:XXX**를 캐시한다. (이미 캐시가 있으면 종료)
2.  taskName에 전달받은 delay만큼 expire를 설정한다. (해당 시간 이후 task 제거)
3.  zrangeByScore 조회를 통해 timeOutSet을 조회하고, expiredKeys1를 가져온다.
    -   range: 0 ~ 현재 시각 timestaimp 범위
    -   offet: 0 ~ keysLimit
4.  name + expiredKeys1로 entity의 Hash Cache에서 각 엔트리를 가져온다. (루프)
    -   false(nil)면 바로 다음 loop로
    -   값이 있다면 다음을 진행한다.
        -   엔트리 unpack → 메시지 가공 → pub/sub 채널에 publish
        -   리스너가 없는 경우 루프종료
5.  expiredKeys1를 루프돌며 관련된 키들을 모두 배치로 제거한다. step 당 최대 5000개 key가 제거된다.
6.  이후 expiredKeys2에 대해 동일한 작업을 반복한다.

> expiredKeys1의 경우 캐시 timeoutSet을 기준으로 제거한다면 expiredKeys2는 maxIdleSet을 기준으로 동일한 제거 작업을 수행한다.

### 스케쥴링 알고리즘

이번엔 어떤 주기로 수행이 되는지 살펴보자

`org.redisson.eviction.**EvictionTask**`

```java
public void run() {
    if (!this.executor.getServiceManager().isShuttingDown()) {
        RFuture<Integer> future = this.execute();
        future.whenComplete((size, e) -> {
            if (e != null) {
                this.log.error("Unable to evict elements for '{}'", this.getName(), e);
                this.schedule();
            } else {
                this.log.debug("{} elements evicted. Object name: {}", size, this.getName());
                if (size == -1) {
                    this.schedule();
                } else {
                    if (this.sizeHistory.size() == 2) {
                        if ((Integer)this.sizeHistory.peekFirst() > (Integer)this.sizeHistory.peekLast() && (Integer)this.sizeHistory.peekLast() > size) {
                            this.delay = Math.min(this.maxDelay, (int)((double)this.delay * 1.5D));
                        }

                        if ((Integer)this.sizeHistory.peekFirst() == (Integer)this.sizeHistory.peekLast() && (Integer)this.sizeHistory.peekLast() == size) {
                            if (size >= this.keysLimit) {
                                this.delay = Math.max(this.minDelay, this.delay / 4);
                            }

                            if (size == 0) {
                                this.delay = Math.min(this.maxDelay, (int)((double)this.delay * 1.5D));
                            }
                        }

                        this.sizeHistory.pollFirst();
                    }

                    this.sizeHistory.add(size);
                    this.schedule();
                }
            }
        });
    }
}
```

delay는 초기에 5초로 시작하며 지속적으로 이전에 지워진 키의 갯수를 관리한다. 삭제 작업을 수행할 때 현재 제거된 키의 수와 이전 히스토리를 비교해 delay를 유동적으로 조절한다. (최대/최소값은 설정에 따라 바뀜)

### 동작 정리

-   Redisson은 2lv Entity cache를 Redis Hash로 관리한다.
-   각 hash entry에 대한 timeout을 설정하기 위해 ZSET을 사용한다.
-   스케쥴링된 lua script는 주기적으로 zscore를 통해 현시점에 만료된 키 목록(Entity ID)을 조회한다.
-   키를 사용해 각 Hash entry를 제거한다.

이 과정에서 현재 지워진 키의 갯수와 이전에 지워진 키의 갯수를 비교하며 스케쥴을 조정한다.

### 개선

현재 상태는 다음과 같았다.

-   모든 설정은 기본값 사용, 최대 빈도로 삭제가 수행될 때 각 Entity 2lv cache는 5초마다 100개씩 지워진다.
-   Production 환경에서는 최소 40개의 애플리케이션 인스턴스가 존재하기에에 한 시간당 최대 제거 가능한 캐시 엔트리는 **2,880,000**개가 된다.

약 10개의 Entity에 대해 2lv 캐시가 걸려있으며, 각 Entity의 TTL은 12시간을 설정했다. 현재 Production 환경에서 가장 많이 사용되는 Entity의 경우 2lv에 저장된 entry는 약 300만개다. 이 정도 양은 충분히 처리가 가능한 수치이다. 다만, keysAmount를 과하게 늘리는 경우 Redis block이 걸릴 수 있기 때문에 다음과 같이 조정하기로 했다.

- 최소 delay (**minCleanUpDelay): 1분**
- 최대 delay (**maxCleanUpDelay): 5분**
- 한 번에 삭제 가능한 최대 키 갯수 (**cleanUpKeysAmount): 500개**
⇒ 최대 시간 당 120만개 제거, 최저 시간 당 24만개 제거 보장

### 왜 Redisson은 스케쥴링을 사용할까?

단순히 생각해본다면 이는 Redis에 부하를 일으키는 TTL 적용 방식으로 보인다. 그럼에도 불구하고 이런 클라이언트 사이드 관리 방식을 고수하는 것은 Redis Hash에서 entry별 TTL을 지원하지 않기 때문이다. 만약, 모든 entry에 대한 만료를 Redis 서버로 관리하기 위해 한 개의 Entity마다 독립적인 캐시를 만든다면 어떨까?

관련된 [이슈](https://github.com/redisson/redisson/issues/1064](https://github.com/redisson/redisson/issues/1064)는 이미 오래전에 논의가 됐었다. 내용을 요약하면 다음과 같다.
-   Redis 메모리 사용량을 매우 높일 수 있다.
-   클라이언트에서 관리하는 방식은 실제 Redis 서버 내부 동작을 모티브로 하며 스케쥴링 빈도는 더 낮기때문에 부하에 큰 영향을 주지 않는다.
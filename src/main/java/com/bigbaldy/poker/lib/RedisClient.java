package com.bigbaldy.poker.lib;

import com.bigbaldy.poker.util.StringUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.DefaultStringTuple;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.connection.RedisZSetCommands.Tuple;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;

import javax.validation.constraints.NotNull;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public class RedisClient {

    private static final Logger logger = LoggerFactory.getLogger(RedisClient.class);

    private static final String UNLOCK_SCRIPT_NAME = "unlock";
    private static final String PLACEHOLDER_SUFFIX = "placeholder";
    private static final String PLACEHOLDER = StringUtils.repeat("x", 100);

    private RedisTemplate<String, String> write;

    private Map<Class<?>, RScript.ReturnType> returnTypeMap = new HashMap<Class<?>, RScript.ReturnType>() {
        {
            put(String.class, RScript.ReturnType.STATUS);
            put(Long.class, RScript.ReturnType.INTEGER);
            put(Boolean.class, RScript.ReturnType.BOOLEAN);
            put(List.class, RScript.ReturnType.MULTI);
            put(Object.class, RScript.ReturnType.VALUE);
            put(Map.class, RScript.ReturnType.MAPVALUE);
        }
    };

    @Getter
    private RedissonClient redissonClient;

    private ScriptLoader scriptLoader;

    public RedisClient(RedisTemplate<String, String> write, RedissonClient redissonClient, ScriptLoader scriptLoader) {
        this.write = write;
        this.redissonClient = redissonClient;
        this.scriptLoader = scriptLoader;
    }

    public RedisTemplate<String, String> getTemplate() {
        return write;
    }

    public String setPlaceholder(String prefix) {
        return setPlaceholder(prefix, 100);
    }

    public String setPlaceholder(String prefix, int length) {
        String key = prefix + ":" + PLACEHOLDER_SUFFIX;
        String value;
        if (length == 100) {
            value = PLACEHOLDER;
        } else {
            value = StringUtils.repeat("x", length);
        }
        write.opsForValue().set(key, value, 5, TimeUnit.SECONDS);
        return key;
    }

    public void set(@NotNull String key, String value, long ttl) {
        write.opsForValue().set(key, value, ttl, TimeUnit.SECONDS);
    }

    public long increment(String key, long ttl) {
        List<Object> ret = redisExecuteInTransaction(redisConnection -> {
            byte[] k = StringUtil.getBytes(key);
            redisConnection.incr(k);
            redisConnection.expire(k, ttl);
        });

        return Long.parseLong(ret.get(0).toString());
    }

    public List<Long> increment(List<String> finalKeys, long ttl) {
        if (finalKeys.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        List<Object> ret = redisExecuteInTransaction(redisConnection -> {
            finalKeys.forEach(e -> {
                byte[] k = StringUtil.getBytes(e);

                redisConnection.incr(k);
                redisConnection.expire(k, ttl);
            });
        });

        List<Long> result = new ArrayList<>();

        for (int i = 0; i < ret.size(); i += 2) {
            result.add(Long.valueOf(ret.get(i).toString()));
        }

        return result;
    }


    public Boolean setIfAbsent(@NotNull String key, String value) {
        return write.opsForValue().setIfAbsent(key, value);
    }

    public Boolean setIfAbsent(@NotNull String key, @NotNull String value, long ttl) {
        RBucket<String> rBucket = redissonClient.getBucket(key);
        return rBucket.trySet(value, ttl, TimeUnit.MILLISECONDS);
    }

    public void set(@NotNull Map<String, String> keyValues, long ttl) {
        RBatch batch = redissonClient.createBatch();
        keyValues.forEach((k, v) -> {
            RBucketAsync<String> rBucketAsync = batch.getBucket(k);
            rBucketAsync.setAsync(v);
            rBucketAsync.expireAsync(ttl, TimeUnit.SECONDS);
        });
        batch.execute();
    }

    public String get(@NotNull String key) {
        return getTemplate().opsForValue().get(key);
    }

    public List<String> get(@NotNull Collection<String> keys) {
        RBatch batch = redissonClient.createBatch();
        keys.forEach(p -> batch.getBucket(p).getAsync());
        return batch.execute().getResponses().stream().map(p -> {
            if (p != null) {
                return p.toString();
            }
            return null;
        }).collect(Collectors.toList());
    }

    public Boolean delete(@NotNull String key) {
        return write.delete(key);
    }

    public Long delete(List<String> keys) {
        RBatch batch = redissonClient.createBatch();
        keys.forEach(p -> batch.getBucket(p).deleteAsync());
        return batch.execute().getResponses().stream().filter(p -> (Boolean) p).count();
    }

    public void addListValueToCache(String key, String value, long ttl) {
        redisExecuteInTransaction(redisConnection -> {
            byte[] k = StringUtil.getBytes(key);
            redisConnection.rPush(k, StringUtil.getBytes(value));
            redisConnection.expire(k, ttl);
        });
    }

    public void addListValueLeftToCache(String key, String value, long ttl) {
        redisExecuteInTransaction(redisConnection -> {
            byte[] k = StringUtil.getBytes(key);
            redisConnection.lPush(k, StringUtil.getBytes(value));
            redisConnection.expire(k, ttl);
        });
    }

    public void addListValueLeftToCache(String key, List<String> value, long ttl) {
        if (value.isEmpty()) {
            return;
        }
        redisExecuteInTransaction(redisConnection -> {
            byte[] k = StringUtil.getBytes(key);
            byte[][] bytes = value.stream().map(StringUtil::getBytes).toArray(byte[][]::new);
            redisConnection.lPush(k, bytes);
            redisConnection.expire(k, ttl);
        });
    }

    public void addListValueToCache(String key, List<String> value, long ttl) {
        if (value.isEmpty()) {
            return;
        }
        redisExecuteInTransaction(redisConnection -> {
            byte[] k = StringUtil.getBytes(key);
            byte[][] bytes = value.stream().map(v -> StringUtil.getBytes(v)).toArray(byte[][]::new);
            redisConnection.rPush(k, bytes);
            redisConnection.expire(k, ttl);
        });
    }

    public void reAddListValueToCache(String key, List<String> value, long ttl) {
        if (value.isEmpty()) {
            return;
        }
        redisExecuteInTransaction(redisConnection -> {
            byte[] k = StringUtil.getBytes(key);
            byte[][] bytes = value.stream().map(StringUtil::getBytes).toArray(byte[][]::new);
            redisConnection.del(k);
            redisConnection.rPush(k, bytes);
            redisConnection.expire(k, ttl);
        });
    }

    public List<String> getListValueFromCache(String key) {
        return getListValueFromCache(key, 0, -1);
    }

    /**
     * start 0 stop 10 return 11 members
     *
     * @param stop include stop
     */
    public List<String> getListValueFromCache(String key, int start, int stop) {
        return getTemplate().opsForList().range(key, start, stop);
    }

    public List<String> getAndDeleteListValueFromCache(String key, int start, int stop) {

        List<Object> transaction = redisExecuteInTransaction(redisConnection -> {
            byte[] k = key.getBytes(Charset.forName("utf-8"));
            redisConnection.lRange(k, start, stop - 1);
            redisConnection.lTrim(k, stop, -1);

        });

        List<String> lst = (List<String>) transaction.get(0);
        return lst;
    }

    public void trimListValueFromCache(String key, long start, long stop) {
        write.opsForList().trim(key, start, stop);
    }

    public void deleteListValueFromCache(String key, String k) {
        write.opsForList().remove(key, 0, k);
    }

    public long getListSize(String key) {
        return getTemplate().opsForList().size(key);
    }

    public Optional<String> listRightPop(String key) {
        String ret = write.opsForList().rightPop(key);
        if (ret != null) {
            return Optional.of(ret);
        }
        return Optional.empty();
    }

    public void addSetValueToCache(String key, String value, long ttl) {
        redisExecuteInTransaction(redisConnection -> {
            byte[] k = StringUtil.getBytes(key);
            redisConnection.sAdd(k, StringUtil.getBytes(value));
            redisConnection.expire(k, ttl);
        });
    }

    public void addSetValueToCache(String key, @NotNull Set<String> ids, long ttl) {
        if (ids.isEmpty()) {
            return;
        }

        byte[][] bytes = ids.stream().map(e -> StringUtil.getBytes(e)).toArray(byte[][]::new);

        redisExecuteInTransaction(redisConnection -> {
            byte[] k = key.getBytes(Charset.forName("utf-8"));
            redisConnection.sAdd(k, bytes);
            redisConnection.expire(k, ttl);
        });
    }

    public long deleteSetValueFromCache(String key, Collection<String> value) {
        String[] values = value.stream().toArray(String[]::new);
        return write.opsForSet().remove(key, values);
    }

    public Set<String> hKeys(String key) {
        HashOperations<String, String, String> hashOperations = getTemplate().opsForHash();
        return hashOperations.keys(key);
    }

    public Map<String, String> hGet(String key, List<String> hashKeys) {
        if (hashKeys.isEmpty()) {
            return Collections.EMPTY_MAP;
        }
        HashOperations<String, String, String> hashOperations = getTemplate().opsForHash();
        List<String> values = hashOperations.multiGet(key, hashKeys);

        ArrayList<String> hashKeyList = new ArrayList<>(hashKeys);
        Map<String, String> results = new HashMap<>();
        for (int i = 0; i < hashKeys.size(); i++) {
            if (values.get(i) != null) {
                results.put(hashKeyList.get(i), values.get(i));
            }
        }

        return results;
    }

    public Map<String, String> hGet(String key) {
        HashOperations<String, String, String> hashOperations = getTemplate().opsForHash();
        return hashOperations.entries(key);
    }

    public String hGet(String key, String hashKey) {
        HashOperations<String, String, String> stringObjectObjectHashOperations = getTemplate()
                .opsForHash();
        return stringObjectObjectHashOperations.get(key, hashKey);
    }

    public void hSet(String key, String hashKey, String hashVal, long ttl) {
        redisExecuteInTransaction(redisConnection -> {
            byte[] k = StringUtil.getBytes(key);
            redisConnection.hSet(k, StringUtil.getBytes(hashKey),
                    StringUtil.getBytes(hashVal));
            redisConnection.expire(k, ttl);
        });
    }

    public void hIncrement(String key, String hashKey, long ttl) {
        hIncrement(key, hashKey, 1L, ttl);
    }

    public void hIncrement(String key, String hashKey, Long value, long ttl) {
        redisExecuteInTransaction(redisConnection -> {
            byte[] k = StringUtil.getBytes(key);
            redisConnection.hIncrBy(k, StringUtil.getBytes(hashKey),
                    value);
            redisConnection.expire(k, ttl);
        });
    }

    public List<Long> hIncrement(String key, List<String> hashKeys, long ttl) {
        if (hashKeys.isEmpty()) {
            return Collections.EMPTY_LIST;
        }
        List<Object> ret = redisExecuteInTransaction(redisConnection -> {
            byte[] k = StringUtil.getBytes(key);
            hashKeys.forEach(e -> {
                redisConnection.hIncrBy(k, StringUtil.getBytes(e),
                        1);
            });
            redisConnection.expire(k, ttl);
        });

        List<Long> result = new ArrayList<>();
        for (int i = 0; i < hashKeys.size(); i++) {
            result.add(Long.parseLong(ret.get(i).toString()));
        }
        return result;
    }

    public boolean hSetIfAbsent(String key, String hashKey, String hashVal, long ttl) {
        List<Object> ret = redisExecuteInTransaction(redisConnection -> {
            byte[] k = StringUtil.getBytes(key);
            redisConnection.hSetNX(k, StringUtil.getBytes(hashKey),
                    StringUtil.getBytes(hashVal));
            redisConnection.expire(k, ttl);
        });
        return Boolean.parseBoolean(ret.get(0).toString());
    }

    public void hSetIfAbsent(String key, Map<String, String> keyVals, long ttl) {
        if (keyVals.isEmpty()) {
            return;
        }
        redisExecuteInTransaction(redisConnection -> {
            byte[] kb = StringUtil.getBytes(key);
            keyVals.forEach((k, v) -> {
                redisConnection.hSetNX(kb, StringUtil.getBytes(k), StringUtil.getBytes(v));
            });
            redisConnection.expire(kb, ttl);
        });
    }

    public void hSet(String key, Map<String, String> keyVals, long ttl) {
        if (keyVals.isEmpty()) {
            return;
        }
        redisExecuteInTransaction(redisConnection -> {
            byte[] k = StringUtil.getBytes(key);
            Map<byte[], byte[]> collect = keyVals.entrySet().stream()
                    .collect(Collectors
                            .toMap(e -> StringUtil.getBytes(e.getKey()), e -> StringUtil.getBytes(e.getValue())));
            redisConnection.hMSet(k, collect);
            redisConnection.expire(k, ttl);
        });
    }

    public void hDelete(String key, String hashKey) {
        write.opsForHash().delete(key, hashKey);
    }

    public void hDelete(String key, Collection<String> hashKeys) {
        if (hashKeys.isEmpty()) {
            return;
        }
        String[] ks = hashKeys.stream().toArray(String[]::new);
        write.opsForHash().delete(key, ks);
    }

    public long hsize(String key) {
        return write.opsForHash().size(key);
    }

    public List<Object> hvalues(String key) {
        return write.opsForHash().values(key);
    }

    public boolean hexist(String key, String fieldKey) {
        return write.opsForHash().hasKey(key, fieldKey);
    }

    public List<String> getZSetValueAscFromCache(String key, int start, int end) {
        Set<String> ret = getTemplate().opsForZSet().range(key, start, end);
        return new ArrayList<>(ret);
    }

    public void addZSetValueToCache(String key, Map<String, Double> items, long ttl) {
        if (items.isEmpty()) {
            return;
        }
        redisExecuteInTransaction(redisConnection -> {
            byte[] k = StringUtil.getBytes(key);
            Set<Tuple> set = new HashSet<>();
            items.forEach((s, d) -> {
                Tuple tuple = new DefaultStringTuple(StringUtil.getBytes(s), s, d);
                set.add(tuple);
            });
            redisConnection.zAdd(k, set);
            redisConnection.expire(k, ttl);
        });
    }

    public void addZSetValueToCache(String key, String k, double v, long ttl) {
        redisExecuteInTransaction(redisConnection -> {
            redisConnection.zAdd(StringUtil.getBytes(key), v, StringUtil.getBytes(k));
            redisConnection.expire(StringUtil.getBytes(key), ttl);
        });
    }

    public void zremrangebyrank(String key, Long start, Long end) {
        write.opsForZSet().removeRange(key, start, end);
    }

    public List<String> getAllZSetValueDescFromCache(String key) {
        return getZSetValueDescFromCache(key, 0, -1);
    }

    public List<String> getZSetValueDescFromCache(String key, int start, int end) {
        Set<String> ret = getTemplate().opsForZSet().reverseRange(key, start, end);
        return new ArrayList<>(ret);
    }

    public List<ZSetEntity> getZSetValueWithScoresDescFromCache(String key, int start,
                                                                int end) {
        Set<TypedTuple<String>> ret = getTemplate().opsForZSet()
                .reverseRangeWithScores(key, start, end);
        List<ZSetEntity> entities = new ArrayList<>();
        ret.forEach(e -> {
            entities.add(new ZSetEntity(e.getValue(), e.getScore()));
        });

        return entities;
    }

    public List<String> getZSetValueDescFromCache(String key, double minScore, double maxScore,
                                                  int limit) {
        Set<String> ret = getTemplate().opsForZSet()
                .reverseRangeByScore(key, minScore, maxScore, 0, limit);
        return new ArrayList<>(ret);
    }

    public List<ZSetEntity> getZSetValueWithScoresDescFromCache(String key,
                                                                double minScore, double maxScore,
                                                                int limit) {
        Set<TypedTuple<String>> ret = getTemplate().opsForZSet()
                .reverseRangeByScoreWithScores(key, minScore, maxScore, 0, limit);
        List<ZSetEntity> list = new ArrayList<>();
        ret.forEach(e -> {
            list.add(new ZSetEntity(e.getValue(), e.getScore()));
        });

        return list;
    }

    public List<String> getZSetValueAscFromCache(String key, double minScore, double maxScore,
                                                 int limit) {
        Set<String> ret = getTemplate().opsForZSet()
                .rangeByScore(key, minScore, maxScore, 0, limit);
        return new ArrayList<>(ret);
    }

    public Optional<Long> getZSetValueDescRank(String key, String fieldKey) {
        Long ret = getTemplate().opsForZSet().reverseRank(key, fieldKey);
        if (ret != null) {
            return Optional.of(ret);
        }
        return Optional.empty();
    }

    public List<Integer> getZSetValueDescRank(String key, List<String> fieldKeys) {
        if (fieldKeys.isEmpty()) {
            return Collections.EMPTY_LIST;
        }
        List<Object> ret = redisExecuteInTransaction(redisConnection -> {
            byte[] k = StringUtil.getBytes(key);
            fieldKeys.forEach(e -> {
                redisConnection.zRevRank(k, StringUtil.getBytes(e));
            });
        });

        return ret.stream().map(e -> e == null ? -1 : Integer.parseInt(e.toString()))
                .collect(Collectors.toList());
    }

    public long deleteZSetValueFromCache(String key, String fieldKey) {
        Long ret = write.opsForZSet().remove(key, fieldKey);
        return ret;
    }

    public long deleteZSetValueFromCache(String key, String... fieldKeys) {
        Long ret = write.opsForZSet().remove(key, fieldKeys);
        return ret;
    }

    public long deleteZSetValueByScoreFromCache(String key, double min, double max) {
        Long ret = write.opsForZSet().removeRangeByScore(key, min, max);
        return ret;
    }

    public long deleteZSetValueByRankFromCache(String key, long min, long max) {
        Long ret = write.opsForZSet().removeRange(key, min, max);
        return ret;
    }

    public Double getZSetValueWithScore(String key, String fieldKey) {
        return getTemplate().opsForZSet().score(key, fieldKey);
    }

    public boolean existZSetValueFromCache(String key, @NotNull String k) {
        return getZSetValueWithScore(key, k) != null;
    }

    public long getZSetSize(String key) {
        return getTemplate().opsForZSet().size(key);
    }

    public long getZSetSize(String key, double min, double max) {
        return getTemplate().opsForZSet().count(key, min, max);
    }

    public boolean isExists(String key) {
        return getTemplate().hasKey(key);
    }

    public Set<String> keys(String keyPattern) {
        return getTemplate().keys(keyPattern);
    }

    public void expire(String key, long ttl) {
        write.expire(key, ttl, TimeUnit.SECONDS);
    }

    public void pexpire(String key, long ttl) {
        write.expire(key, ttl, TimeUnit.MILLISECONDS);
    }

    public Long getExpire(String key) {
        return getTemplate().getExpire(key, TimeUnit.SECONDS);
    }

    public Long getPexpire(String key) {
        return getTemplate().getExpire(key, TimeUnit.MILLISECONDS);
    }

    public Set<String> getSetFromCache(String key) {
        return getTemplate().opsForSet().members(key);
    }

    public Set<String> getSetIntersect(String key, String key2) {
        return getTemplate().opsForSet().intersect(key, key2);
    }

    public Set<String> getSetRandomMembers(String key, int num) {
        return redissonClient.getSet(key).random(num).stream().map(Object::toString).collect(Collectors.toSet());
    }

    public boolean existSetValueFromCache(String key, @NotNull String value) {
        return getTemplate().opsForSet().isMember(key, value);
    }

    public void rename(String oldKey, String newKey) {
        write.rename(oldKey, newKey);
    }

    public Long geoadd(String key, Map<String, Point> map) {
        return write.opsForGeo().add(key, map);
    }

    public GeoResults<RedisGeoCommands.GeoLocation<String>> georadius(String key, Circle circle, RedisGeoCommands.GeoRadiusCommandArgs geoRadiusCommandArgs) {

        return write.opsForGeo().radius(key, circle, geoRadiusCommandArgs);
    }

    public void execScript(String scriptName, List<Object> keys, Object... args) {
        redissonClient.getScript().eval(RScript.Mode.READ_WRITE, scriptLoader.getScript(scriptName), RScript.ReturnType.VALUE, keys, args);
    }

    public <R> R execScript(String scriptName, Class<R> returnClass, List<Object> keys, Object... args) {
        return redissonClient.getScript().eval(RScript.Mode.READ_WRITE, scriptLoader.getScript(scriptName), returnTypeMap.get(returnClass), keys, args);
    }

    public void flushAll() {
        write.execute((RedisCallback<Boolean>) connection -> {
            connection.flushAll();
            return true;
        });
    }

    public List<Object> redisExecuteInTransaction(Consumer<RedisConnection> consumer) {
        return (List<Object>) write.executePipelined((RedisCallback<List<Object>>) connection -> {
            try {
                connection.multi();
                consumer.accept(connection);
                return connection.exec();
            } catch (Exception e) {
                logger.error("redisExecuteInTrasaction failed", e);
                return Collections.emptyList();
            }
        }).get(0);
    }

    public RedisBlockLock getLock(String key, long milliseconds) {
        RedisBlockLock lock = new RedisBlockLock();
        String value = UUID.randomUUID().toString();
        lock.key = key;
        lock.value = value;
        lock.ttl = milliseconds;
        return lock;
    }

    public RedisLock lock(String key, long milliseconds) {
        String value = UUID.randomUUID().toString();
        boolean ret = setIfAbsent(key, value, milliseconds);
        RedisLock lock = new RedisLock();
        lock.key = key;
        lock.value = value;
        lock.success = ret;
        return lock;
    }

    public boolean unlock(RedisLock redisLock) {
        List<Object> keys = new ArrayList<>();
        keys.add(redisLock.key);
        return execScript(UNLOCK_SCRIPT_NAME, Long.class, keys, redisLock.value) != 0;
    }

    public String lindex(String key, long index) {
        return write.opsForList().index(key, index);
    }

    public void persist(String key) {
        write.persist(key);
    }

    private Map<Integer, List<String>> groupKeysBySlot(List<String> keys) {
        if (redissonClient.getConfig().isClusterConfig()) {
            BatchOptions options = BatchOptions.defaults();
            RBatch batch = redissonClient.createBatch(options);
            for (String key : keys) {
                batch.getKeys().getSlotAsync(key);
            }

            BatchResult<?> result = batch.execute();
            Map<Integer, List<String>> keysGroupBySlot = new HashMap<>();
            List<Integer> slots = (List<Integer>) result.getResponses();
            int size = slots.size();
            for (int i = 0; i < size; i++) {

                Integer slot = slots.get(i);
                List<String> keysInSlot = keysGroupBySlot.computeIfAbsent(slot, k -> new ArrayList<>());
                keysInSlot.add(keys.get(i));
            }

            return keysGroupBySlot;
        }

        // otherwise
        Map<Integer, List<String>> defaults = new HashMap<>();
        defaults.put(0, keys);
        return defaults;
    }

    public static class ZSetEntity {

        public ZSetEntity(String value, Double score) {
            this.value = value;
            this.score = score;
        }

        private String value;
        private Double score;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public Double getScore() {
            return score;
        }

        public void setScore(Double score) {
            this.score = score;
        }
    }

    public class RedisBlockLock {
        String key;

        String value;

        long ttl;

        public void lock() {
            while (!setIfAbsent(key, value, ttl)) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public boolean unlock() {
            List<Object> keys = new ArrayList<>();
            keys.add(key);
            return execScript(UNLOCK_SCRIPT_NAME, Boolean.class, keys, value);
        }
    }

    @Getter
    public static class RedisLock {

        String key;

        String value;

        boolean success;
    }
}

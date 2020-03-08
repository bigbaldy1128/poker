package com.bigbaldy.poker.service;

import com.bigbaldy.poker.lib.RedisClient;
import com.bigbaldy.poker.model.BaseModel;
import com.bigbaldy.poker.util.JsonUtil;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractService<T extends BaseModel<ID>, ID> implements IService<T, ID> {

    protected static final Long DEFAULT_TTL = 7 * 24 * 60 * 60L; // seconds

    private static final int BATCH_SIZE = 100;

    protected static final String NULL_VALUE = "$N";

    private volatile LoadingCache<ID, T> localCache;

    @Override
    public abstract T saveToDb(T entity);

    protected abstract void deleteFromDbById(ID id);

    protected abstract void deleteFromDb(List<T> entity);

    protected abstract Iterable<T> saveAllToDb(List<T> list);

    protected abstract Iterable<T> findAllFromDbById(List<ID> ids);

    protected abstract Optional<T> findFromDbById(ID id);

    protected abstract RedisClient getRedisClient();

    protected abstract long getCacheTTL();

    protected long getLocalCacheTTL() {
        return 3000L; //milliseconds
    }

    protected int initialCapacity() {
        return 1000;
    }

    protected int concurrencyLevel() {
        return 200;
    }

    protected abstract Class<T> getModelClass();

    protected static final ExecutorService executor = Executors.newFixedThreadPool(50, (r) -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    @Override
    public Optional<T> get(@NotNull ID id) {

        String json = getFromCache(id, getModelClass());

        if (NULL_VALUE.equals(json)) {
            return Optional.empty();
        }

        if (json != null) {
            return JsonUtil.toObject(json, getModelClass());
        }

        Optional<T> t = findFromDbById(id);
        if (t.isPresent()) {
            setToCache(t.get());
        } else {
            String key = getCacheKeyFromId(id, getModelClass());
            setToCache(key, NULL_VALUE, getCacheTTL());
        }
        return t;

    }

    @Override
    public Optional<T> getFromDb(@NotNull ID id) {
        return findFromDbById(id);
    }

    @Override
    public Optional<T> getFromLocalCache(ID id) {
        try {
            return Optional.of(getLocalCache().get(id));
        } catch (ExecutionException e) {
            log.error("getFromLocalCache failed", e);
        }
        return Optional.empty();
    }

    @Override
    public Map<ID, T> getMap(@NotNull List<ID> ids) {
        List<T> ret = get(ids);
        Map<ID, T> map = new LinkedHashMap<>();
        ret.forEach(e -> {
            map.put(e.getId(), e);
        });
        return map;
    }

    @Override
    public Map<ID, T> getMapFromDb(@NotNull List<ID> ids) {
        List<T> ret = getFromDb(ids);
        Map<ID, T> map = new LinkedHashMap<>();
        ret.forEach(e -> {
            map.put(e.getId(), e);
        });
        return map;
    }

    @Override
    public Map<ID, T> getMapFromLocalCache(List<ID> ids) {
        try {
            return getLocalCache().getAll(ids);
        } catch (ExecutionException e) {
            log.error("getMapFromLocalCache failed", e);
        }
        return new HashMap<>();
    }

    @Override
    public List<T> get(@NotNull List<ID> ids) {

        if (ids.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        ids = ids.stream().distinct().collect(Collectors.toList());

        int total = ids.size();

        if (total < BATCH_SIZE) {
            return batchGets(ids);
        }

        int start = 0;
        List<T> ret = new ArrayList<>();
        for (; ; ) {
            List<ID> tmps = ids.stream().skip(start).limit(BATCH_SIZE).collect(Collectors.toList());
            ret.addAll(batchGets(tmps));
            start += BATCH_SIZE;
            if (start > total - 1) {
                break;
            }
        }
        return ret;
    }

    @Override
    public List<T> getFromDb(@NotNull List<ID> ids) {
        return (List<T>)findAllFromDbById(ids);
    }

    @Override
    public List<T> getOnlyFromCache(@NotNull List<ID> ids) {
        if (ids.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        ids = ids.stream().distinct().collect(Collectors.toList());

        int total = ids.size();

        if (total < BATCH_SIZE) {
            return batchGetsFromCache(ids);
        }

        int start = 0;
        List<T> ret = new ArrayList<>();
        for (; ; ) {
            List<ID> tmps = ids.stream().skip(start).limit(BATCH_SIZE).collect(Collectors.toList());
            ret.addAll(batchGetsFromCache(tmps));
            start += BATCH_SIZE;
            if (start > total - 1) {
                break;
            }
        }
        return ret;
    }


    private List<T> batchGets(List<ID> ids) {
        List<String> jsons = getFromCache(ids, getModelClass());

        Map<ID, T> entityMap = new HashMap<>();
        jsons.forEach(v -> {
            if (!NULL_VALUE.equals(v)) {
                Optional<T> val = JsonUtil.toObject(v, getModelClass());
                if (val.isPresent()) {
                    entityMap.put(val.get().getId(), val.get());
                }
            }
        });

        List<ID> uncached = new ArrayList<>();

        ids.forEach(e -> {
            if (!entityMap.containsKey(e)) {
                uncached.add(e);
            }
        });

        if (uncached.size() > 0) {
            Iterable<T> dbEntityList = findAllFromDbById(uncached);
            List<T> lst = interableToList(dbEntityList);
            setToCache(lst);
            lst.forEach(e -> {
                entityMap.put(e.getId(), e);
            });
            lst.forEach(e -> {
                uncached.remove(e.getId());
            });
            if (!uncached.isEmpty()) {
                Map<String, String> saves = new HashMap<>();
                uncached.forEach(e -> {
                    saves.put(getCacheKeyFromId(e, getModelClass()), NULL_VALUE);
                });
                setToCache(saves);
            }
        }

        List<T> result = new ArrayList<>();

        ids.forEach(e -> {
            if (entityMap.containsKey(e)) {
                result.add(entityMap.get(e));
            }
        });

        return result;
    }

    private List<T> batchGetsFromCache(List<ID> ids) {
        List<T> result = new ArrayList<>();
        List<String> jsons = getFromCache(ids, getModelClass());
        jsons.forEach(v -> {
            if (!NULL_VALUE.equals(v)) {
                Optional<T> val = JsonUtil.toObject(v, getModelClass());
                if (val.isPresent()) {
                    result.add(val.get());
                }
            }
        });
        return result;
    }

    @Override
    public T save(@NotNull T t) {
        t = saveToDb(t);
        setToCache(t);
        return t;
    }

    @Override
    public List<T> save(List<T> t) {
        if (t.isEmpty()) {
            return t;
        }
        int total = t.size();
        if (total < BATCH_SIZE) {
            return batchSave(t);
        }

        int start = 0;
        List<T> ret = new ArrayList<>();
        for (; ; ) {
            List<T> tmps = t.stream().skip(start).limit(BATCH_SIZE).collect(Collectors.toList());
            ret.addAll(batchSave(tmps));
            start += BATCH_SIZE;
            if (start > total - 1) {
                break;
            }
        }
        return ret;
    }

    @Override
    public List<T> saveToDb(List<T> t) {
        return (List<T>) saveAllToDb(t);
    }

    private List<T> batchSave(List<T> t) {
        Iterable<T> ret = saveAllToDb(t);
        List<T> lst = interableToList(ret);
        setToCache(t);
        return lst;
    }

    @Override
    public void remove(@NotNull ID id) {
        deleteFromCache(getCacheKeyFromId(id, getModelClass()));
        deleteFromDbById(id);
    }

    @Override
    public void removeFromDb(@NotNull ID id) {
        deleteFromDbById(id);
    }

    @Override
    public void remove(@NotNull List<T> t) {
        t.removeIf(e -> e == null);
        if (t.isEmpty()) {
            return;
        }
        int total = t.size();
        if (total < BATCH_SIZE) {
            batchRemove(t);
            return;
        }

        int start = 0;
        for (; ; ) {
            List<T> tmps = t.stream().skip(start).limit(BATCH_SIZE).collect(Collectors.toList());
            batchRemove(tmps);
            start += BATCH_SIZE;
            if (start > total - 1) {
                break;
            }
        }
    }

    @Override
    public void removeFromDb(@NotNull List<T> t) {
        deleteFromDb(t);
    }

    private void batchRemove(@NotNull List<T> t) {
        List<String> cacheIds = t.stream()
                .map(e -> getCacheKeyFromId(e.getId(), (Class<T>) e.getClass()))
                .collect(Collectors.toList());
        deleteFromCache(cacheIds);
        deleteFromDb(t);
    }

    protected void setToCache(@NotNull T t, long ttl) {
        String key = getCacheKeyFromId(t.getId(), (Class<T>) t.getClass());
        Optional<String> json = JsonUtil.toJson(t);

        if (json.isPresent()) {
            getRedisClient().set(key, json.get(), ttl);
        }
    }

    protected void setToCache(@NotNull T t) {

        setToCache(t, getCacheTTL());
    }

    protected void setToCache(@NotNull List<T> list) {
        if (list.isEmpty()) {
            return;
        }

        Map<String, String> keyValues = list.stream().collect(Collectors.toMap(
                t -> getCacheKeyFromId(t.getId(), (Class<T>) t.getClass()), t -> JsonUtil.toJson(t).get()));

        getRedisClient().set(keyValues, getCacheTTL());
    }

    protected void setToCache(@NotNull Map<String, String> keyValues) {
        getRedisClient().set(keyValues, getCacheTTL());
    }

    protected void setToCache(@NotNull String key, String value, long ttl) {
        if (key.isEmpty()) {
            return;
        }

        getRedisClient().set(key, value, ttl);
    }

    protected void setToCache(@NotNull String key, String value) {
        setToCache(key, value, getCacheTTL());
    }

    protected boolean deleteFromCache(@NotNull String key) {
        return getRedisClient().delete(key);
    }

    protected boolean deleteFromCache(List<String> keys) {
        getRedisClient().delete(keys);
        return true;
    }

    protected Optional<String> getFromCache(@NotNull String key) {
        String result = getRedisClient().get(key);
        if (result == null) {
            return Optional.empty();
        }

        return Optional.of(result);
    }

    protected String getFromCache(@NotNull ID id, Class<T> clz) {
        String cacheKey = getCacheKeyFromId(id, clz);
        String json = getRedisClient().get(cacheKey);
        return json;
    }

    protected List<String> getFromCache(@NotNull Collection<String> keys) {

        List<String> ret = getFromCacheContainsNull(keys);
        ret.removeIf(e -> e == null);
        return ret;
    }

    protected List<String> getFromCacheContainsNull(@NotNull Collection<String> keys) {
        if (keys.isEmpty()) {
            return Collections.EMPTY_LIST;
        }
        List<String> ret = getRedisClient().get(keys);
        return ret;
    }


    protected List<String> getFromCache(@NotNull List<ID> ids, Class<T> clz) {
        if (ids.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        List<String> cacheKeys = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            cacheKeys.add(getCacheKeyFromId(ids.get(i), clz));
        }

        List<String> ret = getRedisClient().get(cacheKeys);
        ret.removeIf(e -> e == null);
        return ret;
    }

    protected void addListValueToCache(String key, String value) {
        getRedisClient().addListValueToCache(key, value, getCacheTTL());
    }

    protected void addListValueLeftToCache(String key, String value) {
        getRedisClient().addListValueLeftToCache(key, value, getCacheTTL());
    }

    protected void addListValueToCache(String key, List<String> value) {
        getRedisClient().addListValueToCache(key, value, getCacheTTL());
    }

    protected void addListValueLeftToCache(String key, List<String> value) {
        getRedisClient().addListValueLeftToCache(key, value, getCacheTTL());
    }

    protected List<String> getListValueFromCache(String key) {
        return getListValueFromCache(key, 0, -1);
    }

    protected List<String> getListValueFromCache(String key, int start, int stop) {
        return getRedisClient().getListValueFromCache(key, start, stop);
    }

    protected void trimListValueFromCache(String key, int start, int stop) {
        getRedisClient().trimListValueFromCache(key, start, stop);
    }

    protected void addSetValueToCache(String key, String value) {
        getRedisClient().addSetValueToCache(key, value, getCacheTTL());
    }

    protected void addSetValueToCache(String key, String value, long ttl) {
        getRedisClient().addSetValueToCache(key, value, ttl);
    }

    public void addSetValueToCache(String key, @NotNull Set<String> ids) {
        int step = 500;
        int idsSize = ids.size();
        List<String> list = new ArrayList<>(ids);
        for (int i = 0; i < idsSize; i = i + step) {
            Set<String> subSet = new HashSet(list.subList(i, Math.min(i + step, idsSize)));
            getRedisClient().addSetValueToCache(key, subSet, getCacheTTL());
        }
    }

    protected void deleteSetValueFromCache(String key, String value) {
        ArrayList<String> values = new ArrayList<>();
        values.add(value);
        deleteSetValueFromCache(key, values);
    }

    protected void deleteSetValueFromCache(String key, Collection<String> value) {
        getRedisClient().deleteSetValueFromCache(key, value);
    }

    protected Set<String> getSetFromCache(String key) {
        return getRedisClient().getSetFromCache(key);
    }

    public boolean existSetValueFromCache(String key, @NotNull String value) {
        return getRedisClient().existSetValueFromCache(key, value);
    }

    protected boolean exist(String key) {
        return getRedisClient().isExists(key);
    }

    protected boolean existId(ID id) {
        String key = getCacheKeyFromId(id, getModelClass());
        String value = getRedisClient().get(key);
        return value != null && !value.equals(NULL_VALUE);
    }

    protected String getCacheKey(String key, Class<T> clz) {
        StringBuilder sb = new StringBuilder();
        sb.append(clz.getSimpleName()).append(":").append(key);
        return sb.toString();
    }

    private String getCacheKeyFromId(@NotNull ID id, Class<T> clz) {
        StringBuilder sb = new StringBuilder();
        sb.append("id:").append(id.toString());
        return getCacheKey(sb.toString(), clz);
    }

    private LoadingCache<ID, T> getLocalCache() {
        if (localCache == null) {
            synchronized (this) {
                if (localCache == null) {
                    localCache = CacheBuilder.newBuilder()
                            .expireAfterAccess(getLocalCacheTTL(), TimeUnit.MILLISECONDS)
                            .initialCapacity(initialCapacity())
                            .concurrencyLevel(concurrencyLevel())
                            .build(new CacheLoader<ID, T>() {
                                @Override
                                public T load(ID id) throws Exception {
                                    return get(id).orElse(null);
                                }

                                @Override
                                public Map<ID, T> loadAll(Iterable<? extends ID> ids) throws Exception {
                                    return getMap(Lists.newArrayList(ids));
                                }
                            });
                }
            }
        }
        return localCache;
    }

    protected <S> List<S> interableToList(Iterable<S> iterable) {
        List<S> list = new ArrayList<>();
        for (S i : iterable) {
            list.add(i);
        }
        return list;
    }

    public static class CacheResult<T> {

        private boolean cached;
        private T value;

        private CacheResult(boolean cached, T value) {
            this.cached = cached;
            this.value = value;
        }

        public static CacheResult noCache() {
            return new CacheResult<>(false, null);
        }

        public static CacheResult noResult() {
            return new CacheResult<>(true, null);
        }

        public static <T> CacheResult<T> of(T v) {
            return new CacheResult(true, v);
        }

        public boolean isCached() {
            return cached;
        }

        public void setCached(boolean cached) {
            this.cached = cached;
        }

        public Optional<T> getValue() {
            if (value == null) {
                return Optional.empty();
            }

            return Optional.of(value);
        }

        public void setValue(T value) {
            this.value = value;
        }
    }
}

package com.bigbaldy.poker.service;

import com.bigbaldy.poker.lib.RedisClient;
import com.bigbaldy.poker.model.BaseModel;
import com.bigbaldy.poker.model.PageArgument;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.bigbaldy.poker.service.AbstractService.DEFAULT_TTL;
import static com.bigbaldy.poker.service.AbstractService.NULL_VALUE;


/**
 * @author wangjinzhao on 2020/5/15
 */
public abstract class AbstractIdSupplier<T extends BaseModel<ID>, ID, QUEUE_ID, ELEMENT_ID> {
    private final RedisClient redisClient;
    private final AbstractService<T, ID> service;

    public AbstractIdSupplier(RedisClient redisClient, AbstractService<T, ID> service) {
        this.redisClient = redisClient;
        this.service = service;
    }

    public abstract List<T> getFromDB(QUEUE_ID id, Long end, Pageable pageable);

    protected abstract String getQueueCacheKey(QUEUE_ID queueId);

    protected abstract QUEUE_ID getQueueId(T item);

    protected abstract ELEMENT_ID getElementId(T item);

    protected abstract ELEMENT_ID convertToElementType(String value);

    public final List<ELEMENT_ID> getIdsWithPaged(QUEUE_ID queueId, PageArgument page) {
        page.setSize(page.getSize() + 1);
        int cacheTotal = getCacheTotal();
        if (cacheTotal < page.getSize()) {
            throw new RuntimeException("cache total less than page size +1");
        }
        String cacheKey = getQueueCacheKey(queueId);
        List<ELEMENT_ID> cache = getFromCache(queueId, page, redisClient);
        if (cache != null && cache.size() == page.getSize()) {
            return handleResult(cache, page);
        }
        if (cache == null) {
            Pageable pageable = PageRequest.of(0, cacheTotal);
            List<T> fromDB = getFromDB(queueId, Long.MAX_VALUE, pageable);
            if (!fromDB.isEmpty()) {
                Map<String, Double> items = new HashMap<>();
                for (T item : fromDB) {
                    items.put(getElementId(item).toString(), (double) getItemScore(item));
                }
                redisClient.addZSetValueToCache(
                        cacheKey,
                        items,
                        DEFAULT_TTL
                );
                if (fromDB.size() >= page.getSize()) {
                    page.setToken(getItemScore(fromDB.get(page.getSize() - 1)));
                }
            }

            List<ELEMENT_ID> ret = fromDB
                    .stream()
                    .limit(page.getSize())
                    .map(this::getElementId)
                    .collect(Collectors.toList());
            if (ret.size() < page.getSize()) {
                page.setToken(null);
            }
            return handleResult(ret, page);
        }

        //from cache and db
        long size = redisClient.getZSetSize(cacheKey);
        if (size >= cacheTotal && cache.size() < page.getSize()) {
            int sizeFromDB = page.getSize() - cache.size();
            Pageable pageable = PageRequest.of(0, sizeFromDB);
            List<T> fromDB = getFromDB(queueId, page.getToken(), pageable);
            if (fromDB.size() == sizeFromDB) {
                page.setToken(getItemScore((fromDB.get(fromDB.size() - 1))));
            }
            cache.addAll(fromDB.stream().map(this::getElementId).collect(Collectors.toList()));
        }

        return handleResult(cache, page);
    }

    private List<ELEMENT_ID> handleResult(List<ELEMENT_ID> result, PageArgument page) {
        page.setSize(page.getSize() - 1);
        if (result.size() <= page.getSize()) {
            page.setToken(null);
        } else {
            result = result.subList(0, page.getSize());
            page.setToken(page.getToken() + 1);
        }
        return result;
    }

    public List<ELEMENT_ID> getFromCache(QUEUE_ID queueId, PageArgument page, RedisClient redisClient) {
        List<ELEMENT_ID> ret = new ArrayList<>();
        String cacheKey = getQueueCacheKey(queueId);
        if (!redisClient.isExists(cacheKey)) {
            return null;
        }
        List<RedisClient.ZSetEntity> zSetEntities = redisClient.getZSetValueWithScoresDescFromCache(
                cacheKey,
                0D,
                page.getToken(),
                page.getSize());
        for (RedisClient.ZSetEntity zSetEntity : zSetEntities) {
            String value = zSetEntity.getValue();
            if (NULL_VALUE.equals(value)) {
                continue;
            }
            ret.add(convertToElementType(value));
            page.setToken((long) (double) zSetEntity.getScore());
        }
        return ret;
    }

    public Long getItemScore(T item) {
        return item.getUpdatedAt().toEpochMilli();
    }

    public int getCacheTotal() {
        return 500;
    }

    public T save(T item) {
        String cacheKey = getQueueCacheKey(getQueueId(item));
        if (redisClient.isExists(cacheKey)) {
            redisClient.addZSetValueToCache(cacheKey, getElementId(item).toString(), (double) getItemScore(item), service.getCacheTTL());
            redisClient.trimZSetValueFromCache(cacheKey, getCacheTotal());
        }
        return item;
    }

    public List<T> save(List<T> items) {
        if (items.isEmpty()) {
            return new ArrayList<>();
        }
        String cacheKey = getQueueCacheKey(getQueueId(items.get(0)));
        if (redisClient.isExists(cacheKey)) {
            Map<String, Double> _items = items.stream()
                    .collect(Collectors.toMap(p -> getElementId(p).toString(), p -> (double) getItemScore(p)));
            redisClient.addZSetValueToCache(cacheKey, _items, service.getCacheTTL());
            redisClient.trimZSetValueFromCache(cacheKey, getCacheTotal());
        }
        return items;
    }

    public void remove(T item) {
        String cacheKey = getQueueCacheKey(getQueueId(item));
        redisClient.deleteZSetValueFromCache(cacheKey, getElementId(item).toString());
    }

    public void remove(List<T> items) {
        if (items.isEmpty()) {
            return;
        }
        String cacheKey = getQueueCacheKey(getQueueId(items.get(0)));
        String[] fieldKeys = items.stream()
                .map(p -> getElementId(p).toString())
                .toArray(String[]::new);
        redisClient.deleteZSetValueFromCache(cacheKey, fieldKeys);
    }
}

package com.bigbaldy.poker.service;

import com.bigbaldy.poker.lib.RedisClient;
import com.bigbaldy.poker.model.BaseModel;
import com.bigbaldy.poker.model.PageArgument;
import com.bigbaldy.poker.model.id.ICacheId;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.bigbaldy.poker.service.AbstractService.NULL_VALUE;

/**
 * @author wangjinzhao on 2020/5/15
 */
public abstract class AbstractIdSupplier<T extends BaseModel<ID> & ICacheId<QUEUE_ID>, ID, QUEUE_ID> {
    private RedisClient redisClient;
    private AbstractService<T, ID> service;

    public AbstractIdSupplier(RedisClient redisClient, AbstractService<T, ID> service) {
        this.redisClient = redisClient;
        this.service = service;
    }

    public abstract List<T> getFromDB(QUEUE_ID id, Instant end, Pageable pageable);

    public abstract String getCacheKey(QUEUE_ID queueId);

    public List<Long> getFromCache(QUEUE_ID queueId, PageArgument page, RedisClient redisClient) {
        List<Long> ret = new ArrayList<>();
        String cacheKey = getCacheKey(queueId);
        if (!redisClient.isExists(cacheKey)) {
            return null;
        }
        List<RedisClient.ZSetEntity> zSetEntities = redisClient.getZSetValueWithScoresDescFromCache(
                cacheKey,
                0D,
                page.getEnd().toEpochMilli(),
                page.getSize());
        for (RedisClient.ZSetEntity zSetEntity : zSetEntities) {
            String value = zSetEntity.getValue();
            if (NULL_VALUE.equals(value)) {
                continue;
            }
            ret.add(Long.parseLong(value));
            long score = (long) (double) zSetEntity.getScore();
            page.setToken(score);
        }
        return ret;
    }

    public Long getItemScore(T item) {
        return item.getUpdatedAt().toEpochMilli();
    }

    public int getCacheTotal() {
        return 500;
    }

    public T save(QUEUE_ID queueId, T item) {
        String cacheKey = getCacheKey(queueId);
        if (redisClient.isExists(cacheKey)) {
            redisClient.addZSetValueToCache(cacheKey, item.getCacheId(queueId).toString(), (double) getItemScore(item), service.getCacheTTL());
            redisClient.trimZSetValueFromCache(cacheKey, getCacheTotal());
        }
        return item;
    }

    public List<T> save(QUEUE_ID queueId, List<T> items) {
        String cacheKey = getCacheKey(queueId);
        if (redisClient.isExists(cacheKey)) {
            Map<String, Double> _items = items.stream()
                    .collect(Collectors.toMap(p -> p.getCacheId(queueId).toString(), p -> (double) getItemScore(p)));
            redisClient.addZSetValueToCache(cacheKey, _items, service.getCacheTTL());
            redisClient.trimZSetValueFromCache(cacheKey, getCacheTotal());
        }
        return items;
    }

    public void remove(QUEUE_ID queueId, T item) {
        redisClient.deleteZSetValueFromCache(getCacheKey(queueId), item.getCacheId(queueId).toString());
    }

    public void remove(QUEUE_ID queueId, List<T> items) {
        String[] fieldKeys = items.stream()
                .map(p -> p.getCacheId(queueId).toString())
                .toArray(String[]::new);
        redisClient.deleteZSetValueFromCache(getCacheKey(queueId), fieldKeys);
    }
}

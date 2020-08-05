package com.bigbaldy.poker.service;

import com.bigbaldy.poker.model.BaseModel;
import com.bigbaldy.poker.model.PageArgument;
import com.bigbaldy.poker.model.id.ICacheId;
import org.redisson.api.RLock;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author wangjinzhao on 2020/5/15
 */
public abstract class AbstractQueuedDbService<T extends BaseModel<ID> & ICacheId<QUEUE_ID>, ID, QUEUE_ID> extends AbstractDbService<T, ID> {

    protected final List<Long> getIdsFromQueue(QUEUE_ID queueId, AbstractIdSupplier<T, ID, QUEUE_ID> supplier, PageArgument page) {
        List<Long> cache = supplier.getFromCache(queueId, page, getRedisClient());
        //from cache
        if (cache != null && cache.size() == page.getSize()) {
            return cache;
        }
        int cacheTotal = supplier.getCacheTotal();
        String cacheKey = supplier.getCacheKey(queueId);
        //from db
        if (cache == null) {
            RLock lock = getRedisClient().getRedissonClient().getLock(cacheKey + ":lock:" + queueId);
            try {
                lock.lock(3, TimeUnit.SECONDS);
                cache = supplier.getFromCache(queueId, page, getRedisClient());
                if (cache != null && cache.size() == page.getSize()) {
                    return cache;
                }
                if (cache == null) {
                    Pageable pageable = PageRequest.of(0, cacheTotal);
                    List<T> fromDB = supplier.getFromDB(queueId, Instant.now(), pageable);
                    if (!fromDB.isEmpty()) {
                        Map<String, Double> items = new HashMap<>();
                        for (T item : fromDB) {
                            items.put(item.getCacheId(queueId).toString(), (double) supplier.getItemScore(item));
                        }
                        getRedisClient().addZSetValueToCache(
                                cacheKey,
                                items,
                                DEFAULT_TTL
                        );
                        if (fromDB.size() >= page.getSize()) {
                            page.setToken(supplier.getItemScore(fromDB.get(page.getSize() - 1)));
                        }
                    }

                    return fromDB
                            .stream()
                            .limit(page.getSize())
                            .map(p -> p.getCacheId(queueId))
                            .collect(Collectors.toList());
                }
            } finally {
                lock.unlock();
            }
        }

        //from cache and db
        long size = getRedisClient().getZSetSize(cacheKey);
        if (size >= cacheTotal && cache.size() < page.getSize()) {
            int sizeFromDB = page.getSize() - cache.size();
            Pageable pageable = PageRequest.of(0, sizeFromDB);
            List<T> fromDB = supplier.getFromDB(queueId, page.getEnd(), pageable);
            if (fromDB.size() == sizeFromDB) {
                page.setToken(supplier.getItemScore((fromDB.get(fromDB.size() - 1))));
            }
            cache.addAll(fromDB.stream().map(p -> p.getCacheId(queueId)).collect(Collectors.toList()));
            return cache;
        }

        return cache;
    }

    protected final List<T> getModelsFromQueue(QUEUE_ID queueId, AbstractModelSupplier<T, ID, QUEUE_ID> supplier, PageArgument page) {
        List<Long> cache = supplier.getFromCache(queueId, page, getRedisClient());
        //from cache
        if (cache != null && cache.size() == page.getSize()) {
            List<ID> ids = cache.stream()
                    .map(p -> supplier.convertToId(queueId, p))
                    .collect(Collectors.toList());
            return get(ids);
        }
        String cacheKey = supplier.getCacheKey(queueId);
        int cacheTotal = supplier.getCacheTotal();
        //from db
        if (cache == null) {
            RLock lock = getRedisClient().getRedissonClient().getLock(cacheKey + ":lock:" + queueId);
            try {
                lock.lock(3, TimeUnit.SECONDS);
                cache = supplier.getFromCache(queueId, page, getRedisClient());
                if (cache != null && cache.size() == page.getSize()) {
                    List<ID> ids = cache.stream()
                            .map(p -> supplier.convertToId(queueId, p))
                            .collect(Collectors.toList());
                    return get(ids);
                }
                Pageable pageable = PageRequest.of(0, cacheTotal);
                List<T> fromDB = supplier.getFromDB(queueId, Instant.now(), pageable);
                Map<String, Double> items = new HashMap<>();
                for (T item : fromDB) {
                    items.put(item.getCacheId(queueId).toString(), (double) supplier.getItemScore(item));
                }
                getRedisClient().addZSetValueToCache(
                        cacheKey,
                        items,
                        DEFAULT_TTL
                );
                if (fromDB.size() >= page.getSize()) {
                    page.setToken(supplier.getItemScore(fromDB.get(page.getSize() - 1)));
                }
                return fromDB
                        .stream()
                        .limit(page.getSize())
                        .collect(Collectors.toList());
            } finally {
                lock.unlock();
            }
        }

        //from cache and db
        List<ID> ids = cache.stream()
                .map(cacheId -> supplier.convertToId(queueId, cacheId))
                .collect(Collectors.toList());
        long size = getRedisClient().getZSetSize(cacheKey);
        if (size >= cacheTotal && cache.size() < page.getSize()) {
            List<T> ret;
            if (ids.isEmpty()) {
                ret = new ArrayList<>();
            } else {
                ret = get(ids);
            }
            int sizeFromDB = page.getSize() - cache.size();
            Pageable pageable = PageRequest.of(0, sizeFromDB);
            List<T> fromDB = supplier.getFromDB(queueId, page.getEnd(), pageable);
            if (fromDB.size() == sizeFromDB) {
                page.setToken(supplier.getItemScore(fromDB.get(fromDB.size() - 1)));
            }
            ret.addAll(fromDB);
            return ret;
        }

        return get(ids);
    }
}

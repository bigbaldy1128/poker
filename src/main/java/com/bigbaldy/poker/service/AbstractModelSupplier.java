package com.bigbaldy.poker.service;

import com.bigbaldy.poker.lib.RedisClient;
import com.bigbaldy.poker.model.BaseModel;
import com.bigbaldy.poker.model.id.ICacheId;

/**
 * @author wangjinzhao on 2020/5/15
 */
public abstract class AbstractModelSupplier<T extends BaseModel<ID> & ICacheId<QUEUE_ID>, ID, QUEUE_ID> extends AbstractIdSupplier<T, ID, QUEUE_ID> {
    public AbstractModelSupplier(RedisClient redisClient, AbstractService<T, ID> service) {
        super(redisClient, service);
    }

    public abstract ID convertToId(QUEUE_ID queueId, Long cacheId);
}

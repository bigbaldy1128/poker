package com.bigbaldy.poker.model.id;

/**
 * @author wangjinzhao on 2020/5/15
 */
public interface ICacheId<QUEUE_ID> {
    Long getCacheId(QUEUE_ID id);
}

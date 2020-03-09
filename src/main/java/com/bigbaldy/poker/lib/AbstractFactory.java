package com.bigbaldy.poker.lib;

import com.bigbaldy.poker.util.SpringContextHolder;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * @author wangjinzhao on 2020/3/9
 */
@Slf4j
public abstract class AbstractFactory<K, V extends IFactoryItem<K>> {
    private final static Map<String, AbstractFactory> factoryMap = new HashMap<>();
    private Map<K, V> itemHashMap = new HashMap<>();

    protected AbstractFactory() {
        String methodName = Thread.currentThread().getStackTrace()[3].getMethodName();
        if (methodName.equals("newInstance0")) {
            return;
        }
        String clazzName = this.getClass().getName();
        throw new RuntimeException("Call constructor is not allowed for class " + clazzName);
    }

    @SuppressWarnings("unchecked")
    public static <K, V extends IFactoryItem<K>, T extends AbstractFactory<K, V>> T getInstance(Class<T> clazz) {
        String clazzName = clazz.getName();
        if (!factoryMap.containsKey(clazzName)) {
            synchronized (factoryMap) {
                if (!factoryMap.containsKey(clazzName)) {
                    try {
                        T instance = clazz.newInstance();
                        Type type = clazz.getGenericSuperclass();
                        Type item = ((ParameterizedType) type).getActualTypeArguments()[1];
                        instance.scan((Class<V>) Class.forName(item.getTypeName()));
                        factoryMap.put(clazzName, instance);
                    } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                        log.error(String.format("failed to initialize %s", clazzName), e);
                    }
                }
            }
        }
        return (T) factoryMap.get(clazzName);
    }

    public V get(K key) {
        return itemHashMap.get(key);
    }

    void scan(Class<V> type) {
        for (V item : SpringContextHolder.getBeans(type)) {
            itemHashMap.put(item.getKey(), item);
        }
    }
}

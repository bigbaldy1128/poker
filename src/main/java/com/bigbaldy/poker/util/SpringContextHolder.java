package com.bigbaldy.poker.util;

import org.apache.commons.text.WordUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

/**
 * @author wangjinzhao on 2019/5/30
 */
@Component
public class SpringContextHolder implements ApplicationContextAware {
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringContextHolder.applicationContext = applicationContext;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getBean(String name) {
        return (T) applicationContext.getBean(name);
    }

    public static <T> T getBean(Class<T> clazz) {
        return applicationContext.getBean(clazz);
    }

    public static <T> T getPreciseBean(Class<T> clazz) {
        Map<String, T> map = applicationContext.getBeansOfType(clazz);
        return map.get(WordUtils.uncapitalize(clazz.getSimpleName()));
    }

    public static <T> Collection<T> getBeans(Class<T> clazz) {
        return applicationContext.getBeansOfType(clazz).values();
    }
}

package com.bigbaldy.poker.config;

import com.bigbaldy.poker.lib.RedisClient;
import com.bigbaldy.poker.lib.ScriptLoader;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.redisson.config.ReadMode;
import org.redisson.config.SubscriptionMode;
import org.redisson.config.TransportMode;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

//@Configuration
//@ConfigurationProperties(prefix = "redis")
//@Import(ScriptLoader.class)
//@Getter
//@Setter
public class RedisConfiguration implements InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(RedisConfiguration.class);

    private final ConfigurableApplicationContext applicationContext;
    private final ScriptLoader scriptLoader;

    private List<Redis> config;

    public RedisConfiguration(ConfigurableApplicationContext applicationContext,
                              ScriptLoader scriptLoader) {
        this.applicationContext = applicationContext;
        this.scriptLoader = scriptLoader;
    }

    private RedissonClient redissonClient(Redis redis) {
        List<String> endpointList = parseRedissonEndpoints(redis);
        if (endpointList.size() == 1) {
            Config config = new Config().setCodec(StringCodec.INSTANCE).setTransportMode(TransportMode.NIO);
            config.useSingleServer().setAddress(endpointList.get(0));
            return Redisson.create(config);
        } else {
            Config config = new Config().setCodec(StringCodec.INSTANCE).setTransportMode(TransportMode.NIO);
            config
                    .useClusterServers()
                    .setScanInterval(2000)
                    .setReadMode(ReadMode.MASTER)
                    .setSubscriptionMode(SubscriptionMode.SLAVE)
                    .setPingConnectionInterval(10000)//10秒
                    .setSubscriptionConnectionPoolSize(64)
                    .setSubscriptionsPerConnection(50)
                    .addNodeAddress(endpointList.toArray(new String[0]));
            return Redisson.create(config);
        }
    }

    private List<String> parseRedissonEndpoints(Redis redis) {
        return Arrays.stream(redis.endPoints).map(p -> "redis://" + p).collect(Collectors.toList());
    }

    private RedisTemplate<String, String> getRedisTemplate(RedissonClient redissonClient) {
        final RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(new RedissonConnectionFactory(redissonClient));
        template.setEnableDefaultSerializer(true);
        template.setDefaultSerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        BeanDefinitionRegistry beanDefinitionRegistry = (BeanDefinitionRegistry) applicationContext.getBeanFactory();
        for (Redis redis : config) {
            BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(RedisClient.class, () -> {
                RedissonClient redissonClient = redissonClient(redis);
                RedisTemplate<String, String> redisTemplate = getRedisTemplate(redissonClient);
                return new RedisClient(redisTemplate, redissonClient, scriptLoader);
            });
            BeanDefinition BeanDefinition = beanDefinitionBuilder.getRawBeanDefinition();
            for (String name : redis.getNames()) {
                String beanName = name + "RedisClient";
                beanDefinitionRegistry.registerBeanDefinition(beanName, BeanDefinition);
                logger.info("register {}", beanName);
            }
        }
    }

    @ToString
    @Getter
    @Setter
    public static class Redis {
        private String[] names;
        private String[] endPoints;
    }

}

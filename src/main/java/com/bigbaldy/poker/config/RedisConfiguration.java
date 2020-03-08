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
import org.redisson.config.TransportMode;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@ConfigurationProperties(prefix = "redis")
@Import(ScriptLoader.class)
@Getter
@Setter
public class RedisConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(RedisConfiguration.class);

    private Main main;

    @Bean(name = "mainRedissonClient")
    @ConditionalOnProperty(name = "redis.main.end-point")
    public RedissonClient mainRedissonClient() {
        return redissonClient(main);
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
                    .addNodeAddress(endpointList.toArray(new String[0]));
            return Redisson.create(config);
        }
    }

    private List<String> parseRedissonEndpoints(Redis redis) {
        return Arrays.stream(redis.endPoint).map(p -> "redis://" + p).collect(Collectors.toList());
    }

    @Bean(name = "mainRedisClient")
    @ConditionalOnProperty(name = "redis.main.end-point")
    public RedisClient mainRedisClient(
            @Qualifier("mainRedisTemplate") RedisTemplate<String, String> write,
            @Qualifier("mainRedissonClient") RedissonClient redissonClient,
            @Autowired ScriptLoader scriptLoader) {
        return new RedisClient(write, redissonClient, scriptLoader);
    }

    @Bean("mainRedisTemplate")
    @ConditionalOnProperty(name = "redis.main.end-point")
    public RedisTemplate<String, String> userRedissonTemplate(@Qualifier("mainRedissonClient") RedissonClient redissonClient) {
        return getRedisTemplate(redissonClient);
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
        return template;
    }

    public static class Main extends Redis {

    }

    @ToString
    @Getter
    @Setter
    public abstract static class Redis {

        protected String[] endPoint;

        @Min(0)
        @Max(12)
        protected int database;

    }

}

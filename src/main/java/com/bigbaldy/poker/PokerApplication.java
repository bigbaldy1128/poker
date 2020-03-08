package com.bigbaldy.poker;

import com.spring4all.swagger.EnableSwagger2Doc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;

@EnableSwagger2Doc
@SpringBootApplication(scanBasePackages = "com.bigbaldy", exclude = RedisAutoConfiguration.class)
public class PokerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PokerApplication.class, args);
    }

}

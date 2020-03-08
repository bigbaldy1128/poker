package com.bigbaldy.poker.config;

import com.bigbaldy.poker.util.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JsonConfiguration {

    @Bean("customizedObjectMapper")
    @Primary
    public ObjectMapper customizedObjectMapper() {
        return JsonUtil.getDefaultObjectMapper();
    }

}

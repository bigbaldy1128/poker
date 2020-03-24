package com.bigbaldy.poker.util;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

public class JsonUtil {

    private static final Logger logger = LoggerFactory.getLogger(JsonUtil.class);
    private static final Object lock = new Object();
    private static ObjectMapper objectMapper = getDefaultObjectMapper();

    /**
     * 判断对象是否为合法JSON字符串
     *
     * @return boolean
     */
    public static boolean mayBeJson(Object object) {
        if (object == null
                || !String.class.isAssignableFrom(object.getClass())) {
            return false;
        }
        String string = (String) object;
        if (string.isEmpty()) {
            return false;
        }
        char head = string.charAt(0);
        return head == '[' || head == '{';
    }

    /**
     * 判断对象是否为合法JSON Object的字符串
     *
     * @return boolean
     */
    public static boolean mayBeJsonObject(Object object) {
        if (object == null
                || !String.class.isAssignableFrom(object.getClass())) {
            return false;
        }
        String string = (String) object;
        if (string.isEmpty()) {
            return false;
        }
        char head = string.charAt(0);
        return head == '{';
    }

    /**
     * 判断对象是否为合法JSON Array的字符串
     *
     * @return boolean
     */
    public static boolean mayBeJsonArray(Object object) {
        if (object == null
                || !String.class.isAssignableFrom(object.getClass())) {
            return false;
        }
        String string = (String) object;
        if (string.isEmpty()) {
            return false;
        }
        char head = string.charAt(0);
        return head == '[';
    }

    /**
     * 将JSON串转换为对象
     *
     * @param json  JSON串
     * @param clazz clazz 指定的对象类型
     */
    public static <T> Optional<T> toObject(@NotNull String json, Class<T> clazz) {
        try {
            return Optional.of(getDefaultObjectMapper().readValue(json, clazz));
        } catch (IOException e) {
            logger.error("failed to parse json string: {} to {}.", json, clazz, e);
            return Optional.empty();
        }
    }

    public static <T> List<T> toObjects(@NotNull List<String> jsons, Class<T> clazz) {
        List<T> result = new ArrayList<>();
        for (String json : jsons) {
            toObject(json, clazz).ifPresent(result::add);
        }
        return result;
    }

    /**
     * 将JSON串转换为对象
     *
     * @param json      JSON串
     * @param reference clazz 指定的对象类型
     */
    public static <T> Optional<T> toObject(@NotNull String json, TypeReference<T> reference) {
        try {
            return Optional.of(objectMapper.readValue(json, reference));
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("failed to parse json string: {} to {}.", json, reference, e);
            return Optional.empty();
        }
    }


    /**
     * 将JSON串转换为对象
     *
     * @param json  JSON串
     * @param clazz clazz 指定的对象类型 or null if got a exception
     */
    public static <T> Optional<T> toObject(byte[] json, Class<T> clazz) {
        try {
            return Optional.of(getDefaultObjectMapper().readValue(json, clazz));
        } catch (IOException e) {
            logger.error("failed to parse json string: {} to {}", json, clazz, e);
            return Optional.empty();
        }
    }

    /**
     * 将对象转换为JSON串
     *
     * @return String or null if got a exception
     */
    public static Optional<String> toJson(Object object) {
        try {
            return Optional.of(getDefaultObjectMapper().writeValueAsString(object));
        } catch (JsonProcessingException e) {
            logger.error("failed to convert object: {} to json，{}", object, e);
            return Optional.empty();
        }
    }

    public static <T> List<String> toJsons(List<T> objectList) {
        List<String> result = new ArrayList<>();
        for (T object : objectList) {
            toJson(object).ifPresent(result::add);
        }
        return result;
    }

    public static List<String> getDataFromJsonArray(String targetJson, String fieldName) {
        List<String> list = new ArrayList<>();
        if (StringUtils.isEmpty(targetJson) || StringUtils.isEmpty(fieldName)) {
            return list;
        }
        JsonNode jn;
        try {
            jn = JsonUtil.getDefaultObjectMapper().readTree(targetJson);
            if (null != jn && jn.isArray()) {
                Iterator<JsonNode> iterator = jn.elements();
                while (iterator.hasNext()) {
                    JsonNode item = iterator.next();
                    if (null != item && null != item.get(fieldName)) {
                        String questionTitle = item.get(fieldName).asText();
                        if (!StringUtils.isEmpty(questionTitle)) {
                            list.add(questionTitle);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("failed to parsing targetJson:{}, filedName:{}", targetJson, fieldName, e);
        }
        return list;
    }

    public static ObjectMapper getDefaultObjectMapper() {
        if (objectMapper == null) {
            synchronized (lock) {
                if (objectMapper == null) {
                    objectMapper = new ObjectMapper();
                    objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
                    objectMapper.setSerializationInclusion(Include.NON_NULL);
                    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
                    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                    objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
                    SimpleModule customModule = new SimpleModule();
                    customModule.addSerializer(Instant.class, new CustomInstantSerializer());
                    customModule.addDeserializer(Instant.class, new CustomInstantDeserializer());
                    objectMapper.findAndRegisterModules();
                    objectMapper.registerModule(customModule);
                }
            }
        }

        return objectMapper;
    }


    public static Optional<String> merge(@NotNull String... json) {

        Map<String, Object> merged = new HashMap<>();

        for (int i = 0; i < json.length; i++) {
            Optional<Map> tmp = toObject(json[i], Map.class);
            tmp.ifPresent(merged::putAll);
        }
        return toJson(merged);
    }


    final static class CustomInstantSerializer extends JsonSerializer<Instant> {

        @Override
        public void serialize(Instant o, JsonGenerator jsonGenerator,
                              SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeObject(o.toEpochMilli());
        }
    }

    static class CustomInstantDeserializer extends JsonDeserializer<Instant> {

        @Override
        public Instant deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
                throws IOException {
            long longValue = jsonParser.getValueAsLong();
            return Instant.ofEpochMilli(longValue);
        }
    }
}
package com.bigbaldy.poker.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.NoHttpResponseException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author wangjinzhao on 2019/5/30
 */
@Slf4j
public class RestHttpClientUtil {
    private static RestTemplate restTemplate;

    static {
        HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory(httpClient());
        httpRequestFactory.setConnectionRequestTimeout(3000);
        httpRequestFactory.setConnectTimeout(3000);
        httpRequestFactory.setReadTimeout(3000);
        restTemplate = new RestTemplate(httpRequestFactory);
        //find and replace Jackson message converter with our own
        for (int i = 0; i < restTemplate.getMessageConverters().size(); i++) {
            final HttpMessageConverter<?> httpMessageConverter = restTemplate.getMessageConverters()
                    .get(i);
            if (httpMessageConverter instanceof MappingJackson2HttpMessageConverter) {
                MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
                converter.setObjectMapper(JsonUtil.getDefaultObjectMapper());
                MediaType[] mediaTypes = new MediaType[]{
                        MediaType.APPLICATION_JSON,
                        MediaType.APPLICATION_OCTET_STREAM,
                        MediaType.TEXT_HTML,
                        MediaType.TEXT_PLAIN,
                        MediaType.TEXT_XML,
                        MediaType.APPLICATION_STREAM_JSON,
                        MediaType.APPLICATION_ATOM_XML,
                        MediaType.APPLICATION_FORM_URLENCODED,
                        MediaType.APPLICATION_JSON, //MediaType.APPLICATION_JSON_UTF8,
                        MediaType.APPLICATION_PDF,
                };
                converter.setSupportedMediaTypes(Arrays.asList(mediaTypes));

                restTemplate.getMessageConverters().set(i, converter);
            }
        }
    }

    private static CloseableHttpClient httpClient() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(5000);
        connectionManager.setDefaultMaxPerRoute(300);
        return HttpClientBuilder
                .create()
                .setConnectionManager(connectionManager)
                .setRetryHandler((exception, executionCount, context) -> {
                    if (executionCount > 2) {
                        log.warn("Maximum tries reached for client http pool ");
                        return false;
                    }

                    if (exception instanceof NoHttpResponseException) {
                        log.warn("NoHttpResponseException on " + executionCount + " call");
                        return true;
                    }
                    return false;
                })
                .build();
    }

    public static RestTemplate getRestTemplate() {
        return restTemplate;
    }

    private static HttpEntity<String> getHttpEntity(String jsonBody,
                                                    Map<String, String> headers) {
        HttpHeaders httpHeaders = convertToHttpHeaders(headers, null);
        return new HttpEntity<>(jsonBody, httpHeaders);
    }

    public static <T> T post(String url, Map<String, Object> params,
                             Map<String, String> headers, Class<T> clz) {
        return getRestTemplate()
                .postForObject(url, getHttpEntity(JsonUtil.toJson(params).get(), headers), clz);
    }

    public static <T> T post(String url, String jsonBody,
                             Map<String, String> headers, Class<T> clz) {
        return getRestTemplate().postForObject(url, getHttpEntity(jsonBody, headers), clz);
    }

    public static <T> T put(String url, String jsonBody,
                            Map<String, String> headers, Class<T> clz) {
        ResponseEntity<T> responseEntity = getRestTemplate()
                .exchange(url, HttpMethod.PUT, getHttpEntity(jsonBody, headers), clz);

        return responseEntity.getBody();
    }

    public static <T> T delete(String url,
                               Map<String, String> headers, Class<T> clz) {
        ResponseEntity<T> responseEntity = getRestTemplate()
                .exchange(url, HttpMethod.DELETE, getHttpEntity(null, headers), clz);
        return responseEntity.getBody();
    }


    public static <T> T postForm(String url, Map<String, String> params,
                                 Map<String, String> headers, Class<T> clz) {
        HttpHeaders httpHeaders = convertToHttpHeaders(headers, MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, Object> multiValueMap = new LinkedMultiValueMap<>();
        if (params != null) {
            for (Entry<String, String> e : params.entrySet()) {
                multiValueMap.add(e.getKey(), e.getValue());
            }
        }
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(
                multiValueMap, httpHeaders);
        return getRestTemplate().postForObject(url, requestEntity, clz);
    }

    public static <T> T get(String url, Map<String, String> params,
                            Map<String, String> headers, Class<T> clz) {
        HttpHeaders httpHeaders = convertToHttpHeaders(headers, null);
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
        params.forEach(builder::queryParam);
        HttpEntity<Object> httpEntity = new HttpEntity<>(httpHeaders);
        ResponseEntity<T> forEntity = RestHttpClientUtil.getRestTemplate()
                .exchange(builder.toUriString(), HttpMethod.GET, httpEntity, clz);
        return forEntity.getBody();
    }

    private static HttpHeaders convertToHttpHeaders(Map<String, String> headers, MediaType mediaType) {
        HttpHeaders httpHeaders = new HttpHeaders();
        if (mediaType != null) {
            httpHeaders.setContentType(mediaType);
        }
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        if (headers != null) {
            headers.forEach(httpHeaders::add);
        }
        return httpHeaders;
    }

}

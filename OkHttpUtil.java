package org.example;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import okhttp3.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class OkHttpUtil {

    private static final OkHttpClient CLIENT;
    private static final ObjectMapper MAPPER;
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    // ==================== 静态初始化 ====================
    static {
        CLIENT = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(50, 5, TimeUnit.MINUTES))
                .retryOnConnectionFailure(true)
                .build();

        MAPPER = new ObjectMapper();
        MAPPER.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    private OkHttpUtil() {
    }

    // ==================== GET 请求 ====================

    /**
     * GET → 反序列化为指定类型
     */
    public static <T> T get(String url, Class<T> clazz) throws IOException {
        Request request = new Request.Builder().url(url).get().build();
        return execute(request, clazz);
    }

    /**
     * GET → 反序列化为泛型类型 (如 List<User>)
     */
    public static <T> T get(String url, TypeReference<T> typeRef) throws IOException {
        Request request = new Request.Builder().url(url).get().build();
        return execute(request, typeRef);
    }

    /**
     * GET 带自定义 Headers
     */
    public static <T> T get(String url, Map<String, String> headers, Class<T> clazz) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .headers(buildHeaders(headers))
                .get()
                .build();
        return execute(request, clazz);
    }

    // ==================== POST 请求 ====================

    /**
     * POST JSON Body → 反序列化为指定类型
     */
    public static <T> T post(String url, Object body, Class<T> clazz) throws IOException {
        RequestBody requestBody = RequestBody.create(JacksonUtil.writeValue(body), JSON_MEDIA_TYPE);
        Request request = new Request.Builder().url(url).post(requestBody).build();
        return execute(request, clazz);
    }

    /**
     * POST JSON Body → 泛型返回值
     */
    public static <T> T post(String url, Object body, TypeReference<T> typeRef) throws IOException {
        RequestBody requestBody = RequestBody.create(JacksonUtil.writeValue(body), JSON_MEDIA_TYPE);
        Request request = new Request.Builder().url(url).post(requestBody).build();
        return execute(request, typeRef);
    }

    /**
     * POST 表单提交
     */
    public static <T> T postForm(String url, Map<String, String> formParams, Class<T> clazz) throws IOException {
        FormBody.Builder formBuilder = new FormBody.Builder();
        if (formParams != null) {
            formParams.forEach(formBuilder::add);
        }
        Request request = new Request.Builder().url(url).post(formBuilder.build()).build();
        return execute(request, clazz);
    }

    // ==================== PUT / DELETE ====================

    public static <T> T put(String url, Object body, Class<T> clazz) throws IOException {
        RequestBody requestBody = RequestBody.create(JacksonUtil.writeValue(body), JSON_MEDIA_TYPE);
        Request request = new Request.Builder().url(url).put(requestBody).build();
        return execute(request, clazz);
    }

    public static <T> T delete(String url, Class<T> clazz) throws IOException {
        Request request = new Request.Builder().url(url).delete().build();
        return execute(request, clazz);
    }

    // ==================== 获取原始字符串响应 ====================

    public static String getString(String url) throws IOException {
        Request request = new Request.Builder().url(url).get().build();
        return executeString(request);
    }

    public static String postString(String url, Object body) throws IOException {
        RequestBody requestBody = RequestBody.create(JacksonUtil.writeValue(body), JSON_MEDIA_TYPE);
        Request request = new Request.Builder().url(url).post(requestBody).build();
        return executeString(request);
    }

    // ==================== 核心执行方法 ====================

    private static <T> T execute(Request request, Class<T> clazz) throws IOException {
        String json = executeString(request);
        return JacksonUtil.readValue(json, clazz);
    }

    private static <T> T execute(Request request, TypeReference<T> typeRef) throws IOException {
        String json = executeString(request);
        return JacksonUtil.readValue(json, typeRef);
    }

    private static String executeString(Request request) throws IOException {
        // noinspection WithSSRFCheckingInspection
        try (Response response = CLIENT.newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            String bodyStr = responseBody != null ? responseBody.string() : "";

            if (!response.isSuccessful()) {
                throw new IOException(
                        String.format("HTTP %d: %s | Body: %s",
                                response.code(), response.message(), bodyStr));
            }
            return bodyStr;
        }
    }

    public static ObjectMapper getMapper() {
        return MAPPER;
    }

    public static OkHttpClient getClient() {
        return CLIENT;
    }

    // ==================== 内部工具 ====================

    private static Headers buildHeaders(Map<String, String> headerMap) {
        Headers.Builder builder = new Headers.Builder();
        if (headerMap != null) {
            headerMap.forEach(builder::add);
        }
        return builder.build();
    }
}

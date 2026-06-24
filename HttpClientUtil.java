package org.example;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.net.URIBuilder;

import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;


public class HttpClientUtil {


    private final CloseableHttpClient httpClient = HttpClients.createDefault();

    private final JacksonUtil jacksonUtil;

    public HttpClientUtil(JacksonUtil jacksonUtil) {
        this.jacksonUtil = jacksonUtil;
    }

    public String get(String url, Map<String, Object> param) {
        try {
            checkUrl(url);
            HttpGet httpGet = createHttpGetWithParams(url, param);
            HttpClientResponseHandler<String> responseHandler = response -> EntityUtils.toString(response.getEntity());
            // noinspection WithSSRFCheckingInspection
            return httpClient.execute(httpGet, responseHandler);
        } catch (Exception e) {
            throw new RuntimeException("Error executing GET request to " + url, e);
        }

    }

    public String post(String url, Map<String, Object> param) {
        checkUrl(url);
        return this.post(url, JacksonUtil.writeValue(param));
    }

    public String post(String url, String param) {
        try {
            checkUrl(url);
            HttpPost httpPost = new HttpPost(url);
            httpPost.setEntity(new StringEntity(param, ContentType.APPLICATION_JSON));
            HttpClientResponseHandler<String> responseHandler = response -> EntityUtils.toString(response.getEntity());
            // noinspection WithSSRFCheckingInspection
            return httpClient.execute(httpPost, responseHandler);
        } catch (Exception e) {
            throw new RuntimeException("Error executing POST request to " + url, e);
        }
    }

    private HttpGet createHttpGetWithParams(String baseUrl, Map<String, Object> params) {
        try {
            URIBuilder uriBuilder = new URIBuilder(baseUrl);
            if (!params.isEmpty()) {
                params.forEach((key, value) -> {
                    if (value != null) {
                        uriBuilder.addParameter(key, String.valueOf(value));
                    }
                });
            }
            return new HttpGet(uriBuilder.build());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL syntax: " + baseUrl, e);
        }
    }


    private void checkUrl(String url) {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }
    }

    @Deprecated
    private String buildFormDataFromMap(Map<String, Object> data) {
        StringBuilder builder = new StringBuilder();
        data.forEach((key, value) -> {
            if (key == null || value == null) {
                return;
            }
            if (!builder.isEmpty()) {
                builder.append("&");
            }
            builder.append(URLEncoder.encode(key, StandardCharsets.UTF_8));
            builder.append("=");
            builder.append(URLEncoder.encode(value.toString(), StandardCharsets.UTF_8));
        });
        return builder.toString();
    }

}

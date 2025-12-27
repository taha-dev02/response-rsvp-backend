package com.rsvp.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
public class SupabaseClient {

    private final String baseUrl;
    private final String serviceKey;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SupabaseClient(String url, String serviceKey) {
        this.baseUrl = url + "/rest/v1";
        this.serviceKey = serviceKey;
        this.objectMapper = new ObjectMapper();

        this.httpClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request request = original.newBuilder()
                            .header("apikey", serviceKey)
                            .header("Authorization", "Bearer " + serviceKey)
                            .header("Content-Type", "application/json")
                            .header("Prefer", "return=representation")
                            .method(original.method(), original.body())
                            .build();
                    return chain.proceed(request);
                })
                .build();
    }

    public SupabaseQuery from(String table) {
        return new SupabaseQuery(this, table);
    }

    public class SupabaseQuery {
        private final SupabaseClient client;
        private final String table;
        private final StringBuilder queryParams;

        public SupabaseQuery(SupabaseClient client, String table) {
            this.client = client;
            this.table = table;
            this.queryParams = new StringBuilder();
        }

        public SupabaseQuery select(String columns) {
            queryParams.append("select=").append(columns);
            return this;
        }

        public SupabaseQuery eq(String column, Object value) {
            if (queryParams.length() > 0) queryParams.append("&");
            queryParams.append(column).append("=eq.").append(value);
            return this;
        }

        public List<Map<String, Object>> execute() throws IOException {
            String url = client.baseUrl + "/" + table;
            if (queryParams.length() > 0) {
                url += "?" + queryParams.toString();
            }

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = client.httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Supabase request failed: " + response);
                }

                String responseBody = response.body().string();
                return client.objectMapper.readValue(
                        responseBody,
                        new TypeReference<List<Map<String, Object>>>() {}
                );
            }
        }

        public void update(Map<String, Object> data) throws IOException {
            String url = client.baseUrl + "/" + table;
            if (queryParams.length() > 0) {
                url += "?" + queryParams.toString();
            }

            RequestBody body = RequestBody.create(
                    client.objectMapper.writeValueAsString(data),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .patch(body)
                    .build();

            try (Response response = client.httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Supabase update failed: " + response);
                }
            }
        }
    }
}
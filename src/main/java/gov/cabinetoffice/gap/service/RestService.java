package gov.cabinetoffice.gap.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;

public class RestService {

    public static final String BACKEND_API_URL = System.getenv("BACKEND_API_URL");

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public static final Gson gson = new GsonBuilder().registerTypeAdapter(Instant.class,
            (JsonDeserializer<Instant>) (json, type, jsonDeserializationContext) -> OffsetDateTime
                    .parse(json.getAsJsonPrimitive().getAsString()).toInstant())
            .create();

    private static final Logger logger = LoggerFactory.getLogger(RestService.class);

    private static final String ADMIN_API_SECRET = System.getenv("ADMIN_API_SECRET");

    public static <T> T sendGetRequest(Map<String, String> params, String endpoint, Class<T> clazz) throws Exception {

        HttpUrl.Builder httpBuilder = HttpUrl.get(BACKEND_API_URL + endpoint).newBuilder();
        if (params != null) {
            for (Map.Entry<String, String> param : params.entrySet()) {
                httpBuilder.addQueryParameter(param.getKey(), param.getValue());
            }
        }

        final Request request = defaultRequestBuilder().url(httpBuilder.build()).build();

        try (Response response = getClient().newCall(request).execute()) {
            if (response.isSuccessful()) {
                return gson.fromJson(response.body().string(), clazz);
            }
            else {
                throw new RuntimeException();
            }
        }
    }

    public static <T> void sendPostRequest(T requestBodyDTO, String endpoint) throws Exception {

        final RequestBody body = RequestBody.create(gson.toJson(requestBodyDTO), JSON);

        executePost(body, endpoint);
    }

    public static void sendPostRequest(String body, String endpoint) throws Exception {

        final RequestBody requestBody = RequestBody.create(body, JSON);

        executePost(requestBody, endpoint);
    }

    public static void executePost(RequestBody body, String endpoint) throws Exception {
        final Request request = defaultRequestBuilder().url(BACKEND_API_URL + endpoint).post(body).build();

        try (Response response = getClient().newCall(request).execute()) {
            if (response.isSuccessful()) {
                logger.info("Successfully posted to " + endpoint);
            }
            else {
                throw new RuntimeException("Error occured while posting to " + endpoint);
            }
        }
    }

    public static <T> void sendPatchRequest(T requestBodyDTO, String endpoint) throws Exception {

        final RequestBody body = RequestBody.create(gson.toJson(requestBodyDTO), JSON);

        final Request request = defaultRequestBuilder().url(BACKEND_API_URL + endpoint).patch(body).build();

        try (Response response = getClient().newCall(request).execute()) {
            if (response.isSuccessful()) {
                logger.info("Successfully patched to " + endpoint);
            }
            else {
                throw new RuntimeException("Error occured while patching to " + endpoint);
            }
        }
    }

    public static OkHttpClient getClient() {
        return new OkHttpClient();
    }

    /**
     * Adds ADMIN_API_SECRET as an Authorization header to every outbound REST call
     */
    public static Request.Builder defaultRequestBuilder() {
        return new Request.Builder().addHeader("Authorization", ADMIN_API_SECRET);
    }

}

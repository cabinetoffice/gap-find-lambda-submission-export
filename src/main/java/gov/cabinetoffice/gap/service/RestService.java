package gov.cabinetoffice.gap.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Map;

public class RestService {

    public static final String BACKEND_API_URL = System.getenv("BACKEND_API_URL");

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public static final Gson gson = new GsonBuilder().registerTypeAdapter(Instant.class,
                    (JsonDeserializer<Instant>) (json, type, jsonDeserializationContext) -> OffsetDateTime
                            .parse(json.getAsJsonPrimitive().getAsString()).toInstant())
            .registerTypeAdapter(ZonedDateTime.class,
                    (JsonDeserializer<ZonedDateTime>) (json, type, jsonDeserializationContext) -> OffsetDateTime
                            .parse(json.getAsJsonPrimitive().getAsString()).toZonedDateTime())
            .create();

    private static final Logger logger = LoggerFactory.getLogger(RestService.class);

    private static final String ADMIN_API_SECRET = System.getenv("ADMIN_API_SECRET");

    private static final String PUBLIC_KEY = System.getenv("PUBLIC_KEY");

    public static <T> T sendGetRequest(OkHttpClient restClient, Map<String, String> params, String endpoint, Class<T> clazz) throws Exception {

        HttpUrl.Builder httpBuilder = HttpUrl.get(BACKEND_API_URL + endpoint).newBuilder();
        if (params != null) {
            for (Map.Entry<String, String> param : params.entrySet()) {
                httpBuilder.addQueryParameter(param.getKey(), param.getValue());
            }
        }

        final Request request = defaultRequestBuilder().url(httpBuilder.build()).build();

        try (Response response = restClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                logger.info("Successfully fetched from " + endpoint);
                return gson.fromJson(response.body().string(), clazz);
            } else {
                throw new RuntimeException();
            }
        }
    }

    public static <T> void sendPostRequest(OkHttpClient restClient, T requestBodyDTO, String endpoint) throws Exception {

        final RequestBody body = RequestBody.create(gson.toJson(requestBodyDTO), JSON);

        executePost(restClient, body, endpoint);
    }

    public static void sendPostRequest(OkHttpClient restClient, String body, String endpoint) throws Exception {

        final RequestBody requestBody = RequestBody.create(body, JSON);

        executePost(restClient, requestBody, endpoint);
    }

    public static void executePost(OkHttpClient restClient, RequestBody body, String endpoint) throws Exception {
        final Request request = defaultRequestBuilder().url(BACKEND_API_URL + endpoint).post(body).build();

        try (Response response = restClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                logger.info("Successfully posted to " + endpoint);
            } else {
                throw new RuntimeException("Error occured while posting to " + endpoint);
            }
        }
    }

    public static <T> void sendPatchRequest(OkHttpClient restClient, T requestBodyDTO, String endpoint) throws Exception {

        final RequestBody body = RequestBody.create(gson.toJson(requestBodyDTO), JSON);

        final Request request = defaultRequestBuilder().url(BACKEND_API_URL + endpoint).patch(body).build();

        try (Response response = restClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                logger.info("Successfully patched to " + endpoint);
            } else {
                throw new RuntimeException("Error occured while patching to " + endpoint);
            }
        }
    }

    /**
     * Adds encrypted ADMIN_API_SECRET as an Authorization header to every outbound REST call
     */
    public static Request.Builder defaultRequestBuilder() {
        final String encryptedSecret = encrypt(ADMIN_API_SECRET, PUBLIC_KEY);
        logger.info("Secret successfully encrypted");

        return new Request.Builder().addHeader("Authorization", encryptedSecret);
    }

    public static String encrypt(String secret, String publicKey) {

        try {
            final byte[] publicKeyBytes = Base64.getDecoder().decode(publicKey);
            final X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
            final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            final PublicKey rsaPublicKey = keyFactory.generatePublic(keySpec);
            final Cipher encryptCipher = Cipher.getInstance("RSA");

            encryptCipher.init(Cipher.ENCRYPT_MODE, rsaPublicKey);
            final byte[] cipherText = encryptCipher.doFinal(secret.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(cipherText);
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while encrypting the secret " + e);
        }
    }

}

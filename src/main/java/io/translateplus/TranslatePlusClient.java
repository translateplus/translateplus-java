package io.translateplus;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import okhttp3.MediaType;
import okhttp3.RequestBody;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Official Java client for TranslatePlus API.
 *
 * <p>This client provides a simple and intuitive interface to all TranslatePlus
 * translation endpoints including text, batch, HTML, email, subtitles, and i18n translation.
 *
 * <p>Example:
 * <pre>{@code
 * TranslatePlusClient client = new TranslatePlusClient.Builder()
 *     .apiKey("your-api-key")
 *     .build();
 *
 * Map<String, Object> result = client.translate(
 *     "Hello, world!",
 *     "en",
 *     "fr"
 * );
 * System.out.println(result.get("translations"));
 * }</pre>
 */
public class TranslatePlusClient {
    private static final String DEFAULT_BASE_URL = "https://api.translateplus.io";
    private static final int DEFAULT_TIMEOUT = 30;
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final int DEFAULT_MAX_CONCURRENT = 5;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final String apiKey;
    private final String baseUrl;
    private final int timeout;
    private final int maxRetries;
    private final int maxConcurrent;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final Semaphore semaphore;

    private TranslatePlusClient(Builder builder) {
        if (builder.apiKey == null || builder.apiKey.isEmpty()) {
            throw new TranslatePlusValidationError("API key is required");
        }

        this.apiKey = builder.apiKey;
        this.baseUrl = builder.baseUrl != null ? builder.baseUrl.replaceAll("/$", "") : DEFAULT_BASE_URL;
        this.timeout = builder.timeout > 0 ? builder.timeout : DEFAULT_TIMEOUT;
        this.maxRetries = builder.maxRetries > 0 ? builder.maxRetries : DEFAULT_MAX_RETRIES;
        this.maxConcurrent = builder.maxConcurrent > 0 ? builder.maxConcurrent : DEFAULT_MAX_CONCURRENT;
        this.semaphore = new Semaphore(this.maxConcurrent);

        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(this.timeout, TimeUnit.SECONDS)
                .readTimeout(this.timeout, TimeUnit.SECONDS)
                .writeTimeout(this.timeout, TimeUnit.SECONDS)
                .build();

        this.gson = new GsonBuilder().create();
    }

    /**
     * Make an HTTP request to the API.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> makeRequest(
            String method,
            String endpoint,
            Map<String, Object> data,
            Map<String, File> files,
            Map<String, String> params
    ) throws TranslatePlusException {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TranslatePlusAPIError("Request interrupted", null);
        }

        try {
            String url = baseUrl + "/" + endpoint.replaceAll("^/", "");
            if (params != null && !params.isEmpty()) {
                HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
                for (Map.Entry<String, String> param : params.entrySet()) {
                    urlBuilder.addQueryParameter(param.getKey(), param.getValue());
                }
                url = urlBuilder.build().toString();
            }

            Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .addHeader("X-API-KEY", apiKey)
                    .addHeader("User-Agent", "translateplus-java/1.0.0");

            RequestBody requestBody = null;

            if (files != null && !files.isEmpty()) {
                // Multipart form data for file uploads
                MultipartBody.Builder multipartBuilder = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM);

                if (data != null) {
                    for (Map.Entry<String, Object> entry : data.entrySet()) {
                        multipartBuilder.addFormDataPart(entry.getKey(), String.valueOf(entry.getValue()));
                    }
                }

                for (Map.Entry<String, File> entry : files.entrySet()) {
                    File file = entry.getValue();
                    if (!file.exists()) {
                        throw new TranslatePlusValidationError("File not found: " + file.getPath());
                    }
                    multipartBuilder.addFormDataPart(
                            entry.getKey(),
                            file.getName(),
                            RequestBody.create(file, MediaType.parse("application/octet-stream"))
                    );
                }

                requestBody = multipartBuilder.build();
            } else if (data != null) {
                // JSON request
                String json = gson.toJson(data);
                requestBody = RequestBody.create(json, JSON);
            }

            Request request = requestBuilder
                    .method(method, requestBody)
                    .build();

            Exception lastError = null;
            for (int attempt = 0; attempt <= maxRetries; attempt++) {
                try (Response response = httpClient.newCall(request).execute()) {
                    int statusCode = response.code();
                    String body = response.body() != null ? response.body().string() : "{}";

                    if (statusCode >= 200 && statusCode < 300) {
                        if (body == null || body.isEmpty()) {
                            return new HashMap<>();
                        }
                        return gson.fromJson(body, Map.class);
                    }

                    // Parse error response
                    Map<String, Object> errorData = new HashMap<>();
                    String errorMessage = "API request failed with status " + statusCode;

                    try {
                        errorData = gson.fromJson(body, Map.class);
                        if (errorData.containsKey("detail")) {
                            errorMessage = String.valueOf(errorData.get("detail"));
                        }
                    } catch (Exception e) {
                        // Ignore JSON parsing errors
                    }

                    if (statusCode == 401 || statusCode == 403) {
                        throw new TranslatePlusAuthenticationError(errorMessage, statusCode, errorData);
                    } else if (statusCode == 402) {
                        throw new TranslatePlusInsufficientCreditsError(errorMessage, statusCode, errorData);
                    } else if (statusCode == 429) {
                        throw new TranslatePlusRateLimitError(errorMessage, statusCode, errorData);
                    } else {
                        throw new TranslatePlusAPIError(errorMessage, statusCode, errorData);
                    }
                } catch (TranslatePlusAuthenticationError | TranslatePlusInsufficientCreditsError e) {
                    // Don't retry authentication or credit errors
                    throw e;
                } catch (IOException e) {
                    lastError = e;
                    if (attempt < maxRetries) {
                        // Exponential backoff
                        try {
                            Thread.sleep((long) Math.pow(2, attempt) * 1000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new TranslatePlusAPIError("Request interrupted", null);
                        }
                        continue;
                    }
                }
            }

            throw new TranslatePlusAPIError(
                    "Request failed after " + maxRetries + " retries: " +
                            (lastError != null ? lastError.getMessage() : "Unknown error"),
                    null
            );
        } finally {
            semaphore.release();
        }
    }

    /**
     * Translate a single text.
     *
     * @param text   Text to translate
     * @param source Source language code (or "auto" for auto-detection)
     * @param target Target language code
     * @return Translation result
     */
    public Map<String, Object> translate(String text, String source, String target) throws TranslatePlusException {
        Map<String, Object> data = new HashMap<>();
        data.put("text", text);
        data.put("source", source != null ? source : "auto");
        data.put("target", target);
        return makeRequest("POST", "/v2/translate", data, null, null);
    }

    /**
     * Translate multiple texts in a single request.
     *
     * @param texts  Array of texts to translate
     * @param source Source language code (or "auto" for auto-detection)
     * @param target Target language code
     * @return Batch translation result
     */
    public Map<String, Object> translateBatch(List<String> texts, String source, String target) throws TranslatePlusException {
        if (texts == null || texts.isEmpty()) {
            throw new TranslatePlusValidationError("Texts list cannot be empty");
        }
        if (texts.size() > 100) {
            throw new TranslatePlusValidationError("Maximum 100 texts allowed per batch request");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("texts", texts);
        data.put("source", source != null ? source : "auto");
        data.put("target", target);
        return makeRequest("POST", "/v2/translate/batch", data, null, null);
    }

    /**
     * Translate HTML content while preserving all tags and structure.
     *
     * @param html   HTML content to translate
     * @param source Source language code (or "auto" for auto-detection)
     * @param target Target language code
     * @return Translated HTML content
     */
    public Map<String, Object> translateHTML(String html, String source, String target) throws TranslatePlusException {
        Map<String, Object> data = new HashMap<>();
        data.put("html", html);
        data.put("source", source != null ? source : "auto");
        data.put("target", target);
        return makeRequest("POST", "/v2/translate/html", data, null, null);
    }

    /**
     * Translate email subject and HTML body.
     *
     * @param subject   Email subject
     * @param emailBody Email HTML body
     * @param source    Source language code (or "auto" for auto-detection)
     * @param target    Target language code
     * @return Translated email
     */
    public Map<String, Object> translateEmail(String subject, String emailBody, String source, String target) throws TranslatePlusException {
        Map<String, Object> data = new HashMap<>();
        data.put("subject", subject);
        data.put("email_body", emailBody);
        data.put("source", source != null ? source : "auto");
        data.put("target", target);
        return makeRequest("POST", "/v2/translate/email", data, null, null);
    }

    /**
     * Translate subtitle files (SRT or VTT format).
     *
     * @param content Subtitle content
     * @param format  Format ("srt" or "vtt")
     * @param source  Source language code (or "auto" for auto-detection)
     * @param target  Target language code
     * @return Translated subtitle content
     */
    public Map<String, Object> translateSubtitles(String content, String format, String source, String target) throws TranslatePlusException {
        if (!format.equals("srt") && !format.equals("vtt")) {
            throw new TranslatePlusValidationError("Format must be 'srt' or 'vtt'");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("content", content);
        data.put("format", format);
        data.put("source", source != null ? source : "auto");
        data.put("target", target);
        return makeRequest("POST", "/v2/translate/subtitles", data, null, null);
    }

    /**
     * Detect the language of a text.
     *
     * @param text Text to detect language from
     * @return Language detection result
     */
    public Map<String, Object> detectLanguage(String text) throws TranslatePlusException {
        Map<String, Object> data = new HashMap<>();
        data.put("text", text);
        return makeRequest("POST", "/v2/language_detect", data, null, null);
    }

    /**
     * Get list of all supported languages.
     *
     * @return Supported languages
     */
    public Map<String, Object> getSupportedLanguages() throws TranslatePlusException {
        return makeRequest("GET", "/v2/supported_languages", null, null, null);
    }

    /**
     * Get account summary (credits, plan, etc.).
     *
     * @return Account summary
     */
    public Map<String, Object> getAccountSummary() throws TranslatePlusException {
        return makeRequest("GET", "/v2/account/summary", null, null, null);
    }

    /**
     * Create an i18n translation job.
     *
     * @param filePath        Path to the i18n file
     * @param targetLanguages List of target language codes
     * @param sourceLanguage  Source language code (or "auto" for auto-detection)
     * @param webhookUrl      Optional webhook URL for job completion notification
     * @return Job creation result
     */
    public Map<String, Object> createI18nJob(
            File filePath,
            List<String> targetLanguages,
            String sourceLanguage,
            String webhookUrl
    ) throws TranslatePlusException {
        if (filePath == null || !filePath.exists()) {
            throw new TranslatePlusValidationError("File not found: " + (filePath != null ? filePath.getPath() : "null"));
        }
        if (targetLanguages == null || targetLanguages.isEmpty()) {
            throw new TranslatePlusValidationError("targetLanguages must be a non-empty list");
        }

        Map<String, Object> formData = new HashMap<>();
        formData.put("source_language", sourceLanguage != null ? sourceLanguage : "auto");
        formData.put("target_languages", String.join(",", targetLanguages));
        if (webhookUrl != null && !webhookUrl.isEmpty()) {
            formData.put("webhook_url", webhookUrl);
        }

        Map<String, File> files = new HashMap<>();
        files.put("file", filePath);

        return makeRequest("POST", "/v2/i18n/create_job", formData, files, null);
    }

    /**
     * Get i18n job status.
     *
     * @param jobId Job ID
     * @return Job status
     */
    public Map<String, Object> getI18nJobStatus(String jobId) throws TranslatePlusException {
        return makeRequest("GET", "/v2/i18n/job/" + jobId, null, null, null);
    }

    /**
     * List i18n jobs.
     *
     * @param page     Page number (default: 1)
     * @param pageSize Page size (default: 10)
     * @return List of jobs
     */
    public Map<String, Object> listI18nJobs(Integer page, Integer pageSize) throws TranslatePlusException {
        Map<String, String> params = new HashMap<>();
        params.put("page", String.valueOf(page != null ? page : 1));
        params.put("page_size", String.valueOf(pageSize != null ? pageSize : 10));
        return makeRequest("GET", "/v2/i18n/jobs", null, null, params);
    }

    /**
     * Builder for TranslatePlusClient.
     */
    public static class Builder {
        private String apiKey;
        private String baseUrl;
        private int timeout;
        private int maxRetries;
        private int maxConcurrent;

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder timeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder maxConcurrent(int maxConcurrent) {
            this.maxConcurrent = maxConcurrent;
            return this;
        }

        public TranslatePlusClient build() {
            return new TranslatePlusClient(this);
        }
    }
}

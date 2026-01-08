package dev.qingzhou.push.core.channel.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.qingzhou.push.core.api.ChannelIds;
import dev.qingzhou.push.core.channel.AbstractChannel;
import dev.qingzhou.push.core.model.PushConfig;
import dev.qingzhou.push.core.model.PushMessage;
import dev.qingzhou.push.core.model.PushResult;
import dev.qingzhou.push.core.utils.HttpUtils;
import dev.qingzhou.push.core.utils.TokenCache;
import lombok.extern.slf4j.Slf4j;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class FeishuChannel extends AbstractChannel {

    private static final String AUTH_URL = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal";
    private static final String SEND_URL = "https://open.feishu.cn/open-apis/im/v1/messages";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String CACHE_KEY_PREFIX = "feishu_token_";

    @Override
    public String type() {
        return ChannelIds.FEISHU;
    }

    @Override
    public int weight() {
        return 100;
    }

    @Override
    protected void checkConfig(PushConfig config) {
        if (config.getAppKey() == null || config.getAppKey().isBlank()) {
            throw new IllegalArgumentException("Feishu App ID (appKey) cannot be empty");
        }
        if (config.getAppSecret() == null || config.getAppSecret().isBlank()) {
            throw new IllegalArgumentException("Feishu App Secret (appSecret) cannot be empty");
        }
    }

    @Override
    protected PushResult doSend(PushMessage message, PushConfig config) throws Exception {
        String token = getAccessToken(config.getAppKey(), config.getAppSecret());

        // Default receive_id_type is open_id
        // See dev.qingzhou.push.core.model.enums.FeishuReceiveIdType
        String receiveIdType = "open_id";
        if (message.getExtras() != null && message.getExtras().containsKey("receive_id_type")) {
            receiveIdType = message.getExtras().get("receive_id_type").toString();
        }

        String url = SEND_URL + "?receive_id_type=" + receiveIdType;

        Map<String, Object> body = new HashMap<>();
        body.put("receive_id", message.getTarget());

        switch (message.getType()) {
            case TEXT:
                buildText(body, message);
                break;
            case MARKDOWN:
                buildMarkdown(body, message);
                break;
            case TEXT_CARD:
                buildTextCard(body, message);
                break;
            case IMAGE:
                buildImage(body, message);
                break;
            case NEWS:
                buildNews(body, message);
                break;
            default:
                buildText(body, message);
        }

        String jsonBody = MAPPER.writeValueAsString(body);

        return executeRequest(url, jsonBody, token);
    }

    // Reuse HttpClient to avoid resource exhaustion
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private PushResult executeRequest(String url, String jsonBody, String token) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            return parseResult(response.body());
        } catch (Exception e) {
            log.error("Feishu Send Error", e);
            return PushResult.fail("Feishu Send Exception: " + e.getMessage());
        }
    }

    @Override
    protected PushResult parseResult(String rawJson) {
        try {
            JsonNode node = MAPPER.readTree(rawJson);
            if (node.path("code").asInt() == 0) {
                // Success
                String msgId = node.path("data").path("message_id").asText();
                return PushResult.success(msgId, rawJson);
            } else {
                String errorMsg = node.path("msg").asText();
                return PushResult.fail("Feishu Error: " + errorMsg + " (code: " + node.path("code").asInt() + ")", rawJson);
            }
        } catch (JsonProcessingException e) {
            return PushResult.fail("Parse Feishu Response Fail", rawJson);
        }
    }

    // --- Message Builders ---

    private void buildText(Map<String, Object> body, PushMessage message) throws JsonProcessingException {
        body.put("msg_type", "text");
        Map<String, String> content = new HashMap<>();

        StringBuilder text = new StringBuilder();
        if (message.getTitle() != null && !message.getTitle().isBlank()) {
            text.append(message.getTitle()).append("\n");
        }
        text.append(message.getContent());

        content.put("text", text.toString());
        body.put("content", MAPPER.writeValueAsString(content));
    }

    private void buildImage(Map<String, Object> body, PushMessage message) throws JsonProcessingException {
        body.put("msg_type", "image");
        Map<String, Object> extras = message.getExtras();
        Object mediaId = extras != null ? extras.get("media_id") : null;
        if (mediaId == null) {
            throw new IllegalArgumentException("Image message must have 'media_id' in extras (mapped to image_key)");
        }
        Map<String, String> content = new HashMap<>();
        content.put("image_key", mediaId.toString());
        body.put("content", MAPPER.writeValueAsString(content));
    }

    // Markdown in Feishu is best done via Interactive Card
    private void buildMarkdown(Map<String, Object> body, PushMessage message) throws JsonProcessingException {
        body.put("msg_type", "interactive");

        ObjectNode card = MAPPER.createObjectNode();
        ObjectNode config = card.putObject("config");
        config.put("wide_screen_mode", true);

        // Header (Title)
        if (message.getTitle() != null && !message.getTitle().isBlank()) {
            ObjectNode header = card.putObject("header");
            header.put("template", "blue");
            ObjectNode title = header.putObject("title");
            title.put("tag", "plain_text");
            title.put("content", message.getTitle());
        }

        // Body (Markdown Element)
        ArrayNode elements = card.putArray("elements");
        ObjectNode div = elements.addObject();
        div.put("tag", "div");
        ObjectNode text = div.putObject("text");
        text.put("tag", "lark_md");
        text.put("content", message.getContent());

        body.put("content", MAPPER.writeValueAsString(card));
    }

    private void buildTextCard(Map<String, Object> body, PushMessage message) throws JsonProcessingException {
        body.put("msg_type", "interactive");

        ObjectNode card = MAPPER.createObjectNode();
        ObjectNode config = card.putObject("config");
        config.put("wide_screen_mode", true);

        // Header
        if (message.getTitle() != null && !message.getTitle().isBlank()) {
            ObjectNode header = card.putObject("header");
            header.put("template", "wathet"); // Different color for card
            ObjectNode title = header.putObject("title");
            title.put("tag", "plain_text");
            title.put("content", message.getTitle());
        }

        ArrayNode elements = card.putArray("elements");

        // Content
        ObjectNode contentDiv = elements.addObject();
        contentDiv.put("tag", "div");
        ObjectNode text = contentDiv.putObject("text");
        text.put("tag", "lark_md"); // Use markdown for content to support rich text if needed
        text.put("content", message.getContent());

        // Button (Action)
        String url = null;
        if (message.getExtras() != null && message.getExtras().get("url") != null) {
            url = message.getExtras().get("url").toString();
        }
        if (url == null || url.isBlank()) {
             throw new IllegalArgumentException("TextCard message must have 'url' in extras");
        }

        ObjectNode actionDiv = elements.addObject();
        actionDiv.put("tag", "action");
        ArrayNode actions = actionDiv.putArray("actions");
        ObjectNode button = actions.addObject();
        button.put("tag", "button");
        ObjectNode btnText = button.putObject("text");
        btnText.put("tag", "plain_text");
        btnText.put("content", "详情"); // "Details"
        button.put("url", url);
        button.put("type", "primary");

        body.put("content", MAPPER.writeValueAsString(card));
    }

    private void buildNews(Map<String, Object> body, PushMessage message) throws JsonProcessingException {
        // Feishu Card for News (List of articles)
        body.put("msg_type", "interactive");

        ObjectNode card = MAPPER.createObjectNode();
        ObjectNode config = card.putObject("config");
        config.put("wide_screen_mode", true);

        // Header (Use first article title or generic?)
        // PushMessage doesn't have a main title for News, usually just a list of articles.
        // We can use the first article title as header, or no header.

        ArrayNode elements = card.putArray("elements");

        List<PushMessage.Article> articles = message.getArticles();
        if (articles == null || articles.isEmpty()) {
            throw new IllegalArgumentException("News message must have at least one article");
        }

        for (PushMessage.Article article : articles) {
            // Title + Link
            ObjectNode div = elements.addObject();
            div.put("tag", "div");
            ObjectNode text = div.putObject("text");
            text.put("tag", "lark_md");
            // Markdown link: [Title](URL)
            String mdContent = String.format("**[%s](%s)**\n%s",
                    article.getTitle(),
                    article.getUrl(),
                    article.getDescription() != null ? article.getDescription() : "");
            text.put("content", mdContent);

            // Note: Feishu Card doesn't support remote image URL easily in `img` tag (needs image_key).
            // So we skip the image unless we want to do complex uploading.
            // We just render text.

            // Add a separator line
            elements.addObject().put("tag", "hr");
        }

        body.put("content", MAPPER.writeValueAsString(card));
    }


    private String getAccessToken(String appId, String appSecret) {
        String cacheKey = CACHE_KEY_PREFIX + appId;
        String cachedToken = TokenCache.get(cacheKey);
        if (cachedToken != null) {
            return cachedToken;
        }

        Map<String, String> params = new HashMap<>();
        params.put("app_id", appId);
        params.put("app_secret", appSecret);

        String resultJson = HttpUtils.post(AUTH_URL, params);

        try {
            JsonNode node = MAPPER.readTree(resultJson);
            if (node.path("code").asInt() == 0) {
                String token = node.path("tenant_access_token").asText();
                long expireIn = node.path("expire").asLong(7200);
                TokenCache.set(cacheKey, token, expireIn);
                return token;
            } else {
                throw new RuntimeException("Get Feishu Token Fail: " + resultJson);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Parse Feishu Token Response Fail", e);
        }
    }
}

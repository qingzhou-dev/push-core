package dev.qingzhou.push.core.channel.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.qingzhou.push.core.channel.AbstractChannel;
import dev.qingzhou.push.core.model.PushConfig;
import dev.qingzhou.push.core.model.PushMessage;
import dev.qingzhou.push.core.model.PushResult;
import dev.qingzhou.push.core.api.ChannelIds;
import dev.qingzhou.push.core.utils.HttpUtils;
import dev.qingzhou.push.core.utils.TokenCache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WecomChannel extends AbstractChannel {

    private static final String API_URL = "https://qyapi.weixin.qq.com/cgi-bin/message/send?access_token=";

    // 优化1: 全局复用 ObjectMapper (线程安全且性能更好)
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String type() {
        return ChannelIds.WECOM;
    }

    @Override
    public int weight() {
        return 100;
    }

    @Override
    protected void checkConfig(PushConfig config) {
        if (config.getAppKey() == null || config.getAppKey().isBlank()) {
            throw new IllegalArgumentException("WeCom CorpId (appKey) cannot be empty");
        }
        if (config.getAppSecret() == null || config.getAppSecret().isBlank()) {
            throw new IllegalArgumentException("WeCom Secret cannot be empty");
        }
        if (config.getAgentId() == null || config.getAgentId().isBlank()) {
            throw new IllegalArgumentException("WeCom AgentId cannot be empty");
        }
    }

    @Override
    protected PushResult doSend(PushMessage message, PushConfig config) throws Exception {
        String token = getAccessToken(config.getAppKey(), config.getAppSecret());

        // 1. 公共参数
        Map<String, Object> body = new HashMap<>();
        body.put("touser", message.getTarget());
        body.put("agentid", config.getAgentId());

        // 2. 根据类型组装不同的 body
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
                buildNews(body,message);
                break;
            default:
                buildText(body, message);
        }

        // 3. 发送 (代码不变)
        String result = HttpUtils.post(API_URL + token, body);
        return parseResult(result);
    }

    /**
     * 专门负责解析企业微信的响应
     * 成功示例: {"errcode":0,"errmsg":"ok","msgid":"..."}
     * 失败示例: {"errcode":40014,"errmsg":"invalid access_token"}
     */
    @Override

    protected PushResult parseResult(String rawJson) {
        try {
            JsonNode node = MAPPER.readTree(rawJson);

            // 企微成功的标志：errcode 存在且为 0
            if (node.path("errcode").asInt() == 0) {
                return PushResult.success(node.path("msgid").asText(), rawJson);
            } else {
                // 失败情况
                // 优先取 errmsg，如果没有则返回 rawJson
                String errorMsg = node.has("errmsg") ? node.get("errmsg").asText() : rawJson;
                return PushResult.fail("WeCom Error: " + errorMsg, rawJson);
            }
        } catch (JsonProcessingException e) {
            // 如果解析 JSON 报错（比如网络层返回了 502 HTML），直接认定为失败
            return PushResult.fail("Parse Response Fail", rawJson);
        }
    }

    // --- 各种类型的组装逻辑 ---

    private void buildText(Map<String, Object> body, PushMessage message) {
        body.put("msgtype", "text");

        // 拼接 Title 和 Content
        String textContent = message.getContent();
        if (textContent == null) {
            throw new IllegalArgumentException("Text message content cannot be null");
        }
        StringBuilder content = new StringBuilder();
        if (message.getTitle() != null) content.append(message.getTitle()).append("\n");
        content.append(textContent);

        body.put("text", Map.of("content", content.toString()));
    }

    private void buildMarkdown(Map<String, Object> body, PushMessage message) {
        body.put("msgtype", "markdown");
        // 企业微信 Markdown 不支持 Title 字段，直接把 content 发过去
        // 注意：用户需要在 content 里自己写 markdown 语法
        String markdownContent = message.getContent();
        if (markdownContent == null) {
            throw new IllegalArgumentException("Markdown content cannot be null");
        }
        body.put("markdown", Map.of("content", markdownContent));
    }

    private void buildTextCard(Map<String, Object> body, PushMessage message) {
        body.put("msgtype", "textcard");
        Map<String, String> card = new HashMap<>();
        String title = message.getTitle();
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("TextCard title cannot be blank");
        }
        String description = message.getContent();
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("TextCard description cannot be blank");
        }
        card.put("title", title);
        card.put("description", description);
        // 卡片消息必须有跳转链接，如果用户没传 url，可以给个默认的或者抛错
        // 这里假设 PushMessage 里加了一个 url 字段，或者放在 extras 里
        String url = null;
        Map<String, Object> extras = message.getExtras();
        if (extras != null) {
            Object rawUrl = extras.get("url");
            if (rawUrl != null) {
                url = rawUrl.toString();
            }
        }
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("TextCard message must have 'url' in extras");
        }
        card.put("url", url);
        card.put("btntxt", "详情"); // 按钮文字
        body.put("textcard", card);
    }

    private void buildImage(Map<String, Object> body, PushMessage message) {
        body.put("msgtype", "image");
        // 图片需要 media_id，这个必须从 extras 里取
        Map<String, Object> extras = message.getExtras();
        Object mediaId = extras != null ? extras.get("media_id") : null;
        if (mediaId == null) {
            throw new IllegalArgumentException("Image message must have 'media_id' in extras");
        }
        body.put("image", Map.of("media_id", mediaId.toString()));
    }

    /**
     * 构建图文消息 (news)
     * 结构: { "msgtype": "news", "news": { "articles": [ ... ] } }
     */
    private void buildNews(Map<String, Object> body, PushMessage message) {
        body.put("msgtype", "news");

        List<PushMessage.Article> articles = message.getArticles();
        if (articles == null || articles.isEmpty()) {
            throw new IllegalArgumentException("News message must have at least one article");
        }

        // 转换 Article 对象为 Map (为了让 Jackson 序列化时 key 变成小写，或者直接传对象也行)
        // 这里直接传对象，Jackson 会根据 @Data 生成 getter 自动转 JSON
        // 企微字段: title, description, url, picurl
        // 注意：Java 字段是 picUrl，Jackson 默认转成 picUrl，但企微要 picurl (全小写)
        // Article 类字段上加 @JsonProperty("picurl")

        body.put("news", Map.of("articles", articles));
    }

    // 定义一个缓存 Key 的前缀，避免混淆
    private static final String CACHE_KEY_PREFIX = "wecom_token_";

    private String getAccessToken(String corpId, String secret) {
        // 1. 生成唯一 Key (企业ID + Secret 才能确定一个 Token)
        String cacheKey = CACHE_KEY_PREFIX + corpId + "_" + secret;

        // 2. 先查缓存
        String cachedToken = TokenCache.get(cacheKey);
        if (cachedToken != null) {
            return cachedToken; // 命中缓存，直接返回！速度起飞！
        }

        // ---------------- 以下是未命中的逻辑 ----------------

        String baseUrl = "https://qyapi.weixin.qq.com/cgi-bin/gettoken";
        Map<String, Object> params = Map.of(
                "corpid", corpId,
                "corpsecret", secret
        );

        String resultJson = HttpUtils.get(baseUrl, params);

        try {
            JsonNode node = MAPPER.readTree(resultJson);
            if (node.has("access_token")) {
                String token = node.get("access_token").asText();
                // 官方返回的有效期，通常是 7200 (秒)
                long expiresIn = node.path("expires_in").asLong(7200);

                // 3. 存入缓存 (重要！)
                TokenCache.set(cacheKey, token, expiresIn);

                return token;
            } else {
                throw new RuntimeException("Get WeCom Token Fail: " + resultJson);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Parse Token Response Fail", e);
        }
    }
}
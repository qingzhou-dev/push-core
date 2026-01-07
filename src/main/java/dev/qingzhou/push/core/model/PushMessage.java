package dev.qingzhou.push.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.qingzhou.push.core.model.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull; // 引入 Lombok 的 NonNull

import java.util.Collections; // 引入 Collections 处理空 List
import java.util.List;
import java.util.Map;
import java.util.Objects; // 引入 Objects 做原生校验

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushMessage {

    /**
     * 接收目标 (必填)
     */
    private String target;

    @Builder.Default
    private MessageType type = MessageType.TEXT;

    private String title;
    private String content;
    private Map<String, Object> extras;
    private List<Article> articles;

    // ==========================================
    // 静态工厂方法 (增强校验版)
    // ==========================================

    /**
     * 快速构建普通文本
     */
    public static PushMessage text(@NonNull String target, @NonNull String content) {
        // @NonNull 会自动生成 if (target == null) throw new NullPointerException("target is marked non-null but is null");
        return PushMessage.builder()
                .type(MessageType.TEXT)
                .target(target)
                .content(content)
                .build();
    }

    /**
     * 快速构建 Markdown
     */
    public static PushMessage markdown(@NonNull String target, String title, @NonNull String content) {
        return PushMessage.builder()
                .type(MessageType.MARKDOWN)
                .target(target)
                .title(title)
                .content(content)
                .build();
    }

    /**
     * 快速构建文本卡片
     * 注意：jumpUrl 必须校验，否则 Map.of 会炸
     */
    public static PushMessage textCard(@NonNull String target, @NonNull String title, @NonNull String content, @NonNull String jumpUrl) {
        return PushMessage.builder()
                .type(MessageType.TEXT_CARD)
                .target(target)
                .title(title)
                .content(content)
                // 此时 jumpUrl 已经被 @NonNull 保护，Map.of 安全了
                .extras(Map.of("url", jumpUrl))
                .build();
    }

    /**
     * 快速构建图片消息
     */
    public static PushMessage image(@NonNull String target, @NonNull String mediaId) {
        return PushMessage.builder()
                .type(MessageType.IMAGE)
                .target(target)
                // mediaId 必须存在，否则没法发图片
                .extras(Map.of("media_id", mediaId))
                .build();
    }

    /**
     * 快速构建单条图文消息
     */
    public static PushMessage news(@NonNull String target, @NonNull Article article) {
        // 深度校验：防止 article 对象本身不为空，但里面的 url 是空的
        checkArticle(article);

        return PushMessage.builder()
                .type(MessageType.NEWS)
                .target(target)
                .articles(List.of(article))
                .build();
    }

    /**
     * 快速构建多条图文消息
     */
    public static PushMessage news(@NonNull String target, List<Article> articles) {
        if (articles == null || articles.isEmpty()) {
            throw new IllegalArgumentException("News articles cannot be empty");
        }
        articles.forEach(PushMessage::checkArticle); // 遍历校验每一条

        return PushMessage.builder()
                .type(MessageType.NEWS)
                .target(target)
                .articles(articles)
                .build();
    }

    /**
     * 内部辅助校验方法
     */
    private static void checkArticle(Article article) {
        Objects.requireNonNull(article.getTitle(), "Article title cannot be null");
        Objects.requireNonNull(article.getUrl(), "Article URL cannot be null");
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Article {
        private String title;
        private String description;
        private String url;

        @JsonProperty("picurl")
        private String picUrl;
    }
}
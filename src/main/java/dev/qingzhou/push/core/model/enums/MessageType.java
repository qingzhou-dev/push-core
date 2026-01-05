package dev.qingzhou.push.core.model.enums;

public enum MessageType {
    TEXT,       // 文本 (最常用)
    MARKDOWN,   // Markdown (极其推荐，支持颜色、加粗)
    TEXT_CARD,  // 文本卡片 (有标题、摘要、链接，非常适合做通知)
    IMAGE,      // 图片 (需要传 media_id)
    NEWS        // 图文 (图文链接)
}
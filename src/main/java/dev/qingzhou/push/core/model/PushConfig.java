package dev.qingzhou.push.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushConfig {
    // 鉴权三要素
    private String appKey;    // CorpId / AppKey / BotToken
    private String appSecret; // Secret
    private String agentId;   // AgentId (企微/钉钉专用)

    // Webhook 模式专用
    private String webhookUrl;
}
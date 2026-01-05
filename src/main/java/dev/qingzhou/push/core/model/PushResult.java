package dev.qingzhou.push.core.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PushResult {
    private boolean success;
    private String msgId;     // 渠道返回的消息ID (便于追踪)
    private String errorMsg;  // 错误信息
    private String rawResult; // 原始响应 JSON

    public static PushResult success(String msgId, String rawResult) {
        return PushResult.builder().success(true).msgId(msgId).rawResult(rawResult).build();
    }

    public static PushResult fail(String errorMsg, String rawResult) {
        return PushResult.builder().success(false).errorMsg(errorMsg).rawResult(rawResult).build();
    }

    public static PushResult fail(String errorMsg) {
        return fail(errorMsg, null);
    }
}
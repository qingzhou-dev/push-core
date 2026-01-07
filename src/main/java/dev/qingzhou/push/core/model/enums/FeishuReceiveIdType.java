package dev.qingzhou.push.core.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 飞书消息接收者 ID 类型
 * <p>
 * 详情参考: <a href="https://open.feishu.cn/document/server-docs/im-v1/message/create">飞书文档</a>
 */
@Getter
@AllArgsConstructor
public enum FeishuReceiveIdType {

    /**
     * 标识一个用户在某个应用中的身份。同一个用户在不同应用中的 Open ID 不同。
     */
    OPEN_ID("open_id"),

    /**
     * 标识一个用户在某个应用开发商下的身份。同一用户在同一开发商下的应用中的 Union ID 是相同的。
     */
    UNION_ID("union_id"),

    /**
     * 标识一个用户在某个租户内的身份。同一个用户在租户 A 和租户 B 内的 User ID 是不同的。
     */
    USER_ID("user_id"),

    /**
     * 以用户的真实邮箱来标识用户。
     */
    EMAIL("email"),

    /**
     * 以群 ID 来标识群聊。
     */
    CHAT_ID("chat_id");

    private final String value;

    @Override
    public String toString() {
        return value;
    }
}

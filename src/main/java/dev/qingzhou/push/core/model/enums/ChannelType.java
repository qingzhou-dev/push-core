package dev.qingzhou.push.core.model.enums;

/**
 * @deprecated Use String-based channel IDs from dev.qingzhou.push.core.api.ChannelIds.
 */
@Deprecated(forRemoval = true, since = "1.0.0")
public enum ChannelType {
    WECOM,
    DINGTALK,
    FEISHU,
    TELEGRAM,
    EMAIL
}

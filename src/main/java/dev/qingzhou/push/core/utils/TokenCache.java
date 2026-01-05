package dev.qingzhou.push.core.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 极简的内存 Token 缓存 (专为 push-core 设计)
 * 避免引入 Redis/Caffeine 等重依赖
 */
public class TokenCache {

    // 缓存容器: Key -> TokenValue
    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();
    // 过期时间容器: Key -> ExpirationTime (毫秒)
    private static final Map<String, Long> EXPIRE_MAP = new ConcurrentHashMap<>();

    /**
     * 获取缓存的 Token
     * @param key 缓存Key (通常是 appId)
     * @return token 或 null (如果不存在或已过期)
     */
    public static String get(String key) {
        String token = CACHE.get(key);
        Long expireTime = EXPIRE_MAP.get(key);

        if (token == null || expireTime == null) {
            return null;
        }

        // 如果当前时间 > 过期时间，说明过期了
        if (System.currentTimeMillis() > expireTime) {
            CACHE.remove(key);
            EXPIRE_MAP.remove(key);
            return null;
        }

        return token;
    }

    /**
     * 写入缓存
     * @param key 缓存Key
     * @param token Token值
     * @param expireSeconds 有效期(秒)
     */
    public static void set(String key, String token, long expireSeconds) {
        // 为了安全，我们通常会打个折扣 (比如有效期 7200秒，我们只存 7000秒)
        // 这样可以避免临界点的时间差问题
        long safeExpireSeconds = expireSeconds - 200;
        if (safeExpireSeconds < 1) {
            safeExpireSeconds = 1;
        }
        long safeExpireMillis = System.currentTimeMillis() + safeExpireSeconds * 1000;

        CACHE.put(key, token);
        EXPIRE_MAP.put(key, safeExpireMillis);
    }
}
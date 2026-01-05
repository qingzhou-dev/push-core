package dev.qingzhou.push.core.api;

import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PushChannelFactory {

    private static final Map<String, IPushChannel> CHANNEL_MAP = new ConcurrentHashMap<>();

    static {
        try {
            ServiceLoader<IPushChannel> loader = ServiceLoader.load(IPushChannel.class);
            var iterator = loader.iterator();
            while (true) {
                try {
                    if (!iterator.hasNext()) {
                        break;
                    }
                } catch (Throwable t) {
                    log.warn("Failed to iterate channel providers", t);
                    break;
                }

                IPushChannel channel;
                try {
                    channel = iterator.next();
                } catch (Throwable t) {
                    log.warn("Failed to load channel provider", t);
                    continue;
                }

                try {
                    registerInternal(channel.type(), channel);
                } catch (Throwable t) {
                    log.warn("Failed to register channel provider: {}", channel.getClass().getName(), t);
                }
            }
        } catch (Throwable t) {
            log.error("Failed to load channel providers", t);
        }
    }

    public static IPushChannel getChannel(String type) {
        IPushChannel channel = CHANNEL_MAP.get(normalizeType(type));
        if (channel == null) {
            throw new IllegalArgumentException("Channel not found: " + type);
        }
        return channel;
    }

    public static void register(String type, IPushChannel channel) {
        if (channel == null) {
            throw new IllegalArgumentException("Channel cannot be null");
        }
        String normalizedType = normalizeType(type);
        String channelType = normalizeType(channel.type());
        if (!normalizedType.equals(channelType)) {
            throw new IllegalArgumentException("Channel type mismatch: " + type + " vs " + channel.type());
        }
        registerInternal(normalizedType, channel);
    }

    public static void register(IPushChannel channel) {
        registerInternal(channel.type(), channel);
    }

    private static void registerInternal(String type, IPushChannel channel) {
        if (channel == null) {
            throw new IllegalArgumentException("Channel cannot be null");
        }
        String normalizedType = normalizeType(type);
        CHANNEL_MAP.compute(normalizedType, (key, existing) -> {
            if (existing == null) {
                return channel;
            }
            int existingWeight = existing.weight();
            int newWeight = channel.weight();
            if (newWeight > existingWeight) {
                return channel;
            }
            if (newWeight == existingWeight) {
                String existingName = existing.getClass().getName();
                String incomingName = channel.getClass().getName();
                if (existingName.compareTo(incomingName) <= 0) {
                    log.warn("Duplicate channel provider with same weight; keeping existing by class name. type={}, existing={}, incoming={}",
                            normalizedType, existingName, incomingName);
                    return existing;
                }
                log.warn("Duplicate channel provider with same weight; replacing by class name. type={}, existing={}, incoming={}",
                        normalizedType, existingName, incomingName);
                return channel;
            }
            return existing;
        });
    }

    private static String normalizeType(String type) {
        if (type == null) {
            throw new IllegalArgumentException("Channel type cannot be null");
        }
        String normalized = type.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Channel type cannot be blank");
        }
        return normalized;
    }
}

package dev.qingzhou.push.core.api;

import dev.qingzhou.push.core.model.PushConfig;
import dev.qingzhou.push.core.model.PushMessage;
import dev.qingzhou.push.core.model.PushResult;

public interface IPushChannel {
    String type();

    default int weight() {
        return 0;
    }

    PushResult send(PushMessage message, PushConfig config);
}

package dev.qingzhou.push.core.channel;

import dev.qingzhou.push.core.api.IPushChannel;
import dev.qingzhou.push.core.model.PushConfig;
import dev.qingzhou.push.core.model.PushMessage;
import dev.qingzhou.push.core.model.PushResult;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractChannel implements IPushChannel {

    @Override
    public abstract String type();

    @Override
    public PushResult send(PushMessage message, PushConfig config) {
        // 1. ????
        if (message == null) {
            return PushResult.fail("Content cannot be empty");
        }
        if (config == null) {
            return PushResult.fail("Config cannot be null");
        }
        if (message.getType() == null) {
            return PushResult.fail("Message type cannot be null");
        }
        String target = message.getTarget();
        if (!allowEmptyTarget() && (target == null || target.isBlank())) {
            return PushResult.fail("Target cannot be blank");
        }

        try {
            log.debug("Start sending message to target: {}", message.getTarget());
            checkConfig(config);
            return doSend(message, config);
        } catch (Exception e) {
            log.error("Send Error", e);
            return PushResult.fail(e.getMessage());
        }
    }

    protected boolean allowEmptyTarget() {
        return false;
    }

    protected abstract void checkConfig(PushConfig config);
    protected abstract PushResult doSend(PushMessage message, PushConfig config) throws Exception;
    protected abstract PushResult parseResult(String rawJson);
}

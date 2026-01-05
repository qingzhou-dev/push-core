# push-core

一个轻量级、统一的消息推送库，适用于 Java 21+。通过统一的消息模型与通道抽象，快速接入企业微信等渠道。

## 特性
- 统一消息模型：文本、Markdown、文本卡片、图片、图文
- 通道自动发现：基于 ServiceLoader 的插件式加载
- 轻量依赖：Jackson + commons-codec + slf4j
- 易扩展：实现 IPushChannel 即可接入新渠道

## 环境要求
- Java 21+
- Maven

## 安装

```xml
<dependency>
  <groupId>dev.qingzhou</groupId>
  <artifactId>push-core</artifactId>
  <version>1.0.0</version>
</dependency>
```

## 配置说明
- appKey/appSecret/agentId：认证信息（企业微信使用 CorpId/Secret/AgentId）
- webhookUrl：Webhook 模式专用

## 快速开始

```java
import dev.qingzhou.push.core.api.ChannelIds;
import dev.qingzhou.push.core.api.IPushChannel;
import dev.qingzhou.push.core.api.PushChannelFactory;
import dev.qingzhou.push.core.model.PushConfig;
import dev.qingzhou.push.core.model.PushMessage;
import dev.qingzhou.push.core.model.PushResult;

PushConfig config = PushConfig.builder()
    .appKey("corpId")
    .appSecret("secret")
    .agentId("agentId")
    .build();

IPushChannel channel = PushChannelFactory.getChannel(ChannelIds.WECOM);

PushMessage msg = PushMessage.text("userId", "你好，push-core");
PushResult result = channel.send(msg, config);
```

### 更多消息类型

```java
PushMessage markdown = PushMessage.markdown("userId", "标题", "**Markdown** 内容");

PushMessage textCard = PushMessage.textCard(
    "userId",
    "卡片标题",
    "卡片内容",
    "https://example.com"
);

PushMessage image = PushMessage.image("userId", "mediaId");

PushMessage.Article article = PushMessage.Article.builder()
    .title("标题")
    .description("描述")
    .url("https://example.com")
    .picUrl("https://example.com/cover.png")
    .build();
PushMessage news = PushMessage.news("userId", article);
```

## 支持渠道
- 企业微信（WeCom）：已实现
- 钉钉、飞书、Telegram、Email：预留通道标识，尚未实现

## 扩展自定义通道
1. 实现 `IPushChannel`
2. 在 `META-INF/services/dev.qingzhou.push.core.api.IPushChannel` 中声明实现类
3. 通过 `PushChannelFactory.getChannel("yourType")` 使用

也可在运行时调用 `PushChannelFactory.register(...)` 手动注册通道。

## 许可证
本项目采用双许可：
- GNU Affero General Public License v3.0 (AGPL v3)
- Commercial License

开源使用请遵守 AGPL v3；如需闭源或商业使用，请联系 dev@qingzhou.dev 获取商业许可。

<div align="center">
  <img src="docs/images/logo.png" alt="push-core logo" width="200" height="auto" />
  <h1>push-core</h1>
  <p>一个轻量级、统一的消息推送库，适用于 Java 21+。通过统一的消息模型与通道抽象，快速接入企业微信等渠道。</p>

  <p>
    <a href="https://central.sonatype.com/artifact/dev.qingzhou/push-core"><img src="https://img.shields.io/maven-central/v/dev.qingzhou/push-core.svg" alt="Maven Central"></a>
    <a href="https://www.gnu.org/licenses/agpl-3.0.txt"><img src="https://img.shields.io/badge/License-AGPL%20v3-blue.svg" alt="License"></a>
    <a href="https://openjdk.org/projects/jdk/21/"><img src="https://img.shields.io/badge/Java-21%2B-ED8B00?logo=openjdk&logoColor=white" alt="Java 21+"></a>
  </p>
</div>

---

## 目录

- [特性](#特性)
- [环境要求](#环境要求)
- [安装](#安装)
- [配置说明](#配置说明)
- [快速开始](#快速开始)
- [支持渠道](#支持渠道)
- [扩展自定义通道](#扩展自定义通道)
- [许可证](#许可证)

---

## 特性
- **统一消息模型**：支持文本、Markdown、文本卡片、图片、图文等多种格式。
- **通道自动发现**：基于 Java SPI (ServiceLoader) 的插件式加载机制。
- **轻量依赖**：核心仅依赖 Jackson、Commons-Codec 和 SLF4J。
- **易扩展**：只需实现 `IPushChannel` 接口即可接入新渠道。

## 环境要求
- Java 21+
- Maven

## 安装

将以下依赖添加到你的 `pom.xml` 中：

```xml
<dependency>
  <groupId>dev.qingzhou</groupId>
  <artifactId>push-core</artifactId>
  <version>1.0.0</version>
</dependency>
```

## 配置说明
初始化 `PushConfig` 时需要提供认证信息：
- `appKey`: 应用Key (企业微信对应 CorpId)
- `appSecret`: 应用密钥 (Secret)
- `agentId`: 应用ID (AgentId)
- `webhookUrl`: Webhook 地址 (部分通道专用)

## 快速开始

### 发送文本消息

```java
import dev.qingzhou.push.core.api.ChannelIds;
import dev.qingzhou.push.core.api.IPushChannel;
import dev.qingzhou.push.core.api.PushChannelFactory;
import dev.qingzhou.push.core.model.PushConfig;
import dev.qingzhou.push.core.model.PushMessage;
import dev.qingzhou.push.core.model.PushResult;

// 1. 配置认证信息
PushConfig config = PushConfig.builder()
    .appKey("your_corp_id")
    .appSecret("your_secret")
    .agentId("your_agent_id")
    .build();

// 2. 获取通道实例 (例如：企业微信)
IPushChannel channel = PushChannelFactory.getChannel(ChannelIds.WECOM);

// 3. 构建并发送消息
PushMessage msg = PushMessage.text("userId", "你好，push-core");
PushResult result = channel.send(msg, config);

if (result.isSuccess()) {
    System.out.println("发送成功: " + result.getMessageId());
} else {
    System.err.println("发送失败: " + result.getErrorMessage());
}
```

### 更多消息类型

#### Markdown 消息
```java
PushMessage markdown = PushMessage.markdown("userId", "标题", "**Markdown** 内容");
```

#### 文本卡片
```java
PushMessage textCard = PushMessage.textCard(
    "userId",
    "卡片标题",
    "卡片内容详情",
    "https://example.com"
);
```

#### 图片消息
```java
PushMessage image = PushMessage.image("userId", "mediaId_from_upload");
```

#### 图文消息 (News)
```java
PushMessage.Article article = PushMessage.Article.builder()
    .title("新闻标题")
    .description("新闻简述")
    .url("https://example.com")
    .picUrl("https://example.com/cover.png")
    .build();

PushMessage news = PushMessage.news("userId", article);
```

## 支持渠道
- **企业微信 (WeCom)**：已实现
- **钉钉、飞书、Telegram、Email**：预留通道标识，尚未实现

## 扩展自定义通道

1. **实现接口**：创建一个类实现 `IPushChannel` 接口。
2. **注册服务**：在 `META-INF/services/dev.qingzhou.push.core.api.IPushChannel` 文件中添加你的实现类全限定名。
3. **调用**：通过 `PushChannelFactory.getChannel("yourType")` 获取实例。

你也可以在运行时手动注册：
```java
PushChannelFactory.register("myChannel", new MyCustomChannel());
```

## 许可证

本项目采用双许可模式：
- **开源使用**：遵循 [GNU Affero General Public License v3.0 (AGPL v3)](https://www.gnu.org/licenses/agpl-3.0.txt)。
- **商业使用**：如需闭源或商业使用，请联系 [dev@qingzhou.dev](mailto:dev@qingzhou.dev) 获取商业许可。

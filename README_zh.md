
![Logo](feat_rect_logo.svg)


[![AGPL License](https://img.shields.io/badge/license-AGPL-blue.svg)](http://www.gnu.org/licenses/agpl-3.0)


# Feat - 高性能Java Web服务框架

Feat 是一个类似于 Vert.x 和 Spring Boot 的 Java Web 服务开发框架，专注于提供高性能、低资源消耗的解决方案。它支持多种协议和功能，适合构建高效、灵活的企业级 Web 应用。

## 功能亮点

- **高性能**：基于智能异步通信框架，轻松处理高并发场景。
- **支持多种协议**：包括 HTTP/1.0、HTTP/1.1、HTTP/2、WebSocket、SSE。
- **静态服务**：内置静态资源服务器，方便快速部署。
- **HTTPS 支持**：支持 PEM 格式的证书，轻松配置 SSL/TLS。
- **灵活配置**：通过插件和配置选项，满足不同需求。
- **企业级解决方案**：Feat Cloud 提供类似 Spring Boot 的开发体验，适合复杂应用。

## 快速上手

### 1. 引入依赖

在 Maven 项目中添加以下依赖：

```xml
<dependency>
    <groupId>tech.smartboot.feat</groupId>
    <artifactId>feat-core</artifactId>
    <version>${feat.version}</version>
</dependency>
```

### 2. 创建一个简单的 HTTP 服务

```java
public class HelloWorld {
    public static void main(String[] args) {
        Feat.httpServer()
            .httpHandler(request -> request.getResponse().write("Hello Feat"))
            .listen(8080);
    }
}
```

### 3. 启动服务

运行程序后，访问 `http://localhost:8080` 即可看到 "Hello Feat"。

## 使用示例

### 创建 WebSocket 服务

```java
public class WebSocketDemo {
    public static void main(String[] args) {
        Feat.httpServer().httpHandler(request -> {
            request.upgrade(new WebSocketUpgrade() {
                @Override
                public void handleTextMessage(WebSocketRequest request, WebSocketResponse response, String message) {
                    response.sendTextMessage("接受到消息：" + message);
                }
            });
        }).listen();
    }
}
```

### 配置 HTTPS

```java
public class HttpsPemDemo {
    public static void main(String[] args) {
        InputStream certPem = HttpsPemDemo.class.getClassLoader().getResourceAsStream("example.org.pem");
        InputStream keyPem = HttpsPemDemo.class.getClassLoader().getResourceAsStream("example.org-key.pem");
        SslPlugin sslPlugin = new SslPlugin(new PemServerSSLContextFactory(certPem, keyPem));
        Feat.httpServer(opt -> opt.addPlugin(sslPlugin)).httpHandler(req -> {
            req.getResponse().write("Hello Feat Https");
        }).listen();
    }
}
```

## 文档与资源

- **官方文档**：[Feat 文档](https://smartboot.tech/feat)
- **GitHub 仓库**：[Feat 仓库](https://github.com/smartboot/feat)
- **Gitee 仓库**：[Feat Gitee](https://gitee.com/smartboot/feat)

## 贡献指南

- **提交问题**：请访问 [GitHub Issues](https://github.com/smartboot/feat/issues) 或 [Gitee Issues](https://gitee.com/smartboot/feat/issues) 提交问题。
- **贡献代码**：fork 仓库，创建分支，提交 Pull Request。
- **代码风格**：遵循标准的 Java 编码规范，确保代码清晰可读。

## 许可证

Feat 使用 AGPL 协议开源，详细信息请查看 [LICENSE](LICENSE) 文件。

---

Feaut 是一个强大且灵活的框架，适合需要高性能和低资源消耗的场景。欢迎加入社区，共同推动项目发展！
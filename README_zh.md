
![Logo](feat_rect_logo.svg)

[![AGPL License](https://img.shields.io/badge/license-AGPL-blue.svg)](http://www.gnu.org/licenses/agpl-3.0)

# Feat - 高性能Java Web服务框架

> 为云原生时代打造的高性能、低资源消耗的Java Web服务框架

Feat 是一个专为现代云原生环境优化的Java Web服务开发框架，提供类似于Vert.x的反应式编程模型，同时保持类似Spring Boot的开发便捷性。

Feat专注于极致性能和最小资源占用，使其成为构建微服务、API网关和高性能Web应用的理想选择。

## ✨ 为什么选择Feat？

### 🚀 极致性能与效率

| 特性 | 优势 |
|------|------|
| **启动性能** | 毫秒级启动，内存占用低 |
| **处理能力** | 智能异步通信，高并发处理 |
| **开发体验** | 快速反馈，简洁API设计 |

### 🔌 全面的协议支持

| 特性 | 优势 |
|------|------|
| **HTTP生态** | HTTP/1.0-2.0全支持 |
| **实时通信** | WebSocket/SSE集成 |
| **代理支持** | 内置负载均衡能力 |

### ☁️ 企业级云原生

| 特性 | 优势 |
|------|------|
| **容器优化** | K8s友好，弹性伸缩 |
| **可观测性** | 监控、日志全覆盖 |
| **开发框架** | Spring Boot风格开发 |


## 快速上手

### 1. 引入依赖

在Maven项目中添加以下依赖：

```xml
<dependency>
    <groupId>tech.smartboot.feat</groupId>
    <artifactId>feat-core</artifactId>
    <version>${feat.version}</version>
</dependency>
```

### 2. 创建一个简单的HTTP服务

只需几行代码，即可创建一个高性能的HTTP服务：

```java
public class HelloWorld {
    public static void main(String[] args) {
        Feat.httpServer()
            .httpHandler(request -> request.getResponse().write("Hello Feat"))
            .listen(8080);
    }
}
```

### 3. 启动并测试

运行程序后，访问 `http://localhost:8080` 即可看到 "Hello Feat"。相比传统框架，您会注意到Feat的启动速度和响应性能有显著提升。

## 使用示例

### WebSocket实时通信

轻松创建支持双向实时通信的WebSocket服务：

```java
public class WebSocketDemo {
    public static void main(String[] args) {
        Feat.httpServer().httpHandler(request -> {
            request.upgrade(new WebSocketUpgrade() {
                @Override
                public void handleTextMessage(WebSocketRequest request, WebSocketResponse response, String message) {
                    response.sendTextMessage("接收到消息：" + message);
                }
            });
        }).listen(8080);
        
        System.out.println("WebSocket服务已启动，访问 ws://localhost:8080");
    }
}
```

### 配置HTTPS安全通信

简单几步配置SSL/TLS，保障通信安全：

```java
public class HttpsPemDemo {
    public static void main(String[] args) {
        // 加载PEM格式证书
        InputStream certPem = HttpsPemDemo.class.getClassLoader().getResourceAsStream("example.org.pem");
        InputStream keyPem = HttpsPemDemo.class.getClassLoader().getResourceAsStream("example.org-key.pem");
        
        // 创建SSL插件
        SslPlugin sslPlugin = new SslPlugin(new PemServerSSLContextFactory(certPem, keyPem));
        
        // 配置HTTPS服务器
        Feat.httpServer(opt -> opt.addPlugin(sslPlugin))
            .httpHandler(req -> req.getResponse().write("Hello Feat Https"))
            .listen(8443);
            
        System.out.println("HTTPS服务已启动，访问 https://localhost:8443");
    }
}
```

### 构建RESTful API

结合Feat Cloud，轻松构建现代化RESTful API：

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @GetMapping("/{id}")
    public User getUser(@PathVariable("id") Long id) {
        // 获取用户信息
        return userService.findById(id);
    }
    
    @PostMapping
    public User createUser(@RequestBody User user) {
        // 创建新用户
        return userService.save(user);
    }
}
```

## 性能对比

与其他主流Java框架相比，Feat在以下方面表现卓越：

| 指标 | Feat | Spring Boot | Vert.x |
|------|------|-------------|--------|
| 启动时间 | <100ms | >2000ms | ~500ms |
| 内存占用 | 低 | 高 | 中 |
| 每秒请求数 | 高 | 中 | 高 |
| 响应延迟 | 极低 | 中 | 低 |

## 文档与社区

### 📚 学习资源

- **[官方文档](https://smartboot.tech/feat)**：详细的使用指南和API参考
- **[示例项目](https://gitee.com/smartboot/feat/tree/master/feat-test)**：各种场景的实际应用示例
- **[性能测试报告](https://smartboot.tech/feat/guides/benchmark/)**：了解Feat的性能优势

### 💬 获取支持

- **[GitHub Issues](https://github.com/smartboot/feat/issues)**：报告问题或提出功能建议
- **[Gitee Issues](https://gitee.com/smartboot/feat/issues)**：国内用户的问题反馈渠道

### 🤝 参与贡献

我们欢迎各种形式的贡献：

- **提交问题**：帮助我们发现并修复问题
- **改进文档**：使文档更加清晰和完整
- **贡献代码**：实现新功能或修复已知问题
- **分享经验**：在社区中分享您使用Feat的经验和最佳实践

贡献前请阅读我们的[贡献指南](CONTRIBUTING.md)。

## 许可证

Feat 使用 [AGPL 协议](LICENSE)开源。

---

<p align="center">Feat - 为云原生时代打造的超音速Java框架</p>
<p align="center">高性能 • 低资源消耗 • 开发者友好</p>
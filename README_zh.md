
![Logo](feat_rect_logo.svg)

[![AGPL License](https://img.shields.io/badge/license-AGPL-blue.svg)](http://www.gnu.org/licenses/agpl-3.0)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/smartboot/feat)

# Feat - 高性能Java Web服务框架

有点像 Vert.x，又有点像 SpringBoot 的 Java Web 服务开发框架

### 🚀 Feat 的企业价值

| 优势 | 描述 |
|------|------|
| ⚡ 高性能 | 比传统框架快数倍以上，显著降低服务器成本 |
| ⏱️ 低延迟 | 毫秒级响应，提升用户体验 |
| 📦 易上手 | 简洁 API 设计，降低学习门槛 |
| 🔐 高可靠 | 稳定运行，减少运维压力 |

---

### 📦 三方包依赖

Feat 框架各模块的主要三方包依赖如下：

<table>
<thead>
<tr>
<th>模块</th>
<th>依赖包</th>
<th>依赖形式</th>
<th>说明</th>
</tr>
</thead>
<tbody>
<tr>
<td rowspan="2">Feat Core</td>
<td><a href="https://gitee.com/smartboot/smart-socket" target="_blank">smart-socket</a></td>
<td>运行时</td>
<td>基于Java AIO的网络通信框架</td>
</tr>
<tr>
<td><a href="https://gitee.com/alibaba/fastjson2" target="_blank">fastjson2</a></td>
<td>运行时</td>
<td>阿里巴巴的JSON处理库</td>
</tr>
<tr>
<td rowspan="2">Feat Cloud</td>
<td><a href="https://github.com/mybatis/mybatis-3" target="_blank">mybatis</a></td>
<td>编译时</td>
<td>优秀的持久层框架</td>
</tr>
<tr>
<td><a href="https://github.com/snakeyaml/snakeyaml" target="_blank">snakeyaml</a></td>
<td>编译时</td>
<td>YAML配置文件解析库</td>
</tr>
<tr>
<td rowspan="2">Feat AI</td>
<td>-</td>
<td>-</td>
<td>-</td>
</tr>
</tbody>
</table>

所有依赖均采用最新稳定版本，确保安全性和性能的最优表现。


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

### 构建RESTful API

结合Feat Cloud，轻松构建现代化RESTful API。Feat Cloud 在编译时完成静态转码，相比传统框架的运行时反射机制，具有更高的性能优势：

```java
@Controller("userApi")
public class UserController {
    
    // 支持路径参数
    @RequestMapping("/users/:id")
    public String getUser(@PathParam("id") String id) {
        return "User: " + id;
    }
    
    // 支持查询参数
    @RequestMapping("/users/search")
    public String searchUsers(@Param("name") String name, @Param("age") int age) {
        return "Search users with name: " + name + ", age: " + age;
    }
    
    // 支持对象参数绑定
    @RequestMapping("/users/create")
    public RestResult<Map<String, String>> createUser(UserParam param) {
        RestResult<Map<String, String>> result = new RestResult<>();
        result.setData(Collections.singletonMap("id", "123"));
        return result;
    }
    
    // 支持拦截器
    @InterceptorMapping({"/users/*"})
    public Interceptor userApiInterceptor() {
        return (context, completableFuture, chain) -> {
            System.out.println("Intercepting user API request...");
            chain.proceed(context, completableFuture);
        };
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
- **[企业授权方案](https://smartboot.tech/feat/auth/)**：助力高效开发，赋能企业成长

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
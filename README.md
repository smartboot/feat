
![Logo](feat_rect_logo.svg)

[![AGPL License](https://img.shields.io/badge/license-AGPL-blue.svg)](http://www.gnu.org/licenses/agpl-3.0)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/smartboot/feat)

# Feat - High-Performance Java Web Service Framework

> A high-performance, resource-efficient Java Web service framework built for the cloud-native era

Feat is a Java Web service development framework optimized for modern cloud-native environments, offering a reactive programming model similar to Vert.x while maintaining the development convenience of Spring Boot.

Feat focuses on ultimate performance and minimal resource consumption, making it an ideal choice for building microservices, API gateways, and high-performance web applications.

## ✨ Why Choose Feat?

### 🚀 Ultimate Performance and Efficiency

- **Startup Performance**: Millisecond-level startup, low memory footprint
- **Processing Capability**: Smart asynchronous communication, high concurrency handling
- **Development Experience**: Rapid feedback, clean API design

### 🔌 Comprehensive Protocol Support

- **HTTP Ecosystem**: Full support for HTTP/1.0-2.0
- **Real-time Communication**: WebSocket/SSE integration
- **Proxy Support**: Built-in load balancing capabilities

### ☁️ Enterprise-Grade Cloud Native

- **Container Optimization**: K8s-friendly, elastic scaling
- **Observability**: Complete monitoring and logging coverage
- **Development Framework**: Spring Boot-style development

### 📦 Third-party Dependencies

Feat framework's main third-party dependencies by module:

<table>
<thead>
<tr>
<th>Module</th>
<th>Dependency</th>
<th>Scope</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td rowspan="2">Feat Core</td>
<td><a href="https://gitee.com/smartboot/smart-socket" target="_blank">smart-socket</a></td>
<td>Runtime</td>
<td>Java AIO based network communication framework</td>
</tr>
<tr>
<td><a href="https://gitee.com/alibaba/fastjson2" target="_blank">fastjson2</a></td>
<td>Runtime</td>
<td>Alibaba's JSON processing library</td>
</tr>
<tr>
<td rowspan="2">Feat Cloud</td>
<td><a href="https://github.com/mybatis/mybatis-3" target="_blank">mybatis</a></td>
<td>Compile</td>
<td>Excellent persistence layer framework</td>
</tr>
<tr>
<td><a href="https://github.com/snakeyaml/snakeyaml" target="_blank">snakeyaml</a></td>
<td>Compile</td>
<td>YAML configuration parsing library</td>
</tr>
<tr>
<td>Feat AI</td>
<td>-</td>
<td>-</td>
<td>-</td>
</tr>
</tbody>
</table>

All dependencies use the latest stable versions to ensure optimal security and performance.

## Quick Start

### 1. Add Dependency

Add the following dependency to your Maven project:

```xml
<dependency>
    <groupId>tech.smartboot.feat</groupId>
    <artifactId>feat-core</artifactId>
    <version>${feat.version}</version>
</dependency>
```

### 2. Create a Simple HTTP Service

Create a high-performance HTTP service with just a few lines of code:

```java
public class HelloWorld {
    public static void main(String[] args) {
        Feat.httpServer()
            .httpHandler(request -> request.getResponse().write("Hello Feat"))
            .listen(8080);
    }
}
```

### 3. Launch and Test

After running the program, visit `http://localhost:8080` to see "Hello Feat". Compared to traditional frameworks, you'll notice significant improvements in Feat's startup speed and response performance.

## Usage Examples

### WebSocket Real-time Communication

Easily create a WebSocket service supporting bidirectional real-time communication:

```java
public class WebSocketDemo {
    public static void main(String[] args) {
        Feat.httpServer().httpHandler(request -> {
            request.upgrade(new WebSocketUpgrade() {
                @Override
                public void handleTextMessage(WebSocketRequest request, WebSocketResponse response, String message) {
                    response.sendTextMessage("Message received: " + message);
                }
            });
        }).listen(8080);
        
        System.out.println("WebSocket service started, access ws://localhost:8080");
    }
}
```

### Building RESTful APIs

Combined with Feat Cloud, easily build modern RESTful APIs. Feat Cloud performs static transcoding at compile time, offering superior performance compared to traditional frameworks' runtime reflection mechanisms:

```java
@Controller("userApi")
public class UserController {
    
    // Support path parameters
    @RequestMapping("/users/:id")
    public String getUser(@PathParam("id") String id) {
        return "User: " + id;
    }
    
    // Support query parameters
    @RequestMapping("/users/search")
    public String searchUsers(@Param("name") String name, @Param("age") int age) {
        return "Search users with name: " + name + ", age: " + age;
    }
    
    // Support object parameter binding
    @RequestMapping("/users/create")
    public RestResult<Map<String, String>> createUser(UserParam param) {
        RestResult<Map<String, String>> result = new RestResult<>();
        result.setData(Collections.singletonMap("id", "123"));
        return result;
    }
    
    // Support interceptors
    @InterceptorMapping({"/users/*"})
    public Interceptor userApiInterceptor() {
        return (context, completableFuture, chain) -> {
            System.out.println("Intercepting user API request...");
            chain.proceed(context, completableFuture);
        };
    }
}
```

## Performance Comparison

Compared to other mainstream Java frameworks, Feat excels in the following aspects:

| Metric | Feat | Spring Boot | Vert.x |
|--------|------|-------------|--------|
| Startup Time | <100ms | >2000ms | ~500ms |
| Memory Usage | Low | High | Medium |
| Requests/Second | High | Medium | High |
| Response Latency | Very Low | Medium | Low |

## Documentation and Community

### 📚 Learning Resources

- **[Official Documentation](https://smartboot.tech/feat)**: Detailed user guides and API references
- **[Example Projects](https://gitee.com/smartboot/feat/tree/master/feat-test)**: Real-world application examples
- **[Performance Test Reports](https://smartboot.tech/feat/guides/benchmark/)**: Understand Feat's performance advantages

### 💬 Get Support

- **[GitHub Issues](https://github.com/smartboot/feat/issues)**: Report issues or suggest features
- **[Gitee Issues](https://gitee.com/smartboot/feat/issues)**: Issue feedback channel for domestic users

### 🤝 Contributing

We welcome all forms of contributions:

- **Submit Issues**: Help us identify and fix problems
- **Improve Documentation**: Make documentation clearer and more complete
- **Contribute Code**: Implement new features or fix known issues
- **Share Experience**: Share your experience and best practices using Feat in the community

Please read our [Contributing Guide](CONTRIBUTING.md) before contributing.

## License

Feat is open-source under the [AGPL License](LICENSE).

---

<p align="center">Feat - The Supersonic Java Framework for the Cloud Native Era</p>
<p align="center">High Performance • Low Resource Consumption • Developer Friendly</p>


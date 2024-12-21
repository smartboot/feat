# smart-http
smart-http 是一款可编程的 Http 应用微内核，方便用户根据自身需求进行 Server 或 Client 的应用开发。

感兴趣的朋友请记得 Star一下该项目，并且非常欢迎有能力的朋友贡献你的想法和代码。
## 功能列表
1. 支持GET、POST的 HTTP 请求。
2. 提供了 URL 路由组件，可以快速搭建一套静态服务器。
3. 支持部分 RFC2612 规范，后续会逐渐完善。
4. 支持 Https 协议，由 smart-socket 为其赋能。
5. 具备文件上传的能力。
6. 支持 websocket、Cookie
7. 支持 Server、Client 开发

## 快速体验
### 服务端开发
1. 在您的Maven工程中引入smart-http依赖。
    ```xml
    <dependency>
        <groupId>io.github.smartboot.http</groupId>
        <artifactId>smart-http-server</artifactId>
        <version>2.5</version>
    </dependency>
    ```
2. 拷贝以下代码并启动。
    ```java
   public class SimpleSmartHttp {
       public static void main(String[] args) {
           HttpBootstrap bootstrap = new HttpBootstrap();
           bootstrap.configuration().debug(true);
           bootstrap.httpHandler(new HttpServerHandler() {
               @Override
               public void handle(HttpRequest request, HttpResponse response) throws IOException {
                   response.write("hello smart-http<br/>".getBytes());
               }
           }).setPort(8080).start();
       }
   }
    ```
3. 浏览器访问:`http://localhost:8080/`，亦或采用websocket请求`ws://127.0.0.1:8080/`

### 客户端开发
1. 在您的Maven工程中引入smart-http依赖。
    ```xml
    <dependency>
        <groupId>io.github.smartboot.http</groupId>
        <artifactId>smart-http-client</artifactId>
        <version>2.5</version>
    </dependency>
    ```
2. 拷贝以下代码并启动。
    ```java
   public class HttpGetDemo {
       public static void main(String[] args) {
           HttpClient httpClient = new HttpClient("www.baidu.com", 80);
           httpClient.get("/").header().keepalive(false).done()
                   .onSuccess(response -> System.out.println(response.body()))
                   .onFailure(Throwable::printStackTrace)
                   .done();
       }
   }
    ```



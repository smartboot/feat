package tech.smartboot.feat.demo;

import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.HttpServerHandler;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;

public class PostChunkedDemo {
    public static void main(String[] args) throws IOException {
        HttpServer bootstrap = new HttpServer();
        bootstrap.configuration().debug(true);
        bootstrap.httpHandler(new HttpServerHandler() {
            @Override
            public void handle(HttpRequest request, HttpResponse response) throws Throwable {
                System.out.println("http request...");
                for (String s : request.getParameters().keySet()) {
                    System.out.println(s + " :" + request.getParameter(s));
                }
            }
        });
        bootstrap.start();

        AioQuickClient client = new AioQuickClient("localhost", 8080, (readBuffer, session) -> null,
                (session, msg) -> {
                });
        AioSession session = client.start();
        session.writeBuffer().write(("POST /cmdb/admin-api/model/info/list " +
                "HTTP/1.1\r\n" +
                "Accept: */*\r\n" +
                "orgId: aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\r\n" +
                "sec-ch-ua-mobile: ?0\r\n" +
                "Sec-Fetch-Dest: empty\r\n" +
                "name: l4qiang\r\n" +
                "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJkZXB0TmFtZSI6IueUqOaIt-acuuaehCIsIm9yZ05hbWUiOiLnlKjmiLfmnLrmnoQiLCJyb2xlcyI6W3siaWQiOiI2IiwibmFtZSI6bnVsbH0seyJpZCI6IjYiLCJuYW1lIjpudWxsfV0sImRlcHRJZCI6IjIiLCJ1c2VyTmFtZSI6Imw0cWlhbmciLCJvcmdVaWQiOiI5MzgwYzlmYzk5NTQ0ZjM0ODdkMDc3MTcxYjIyMGE3ZiIsInVzZXJJZCI6IjQ4NWRjZmUzY2VhMDQwMTU5OGRlNzRmODRhNmJkYmM2IiwicG9zdHMiOltdLCJvcmdJZCI6ImFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhIiwidWlkIjoiZWNhMmM5MDE1MmE5NDBlMTkyMWMxNjZiNmZkNjc2N2YiLCJjbGllbnRUeXBlIjoicGMiLCJuYW1lIjoibDRxaWFuZyIsInVzZXJUeXBlIjoiMCIsImRhdGFMZXZlbCI6IjEiLCJqdGkiOiJlY2EyYzkwMTUyYTk0MGUxOTIxYzE2NmI2ZmQ2NzY3ZiIsImlhdCI6MTcyMjIwNDExNSwiaXNzIjoiVG9wQ2xvdWQifQ.0WtDMfVsPCqMwjNQHoaAG6UfTokjZL854_P485I0Uro\r\n" +
                "Host: 127.0.0.1:1010\r\n" +
                "Accept-Encoding: gzip, deflate, br, zstd\r\n" +
                "Sec-Fetch-Site: none\r\n" +
                "Sec-Fetch-Mode: cors\r\n" +
                "___internal-request-id: c2596b6a-f52e-4c6d-a938-ec7081f6e101\r\n" +
                "content-type: application/x-www-form-urlencoded\r\n" +
                "userName: l4qiang\r\n" +
                "tenantId: aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\r\n" +
                "appId: 57\r\n" +
                "userId: 485dcfe3cea0401598de74f84a6bdbc6\r\n" +
                "DNT: 1\r\n" +
                "Cookie: JSESSIONID=nrzskrddPdFKjNRcXJzgnnOTI6s5Y83Bwuu6x6XY\r\n" +
                "sec-ch-ua-platform: \"macOS\"\r\n" +
                "sec-ch-ua: \"Not)A;Brand\";v=\"99\", \"Microsoft Edge\";v=\"127\", \"Chromium\";v=\"127\"\r\n" +
                "Accept-Language: en-US,en;q=0.9,zh-CN;q=0.8,zh-TW;q=0.7,zh;q=0.6,ja;q=0.5\r\n" +
                "Forwarded: proto=http;host=\"127.0.0.1:1010\";for=\"127.0.0.1:51460\"\r\n" +
                "X-Forwarded-For: 127.0.0.1\r\n" +
                "X-Forwarded-Proto: http\r\n" +
                "X-Forwarded-Port: 1010\r\n" +
                "X-Forwarded-Host: 127.0.0.1:1010\r\n" +
                "user-agent: ReactorNetty/0.8.3.RELEASE\r\n" +
                "transfer-encoding: chunked\r\n" +
                "\r\n" +
                "8\r\n"+
                "test=111\r\n"+
                "0\r\n\r\n").getBytes());
        session.writeBuffer().flush();
    }
}

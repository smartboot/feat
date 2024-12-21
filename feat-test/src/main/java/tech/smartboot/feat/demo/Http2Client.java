//package tech.smartboot.feat.demo;
//
//
//import java.net.URI;
//import java.net.http.HttpClient;
//import java.net.http.HttpRequest;
//import java.net.http.HttpResponse;
//import java.util.concurrent.CompletableFuture;
//
//public class Http2Client {
//    public static void main(String[] args) {
//
//        HttpClient client = HttpClient.newBuilder()
//                .version(HttpClient.Version.HTTP_2)
//                .build();
//
//        for (int i = 0; i < 1; i++) {
//            HttpRequest request = HttpRequest.newBuilder()
////                    .uri(URI.create("http://nghttp2.org/"))
//                    .uri(URI.create("http://127.0.0.1:8080/"))
//                    .POST(HttpRequest.BodyPublishers.ofString("hello world"))
//                    .build();
//
//            CompletableFuture<HttpResponse<String>> response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
//            response.exceptionally(ex -> {
//                System.out.println(ex.getMessage());
//                return null;
//            });
//            response.thenAccept(res -> {
//                System.out.println("Status Code: " + res.statusCode());
//                System.out.println("Headers: " + res.headers().map());
//                System.out.println("Body: " + res.body());
//            }).join();
//        }
//    }
//}

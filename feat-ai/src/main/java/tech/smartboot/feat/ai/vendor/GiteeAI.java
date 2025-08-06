/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.vendor;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.smartboot.socket.timer.HashedWheelTimer;
import tech.smartboot.feat.Feat;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.client.HttpPost;
import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.client.Multipart;
import tech.smartboot.feat.core.common.HeaderName;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 3/23/25
 */
public class GiteeAI {
    /**
     * Gitee AI服务的基础URL
     */
    public static final String BASE_URL = "https://ai.gitee.com/v1/";
    private final String apiKey;

    public GiteeAI(String apiKey) {
        this.apiKey = apiKey;
    }

    private static final String CACHE_DIR = System.getProperty("java.io.tmpdir") + File.separator + "gitee-ai-cache";

    /**
     * 解析PDF文件并返回内容
     * 优先从缓存中获取，如果缓存不存在则调用Gitee AI服务解析文件
     *
     * @param filePath PDF文件路径
     * @return 解析结果的CompletableFuture
     */
    public CompletableFuture<String> parsePdf(String filePath) {
        CompletableFuture<String> future = new CompletableFuture<>();
        File pdfFile = new File(filePath);
        if (!pdfFile.exists() || !pdfFile.isFile()) {
            future.completeExceptionally(new IllegalArgumentException("PDF文件不存在: " + filePath));
            return future;
        }

        // 计算文件的MD5值作为缓存键
        String cacheKey = calculateMD5(pdfFile);
        if (cacheKey == null) {
            future.completeExceptionally(new RuntimeException("无法计算文件MD5"));
            return future;
        }

        // 检查缓存是否存在
        File cacheFile = getCacheFile(cacheKey);
        if (cacheFile.exists()) {
            try {
                // 从缓存文件读取内容
                String cachedContent = readFromFile(cacheFile);
                return CompletableFuture.completedFuture(cachedContent);
            } catch (Exception e) {
                // 缓存读取失败，继续使用API解析
                System.err.println("缓存读取失败: " + e.getMessage());
            }
        } else {
            // 将解析成功的文档内容写入缓存文件
            future.thenAccept(content -> saveToCache(cacheKey, content));
        }

        // 缓存不存在，调用Gitee AI服务解析文件
        HttpClient client = Feat.httpClient(BASE_URL + "/async/documents/parse", opts -> {
            opts.debug(true);
        });
        HttpPost post = client.post().header(header -> {
            header.add(HeaderName.AUTHORIZATION, "Bearer " + apiKey);
        });
        post.postBody(postBody -> {
            List<Multipart> multiparts = new ArrayList<>();
            multiparts.add(Multipart.newFormMultipart("model", "PDF-Extract-Kit-1.0"));
            multiparts.add(Multipart.newFormMultipart("is_ocr", "true"));
            multiparts.add(Multipart.newFormMultipart("formula_enable", "true"));
            multiparts.add(Multipart.newFormMultipart("table_enable", "true"));
            multiparts.add(Multipart.newFormMultipart("layout_model", "doclayout_yolo"));
            multiparts.add(Multipart.newFileMultipart("file", pdfFile));
            postBody.multipart(multiparts);
        });

        Consumer<HttpResponse> onSuccess = new Consumer<HttpResponse>() {
            @Override
            public void accept(HttpResponse response) {
                // 获取解析结果
                JSONObject jsonObject = JSON.parseObject(response.body());
                String status = jsonObject.getString("status");
                JSONObject urls = jsonObject.getJSONObject("urls");
                String getUrl = urls.getString("get");
                if (Arrays.asList("waiting", "in_progress").contains(status)) {
                    Consumer<HttpResponse> onSuccess = this;
                    HashedWheelTimer.DEFAULT_TIMER.schedule(() -> {
                        HttpClient get = Feat.httpClient(getUrl, opts -> {
                        });
                        get.get().header(header -> {
                            header.add(HeaderName.AUTHORIZATION, "Bearer " + apiKey);
                        }).onSuccess(onSuccess).submit();
                    }, 1, TimeUnit.SECONDS);

                    return;
                }
                if ("success".equals(status)) {
                    StringBuilder stringBuilder = new StringBuilder();
                    List<String> contents = (List<String>) jsonObject.getByPath("$.output.segments[*].content");
                    for (String s : contents) {
                        stringBuilder.append(s);
                    }
                    // 完成Future
                    future.complete(stringBuilder.toString());
                } else {
                    future.completeExceptionally(new RuntimeException(status));
                }
            }
        };
        post.onSuccess(onSuccess);
        post.onFailure(throwable -> {
            throwable.printStackTrace();
            future.completeExceptionally(throwable);
        }).submit();

        return future;
    }


    /**
     * 计算文件的MD5哈希值
     *
     * @param file 需要计算哈希值的文件
     * @return MD5哈希值字符串，如果计算失败则返回null
     */
    private String calculateMD5(File file) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
                byte[] buffer = new byte[8192]; // 8KB缓冲区
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    md.update(buffer, 0, bytesRead);
                }
            }
            byte[] digest = md.digest();

            // 将字节数组转换为十六进制字符串
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取缓存文件对象
     *
     * @param cacheKey 缓存键（文件MD5值）
     * @return 缓存文件对象
     */
    private File getCacheFile(String cacheKey) {
        File cacheDirectory = new File(CACHE_DIR);
        if (!cacheDirectory.exists()) {
            cacheDirectory.mkdirs();
        }
        return new File(cacheDirectory, cacheKey + ".txt");
    }

    /**
     * 从文件中读取内容
     *
     * @param file 要读取的文件
     * @return 文件内容字符串
     * @throws Exception 如果读取失败
     */
    private String readFromFile(File file) throws Exception {
        StringBuilder content = new StringBuilder();
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    /**
     * 将内容保存到缓存文件
     *
     * @param cacheKey 缓存键（文件MD5值）
     * @param content  要保存的内容
     */
    private void saveToCache(String cacheKey, String content) {
        try {
            File cacheFile = getCacheFile(cacheKey);
            try (java.io.BufferedWriter writer = new java.io.BufferedWriter(new java.io.FileWriter(cacheFile))) {
                writer.write(content);
            }
        } catch (Exception e) {
            System.err.println("缓存保存失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

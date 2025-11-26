/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.agent.search;

import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.client.HttpGet;
import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.common.HeaderName;
import tech.smartboot.feat.core.common.HttpStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author 三刀
 * @version v1.0 11/26/25
 */
public abstract class Searcher {
    public static String search(String url, Consumer<HttpGet> consumer) {
        Searcher searcher;
        if (url.startsWith(BaiduSearcher.BASE_URL)) {
            searcher = new BaiduSearcher();
        } else if (url.startsWith(BingSearcher.BASE_URL)) {
            searcher = new BingSearcher();
        } else {
            searcher = new DefaultSearcher();
        }
        HttpClient httpClient = new HttpClient(url);
        try {
            HttpGet httpGet = httpClient.get();
            if (consumer != null) {
                consumer.accept(httpGet);
            }
            httpGet.header(header -> {
                baseHeader().forEach(header::set);
            });
            searcher.initRequest(httpGet);
            HttpResponse response = httpGet.submit().get();
            if (response.statusCode() != HttpStatus.OK.value()) {
                return "请求失败.";
            } else {
                return searcher.toMarkdown(response.body());
            }
        } catch (Throwable e) {
            return "请求失败.";
        }
    }

    public static String search(String url) {
        return search(url, null);
    }

    protected void initRequest(HttpGet httpGet) {
    }

    protected abstract String toMarkdown(String html);

    private static Map<String, String> baseHeader() {
        Map<String, String> headers = new HashMap<>();
        headers.put(HeaderName.ACCEPT.getName(), "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
        headers.put(HeaderName.ACCEPT_ENCODING.getName(), "gzip, deflate, zstd, dcb, dcz");
        headers.put(HeaderName.ACCEPT_LANGUAGE.getName(), "zh-CN,zh;q=0.9");
        headers.put(HeaderName.USER_AGENT.getName(), "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36");
        headers.put(HeaderName.CACHE_CONTROL.getName(), "max-age=0");
        headers.put("Sec-Ch-Ua", "\"Chromium\";v=\"142\", \"Google Chrome\";v=\"142\", \"Not_A Brand\";v=\"99\"");
        headers.put("Sec-Ch-Ua-Arch", "");
        headers.put("Sec-Ch-Ua-Bitness", "\"64\"");
        headers.put("Sec-Ch-Ua-Full-Version", "\"142.0.7444.163\"");
        headers.put("Sec-Ch-Ua-Full-Version-List", "\"Chromium\";v=\"142.0.7444.163\", \"Google Chrome\";v=\"142.0.7444.163\", \"Not_A Brand\";v=\"99.0.0.0\"");
        headers.put("Sec-Ch-Ua-Mobile", "?0");
        headers.put("Sec-Ch-Ua-Platform", "\"macOS\"");
        headers.put("Sec-Fetch-Dest", "document");
        headers.put("Sec-Fetch-Mode", "navigate");
        headers.put("Sec-Fetch-Site", "same-origin");
        headers.put("Sec-Fetch-User", "?1");
        headers.put("Prority", "u=0, i");
        headers.put("Upgrade-Insecure-Requests", "1");
        return headers;
    }
}

/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.agent.tools.reader;

import com.alibaba.fastjson2.JSONObject;

/**
 * OSChina 新闻阅读器
 * 用于解析 OSChina 新闻 API 返回的 JSON 数据并转换为 Markdown 格式
 *
 * @author 三刀
 * @version v1.0 12/1/25
 */
public class OsChinaNewsReader extends WebReader {
    /**
     * OSChina 新闻详情的基础URL
     */
    public static final String BASE_URL = "https://apiv1.oschina.net/oschinapi/new/detail";

    @Override
    protected String toMarkdown(String html) {
        StringBuilder stringBuilder = new StringBuilder();
        JSONObject jsonObject = JSONObject.parseObject(html);
        if (jsonObject.getBoolean("success")) {
            JSONObject result = jsonObject.getJSONObject("result");

            // 添加标题
            stringBuilder.append("# ").append(result.getString("title")).append("\n\n");

            // 添加作者和发布时间信息
            JSONObject userVo = result.getJSONObject("userVo");
            if (userVo != null) {
                stringBuilder.append("**作者**: ").append(userVo.getString("spaceName")).append("  \n");
            }
            stringBuilder.append("**发布时间**: ").append(result.getString("pubTime")).append("  \n");
            stringBuilder.append("**阅读数**: ").append(result.getInteger("viewCount")).append("  \n");
            stringBuilder.append("**评论数**: ").append(result.getInteger("commentCount")).append("\n\n");

            // 添加正文内容
            String detail = result.getString("detail");
            if (detail != null && !detail.isEmpty()) {
                stringBuilder.append(super.toMarkdown(detail)).append("\n\n");
            }

            // 添加项目相关信息（如果存在）
            String projectName = result.getString("projectName");
            String projectUrl = result.getString("projectUrl");
            if (projectName != null && !projectName.isEmpty() && projectUrl != null && !projectUrl.isEmpty()) {
                stringBuilder.append("相关项目: [").append(projectName).append("](").append(projectUrl).append(")\n");
            }

            return stringBuilder.toString();
        } else {
            return "获取文章内容失败: " + jsonObject.getString("message");
        }
    }
}
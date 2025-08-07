/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai;

/**
 * @author 三刀
 * @version v1.0 8/7/25
 */
public class Vendor {
    /**
     * 服务商
     */
    private final String baseUrl;
    /**
     * 模型名称
     */
    private final String model;

    public Vendor(String baseUrl, String model) {
        this.baseUrl = baseUrl;
        this.model = model;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public String model() {
        return model;
    }
}

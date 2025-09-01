/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud;

import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.server.ServerOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class CloudOptions extends ServerOptions {
    private String[] packages;
    private final Map<String, Object> externalBeans = new HashMap<>();

    String[] getPackages() {
        return packages;
    }

    public CloudOptions setPackages(String... packages) {
        this.packages = packages;
        return this;
    }

    Map<String, Object> getExternalBeans() {
        return externalBeans;
    }

    public CloudOptions registerBean(String key, Object value) {
        if (externalBeans.containsKey(key)) {
            throw new FeatException("bean " + key + " already exists");
        }
        externalBeans.put(key, value);
        return this;
    }

}

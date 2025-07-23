/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.aot.value;

import javax.lang.model.element.Element;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 5/27/25
 */
abstract class AbstractSerializer {
    abstract String serialize(Element field, Object paramValue) throws Exception;


    protected String toString(String str) {
        return "\"" + str.replace("\\", "\\\\").replace("\n", "\\n").replace("\"", "\\\"") + "\"";
    }
}

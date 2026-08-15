/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.aot.value;

import javax.lang.model.element.Element;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 5/27/25
 */
abstract class AbstractSerializer {


    public abstract String serialize(Element field, Object paramValue);


    private String toString(String str) {
        return "\"" + str.replace("\\", "\\\\").replace("\n", "\\n").replace("\"", "\\\"") + "\"";
    }


    protected String resolveIntProperty(Object objVal) {
        if (objVal == null) {
            throw new NullPointerException();
        }
        if (objVal instanceof Integer) {
            return objVal.toString();
        }
        if (!(objVal instanceof String)) {
            return null;
        }
        String val = objVal.toString();
        if (!(val.startsWith("${") && val.endsWith("}"))) {
            return String.valueOf(Integer.parseInt(val));
        }
        int spit = val.indexOf(":");
        String name = val.substring(2, spit > 0 ? spit : val.length() - 1);
        int defaultValue = spit > 0 ? Integer.parseInt(val.substring(spit + 1, val.length() - 1)) : 0;
        return "resolveIntProperty(\"" + name + "\" ,\"" + name.replace('.', '_').toUpperCase() + "\"," + defaultValue + ")";
    }

    protected String resolveStringProperty(String val) {
        if (!(val.startsWith("${") && val.endsWith("}"))) {
            return toString(val);
        }
        int spit = val.indexOf(":");
        String name = val.substring(2, spit > 0 ? spit : val.length() - 1);
        String defaultValue = spit > 0 ? val.substring(spit + 1, val.length() - 1) : null;
        return "resolveProperty(\"" + name + "\" ,\"" + name.replace('.', '_').toUpperCase() + "\"," + (defaultValue == null ? "null" : toString(defaultValue)) + ")";
    }
}

/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *  and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.aot.serializer.orm.dynamic;

/**
 * 动态 SQL 代码生成时的临时变量名生成器。
 */
public class NameGenerator {
    private int counter;

    public String next(String prefix) {
        return "_" + prefix + (counter++);
    }
}

/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.aot.serializer.extension;

import com.alibaba.fastjson2.JSONPath;
import tech.smartboot.redisun.Redisun;

import javax.annotation.processing.ProcessingEnvironment;
import java.io.PrintWriter;

import static tech.smartboot.feat.cloud.aot.controller.JsonSerializer.headBlank;

/**
 * @author 三刀
 * @version v1.0 1/12/26
 */
public class RedisunSerializer extends ExtensionSerializer {
    public RedisunSerializer(ProcessingEnvironment processingEnv, String config, PrintWriter printWriter) {
        super(processingEnv, config, printWriter);
    }

    @Override
    public void serializeImport() {
        printWriter.println("import " + Redisun.class.getName() + ";");
    }

    @Override
    public void serializeLoadBean() {
        printWriter.append(headBlank(0)).println("applicationContext.addBean(\"redisun\", Redisun.create(opt -> {");
        printWriter.append(headBlank(1)).println("opt.setAddress(\"" + JSONPath.eval(config, "$.feat.redis.address") + "\");");
        Object password = JSONPath.eval(config, "$.feat.redis.password");
        if (password != null) {
            printWriter.append(headBlank(1)).println("opt.setPassword(\"" + password + "\");");
        }
        Object db = JSONPath.eval(config, "$.feat.redis.database");
        if (db != null) {
            printWriter.append(headBlank(1)).println("opt.setDatabase(" + db + ");");
        }
        printWriter.append(headBlank(0)).println("}));");
    }
}

/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.aot;

import tech.smartboot.feat.cloud.annotation.Bean;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import java.io.IOException;

/**
 * @author 三刀
 * @version v1.0 7/23/25
 */
final class BeanSerializer extends AbstractSerializer {
    public BeanSerializer(ProcessingEnvironment processingEnv, String config, Element element) throws IOException {
        super(processingEnv, config, element);
    }


    @Override
    public void serializeLoadBean() {
        super.serializeLoadBean();
        Bean annotation = element.getAnnotation(Bean.class);
        String beanName = element.getSimpleName().toString().substring(0, 1).toLowerCase() + element.getSimpleName().toString().substring(1);
        if (!annotation.value().isEmpty()) {
            beanName = annotation.value();
        }
        printWriter.println("\t\tapplicationContext.addBean(\"" + beanName + "\", bean);");
    }

    @Override
    public int order() {
        return element.getAnnotation(Bean.class).order();
    }
}

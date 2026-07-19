/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.aot.serializer;

import tech.smartboot.feat.cloud.annotation.InterceptorMapping;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.cloud.annotation.interceptor.AroundInvoke;
import tech.smartboot.feat.core.common.exception.FeatException;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import java.io.IOException;

/**
 * @author 三刀
 * @version v1.0 7/23/25
 */
public final class InterceptorSerializer extends AbstractSerializer {
    public InterceptorSerializer(ProcessingEnvironment processingEnv, String config, Element element, String classSuffix) throws IOException {
        super(processingEnv, config, element, classSuffix);
    }

    @Override
    public void serializeLoadBean() {
        super.serializeLoadBean();

        //找到包含 AroundInvoke 注解的方法
        Element method = null;
        for (Element e : element.getEnclosedElements()) {
            if (e.getAnnotation(AroundInvoke.class) != null) {
                if (method != null) {
                    throw new FeatException("Interceptor can only have one method with AroundInvoke annotation");
                }
                method = e;
            }
        }
        printWriter.println("\t\tapplicationContext.getInterceptors().put(" + element.getSimpleName() + ".class, new InterceptorFunction() {");
        printWriter.println("\t\t\t@Override");
        printWriter.println("\t\t\tpublic Object apply(InvocationContext invocationContext) throws Exception{");
        printWriter.println("\t\t\t\treturn bean." + method.getSimpleName() + "(invocationContext);");
        printWriter.println("\t\t\t}");
        printWriter.println("\t\t});");
    }

}

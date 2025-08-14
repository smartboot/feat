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

import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author 三刀
 * @version v1.0 7/23/25
 */
interface Serializer {
    default PrintWriter getPrintWriter() {
        throw new UnsupportedOperationException();
    }

    default String packageName() {
        throw new UnsupportedOperationException();
    }

    default String className() {
        throw new UnsupportedOperationException();
    }

    default void serializeImport() {
    }

    default void serializeProperty() {
    }

    default void serializeLoadBean() {
    }

    default void serializeAutowired() {
    }

    default void serializeRouter() throws IOException {
    }

    default void serializeBytePool() {
    }

    default void serializerValueSetter() {
    }

    default void serializePostConstruct() {
    }

    default void serializeDestroy() {
    }

    default int order() {
        return 0;
    }
}

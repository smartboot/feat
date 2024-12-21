/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: LoggerFactory.java
 * Date: 2021-01-27
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.core.common.logging;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/1/27
 */
public final class LoggerFactory {
    private static final Map<String, Logger> loggerMap = new HashMap<>();

    public static Logger getLogger(Class<?> clazz) {
        return getLogger(clazz.getName());
    }

    public static Logger getLogger(String name) {
        Logger logger = loggerMap.get(name);
        if (logger != null) {
            return logger;
        }
        logger = new RunLogger(name);
        loggerMap.put(name, logger);
        return logger;
    }
}

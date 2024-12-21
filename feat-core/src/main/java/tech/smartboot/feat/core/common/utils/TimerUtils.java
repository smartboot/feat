/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: TimerUtils.java
 * Date: 2022-02-04
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.core.common.utils;

import org.smartboot.socket.timer.HashedWheelTimer;

import java.util.concurrent.TimeUnit;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/2/4
 */
public class TimerUtils {

    /**
     * 当前时间
     */
    private static long currentTimeMillis = System.currentTimeMillis();

    static {
        HashedWheelTimer.DEFAULT_TIMER.scheduleWithFixedDelay(() -> currentTimeMillis = System.currentTimeMillis(), 1, TimeUnit.SECONDS);
    }

    public static long currentTimeMillis() {
        return currentTimeMillis;
    }
}

/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package actuator;

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.cloud.RestResult;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.PathParam;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.cloud.annotation.RequestMethod;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.core.common.utils.StringUtils;

import java.util.logging.Level;

/**
 * 日志级别管理控制器
 * <p>
 * 该控制器提供了一组RESTful接口，用于动态管理应用程序的日志级别。
 * 支持查询和修改全局日志级别以及特定logger的日志级别。
 * </p>
 *
 * @author 三刀
 * @version v1.0 5/29/25
 */
@Controller("actuator/loggers")
public class ActuatorLoggerController {

    /**
     * 获取所有已配置的日志级别
     * <p>
     * 该方法会返回系统中所有已经配置的日志级别信息，包括全局日志级别和针对特定logger的日志级别配置。
     * </p>
     *
     * @return 返回包含所有日志级别配置的JSON对象，key为logger名称，value为对应的日志级别
     */
    @RequestMapping("/level")
    public RestResult<JSONObject> getLogger() {
        JSONObject jsonObject = new JSONObject();
        System.getProperties().stringPropertyNames().stream().filter(key -> key.startsWith(LoggerFactory.SYSTEM_PROPERTY_LOG_LEVEL)).forEach(key -> jsonObject.put(key, reverseLevel(System.getProperty(key))));
        return RestResult.ok(jsonObject);
    }

    /**
     * 获取指定logger的日志级别
     * <p>
     * 根据提供的logger名称查询其当前配置的日志级别。如果指定的logger没有特定的级别配置，
     * 则返回全局的默认日志级别。
     * </p>
     *
     * @param loggerName logger的名称，通常是完整的类名或包名
     * @return 返回指定logger的日志级别，如果logger名称为空则返回失败结果
     */
    @RequestMapping(value = "/level/:loggerName", method = RequestMethod.GET)
    public RestResult<String> getLogger(@PathParam("loggerName") String loggerName) {
        if (StringUtils.isBlank(loggerName)) {
            return RestResult.fail("loggerName is empty");
        }
        return RestResult.ok(reverseLevel(System.getProperty(LoggerFactory.SYSTEM_PROPERTY_LOG_LEVEL + "." + loggerName, System.getProperty(LoggerFactory.SYSTEM_PROPERTY_LOG_LEVEL))));
    }

    /**
     * 设置指定logger的日志级别
     * <p>
     * 为指定的logger设置新的日志级别。这个设置是动态的，会立即生效，不需要重启应用。
     * </p>
     *
     * @param loggerName logger的名称，通常是完整的类名或包名
     * @param level      要设置的日志级别，如：DEBUG、INFO、WARN、ERROR等
     * @return 设置成功返回true，如果参数为空则返回失败结果
     */
    @RequestMapping(value = "/level/set/:loggerName/:level")
    public RestResult<Boolean> setLogger(@PathParam("loggerName") String loggerName, @PathParam("level") String level) {
        if (StringUtils.isBlank(loggerName)) {
            return RestResult.fail("loggerName is empty");
        }
        if (StringUtils.isBlank(level)) {
            return RestResult.fail("level is empty");
        }
        level = convertLevel(level);
        System.setProperty(LoggerFactory.SYSTEM_PROPERTY_LOG_LEVEL + "." + loggerName, level);
        return RestResult.ok(true);
    }

    private static String convertLevel(String level) {
        if ("debug".equalsIgnoreCase(level)) {
            level = Level.CONFIG.getName();
        } else if ("info".equalsIgnoreCase(level)) {
            level = Level.INFO.getName();
        } else if ("warn".equalsIgnoreCase(level)) {
            level = Level.WARNING.getName();
        } else if ("error".equalsIgnoreCase(level)) {
            level = Level.SEVERE.getName();
        } else if ("off".equalsIgnoreCase(level)) {
            level = Level.OFF.getName();
            return level;
        }
        return level;
    }

    private static String reverseLevel(String level) {
        if (Level.CONFIG.getName().equalsIgnoreCase(level)) {
            level = "debug";
        } else if (Level.INFO.getName().equalsIgnoreCase(level)) {
            level = "info";
        } else if (Level.WARNING.getName().equalsIgnoreCase(level)) {
            level = "warn";
        } else if (Level.SEVERE.getName().equalsIgnoreCase(level)) {
            level = "error";
        } else if (Level.OFF.getName().equalsIgnoreCase(level)) {
            level = "off";
        }
        return level;
    }

    /**
     * 设置全局默认日志级别
     * <p>
     * 设置系统的全局默认日志级别，并清除所有特定logger的自定义日志级别配置。
     * 这个操作会影响到所有没有特定配置的logger。
     * </p>
     *
     * @param level 要设置的全局日志级别，如：DEBUG、INFO、WARN、ERROR等
     * @return 设置成功返回true，如果日志级别参数为空则返回失败结果
     */
    @RequestMapping(value = "/level/set/:level")
    public RestResult<Boolean> setLogger(@PathParam("level") String level) {
        if (StringUtils.isBlank(level)) {
            return RestResult.fail("level is empty");
        }
        level = convertLevel(level);
        System.setProperty(LoggerFactory.SYSTEM_PROPERTY_LOG_LEVEL, level);
        System.getProperties().stringPropertyNames().stream().filter(key -> key.startsWith(LoggerFactory.SYSTEM_PROPERTY_LOG_LEVEL + ".")).forEach(System::clearProperty);
        return RestResult.ok(true);
    }


}

/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: ParamReflect.java
 * Date: 2020-01-01
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.common.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Properties;
import java.util.logging.Level;

public class ParamReflect {
    private ParamReflect() {
    }

    public static boolean reflect(String file, Object obj) {
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            return reflect(in, obj);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                in = null;
            }
        }
        return false;
    }

    /**
     * @param in  配置文件输入流,将由Properties的对象进行解析
     * @param obj 配置项所要绑定的实体对象
     * @return
     */
    public static boolean reflect(InputStream in, Object obj) {

        boolean flag = false;
        Properties property = new Properties();
        try {
            property.load(in);
            Field[] fileds = obj.getClass().getDeclaredFields();
            for (Field f : fileds) {
                // 过滤未加注解的字段
                if (!f.isAnnotationPresent(Param.class)) {
                    continue;
                }
                Param p = f.getAnnotation(Param.class);// 配置项默认值
                String name = p.name();
                if (StringUtils.isBlank(name)) {
                    name = f.getName();
                }
                Type fieldType = f.getGenericType();
                String value;
                if (property.containsKey(name)) {
                    value = property.getProperty(name);
                } else if (StringUtils.isNotBlank(p.value())) {
                    value = p.value();
                } else {
                    continue;
                }

                f.setAccessible(true);
                if (int.class == fieldType) {
                    if (StringUtils.isNotBlank(value)) {
                        f.setInt(obj, Integer.parseInt(value));
                    }
                } else if (long.class == fieldType) {
                    if (StringUtils.isNotBlank(value)) {
                        f.setLong(obj, Long.parseLong(value));
                    }
                } else if (boolean.class == fieldType) {
                    if (StringUtils.isNotBlank(value)) {
                        f.setBoolean(obj, Boolean.parseBoolean(value));
                    }
                } else if (fieldType == String.class) {
                    f.set(obj, value);
                }
                // 字符串数组
                else if (String.class.equals(f.getType().getComponentType())) {
                    if (StringUtils.isBlank(value)) {
                        continue;
                    }
                    f.set(obj, value.split(","));
                } else if (Level.class.equals(f.getGenericType())) {
                    f.set(obj, Level.parse(value));
                } else if (f.getType().isInterface() && !StringUtils.isBlank(value)) {
                    f.set(obj, Class.forName(value).newInstance());
                }
                // 返回表示数组组件类型的Class。如果此类不表示数组类，则此方法返回 null。
                else if (f.getType().getComponentType() != null) {
                    if (StringUtils.isBlank(value)) {
                        continue;
                    }
                    String[] vals = value.split(",");
                    Object arryObj = Array.newInstance(f.getType().getComponentType(), vals.length);
                    for (int i = 0; i < vals.length; i++) {
                        Array.set(arryObj, i, Class.forName(vals[i]).newInstance());
                    }
                    f.set(obj, arryObj);
                } else {
                    throw new RuntimeException("Unsupport Type " + f.getGenericType());
                }
            }
            flag = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }
}
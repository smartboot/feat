/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.demo.apt;

import java.util.HashMap;

/**
 * 继承 Map 的响应结果封装，用于验证 APT 对 Map 子类返回值的序列化能力。
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class DtoExtendsMap extends HashMap<String, Object> {
    private static final long serialVersionUID = 1L;

    /**
     * 成功状态码
     */
    public static final int SUCCESS = 200;

    /**
     * 失败状态码
     */
    public static final int FAIL = 500;

    /**
     * 状态码字段名
     */
    public static final String CODE_TAG = "code";

    /**
     * 提示信息字段名
     */
    public static final String MSG_TAG = "msg";

    /**
     * 业务数据字段名
     */
    public static final String DATA_TAG = "data";

    /**
     * 创建一个空的响应结果
     */
    public DtoExtendsMap() {
    }

    /**
     * 创建一个不含业务数据的响应结果
     *
     * @param code 状态码
     * @param msg  提示信息
     */
    public DtoExtendsMap(int code, String msg) {
        this(code, msg, null);
    }

    /**
     * 创建一个响应结果
     *
     * @param code 状态码
     * @param msg  提示信息
     * @param data 业务数据，为 null 时不写入
     */
    public DtoExtendsMap(int code, String msg, Object data) {
        super.put(CODE_TAG, code);
        super.put(MSG_TAG, msg);
        if (data != null) {
            super.put(DATA_TAG, data);
        }
    }

    /**
     * 创建成功的响应结果
     *
     * @return 成功的响应结果实例
     */
    public static DtoExtendsMap ok() {
        return new DtoExtendsMap(SUCCESS, "操作成功");
    }

    /**
     * 创建成功的响应结果
     *
     * @param data 业务数据
     * @return 成功的响应结果实例
     */
    public static DtoExtendsMap ok(Object data) {
        return new DtoExtendsMap(SUCCESS, "操作成功", data);
    }

    /**
     * 创建失败的响应结果
     *
     * @param msg 失败提示信息
     * @return 失败的响应结果实例
     */
    public static DtoExtendsMap fail(String msg) {
        return new DtoExtendsMap(FAIL, msg);
    }

    /**
     * 覆写以支持链式调用
     *
     * @param key   键
     * @param value 值
     * @return 当前响应结果实例
     */
    @Override
    public DtoExtendsMap put(String key, Object value) {
        super.put(key, value);
        return this;
    }
}

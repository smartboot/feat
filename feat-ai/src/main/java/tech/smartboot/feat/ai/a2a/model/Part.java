/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.a2a.model;

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.a2a.enums.PartType;

/**
 * A2A 消息部分内容类
 *
 * <p>消息的基本组成部分，可以是文本、文件、数据或函数调用等。</p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 3/12/26
 */
public class Part {
    /**
     * 内容类型
     */
    private PartType type;

    /**
     * 文本内容（当type为TEXT时使用）
     */
    private String text;

    /**
     * 文件内容（当type为FILE时使用）
     */
    private FileContent file;

    /**
     * 结构化数据（当type为DATA时使用）
     */
    private JSONObject data;

    /**
     * 函数调用（当type为FUNCTION_CALL时使用）
     */
    private FunctionCall functionCall;

    /**
     * 函数响应（当type为FUNCTION_RESPONSE时使用）
     */
    private FunctionResponse functionResponse;

    public PartType getType() {
        return type;
    }

    public void setType(PartType type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public FileContent getFile() {
        return file;
    }

    public void setFile(FileContent file) {
        this.file = file;
    }

    public JSONObject getData() {
        return data;
    }

    public void setData(JSONObject data) {
        this.data = data;
    }

    public FunctionCall getFunctionCall() {
        return functionCall;
    }

    public void setFunctionCall(FunctionCall functionCall) {
        this.functionCall = functionCall;
    }

    public FunctionResponse getFunctionResponse() {
        return functionResponse;
    }

    public void setFunctionResponse(FunctionResponse functionResponse) {
        this.functionResponse = functionResponse;
    }

    /**
     * 创建文本类型的Part
     *
     * @param text 文本内容
     * @return Part实例
     */
    public static Part text(String text) {
        Part part = new Part();
        part.setType(PartType.TEXT);
        part.setText(text);
        return part;
    }

    /**
     * 创建文件类型的Part
     *
     * @param file 文件内容
     * @return Part实例
     */
    public static Part file(FileContent file) {
        Part part = new Part();
        part.setType(PartType.FILE);
        part.setFile(file);
        return part;
    }

    /**
     * 创建数据类型的Part
     *
     * @param data 结构化数据
     * @return Part实例
     */
    public static Part data(JSONObject data) {
        Part part = new Part();
        part.setType(PartType.DATA);
        part.setData(data);
        return part;
    }

    /**
     * 创建函数调用类型的Part
     *
     * @param functionCall 函数调用
     * @return Part实例
     */
    public static Part functionCall(FunctionCall functionCall) {
        Part part = new Part();
        part.setType(PartType.FUNCTION_CALL);
        part.setFunctionCall(functionCall);
        return part;
    }

    /**
     * 创建函数响应类型的Part
     *
     * @param functionResponse 函数响应
     * @return Part实例
     */
    public static Part functionResponse(FunctionResponse functionResponse) {
        Part part = new Part();
        part.setType(PartType.FUNCTION_RESPONSE);
        part.setFunctionResponse(functionResponse);
        return part;
    }

    /**
     * 检查是否为文本类型
     *
     * @return 如果是文本类型返回true
     */
    public boolean isText() {
        return type == PartType.TEXT;
    }

    /**
     * 检查是否为文件类型
     *
     * @return 如果是文件类型返回true
     */
    public boolean isFile() {
        return type == PartType.FILE;
    }

    /**
     * 检查是否为数据类型
     *
     * @return 如果是数据类型返回true
     */
    public boolean isData() {
        return type == PartType.DATA;
    }

    /**
     * 检查是否为函数调用类型
     *
     * @return 如果是函数调用类型返回true
     */
    public boolean isFunctionCall() {
        return type == PartType.FUNCTION_CALL;
    }

    /**
     * 检查是否为函数响应类型
     *
     * @return 如果是函数响应类型返回true
     */
    public boolean isFunctionResponse() {
        return type == PartType.FUNCTION_RESPONSE;
    }
}

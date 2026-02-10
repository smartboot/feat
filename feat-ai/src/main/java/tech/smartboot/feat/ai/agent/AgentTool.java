/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.agent;

import com.alibaba.fastjson2.JSONObject;

import java.util.concurrent.CompletableFuture;

/**
 * AI代理工具执行器接口
 * <p>
 * 该接口定义了AI代理工具的标准规范，任何实现此接口的类都可以作为AI代理的工具被调用。
 * 工具通常是一些可以执行特定任务的功能模块，如搜索、计算、数据处理等。
 * </p>
 * <p>
 * 工具的设计遵循以下原则：
 * 1. 单一职责：每个工具只负责一项特定功能
 * 2. 易于扩展：可以通过实现该接口轻松添加新工具
 * 3. 标准化：使用统一的参数传递和结果返回方式
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public interface AgentTool {

    /**
     * 执行工具的具体逻辑
     * <p>
     * 根据传入的参数执行工具对应的功能，并返回执行结果。
     * 例如：如果是一个计算器工具，parameters可能包含数学表达式，
     * 执行后返回计算结果。
     * </p>
     * <p>
     * 工具实现应该处理可能发生的异常，并尽可能返回有意义的错误信息，
     * 而不是直接抛出异常，以便AI Agent能够理解和处理错误情况。
     * </p>
     *
     * @param parameters 工具执行所需的参数，以JSONObject格式传入
     *                   参数格式应与{@link #getParametersSchema()}返回的JSON Schema保持一致
     * @return 执行结果，以字符串形式返回，便于AI Agent处理和理解
     */
    CompletableFuture<String> execute(JSONObject parameters);

    /**
     * 获取工具的唯一标识名称
     * <p>
     * 用于在AI代理系统中识别不同的工具，名称应当具有描述性且全局唯一。
     * 例如："calculator"、"web-search"、"file-reader"等。
     * </p>
     * <p>
     * 名称应遵循以下规范：
     * 1. 使用小写字母和下划线组合
     * 2. 简洁明了，能体现工具功能
     * 3. 在整个系统中保持唯一性
     * </p>
     *
     * @return 工具的名称字符串，用于在Agent中识别和调用该工具
     */
    String getName();

    /**
     * 获取工具的功能描述
     * <p>
     * 详细描述工具的作用和用途，供AI代理理解何时以及如何使用该工具。
     * 描述应该足够清晰，使AI能够判断在何种场景下应该调用此工具。
     * 例如："用于执行基本的数学运算，支持加减乘除等操作"。
     * </p>
     * <p>
     * 描述应该包括：
     * 1. 工具的主要功能
     * 2. 适用场景
     * 3. 可能的限制条件（如果有）
     * </p>
     *
     * @return 工具功能的详细描述文本，供AI Agent在决策时参考
     */
    String getDescription();

    /**
     * 获取工具参数的JSON Schema定义
     * <p>
     * 使用JSON Schema格式定义工具所需的参数结构，包括参数名、类型、是否必需等信息。
     * 这使得AI代理能够了解如何正确地构造参数来调用工具。
     * 例如：{"type": "object", "properties": {"expression": {"type": "string"}}, "required": ["expression"]}
     * </p>
     * <p>
     * JSON Schema应包含以下信息：
     * 1. 参数的数据类型（字符串、数字、布尔值、对象等）
     * 2. 必需参数列表
     * 3. 参数的描述信息
     * 4. 参数的约束条件（如取值范围、格式要求等）
     * </p>
     *
     * @return 参数定义的JSON Schema字符串，用于指导AI Agent正确构建参数
     * @see <a href="https://json-schema.org/">JSON Schema官方文档</a>
     */
    String getParametersSchema();
}
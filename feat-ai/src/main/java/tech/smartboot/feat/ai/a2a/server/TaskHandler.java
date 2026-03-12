/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.a2a.server;

import tech.smartboot.feat.ai.a2a.model.PushNotificationConfig;
import tech.smartboot.feat.ai.a2a.model.SendTaskRequest;
import tech.smartboot.feat.ai.a2a.model.Task;
import tech.smartboot.feat.ai.a2a.model.TaskCancelRequest;
import tech.smartboot.feat.ai.a2a.model.TaskQueryRequest;
import tech.smartboot.feat.ai.a2a.model.TaskResponse;
import tech.smartboot.feat.ai.a2a.model.SetPushNotificationRequest;
import tech.smartboot.feat.ai.a2a.model.SubscribeRequest;

import java.util.concurrent.CompletableFuture;

/**
 * A2A 任务处理器接口
 *
 * <p>定义了处理A2A协议中各种任务操作的方法，实现类需要实现这些方法以提供具体的任务处理能力。</p>
 *
 * <p>主要操作包括：</p>
 * <ul>
 *   <li>发送/创建任务</li>
 *   <li>获取任务状态</li>
 *   <li>取消任务</li>
 *   <li>订阅任务更新</li>
 *   <li>设置推送通知</li>
 * </ul>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 3/12/26
 */
public interface TaskHandler {

    /**
     * 发送/创建任务
     *
     * <p>处理客户端的任务创建请求，返回任务执行结果。</p>
     *
     * @param request 任务请求参数
     * @return 任务响应
     */
    TaskResponse sendTask(SendTaskRequest request);

    /**
     * 异步发送/创建任务
     *
     * <p>异步方式处理任务创建请求。</p>
     *
     * @param request 任务请求参数
     * @return 包含任务响应的CompletableFuture
     */
    default CompletableFuture<TaskResponse> asyncSendTask(SendTaskRequest request) {
        return CompletableFuture.supplyAsync(() -> sendTask(request));
    }

    /**
     * 获取任务
     *
     * <p>根据任务ID查询任务状态和消息历史。</p>
     *
     * @param request 任务查询请求
     * @return 任务对象
     */
    Task getTask(TaskQueryRequest request);

    /**
     * 异步获取任务
     *
     * <p>异步方式查询任务状态。</p>
     *
     * @param request 任务查询请求
     * @return 包含任务对象的CompletableFuture
     */
    default CompletableFuture<Task> asyncGetTask(TaskQueryRequest request) {
        return CompletableFuture.supplyAsync(() -> getTask(request));
    }

    /**
     * 取消任务
     *
     * <p>取消指定ID的任务执行。</p>
     *
     * @param request 任务取消请求
     * @return 任务响应
     */
    TaskResponse cancelTask(TaskCancelRequest request);

    /**
     * 异步取消任务
     *
     * <p>异步方式取消任务。</p>
     *
     * @param request 任务取消请求
     * @return 包含任务响应的CompletableFuture
     */
    default CompletableFuture<TaskResponse> asyncCancelTask(TaskCancelRequest request) {
        return CompletableFuture.supplyAsync(() -> cancelTask(request));
    }

    /**
     * 订阅任务更新
     *
     * <p>订阅指定任务的状态更新。</p>
     *
     * @param request 订阅请求
     * @return 任务响应
     */
    TaskResponse subscribe(SubscribeRequest request);

    /**
     * 异步订阅任务更新
     *
     * <p>异步方式订阅任务更新。</p>
     *
     * @param request 订阅请求
     * @return 包含任务响应的CompletableFuture
     */
    default CompletableFuture<TaskResponse> asyncSubscribe(SubscribeRequest request) {
        return CompletableFuture.supplyAsync(() -> subscribe(request));
    }

    /**
     * 设置推送通知
     *
     * <p>为指定任务配置推送通知。</p>
     *
     * @param request 推送通知设置请求
     * @return 推送通知配置
     */
    PushNotificationConfig setPushNotification(SetPushNotificationRequest request);

    /**
     * 异步设置推送通知
     *
     * <p>异步方式设置推送通知。</p>
     *
     * @param request 推送通知设置请求
     * @return 包含推送通知配置的CompletableFuture
     */
    default CompletableFuture<PushNotificationConfig> asyncSetPushNotification(SetPushNotificationRequest request) {
        return CompletableFuture.supplyAsync(() -> setPushNotification(request));
    }
}

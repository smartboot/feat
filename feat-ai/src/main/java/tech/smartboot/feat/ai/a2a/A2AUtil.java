/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.a2a;

import tech.smartboot.feat.ai.a2a.model.AgentCard;
import tech.smartboot.feat.ai.a2a.model.Task;
import tech.smartboot.feat.ai.a2a.model.TaskResponse;

import java.util.UUID;

/**
 * A2A 工具类
 *
 * <p>提供A2A协议中常用的工具方法。</p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 3/12/26
 */
public final class A2AUtil {

    private A2AUtil() {
        // 工具类，禁止实例化
    }

    /**
     * 生成任务ID
     *
     * @return 唯一的任务ID
     */
    public static String generateTaskId() {
        return "task-" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成会话ID
     *
     * @return 唯一的会话ID
     */
    public static String generateSessionId() {
        return "session-" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 验证AgentCard
     *
     * @param agentCard 智能体卡片
     * @return 如果有效返回true
     */
    public static boolean isValidAgentCard(AgentCard agentCard) {
        if (agentCard == null) {
            return false;
        }
        if (agentCard.getName() == null || agentCard.getName().isEmpty()) {
            return false;
        }
        if (agentCard.getUrl() == null || agentCard.getUrl().isEmpty()) {
            return false;
        }
        return true;
    }

    /**
     * 验证任务ID格式
     *
     * @param taskId 任务ID
     * @return 如果格式有效返回true
     */
    public static boolean isValidTaskId(String taskId) {
        return taskId != null && !taskId.isEmpty() && taskId.startsWith("task-");
    }

    /**
     * 创建成功响应
     *
     * @param task 任务
     * @return 任务响应
     */
    public static TaskResponse createSuccessResponse(Task task) {
        TaskResponse response = new TaskResponse();
        response.setId(task.getId());
        response.setStatus(task.getState());
        if (task.getArtifacts() != null) {
            task.getArtifacts().forEach(response::addArtifact);
        }
        return response;
    }
}

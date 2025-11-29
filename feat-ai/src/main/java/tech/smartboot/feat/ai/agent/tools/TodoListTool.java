/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.agent.tools;

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.agent.AgentTool;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Todo列表工具，用于创建和管理结构化任务列表
 * <p>
 * 该工具允许AI Agent创建和管理任务列表，跟踪复杂工作流程的进度。
 * 支持创建任务列表、添加任务项、标记完成、列出任务和删除任务等功能。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class TodoListTool implements AgentTool {

    private static final String NAME = "todo_list";
    private static final String DESCRIPTION = "创建和管理结构化任务列表，用于跟踪复杂工作流程的进度";

    /**
     * 存储任务列表的内存存储
     * <p>
     * 使用线程安全的ConcurrentHashMap存储所有任务列表，
     * 键为列表ID，值为任务项列表。
     * </p>
     */
    private final ConcurrentHashMap<String, List<TodoItem>> todoLists = new ConcurrentHashMap<>();

    /**
     * 任务项ID生成器
     * <p>
     * 使用原子整数确保在并发环境下生成唯一的任务项ID。
     * </p>
     */
    private final AtomicInteger idGenerator = new AtomicInteger(0);

    /**
     * 执行Todo列表操作
     * <p>
     * 根据操作类型执行相应的任务列表管理操作，支持：
     * 1. create: 创建新的任务列表
     * 2. add: 添加任务项
     * 3. complete: 标记任务项为完成
     * 4. list: 列出任务项
     * 5. remove: 移除任务项
     * </p>
     *
     * @param parameters 包含操作类型和相关参数的JSON对象
     * @return 操作结果字符串
     */
    @Override
    public String execute(JSONObject parameters) {
        String action = parameters.getString("action");
        String listId = parameters.getString("list_id");

        if (action == null) {
            return "错误：必须提供'action'参数";
        }

        switch (action) {
            case "create":
                return createTodoList(parameters);
            case "add":
                return addTodoItem(listId, parameters);
            case "complete":
                return completeTodoItem(listId, parameters);
            case "list":
                return listTodoItems(listId);
            case "remove":
                return removeTodoItem(listId, parameters);
            default:
                return "错误：不支持的操作 '" + action + "'";
        }
    }

    /**
     * 获取工具名称
     *
     * @return 工具名称 "todo_list"
     */
    @Override
    public String getName() {
        return NAME;
    }

    /**
     * 获取工具描述
     *
     * @return 工具功能描述
     */
    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * 获取工具参数的JSON Schema定义
     * <p>
     * 定义了Todo列表工具的参数格式，根据不同操作类型需要不同的参数。
     * </p>
     *
     * @return 参数定义的JSON Schema字符串
     */
    @Override
    public String getParametersSchema() {
        return "{\n" +
                "  \"type\": \"object\",\n" +
                "  \"properties\": {\n" +
                "    \"action\": {\n" +
                "      \"type\": \"string\",\n" +
                "      \"description\": \"操作类型: create, add, complete, list, remove\",\n" +
                "      \"enum\": [\"create\", \"add\", \"complete\", \"list\", \"remove\"]\n" +
                "    },\n" +
                "    \"list_id\": {\n" +
                "      \"type\": \"string\",\n" +
                "      \"description\": \"任务列表ID\"\n" +
                "    },\n" +
                "    \"title\": {\n" +
                "      \"type\": \"string\",\n" +
                "      \"description\": \"任务标题\"\n" +
                "    },\n" +
                "    \"description\": {\n" +
                "      \"type\": \"string\",\n" +
                "      \"description\": \"任务详细描述\"\n" +
                "    },\n" +
                "    \"item_id\": {\n" +
                "      \"type\": \"integer\",\n" +
                "      \"description\": \"任务项ID\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"required\": [\"action\"]\n" +
                "}";
    }

    /**
     * 创建新的任务列表
     * <p>
     * 创建一个新的空任务列表，并为其分配唯一ID。
     * </p>
     *
     * @param parameters 参数（在此方法中未使用）
     * @return 包含新列表ID的结果字符串
     */
    private String createTodoList(JSONObject parameters) {
        String listId = "todo_list_" + System.currentTimeMillis();
        todoLists.put(listId, new ArrayList<>());
        return "已创建新的任务列表，ID为: " + listId;
    }

    /**
     * 添加任务项
     * <p>
     * 向指定的任务列表中添加新的任务项。
     * </p>
     *
     * @param listId     任务列表ID
     * @param parameters 包含任务标题和描述的参数
     * @return 操作结果字符串
     */
    private String addTodoItem(String listId, JSONObject parameters) {
        if (listId == null) {
            return "错误：必须提供'list_id'参数";
        }

        List<TodoItem> todoList = todoLists.get(listId);
        if (todoList == null) {
            return "错误：未找到ID为 '" + listId + "' 的任务列表";
        }

        String title = parameters.getString("title");
        if (title == null || title.isEmpty()) {
            return "错误：必须提供'title'参数";
        }

        String description = parameters.getString("description");
        TodoItem item = new TodoItem(idGenerator.incrementAndGet(), title, description);
        todoList.add(item);

        return "已添加新任务项: " + item.toString();
    }

    /**
     * 完成任务项
     * <p>
     * 将指定任务列表中的特定任务项标记为已完成。
     * </p>
     *
     * @param listId     任务列表ID
     * @param parameters 包含任务项ID的参数
     * @return 操作结果字符串
     */
    private String completeTodoItem(String listId, JSONObject parameters) {
        if (listId == null) {
            return "错误：必须提供'list_id'参数";
        }

        List<TodoItem> todoList = todoLists.get(listId);
        if (todoList == null) {
            return "错误：未找到ID为 '" + listId + "' 的任务列表";
        }

        Integer itemId = parameters.getInteger("item_id");
        if (itemId == null) {
            return "错误：必须提供'item_id'参数";
        }

        for (TodoItem item : todoList) {
            if (item.getId() == itemId) {
                item.setCompleted(true);
                return "已标记任务项为完成: " + item.toString();
            }
        }

        return "错误：未找到ID为 '" + itemId + "' 的任务项";
    }

    /**
     * 列出所有任务项
     * <p>
     * 列出指定任务列表中的所有任务项及其状态。
     * </p>
     *
     * @param listId 任务列表ID
     * @return 包含所有任务项的字符串
     */
    private String listTodoItems(String listId) {
        if (listId == null) {
            return "错误：必须提供'list_id'参数";
        }

        List<TodoItem> todoList = todoLists.get(listId);
        if (todoList == null) {
            return "错误：未找到ID为 '" + listId + "' 的任务列表";
        }

        if (todoList.isEmpty()) {
            return "任务列表为空";
        }

        StringBuilder result = new StringBuilder("任务列表 '" + listId + "':\n");
        for (TodoItem item : todoList) {
            result.append("- ").append(item.toString()).append("\n");
        }

        return result.toString();
    }

    /**
     * 移除任务项
     * <p>
     * 从指定任务列表中移除特定的任务项。
     * </p>
     *
     * @param listId     任务列表ID
     * @param parameters 包含任务项ID的参数
     * @return 操作结果字符串
     */
    private String removeTodoItem(String listId, JSONObject parameters) {
        if (listId == null) {
            return "错误：必须提供'list_id'参数";
        }

        List<TodoItem> todoList = todoLists.get(listId);
        if (todoList == null) {
            return "错误：未找到ID为 '" + listId + "' 的任务列表";
        }

        Integer itemId = parameters.getInteger("item_id");
        if (itemId == null) {
            return "错误：必须提供'item_id'参数";
        }

        TodoItem removedItem = null;
        for (int i = 0; i < todoList.size(); i++) {
            if (todoList.get(i).getId() == itemId) {
                removedItem = todoList.remove(i);
                break;
            }
        }

        if (removedItem == null) {
            return "错误：未找到ID为 '" + itemId + "' 的任务项";
        }

        return "已移除任务项: " + removedItem.toString();
    }

    /**
     * 任务项内部类
     * <p>
     * 表示一个具体的任务项，包含ID、标题、描述、完成状态和创建时间等信息。
     * </p>
     */
    private static class TodoItem {
        /**
         * 任务项唯一ID
         */
        private final int id;

        /**
         * 任务项标题
         */
        private final String title;

        /**
         * 任务项详细描述
         */
        private final String description;

        /**
         * 任务项完成状态
         */
        private boolean completed;

        /**
         * 任务项创建时间戳
         */
        private final long createdAt;

        /**
         * 构造一个新的任务项
         *
         * @param id          任务项ID
         * @param title       任务项标题
         * @param description 任务项描述
         */
        public TodoItem(int id, String title, String description) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.completed = false;
            this.createdAt = System.currentTimeMillis();
        }

        /**
         * 获取任务项ID
         *
         * @return 任务项ID
         */
        public int getId() {
            return id;
        }

        /**
         * 获取任务项标题
         *
         * @return 任务项标题
         */
        public String getTitle() {
            return title;
        }

        /**
         * 获取任务项描述
         *
         * @return 任务项描述
         */
        public String getDescription() {
            return description;
        }

        /**
         * 检查任务项是否已完成
         *
         * @return 如果任务已完成返回true，否则返回false
         */
        public boolean isCompleted() {
            return completed;
        }

        /**
         * 设置任务项完成状态
         *
         * @param completed 完成状态
         */
        public void setCompleted(boolean completed) {
            this.completed = completed;
        }

        /**
         * 获取任务项创建时间戳
         *
         * @return 创建时间戳
         */
        public long getCreatedAt() {
            return createdAt;
        }

        /**
         * 返回任务项的字符串表示
         *
         * @return 任务项的格式化字符串表示
         */
        @Override
        public String toString() {
            return String.format("[%d] %s %s %s",
                    id,
                    title,
                    description != null ? "(" + description + ")" : "",
                    completed ? "[完成]" : "[未完成]");
        }
    }
}
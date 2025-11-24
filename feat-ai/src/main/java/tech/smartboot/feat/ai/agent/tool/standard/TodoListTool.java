/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.agent.tool.standard;

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.agent.tool.ToolExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Todo列表工具，用于创建和管理结构化任务列表
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class TodoListTool implements ToolExecutor {
    
    private static final String NAME = "todo_list";
    private static final String DESCRIPTION = "创建和管理结构化任务列表，用于跟踪复杂工作流程的进度";
    
    // 存储任务列表的内存存储
    private final ConcurrentHashMap<String, List<TodoItem>> todoLists = new ConcurrentHashMap<>();
    private final AtomicInteger idGenerator = new AtomicInteger(0);
    
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
    
    @Override
    public String getName() {
        return NAME;
    }
    
    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
    
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
     */
    private String createTodoList(JSONObject parameters) {
        String listId = "todo_list_" + System.currentTimeMillis();
        todoLists.put(listId, new ArrayList<>());
        return "已创建新的任务列表，ID为: " + listId;
    }
    
    /**
     * 添加任务项
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
     */
    private static class TodoItem {
        private final int id;
        private final String title;
        private final String description;
        private boolean completed;
        private final long createdAt;
        
        public TodoItem(int id, String title, String description) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.completed = false;
            this.createdAt = System.currentTimeMillis();
        }
        
        public int getId() {
            return id;
        }
        
        public String getTitle() {
            return title;
        }
        
        public String getDescription() {
            return description;
        }
        
        public boolean isCompleted() {
            return completed;
        }
        
        public void setCompleted(boolean completed) {
            this.completed = completed;
        }
        
        public long getCreatedAt() {
            return createdAt;
        }
        
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
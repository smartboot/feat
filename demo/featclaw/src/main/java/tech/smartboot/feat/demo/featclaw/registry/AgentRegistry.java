/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.demo.featclaw.registry;

import org.yaml.snakeyaml.Yaml;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.demo.featclaw.config.AgentConfig;
import tech.smartboot.feat.demo.featclaw.config.ConfigManager;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Agent注册表 - 管理所有Agent配置
 * <p>
 * 从外部配置目录 ~/.featclaw/agents/ 加载YAML格式的Agent配置，
 * 并提供查询和管理功能。
 * </p>
 *
 * @author Feat Team
 * @version v2.0.0
 */
public class AgentRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentRegistry.class);
    
    /**
     * Agent配置存储 Map<agentName, AgentConfig>
     */
    private final Map<String, AgentConfig> agents = new ConcurrentHashMap<>();
    
    /**
     * 配置管理器
     */
    private final ConfigManager configManager;
    
    /**
     * YAML解析器
     */
    private final Yaml yaml;
    
    /**
     * 单例实例
     */
    private static volatile AgentRegistry instance;
    
    /**
     * 私有构造函数
     */
    private AgentRegistry() {
        this.configManager = ConfigManager.getInstance();
        this.yaml = new Yaml();
        loadAllAgents();
    }
    
    /**
     * 获取单例实例
     */
    public static AgentRegistry getInstance() {
        if (instance == null) {
            synchronized (AgentRegistry.class) {
                if (instance == null) {
                    instance = new AgentRegistry();
                }
            }
        }
        return instance;
    }
    
    /**
     * 加载所有Agent配置
     */
    private void loadAllAgents() {
        logger.info("开始从外部目录加载Agent配置...");
        
        File[] agentFiles = configManager.listAgentConfigFiles();
        
        if (agentFiles == null || agentFiles.length == 0) {
            logger.warn("未找到Agent配置文件");
            return;
        }
        
        for (File file : agentFiles) {
            loadAgent(file);
        }
        
        logger.info("Agent配置加载完成，共加载 {} 个Agent", agents.size());
    }
    
    /**
     * 加载单个Agent配置文件
     */
    private void loadAgent(File file) {
        try {
            String content = configManager.readYamlFile(file);
            if (content == null) {
                logger.warn("无法读取Agent配置文件: {}", file.getAbsolutePath());
                return;
            }
            
            // 解析YAML
            Map<String, Object> data = yaml.load(content);
            if (data == null) {
                logger.error("YAML解析失败: {}", file.getAbsolutePath());
                return;
            }
            
            // 转换为AgentConfig
            AgentConfig config = parseAgentConfig(data);
            
            if (config.getName() == null || config.getName().isEmpty()) {
                logger.error("Agent配置缺少name属性: {}", file.getAbsolutePath());
                return;
            }
            
            agents.put(config.getName(), config);
            logger.info("已加载Agent: {} ({})", config.getName(), config.getDisplayName());
            
        } catch (Exception e) {
            logger.error("加载Agent配置失败: {}", file.getAbsolutePath(), e);
        }
    }
    
    /**
     * 解析Agent配置
     */
    @SuppressWarnings("unchecked")
    private AgentConfig parseAgentConfig(Map<String, Object> data) {
        AgentConfig config = new AgentConfig();
        
        config.setName(getString(data, "name"));
        config.setDisplayName(getString(data, "displayName"));
        config.setDescription(getString(data, "description"));
        config.setRole(getString(data, "role"));
        config.setSystemPrompt(getString(data, "systemPrompt"));
        config.setSkills(getList(data, "skills"));
        config.setTools(getList(data, "tools"));
        config.setOrchestrator(getBoolean(data, "isOrchestrator", false));
        
        // 解析model配置
        Map<String, Object> modelData = (Map<String, Object>) data.get("model");
        if (modelData != null) {
            AgentConfig.ModelConfig modelConfig = new AgentConfig.ModelConfig();
            modelConfig.setTemperature(getFloat(modelData, "temperature", 0.7f));
            modelConfig.setMaxIterations(getInt(modelData, "maxIterations", 20));
            modelConfig.setModelName(getString(modelData, "modelName"));
            config.setModel(modelConfig);
        }
        
        // 解析memory配置
        Map<String, Object> memoryData = (Map<String, Object>) data.get("memory");
        if (memoryData != null) {
            AgentConfig.MemoryConfig memoryConfig = new AgentConfig.MemoryConfig();
            memoryConfig.setEnabled(getBoolean(memoryData, "enabled", false));
            memoryConfig.setTopK(getInt(memoryData, "topK", 5));
            config.setMemory(memoryConfig);
        }
        
        // 解析extra配置
        Map<String, Object> extraData = (Map<String, Object>) data.get("extra");
        if (extraData != null) {
            config.setExtra(extraData);
        }
        
        return config;
    }
    
    // 辅助方法
    
    private String getString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : null;
    }
    
    @SuppressWarnings("unchecked")
    private java.util.List<String> getList(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof java.util.List) {
            return (java.util.List<String>) value;
        }
        return null;
    }
    
    private boolean getBoolean(Map<String, Object> data, String key, boolean defaultValue) {
        Object value = data.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
    
    private int getInt(Map<String, Object> data, String key, int defaultValue) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
    
    private float getFloat(Map<String, Object> data, String key, float defaultValue) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        return defaultValue;
    }
    
    /**
     * 根据名称获取Agent配置
     *
     * @param name Agent名称
     * @return Agent配置，如果不存在返回null
     */
    public AgentConfig getAgent(String name) {
        return agents.get(name);
    }
    
    /**
     * 获取所有Agent配置
     *
     * @return 所有Agent配置的集合
     */
    public Collection<AgentConfig> getAllAgents() {
        return Collections.unmodifiableCollection(agents.values());
    }
    
    /**
     * 根据技能查找Agent
     *
     * @param skillName 技能名称
     * @return 支持该技能的所有Agent
     */
    public Collection<AgentConfig> findAgentsBySkill(String skillName) {
        return agents.values().stream()
                .filter(agent -> agent.getSkills() != null 
                        && agent.getSkills().contains(skillName))
                .collect(Collectors.toList());
    }
    
    /**
     * 查找调度器Agent
     *
     * @return 调度器Agent配置
     */
    public AgentConfig findOrchestrator() {
        return agents.values().stream()
                .filter(AgentConfig::isOrchestrator)
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 检查Agent是否存在
     *
     * @param name Agent名称
     * @return 是否存在
     */
    public boolean hasAgent(String name) {
        return agents.containsKey(name);
    }
    
    /**
     * 获取Agent数量
     *
     * @return Agent数量
     */
    public int getAgentCount() {
        return agents.size();
    }
    
    /**
     * 重新加载所有Agent配置
     */
    public void reload() {
        logger.info("重新加载Agent配置...");
        agents.clear();
        loadAllAgents();
    }
    
    /**
     * 获取所有Agent名称列表
     *
     * @return Agent名称数组
     */
    public String[] getAgentNames() {
        return agents.keySet().toArray(new String[0]);
    }
    
    /**
     * 打印Agent信息（用于调试）
     */
    public void printAgentInfo() {
        System.out.println("\n========================================");
        System.out.println("           Agent注册表信息               ");
        System.out.println("========================================\n");
        
        for (AgentConfig agent : agents.values()) {
            System.out.println("Agent: " + agent.getName());
            System.out.println("  显示名: " + agent.getDisplayName());
            System.out.println("  描述: " + agent.getDescription());
            System.out.println("  技能: " + (agent.getSkills() != null ? String.join(", ", agent.getSkills()) : "无"));
            System.out.println("  工具: " + (agent.getTools() != null ? String.join(", ", agent.getTools()) : "无"));
            System.out.println("  调度器: " + (agent.isOrchestrator() ? "是" : "否"));
            System.out.println();
        }
        
        System.out.println("共计: " + agents.size() + " 个Agent");
        System.out.println("========================================\n");
    }
    
    /**
     * 添加或更新Agent配置（运行时）
     *
     * @param config Agent配置
     */
    public void registerAgent(AgentConfig config) {
        agents.put(config.getName(), config);
        logger.info("注册Agent: {}", config.getName());
    }
    
    /**
     * 保存Agent配置到文件
     *
     * @param config Agent配置
     * @return 是否保存成功
     */
    public boolean saveAgentConfig(AgentConfig config) {
        try {
            File file = configManager.getAgentConfigFile(config.getName());
            
            // 构建YAML内容
            Map<String, Object> data = new java.util.HashMap<>();
            data.put("name", config.getName());
            data.put("displayName", config.getDisplayName());
            data.put("description", config.getDescription());
            data.put("role", config.getRole());
            data.put("systemPrompt", config.getSystemPrompt());
            data.put("skills", config.getSkills());
            data.put("tools", config.getTools());
            data.put("isOrchestrator", config.isOrchestrator());
            
            if (config.getModel() != null) {
                Map<String, Object> modelData = new java.util.HashMap<>();
                modelData.put("temperature", config.getModel().getTemperature());
                modelData.put("maxIterations", config.getModel().getMaxIterations());
                modelData.put("modelName", config.getModel().getModelName());
                data.put("model", modelData);
            }
            
            if (config.getMemory() != null) {
                Map<String, Object> memoryData = new java.util.HashMap<>();
                memoryData.put("enabled", config.getMemory().isEnabled());
                memoryData.put("topK", config.getMemory().getTopK());
                data.put("memory", memoryData);
            }
            
            String yamlContent = yaml.dumpAsMap(data);
            return configManager.writeYamlFile(file, yamlContent);
            
        } catch (Exception e) {
            logger.error("保存Agent配置失败: {}", config.getName(), e);
            return false;
        }
    }
}

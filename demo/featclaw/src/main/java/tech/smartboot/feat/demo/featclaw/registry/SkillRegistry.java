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
import tech.smartboot.feat.demo.featclaw.config.ConfigManager;
import tech.smartboot.feat.demo.featclaw.config.SkillConfig;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Skill注册表 - 管理所有Skill配置
 * <p>
 * 从外部配置目录 ~/.featclaw/skills/ 加载YAML格式的Skill配置，
 * 每个Skill一个独立目录，包含 skill.yaml 和相关文件。
 * </p>
 *
 * @author Feat Team
 * @version v2.0.0
 */
public class SkillRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(SkillRegistry.class);
    
    /**
     * Skill配置存储 Map<skillName, SkillConfig>
     */
    private final Map<String, SkillConfig> skills = new ConcurrentHashMap<>();
    
    /**
     * Skill目录映射 Map<skillName, skillDirectory>
     */
    private final Map<String, File> skillDirectories = new ConcurrentHashMap<>();
    
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
    private static volatile SkillRegistry instance;
    
    /**
     * 私有构造函数
     */
    private SkillRegistry() {
        this.configManager = ConfigManager.getInstance();
        this.yaml = new Yaml();
        loadAllSkills();
    }
    
    /**
     * 获取单例实例
     */
    public static SkillRegistry getInstance() {
        if (instance == null) {
            synchronized (SkillRegistry.class) {
                if (instance == null) {
                    instance = new SkillRegistry();
                }
            }
        }
        return instance;
    }
    
    /**
     * 加载所有Skill配置
     */
    private void loadAllSkills() {
        logger.info("开始从外部目录加载Skill配置...");
        
        File[] skillDirs = configManager.listSkillConfigDirs();
        
        if (skillDirs == null || skillDirs.length == 0) {
            logger.warn("未找到Skill配置目录");
            return;
        }
        
        for (File dir : skillDirs) {
            loadSkill(dir);
        }
        
        logger.info("Skill配置加载完成，共加载 {} 个Skill", skills.size());
    }
    
    /**
     * 加载单个Skill目录
     */
    private void loadSkill(File skillDir) {
        File skillFile = new File(skillDir, "skill.yaml");
        
        if (!skillFile.exists()) {
            logger.warn("Skill配置文件不存在: {}", skillFile.getAbsolutePath());
            return;
        }
        
        try {
            String content = configManager.readYamlFile(skillFile);
            if (content == null) {
                logger.warn("无法读取Skill配置文件: {}", skillFile.getAbsolutePath());
                return;
            }
            
            // 解析YAML
            Map<String, Object> data = yaml.load(content);
            if (data == null) {
                logger.error("YAML解析失败: {}", skillFile.getAbsolutePath());
                return;
            }
            
            // 转换为SkillConfig
            SkillConfig config = parseSkillConfig(data);
            
            if (config.getName() == null || config.getName().isEmpty()) {
                logger.error("Skill配置缺少name属性: {}", skillFile.getAbsolutePath());
                return;
            }
            
            skills.put(config.getName(), config);
            skillDirectories.put(config.getName(), skillDir);
            logger.info("已加载Skill: {} ({})", config.getName(), config.getDisplayName());
            
        } catch (Exception e) {
            logger.error("加载Skill配置失败: {}", skillDir.getAbsolutePath(), e);
        }
    }
    
    /**
     * 解析Skill配置
     */
    @SuppressWarnings("unchecked")
    private SkillConfig parseSkillConfig(Map<String, Object> data) {
        SkillConfig config = new SkillConfig();
        
        config.setName(getString(data, "name"));
        config.setDisplayName(getString(data, "displayName"));
        config.setDescription(getString(data, "description"));
        config.setCategory(getString(data, "category"));
        config.setKeywords(getList(data, "keywords"));
        config.setRequiredTools(getList(data, "requiredTools"));
        config.setPrompt(getString(data, "prompt"));
        config.setExamples(getList(data, "examples"));
        
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
    private List<String> getList(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof List) {
            return (List<String>) value;
        }
        return null;
    }
    
    /**
     * 根据名称获取Skill配置
     *
     * @param name Skill名称
     * @return Skill配置，如果不存在返回null
     */
    public SkillConfig getSkill(String name) {
        return skills.get(name);
    }
    
    /**
     * 获取所有Skill配置
     *
     * @return 所有Skill配置的集合
     */
    public Collection<SkillConfig> getAllSkills() {
        return Collections.unmodifiableCollection(skills.values());
    }
    
    /**
     * 根据类别获取Skill
     *
     * @param category 技能类别
     * @return 该类别的所有Skill
     */
    public Collection<SkillConfig> getSkillsByCategory(String category) {
        return skills.values().stream()
                .filter(skill -> category.equalsIgnoreCase(skill.getCategory()))
                .collect(Collectors.toList());
    }
    
    /**
     * 根据用户输入匹配最合适的Skill
     * <p>
     * 使用关键词匹配算法，计算输入与每个skill的匹配度
     * </p>
     *
     * @param userInput 用户输入
     * @return 最佳匹配的Skill，如果没有匹配返回null
     */
    public SkillConfig matchSkill(String userInput) {
        if (userInput == null || userInput.isEmpty()) {
            return null;
        }
        
        String lowerInput = userInput.toLowerCase();
        SkillConfig bestMatch = null;
        int bestScore = 0;
        
        for (SkillConfig skill : skills.values()) {
            int score = calculateMatchScore(lowerInput, skill);
            if (score > bestScore) {
                bestScore = score;
                bestMatch = skill;
            }
        }
        
        // 设置匹配阈值
        if (bestScore >= 1) {
            return bestMatch;
        }
        
        return null;
    }
    
    /**
     * 计算匹配分数
     */
    private int calculateMatchScore(String userInput, SkillConfig skill) {
        int score = 0;
        
        // 关键词匹配
        List<String> keywords = skill.getKeywords();
        if (keywords != null) {
            for (String keyword : keywords) {
                if (userInput.contains(keyword.toLowerCase())) {
                    score += 2;
                }
            }
        }
        
        // 技能名称匹配
        if (userInput.contains(skill.getName().toLowerCase())) {
            score += 3;
        }
        
        // 显示名称匹配
        if (skill.getDisplayName() != null && 
            userInput.contains(skill.getDisplayName().toLowerCase())) {
            score += 2;
        }
        
        // 描述匹配
        if (skill.getDescription() != null && 
            userInput.contains(skill.getDescription().toLowerCase())) {
            score += 1;
        }
        
        return score;
    }
    
    /**
     * 检查Skill是否存在
     *
     * @param name Skill名称
     * @return 是否存在
     */
    public boolean hasSkill(String name) {
        return skills.containsKey(name);
    }
    
    /**
     * 获取Skill数量
     *
     * @return Skill数量
     */
    public int getSkillCount() {
        return skills.size();
    }
    
    /**
     * 重新加载所有Skill配置
     */
    public void reload() {
        logger.info("重新加载Skill配置...");
        skills.clear();
        skillDirectories.clear();
        loadAllSkills();
    }
    
    /**
     * 获取指定工具所需的所有Skill
     *
     * @param toolName 工具名称
     * @return 需要该工具的所有Skill
     */
    public Collection<SkillConfig> getSkillsByTool(String toolName) {
        return skills.values().stream()
                .filter(skill -> skill.getRequiredTools() != null 
                        && skill.getRequiredTools().contains(toolName))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取Skill的配置目录
     *
     * @param skillName Skill名称
     * @return 配置目录，如果不存在返回null
     */
    public File getSkillDirectory(String skillName) {
        return skillDirectories.get(skillName);
    }
    
    /**
     * 打印Skill信息（用于调试）
     */
    public void printSkillInfo() {
        System.out.println("\n========================================");
        System.out.println("           Skill注册表信息               ");
        System.out.println("========================================\n");
        
        for (SkillConfig skill : skills.values()) {
            System.out.println("Skill: " + skill.getName());
            System.out.println("  显示名: " + skill.getDisplayName());
            System.out.println("  描述: " + skill.getDescription());
            System.out.println("  类别: " + skill.getCategory());
            System.out.println("  关键词: " + (skill.getKeywords() != null ? String.join(", ", skill.getKeywords()) : "无"));
            System.out.println("  所需工具: " + (skill.getRequiredTools() != null ? String.join(", ", skill.getRequiredTools()) : "无"));
            System.out.println();
        }
        
        System.out.println("共计: " + skills.size() + " 个Skill");
        System.out.println("========================================\n");
    }
    
    /**
     * 添加或更新Skill配置（运行时）
     *
     * @param config Skill配置
     * @param skillDir Skill目录
     */
    public void registerSkill(SkillConfig config, File skillDir) {
        skills.put(config.getName(), config);
        skillDirectories.put(config.getName(), skillDir);
        logger.info("注册Skill: {}", config.getName());
    }
    
    /**
     * 保存Skill配置到文件
     *
     * @param config Skill配置
     * @return 是否保存成功
     */
    public boolean saveSkillConfig(SkillConfig config) {
        try {
            File skillDir = skillDirectories.get(config.getName());
            if (skillDir == null) {
                // 创建新目录
                skillDir = configManager.getSkillConfigDir(config.getName());
                skillDir.mkdirs();
                skillDirectories.put(config.getName(), skillDir);
            }
            
            File skillFile = new File(skillDir, "skill.yaml");
            
            // 构建YAML内容
            Map<String, Object> data = new java.util.HashMap<>();
            data.put("name", config.getName());
            data.put("displayName", config.getDisplayName());
            data.put("description", config.getDescription());
            data.put("category", config.getCategory());
            data.put("keywords", config.getKeywords());
            data.put("requiredTools", config.getRequiredTools());
            data.put("prompt", config.getPrompt());
            data.put("examples", config.getExamples());
            
            String yamlContent = yaml.dumpAsMap(data);
            return configManager.writeYamlFile(skillFile, yamlContent);
            
        } catch (Exception e) {
            logger.error("保存Skill配置失败: {}", config.getName(), e);
            return false;
        }
    }
}

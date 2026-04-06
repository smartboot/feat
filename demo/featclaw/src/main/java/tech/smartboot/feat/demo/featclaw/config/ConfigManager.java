/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.demo.featclaw.config;

import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * 配置管理器 - 管理外部配置目录
 * <p>
 * 负责：
 * 1. 确定配置目录位置（~/.featclaw/）
 * 2. 首次运行时从resources复制默认配置
 * 3. 提供配置的读取和写入接口
 * </p>
 *
 * @author Feat Team
 * @version v1.0.0
 */
public class ConfigManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    
    /**
     * 配置根目录名称
     */
    private static final String CONFIG_DIR_NAME = ".featclaw";
    
    /**
     * Agent配置子目录
     */
    private static final String AGENTS_DIR = "agents";
    
    /**
     * Skill配置子目录
     */
    private static final String SKILLS_DIR = "skills";
    
    /**
     * 默认配置资源路径
     */
    private static final String DEFAULT_CONFIG_RESOURCE = "default-config";
    
    /**
     * 用户主目录
     */
    private final String userHome;
    
    /**
     * 配置根目录
     */
    private final File configRootDir;
    
    /**
     * Agent配置目录
     */
    private final File agentsDir;
    
    /**
     * Skill配置目录
     */
    private final File skillsDir;
    
    /**
     * 单例实例
     */
    private static volatile ConfigManager instance;
    
    /**
     * 私有构造函数
     */
    private ConfigManager() {
        this.userHome = System.getProperty("user.home");
        this.configRootDir = new File(userHome, CONFIG_DIR_NAME);
        this.agentsDir = new File(configRootDir, AGENTS_DIR);
        this.skillsDir = new File(configRootDir, SKILLS_DIR);
        
        initialize();
    }
    
    /**
     * 获取单例实例
     */
    public static ConfigManager getInstance() {
        if (instance == null) {
            synchronized (ConfigManager.class) {
                if (instance == null) {
                    instance = new ConfigManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * 初始化配置目录
     */
    private void initialize() {
        // 创建配置目录
        if (!configRootDir.exists()) {
            configRootDir.mkdirs();
            logger.info("创建配置目录: {}", configRootDir.getAbsolutePath());
        }
        
        if (!agentsDir.exists()) {
            agentsDir.mkdirs();
            logger.info("创建Agent配置目录: {}", agentsDir.getAbsolutePath());
        }
        
        if (!skillsDir.exists()) {
            skillsDir.mkdirs();
            logger.info("创建Skill配置目录: {}", skillsDir.getAbsolutePath());
        }
        
        // 首次运行检查：如果Agent目录为空，复制默认配置
        if (isFirstRun()) {
            logger.info("首次运行，复制默认配置...");
            copyDefaultConfig();
        }
    }
    
    /**
     * 检查是否首次运行
     */
    private boolean isFirstRun() {
        File[] agentFiles = agentsDir.listFiles();
        return agentFiles == null || agentFiles.length == 0;
    }
    
    /**
     * 从resources复制默认配置到外部目录
     */
    private void copyDefaultConfig() {
        try {
            // 复制Agent配置
            copyResourceDirectory(DEFAULT_CONFIG_RESOURCE + "/" + AGENTS_DIR, agentsDir);
            
            // 复制Skill配置
            copyResourceDirectory(DEFAULT_CONFIG_RESOURCE + "/" + SKILLS_DIR, skillsDir);
            
            logger.info("默认配置复制完成");
            
        } catch (Exception e) {
            logger.error("复制默认配置失败", e);
        }
    }
    
    /**
     * 复制资源目录到目标目录
     */
    private void copyResourceDirectory(String resourcePath, File targetDir) throws IOException {
        // 获取资源目录的URL
        java.net.URL resourceUrl = getClass().getClassLoader().getResource(resourcePath);
        if (resourceUrl == null) {
            logger.warn("资源目录不存在: {}", resourcePath);
            return;
        }
        
        // 如果是jar包内的资源，需要特殊处理
        if (resourceUrl.getProtocol().equals("jar")) {
            copyJarResourceDirectory(resourcePath, targetDir);
        } else {
            // 文件系统中的资源
            File sourceDir = new File(resourceUrl.getFile());
            if (sourceDir.exists() && sourceDir.isDirectory()) {
                copyDirectory(sourceDir, targetDir);
            }
        }
    }
    
    /**
     * 复制jar包内的资源目录
     */
    private void copyJarResourceDirectory(String resourcePath, File targetDir) throws IOException {
        // 列出资源目录中的内容
        try (java.io.InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                return;
            }
        }
        
        // 使用类加载器遍历资源
        // 注意：在jar包中，我们需要知道具体的文件列表
        // 这里采用从预定义列表复制的方式
        if (resourcePath.contains(AGENTS_DIR)) {
            copyDefaultAgents();
        } else if (resourcePath.contains(SKILLS_DIR)) {
            copyDefaultSkills();
        }
    }
    
    /**
     * 复制默认Agent配置
     */
    private void copyDefaultAgents() {
        String[] agentFiles = {
            "project-analyzer.yaml",
            "code-generator.yaml",
            "command-executor.yaml",
            "research-assistant.yaml",
            "orchestrator.yaml"
        };
        
        for (String fileName : agentFiles) {
            copyResourceFile(DEFAULT_CONFIG_RESOURCE + "/" + AGENTS_DIR + "/" + fileName, 
                new File(agentsDir, fileName));
        }
    }
    
    /**
     * 复制默认Skill配置
     */
    private void copyDefaultSkills() {
        String[] skillDirs = {
            "project-analysis",
            "code-generation",
            "shell-execution",
            "web-search",
            "agent-coordination"
        };
        
        for (String dirName : skillDirs) {
            File skillDir = new File(skillsDir, dirName);
            skillDir.mkdirs();
            
            // 复制skill.yaml
            String resourcePath = DEFAULT_CONFIG_RESOURCE + "/" + SKILLS_DIR + "/" + dirName + "/skill.yaml";
            File targetFile = new File(skillDir, "skill.yaml");
            copyResourceFile(resourcePath, targetFile);
            
            // 复制README.md
            resourcePath = DEFAULT_CONFIG_RESOURCE + "/" + SKILLS_DIR + "/" + dirName + "/README.md";
            targetFile = new File(skillDir, "README.md");
            copyResourceFile(resourcePath, targetFile);
        }
    }
    
    /**
     * 复制单个资源文件
     */
    private void copyResourceFile(String resourcePath, File targetFile) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                logger.warn("资源文件不存在: {}", resourcePath);
                return;
            }
            
            Files.copy(is, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            logger.debug("复制文件: {} -> {}", resourcePath, targetFile.getAbsolutePath());
            
        } catch (IOException e) {
            logger.error("复制资源文件失败: {}", resourcePath, e);
        }
    }
    
    /**
     * 复制整个目录
     */
    private void copyDirectory(File sourceDir, File targetDir) throws IOException {
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        
        File[] files = sourceDir.listFiles();
        if (files == null) {
            return;
        }
        
        for (File file : files) {
            File targetFile = new File(targetDir, file.getName());
            if (file.isDirectory()) {
                copyDirectory(file, targetFile);
            } else {
                Files.copy(file.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
    
    /**
     * 获取配置根目录
     */
    public File getConfigRootDir() {
        return configRootDir;
    }
    
    /**
     * 获取Agent配置目录
     */
    public File getAgentsDir() {
        return agentsDir;
    }
    
    /**
     * 获取Skill配置目录
     */
    public File getSkillsDir() {
        return skillsDir;
    }
    
    /**
     * 获取Agent配置文件
     */
    public File getAgentConfigFile(String agentName) {
        return new File(agentsDir, agentName + ".yaml");
    }
    
    /**
     * 获取Skill配置目录
     */
    public File getSkillConfigDir(String skillName) {
        return new File(skillsDir, skillName);
    }
    
    /**
     * 获取Skill配置文件
     */
    public File getSkillConfigFile(String skillName) {
        return new File(getSkillConfigDir(skillName), "skill.yaml");
    }
    
    /**
     * 读取YAML文件内容
     */
    public String readYamlFile(File file) {
        if (!file.exists()) {
            return null;
        }
        
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            logger.error("读取YAML文件失败: {}", file.getAbsolutePath(), e);
            return null;
        }
        
        return content.toString();
    }
    
    /**
     * 写入YAML文件
     */
    public boolean writeYamlFile(File file, String content) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
            return true;
        } catch (IOException e) {
            logger.error("写入YAML文件失败: {}", file.getAbsolutePath(), e);
            return false;
        }
    }
    
    /**
     * 列出Agent配置文件
     */
    public File[] listAgentConfigFiles() {
        return agentsDir.listFiles((dir, name) -> name.endsWith(".yaml") || name.endsWith(".yml"));
    }
    
    /**
     * 列出Skill配置目录
     */
    public File[] listSkillConfigDirs() {
        return skillsDir.listFiles(File::isDirectory);
    }
    
    /**
     * 重置为默认配置
     */
    public void resetToDefault() {
        logger.info("重置配置为默认状态...");
        
        // 删除现有配置
        deleteDirectory(agentsDir);
        deleteDirectory(skillsDir);
        
        // 重新创建目录
        agentsDir.mkdirs();
        skillsDir.mkdirs();
        
        // 复制默认配置
        copyDefaultConfig();
        
        logger.info("配置重置完成");
    }
    
    /**
     * 删除目录及其内容
     */
    private void deleteDirectory(File dir) {
        if (!dir.exists()) {
            return;
        }
        
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }
    
    /**
     * 打印配置信息
     */
    public void printConfigInfo() {
        System.out.println("\n========================================");
        System.out.println("           配置目录信息                  ");
        System.out.println("========================================\n");
        System.out.println("配置根目录: " + configRootDir.getAbsolutePath());
        System.out.println("Agent目录:  " + agentsDir.getAbsolutePath());
        System.out.println("Skill目录:  " + skillsDir.getAbsolutePath());
        
        File[] agentFiles = listAgentConfigFiles();
        System.out.println("\n已加载Agent配置: " + (agentFiles != null ? agentFiles.length : 0) + " 个");
        if (agentFiles != null) {
            for (File file : agentFiles) {
                System.out.println("  - " + file.getName());
            }
        }
        
        File[] skillDirs = listSkillConfigDirs();
        System.out.println("\n已加载Skill配置: " + (skillDirs != null ? skillDirs.length : 0) + " 个");
        if (skillDirs != null) {
            for (File dir : skillDirs) {
                System.out.println("  - " + dir.getName());
            }
        }
        
        System.out.println("\n========================================\n");
    }
}

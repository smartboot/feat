/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.demo.mapper;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import tech.smartboot.feat.cloud.FeatCloud;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.Param;
import tech.smartboot.feat.cloud.annotation.PostConstruct;
import tech.smartboot.feat.cloud.annotation.RequestMapping;

import java.io.IOException;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 3/10/25
 */
@Controller
public class FeatOrmDemo {

    @Autowired
    private SqlSessionFactory sessionFactory;

    @Autowired
    private DemoMapper demoMapper;

    @Bean("sessionFactory")
    public SqlSessionFactory sessionFactory() throws IOException {
        return new SqlSessionFactoryBuilder().build(Resources.getResourceAsStream("mybatis/mybatis-config.xml"));
    }

    @PostConstruct
    public void init() throws IOException {
        // 初始化数据库
        try (SqlSession session = sessionFactory.openSession()) {
            ScriptRunner runner = new ScriptRunner(session.getConnection());
            runner.setLogWriter(null);
            runner.runScript(Resources.getResourceAsReader("mybatis/ddl/schema.sql"));
        }
    }

    @RequestMapping("/getUser")
    public User test(@Param(value = "username") String username) {
        return demoMapper.selectByUsername(username);
    }

    public void setSessionFactory(SqlSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void setDemoMapper(DemoMapper demoMapper) {
        this.demoMapper = demoMapper;
    }

    public static void main(String[] args) {
        FeatCloud.cloudServer(props -> props.setPackages("tech.smartboot.feat.demo.mapper")).listen();

    }
}

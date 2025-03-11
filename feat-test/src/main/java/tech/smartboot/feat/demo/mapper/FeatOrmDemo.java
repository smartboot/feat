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
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import tech.smartboot.feat.cloud.FeatCloud;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.Param;
import tech.smartboot.feat.cloud.annotation.RequestMapping;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author 三刀
 * @version v1.0 3/10/25
 */
@Controller
public class FeatOrmDemo {

    @Bean("sessionFactory")
    public SqlSessionFactory sessionFactory() throws IOException {
        InputStream inputStream = Resources.getResourceAsStream("mybatis/mybatis-config.xml");
        SqlSessionFactory sessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        // 初始化数据库(可选)
        ScriptRunner runner = new ScriptRunner(sessionFactory.openSession().getConnection());
        runner.setLogWriter(null);
        runner.runScript(Resources.getResourceAsReader("mybatis/ddl/schema.sql"));
        return sessionFactory;
    }

    @Autowired
    private DemoMapper demoMapper;

    @RequestMapping("/test")
    public User test(@Param(value = "name") String username) {
        return demoMapper.selectById(username);
    }

    public DemoMapper getDemoMapper() {
        return demoMapper;
    }

    public void setDemoMapper(DemoMapper demoMapper) {
        this.demoMapper = demoMapper;
    }

    public static void main(String[] args) {
        FeatCloud.cloudServer(props -> props.setPackages("tech.smartboot.feat.demo.mapper")).listen();

    }
}

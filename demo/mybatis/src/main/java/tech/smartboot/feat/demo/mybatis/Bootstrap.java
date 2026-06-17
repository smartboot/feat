/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.demo.mybatis;

import tech.smartboot.feat.cloud.FeatCloud;
import tech.smartboot.feat.cloud.annotation.Bean;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

@Bean
public class Bootstrap {
    public static void main(String[] args) throws Exception {
        Class.forName("org.h2.Driver");
        try (Connection conn = DriverManager.getConnection("jdbc:h2:mem:feat;DB_CLOSE_DELAY=-1", "sa", "");
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS user_info (" +
                    "username VARCHAR(32) NOT NULL PRIMARY KEY," +
                    "password VARCHAR(128) NOT NULL," +
                    "desc VARCHAR(256)," +
                    "role VARCHAR(32)," +
                    "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "edit_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            stmt.execute("MERGE INTO user_info(username, password, role, desc) VALUES('admin', 'admin123', 'admin', 'admin user')");
            stmt.execute("MERGE INTO user_info(username, password, role, desc) VALUES('user1', 'password1', 'user', 'normal user')");
        }
        FeatCloud.cloudServer().listen();
    }
}

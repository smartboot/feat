-- 用户信息
CREATE TABLE IF NOT EXISTS user_info
(
    username    varchar(32)  NOT NULL COMMENT '用户名',
    password    varchar(128) NOT NULL COMMENT '密码',
    `desc`      varchar(256) COMMENT '备注',
    role        varchar(32) COMMENT '角色',
    create_time timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    edit_time   timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX       idx_user_password (username, password),
    PRIMARY KEY (username)
);

insert ignore into user_info(username, password, role, `desc`)values ('feat', 'feat', 'admin', '超级账户');

-- 插入更多测试数据
insert ignore into user_info(username, password, role, `desc`)values ('admin', 'admin123', 'admin', '管理员用户');
insert ignore into user_info(username, password, role, `desc`)values ('user1', 'password1', 'user', '普通用户1');
insert ignore into user_info(username, password, role, `desc`)values ('user2', 'password2', 'user', '普通用户2');
insert ignore into user_info(username, password, role, `desc`)values ('guest', 'guest123', 'guest', '访客用户');
insert ignore into user_info(username, password, role, `desc`)values ('developer', 'dev123', 'developer', '开发人员');

CREATE TABLE IF NOT EXISTS `user`(username varchar(32) NOT NULL COMMENT '用户名',`password`     varchar(128) NOT NULL COMMENT '密码',email        varchar(64)  NULL COMMENT '邮箱',phone        varchar(20)  NULL COMMENT '手机号');

INSERT INTO `user`(username, `password`, email, phone) VALUES('admin', '$2a$10$ExampleHash', 'admin@example.com', '13800000000');
INSERT INTO `user`(username, `password`, email, phone) VALUES('test_user', '$2a$10$ExampleHash', 'user@example.com', '13912345678');
INSERT INTO `user`(username, `password`, email, phone) VALUES('guest', '$2a$10$ExampleHash','a', 'c');

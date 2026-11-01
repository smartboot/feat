package tech.smartboot.feat.demo.mybatis.service;

import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.demo.mybatis.entity.User;
import tech.smartboot.feat.demo.mybatis.mapper.UserMapper;

import java.util.List;

@Bean
public class UserService {

    @Autowired
    private UserMapper userMapper;

    public User findByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    public List<User> findAll() {
        return userMapper.selectAll();
    }

    public List<User> findByRole(String role) {
        return userMapper.selectByRole(role);
    }

    /**
     * 写方法不要 try/catch 吞掉异常。
     * <p>
     * 吞掉之后调用方拿到 false，以为「这次没成功，重试一次就好」，而实际上事务早就该回滚了。
     * 回滚的唯一触发条件是异常穿出事务边界；要返回业务错误，让异常抛到 Controller 层翻译成状态码。
     */
    public boolean insert(User user) {
        return userMapper.insert(user) > 0;
    }

    public boolean update(User user) {
        return userMapper.update(user) > 0;
    }

    public boolean deleteByUsername(String username) {
        return userMapper.deleteByUsername(username) > 0;
    }
}
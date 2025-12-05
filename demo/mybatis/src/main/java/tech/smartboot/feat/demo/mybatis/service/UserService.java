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

    public boolean insert(User user) {
        try {
            return userMapper.insert(user) > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean update(User user) {
        try {
            return userMapper.update(user) > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteByUsername(String username) {
        try {
            return userMapper.deleteByUsername(username) > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
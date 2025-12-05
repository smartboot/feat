package tech.smartboot.feat.demo.mybatis.controller;

import tech.smartboot.feat.cloud.RestResult;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.PathParam;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.cloud.annotation.RequestMethod;
import tech.smartboot.feat.demo.mybatis.entity.User;
import tech.smartboot.feat.demo.mybatis.service.UserService;

import java.util.List;

@Controller
public class UserController {

    @Autowired
    private UserService userService;

    @RequestMapping("/users")
    public RestResult<List<User>> getAllUsers() {
        List<User> users = userService.findAll();
        return RestResult.ok(users);
    }

    @RequestMapping("/users/{username}")
    public RestResult<User> getUserByUsername(@PathParam("username") String username) {
        User user = userService.findByUsername(username);
        if (user != null) {
            return RestResult.ok(user);
        } else {
            return RestResult.fail("User not found");
        }
    }

    @RequestMapping("/users/role/{role}")
    public RestResult<List<User>> getUsersByRole(@PathParam("role") String role) {
        List<User> users = userService.findByRole(role);
        return RestResult.ok(users);
    }

    @RequestMapping(value = "/users", method = RequestMethod.POST)
    public RestResult<String> createUser(User user) {
        boolean success = userService.insert(user);
        if (success) {
            return RestResult.ok("User created successfully");
        } else {
            return RestResult.fail("Failed to create user");
        }
    }

    @RequestMapping(value = "/users", method = RequestMethod.PUT)
    public RestResult<String> updateUser(User user) {
        boolean success = userService.update(user);
        if (success) {
            return RestResult.ok("User updated successfully");
        } else {
            return RestResult.fail("Failed to update user");
        }
    }

    @RequestMapping(value = "/users/{username}", method = RequestMethod.DELETE)
    public RestResult<String> deleteUser(@PathParam("username") String username) {
        boolean success = userService.deleteByUsername(username);
        if (success) {
            return RestResult.ok("User deleted successfully");
        } else {
            return RestResult.fail("Failed to delete user");
        }
    }
}
package tech.smartboot.feat.demo.mybatis.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import tech.smartboot.feat.demo.mybatis.entity.User;

import java.util.List;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM user_info WHERE username = #{username}")
    @Results({
            @Result(property = "username", column = "username"),
            @Result(property = "password", column = "password"),
            @Result(property = "desc", column = "desc"),
            @Result(property = "role", column = "role"),
            @Result(property = "createTime", column = "create_time"),
            @Result(property = "editTime", column = "edit_time")
    })
    User selectByUsername(@Param("username") String username);

    @Select("SELECT * FROM user_info")
    @Results({
            @Result(property = "username", column = "username"),
            @Result(property = "password", column = "password"),
            @Result(property = "desc", column = "desc"),
            @Result(property = "role", column = "role"),
            @Result(property = "createTime", column = "create_time"),
            @Result(property = "editTime", column = "edit_time")
    })
    List<User> selectAll();

    @Insert("INSERT INTO user_info(username, password, `desc`, role) VALUES(#{username}, #{password}, #{desc}, #{role})")
    int insert(User user);

    @Update("UPDATE user_info SET password=#{password}, `desc`=#{desc}, role=#{role} WHERE username=#{username}")
    int update(User user);

    @Delete("DELETE FROM user_info WHERE username = #{username}")
    int deleteByUsername(@Param("username") String username);

    @Select("SELECT * FROM user_info WHERE role = #{role}")
    @Results({
            @Result(property = "username", column = "username"),
            @Result(property = "password", column = "password"),
            @Result(property = "desc", column = "desc"),
            @Result(property = "role", column = "role"),
            @Result(property = "createTime", column = "create_time"),
            @Result(property = "editTime", column = "edit_time")
    })
    List<User> selectByRole(@Param("role") String role);
}
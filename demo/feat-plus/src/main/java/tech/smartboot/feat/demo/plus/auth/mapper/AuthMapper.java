package tech.smartboot.feat.demo.plus.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tech.smartboot.feat.demo.plus.auth.model.AuthAccount;

@Mapper
public interface AuthMapper {

    @Select("SELECT id, username, password_hash, display_name, role_code, status "
            + "FROM sys_user WHERE username = #{username}")
    AuthAccount selectByUsername(@Param("username") String username);

    @Select("SELECT id, username, password_hash, display_name, role_code, status "
            + "FROM sys_user WHERE id = #{id}")
    AuthAccount selectById(@Param("id") long id);
}

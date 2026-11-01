package tech.smartboot.feat.demo.plus.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tech.smartboot.feat.demo.plus.auth.model.AuthAccount;

/**
 * 登录态读取。两条查询都带 is_deleted = 0：
 * 已软删除的账号既不能登录，也不能再用旧令牌拿到 profile。
 */
@Mapper
public interface AuthMapper {

    @Select("SELECT id, username, password_hash, display_name, role_code, status "
            + "FROM sys_user WHERE username = #{username} AND is_deleted = 0")
    AuthAccount selectByUsername(@Param("username") String username);

    @Select("SELECT id, username, password_hash, display_name, role_code, status "
            + "FROM sys_user WHERE id = #{id} AND is_deleted = 0")
    AuthAccount selectById(@Param("id") long id);
}

package tech.smartboot.feat.demo.plus.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tech.smartboot.feat.demo.plus.auth.model.AuthAccount;

/**
 * 登录态读取。两条查询都带 is_deleted = 0：
 * 已软删除的账号既不能登录，也不能再用旧令牌拿到 profile。
 * <p>
 * 角色权限走左连接一次性带出，避免每个请求多打一次库；
 * 角色被删时 LEFT JOIN 为 null，由 AuthAccount.effectivePermissions 按「无权限」处理。
 */
@Mapper
public interface AuthMapper {

    @Select("SELECT u.id, u.username, u.password_hash, u.display_name, u.role_code, u.status, "
            + "r.name AS role_name, r.status AS role_status, r.permissions AS permissions "
            + "FROM sys_user u LEFT JOIN sys_role r ON r.code = u.role_code AND r.is_deleted = 0 "
            + "WHERE u.username = #{username} AND u.is_deleted = 0")
    AuthAccount selectByUsername(@Param("username") String username);

    @Select("SELECT u.id, u.username, u.password_hash, u.display_name, u.role_code, u.status, "
            + "r.name AS role_name, r.status AS role_status, r.permissions AS permissions "
            + "FROM sys_user u LEFT JOIN sys_role r ON r.code = u.role_code AND r.is_deleted = 0 "
            + "WHERE u.id = #{id} AND u.is_deleted = 0")
    AuthAccount selectById(@Param("id") long id);
}

package tech.smartboot.feat.demo.plus.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import tech.smartboot.feat.demo.plus.model.AuthAccount;
import tech.smartboot.feat.demo.plus.model.AuthRole;

/**
 * 登录态读取。
 * <p>
 * 账号与角色分两次查，而不是一条 LEFT JOIN：
 * 多角色下人会被 join 成多行，一条查询要么需要 GROUP BY 聚合权限文本（H2 与 MySQL 写法不一致），
 * 要么让调用方处理重复行。分两次查逻辑直白，总代价仍然是一个请求两次查询。
 * <p>
 * 所有查询都带 is_deleted = 0：已软删除的账号既不能登录，也不能再用旧令牌拿到 profile。
 */
@Mapper
public interface AuthMapper {

    @Select("SELECT u.id, u.username, u.password_hash, u.display_name, u.department_id, u.status "
            + "FROM sys_user u WHERE u.username = #{username} AND u.is_deleted = 0")
    AuthAccount selectByUsername(@Param("username") String username);

    @Select("SELECT u.id, u.username, u.display_name, u.department_id, u.status "
            + "FROM sys_user u WHERE u.id = #{id} AND u.is_deleted = 0")
    AuthAccount selectById(@Param("id") long id);

    /**
     * 账号身上的角色。停用的角色也查出来，由 AuthAccount 判断是否生效——
     * 这样「角色被停用后权限消失」与「角色被删除后关联消失」两种情形有各自的处理路径。
     */
    @Select("SELECT r.code, r.name, r.status, r.permissions, r.department_scope "
            + "FROM sys_role r "
            + "JOIN sys_user_role ur ON ur.role_code = r.code AND ur.is_deleted = 0 "
            + "WHERE ur.user_id = #{userId} AND r.is_deleted = 0 ORDER BY r.id")
    List<AuthRole> selectRolesByUserId(@Param("userId") long userId);

    /**
     * 「自定义」数据范围的部门集合。roleCodes 必须非空，否则 IN () 不是合法 SQL。
     */
    @Select("<script>SELECT department_id FROM sys_role_dept WHERE is_deleted = 0 AND role_code IN "
            + "<foreach collection=\"roleCodes\" item=\"code\" open=\"(\" separator=\",\" close=\")\">"
            + "#{code}</foreach></script>")
    List<Long> selectCustomDepartmentIds(@Param("roleCodes") List<String> roleCodes);
}

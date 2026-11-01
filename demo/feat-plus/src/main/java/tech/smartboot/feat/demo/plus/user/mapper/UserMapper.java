package tech.smartboot.feat.demo.plus.user.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import tech.smartboot.feat.demo.plus.common.SqlFragments;
import tech.smartboot.feat.demo.plus.user.model.UserAccount;
import tech.smartboot.feat.demo.plus.user.model.UserRoleRow;

/**
 * 用户档案读写。
 * <p>
 * 列表类查询不返回 password_hash，避免哈希进入任何 HTTP 响应。
 * 注意：Feat 的注解处理器不支持 @Mapper 接口里出现常量字段，SQL 片段统一放在 SqlFragments。
 * <p>
 * 软删除约定：每条读写 SQL 都显式带 is_deleted = 0，删除只置位不物理删行。
 * 部门名称通过 LEFT JOIN sys_department 带出，因此按部门筛选传的是 id 集合（含下级）。
 * <p>
 * 数据权限通过 scopeDepartmentIds / selfOnlyUserId 两个参数落到 SQL 上，
 * 两者都由 AccessContext 计算，Service 层负责把「空集合」翻译成不可能的 id。
 */
@Mapper
public interface UserMapper {

    @Select("<script>SELECT " + SqlFragments.USER_COLUMNS + SqlFragments.USER_FROM + SqlFragments.USER_FILTER
            + " ORDER BY u.id DESC LIMIT #{limit} OFFSET #{offset}</script>")
    List<UserAccount> selectPage(@Param("keywordLike") String keywordLike,
                                 @Param("status") Integer status,
                                 @Param("roleCode") String roleCode,
                                 @Param("departmentIds") List<Long> departmentIds,
                                 @Param("scopeDepartmentIds") List<Long> scopeDepartmentIds,
                                 @Param("selfOnlyUserId") Long selfOnlyUserId,
                                 @Param("limit") int limit,
                                 @Param("offset") int offset);

    @Select("<script>SELECT COUNT(*)" + SqlFragments.USER_FROM + SqlFragments.USER_FILTER + "</script>")
    long countPage(@Param("keywordLike") String keywordLike,
                   @Param("status") Integer status,
                   @Param("roleCode") String roleCode,
                   @Param("departmentIds") List<Long> departmentIds,
                   @Param("scopeDepartmentIds") List<Long> scopeDepartmentIds,
                   @Param("selfOnlyUserId") Long selfOnlyUserId);

    /**
     * 下拉选项用的全量列表，同样受数据范围约束——否则「仅本人」的账号
     * 能通过下拉框拿到全公司名单，等于绕过了列表过滤。
     */
    @Select("<script>SELECT " + SqlFragments.USER_COLUMNS + SqlFragments.USER_FROM
            + SqlFragments.USER_SCOPE_FILTER + " ORDER BY u.id DESC</script>")
    List<UserAccount> selectAll(@Param("scopeDepartmentIds") List<Long> scopeDepartmentIds,
                                @Param("selfOnlyUserId") Long selfOnlyUserId);

    @Select("SELECT " + SqlFragments.USER_COLUMNS + ", u.password_hash" + SqlFragments.USER_FROM
            + " WHERE u.id = #{id} AND u.is_deleted = 0")
    UserAccount selectById(@Param("id") long id);

    @Select("SELECT " + SqlFragments.USER_COLUMNS + ", u.password_hash" + SqlFragments.USER_FROM
            + " WHERE u.username = #{username} AND u.is_deleted = 0")
    UserAccount selectByUsername(@Param("username") String username);

    @Select("SELECT COUNT(*) FROM sys_user WHERE LOWER(username) = LOWER(#{username}) "
            + "AND id <> #{excludeId} AND is_deleted = 0")
    long countByUsername(@Param("username") String username, @Param("excludeId") long excludeId);

    @Select("SELECT COUNT(*) FROM sys_user WHERE LOWER(email) = LOWER(#{email}) "
            + "AND id <> #{excludeId} AND is_deleted = 0")
    long countByEmail(@Param("email") String email, @Param("excludeId") long excludeId);

    /**
     * 至少保留一个「已启用且挂着 ADMIN 角色」的账号。多角色下要 DISTINCT，
     * 否则同一行被 join 出多条会重复计数。
     */
    @Select("SELECT COUNT(DISTINCT u.id) FROM sys_user u "
            + "JOIN sys_user_role ur ON ur.user_id = u.id AND ur.is_deleted = 0 "
            + "WHERE ur.role_code = 'ADMIN' AND u.status = 1 AND u.is_deleted = 0")
    long countEnabledAdmins();

    /** 该账号是否挂着指定角色，供「保护最后一个管理员」的判定使用。 */
    @Select("SELECT COUNT(*) FROM sys_user_role WHERE user_id = #{userId} "
            + "AND role_code = #{roleCode} AND is_deleted = 0")
    long countUserRole(@Param("userId") long userId, @Param("roleCode") String roleCode);

    @Select("<script>SELECT ur.user_id, ur.role_code, r.name AS role_name "
            + "FROM sys_user_role ur JOIN sys_role r ON r.code = ur.role_code AND r.is_deleted = 0 "
            + "WHERE ur.is_deleted = 0 AND ur.user_id IN "
            + "<foreach collection=\"userIds\" item=\"id\" open=\"(\" separator=\",\" close=\")\">#{id}</foreach>"
            + " ORDER BY r.id</script>")
    List<UserRoleRow> selectRolesByUserIds(@Param("userIds") List<Long> userIds);

    @Select("SELECT COUNT(*) FROM sys_user WHERE is_deleted = 0")
    long countAll();

    @Insert("INSERT INTO sys_user (username, password_hash, display_name, department_id, email, status, "
            + "is_deleted, created_time, edit_time, creator, editor) "
            + "VALUES (#{username}, #{passwordHash}, #{displayName}, #{departmentId}, #{email}, "
            + "#{status}, #{isDeleted}, #{createdTime}, #{editTime}, #{creator}, #{editor})")
    int insert(UserAccount user);

    @Update("UPDATE sys_user SET username = #{username}, display_name = #{displayName}, "
            + "department_id = #{departmentId}, email = #{email}, status = #{status}, "
            + "edit_time = #{editTime}, editor = #{editor} "
            + "WHERE id = #{id} AND is_deleted = 0")
    int updateProfile(UserAccount user);

    @Update("UPDATE sys_user SET status = #{status}, edit_time = CURRENT_TIMESTAMP, editor = #{editor} "
            + "WHERE id = #{id} AND is_deleted = 0")
    int updateStatus(@Param("id") long id, @Param("status") int status, @Param("editor") long editor);

    @Update("UPDATE sys_user SET password_hash = #{passwordHash}, edit_time = CURRENT_TIMESTAMP, editor = #{editor} "
            + "WHERE id = #{id} AND is_deleted = 0")
    int updatePassword(@Param("id") long id, @Param("passwordHash") String passwordHash, @Param("editor") long editor);

    /**
     * 软删除：只置 is_deleted，行本身保留，便于审计追查。
     */
    @Update("UPDATE sys_user SET is_deleted = 1, edit_time = CURRENT_TIMESTAMP, editor = #{editor} "
            + "WHERE id = #{id} AND is_deleted = 0")
    int softDelete(@Param("id") long id, @Param("editor") long editor);

    @Insert("INSERT INTO sys_user_role (user_id, role_code, is_deleted, created_time, edit_time, creator, editor) "
            + "VALUES (#{userId}, #{roleCode}, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, #{editor}, #{editor})")
    int insertUserRole(@Param("userId") long userId, @Param("roleCode") String roleCode,
                       @Param("editor") long editor);

    /**
     * 重新挂上之前被软删的同一个角色。
     * <p>
     * 必须有这一步：sys_user_role 的主键是 (user_id, role_code)，软删只是置 is_deleted = 1，
     * 行还在表里。直接 INSERT 同一个人同一个角色会撞主键，
     * 表现为「把角色改回去」时接口 500。
     */
    @Update("UPDATE sys_user_role SET is_deleted = 0, edit_time = CURRENT_TIMESTAMP, editor = #{editor} "
            + "WHERE user_id = #{userId} AND role_code = #{roleCode}")
    int restoreUserRole(@Param("userId") long userId, @Param("roleCode") String roleCode,
                        @Param("editor") long editor);

    /**
     * 改派角色时先整组失效再重新插入，避免做「差异比对」时漏掉边界情况。
     * 保留行而不是物理删除，与全表软删除的约定一致。
     */
    @Update("UPDATE sys_user_role SET is_deleted = 1, edit_time = CURRENT_TIMESTAMP, editor = #{editor} "
            + "WHERE user_id = #{userId} AND is_deleted = 0")
    int softDeleteUserRoles(@Param("userId") long userId, @Param("editor") long editor);

    /**
     * 用户被删除时，其角色关联一并失效，否则统计「该角色下有多少人」会算进已删除账号。
     */
    @Update("UPDATE sys_user_role SET is_deleted = 1, edit_time = CURRENT_TIMESTAMP, editor = #{editor} "
            + "WHERE is_deleted = 0 AND user_id IN (SELECT id FROM sys_user WHERE username = #{username})")
    int softDeleteUserRolesByUsername(@Param("username") String username, @Param("editor") long editor);

    /**
     * 演示数据重置用，是真清空：RESTART IDENTITY 让重新播种后的主键从 1 开始，
     * 否则每次 reset-demo 都会整体后移，「恢复演示数据」就不是真正的初始状态。
     */
    @Delete("TRUNCATE TABLE sys_user_role")
    int truncateUserRoles();

    @Delete("TRUNCATE TABLE sys_user RESTART IDENTITY")
    int truncate();
}

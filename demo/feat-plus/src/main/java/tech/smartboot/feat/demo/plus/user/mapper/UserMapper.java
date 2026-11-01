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

/**
 * 用户档案读写。
 * <p>
 * 列表类查询不返回 password_hash，避免哈希进入任何 HTTP 响应。
 * 注意：Feat 的注解处理器不支持 @Mapper 接口里出现常量字段，SQL 片段统一放在 SqlFragments。
 */
@Mapper
public interface UserMapper {

    @Select("<script>SELECT " + SqlFragments.USER_COLUMNS + " FROM sys_user" + SqlFragments.USER_FILTER
            + " ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}</script>")
    List<UserAccount> selectPage(@Param("keywordLike") String keywordLike,
                                 @Param("status") Integer status,
                                 @Param("roleCode") String roleCode,
                                 @Param("department") String department,
                                 @Param("limit") int limit,
                                 @Param("offset") int offset);

    @Select("<script>SELECT COUNT(*) FROM sys_user" + SqlFragments.USER_FILTER + "</script>")
    long countPage(@Param("keywordLike") String keywordLike,
                   @Param("status") Integer status,
                   @Param("roleCode") String roleCode,
                   @Param("department") String department);

    @Select("SELECT " + SqlFragments.USER_COLUMNS + " FROM sys_user ORDER BY id DESC")
    List<UserAccount> selectAll();

    @Select("SELECT " + SqlFragments.USER_COLUMNS + ", password_hash FROM sys_user WHERE id = #{id}")
    UserAccount selectById(@Param("id") long id);

    @Select("SELECT " + SqlFragments.USER_COLUMNS + ", password_hash FROM sys_user WHERE username = #{username}")
    UserAccount selectByUsername(@Param("username") String username);

    @Select("SELECT COUNT(*) FROM sys_user WHERE LOWER(username) = LOWER(#{username}) AND id <> #{excludeId}")
    long countByUsername(@Param("username") String username, @Param("excludeId") long excludeId);

    @Select("SELECT COUNT(*) FROM sys_user WHERE LOWER(email) = LOWER(#{email}) AND id <> #{excludeId}")
    long countByEmail(@Param("email") String email, @Param("excludeId") long excludeId);

    @Select("SELECT COUNT(*) FROM sys_user WHERE role_code = 'ADMIN' AND status = 1")
    long countEnabledAdmins();

    @Select("SELECT COUNT(*) FROM sys_user")
    long countAll();

    @Insert("INSERT INTO sys_user (username, password_hash, display_name, role_code, department, email, status, created_at, updated_at) "
            + "VALUES (#{username}, #{passwordHash}, #{displayName}, #{roleCode}, #{department}, #{email}, #{status}, #{createdAt}, #{updatedAt})")
    int insert(UserAccount user);

    @Update("UPDATE sys_user SET username = #{username}, display_name = #{displayName}, role_code = #{roleCode}, "
            + "department = #{department}, email = #{email}, status = #{status}, updated_at = CURRENT_TIMESTAMP "
            + "WHERE id = #{id}")
    int updateProfile(UserAccount user);

    @Update("UPDATE sys_user SET status = #{status}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateStatus(@Param("id") long id, @Param("status") int status);

    @Update("UPDATE sys_user SET password_hash = #{passwordHash}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updatePassword(@Param("id") long id, @Param("passwordHash") String passwordHash);

    @Delete("DELETE FROM sys_user WHERE id = #{id}")
    int deleteById(@Param("id") long id);

    /**
     * RESTART IDENTITY 让重新播种后的主键从 1 开始，
     * 否则每次 reset-demo 都会整体后移，「恢复演示数据」就不是真正的初始状态。
     */
    @Delete("TRUNCATE TABLE sys_user RESTART IDENTITY")
    int deleteAll();
}

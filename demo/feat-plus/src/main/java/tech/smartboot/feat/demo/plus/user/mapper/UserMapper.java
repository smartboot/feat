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
 * <p>
 * 软删除约定：每条读写 SQL 都显式带 is_deleted = 0，删除只置位不物理删行。
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

    @Select("SELECT " + SqlFragments.USER_COLUMNS + " FROM sys_user WHERE is_deleted = 0 ORDER BY id DESC")
    List<UserAccount> selectAll();

    @Select("SELECT " + SqlFragments.USER_COLUMNS + ", password_hash FROM sys_user "
            + "WHERE id = #{id} AND is_deleted = 0")
    UserAccount selectById(@Param("id") long id);

    @Select("SELECT " + SqlFragments.USER_COLUMNS + ", password_hash FROM sys_user "
            + "WHERE username = #{username} AND is_deleted = 0")
    UserAccount selectByUsername(@Param("username") String username);

    @Select("SELECT COUNT(*) FROM sys_user WHERE LOWER(username) = LOWER(#{username}) "
            + "AND id <> #{excludeId} AND is_deleted = 0")
    long countByUsername(@Param("username") String username, @Param("excludeId") long excludeId);

    @Select("SELECT COUNT(*) FROM sys_user WHERE LOWER(email) = LOWER(#{email}) "
            + "AND id <> #{excludeId} AND is_deleted = 0")
    long countByEmail(@Param("email") String email, @Param("excludeId") long excludeId);

    @Select("SELECT COUNT(*) FROM sys_user WHERE role_code = 'ADMIN' AND status = 1 AND is_deleted = 0")
    long countEnabledAdmins();

    @Select("SELECT COUNT(*) FROM sys_user WHERE is_deleted = 0")
    long countAll();

    @Insert("INSERT INTO sys_user (username, password_hash, display_name, role_code, department, email, status, "
            + "is_deleted, created_time, edit_time, creator, editor) "
            + "VALUES (#{username}, #{passwordHash}, #{displayName}, #{roleCode}, #{department}, #{email}, #{status}, "
            + "#{isDeleted}, #{createdTime}, #{editTime}, #{creator}, #{editor})")
    int insert(UserAccount user);

    @Update("UPDATE sys_user SET username = #{username}, display_name = #{displayName}, role_code = #{roleCode}, "
            + "department = #{department}, email = #{email}, status = #{status}, "
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

    /**
     * 演示数据重置用，是真清空：RESTART IDENTITY 让重新播种后的主键从 1 开始，
     * 否则每次 reset-demo 都会整体后移，「恢复演示数据」就不是真正的初始状态。
     */
    @Delete("TRUNCATE TABLE sys_user RESTART IDENTITY")
    int truncate();
}

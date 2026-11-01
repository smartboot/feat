package tech.smartboot.feat.demo.plus.role.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import tech.smartboot.feat.demo.plus.common.SqlFragments;
import tech.smartboot.feat.demo.plus.role.model.RoleRecord;

/**
 * 角色读写。
 * <p>
 * 软删除约定：每条 SQL 都带 is_deleted = 0，删除只置位。
 * 内置角色（builtin = 1）的编码与删除由 Service 层拦截，这里不做限制。
 */
@Mapper
public interface RoleMapper {

    @Select("SELECT " + SqlFragments.ROLE_COLUMNS + " FROM sys_role WHERE is_deleted = 0 "
            + "ORDER BY builtin DESC, id ASC")
    List<RoleRecord> selectAll();

    @Select("SELECT " + SqlFragments.ROLE_COLUMNS + " FROM sys_role WHERE id = #{id} AND is_deleted = 0")
    RoleRecord selectById(@Param("id") long id);

    @Select("SELECT " + SqlFragments.ROLE_COLUMNS + " FROM sys_role WHERE code = #{code} AND is_deleted = 0")
    RoleRecord selectByCode(@Param("code") String code);

    @Select("SELECT COUNT(*) FROM sys_role WHERE LOWER(code) = LOWER(#{code}) "
            + "AND id <> #{excludeId} AND is_deleted = 0")
    long countByCode(@Param("code") String code, @Param("excludeId") long excludeId);

    @Select("SELECT COUNT(*) FROM sys_role WHERE LOWER(name) = LOWER(#{name}) "
            + "AND id <> #{excludeId} AND is_deleted = 0")
    long countByName(@Param("name") String name, @Param("excludeId") long excludeId);

    /**
     * 该角色下的在用账号数，用于「删除前检查」和列表展示。
     */
    @Select("SELECT COUNT(*) FROM sys_user WHERE role_code = #{code} AND is_deleted = 0")
    long countUsers(@Param("code") String code);

    @Insert("INSERT INTO sys_role (code, name, description, permissions, builtin, status, "
            + "is_deleted, created_time, edit_time, creator, editor) "
            + "VALUES (#{code}, #{name}, #{description}, #{permissions}, #{builtin}, #{status}, "
            + "#{isDeleted}, #{createdTime}, #{editTime}, #{creator}, #{editor})")
    int insert(RoleRecord role);

    @Update("UPDATE sys_role SET name = #{name}, description = #{description}, "
            + "permissions = #{permissions}, edit_time = #{editTime}, editor = #{editor} "
            + "WHERE id = #{id} AND is_deleted = 0")
    int update(RoleRecord role);

    @Update("UPDATE sys_role SET status = #{status}, edit_time = CURRENT_TIMESTAMP, editor = #{editor} "
            + "WHERE id = #{id} AND is_deleted = 0")
    int updateStatus(@Param("id") long id, @Param("status") int status, @Param("editor") long editor);

    @Update("UPDATE sys_role SET is_deleted = 1, edit_time = CURRENT_TIMESTAMP, editor = #{editor} "
            + "WHERE id = #{id} AND is_deleted = 0")
    int softDelete(@Param("id") long id, @Param("editor") long editor);

    @Delete("TRUNCATE TABLE sys_role RESTART IDENTITY")
    int truncate();
}

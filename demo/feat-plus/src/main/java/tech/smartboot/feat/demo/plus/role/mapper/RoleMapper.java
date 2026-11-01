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
 * <p>
 * 「该角色下的账号数」走 sys_user_role 而不是 sys_user.role_code：
 * 多角色下一个人可能被两个角色同时引用。
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
     * 该角色下仍在用的账号数，用于「删除前检查」和列表展示。
     * 已停用账号也算「在用」——角色被删后他们会彻底失去权限，必须让人先处理。
     */
    @Select("SELECT COUNT(DISTINCT ur.user_id) FROM sys_user_role ur "
            + "JOIN sys_user u ON u.id = ur.user_id AND u.is_deleted = 0 "
            + "WHERE ur.role_code = #{code} AND ur.is_deleted = 0")
    long countUsers(@Param("code") String code);

    /**
     * 「自定义」数据范围的部门集合。
     */
    @Select("SELECT department_id FROM sys_role_dept WHERE role_code = #{roleCode} AND is_deleted = 0 "
            + "ORDER BY department_id")
    List<Long> selectDepartmentIds(@Param("roleCode") String roleCode);

    @Insert("INSERT INTO sys_role (code, name, description, permissions, department_scope, builtin, status, "
            + "is_deleted, created_time, edit_time, creator, editor) "
            + "VALUES (#{code}, #{name}, #{description}, #{permissions}, #{departmentScope}, #{builtin}, #{status}, "
            + "#{isDeleted}, #{createdTime}, #{editTime}, #{creator}, #{editor})")
    int insert(RoleRecord role);

    @Update("UPDATE sys_role SET name = #{name}, description = #{description}, "
            + "permissions = #{permissions}, department_scope = #{departmentScope}, "
            + "edit_time = #{editTime}, editor = #{editor} "
            + "WHERE id = #{id} AND is_deleted = 0")
    int update(RoleRecord role);

    @Update("UPDATE sys_role SET status = #{status}, edit_time = CURRENT_TIMESTAMP, editor = #{editor} "
            + "WHERE id = #{id} AND is_deleted = 0")
    int updateStatus(@Param("id") long id, @Param("status") int status, @Param("editor") long editor);

    @Update("UPDATE sys_role SET is_deleted = 1, edit_time = CURRENT_TIMESTAMP, editor = #{editor} "
            + "WHERE id = #{id} AND is_deleted = 0")
    int softDelete(@Param("id") long id, @Param("editor") long editor);

    @Insert("INSERT INTO sys_role_dept (role_code, department_id, is_deleted, created_time, edit_time, creator, editor) "
            + "VALUES (#{roleCode}, #{departmentId}, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, #{editor}, #{editor})")
    int insertRoleDept(@Param("roleCode") String roleCode, @Param("departmentId") long departmentId,
                       @Param("editor") long editor);

    /**
     * 重新启用之前软删过的同一条「角色 × 部门」配置。
     * 理由是主键冲突：sys_role_dept 主键是 (role_code, department_id)，
     * 反复编辑同一个角色的数据范围时，直接 INSERT 会撞上残留的行。
     */
    @Update("UPDATE sys_role_dept SET is_deleted = 0, edit_time = CURRENT_TIMESTAMP, editor = #{editor} "
            + "WHERE role_code = #{roleCode} AND department_id = #{departmentId}")
    int restoreRoleDept(@Param("roleCode") String roleCode, @Param("departmentId") long departmentId,
                        @Param("editor") long editor);

    @Update("UPDATE sys_role_dept SET is_deleted = 1, edit_time = CURRENT_TIMESTAMP, editor = #{editor} "
            + "WHERE role_code = #{roleCode} AND is_deleted = 0")
    int softDeleteRoleDepts(@Param("roleCode") String roleCode, @Param("editor") long editor);

    @Delete("TRUNCATE TABLE sys_role_dept")
    int truncateRoleDepts();

    @Delete("TRUNCATE TABLE sys_role RESTART IDENTITY")
    int truncate();
}

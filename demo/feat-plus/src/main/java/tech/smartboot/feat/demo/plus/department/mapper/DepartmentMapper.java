package tech.smartboot.feat.demo.plus.department.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import tech.smartboot.feat.demo.plus.common.SqlFragments;
import tech.smartboot.feat.demo.plus.department.model.DepartmentRecord;
import tech.smartboot.feat.demo.plus.department.model.DepartmentUserCount;

/**
 * 部门读写。parent_id = 0 表示根部门。
 */
@Mapper
public interface DepartmentMapper {

    @Select("SELECT " + SqlFragments.DEPARTMENT_COLUMNS + " FROM sys_department WHERE is_deleted = 0 "
            + "ORDER BY sort_order ASC, id ASC")
    List<DepartmentRecord> selectAll();

    @Select("SELECT " + SqlFragments.DEPARTMENT_COLUMNS
            + " FROM sys_department WHERE id = #{id} AND is_deleted = 0")
    DepartmentRecord selectById(@Param("id") long id);

    @Select("SELECT COUNT(*) FROM sys_department WHERE parent_id = #{parentId} AND is_deleted = 0")
    long countChildren(@Param("parentId") long parentId);

    /**
     * 同一父节点下重名校验：不同部门下的「前端组」是合理的，因此比较范围限定在兄弟节点。
     */
    @Select("SELECT COUNT(*) FROM sys_department WHERE parent_id = #{parentId} "
            + "AND LOWER(name) = LOWER(#{name}) AND id <> #{excludeId} AND is_deleted = 0")
    long countSiblingName(@Param("parentId") long parentId, @Param("name") String name,
                          @Param("excludeId") long excludeId);

    @Select("SELECT COUNT(*) FROM sys_user WHERE department_id = #{id} AND is_deleted = 0")
    long countUsers(@Param("id") long id);

    /**
     * 全量在用账号的部门分布，一次性取回，避免为每个部门单独查一次。
     */
    @Select("SELECT department_id, COUNT(*) AS total FROM sys_user WHERE is_deleted = 0 "
            + "AND department_id IS NOT NULL GROUP BY department_id")
    List<DepartmentUserCount> selectUserCounts();

    @Insert("INSERT INTO sys_department (parent_id, code, name, sort_order, description, status, "
            + "is_deleted, created_time, edit_time, creator, editor) "
            + "VALUES (#{parentId}, #{code}, #{name}, #{sortOrder}, #{description}, #{status}, "
            + "#{isDeleted}, #{createdTime}, #{editTime}, #{creator}, #{editor})")
    int insert(DepartmentRecord department);

    @Update("UPDATE sys_department SET parent_id = #{parentId}, code = #{code}, name = #{name}, "
            + "sort_order = #{sortOrder}, description = #{description}, status = #{status}, "
            + "edit_time = #{editTime}, editor = #{editor} "
            + "WHERE id = #{id} AND is_deleted = 0")
    int update(DepartmentRecord department);

    /**
     * 仅供种子数据使用：部门按「先父后子」插入，子节点插入时父节点 id 已经确定，
     * 但为了不依赖自增序列的具体取值，仍然插入后再回填一次层级关系。
     */
    @Update("UPDATE sys_department SET parent_id = #{parentId} WHERE id = #{id} AND is_deleted = 0")
    int updateParent(@Param("id") long id, @Param("parentId") long parentId);

    @Update("UPDATE sys_department SET is_deleted = 1, edit_time = CURRENT_TIMESTAMP, editor = #{editor} "
            + "WHERE id = #{id} AND is_deleted = 0")
    int softDelete(@Param("id") long id, @Param("editor") long editor);

    @Delete("TRUNCATE TABLE sys_department RESTART IDENTITY")
    int truncate();
}

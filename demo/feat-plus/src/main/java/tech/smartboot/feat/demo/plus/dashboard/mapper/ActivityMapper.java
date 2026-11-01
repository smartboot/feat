package tech.smartboot.feat.demo.plus.dashboard.mapper;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import tech.smartboot.feat.demo.plus.common.SqlFragments;
import tech.smartboot.feat.demo.plus.dashboard.model.ActivityRecord;

/**
 * 操作日志读写，软删除约定与 sys_user 一致。
 * <p>
 * 分页查询会联 sys_user 取操作人姓名：表里只存 creator 这个 id，
 * 日志页要显示「谁做的」，每次再单独查一遍用户太浪费。
 */
@Mapper
public interface ActivityMapper {

    @Insert("INSERT INTO sys_activity (kind, title, description, user_id, "
            + "is_deleted, created_time, edit_time, creator, editor) "
            + "VALUES (#{kind}, #{title}, #{description}, #{userId}, "
            + "#{isDeleted}, #{createdTime}, #{editTime}, #{creator}, #{editor})")
    int insert(ActivityRecord record);

    /**
     * 仪表盘「近期动态」。同样走数据范围：能看本部门的人数，却看到全公司的动态，比数字不对更让人困惑。
     */
    @Select("<script>SELECT a.id, a.kind, a.title, a.description, a.user_id, a.is_deleted, a.created_time, "
            + "a.edit_time, a.creator, a.editor, u.display_name AS operator_name "
            + "FROM sys_activity a LEFT JOIN sys_user u ON u.id = a.creator "
            + "<where> a.is_deleted = 0 " + SqlFragments.ACTIVITY_SCOPE_FILTER + "</where>"
            + " ORDER BY a.id DESC LIMIT #{limit}</script>")
    List<ActivityRecord> selectRecent(@Param("limit") int limit,
                                      @Param("scopeDepartmentIds") List<Long> scopeDepartmentIds,
                                      @Param("selfOnlyUserId") Long selfOnlyUserId);

    /**
     * 操作日志分页。kind / operatorId / from / to 都是可选条件，为 null 表示不限。
     * to 是「小于」，由上层把结束日期 +1 天传进来，这样结束当天是包含的。
     */
    @Select("<script>"
            + "SELECT a.id, a.kind, a.title, a.description, a.user_id, a.is_deleted, a.created_time, "
            + "a.edit_time, a.creator, a.editor, u.display_name AS operator_name "
            + "FROM sys_activity a LEFT JOIN sys_user u ON u.id = a.creator "
            + "<where> a.is_deleted = 0 "
            + "<if test='kind != null'> AND a.kind = #{kind} </if>"
            + "<if test='operatorId != null'> AND a.creator = #{operatorId} </if>"
            + "<if test='from != null'> AND a.created_time &gt;= #{from} </if>"
            + "<if test='to != null'> AND a.created_time &lt; #{to} </if>"
            + SqlFragments.ACTIVITY_SCOPE_FILTER
            + "</where>"
            + " ORDER BY a.id DESC LIMIT #{limit} OFFSET #{offset}"
            + "</script>")
    List<ActivityRecord> selectPage(@Param("kind") String kind,
                                    @Param("operatorId") Long operatorId,
                                    @Param("from") Date from,
                                    @Param("to") Date to,
                                    @Param("limit") int limit,
                                    @Param("offset") int offset,
                                    @Param("scopeDepartmentIds") List<Long> scopeDepartmentIds,
                                    @Param("selfOnlyUserId") Long selfOnlyUserId);

    @Select("<script>"
            + "SELECT COUNT(*) FROM sys_activity a LEFT JOIN sys_user u ON u.id = a.creator "
            + "<where> a.is_deleted = 0 "
            + "<if test='kind != null'> AND a.kind = #{kind} </if>"
            + "<if test='operatorId != null'> AND a.creator = #{operatorId} </if>"
            + "<if test='from != null'> AND a.created_time &gt;= #{from} </if>"
            + "<if test='to != null'> AND a.created_time &lt; #{to} </if>"
            + SqlFragments.ACTIVITY_SCOPE_FILTER
            + "</where>"
            + "</script>")
    long countPage(@Param("kind") String kind,
                   @Param("operatorId") Long operatorId,
                   @Param("from") Date from,
                   @Param("to") Date to,
                   @Param("scopeDepartmentIds") List<Long> scopeDepartmentIds,
                   @Param("selfOnlyUserId") Long selfOnlyUserId);

    /**
     * 日志里实际出现过的操作人，供筛选下拉使用。
     * 不用 sys_user 全量：被删掉的人也可能留下日志，这些历史记录同样要能按人筛。
     * <p>
     * 同样受数据范围约束——下拉里列出「你筛了也查不到任何结果」的人，只会让人以为筛选坏了。
     */
    @Select("<script>SELECT DISTINCT a.creator, u.display_name AS operator_name "
            + "FROM sys_activity a LEFT JOIN sys_user u ON u.id = a.creator "
            + "<where> a.is_deleted = 0 AND a.creator IS NOT NULL " + SqlFragments.ACTIVITY_SCOPE_FILTER + "</where>"
            + " ORDER BY u.display_name</script>")
    List<ActivityRecord> selectOperators(@Param("scopeDepartmentIds") List<Long> scopeDepartmentIds,
                                         @Param("selfOnlyUserId") Long selfOnlyUserId);

    /**
     * 演示数据重置用，真清空并重置自增。
     */
    @Delete("TRUNCATE TABLE sys_activity RESTART IDENTITY")
    int truncate();
}

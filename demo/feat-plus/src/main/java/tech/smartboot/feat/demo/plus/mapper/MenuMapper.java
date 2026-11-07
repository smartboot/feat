package tech.smartboot.feat.demo.plus.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import tech.smartboot.feat.demo.plus.model.MenuRecord;

/**
 * 侧边栏菜单读写。
 * <p>
 * 排序用 {@code group_sort, sort_order, id} 三列而不是只靠 id：组间顺序与组内顺序是两个独立维度，
 * id 只作为最后的稳定兜底（同序号时不会因为查询计划变化而抖动）。
 */
@Mapper
public interface MenuMapper {

    @Select("SELECT id, group_key, group_label, group_sort, path, name, icon, permission, sort_order, "
            + "visible, is_deleted, created_time, edit_time, creator, editor "
            + "FROM sys_menu WHERE is_deleted = 0 ORDER BY group_sort ASC, sort_order ASC, id ASC")
    List<MenuRecord> selectAll();

    @Select("SELECT id, group_key, group_label, group_sort, path, name, icon, permission, sort_order, "
            + "visible, is_deleted, created_time, edit_time, creator, editor "
            + "FROM sys_menu WHERE id = #{id} AND is_deleted = 0")
    MenuRecord selectById(@Param("id") long id);

    /**
     * 同一路径只能有一条菜单，否则侧边栏会出现两个都指向同一页面的入口。
     * 比较范围限定 is_deleted = 0，与用户、角色表的唯一性校验保持一致。
     */
    @Select("SELECT COUNT(*) FROM sys_menu WHERE path = #{path} AND id <> #{excludeId} AND is_deleted = 0")
    long countPath(@Param("path") String path, @Param("excludeId") long excludeId);

    /**
     * 插入后回读。MyBatis 这条链路没有开 useGeneratedKeys，而 path 在未删除范围内唯一，
     * 所以拿 path 作回读条件，和 UserService 用 username 回读是同一个套路。
     */
    @Select("SELECT id, group_key, group_label, group_sort, path, name, icon, permission, sort_order, "
            + "visible, is_deleted, created_time, edit_time, creator, editor "
            + "FROM sys_menu WHERE path = #{path} AND is_deleted = 0")
    MenuRecord selectByPath(@Param("path") String path);

    @Select("SELECT COUNT(*) FROM sys_menu WHERE is_deleted = 0")
    long countAll();

    @Insert("INSERT INTO sys_menu (group_key, group_label, group_sort, path, name, icon, permission, "
            + "sort_order, visible, is_deleted, created_time, edit_time, creator, editor) "
            + "VALUES (#{groupKey}, #{groupLabel}, #{groupSort}, #{path}, #{name}, #{icon}, #{permission}, "
            + "#{sortOrder}, #{visible}, #{isDeleted}, #{createdTime}, #{editTime}, #{creator}, #{editor})")
    int insert(MenuRecord menu);

    @Update("UPDATE sys_menu SET group_key = #{groupKey}, group_label = #{groupLabel}, "
            + "group_sort = #{groupSort}, path = #{path}, name = #{name}, icon = #{icon}, "
            + "permission = #{permission}, sort_order = #{sortOrder}, visible = #{visible}, "
            + "edit_time = #{editTime}, editor = #{editor} WHERE id = #{id} AND is_deleted = 0")
    int update(MenuRecord menu);

    /**
     * 分组信息冗余存在每行上，所以改组名 / 组顺序要落到同组的所有行，否则前端分组取「该组第一条的
     * groupLabel」，会看到只有部分行改了名。调用方保证 groupKey 相等的行确实属于同一组。
     */
    @Update("UPDATE sys_menu SET group_label = #{groupLabel}, group_sort = #{groupSort}, "
            + "edit_time = CURRENT_TIMESTAMP, editor = #{editor} "
            + "WHERE group_key = #{groupKey} AND is_deleted = 0")
    int updateGroupMeta(@Param("groupKey") String groupKey, @Param("groupLabel") String groupLabel,
                        @Param("groupSort") int groupSort, @Param("editor") long editor);

    @Update("UPDATE sys_menu SET is_deleted = 1, edit_time = CURRENT_TIMESTAMP, editor = #{editor} "
            + "WHERE id = #{id} AND is_deleted = 0")
    int softDelete(@Param("id") long id, @Param("editor") long editor);

    @Delete("TRUNCATE TABLE sys_menu RESTART IDENTITY")
    int truncate();
}

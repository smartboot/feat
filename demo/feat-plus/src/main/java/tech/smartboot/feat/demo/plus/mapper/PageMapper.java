package tech.smartboot.feat.demo.plus.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import tech.smartboot.feat.demo.plus.model.PageRecord;

/**
 * 动态页面配置读写。
 * <p>
 * 查询频率与菜单同量级（渲染一个动态页 = 拉一次配置），读路径走 PageSnapshot 缓存；
 * 排序按 page_key 字典序，稳定的顺序让配置管理页的列表行为可预测。
 */
@Mapper
public interface PageMapper {

    @Select("SELECT id, page_key, title, description, datasource_key, list_json, form_json, "
            + "is_deleted, created_time, edit_time, creator, editor "
            + "FROM sys_page WHERE is_deleted = 0 ORDER BY page_key ASC, id ASC")
    List<PageRecord> selectAll();

    @Select("SELECT id, page_key, title, description, datasource_key, list_json, form_json, "
            + "is_deleted, created_time, edit_time, creator, editor "
            + "FROM sys_page WHERE id = #{id} AND is_deleted = 0")
    PageRecord selectById(@Param("id") long id);

    /**
     * 同一 pageKey 只能有一条配置：它同时是菜单 path（/p/{pageKey}）与前端拉配置的键。
     * 比较范围限定 is_deleted = 0，与其他表的唯一性校验保持一致。
     */
    @Select("SELECT COUNT(*) FROM sys_page WHERE page_key = #{pageKey} AND id <> #{excludeId} AND is_deleted = 0")
    long countPageKey(@Param("pageKey") String pageKey, @Param("excludeId") long excludeId);

    /**
     * 插入后回读。未开 useGeneratedKeys，而 pageKey 在未删除范围内唯一，
     * 拿它作回读条件，与 MenuMapper.selectByPath 是同一个套路。
     */
    @Select("SELECT id, page_key, title, description, datasource_key, list_json, form_json, "
            + "is_deleted, created_time, edit_time, creator, editor "
            + "FROM sys_page WHERE page_key = #{pageKey} AND is_deleted = 0")
    PageRecord selectByPageKey(@Param("pageKey") String pageKey);

    @Select("SELECT COUNT(*) FROM sys_page WHERE is_deleted = 0")
    long countAll();

    @Insert("INSERT INTO sys_page (page_key, title, description, datasource_key, list_json, form_json, "
            + "is_deleted, created_time, edit_time, creator, editor) "
            + "VALUES (#{pageKey}, #{title}, #{description}, #{datasourceKey}, #{listJson}, #{formJson}, "
            + "#{isDeleted}, #{createdTime}, #{editTime}, #{creator}, #{editor})")
    int insert(PageRecord page);

    @Update("UPDATE sys_page SET title = #{title}, description = #{description}, "
            + "datasource_key = #{datasourceKey}, list_json = #{listJson}, form_json = #{formJson}, "
            + "edit_time = #{editTime}, editor = #{editor} WHERE id = #{id} AND is_deleted = 0")
    int update(PageRecord page);

    @Update("UPDATE sys_page SET is_deleted = 1, edit_time = CURRENT_TIMESTAMP, editor = #{editor} "
            + "WHERE id = #{id} AND is_deleted = 0")
    int softDelete(@Param("id") long id, @Param("editor") long editor);

    @Delete("TRUNCATE TABLE sys_page RESTART IDENTITY")
    int truncate();
}

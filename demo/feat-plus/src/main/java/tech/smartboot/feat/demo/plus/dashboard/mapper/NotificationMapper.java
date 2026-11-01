package tech.smartboot.feat.demo.plus.dashboard.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import tech.smartboot.feat.demo.plus.dashboard.model.NotificationRecord;

/**
 * 站内通知读写，软删除约定与 sys_user 一致：读带 is_deleted = 0，删只置位。
 */
@Mapper
public interface NotificationMapper {

    @Insert("INSERT INTO sys_notification (title, description, user_id, read_flag, "
            + "is_deleted, created_time, edit_time, creator, editor) "
            + "VALUES (#{title}, #{description}, #{userId}, #{readFlag}, "
            + "#{isDeleted}, #{createdTime}, #{editTime}, #{creator}, #{editor})")
    int insert(NotificationRecord record);

    @Select("SELECT id, title, description, user_id, read_flag, is_deleted, created_time, edit_time, creator, editor "
            + "FROM sys_notification WHERE is_deleted = 0 ORDER BY id DESC")
    List<NotificationRecord> selectAll();

    @Select("SELECT COUNT(*) FROM sys_notification WHERE read_flag = 0 AND is_deleted = 0")
    long countUnread();

    @Update("UPDATE sys_notification SET read_flag = 1, edit_time = CURRENT_TIMESTAMP, editor = #{editor} "
            + "WHERE id = #{id} AND is_deleted = 0")
    int markRead(@Param("id") long id, @Param("editor") long editor);

    @Update("UPDATE sys_notification SET read_flag = 1, edit_time = CURRENT_TIMESTAMP, editor = #{editor} "
            + "WHERE read_flag = 0 AND is_deleted = 0")
    int markAllRead(@Param("editor") long editor);

    /**
     * 演示数据重置用，真清空并重置自增。
     */
    @Delete("TRUNCATE TABLE sys_notification RESTART IDENTITY")
    int truncate();
}

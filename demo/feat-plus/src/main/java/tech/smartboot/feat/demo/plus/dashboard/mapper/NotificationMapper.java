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
 * 站内通知读写。
 */
@Mapper
public interface NotificationMapper {

    @Insert("INSERT INTO sys_notification (title, description, user_id, read_flag, created_at) "
            + "VALUES (#{title}, #{description}, #{userId}, #{readFlag}, #{createdAt})")
    int insert(NotificationRecord record);

    @Select("SELECT id, title, description, user_id, read_flag, created_at FROM sys_notification ORDER BY id DESC")
    List<NotificationRecord> selectAll();

    @Select("SELECT COUNT(*) FROM sys_notification WHERE read_flag = 0")
    long countUnread();

    @Update("UPDATE sys_notification SET read_flag = 1 WHERE id = #{id}")
    int markRead(@Param("id") long id);

    @Update("UPDATE sys_notification SET read_flag = 1 WHERE read_flag = 0")
    int markAllRead();

    @Delete("TRUNCATE TABLE sys_notification RESTART IDENTITY")
    int deleteAll();
}

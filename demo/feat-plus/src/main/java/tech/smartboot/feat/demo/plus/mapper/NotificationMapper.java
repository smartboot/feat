package tech.smartboot.feat.demo.plus.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import tech.smartboot.feat.demo.plus.model.NotificationRecord;

/**
 * 站内通知读写。
 * <p>
 * 可见性规则：user_id 为 0 或 NULL 是广播，所有人可见；大于 0 时只有该用户可见。
 * <p>
 * 已读状态不落在 sys_notification 上，而是每人一行记在 sys_notification_read。
 * 以前用单列 read_flag 时，广播通知只有一行，两个管理员谁点了已读另一个人也会变成已读。
 * <p>
 * 软删除约定与 sys_user 一致：读带 is_deleted = 0，删只置位。
 * <p>
 * 注意：可见性条件在每条 SQL 里重复写了 4 遍，没有抽成常量字段——@Mapper 接口里不能有字段，
 * 注解处理器会把接口内所有元素当方法强转 ExecutableElement，字段是 VarSymbol 会直接抛异常。
 */
@Mapper
public interface NotificationMapper {

    @Insert("INSERT INTO sys_notification (title, description, user_id, "
            + "is_deleted, created_time, edit_time, creator, editor) "
            + "VALUES (#{title}, #{description}, #{userId}, "
            + "#{isDeleted}, #{createdTime}, #{editTime}, #{creator}, #{editor})")
    int insert(NotificationRecord record);

    /**
     * 当前用户可见的通知。read_flag 是被关联表算出来的，不是 sys_notification 的列。
     */
    @Select("SELECT n.id, n.title, n.description, n.user_id, n.created_time, n.edit_time, n.creator, n.editor, "
            + "CASE WHEN r.notification_id IS NULL THEN 0 ELSE 1 END AS read_flag "
            + "FROM sys_notification n "
            + "LEFT JOIN sys_notification_read r ON r.notification_id = n.id AND r.user_id = #{userId} "
            + "WHERE n.is_deleted = 0 "
            + "AND (n.user_id IS NULL OR n.user_id = 0 OR n.user_id = #{userId}) "
            + "ORDER BY n.id DESC")
    List<NotificationRecord> selectVisible(@Param("userId") long userId);

    @Select("SELECT COUNT(*) FROM sys_notification n "
            + "LEFT JOIN sys_notification_read r ON r.notification_id = n.id AND r.user_id = #{userId} "
            + "WHERE n.is_deleted = 0 AND r.notification_id IS NULL "
            + "AND (n.user_id IS NULL OR n.user_id = 0 OR n.user_id = #{userId})")
    long countUnread(@Param("userId") long userId);

    /**
     * 这条通知对该用户是否可见。标记已读前用它区分「不存在 / 不是给我的」与「已经读过」。
     */
    @Select("SELECT COUNT(*) FROM sys_notification n "
            + "WHERE n.id = #{id} AND n.is_deleted = 0 "
            + "AND (n.user_id IS NULL OR n.user_id = 0 OR n.user_id = #{userId})")
    long countVisibleById(@Param("id") long id, @Param("userId") long userId);

    /**
     * 标记已读：往关联表插一行，已存在则跳过，因此重复调用是幂等的。
     */
    @Insert("INSERT INTO sys_notification_read "
            + "(notification_id, user_id, is_deleted, created_time, edit_time, creator, editor) "
            + "SELECT n.id, #{userId}, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, #{userId}, #{userId} "
            + "FROM sys_notification n "
            + "WHERE n.id = #{id} AND n.is_deleted = 0 "
            + "AND (n.user_id IS NULL OR n.user_id = 0 OR n.user_id = #{userId}) "
            + "AND NOT EXISTS (SELECT 1 FROM sys_notification_read r "
            + "WHERE r.notification_id = n.id AND r.user_id = #{userId})")
    int markRead(@Param("id") long id, @Param("userId") long userId);

    @Insert("INSERT INTO sys_notification_read "
            + "(notification_id, user_id, is_deleted, created_time, edit_time, creator, editor) "
            + "SELECT n.id, #{userId}, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, #{userId}, #{userId} "
            + "FROM sys_notification n "
            + "WHERE n.is_deleted = 0 "
            + "AND (n.user_id IS NULL OR n.user_id = 0 OR n.user_id = #{userId}) "
            + "AND NOT EXISTS (SELECT 1 FROM sys_notification_read r "
            + "WHERE r.notification_id = n.id AND r.user_id = #{userId})")
    int markAllRead(@Param("userId") long userId);

    /**
     * 演示数据重置用，真清空并重置自增。
     */
    @Delete("TRUNCATE TABLE sys_notification RESTART IDENTITY")
    int truncate();

    /**
     * 关联表主键是 (notification_id, user_id)，没有自增列，DELETE 即可。
     */
    @Delete("DELETE FROM sys_notification_read")
    int truncateReads();
}

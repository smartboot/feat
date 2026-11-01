package tech.smartboot.feat.demo.plus.dashboard.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import tech.smartboot.feat.demo.plus.dashboard.model.ActivityRecord;

/**
 * 操作动态读写，软删除约定与 sys_user 一致。
 */
@Mapper
public interface ActivityMapper {

    @Insert("INSERT INTO sys_activity (kind, title, description, user_id, "
            + "is_deleted, created_time, edit_time, creator, editor) "
            + "VALUES (#{kind}, #{title}, #{description}, #{userId}, "
            + "#{isDeleted}, #{createdTime}, #{editTime}, #{creator}, #{editor})")
    int insert(ActivityRecord record);

    @Select("SELECT id, kind, title, description, user_id, is_deleted, created_time, edit_time, creator, editor "
            + "FROM sys_activity WHERE is_deleted = 0 ORDER BY id DESC LIMIT #{limit}")
    List<ActivityRecord> selectRecent(@Param("limit") int limit);

    /**
     * 演示数据重置用，真清空并重置自增。
     */
    @Delete("TRUNCATE TABLE sys_activity RESTART IDENTITY")
    int truncate();
}

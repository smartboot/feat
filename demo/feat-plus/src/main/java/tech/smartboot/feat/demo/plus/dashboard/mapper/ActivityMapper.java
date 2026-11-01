package tech.smartboot.feat.demo.plus.dashboard.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tech.smartboot.feat.demo.plus.dashboard.model.ActivityRecord;

/**
 * 操作动态读写。
 */
@Mapper
public interface ActivityMapper {

    @Insert("INSERT INTO sys_activity (kind, title, description, user_id, created_at) "
            + "VALUES (#{kind}, #{title}, #{description}, #{userId}, #{createdAt})")
    int insert(ActivityRecord record);

    @Select("SELECT id, kind, title, description, user_id, created_at FROM sys_activity ORDER BY id DESC LIMIT #{limit}")
    List<ActivityRecord> selectRecent(@Param("limit") int limit);

    @Delete("TRUNCATE TABLE sys_activity RESTART IDENTITY")
    int deleteAll();
}

package tech.smartboot.feat.demo.plus.dashboard.mapper;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import tech.smartboot.feat.demo.plus.dashboard.model.DailyLogin;

/**
 * 按天聚合的登录统计，驱动仪表盘的趋势曲线。
 * <p>
 * 注意：@Mapper 接口内既不能有常量字段，也不能有内部类，
 * Feat 的注解处理器会把接口内所有元素强转成 ExecutableElement。
 */
@Mapper
public interface LoginStatMapper {

    @Update("UPDATE sys_login_stat SET login_count = login_count + 1 WHERE stat_date = CURRENT_DATE")
    int increaseToday();

    @Insert("INSERT INTO sys_login_stat (stat_date, login_count) VALUES (CURRENT_DATE, 1)")
    int insertToday();

    @Insert("INSERT INTO sys_login_stat (stat_date, login_count) VALUES (#{statDate}, #{loginCount})")
    int insert(@Param("statDate") Date statDate, @Param("loginCount") int loginCount);

    @Select("SELECT stat_date AS statDate, login_count AS loginCount FROM sys_login_stat "
            + "ORDER BY stat_date DESC LIMIT #{limit}")
    List<DailyLogin> selectRecent(@Param("limit") int limit);

    @Delete("DELETE FROM sys_login_stat")
    int deleteAll();
}

package tech.smartboot.feat.demo.plus.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 就绪探针专用：只做一次最小的数据库往返，证明「连接池、驱动、库」三层都通。
 * 不复用业务查询——业务 SQL 改了不应该牵连探针的语义。
 */
@Mapper
public interface HealthMapper {

    @Select("SELECT 1")
    int ping();
}

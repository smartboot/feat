package tech.smartboot.feat.demo.plus.service;

import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.demo.plus.dto.HealthResponse;
import tech.smartboot.feat.demo.plus.mapper.HealthMapper;

/**
 * 健康探针的业务侧：存活不碰数据库，就绪做一次最小数据库往返。
 * 区别放在这里而不是 Controller，与其它接口的分层保持一致——Controller 只做翻译，不摸 Mapper。
 */
@Bean
public class HealthService {

    @Autowired
    private HealthMapper healthMapper;

    private final long startedAt = System.currentTimeMillis();

    public HealthResponse live() {
        return new HealthResponse("up", "skipped", uptimeSeconds());
    }

    public HealthResponse ready() {
        healthMapper.ping();
        return new HealthResponse("up", "ok", uptimeSeconds());
    }

    private long uptimeSeconds() {
        return (System.currentTimeMillis() - startedAt) / 1000;
    }
}

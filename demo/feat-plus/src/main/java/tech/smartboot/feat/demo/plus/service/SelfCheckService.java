package tech.smartboot.feat.demo.plus.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.demo.plus.dto.ProbeResult;
import tech.smartboot.feat.demo.plus.mapper.DepartmentMapper;
import tech.smartboot.feat.demo.plus.model.DepartmentRecord;
import tech.smartboot.feat.demo.plus.tx.TxTemplate;

/**
 * 地基自检：验证事务模板在<b>当前这套数据库和驱动上</b>真的生效。
 * <p>
 * 为什么需要它：事务是那种「写错了不报错」的机制。Mapper 混用、代理没接上、驱动把 DDL 隐式提交、
 * 内层加入外层却没提交——任何一种错法都表现为「接口正常返回 200」，只有等到某次上线少提交一笔、
 * 或者多写了一行脏数据时才会被发现，而那时已经没人记得是配置的问题。
 * <p>
 * 做法：在一个事务里写一行，然后故意抛异常，<b>事务外</b>回查这一行是否真的没落库。
 * 只回滚不看残留是不够的——「回滚了但连接池没有归还」和「根本没进事务」都表现为 200。
 */
@Bean
public class SelfCheckService {

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private TxTemplate txTemplate;

    public List<ProbeResult> run() {
        List<ProbeResult> results = new ArrayList<>(1);
        results.add(transactionRollback());
        return results;
    }

    private ProbeResult transactionRollback() {
        final String name = "__selftest_" + System.currentTimeMillis();
        RuntimeException failure = null;
        try {
            txTemplate.run(session -> {
                DepartmentMapper departments = session.getMapper(DepartmentMapper.class);
                Date now = new Date();
                DepartmentRecord record = new DepartmentRecord();
                record.setParentId(0L);
                record.setCode("SELFTEST");
                record.setName(name);
                record.setSortOrder(0);
                record.setDescription("");
                record.setStatus(1);
                record.setIsDeleted(0);
                record.setCreatedTime(now);
                record.setEditTime(now);
                record.setCreator(0L);
                record.setEditor(0L);
                departments.insert(record);
                throw new IllegalStateException("自检：写入一行后故意失败，用来验证回滚");
            });
        } catch (RuntimeException e) {
            failure = e;
        }

        if (txTemplate.active()) {
            return new ProbeResult("transaction.rollback", false,
                    "事务结束后会话仍绑定在当前线程上，连接会被下一次请求复用");
        }
        if (failure == null) {
            return new ProbeResult("transaction.rollback", false,
                    "事务体抛出的异常没有传出模板，调用方会以为写成功了");
        }
        // 事务外直读，绕开任何一层缓存：要确认的是「库里到底有没有」。
        long residue = departmentMapper.countSiblingName(0L, name, 0L);
        if (residue > 0) {
            return new ProbeResult("transaction.rollback", false,
                    "回滚未生效，库里残留 " + residue + " 行");
        }
        return new ProbeResult("transaction.rollback", true,
                "写入后抛异常，提交未发生，事务外查不到该行");
    }
}

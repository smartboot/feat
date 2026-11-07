package tech.smartboot.feat.demo.plus.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.demo.plus.common.DomainException;
import tech.smartboot.feat.demo.plus.dto.DepartmentInputRequest;
import tech.smartboot.feat.demo.plus.dto.DepartmentNodeResponse;
import tech.smartboot.feat.demo.plus.mapper.DepartmentMapper;
import tech.smartboot.feat.demo.plus.model.DepartmentRecord;
import tech.smartboot.feat.demo.plus.model.DepartmentUserCount;
import tech.smartboot.feat.demo.plus.security.DepartmentSnapshot;
import tech.smartboot.feat.demo.plus.security.DepartmentTree;
import tech.smartboot.feat.demo.plus.tx.TxTemplate;

/**
 * 部门树业务规则：
 * <ul>
 *   <li>parentId = 0 为根；同一父节点下不允许重名，不同部门下可以同名（如各自的「前端组」）；</li>
 *   <li>移动节点时校验环路：不能挂到自己或自己的后代下面，否则整棵子树会从树上脱落；</li>
 *   <li>删除前必须没有子部门、没有成员，避免出现挂在空部门上的账号；</li>
 *   <li>按部门筛选用户时默认包含全部下级，由 {@link #subtreeIds(long)} 提供 id 集合。</li>
 * </ul>
 * <p>
 * <b>缓存失效必须在事务提交之后</b>：提交前清缓存，下一个请求会用提交前的数据把缓存重新填满，
 * 于是「改完立刻生效」变成「改完十分钟才生效」——而且只在有并发时出现，本地单线程测不出来。
 */
@Bean
public class DepartmentService {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z0-9_-]{0,32}$");
    private static final long ROOT = 0L;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private ActivityRecorder activityRecorder;

    @Autowired
    private DepartmentSnapshot departmentSnapshot;

    @Autowired
    private TxTemplate txTemplate;

    /**
     * 组装成树返回。节点顺序按 sortOrder、id 升序，前端不需要再排。
     */
    public List<DepartmentNodeResponse> tree() {
        List<DepartmentRecord> rows = departmentSnapshot.all();
        Map<Long, Long> userCounts = userCounts();
        Map<Long, DepartmentNodeResponse> nodes = new LinkedHashMap<>();
        for (DepartmentRecord row : rows) {
            Long count = userCounts.get(row.getId());
            nodes.put(row.getId(), DepartmentNodeResponse.from(row, count == null ? 0L : count));
        }

        List<DepartmentNodeResponse> roots = new ArrayList<>();
        for (DepartmentNodeResponse node : nodes.values()) {
            DepartmentNodeResponse parent = node.getParentId() == ROOT ? null : nodes.get(node.getParentId());
            if (parent == null) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }

    /**
     * 某部门及其全部下级的 id 集合，用于「按部门筛选成员」时把子部门一并算进去。
     * <p>
     * 算法在 {@link DepartmentTree}：数据范围解析也要用它，而机制层不能依赖业务层。
     */
    public List<Long> subtreeIds(long rootId) {
        return DepartmentTree.subtreeIds(departmentSnapshot.all(), rootId);
    }

    /**
     * 校验一组部门 id 都真实存在（未软删）。
     * <p>
     * 角色配置「自定义数据范围」时用它把关：允许保存指向已删除部门的引用，
     * 会让数据权限的范围悄悄缩小，排查起来非常费劲。
     */
    public void assertAllExist(List<Long> departmentIds) {
        if (departmentIds == null || departmentIds.isEmpty()) {
            return;
        }
        Set<Long> existing = new LinkedHashSet<>();
        for (DepartmentRecord row : departmentMapper.selectAll()) {
            existing.add(row.getId());
        }
        for (Long id : departmentIds) {
            if (id == null || !existing.contains(id)) {
                throw new DomainException("部门不存在：" + id);
            }
        }
    }

    public DepartmentNodeResponse create(DepartmentInputRequest input, long operatorId) {
        long parentId = input == null || input.getParentId() == null ? ROOT : input.getParentId();
        assertParentExists(parentId);
        String name = requireName(input == null ? null : input.getName());
        assertSiblingNameUnique(parentId, name, 0L);
        String parentLabel = parentId == ROOT ? "顶级部门" : "归属于 " + nameOf(parentId);

        DepartmentRecord saved = txTemplate.call(session -> {
            DepartmentMapper departments = session.getMapper(DepartmentMapper.class);
            Date now = new Date();
            DepartmentRecord record = new DepartmentRecord();
            record.setParentId(parentId);
            record.setCode(validateCode(input.getCode()));
            record.setName(name);
            record.setSortOrder(input.getSortOrder() == null ? 0 : input.getSortOrder());
            record.setDescription(trimToEmpty(input.getDescription()));
            record.setStatus(input.getStatus() == null ? 1 : input.getStatus());
            record.setIsDeleted(0);
            record.setCreatedTime(now);
            record.setEditTime(now);
            record.setCreator(operatorId);
            record.setEditor(operatorId);
            departments.insert(record);

            activityRecorder.record("create", "创建了部门 " + name, parentLabel, null, operatorId);
            // 读回走事务会话：注入的 Mapper 是另一条连接，看不到本事务尚未提交的行。
            return requireByName(departments, name, parentId);
        });
        departmentSnapshot.invalidate();
        return DepartmentNodeResponse.from(saved, 0);
    }

    public DepartmentNodeResponse update(long id, DepartmentInputRequest input, long operatorId) {
        DepartmentRecord current = require(id);
        long parentId = input == null || input.getParentId() == null ? current.getParentId() : input.getParentId();
        if (parentId == id) {
            throw new DomainException("部门不能作为自己的上级");
        }
        assertParentExists(parentId);
        assertNoCycle(id, parentId);
        String name = requireName(input == null ? null : input.getName());
        assertSiblingNameUnique(parentId, name, id);
        String parentLabel = parentId == ROOT ? "调整为顶级部门" : "上级调整为 " + nameOf(parentId);

        current.setParentId(parentId);
        current.setCode(validateCode(input.getCode()));
        current.setName(name);
        current.setSortOrder(input.getSortOrder() == null ? current.getSortOrder() : input.getSortOrder());
        current.setDescription(trimToEmpty(input.getDescription()));
        if (input.getStatus() != null) {
            current.setStatus(input.getStatus());
        }
        current.setEditTime(new Date());
        current.setEditor(operatorId);

        txTemplate.run(session -> {
            session.getMapper(DepartmentMapper.class).update(current);
            activityRecorder.record("update", "更新了部门 " + name, parentLabel, null, operatorId);
        });
        departmentSnapshot.invalidate();

        DepartmentRecord saved = require(id);
        Long count = userCounts().get(id);
        return DepartmentNodeResponse.from(saved, count == null ? 0L : count);
    }

    public void delete(long id, long operatorId) {
        DepartmentRecord record = require(id);
        if (departmentMapper.countChildren(id) > 0) {
            throw new DomainException("该部门下还有子部门，请先删除或移动子部门");
        }
        long users = departmentMapper.countUsers(id);
        if (users > 0) {
            throw new DomainException("该部门下还有 " + users + " 位成员，请先调整成员归属");
        }
        txTemplate.run(session -> {
            session.getMapper(DepartmentMapper.class).softDelete(id, operatorId);
            activityRecorder.record("delete", "删除了部门 " + record.getName(),
                    record.getCode().isEmpty() ? "部门已移除" : record.getCode() + " 已移除", null, operatorId);
        });
        departmentSnapshot.invalidate();
    }

    /**
     * 供用户模块校验：成员必须挂在一个存在且启用的部门上。
     */
    public DepartmentRecord requireAssignable(Long departmentId) {
        if (departmentId == null) {
            throw new DomainException("请选择所属部门");
        }
        DepartmentRecord record = departmentMapper.selectById(departmentId);
        if (record == null) {
            throw new DomainException("部门不存在或已被删除");
        }
        if (record.getStatus() != 1) {
            throw new DomainException("部门已停用，无法分配成员：" + record.getName());
        }
        return record;
    }

    private Map<Long, Long> userCounts() {
        Map<Long, Long> counts = new HashMap<>();
        for (DepartmentUserCount row : departmentMapper.selectUserCounts()) {
            if (row.getDepartmentId() != null) {
                counts.put(row.getDepartmentId(), row.getTotal());
            }
        }
        return counts;
    }

    private DepartmentRecord require(long id) {
        DepartmentRecord record = departmentMapper.selectById(id);
        if (record == null) {
            throw DomainException.notFound("部门不存在或已被删除");
        }
        return record;
    }

    private DepartmentRecord requireByName(DepartmentMapper departments, String name, long parentId) {
        for (DepartmentRecord row : departments.selectAll()) {
            if (row.getParentId() == parentId && name.equals(row.getName())) {
                return row;
            }
        }
        throw new DomainException("部门保存失败");
    }

    private String nameOf(long id) {
        DepartmentRecord record = departmentMapper.selectById(id);
        return record == null ? "未知部门" : record.getName();
    }

    private void assertParentExists(long parentId) {
        if (parentId == ROOT) {
            return;
        }
        if (departmentMapper.selectById(parentId) == null) {
            throw new DomainException("上级部门不存在或已被删除");
        }
    }

    /**
     * 从目标上级向上回溯，若途经自己，说明把节点挂到了自己的子树里。
     */
    private void assertNoCycle(long id, long parentId) {
        Map<Long, Long> parents = new HashMap<>();
        for (DepartmentRecord row : departmentMapper.selectAll()) {
            parents.put(row.getId(), row.getParentId());
        }
        long cursor = parentId;
        int guard = 0;
        while (cursor != ROOT && guard++ < 1000) {
            if (cursor == id) {
                throw new DomainException("不能把部门移动到自己的下级部门中");
            }
            Long next = parents.get(cursor);
            if (next == null) {
                return;
            }
            cursor = next;
        }
    }

    private void assertSiblingNameUnique(long parentId, String name, long excludeId) {
        if (departmentMapper.countSiblingName(parentId, name, excludeId) > 0) {
            throw new DomainException("同一上级下已存在同名部门");
        }
    }

    private static String requireName(String raw) {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty()) {
            throw new DomainException("请填写部门名称");
        }
        if (name.length() > 64) {
            throw new DomainException("部门名称不能超过 64 个字符");
        }
        return name;
    }

    private static String validateCode(String raw) {
        String code = raw == null ? "" : raw.trim().toUpperCase();
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw new DomainException("部门编码只能包含大写字母、数字、下划线和短横线");
        }
        return code;
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}

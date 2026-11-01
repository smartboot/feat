package tech.smartboot.feat.demo.plus.auth.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.demo.plus.auth.mapper.AuthMapper;
import tech.smartboot.feat.demo.plus.auth.model.AuthAccount;
import tech.smartboot.feat.demo.plus.auth.model.AuthRole;
import tech.smartboot.feat.demo.plus.common.DataScope;
import tech.smartboot.feat.demo.plus.department.service.DepartmentService;

/**
 * 把「账号挂了哪些角色」翻译成一次请求的访问上下文。
 * <p>
 * 独立成一个组件而不是塞进 AccessGuard，是因为登录响应（/api/auth/me）也需要同样的
 * dataScope / scopeDescription，而它手上没有 HTTP 上下文。
 * <p>
 * 多角色的合并规则：
 * <ul>
 *   <li>权限点取<b>并集</b>——挂两个角色就是两个角色能力的合集；</li>
 *   <li>数据范围取<b>最宽</b>——「仅本人」和「本部门」同时存在时按本部门算。
 *       这是有意的选择：范围收紧会让人干不了活，放宽只是多看到几个同事；</li>
 *   <li>停用的角色整条失效，两个维度都不贡献。</li>
 * </ul>
 */
@Bean
public class DataScopeResolver {

    @Autowired
    private AuthMapper authMapper;

    @Autowired
    private DepartmentService departmentService;

    public AccessContext resolve(AuthAccount account) {
        List<String> permissions = account.effectivePermissions();
        List<AuthRole> roles = account.enabledRoles();

        // 超管是兜底通道：即使 ADMIN 角色的数据范围被改坏，也不该把管理员锁在自己的数据之外。
        if (account.hasRole("ADMIN")) {
            return AccessContext.unrestricted(account.getId(), account.getDepartmentId(), permissions);
        }
        if (roles.isEmpty()) {
            return AccessContext.scoped(account.getId(), account.getDepartmentId(), permissions,
                    DataScope.SELF, true, null);
        }

        boolean unrestricted = false;
        boolean allSelf = true;
        List<String> customRoleCodes = new ArrayList<>();

        for (AuthRole role : roles) {
            int scope = DataScope.normalize(role.getDepartmentScope());
            if (scope == DataScope.ALL) {
                unrestricted = true;
            } else if (scope == DataScope.CUSTOM) {
                customRoleCodes.add(role.getCode());
            }
            if (scope != DataScope.SELF) {
                allSelf = false;
            }
        }

        if (unrestricted) {
            return AccessContext.unrestricted(account.getId(), account.getDepartmentId(), permissions);
        }
        if (allSelf) {
            return AccessContext.scoped(account.getId(), account.getDepartmentId(), permissions,
                    DataScope.SELF, true, null);
        }

        // 可见部门是各角色范围的并集：自定义配置的部门 + 本部门 + 本部门子树。
        Set<Long> departmentIds = new LinkedHashSet<>();
        if (!customRoleCodes.isEmpty()) {
            departmentIds.addAll(authMapper.selectCustomDepartmentIds(customRoleCodes));
        }
        Long ownDepartmentId = account.getDepartmentId();
        if (ownDepartmentId != null) {
            for (AuthRole role : roles) {
                int scope = DataScope.normalize(role.getDepartmentScope());
                if (scope == DataScope.DEPARTMENT) {
                    departmentIds.add(ownDepartmentId);
                } else if (scope == DataScope.DEPARTMENT_AND_CHILD) {
                    departmentIds.addAll(departmentService.subtreeIds(ownDepartmentId));
                }
            }
        }

        return AccessContext.scoped(account.getId(), ownDepartmentId, permissions, widestScope(roles),
                false, new ArrayList<>(departmentIds));
    }

    /**
     * 只用于展示「你的数据范围是 X」。真正的判定看 departmentIds 集合，不看这个值。
     */
    private static int widestScope(List<AuthRole> roles) {
        boolean hasDepartment = false;
        boolean hasDepartmentAndChild = false;
        boolean hasCustom = false;
        for (AuthRole role : roles) {
            int scope = DataScope.normalize(role.getDepartmentScope());
            if (scope == DataScope.DEPARTMENT_AND_CHILD) {
                hasDepartmentAndChild = true;
            } else if (scope == DataScope.DEPARTMENT) {
                hasDepartment = true;
            } else if (scope == DataScope.CUSTOM) {
                hasCustom = true;
            }
        }
        if (hasDepartmentAndChild) {
            return DataScope.DEPARTMENT_AND_CHILD;
        }
        if (hasDepartment) {
            return DataScope.DEPARTMENT;
        }
        if (hasCustom) {
            return DataScope.CUSTOM;
        }
        return DataScope.SELF;
    }
}

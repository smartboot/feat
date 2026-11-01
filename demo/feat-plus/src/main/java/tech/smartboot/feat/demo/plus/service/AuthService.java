package tech.smartboot.feat.demo.plus.service;

import java.util.ArrayList;
import java.util.List;

import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.demo.plus.common.DomainException;
import tech.smartboot.feat.demo.plus.dto.CurrentUserResponse;
import tech.smartboot.feat.demo.plus.dto.LoginRequest;
import tech.smartboot.feat.demo.plus.dto.LoginResponse;
import tech.smartboot.feat.demo.plus.dto.MenuItemResponse;
import tech.smartboot.feat.demo.plus.mapper.AuthMapper;
import tech.smartboot.feat.demo.plus.mapper.LoginStatMapper;
import tech.smartboot.feat.demo.plus.model.AuthAccount;
import tech.smartboot.feat.demo.plus.security.AccessContext;
import tech.smartboot.feat.demo.plus.security.DataScopeResolver;
import tech.smartboot.feat.demo.plus.security.MenuItem;
import tech.smartboot.feat.demo.plus.security.MenuRegistry;
import tech.smartboot.feat.demo.plus.security.PasswordHasher;
import tech.smartboot.feat.demo.plus.token.IssuedToken;
import tech.smartboot.feat.demo.plus.token.TokenStore;
import tech.smartboot.feat.demo.plus.tx.TxTemplate;

/**
 * 登录与当前用户信息。
 */
@Bean
public class AuthService {

    private static final String GENERIC_FAILURE = "用户名或密码错误";

    @Autowired
    private AuthMapper authMapper;

    @Autowired
    private TokenStore tokenStore;

    @Autowired
    private DataScopeResolver dataScopeResolver;

    @Autowired
    private TxTemplate txTemplate;

    public LoginResponse login(LoginRequest request) {
        String username = request == null ? null : trim(request.getUsername());
        String password = request == null ? null : request.getPassword();
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            throw new DomainException("请输入用户名和密码");
        }

        AuthAccount account = authMapper.selectByUsername(username);
        if (account == null || !PasswordHasher.matches(password, account.getPasswordHash())) {
            throw new DomainException(GENERIC_FAILURE);
        }
        if (account.getStatus() == 0) {
            throw new DomainException("该账号已被停用，请联系管理员");
        }

        account.setRoles(authMapper.selectRolesByUserId(account.getId()));
        IssuedToken token = tokenStore.issue(account.getId());
        recordLogin();
        return new LoginResponse(token.getValue(), token.getExpiresIn(), toCurrentUser(account));
    }

    /**
     * 登录成功后在当天统计上累加，驱动仪表盘趋势。
     * <p>
     * 「累加，没有就插入」是两条语句，必须在同一个事务里：否则并发登录时可能累加落空，
     * 紧接着的插入又撞主键，当天的登录数就这么少了一次，且没有任何报错。
     * <p>
     * 统计失败一律吞掉——不能因为趋势图写不进去就让人登不进来。
     */
    private void recordLogin() {
        try {
            txTemplate.run(session -> {
                LoginStatMapper stats = session.getMapper(LoginStatMapper.class);
                if (stats.increaseToday() == 0) {
                    stats.insertToday();
                }
            });
        } catch (RuntimeException ignored) {
            // 统计失败不应影响登录本身
        }
    }

    public CurrentUserResponse profile(long userId) {
        AuthAccount account = authMapper.selectById(userId);
        if (account == null) {
            throw DomainException.unauthorized();
        }
        account.setRoles(authMapper.selectRolesByUserId(userId));
        return toCurrentUser(account);
    }

    public void logout(String token) {
        tokenStore.remove(token);
    }

    private CurrentUserResponse toCurrentUser(AuthAccount account) {
        AccessContext access = dataScopeResolver.resolve(account);
        return new CurrentUserResponse(account.getId(), account.getUsername(), account.getDisplayName(),
                account.roleCodes(), account.roleNames(), access.getPermissions(),
                access.getDataScope(), access.describeScope(), toMenus(access.getPermissions()));
    }

    /**
     * 菜单按实际生效的权限点过滤后下发。
     * <p>
     * 这是「显示层」的过滤，不是安全边界：漏了最多是侧栏多一条，点进去仍然被 {@code AccessGuard} 拦住。
     * 反过来也成立——菜单能看见不等于接口放行，两者各自独立校验。
     */
    private static List<MenuItemResponse> toMenus(List<String> permissions) {
        List<MenuItem> items = MenuRegistry.visibleFor(permissions);
        List<MenuItemResponse> menus = new ArrayList<>(items.size());
        for (MenuItem item : items) {
            menus.add(new MenuItemResponse(item.groupKey, item.groupLabel, item.path, item.name,
                    item.icon, item.permission, item.sort, item.visible));
        }
        return menus;
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}

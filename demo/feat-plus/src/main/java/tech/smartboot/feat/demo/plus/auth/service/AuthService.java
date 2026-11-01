package tech.smartboot.feat.demo.plus.auth.service;

import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.demo.plus.auth.dto.CurrentUserResponse;
import tech.smartboot.feat.demo.plus.auth.dto.LoginRequest;
import tech.smartboot.feat.demo.plus.auth.dto.LoginResponse;
import tech.smartboot.feat.demo.plus.auth.mapper.AuthMapper;
import tech.smartboot.feat.demo.plus.auth.model.AuthAccount;
import tech.smartboot.feat.demo.plus.auth.token.IssuedToken;
import tech.smartboot.feat.demo.plus.auth.token.TokenStore;
import tech.smartboot.feat.demo.plus.common.DomainException;
import tech.smartboot.feat.demo.plus.dashboard.mapper.LoginStatMapper;

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
    private LoginStatMapper loginStatMapper;

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

        IssuedToken token = tokenStore.issue(account.getId());
        recordLogin();
        return new LoginResponse(token.getValue(), token.getExpiresIn(), toCurrentUser(account));
    }

    /**
     * 登录成功后在当天统计上累加，驱动仪表盘趋势。
     * 当天首条记录需要插入，并发登录时可能撞主键，忽略即可。
     */
    private void recordLogin() {
        try {
            if (loginStatMapper.increaseToday() == 0) {
                loginStatMapper.insertToday();
            }
        } catch (RuntimeException ignored) {
            // 统计失败不应影响登录本身
        }
    }

    public CurrentUserResponse profile(long userId) {
        AuthAccount account = authMapper.selectById(userId);
        if (account == null) {
            throw DomainException.unauthorized();
        }
        return toCurrentUser(account);
    }

    public void logout(String token) {
        tokenStore.remove(token);
    }

    private CurrentUserResponse toCurrentUser(AuthAccount account) {
        return new CurrentUserResponse(account.getId(), account.getUsername(),
                account.getDisplayName(), account.getRoleCode());
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}

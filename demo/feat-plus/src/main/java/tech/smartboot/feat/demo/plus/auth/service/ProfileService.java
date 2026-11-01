package tech.smartboot.feat.demo.plus.auth.service;

import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.demo.plus.auth.dto.ChangePasswordRequest;
import tech.smartboot.feat.demo.plus.auth.dto.CurrentUserResponse;
import tech.smartboot.feat.demo.plus.auth.dto.ProfileResponse;
import tech.smartboot.feat.demo.plus.auth.dto.ProfileUpdateRequest;
import tech.smartboot.feat.demo.plus.auth.mapper.ProfileMapper;
import tech.smartboot.feat.demo.plus.auth.model.ProfileRecord;
import tech.smartboot.feat.demo.plus.auth.token.TokenStore;
import tech.smartboot.feat.demo.plus.common.DomainException;

/**
 * 个人中心：任何登录用户都能改自己的资料与密码。
 * <p>
 * 这里刻意**不做任何权限点校验**：改自己的昵称、换自己的密码属于基本权利，
 * 要求 user:write 会让普通成员永远改不了密码，只能找管理员重置——
 * 那正是这个模块要修掉的缺陷。
 * <p>
 * 但边界要守住：能改的只有 displayName / email / password 三项。
 * 部门、角色、状态属于管理动作，必须走管理员接口，那套接口带数据范围校验。
 */
@Bean
public class ProfileService {

    /** 与前端提示保持一致；接入参数配置后应改为从 sys_config 读取。 */
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 64;
    private static final int MAX_DISPLAY_NAME_LENGTH = 32;
    private static final int MAX_EMAIL_LENGTH = 128;

    @Autowired
    private ProfileMapper profileMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private TokenStore tokenStore;

    public ProfileResponse get(long userId) {
        return ProfileResponse.from(require(userId), authService.profile(userId));
    }

    public ProfileResponse update(long userId, ProfileUpdateRequest request) {
        if (request == null) {
            throw new DomainException("缺少请求内容");
        }
        String displayName = trim(request.getDisplayName());
        if (displayName == null || displayName.isEmpty()) {
            throw new DomainException("昵称不能为空");
        }
        if (displayName.length() > MAX_DISPLAY_NAME_LENGTH) {
            throw new DomainException("昵称不能超过 " + MAX_DISPLAY_NAME_LENGTH + " 个字符");
        }
        String email = trim(request.getEmail());
        if (email != null && !email.isEmpty()) {
            if (email.length() > MAX_EMAIL_LENGTH) {
                throw new DomainException("邮箱不能超过 " + MAX_EMAIL_LENGTH + " 个字符");
            }
            if (!isEmailLike(email)) {
                throw new DomainException("邮箱格式不正确");
            }
        }

        // require 一次，既确认账号还在，也让「改已被删除的账号」统一返回 401 而不是静默成功。
        require(userId);
        profileMapper.updateProfile(userId, displayName, email, userId);
        return ProfileResponse.from(require(userId), authService.profile(userId));
    }

    /**
     * 改密码。
     * <p>
     * 改完会踢掉该用户的其它会话，只保留当前这一条：
     * 密码是凭证失效的唯一信号，旧密码换掉后旧会话还能用，改密码就只是心理安慰。
     *
     * @return 被踢掉的会话数
     */
    public int changePassword(long userId, ChangePasswordRequest request, String keepToken) {
        if (request == null) {
            throw new DomainException("缺少请求内容");
        }
        String oldPassword = request.getOldPassword();
        String newPassword = request.getNewPassword();
        if (oldPassword == null || oldPassword.isEmpty()) {
            throw new DomainException("请输入当前密码");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            throw new DomainException("请输入新密码");
        }
        if (newPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new DomainException("新密码至少 " + MIN_PASSWORD_LENGTH + " 位");
        }
        if (newPassword.length() > MAX_PASSWORD_LENGTH) {
            throw new DomainException("新密码不能超过 " + MAX_PASSWORD_LENGTH + " 位");
        }

        require(userId);
        String currentHash = profileMapper.selectPasswordHash(userId);
        if (currentHash == null || !PasswordHasher.matches(oldPassword, currentHash)) {
            // 提示语与登录失败一致地模糊：不要告诉调用方「密码对不对」以外的东西。
            throw new DomainException("当前密码不正确");
        }
        if (PasswordHasher.matches(newPassword, currentHash)) {
            throw new DomainException("新密码不能与当前密码相同");
        }

        profileMapper.updatePassword(userId, PasswordHasher.hash(newPassword), userId);
        return tokenStore.removeOthers(userId, keepToken);
    }

    private ProfileRecord require(long userId) {
        ProfileRecord record = profileMapper.selectById(userId);
        if (record == null) {
            throw DomainException.unauthorized();
        }
        return record;
    }

    /**
     * 只做「看起来像邮箱」的校验：有且只有一个 @、两侧非空、带点号域名。
     * 真要做严格校验只能靠发信验证，正则写复杂了反而会拒掉合法地址。
     */
    private static boolean isEmailLike(String email) {
        int at = email.indexOf('@');
        if (at <= 0 || at != email.lastIndexOf('@') || at == email.length() - 1) {
            return false;
        }
        String domain = email.substring(at + 1);
        return domain.indexOf('.') > 0 && !domain.startsWith(".") && !domain.endsWith(".");
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}

package tech.smartboot.feat.demo.plus.controller;

import tech.smartboot.feat.cloud.RestResult;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.cloud.annotation.RequestMethod;
import tech.smartboot.feat.demo.plus.common.ApiController;
import tech.smartboot.feat.demo.plus.dto.ChangePasswordRequest;
import tech.smartboot.feat.demo.plus.dto.ProfileResponse;
import tech.smartboot.feat.demo.plus.dto.ProfileUpdateRequest;
import tech.smartboot.feat.demo.plus.security.AuthGuard;
import tech.smartboot.feat.demo.plus.service.ProfileService;
import tech.smartboot.feat.demo.plus.token.TokenStore;
import tech.smartboot.feat.router.Context;

/**
 * 个人中心。全部接口只要求登录，不要求任何权限点。
 * <p>
 * 与 /api/users 的边界：这里只能改「自己的」昵称、邮箱、密码；
 * 部门、角色、状态属于管理动作，走 /api/users 并受数据范围约束。
 */
@Controller("api/profile")
public class ProfileController extends ApiController {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private TokenStore tokenStore;

    @RequestMapping(value = "", method = RequestMethod.GET)
    public RestResult<ProfileResponse> get(Context context) {
        return execute(context, () -> profileService.get(AuthGuard.require(context, tokenStore)));
    }

    @RequestMapping(value = "", method = RequestMethod.PUT)
    public RestResult<ProfileResponse> update(Context context, ProfileUpdateRequest request) {
        return execute(context, () -> profileService.update(AuthGuard.require(context, tokenStore), request));
    }

    /**
     * 返回被踢掉的其它会话数，前端据此提示「其他 N 个设备已下线」。
     * 返回 0 不代表失败——只是这个人当前只有一处登录。
     */
    @RequestMapping(value = "/password", method = RequestMethod.POST)
    public RestResult<Integer> changePassword(Context context, ChangePasswordRequest request) {
        return execute(context, () -> {
            long userId = AuthGuard.require(context, tokenStore);
            return profileService.changePassword(userId, request, AuthGuard.readToken(context));
        });
    }
}

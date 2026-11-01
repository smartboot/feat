package tech.smartboot.feat.demo.plus.controller;

import tech.smartboot.feat.cloud.RestResult;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.cloud.annotation.RequestMethod;
import tech.smartboot.feat.demo.plus.common.ApiController;
import tech.smartboot.feat.demo.plus.dto.CurrentUserResponse;
import tech.smartboot.feat.demo.plus.dto.LoginRequest;
import tech.smartboot.feat.demo.plus.dto.LoginResponse;
import tech.smartboot.feat.demo.plus.security.AuthGuard;
import tech.smartboot.feat.demo.plus.service.AuthService;
import tech.smartboot.feat.demo.plus.token.TokenStore;
import tech.smartboot.feat.router.Context;

@Controller("api/auth")
public class AuthController extends ApiController {

    @Autowired
    private AuthService authService;

    @Autowired
    private TokenStore tokenStore;

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public RestResult<LoginResponse> login(Context context, LoginRequest request) {
        return execute(context, () -> authService.login(request));
    }

    @RequestMapping(value = "/me", method = RequestMethod.GET)
    public RestResult<CurrentUserResponse> me(Context context) {
        return execute(context, () -> authService.profile(AuthGuard.require(context, tokenStore)));
    }

    @RequestMapping(value = "/logout", method = RequestMethod.POST)
    public RestResult<Void> logout(Context context) {
        return execute(context, () -> {
            AuthGuard.require(context, tokenStore);
            authService.logout(AuthGuard.readToken(context));
            return (Void) null;
        });
    }
}

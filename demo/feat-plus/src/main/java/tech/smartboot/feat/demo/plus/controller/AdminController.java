package tech.smartboot.feat.demo.plus.controller;

import tech.smartboot.feat.cloud.RestResult;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.cloud.annotation.RequestMethod;
import tech.smartboot.feat.demo.plus.common.ApiController;
import tech.smartboot.feat.demo.plus.security.AccessGuard;
import tech.smartboot.feat.demo.plus.security.Permission;
import tech.smartboot.feat.demo.plus.seed.DataInitializer;
import tech.smartboot.feat.router.Context;

@Controller("api/admin")
public class AdminController extends ApiController {

    /**
     * 演示专用：真实项目接入时，连同 {@link DataInitializer} 一起从代码里删掉。
     * 业务 Service 不持有它，避免「核心依赖演示设施」的反向依赖。
     */
    @Autowired
    private DataInitializer dataInitializer;

    @Autowired
    private AccessGuard accessGuard;

    /**
     * 重置演示数据会清空所有业务表，属于系统级动作，要求 system:write。
     */
    @RequestMapping(value = "/reset-demo", method = RequestMethod.POST)
    public RestResult<Void> resetDemo(Context context) {
        return execute(context, () -> {
            accessGuard.require(context, Permission.SYSTEM_WRITE);
            dataInitializer.reset();
            return (Void) null;
        });
    }
}

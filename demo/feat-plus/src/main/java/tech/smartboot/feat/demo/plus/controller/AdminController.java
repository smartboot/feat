package tech.smartboot.feat.demo.plus.controller;

import tech.smartboot.feat.cloud.RestResult;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.cloud.annotation.RequestMethod;
import tech.smartboot.feat.demo.plus.common.ApiController;
import tech.smartboot.feat.demo.plus.security.AccessGuard;
import tech.smartboot.feat.demo.plus.security.DepartmentSnapshot;
import tech.smartboot.feat.demo.plus.security.Permission;
import tech.smartboot.feat.demo.plus.seed.DataInitializer;
import tech.smartboot.feat.router.Context;

@Controller("api/admin")
public class AdminController extends ApiController {

    @Autowired
    private AccessGuard accessGuard;

    /**
     * 演示专用：真实项目接入时，连同 {@link DataInitializer} 一起从代码里删掉。
     * 业务 Service 不持有它，避免「核心依赖演示设施」的反向依赖。
     */
    @Autowired
    private DataInitializer dataInitializer;

    /**
     * 重置会把部门表 TRUNCATE 后重新播种，部门 id 全部变新，而缓存里还是上一批 id。
     * 不清这一处，重置后所有「本部门及以下」的账号都会按旧 id 算范围——查到空列表，
     * 或者更糟，算出不相干的部门。这是加缓存之后新增的失效点，不是历史遗留问题：
     * 凡是「绕过 Service 直接改库」的路径，都必须自己负责清缓存。
     */
    @Autowired
    private DepartmentSnapshot departmentSnapshot;

    /**
     * 重置演示数据会清空所有业务表，属于系统级动作，要求 system:write。
     */
    @RequestMapping(value = "/reset-demo", method = RequestMethod.POST)
    public RestResult<Void> resetDemo(Context context) {
        return execute(context, () -> {
            accessGuard.require(context, Permission.SYSTEM_WRITE);
            dataInitializer.reset();
            departmentSnapshot.invalidate();
            return (Void) null;
        });
    }
}

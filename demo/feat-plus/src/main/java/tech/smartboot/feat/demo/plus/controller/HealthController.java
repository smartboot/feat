package tech.smartboot.feat.demo.plus.controller;

import tech.smartboot.feat.cloud.RestResult;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.cloud.annotation.RequestMethod;
import tech.smartboot.feat.demo.plus.common.ApiController;
import tech.smartboot.feat.demo.plus.dto.HealthResponse;
import tech.smartboot.feat.demo.plus.service.HealthService;
import tech.smartboot.feat.router.Context;

/**
 * 存活与就绪探针，都是免登录的——编排器手里没有业务令牌，探测不应该依赖登录态。
 * <p>
 * 两个探针的分工是容器编排的标准语义：
 * <ul>
 * <li>{@code GET /api/health}：进程活着没有。不碰数据库，数据库挂了也应该回 200，
 *     让编排器相信「重启进程没用」；</li>
 * <li>{@code GET /api/health/ready}：能不能接流量。做一次最小数据库往返，
 *     数据库不通回 500，编排器把它摘出负载均衡但不重启。</li>
 * </ul>
 * 失败时的 detail 只说「数据库不可用」，不透出驱动/SQL 细节，与自检接口同一取舍。
 */
@Controller("api/health")
public class HealthController extends ApiController {

    @Autowired
    private HealthService healthService;

    @RequestMapping(value = "", method = RequestMethod.GET)
    public RestResult<HealthResponse> live(Context context) {
        return execute(context, healthService::live);
    }

    @RequestMapping(value = "/ready", method = RequestMethod.GET)
    public RestResult<HealthResponse> ready(Context context) {
        return execute(context, healthService::ready);
    }
}

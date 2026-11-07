package tech.smartboot.feat.demo.plus.controller;

import java.util.List;

import tech.smartboot.feat.cloud.RestResult;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.cloud.annotation.RequestMethod;
import tech.smartboot.feat.demo.plus.common.ApiController;
import tech.smartboot.feat.demo.plus.dto.ProbeResult;
import tech.smartboot.feat.demo.plus.security.AccessGuard;
import tech.smartboot.feat.demo.plus.security.Permission;
import tech.smartboot.feat.demo.plus.service.SelfCheckService;
import tech.smartboot.feat.router.Context;

/**
 * 地基自检。部署到新环境（换库、换驱动、上容器）后先跑一次，比等业务出事再回头查便宜得多。
 * <p>
 * 需要 {@code system:write}：自检会尝试写入并回滚，不是纯只读操作。
 * 每条结果只有 name / ok / detail 三个字段，细节留在日志里，不通过 HTTP 泄漏 SQL。
 */
@Controller("api/system/selftest")
public class SelfCheckController extends ApiController {

    @Autowired
    private AccessGuard accessGuard;

    @Autowired
    private SelfCheckService selfCheckService;

    @RequestMapping(value = "", method = RequestMethod.GET)
    public RestResult<List<ProbeResult>> run(Context context) {
        return execute(context, () -> {
            accessGuard.require(context, Permission.SYSTEM_WRITE);
            return selfCheckService.run();
        });
    }
}

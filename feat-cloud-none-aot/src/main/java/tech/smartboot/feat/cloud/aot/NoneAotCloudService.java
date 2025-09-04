/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.aot;

import tech.smartboot.feat.cloud.AbstractCloudService;
import tech.smartboot.feat.cloud.ApplicationContext;
import tech.smartboot.feat.router.Router;

/**
 * @author 三刀
 * @version v1.0 9/4/25
 */
public class NoneAotCloudService extends AbstractCloudService {
    @Override
    public void loadBean(ApplicationContext context) throws Throwable {

    }

    @Override
    public void autowired(ApplicationContext context) throws Throwable {

    }

    @Override
    public void postConstruct(ApplicationContext context) throws Throwable {

    }

    @Override
    public void destroy() throws Throwable {

    }

    @Override
    public void router(ApplicationContext context, Router router) {

    }
}

/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.session;

import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.Session;
import tech.smartboot.feat.router.session.SessionManager;
import tech.smartboot.redisun.Redisun;

/**
 * @author 三刀
 * @version v1.0 12/16/25
 */
public class ClusterSessionManager extends SessionManager {
    private Redisun redisun;

    public ClusterSessionManager(Redisun redisun) {
        this.redisun = redisun;
    }

    @Override
    public Session getSession(HttpRequest request, boolean create) {
        return null;
    }

    @Override
    public void updateAccessTime(HttpRequest request) {

    }

}

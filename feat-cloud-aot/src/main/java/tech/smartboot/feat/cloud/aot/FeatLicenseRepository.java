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

import java.util.Map;

/**
 * @author 三刀
 * @version v1.0 8/26/25
 */
public class FeatLicenseRepository {
    private Map<String, License> users;

    public Map<String, License> getUsers() {
        return users;
    }

    public void setUsers(Map<String, License> users) {
        this.users = users;
    }
}


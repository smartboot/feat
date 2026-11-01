package tech.smartboot.feat.demo.plus.dashboard.model;

import java.util.Date;

/**
 * 单日登录数，对应 sys_login_stat 的一行。
 */
public class DailyLogin {
    private Date statDate;
    private int loginCount;

    public Date getStatDate() {
        return statDate;
    }

    public void setStatDate(Date statDate) {
        this.statDate = statDate;
    }

    public int getLoginCount() {
        return loginCount;
    }

    public void setLoginCount(int loginCount) {
        this.loginCount = loginCount;
    }
}

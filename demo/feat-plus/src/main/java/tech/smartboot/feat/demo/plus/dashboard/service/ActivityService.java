package tech.smartboot.feat.demo.plus.dashboard.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.demo.plus.auth.service.AccessContext;
import tech.smartboot.feat.demo.plus.common.DomainException;
import tech.smartboot.feat.demo.plus.common.page.PageResult;
import tech.smartboot.feat.demo.plus.dashboard.dto.ActivityItemResponse;
import tech.smartboot.feat.demo.plus.dashboard.dto.OperatorOptionResponse;
import tech.smartboot.feat.demo.plus.dashboard.mapper.ActivityMapper;
import tech.smartboot.feat.demo.plus.dashboard.model.ActivityRecord;

/**
 * 审计日志查询。
 * <p>
 * 只读：日志由 {@link ActivityRecorder} 在写操作时落库，没有任何接口能改或删单条日志。
 * 筛选条件全部可选，时间按「自然日」理解——传 to=2026-08-02 时，8 月 2 日当天的记录也算在内，
 * 所以下界用 &gt;=、上界用 &lt; 次日零点。
 */
@Bean
public class ActivityService {

    private static final int MAX_PAGE_SIZE = 200;

    @Autowired
    private ActivityMapper activityMapper;

    /**
     * 审计列表。access 不是可选项——日志里写清了「谁改了谁」，能看用户列表不等于能看全公司的操作记录，
     * 所以这条查询必须和用户列表走同一套数据范围。
     */
    public PageResult<ActivityItemResponse> list(String kind, Long operatorId, String from, String to,
                                                 int page, int pageSize, AccessContext access) {
        Date fromDate = parseDay(from, 0);
        Date toDate = parseDay(to, 1);
        int safePage = page < 1 ? 1 : page;
        int safeSize = pageSize < 1 ? 10 : Math.min(pageSize, MAX_PAGE_SIZE);
        List<Long> scope = access.queryDepartmentIds();
        Long selfOnly = access.selfOnlyUserId();

        long total = activityMapper.countPage(emptyToNull(kind), operatorId, fromDate, toDate, scope, selfOnly);
        List<ActivityRecord> rows = activityMapper.selectPage(emptyToNull(kind), operatorId, fromDate, toDate,
                safeSize, (safePage - 1) * safeSize, scope, selfOnly);
        List<ActivityItemResponse> items = new ArrayList<>(rows.size());
        for (ActivityRecord row : rows) {
            items.add(new ActivityItemResponse(row));
        }
        return new PageResult<>(items, total, safePage, safeSize);
    }

    public List<OperatorOptionResponse> operators(AccessContext access) {
        List<ActivityRecord> rows = activityMapper.selectOperators(access.queryDepartmentIds(),
                access.selfOnlyUserId());
        List<OperatorOptionResponse> options = new ArrayList<>(rows.size());
        for (ActivityRecord row : rows) {
            String name = row.getOperatorName() == null ? "已删除用户 #" + row.getCreator() : row.getOperatorName();
            options.add(new OperatorOptionResponse(row.getCreator(), name));
        }
        return options;
    }

    /**
     * 把 yyyy-MM-dd 解析成当天零点，再按 plusDays 偏移。
     * 空值返回 null 表示不限；格式不对直接 400，避免用户误以为筛选生效了。
     */
    private static Date parseDay(String text, int plusDays) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd").parse(text.trim());
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.DAY_OF_MONTH, plusDays);
            return calendar.getTime();
        } catch (ParseException e) {
            throw new DomainException("日期格式应为 yyyy-MM-dd：" + text);
        }
    }

    private static String emptyToNull(String value) {
        return value == null || value.trim().isEmpty() || "all".equalsIgnoreCase(value.trim())
                ? null : value.trim();
    }
}

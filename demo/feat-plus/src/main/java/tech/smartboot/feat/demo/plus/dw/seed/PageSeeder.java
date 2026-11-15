package tech.smartboot.feat.demo.plus.dw.seed;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.ibatis.io.Resources;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import tech.smartboot.feat.demo.plus.dw.mapper.PageMapper;
import tech.smartboot.feat.demo.plus.dw.model.PageRecord;

/**
 * sys_page 初始配置的导入器：把 {@code pages/pages.json} 逐条写成配置行。
 * <p>
 * 配置的唯一真相在那份 JSON 里，Java 只负责搬运。放在 JSON 而不是 SQL 字符串里的原因很实际：
 * 它是嵌套结构，塞进 SQL 后不能出现 ASCII 分号（ScriptRunner 按分号切语句）、不能用单引号、
 * 整行无法 diff——三条纪律都只是为了迁就脚本执行器。
 * <p>
 * 这是 {@code dw} 模块自带的启动数据，所以放在本包内而不是 {@code seed} 包：
 * 谁定义表结构、谁定义配置格式，谁就该负责把它灌进去。
 * <p>
 * 导入只做结构校验，不做「列 / 筛选键是否存在于数据源」的交叉校验：那需要
 * {@code PageDataSourceRegistry}，而首次启动时 {@code DataInitializer} 的 @PostConstruct
 * 可能早于数据源登记（AOT 按全限定类名排序执行，不感知 bean 依赖）。交叉校验放在两处——
 * 保存配置时由 {@code PageService} 拒，提交前由 {@code scripts/check_menu.py} 查。
 */
public final class PageSeeder {

    /** 与 PageService.KEY_PATTERN 同源：pageKey 同时是菜单 path 段和前端拉配置的键。 */
    private static final String PAGES = "pages/pages.json";

    private static final Pattern KEY_PATTERN = Pattern.compile("^[a-z0-9_-]{1,32}$");

    private PageSeeder() {
    }

    /**
     * 表为空时导入；非空直接返回，所以首次启动、reset-demo、存量库升级三条路径可以共用。
     * <p>
     * 配置写错就在启动时炸，而不是让某个页面白屏到被人发现。
     */
    public static void seed(PageMapper pageMapper) {
        if (pageMapper.countAll() > 0) {
            return;
        }
        JSONArray seeds = read();
        Date now = new Date();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < seeds.size(); i++) {
            PageRecord record = toRecord(seeds.getJSONObject(i), i + 1, seen);
            record.setIsDeleted(0);
            record.setCreatedTime(now);
            record.setEditTime(now);
            pageMapper.insert(record);
        }
    }

    private static JSONArray read() {
        String text;
        try (InputStream in = Resources.getResourceAsStream(PAGES)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            text = new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("读取页面配置 " + PAGES + " 失败：" + e);
        }
        JSONArray seeds;
        try {
            seeds = JSON.parseArray(text);
        } catch (RuntimeException e) {
            throw new IllegalStateException(PAGES + " 不是合法的 JSON 数组：" + e);
        }
        if (seeds == null || seeds.isEmpty()) {
            throw new IllegalStateException(PAGES + " 里没有任何页面配置，首次启动会得到空侧栏");
        }
        return seeds;
    }

    /**
     * 一份配置 = 一行 sys_page。list / form 在 JSON 里是嵌套对象，落库时序列化成字符串，
     * 与「配置管理页保存」走完全相同的存储形态——运行期读路径不区分种子还是人工配置。
     */
    private static PageRecord toRecord(JSONObject seed, int ordinal, Set<String> seen) {
        String where = PAGES + " 第 " + ordinal + " 项";
        String pageKey = trim(seed.getString("pageKey"));
        if (pageKey == null || !KEY_PATTERN.matcher(pageKey).matches()) {
            throw new IllegalStateException(where + "的 pageKey 不合法：" + pageKey
                    + "（需匹配 " + KEY_PATTERN.pattern() + "）");
        }
        if (!seen.add(pageKey)) {
            throw new IllegalStateException(where + "的 pageKey 重复：" + pageKey);
        }
        String title = trim(seed.getString("title"));
        if (title == null) {
            throw new IllegalStateException(where + "（" + pageKey + "）缺少 title");
        }
        String datasourceKey = trim(seed.getString("datasourceKey"));
        if (datasourceKey == null) {
            throw new IllegalStateException(where + "（" + pageKey + "）缺少 datasourceKey");
        }
        JSONObject list = seed.getJSONObject("list");
        if (list == null || list.isEmpty()) {
            throw new IllegalStateException(where + "（" + pageKey + "）缺少 list 配置");
        }
        JSONObject form = seed.getJSONObject("form");
        if (form == null) {
            throw new IllegalStateException(where + "（" + pageKey + "）缺少 form 配置（只读页面写空 fields）");
        }

        PageRecord record = new PageRecord();
        record.setPageKey(pageKey);
        record.setTitle(title);
        record.setDescription(trim(seed.getString("description")));
        record.setDatasourceKey(datasourceKey);
        record.setListJson(list.toJSONString());
        record.setFormJson(form.toJSONString());
        return record;
    }

    private static String trim(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        return value.isEmpty() ? null : value;
    }
}

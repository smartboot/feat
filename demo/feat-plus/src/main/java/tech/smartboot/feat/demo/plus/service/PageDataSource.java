package tech.smartboot.feat.demo.plus.service;

import java.util.List;
import java.util.Map;

import tech.smartboot.feat.demo.plus.common.DomainException;
import tech.smartboot.feat.demo.plus.common.PageResult;
import tech.smartboot.feat.demo.plus.dto.DwFieldMeta;
import tech.smartboot.feat.demo.plus.dto.DwSaveRequest;
import tech.smartboot.feat.demo.plus.dto.PageDataQuery;
import tech.smartboot.feat.demo.plus.security.AccessContext;

/**
 * 动态页面的数据源：一个编译期注册的白名单条目，页面配置通过 datasourceKey 绑定它。
 * <p>
 * 这是「数据源注册表」路线的核心边界：<b>配置只描述怎么渲染，数据源描述怎么查、怎么存。</b>
 * 查询与保存留在类型化 Service 里，事务 / 审计 / 业务规则 / 数据权限全部免费复用；
 * 动态层拿到的只是行数据（Map），不接触 SQL、不接触 Mapper。
 * <p>
 * 权限点由实现编译期写死并声明：与 Permission / MenuRoutes 的「白名单在代码里」哲学一致，
 * 配置里挂权限点只会造出「字典里有、接口从不校验」的空权限。
 * <p>
 * 行数据的值只允许 String / Number / Boolean / List：Date 等类型由实现负责转成文本，
 * 否则序列化出来是毫秒数。动态层统一按字符串处理（switch 的 "1"/"0" 同理）。
 */
public interface PageDataSource {

    /** 数据源标识，sys_page.datasource_key 引用它。小写字母 / 数字 / 下划线 / 短横线。 */
    String key();

    /** 给人看的名称，配置管理页的下拉用。 */
    String label();

    /**
     * 可用字段声明——「防配置漂移」的锚点。列 / 筛选 / 表单字段的 key 必须命中这里，
     * 数据源改了字段名，保存页面配置时会立即报错，而不是渲染时静默空列。
     */
    List<DwFieldMeta> fields();

    /** 读权限点，Controller 统一 require。 */
    String readPermission();

    /** 写权限点；纯只读数据源返回与 readPermission 相同的值即可（canWrite 会算出 false）。 */
    String writePermission();

    /**
     * 是否支持保存与删除。返回 false 的数据源，页面配置里出现 create / edit / delete 动作
     * 会在保存配置时被拒绝——「按钮能点、保存 405」的错配提前到配置期暴露。
     */
    default boolean writable() {
        return false;
    }

    /**
     * 列表查询。筛选值的解释权在实现里（约定 input 模糊 / select 精确），
     * 分页由实现完成并回填 total。
     */
    PageResult<Map<String, Object>> list(PageDataQuery query, AccessContext access);

    /** 按 id 取一行，供表单编辑回填。不支持的源抛 DomainException。 */
    default Map<String, Object> get(long id, AccessContext access) {
        throw new DomainException("该数据源不支持按 id 查看");
    }

    /** 保存：id 为 null 表示新建。不支持的源抛 DomainException。 */
    default Map<String, Object> save(DwSaveRequest request, AccessContext access) {
        throw new DomainException("该数据源不支持保存");
    }

    /** 删除。不支持的源抛 DomainException。 */
    default void delete(long id, AccessContext access) {
        throw new DomainException("该数据源不支持删除");
    }
}

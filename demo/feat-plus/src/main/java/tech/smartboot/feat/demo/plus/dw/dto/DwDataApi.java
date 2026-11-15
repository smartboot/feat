package tech.smartboot.feat.demo.plus.dw.dto;

/**
 * 动态页面的<b>数据通道覆盖</b>：列表查询与行删除两个操作可改走自定义接口，
 * 留空则走内置的 {@code /api/dw/pages/{pageKey}/data} 通道，允许混搭
 * （例如查询走聚合接口、删除走内置通道）。
 * <p>
 * 边界刻意收窄到这两个操作：列表查询（读多、聚合场景多）与删除（幂等、形状简单）；
 * 保存 / 详情仍走内置通道——表单写路径牵涉校验与审计，不值得为它开旁路。
 * <p>
 * 自定义接口是「同一份数据的另一种通道」，不是另一份数据：行结构契约不变——
 * 行字段必须与数据源声明一致（列 / 筛选的校验锚点不因通道覆盖而放松），
 * 否则「存进去的一定能渲染」的保证就被绕开了。
 * <p>
 * 接口形状必须与内置通道对齐（权限由目标接口自己校验，动态层不再重复判权）：
 * <ul>
 *   <li>query：{@code GET {path}?page=&pageSize=&filter={"k":"v"}} →
 *       {@code RestResult<PageResult<Map<String,Object>>>}；filter 是 JSON 字符串，坏值按空过滤处理；</li>
 *   <li>delete：{@code DELETE {path}/{id}} → {@code RestResult<Void>}。</li>
 * </ul>
 */
public class DwDataApi {
    /** 列表查询路径，如 /api/roles/overview。以 /api/ 开头的站内相对路径。 */
    private String query = "";

    /** 删除路径，调用时会拼接 /{id}。 */
    private String delete = "";

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getDelete() {
        return delete;
    }

    public void setDelete(String delete) {
        this.delete = delete;
    }
}

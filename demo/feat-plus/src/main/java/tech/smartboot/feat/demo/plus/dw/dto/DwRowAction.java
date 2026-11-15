package tech.smartboot.feat.demo.plus.dw.dto;

/**
 * 自定义行操作：行操作菜单里排在内置「编辑 / 删除」之后的一个按钮。
 * <p>
 * 刻意只留三项配置——名称、提示框、接口路径。老平台把按钮 JS 整段存库，
 * 结果是「页面配置」变成「前端代码仓库」，谁都不敢改；这里换成一句可审计的声明：
 * <b>点了就 POST 这个站内接口，主键放在请求体里</b>。
 * <ul>
 *   <li>{@code label}：按钮文案，同时是审计口径（日志里记的就是它）；</li>
 *   <li>{@code confirm}：二次确认文案，留空表示点了直接执行——只该给可逆、低风险的动作留空；</li>
 *   <li>{@code api}：站内 {@code /api/} 路径，调用方式固定 POST，
 *       请求体为 {@code {"<rowKey>": <主键值>}}，权限由目标接口自己校验。</li>
 * </ul>
 * 不配请求体映射、不配脚本、不配参数模板：行操作只解决「拿这一行去触发一件事」，
 * 需要更复杂入参的场景不该塞进列表页配置。
 */
public class DwRowAction {
    private String label = "";
    private String confirm = "";
    private String api = "";

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label == null ? "" : label;
    }

    public String getConfirm() {
        return confirm;
    }

    public void setConfirm(String confirm) {
        this.confirm = confirm == null ? "" : confirm;
    }

    public String getApi() {
        return api;
    }

    public void setApi(String api) {
        this.api = api == null ? "" : api;
    }
}

package tech.smartboot.feat.demo.plus.dw.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 表单弹窗的渲染描述，即 sys_page.form_json 的结构。
 * <p>
 * create / edit 各自带标题与说明文案：配置驱动页往往只覆盖完整手写页的一部分能力，
 * 说明文案就是向用户交代边界的地方（「简化表单不含权限点，请到原页面补充」）。
 */
public class DwFormConfig {
    private String createTitle = "新建";
    private String editTitle = "编辑";
    private String createDescription = "";
    private String editDescription = "";
    private List<DwFormField> fields = new ArrayList<>();

    public String getCreateTitle() {
        return createTitle;
    }

    public void setCreateTitle(String createTitle) {
        this.createTitle = createTitle;
    }

    public String getEditTitle() {
        return editTitle;
    }

    public void setEditTitle(String editTitle) {
        this.editTitle = editTitle;
    }

    public String getCreateDescription() {
        return createDescription;
    }

    public void setCreateDescription(String createDescription) {
        this.createDescription = createDescription;
    }

    public String getEditDescription() {
        return editDescription;
    }

    public void setEditDescription(String editDescription) {
        this.editDescription = editDescription;
    }

    public List<DwFormField> getFields() {
        return fields;
    }

    public void setFields(List<DwFormField> fields) {
        this.fields = fields == null ? new ArrayList<>() : fields;
    }
}

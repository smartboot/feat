package tech.smartboot.feat.demo.plus.dto;

/**
 * 操作人选项，日志页的筛选下拉用。
 * 数据来自日志表本身而不是用户表：被删掉的人留下的历史记录同样要能按人筛。
 */
public class OperatorOptionResponse {

    private final long id;
    private final String name;

    public OperatorOptionResponse(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}

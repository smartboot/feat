package tech.smartboot.feat.cloud.annotation.orm;

/**
 * 嵌套查询加载策略，与 MyBatis {@code FetchType} 行为一致。
 */
public enum FetchType {
    DEFAULT,
    EAGER,
    LAZY
}

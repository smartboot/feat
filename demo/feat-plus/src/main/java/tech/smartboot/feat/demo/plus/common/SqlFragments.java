package tech.smartboot.feat.demo.plus.common;

/**
 * SQL 片段常量。
 * <p>
 * 必须放在普通类里，不能写成 Mapper 接口的字段：
 * Feat 的注解处理器在解析 @Mapper 时会把接口内的所有元素当作方法强转 ExecutableElement，
 * 接口里一旦出现常量字段就会抛 VarSymbol cannot be cast to ExecutableElement。
 */
public final class SqlFragments {

    /**
     * 除 password_hash 外的全部字段，含五个审计字段。
     * 列名 → 属性名靠 mybatis-config 的 mapUnderscoreToCamelCase 完成。
     */
    public static final String USER_COLUMNS = "id, username, display_name, role_code, department, email, status, "
            + "is_deleted, created_time, edit_time, creator, editor";

    /**
     * 列表与计数共用的过滤条件。
     * <p>
     * is_deleted = 0 放在最前面且不带 AND，改写词交给 <where> 处理前导连接词，
     * 这样无论后面有没有动态条件，拼出来的 WHERE 都是合法的。
     */
    public static final String USER_FILTER = "<where> is_deleted = 0"
            + "<if test=\"keywordLike != null\"> AND (LOWER(username) LIKE #{keywordLike}"
            + " OR LOWER(display_name) LIKE #{keywordLike}"
            + " OR LOWER(department) LIKE #{keywordLike}"
            + " OR LOWER(email) LIKE #{keywordLike})</if>"
            + "<if test=\"status != null\"> AND status = #{status}</if>"
            + "<if test=\"roleCode != null\"> AND role_code = #{roleCode}</if>"
            + "<if test=\"department != null\"> AND department = #{department}</if>"
            + "</where>";

    private SqlFragments() {
    }
}

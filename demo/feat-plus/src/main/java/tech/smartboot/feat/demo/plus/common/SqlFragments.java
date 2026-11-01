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
     * 用户列表字段。部门名称来自左连接，因此部门改名后列表自动跟随，不需要回写用户表。
     * 列名 → 属性名靠 mybatis-config 的 mapUnderscoreToCamelCase 完成。
     */
    public static final String USER_COLUMNS = "u.id, u.username, u.display_name, u.role_code, "
            + "u.department_id, d.name AS department_name, u.email, u.status, "
            + "u.is_deleted, u.created_time, u.edit_time, u.creator, u.editor";

    /**
     * 用户查询的 FROM 段。部门可能已被软删除，所以用 LEFT JOIN 并带上 is_deleted = 0 条件。
     */
    public static final String USER_FROM = " FROM sys_user u "
            + "LEFT JOIN sys_department d ON d.id = u.department_id AND d.is_deleted = 0";

    /**
     * 列表与计数共用的过滤条件。
     * <p>
     * is_deleted = 0 放在最前面且不带 AND，改写词交给 <where> 处理前导连接词，
     * 这样无论后面有没有动态条件，拼出来的 WHERE 都是合法的。
     * <p>
     * departmentIds 是「部门及其全部下级」的 id 集合，由 DepartmentService 算好后传入，
     * 因此按部门筛选时默认包含子部门。
     */
    public static final String USER_FILTER = "<where> u.is_deleted = 0"
            + "<if test=\"keywordLike != null\"> AND (LOWER(u.username) LIKE #{keywordLike}"
            + " OR LOWER(u.display_name) LIKE #{keywordLike}"
            + " OR LOWER(d.name) LIKE #{keywordLike}"
            + " OR LOWER(u.email) LIKE #{keywordLike})</if>"
            + "<if test=\"status != null\"> AND u.status = #{status}</if>"
            + "<if test=\"roleCode != null\"> AND u.role_code = #{roleCode}</if>"
            + "<if test=\"departmentIds != null\"> AND u.department_id IN "
            + "<foreach collection=\"departmentIds\" item=\"deptId\" open=\"(\" separator=\",\" close=\")\">"
            + "#{deptId}</foreach></if>"
            + "</where>";

    public static final String ROLE_COLUMNS = "id, code, name, description, permissions, builtin, status, "
            + "is_deleted, created_time, edit_time, creator, editor";

    public static final String DEPARTMENT_COLUMNS = "id, parent_id, code, name, sort_order, description, status, "
            + "is_deleted, created_time, edit_time, creator, editor";

    private SqlFragments() {
    }
}

package tech.smartboot.feat.demo.plus.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import tech.smartboot.feat.demo.plus.model.ProfileRecord;

/**
 * 个人中心的自助读写。
 * <p>
 * 与 AuthMapper 分开：AuthMapper 是整个登录链路（按用户名查、取角色），
 * 这里只处理「当前登录人改自己」的两件事，混在一起会让登录路径多出无关的 SQL。
 * <p>
 * 共同约束：全部带 is_deleted = 0。已删除的账号即使还握着有效令牌，也不能再改资料。
 * 注意这里**没有**权限点校验——改自己的资料不该需要 user:write，
 * 否则普通成员永远改不了自己的昵称。
 */
@Mapper
public interface ProfileMapper {

    @Select("SELECT u.id, u.username, u.display_name, u.email, u.department_id, d.name AS department_name, "
            + "u.status, u.created_time "
            + "FROM sys_user u LEFT JOIN sys_department d ON d.id = u.department_id AND d.is_deleted = 0 "
            + "WHERE u.id = #{id} AND u.is_deleted = 0")
    ProfileRecord selectById(@Param("id") long id);

    @Select("SELECT password_hash FROM sys_user WHERE id = #{id} AND is_deleted = 0")
    String selectPasswordHash(@Param("id") long id);

    /**
     * 只允许改昵称与邮箱。用户名是登录标识，改它等于换身份；
     * 部门与角色是管理动作，必须走管理员的那套接口（带数据范围校验）。
     */
    @Update("UPDATE sys_user SET display_name = #{displayName}, email = #{email}, "
            + "edit_time = CURRENT_TIMESTAMP, editor = #{editor} "
            + "WHERE id = #{id} AND is_deleted = 0")
    int updateProfile(@Param("id") long id,
                      @Param("displayName") String displayName,
                      @Param("email") String email,
                      @Param("editor") long editor);

    @Update("UPDATE sys_user SET password_hash = #{passwordHash}, "
            + "edit_time = CURRENT_TIMESTAMP, editor = #{editor} "
            + "WHERE id = #{id} AND is_deleted = 0")
    int updatePassword(@Param("id") long id,
                       @Param("passwordHash") String passwordHash,
                       @Param("editor") long editor);
}

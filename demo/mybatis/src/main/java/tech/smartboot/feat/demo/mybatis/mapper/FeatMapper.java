 package tech.smartboot.feat.demo.mybatis.mapper;

import tech.smartboot.feat.cloud.annotation.orm.Delete;
import tech.smartboot.feat.cloud.annotation.orm.Insert;
import tech.smartboot.feat.cloud.annotation.orm.Mapper;
import tech.smartboot.feat.cloud.annotation.orm.Arg;
import tech.smartboot.feat.cloud.annotation.orm.ConstructorArgs;
import tech.smartboot.feat.cloud.annotation.orm.MapKey;
import tech.smartboot.feat.cloud.annotation.orm.Many;
import tech.smartboot.feat.cloud.annotation.orm.Param;
import tech.smartboot.feat.cloud.annotation.orm.Options;
import tech.smartboot.feat.cloud.annotation.orm.ResultSetType;
import tech.smartboot.feat.cloud.annotation.orm.StatementType;
import tech.smartboot.feat.cloud.annotation.orm.Result;
import tech.smartboot.feat.cloud.annotation.orm.Results;
import tech.smartboot.feat.cloud.annotation.orm.SelectKey;
import tech.smartboot.feat.cloud.annotation.orm.Select;
import tech.smartboot.feat.cloud.annotation.orm.Update;
import tech.smartboot.feat.cloud.annotation.orm.One;
import tech.smartboot.feat.demo.mybatis.entity.Address;
import tech.smartboot.feat.demo.mybatis.entity.User;
import tech.smartboot.feat.demo.mybatis.entity.Post;
import tech.smartboot.feat.demo.mybatis.handler.UpperStringTypeHandler;
import tech.smartboot.feat.demo.mybatis.entity.Article;

    import java.util.List;
    import java.util.Map;

    @Mapper
 public interface FeatMapper {

     @Select("SELECT * FROM user_info WHERE username = #{username}")
     @Results({
             @Result(property = "username", column = "username"),
             @Result(property = "password", column = "password"),
             @Result(property = "desc", column = "desc"),
             @Result(property = "role", column = "role"),
             @Result(property = "createTime", column = "create_time"),
             @Result(property = "editTime", column = "edit_time")
     })
     User selectByUsername(@Param("username") String username);

    @Select("SELECT * FROM user_info")
    @Results(id = "userMap", value = {
            @Result(property = "username", column = "username"),
            @Result(property = "password", column = "password"),
            @Result(property = "desc", column = "desc"),
            @Result(property = "role", column = "role"),
            @Result(property = "createTime", column = "create_time"),
            @Result(property = "editTime", column = "edit_time")
    })
    List<User> selectAll();

    @Select(value = "SELECT * FROM user_info WHERE username = #{username}", resultMap = "userMap")
    User selectByUsernameResultMap(@Param("username") String username);

    @Select(value = "SELECT * FROM user_info", resultMap = "userMap")
    @Options(fetchSize = 100)
    List<User> selectAllWithFetchSize();

    @Select(value = "SELECT * FROM user_info", resultMap = "userMap")
    @Options(resultSetType = ResultSetType.SCROLL_INSENSITIVE)
    List<User> selectAllScrollInsensitive();

    @Select(value = "SELECT * FROM user_info", resultMap = "userMap")
    @Options(statementType = StatementType.STATEMENT)
    List<User> selectAllStatement();

    @Select(value = "SELECT * FROM user_info", resultMap = "userMap")
    @Options(statementType = StatementType.CALLABLE)
    List<User> selectAllCallable();

    @Select(value = "SELECT role FROM user_info WHERE username = #{username}", resultType = String.class)
    String selectRoleByUsername(@Param("username") String username);

    @Select(value = "SELECT username FROM user_info", resultType = String.class)
    List<String> selectAllUsernames();

    @Options(useGeneratedKeys = false)
    @Insert("INSERT INTO user_info(username, password, `desc`, role) VALUES(#{username}, #{password}, #{desc}, #{role})")
    int insertWithOptions(User user);

    @Insert("INSERT INTO user_info(username, password, `desc`, role) VALUES(#{username}, #{password}, #{desc}, #{role})")
    int insert(User user);

     @Update("UPDATE user_info SET password=#{password}, `desc`=#{desc}, role=#{role} WHERE username=#{username}")
     int update(User user);

     @Delete("DELETE FROM user_info WHERE username = #{username}")
     int deleteByUsername(@Param("username") String username);

     @Select("SELECT * FROM user_info WHERE role = #{role}")
     @Results({
             @Result(property = "username", column = "username"),
             @Result(property = "password", column = "password"),
             @Result(property = "desc", column = "desc"),
             @Result(property = "role", column = "role"),
             @Result(property = "createTime", column = "create_time"),
             @Result(property = "editTime", column = "edit_time")
     })
     List<User> selectByRole(@Param("role") String role);

    @Select(value = "<script>SELECT * FROM user_info <where><if test=\"role != null\"> role = #{role}</if><if test=\"username != null\"> AND username = #{username}</if></where></script>", resultMap = "userMap")
    List<User> selectDynamic(@Param("role") String role, @Param("username") String username);

    @Update("<script>UPDATE user_info <set><if test=\"user.password != null\"> password = #{user.password},</if><if test=\"user.desc != null\"> `desc` = #{user.desc},</if><if test=\"user.role != null\"> role = #{user.role},</if></set> WHERE username = #{user.username}</script>")
    int updateDynamic(@Param("user") User user);

    @Delete("<script>DELETE FROM user_info WHERE username IN <foreach collection=\"names\" item=\"name\" open=\"(\" separator=\",\" close=\")\">#{name}</foreach></script>")
    int deleteDynamic(@Param("names") List<String> names);

    @Insert("<script>INSERT INTO user_info(username, password, `desc`, role) VALUES <foreach collection=\"users\" item=\"user\" separator=\",\">(#{user.username}, #{user.password}, #{user.desc}, #{user.role})</foreach></script>")
    int batchInsert(@Param("users") List<User> users);

    @Select(value = "<script>SELECT * FROM user_info <trim prefix=\"WHERE\" prefixOverrides=\"AND |OR \"><if test=\"role != null\"> AND role = #{role}</if><if test=\"username != null\"> AND username = #{username}</if></trim></script>", resultMap = "userMap")
    List<User> selectByTrim(@Param("role") String role, @Param("username") String username);

    @Select(value = "<script>SELECT * FROM user_info WHERE username IN <foreach collection=\"names\" item=\"name\" open=\"(\" separator=\",\" close=\")\">#{name}</foreach></script>", resultMap = "userMap")
    List<User> selectByNames(@Param("names") String[] names);

    @Delete("<script>DELETE FROM user_info WHERE username IN <foreach collection=\"names\" item=\"name\" open=\"(\" separator=\",\" close=\")\">#{name}</foreach></script>")
    int deleteByNames(@Param("names") String[] names);

   @Select(value = "<script><bind name=\"pattern\" value=\"'%' + username + '%'\"/>SELECT * FROM user_info WHERE username LIKE #{pattern}</script>", resultMap = "userMap")
   List<User> selectLikeUsername(@Param("username") String username);

   @Select(value = "<script>SELECT * FROM user_info ORDER BY ${column}</script>", resultMap = "userMap")
   List<User> selectOrderBy(@Param("column") String column);

    @Select("SELECT username AS username, role AS role FROM user_info WHERE username = #{username}")
    Map<String, Object> selectByUsernameMap(@Param("username") String username);

    @Select("SELECT username AS username, role AS role FROM user_info")
    List<Map<String, Object>> selectAllMaps();

   @Select(value = "<script>SELECT username AS username, role AS role FROM user_info <where><if test=\"role != null\"> role = #{role}</if></where></script>")
   List<Map<String, Object>> selectDynamicMaps(@Param("role") String role);

   @Select(value = "SELECT * FROM ${tableName} WHERE username = #{username}", resultMap = "userMap")
   User selectFromTable(@Param("tableName") String tableName, @Param("username") String username);

    @Select("SELECT * FROM user_info WHERE role = #{role}")
    @MapKey("username")
    Map<String, User> selectByRoleAsMap(@Param("role") String role);

   @Select(value = "<script>SELECT * FROM user_info <where><if test=\"role != null\"> role = #{role}</if></where></script>", resultMap = "userMap")
   @MapKey("username")
   Map<String, User> selectDynamicAsMap(@Param("role") String role);

    @SelectKey(statement = "SELECT NEXT VALUE FOR post_seq", keyProperty = "post.id", before = true, resultType = Long.class)
    @Insert("INSERT INTO post(id, username, title) VALUES(#{post.id}, #{post.username}, #{post.title})")
    int insertPost(@Param("post") Post post);

    @Select("SELECT * FROM post WHERE id = #{id}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "title", column = "title")
    })
    Post selectPost(@Param("id") Long id);

    @Select("SELECT username, password, `desc`, role FROM user_info WHERE username = #{username}")
    @ConstructorArgs({
            @Arg(column = "username", javaType = String.class),
            @Arg(column = "password", javaType = String.class),
            @Arg(column = "desc", javaType = String.class),
            @Arg(column = "role", javaType = String.class)
    })
    User selectByUsernameConstructor(@Param("username") String username);

    @Select("SELECT username, role FROM user_info WHERE username = #{username}")
    @Results({
            @Result(property = "username", column = "username"),
            @Result(property = "role", column = "role", typeHandler = UpperStringTypeHandler.class)
    })
    User selectByUsernameWithUpperRole(@Param("username") String username);

    @Select("SELECT username FROM user_info WHERE username = #{username}")
    @ConstructorArgs({
            @Arg(column = "username", javaType = String.class),
            @Arg(column = "username", select = "tech.smartboot.feat.demo.mybatis.mapper.PostMapper.selectPostsByUsername")
    })
    User selectByUsernameWithPostsConstructor(@Param("username") String username);

    @Select(value = "<sql id=\"userColumns\">username, password, `desc`, role</sql><script>SELECT <include refid=\"userColumns\"/> FROM user_info WHERE username = #{username}</script>", resultMap = "userMap")
    User selectByUsernameInclude(@Param("username") String username);

    @Select(value = "<script>SELECT * FROM user_info <where><if test=\"role != null\">role = #{role}</if><if test=\"username != null\"> AND username = #{username}</if></where></script>", resultMap = "userMap")
    List<User> selectByRoleAndUsername(@Param("role") String role, @Param("username") String username);

    @Insert("INSERT INTO article(title, content) VALUES(#{title}, #{content})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertArticle(Article article);

    @Select(value = "<script>SELECT * FROM user_info <where>" +
            "<if test=\"username != null and username.length() > 0\"> AND username = #{username}</if>" +
            "</where></script>", resultMap = "userMap")
    List<User> selectByUsernameIfLength(@Param("username") String username);

    @Select(value = "<script>SELECT * FROM user_info <where>" +
            "<if test=\"user != null and user.username.equals('admin')\"> AND username = #{user.username}</if>" +
            "</where></script>", resultMap = "userMap")
    List<User> selectByAdminUser(@Param("user") User user);

    @Update("<script>UPDATE user_info <set><if test=\"password != null\">password = #{password},</if><if test=\"desc != null\">`desc` = #{desc},</if></set> WHERE username = #{username}</script>")
    int updateMulti(@Param("username") String username, @Param("password") String password, @Param("desc") String desc);

    @Select("SELECT * FROM user_info WHERE username = #{username}")
    @Results({
            @Result(property = "username", column = "username"),
            @Result(property = "password", column = "password"),
            @Result(property = "desc", column = "desc"),
            @Result(property = "role", column = "role"),
            @Result(property = "posts", column = "username", many = @Many(select = "tech.smartboot.feat.demo.mybatis.mapper.PostMapper.selectPostsByUsername"))
    })
    User selectUserWithPosts(@Param("username") String username);

    @Select("SELECT * FROM address WHERE username = #{username}")
    @Results({
            @Result(property = "username", column = "username"),
            @Result(property = "city", column = "city")
    })
    Address selectAddressByUsername(@Param("username") String username);

    @Select("SELECT * FROM user_info WHERE username = #{username}")
    @Results({
            @Result(property = "username", column = "username"),
            @Result(property = "password", column = "password"),
            @Result(property = "desc", column = "desc"),
            @Result(property = "role", column = "role"),
            @Result(property = "createTime", column = "create_time"),
            @Result(property = "editTime", column = "edit_time"),
            @Result(property = "address", column = "username", one = @One(select = "selectAddressByUsername"))
    })
    User selectUserWithAddress(@Param("username") String username);
}

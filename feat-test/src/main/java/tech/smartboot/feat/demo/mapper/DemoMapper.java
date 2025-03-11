/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.demo.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author 三刀(zhengjunweimail @ 163.com)
 * @version v1.0.0
 */
@Mapper
public interface DemoMapper {
    void test3();

    @Select("SELECT * FROM `user` WHERE username = #{username}")
    User selectById(String username);

    String testA(String s);

    List<String> testC(int s);
}

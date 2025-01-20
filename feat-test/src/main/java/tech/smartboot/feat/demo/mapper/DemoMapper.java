package tech.smartboot.feat.demo.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DemoMapper {
    void test3();

    String test();

    String testA(String s);

    List<String> testC(int s);
}

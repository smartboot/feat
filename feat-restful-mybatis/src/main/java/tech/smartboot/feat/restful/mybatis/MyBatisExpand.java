package tech.smartboot.feat.restful.mybatis;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import tech.smartboot.feat.restful.context.ApplicationContext;
import tech.smartboot.feat.restful.context.Expand;

import java.lang.reflect.Proxy;
import java.util.List;

public class MyBatisExpand implements Expand<Mapper> {

    @Override
    public void initializeBean(ApplicationContext context, List<Class<?>> mappers) throws Exception {
        SqlSessionFactory factory = context.getBean("sessionFactory");
        for (Class<?> mapperClass : mappers) {
            context.addBean(mapperClass.getName(), Proxy.newProxyInstance(mapperClass.getClassLoader(), new Class[]{mapperClass}, (proxy, method, args) -> {
                try (SqlSession session = factory.openSession(true)) {
                    return method.invoke(session.getMapper(mapperClass), args);
                }
            }));
        }
    }

    @Override
    public Class<Mapper> expandAnnotation() {
        return Mapper.class;
    }
}

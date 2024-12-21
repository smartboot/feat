package tech.smartboot.feat.restful.context;

import java.lang.annotation.Annotation;
import java.util.List;

public interface Expand<T extends Annotation> {

    void initializeBean(ApplicationContext context, List<Class<?>> clazz) throws Exception;

    Class<T> expandAnnotation();
}

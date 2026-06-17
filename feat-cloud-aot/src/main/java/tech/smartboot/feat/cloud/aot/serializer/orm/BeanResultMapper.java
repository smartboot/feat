 package tech.smartboot.feat.cloud.aot.serializer.orm;
 
 import tech.smartboot.feat.cloud.annotation.orm.Result;
 import tech.smartboot.feat.cloud.annotation.orm.Results;
 
 import javax.lang.model.element.Element;
 import javax.lang.model.element.ElementKind;
 import javax.lang.model.element.ExecutableElement;
 import javax.lang.model.element.Modifier;
 import javax.lang.model.type.DeclaredType;
 import javax.lang.model.type.TypeMirror;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 /**
  * 结果集与 Java Bean 的映射。
  */
 public final class BeanResultMapper {
 
     private BeanResultMapper() {
     }
 
     public static List<Element> extractFields(TypeMirror type) {
         List<Element> fields = new ArrayList<>();
         if (!(type instanceof DeclaredType)) {
             return fields;
         }
         for (Element e : ((DeclaredType) type).asElement().getEnclosedElements()) {
             if (e.getKind() != ElementKind.FIELD) {
                 continue;
             }
             Set<Modifier> modifiers = e.getModifiers();
             if (modifiers.contains(Modifier.STATIC) || modifiers.contains(Modifier.TRANSIENT)) {
                 continue;
             }
             if (!modifiers.contains(Modifier.PRIVATE)) {
                 continue;
             }
             fields.add(e);
         }
         return fields;
     }
 
    public static Map<String, Result> buildResultMap(ExecutableElement method) {
        Map<String, Result> map = new HashMap<>();
        Results results = method.getAnnotation(Results.class);
        if (results != null) {
            for (Result result : results.value()) {
                map.put(result.property(), result);
            }
        }
        return map;
    }

    /**
     * 将指定的 {@link Results} 转换为 property -> {@link Result} 索引。
     */
    public static Map<String, Result> buildResultMap(Results results) {
        return ResultMapRegistry.toResultMap(results);
    }
}

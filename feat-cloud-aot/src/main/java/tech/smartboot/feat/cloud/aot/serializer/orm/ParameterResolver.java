 package tech.smartboot.feat.cloud.aot.serializer.orm;
 
 import tech.smartboot.feat.cloud.annotation.orm.Param;
 
 import javax.lang.model.element.ExecutableElement;
 import javax.lang.model.element.VariableElement;
 import java.util.ArrayList;
 import java.util.List;
 
 /**
  * 解析 #{...} 占位符到 Java 表达式。
  */
 public final class ParameterResolver {
 
     private ParameterResolver() {
     }
 
     public static List<ParameterMetadata> resolve(ExecutableElement method) {
         List<? extends VariableElement> params = method.getParameters();
         List<ParameterMetadata> infos = new ArrayList<>(params.size());
         for (int i = 0; i < params.size(); i++) {
             VariableElement param = params.get(i);
             Param paramAnno = param.getAnnotation(Param.class);
             String alias = paramAnno == null ? null : paramAnno.value();
             infos.add(new ParameterMetadata(param.getSimpleName().toString(), alias, param.asType(), i + 1));
         }
         return infos;
     }
 
     public static String toExpression(String placeholder, List<ParameterMetadata> paramInfos) {
         String[] parts = placeholder.split("\\.");
         String first = parts[0];
         ParameterMetadata param = findParameter(first, paramInfos);
         if (param != null) {
             StringBuilder sb = new StringBuilder(param.getVarName());
             for (int i = 1; i < parts.length; i++) {
                 sb.append(".get").append(capitalize(parts[i])).append("()");
             }
             return sb.toString();
         }
         if (paramInfos.size() == 1) {
             StringBuilder sb = new StringBuilder(paramInfos.get(0).getVarName());
             for (String part : parts) {
                 sb.append(".get").append(capitalize(part)).append("()");
             }
             return sb.toString();
         }
         throw new RuntimeException("无法解析SQL参数: #{" + placeholder + "}");
     }
 
     public static ParameterMetadata findParameter(String name, List<ParameterMetadata> paramInfos) {
         for (ParameterMetadata info : paramInfos) {
             if (name.equals(info.getAlias()) || name.equals(info.getVarName()) || name.equals("param" + info.getIndex())) {
                 return info;
             }
         }
         return null;
     }
 
     private static String capitalize(String value) {
         if (value == null || value.isEmpty()) {
             return value;
         }
         return Character.toUpperCase(value.charAt(0)) + value.substring(1);
     }
 }

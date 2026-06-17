 package tech.smartboot.feat.cloud.aot.serializer.orm;
 
 import javax.lang.model.type.TypeMirror;
 
 /**
  * 方法参数元数据。
  */
 public final class ParameterMetadata {
     private final String varName;
     private final String alias;
     private final TypeMirror type;
     private final int index;
 
     public ParameterMetadata(String varName, String alias, TypeMirror type, int index) {
         this.varName = varName;
         this.alias = alias;
         this.type = type;
         this.index = index;
     }
 
     public String getVarName() {
         return varName;
     }
 
     public String getAlias() {
         return alias;
     }
 
     public TypeMirror getType() {
         return type;
     }
 
     public int getIndex() {
         return index;
     }
 }

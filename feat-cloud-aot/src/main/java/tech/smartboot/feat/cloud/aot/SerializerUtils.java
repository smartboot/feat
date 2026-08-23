package tech.smartboot.feat.cloud.aot;

public class SerializerUtils {
    private static String toString(String str) {
        return "\"" + str.replace("\\", "\\\\").replace("\n", "\\n").replace("\"", "\\\"") + "\"";
    }


    public static String resolveIntProperty(Object objVal) {
        if (objVal == null) {
            throw new NullPointerException();
        }
        if (objVal instanceof Integer) {
            return objVal.toString();
        }
        if (!(objVal instanceof String)) {
            return null;
        }
        String val = objVal.toString();
        if (!(val.startsWith("${") && val.endsWith("}"))) {
            return String.valueOf(Integer.parseInt(val));
        }
        int spit = val.indexOf(":");
        String name = val.substring(2, spit > 0 ? spit : val.length() - 1);
        int defaultValue = spit > 0 ? Integer.parseInt(val.substring(spit + 1, val.length() - 1)) : 0;
        return "resolveIntProperty(\"" + name + "\" ,\"" + name.replace('.', '_').toUpperCase() + "\"," + defaultValue + ")";
    }

    public static String resolveBoolProperty(Object objVal) {
        if (objVal instanceof Boolean) {
            return objVal.toString();
        }
        if (!(objVal instanceof String)) {
            return null;
        }
        String val = objVal.toString();
        if (!(val.startsWith("${") && val.endsWith("}"))) {
            return String.valueOf(Boolean.parseBoolean(val));
        }
        int spit = val.indexOf(":");
        String name = val.substring(2, spit > 0 ? spit : val.length() - 1);
        boolean defaultValue = spit > 0 && Boolean.parseBoolean(val.substring(spit + 1, val.length() - 1));
        return "resolveBoolProperty(\"" + name + "\" ,\"" + name.replace('.', '_').toUpperCase() + "\"," + defaultValue + ")";
    }

    public static String resolveStringProperty(Object objVal) {
        if (objVal == null) {
            return null;
        }
        String val = objVal.toString();
        if (!(val.startsWith("${") && val.endsWith("}"))) {
            return toString(val);
        }
        int spit = val.indexOf(":");
        String name = val.substring(2, spit > 0 ? spit : val.length() - 1);
        String defaultValue = spit > 0 ? val.substring(spit + 1, val.length() - 1) : null;
        return "resolveProperty(\"" + name + "\" ,\"" + name.replace('.', '_').toUpperCase() + "\"," + (defaultValue == null ? "null" : toString(defaultValue)) + ")";
    }
}

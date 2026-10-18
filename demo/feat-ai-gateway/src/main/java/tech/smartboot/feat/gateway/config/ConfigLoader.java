package tech.smartboot.feat.gateway.config;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * YAML -> 快照。所有校验都在这里做完：校验不通过就抛异常，旧快照继续服务。
 * <p>
 * 设计约束：这里只产出 GatewayConfig，不做任何 IO 副作用（不建连接、不写文件）。
 */
public final class ConfigLoader {
    private static final Pattern ENV_PATTERN = Pattern.compile("\\$\\{([A-Za-z0-9_]+)}");
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private ConfigLoader() {
    }

    public static GatewayConfig loadFile(Path path) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            return load(new String(bytes, UTF8));
        } catch (IOException e) {
            throw new ConfigException("读取配置文件失败: " + path + " -> " + e.getMessage(), e);
        }
    }

    public static GatewayConfig load(String yamlText) {
        Map<String, Object> root;
        try {
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            root = asMap(yaml.load(new InputStreamReader(new ByteArrayInputStream(yamlText.getBytes(UTF8)), UTF8)));
        } catch (ConfigException e) {
            throw e;
        } catch (Exception e) {
            throw new ConfigException("YAML 解析失败: " + e.getMessage(), e);
        }
        if (root == null) {
            throw new ConfigException("配置文件为空");
        }

        List<String> errors = new ArrayList<String>();

        int version = intVal(root.get("version"), 1);
        if (version <= 0) {
            errors.add("version 必须是正整数");
        }

        Map<String, Object> server = asMap(root.get("server"));
        int port = intVal(server.get("port"), 8080);
        if (port <= 0 || port > 65535) {
            errors.add("server.port 非法: " + port);
        }
        long idleTimeoutMs = longVal(server.get("idleTimeoutMs"), 300000L);
        if (idleTimeoutMs < 0) {
            errors.add("server.idleTimeoutMs 不能为负数（0 表示不限制）");
        }

        Map<String, Object> admin = asMap(root.get("admin"));
        String adminToken = resolve(str(admin.get("token"), ""), errors, "admin.token");

        Map<String, Object> log = asMap(root.get("log"));
        String logDir = str(log.get("dir"), "logs");
        boolean usageEnabled = boolVal(log.get("usageEnabled"), true);

        // ---------- providers ----------
        Map<String, Provider> providers = new LinkedHashMap<String, Provider>();
        List<Object> providerList = asList(root.get("providers"));
        if (providerList.isEmpty()) {
            errors.add("providers 不能为空");
        }
        for (int i = 0; i < providerList.size(); i++) {
            String at = "providers[" + i + "]";
            Map<String, Object> node = tryMap(providerList.get(i), errors, at);
            if (node == null) {
                continue;
            }
            String name = str(node.get("name"), "");
            String baseUrl = str(node.get("baseUrl"), "");
            String apiKey = resolve(str(node.get("apiKey"), ""), errors, at + ".apiKey");
            if (name.isEmpty()) {
                errors.add(at + ".name 不能为空");
                continue;
            }
            if (providers.containsKey(name)) {
                errors.add(at + ".name 重复: " + name);
                continue;
            }
            if (baseUrl.isEmpty() || !(baseUrl.startsWith("http://") || baseUrl.startsWith("https://"))) {
                errors.add(at + ".baseUrl 必须是 http(s) 地址: " + baseUrl);
                continue;
            }
            int maxConnections = intVal(node.get("maxConnections"), 1000);
            if (maxConnections <= 0) {
                errors.add(at + ".maxConnections 必须大于 0");
            }
            int connectTimeoutMs = intVal(node.get("connectTimeoutMs"), 3000);
            int providerIdleTimeoutMs = intVal(node.get("idleTimeoutMs"), 300000);
            String authHeader = str(node.get("authHeader"), "Authorization");
            String authPrefix = str(node.get("authPrefix"), "Bearer ");
            providers.put(name, new Provider(name, baseUrl, apiKey, maxConnections, connectTimeoutMs,
                    providerIdleTimeoutMs, authHeader, authPrefix));
        }

        // ---------- models ----------
        Map<String, List<ModelRoute>> routeMap = new HashMap<String, List<ModelRoute>>();
        List<Object> modelList = asList(root.get("models"));
        if (modelList.isEmpty()) {
            errors.add("models 不能为空");
        }
        for (int i = 0; i < modelList.size(); i++) {
            String at = "models[" + i + "]";
            Map<String, Object> node = tryMap(modelList.get(i), errors, at);
            if (node == null) {
                continue;
            }
            String name = str(node.get("name"), "");
            String provider = str(node.get("provider"), "");
            String upstream = str(node.get("upstream"), name);
            int priority = intVal(node.get("priority"), 0);
            if (name.isEmpty()) {
                errors.add(at + ".name 不能为空");
                continue;
            }
            if (provider.isEmpty()) {
                errors.add(at + ".provider 不能为空（model=" + name + "）");
                continue;
            }
            if (!providers.containsKey(provider)) {
                errors.add(at + ".provider 未定义: " + provider + "（model=" + name + "）");
                continue;
            }
            List<ModelRoute> list = routeMap.get(name);
            if (list == null) {
                list = new ArrayList<ModelRoute>();
                routeMap.put(name, list);
            }
            list.add(new ModelRoute(name, provider, upstream, priority));
        }
        // 同一模型多条路由按 priority 升序，稳定排序保证同优先级按文件顺序
        for (Map.Entry<String, List<ModelRoute>> entry : routeMap.entrySet()) {
            final List<ModelRoute> original = entry.getValue();
            List<ModelRoute> sorted = new ArrayList<ModelRoute>(original);
            Collections.sort(sorted, new Comparator<ModelRoute>() {
                @Override
                public int compare(ModelRoute a, ModelRoute b) {
                    int c = Integer.compare(a.priority(), b.priority());
                    return c != 0 ? c : Integer.compare(original.indexOf(a), original.indexOf(b));
                }
            });
            entry.setValue(Collections.unmodifiableList(sorted));
        }

        // ---------- keys ----------
        List<KeyPolicy> keys = new ArrayList<KeyPolicy>();
        List<Object> keyList = asList(root.get("keys"));
        Set<String> keyIds = new HashSet<String>();
        if (keyList.isEmpty()) {
            errors.add("keys 不能为空（网关默认拒绝所有未授权请求）");
        }
        for (int i = 0; i < keyList.size(); i++) {
            String at = "keys[" + i + "]";
            Map<String, Object> node = tryMap(keyList.get(i), errors, at);
            if (node == null) {
                continue;
            }
            String id = str(node.get("id"), "");
            String keyHash = KeyHash.normalize(str(node.get("keyHash"), ""));
            if (id.isEmpty()) {
                errors.add(at + ".id 不能为空");
                continue;
            }
            if (!keyIds.add(id)) {
                errors.add(at + ".id 重复: " + id);
                continue;
            }
            if (keyHash.isEmpty()) {
                errors.add(at + ".keyHash 不能为空（用 `java -jar feat-ai-gateway.jar hash <token>` 生成）");
                continue;
            }
            if (!keyHash.matches("[0-9a-f]{8,64}")) {
                errors.add(at + ".keyHash 必须是 8-64 位十六进制");
                continue;
            }
            Set<String> models = new HashSet<String>();
            List<Object> modelNames = asList(node.get("models"));
            for (Object m : modelNames) {
                String modelName = String.valueOf(m);
                if (!routeMap.containsKey(modelName)) {
                    errors.add(at + ".models 中的模型未在 models 段定义: " + modelName);
                }
                models.add(modelName);
            }
            int rpm = intVal(node.get("rpm"), 0);
            long tpm = longVal(node.get("tpm"), 0L);
            long monthlyTokens = longVal(node.get("monthlyTokens"), 0L);
            if (rpm < 0 || tpm < 0 || monthlyTokens < 0) {
                errors.add(at + " 的 rpm/tpm/monthlyTokens 不能为负数");
            }
            keys.add(new KeyPolicy(id, keyHash, models, rpm, tpm, monthlyTokens));
        }

        if (!errors.isEmpty()) {
            throw new ConfigException("配置校验失败（" + errors.size() + " 项）：\n  - " + join(errors, "\n  - "));
        }

        return new GatewayConfig(version, port, idleTimeoutMs, adminToken, logDir, usageEnabled,
                providers, routeMap, keys);
    }

    // ------------------------------------------------------------------

    private static String join(List<String> list, String sep) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(sep);
            }
            sb.append(list.get(i));
        }
        return sb.toString();
    }

    /**
     * ${ENV} 占位符必须能在环境变量里解析到，否则视为配置错误——静默留空会导致线上 401 排查半天。
     */
    private static String resolve(String value, List<String> errors, String at) {
        if (value == null || value.indexOf("${") < 0) {
            return value;
        }
        Matcher matcher = ENV_PATTERN.matcher(value);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String env = matcher.group(1);
            String v = System.getenv(env);
            if (v == null) {
                errors.add(at + " 引用的环境变量未设置: " + env);
                v = "";
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(v));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        if (o == null) {
            return new LinkedHashMap<String, Object>();
        }
        if (!(o instanceof Map)) {
            throw new ConfigException("期望 map 结构，实际是: " + o.getClass().getSimpleName());
        }
        return (Map<String, Object>) o;
    }

    private static Map<String, Object> tryMap(Object o, List<String> errors, String at) {
        if (!(o instanceof Map)) {
            errors.add(at + " 必须是 map 结构");
            return null;
        }
        return asMap(o);
    }

    @SuppressWarnings("unchecked")
    private static List<Object> asList(Object o) {
        if (o == null) {
            return new ArrayList<Object>();
        }
        if (!(o instanceof List)) {
            return new ArrayList<Object>();
        }
        return (List<Object>) o;
    }

    private static String str(Object o, String def) {
        if (o == null) {
            return def;
        }
        return String.valueOf(o).trim();
    }

    private static int intVal(Object o, int def) {
        if (o == null) {
            return def;
        }
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static long longVal(Object o, long def) {
        if (o == null) {
            return def;
        }
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static boolean boolVal(Object o, boolean def) {
        if (o == null) {
            return def;
        }
        if (o instanceof Boolean) {
            return (Boolean) o;
        }
        return Boolean.parseBoolean(String.valueOf(o).trim());
    }

    static Reader readerOf(String text) {
        return new InputStreamReader(new ByteArrayInputStream(text.getBytes(UTF8)), UTF8);
    }
}

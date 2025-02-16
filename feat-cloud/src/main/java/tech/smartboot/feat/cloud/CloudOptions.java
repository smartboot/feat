package tech.smartboot.feat.cloud;

import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.server.ServerOptions;

import java.util.HashMap;
import java.util.Map;

public class CloudOptions extends ServerOptions {
    private String[] packages;
    private final Map<String, Object> externalBeans = new HashMap<>();

    public String[] getPackages() {
        return packages;
    }

    public CloudOptions setPackages(String... packages) {
        this.packages = packages;
        return this;
    }

    public Map<String, Object> getExternalBeans() {
        return externalBeans;
    }

    public CloudOptions addExternalBean(String key, Object value) {
        if (externalBeans.containsKey(key)) {
            throw new FeatException("bean " + key + " already exists");
        }
        externalBeans.put(key, value);
        return this;
    }
}

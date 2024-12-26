package tech.smartboot.feat.core.server.waf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WafConfiguration {
    private boolean enable = false;
    public static final String DESC = "Mysterious Power from the East Is Protecting This Area.";
    private final Set<String> allowMethods = new HashSet<>();
    private Set<String> denyMethods = new HashSet<>();

    private List<String> allowUriPrefixes = new ArrayList<>();

    private List<String> allowUriSuffixes = new ArrayList<>();

    public Set<String> getAllowMethods() {
        return allowMethods;
    }

    public WafConfiguration addAllowMethod(String method) {
        enable = true;
        allowMethods.add(method);
        return this;
    }

    public WafConfiguration addAllowMethods(Set<String> allowMethods) {
        enable = true;
        this.allowMethods.addAll(allowMethods);
        return this;
    }

    public Set<String> getDenyMethods() {
        return denyMethods;
    }

    public void setDenyMethods(Set<String> denyMethods) {
        enable = true;
        this.denyMethods = denyMethods;
    }

    public List<String> getAllowUriPrefixes() {
        return allowUriPrefixes;
    }

    public WafConfiguration addAllowUriPrefix(String prefix) {
        enable = true;
        allowUriPrefixes.add(prefix);
        return this;
    }

    public void setAllowUriPrefixes(List<String> allowUriPrefixes) {
        enable = true;
        this.allowUriPrefixes = allowUriPrefixes;
    }

    public List<String> getAllowUriSuffixes() {
        return allowUriSuffixes;
    }

    public void setAllowUriSuffixes(List<String> allowUriSuffixes) {
        enable = true;
        this.allowUriSuffixes = allowUriSuffixes;
    }

    boolean isEnable() {
        return enable;
    }
}

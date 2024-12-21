package tech.smartboot.feat.server.waf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WafConfiguration {
    public static final String DESC = "Mysterious Power from the East Is Protecting This Area.";
    private Set<String> allowMethods = new HashSet<>();
    private Set<String> denyMethods = new HashSet<>();

    private List<String> allowUriPrefixes = new ArrayList<>();

    private List<String> allowUriSuffixes = new ArrayList<>();

    public Set<String> getAllowMethods() {
        return allowMethods;
    }

    public void setAllowMethods(Set<String> allowMethods) {
        this.allowMethods = allowMethods;
    }

    public Set<String> getDenyMethods() {
        return denyMethods;
    }

    public void setDenyMethods(Set<String> denyMethods) {
        this.denyMethods = denyMethods;
    }

    public List<String> getAllowUriPrefixes() {
        return allowUriPrefixes;
    }

    public void setAllowUriPrefixes(List<String> allowUriPrefixes) {
        this.allowUriPrefixes = allowUriPrefixes;
    }

    public List<String> getAllowUriSuffixes() {
        return allowUriSuffixes;
    }

    public void setAllowUriSuffixes(List<String> allowUriSuffixes) {
        this.allowUriSuffixes = allowUriSuffixes;
    }
}

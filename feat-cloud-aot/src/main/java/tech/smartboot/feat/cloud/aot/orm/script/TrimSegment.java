package tech.smartboot.feat.cloud.aot.orm.script;

public class TrimSegment extends ContainerSegment {

    private final String prefix;

    private final String suffix;

    private final String prefixOverrides;

    private final String suffixOverrides;

    public TrimSegment(
            String prefix,
            String suffix,
            String prefixOverrides,
            String suffixOverrides) {

        this.prefix = prefix;
        this.suffix = suffix;
        this.prefixOverrides = prefixOverrides;
        this.suffixOverrides = suffixOverrides;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public String getPrefixOverrides() {
        return prefixOverrides;
    }

    public String getSuffixOverrides() {
        return suffixOverrides;
    }
}
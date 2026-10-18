package tech.smartboot.feat.gateway.config;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 一把下发给调用方的 key 的配额策略。
 * <p>
 * 只保存哈希，不保存明文。0 表示不限制。
 */
public final class KeyPolicy {
    private final String id;
    private final String keyHash;
    private final Set<String> models;
    private final int rpm;
    private final long tpm;
    private final long monthlyTokens;

    public KeyPolicy(String id, String keyHash, Set<String> models, int rpm, long tpm, long monthlyTokens) {
        this.id = id;
        this.keyHash = keyHash;
        this.models = models == null || models.isEmpty()
                ? Collections.<String>emptySet()
                : Collections.unmodifiableSet(new HashSet<String>(models));
        this.rpm = rpm;
        this.tpm = tpm;
        this.monthlyTokens = monthlyTokens;
    }

    public String id() {
        return id;
    }

    public String keyHash() {
        return keyHash;
    }

    public Set<String> models() {
        return models;
    }

    public int rpm() {
        return rpm;
    }

    public long tpm() {
        return tpm;
    }

    public long monthlyTokens() {
        return monthlyTokens;
    }

    /**
     * 空集合表示允许调用任何已配置的模型。
     */
    public boolean allowsModel(String model) {
        return models.isEmpty() || models.contains(model);
    }
}

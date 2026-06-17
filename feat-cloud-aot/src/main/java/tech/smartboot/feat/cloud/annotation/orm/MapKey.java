package tech.smartboot.feat.cloud.annotation.orm;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 指定返回 Map 时的键属性。
 *
 * <p>与 MyBatis {@code @MapKey} 行为一致：将返回对象的某个属性作为 Map 的 key。</p>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface MapKey {
    /**
     * 作为 Map key 的属性名。
     */
    String value();
}

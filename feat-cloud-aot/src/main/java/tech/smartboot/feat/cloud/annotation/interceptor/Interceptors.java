
package tech.smartboot.feat.cloud.annotation.interceptor;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

@Target({TYPE, METHOD, CONSTRUCTOR})
@Retention(RetentionPolicy.SOURCE)
public @interface Interceptors {

    /**
     * An ordered list of interceptors.
     *
     * @return an array representing the interceptor classes
     */
    Class[] value();
}

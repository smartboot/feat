
package tech.smartboot.feat.cloud.annotation.interceptor;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

@Retention(RetentionPolicy.SOURCE)
@Target(TYPE)
@Documented
public @interface Interceptor {

    public static class Priority {
        private Priority() {
        } // don't allow instances

        /**
         * Start of range for early interceptors defined by platform specifications.
         */
        public static final int PLATFORM_BEFORE = 0;

        /**
         * Start of range for early interceptors defined by extension libraries.
         */
        public static final int LIBRARY_BEFORE = 1000;

        /**
         * Start of range for interceptors defined by applications.
         */
        public static final int APPLICATION = 2000;

        /**
         * Start of range for late interceptors defined by extension libraries.
         */
        public static final int LIBRARY_AFTER = 3000;

        /**
         * Start of range for late interceptors defined by platform specifications.
         */
        public static final int PLATFORM_AFTER = 4000;
    }
}

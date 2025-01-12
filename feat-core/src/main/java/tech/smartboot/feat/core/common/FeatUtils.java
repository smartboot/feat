package tech.smartboot.feat.core.common;

import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.core.common.utils.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FeatUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(FeatUtils.class);

    public static String getResourceAsString(String fileName) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (InputStream inputStream = StringUtils.class.getClassLoader().getResourceAsStream(fileName);) {
            if (inputStream == null) {
                LOGGER.error("resource {} not found", fileName);
                return null;
            }
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            return outputStream.toString();
        } catch (IOException e) {
            LOGGER.error("read resource {} error", fileName, e);
            return null;
        }
    }
}

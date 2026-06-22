package tech.smartboot.feat.cloud.aot.license;

import com.alibaba.fastjson2.JSONPath;
import org.yaml.snakeyaml.Yaml;
import tech.smartboot.feat.cloud.CloudService;
import tech.smartboot.feat.cloud.aot.serializer.CloudOptionsSerializer;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.exception.FeatException;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

public class LicenseLoader {
    private final PrintStream BLANK = new PrintStream(new OutputStream() {
        @Override
        public void write(int b) {

        }
    });
    private License license;

    public LicenseLoader(String config) {
        InputStream inputStream = CloudOptionsSerializer.class.getClassLoader().getResourceAsStream("feat_users.yaml");
        if (inputStream == null) {
            throw new FeatException("") {
                @Override
                public void printStackTrace() {
                    System.err.println("################# ERROR ##############");
                    System.err.println("ERROR: Compilation environment exception. Please check if the correct version of feat-cloud-starter is depended on.");
                    System.err.println("######################################");
                }
            };
        }
        Yaml yaml = new Yaml();

        FeatLicenseRepository featUsers = yaml.loadAs(inputStream, FeatLicenseRepository.class);
        Object yamlLicense = JSONPath.eval(config, "$.license");
        String localLicense;
        if (yamlLicense == null) {
            localLicense = System.getenv("FEAT_LICENSE");
        } else {
            localLicense = yamlLicense.toString();
        }
        //无license
        if (FeatUtils.isEmpty(localLicense)) {
            return;
        }
        String[] array = FeatUtils.split(localLicense, "_");
        if (array.length != 2) {
            throw new FeatException("") {
                @Override
                public void printStackTrace() {
                    System.err.println("################# ERROR ##############");
                    System.err.println("invalid Feat License: " + localLicense);
                    System.err.println("######################################");
                }
            };
        }


        license = featUsers.getUsers().get(array[0]);
        if (license == null) {
            throw new RuntimeException("license is invalid");
        }
        license.setNum(array[0]);
        try {
            Signature ecdsaVerify = Signature.getInstance("SHA256withECDSA");
            PublicKey publicKey = KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(array[1])));
            ecdsaVerify.initVerify(publicKey);
            ecdsaVerify.update(license.getName().getBytes(StandardCharsets.UTF_8));
            if (!ecdsaVerify.verify(Base64.getDecoder().decode(license.getLicense()))) {
                System.err.println("################# ERROR ##############");
                System.err.println("invalid Feat License: " + localLicense);
                System.err.println("######################################");
                throw new FeatException("Invalid Feat License");
            }
            supplyChainSecurity(featUsers);
        } catch (FeatException e) {
            throw e;
        } catch (Throwable e) {
            e.printStackTrace();
            System.err.println("################# ERROR ##############");
            System.err.println("Feat License Check ERROR: " + e.getMessage());
            System.err.println("######################################");
            throw new FeatException("Feat License Check ERROR", e);
        }
//        license=null;
    }

    public License getLicense() {
        return license;
    }

    private void supplyChainSecurity(FeatLicenseRepository featUsers) throws IllegalAccessException {
//        featUsers.getUsers().clear();
        ServiceLoader<CloudService> serviceLoader = ServiceLoader.load(CloudService.class, CloudService.class.getClassLoader());
        PrintStream out = System.out;
        System.setOut(BLANK);
        Map<String, String> reasons = new HashMap<>();
        for (CloudService cloudService : serviceLoader) {
            System.setOut(out);
            Class<?> clazz = cloudService.getClass();
            Field licenseNumField = null;
            Field licenseNameField = null;
            for (Field field : clazz.getDeclaredFields()) {
                if (field.getName().equals("license_num")) {
                    licenseNumField = field;
                    field.setAccessible(true);
                } else if (field.getName().equals("license_name")) {
                    licenseNameField = field;
                }
            }
            if (licenseNumField == null || licenseNameField == null) {
                throw new FeatException("") {
                    @Override
                    public void printStackTrace() {
                        System.err.println("################# ERROR ##############");
                        System.err.println("Feat License Check ERROR: " + clazz.getName() + " is not a valid cloud service.");
                        System.err.println("######################################");
                    }
                };
            }
            String licenseNum = (String) licenseNumField.get(null);
            if (FeatUtils.isBlank(licenseNum)) {
                System.setOut(BLANK);
                continue;
            }
            String licenseName = (String) licenseNameField.get(null);
            License license = featUsers.getUsers().get(licenseNum);
            if (license == null) {
                reasons.put(clazz.getName(), "Invalid LICENSE");
            } else if (!FeatUtils.equals(licenseName, license.getName())) {
                reasons.put(clazz.getName(), "License holder mismatch");
            }
            System.setOut(BLANK);
        }
        System.setOut(out);
        if (!reasons.isEmpty()) {
            throw new FeatException("") {
                @Override
                public void printStackTrace() {
                    System.err.println("################# ERROR ##############");
                    reasons.forEach((k, v) -> {
                        System.err.println(k + " reason: " + v);
                    });
                    System.err.println("######################################");
                }
            };
        }
    }
}

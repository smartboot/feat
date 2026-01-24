/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.aot.serializer;

import com.alibaba.fastjson2.JSONPath;
import org.smartboot.socket.extension.plugins.SslPlugin;
import org.smartboot.socket.extension.ssl.factory.AutoServerSSLContextFactory;
import org.yaml.snakeyaml.Yaml;
import tech.smartboot.feat.cloud.AbstractCloudService;
import tech.smartboot.feat.cloud.ApplicationContext;
import tech.smartboot.feat.cloud.CloudService;
import tech.smartboot.feat.cloud.aot.FeatLicenseRepository;
import tech.smartboot.feat.cloud.aot.License;
import tech.smartboot.feat.cloud.aot.Serializer;
import tech.smartboot.feat.cloud.aot.serializer.extension.MybatisSerializer;
import tech.smartboot.feat.cloud.aot.serializer.extension.RedisunSerializer;
import tech.smartboot.feat.cloud.session.ClusterSessionManager;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.core.server.ServerOptions;
import tech.smartboot.feat.router.Router;
import tech.smartboot.feat.router.session.LocalSessionManager;
import tech.smartboot.feat.router.session.SessionManager;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 5/27/25
 */
public final class CloudOptionsSerializer implements Serializer {
    private static final Logger logger = LoggerFactory.getLogger(CloudOptionsSerializer.class);
    private final String PACKAGE;
    private final String CLASS_NAME;
    private final String config;
    private final Set<String> availableTypes = new HashSet<>(Arrays.asList(String.class.getName(), int.class.getName()));

    private final PrintWriter printWriter;
    private final List<String> services;
    private License license;
    private String modelName;
    private final boolean redisSession;

    /**
     * 扩展点
     */
    private final List<Serializer> extensions = new ArrayList<>();

    public CloudOptionsSerializer(ProcessingEnvironment processingEnv, String config, List<String> services) throws Throwable {
        this.config = config;
        this.services = services;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String date = sdf.format(new Date());
        PACKAGE = "tech.smartboot.feat.build.v" + date;
        CLASS_NAME = "FeatApplication";

        //清理build目录
        FileObject preFileObject = processingEnv.getFiler().getResource(StandardLocation.SOURCE_OUTPUT, packageName(), className() + ".java");
        File buildDir = new File(preFileObject.toUri()).getParentFile().getParentFile();
        deleteBuildDir(buildDir);
        preFileObject = processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, packageName(), className() + ".class");
        buildDir = new File(preFileObject.toUri()).getParentFile().getParentFile();
        deleteBuildDir(buildDir);

        //清理feat.yaml文件
//        deleteFeatYamlFile(processingEnv);


        File f = new File(processingEnv.getFiler().getResource(StandardLocation.SOURCE_OUTPUT, "", className() + ".java").toUri()).getParentFile();
        if (f != null) {
            f = f.getParentFile();
            if (f.exists()) {
                f = f.getParentFile();
                if (f.exists()) {
                    f = f.getParentFile();
                    if (f != null) {
                        modelName = f.getName();
                    }
                }
            }
        }

        JavaFileObject javaFileObject = processingEnv.getFiler().createSourceFile(packageName() + "." + className());
        Writer writer = javaFileObject.openWriter();
        printWriter = new PrintWriter(writer);

        //license验签
        loadFeatYaml();

        redisSession = "redis".equals(JSONPath.eval(config, "$.server.session['store-type']"));

        if (JSONPath.eval(config, "$.feat.redis") != null) {
            extensions.add(new RedisunSerializer(processingEnv, config, printWriter));
        }

        if (JSONPath.eval(config, "$.feat.mybatis") != null) {
            extensions.add(new MybatisSerializer(processingEnv, config, printWriter));
        }
    }

//    private static void deleteFeatYamlFile(ProcessingEnvironment processingEnv) throws IOException {
//        File buildDir;
//        buildDir = new File(processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", "feat.yml").toUri());
//        if (buildDir.exists()) {
//            buildDir.delete();
//        }
//        buildDir = new File(processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", "feat.yaml").toUri());
//        if (buildDir.exists()) {
//            buildDir.delete();
//        }
//    }

    public static void main(String[] args) throws Exception {
        // 1. 生成密钥对（同上）
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        keyGen.initialize(256);
        KeyPair keyPair = keyGen.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        //公钥转成字符串
        String publicKeyString = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
//        System.out.println("Public Key: " + publicKeyString);

        // 2. 要签名的数据
        String data = "smartboot开源组织";
        byte[] dataBytes = data.getBytes();

        // 3. 签名
        Signature ecdsaSign = Signature.getInstance("SHA256withECDSA");
        ecdsaSign.initSign(privateKey);
        ecdsaSign.update(dataBytes);
        byte[] signature = ecdsaSign.sign();

//        System.out.println("Signature: " + Base64.getEncoder().encodeToString(signature));

        // 4. 验签
        Signature ecdsaVerify = Signature.getInstance("SHA256withECDSA");
        // 公钥初始化publicKeyString
        PublicKey publicKey = KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyString)));
        ecdsaVerify.initVerify(publicKey);
        ecdsaVerify.update(dataBytes);
        boolean isVerified = ecdsaVerify.verify(signature);
        System.out.println("Signature verified: " + isVerified); // 应输出 true

        String licenseNum = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "000001";
        System.out.println("feat-users配置:");
        System.out.println("\tnum: " + licenseNum);
        System.out.println("\tname: " + data);
        System.out.println("\tlicense: " + Base64.getEncoder().encodeToString(signature));

        System.out.println("feat.yml配置:");
        System.out.println("\tlicense: " + licenseNum + "_" + publicKeyString);

    }


    private void loadFeatYaml() {
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

    private final PrintStream BLANK = new PrintStream(new OutputStream() {
        @Override
        public void write(int b) {

        }
    });

    private void deleteBuildDir(File dir) {
        if (!dir.isDirectory()) {
            return;
        }
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                deleteBuildDir(file);
            }
            file.delete();
        }
    }

    @Override
    public PrintWriter getPrintWriter() {
        return printWriter;
    }

    @Override
    public String packageName() {
        return PACKAGE;
    }

    @Override
    public String className() {
        return CLASS_NAME;
    }

    public void serializeImport() {
        printWriter.println("import " + AbstractCloudService.class.getName() + ";");
        printWriter.println("import " + ApplicationContext.class.getName() + ";");
        printWriter.println("import " + Router.class.getName() + ";");
        printWriter.println("import " + CloudService.class.getName() + ";");
        printWriter.println("import " + List.class.getName() + ";");
        printWriter.println("import " + SessionManager.class.getName() + ";");
        if (redisSession) {
            printWriter.println("import " + ClusterSessionManager.class.getName() + ";");
        } else {
            printWriter.println("import " + LocalSessionManager.class.getName() + ";");
        }
        printWriter.println("import " + ArrayList.class.getName() + ";");
        for (String service : services) {
            printWriter.println("import " + service + ";");
        }
        if (isAutoSSL()) {
            printWriter.println("import " + SslPlugin.class.getName() + ";");
            printWriter.println("import " + AutoServerSSLContextFactory.class.getName() + ";");
        }

        extensions.forEach(Serializer::serializeImport);
    }

    @Override
    public void serializeProperty() {
        printWriter.append("\tpublic static final String license_num = ");
        printWriter.println(license == null ? "null;" : "\"" + license.getNum() + "\";");
        printWriter.append("\tpublic static final String license_name = ");
        printWriter.println(license == null ? "null;" : "\"" + license.getName() + "\";");

        printWriter.println();

        printWriter.println("\tstatic {");
        if (license == null) {
            if (FeatUtils.isBlank(modelName)) {
                printWriter.println("\t\tSystem.out.println(\"\\u001B[31m温馨提示：存在未经商业授权的依赖模块, 请确保在 AGPL 3.0 的协议框架下合法合规使用 Feat.\\u001B[0m\");");
            } else {
                printWriter.println("\t\tSystem.out.println(\"\\u001B[31m温馨提示：当前服务所依赖的模块[\\033[4m" + modelName + "\\u001B[0m]\\u001B[31m尚未获得商业授权, 请确保在 AGPL 3.0 的协议框架下合法合规使用 Feat.\\u001B[0m\");");
            }
        } else {
            if (FeatUtils.isBlank(modelName)) {
                printWriter.println("\t\tSystem.out.println(\"\\u001B[32mFeat License verification passed! License No: \\033[4m" + license.getNum() + "\\u001B[0m\\u001B[32m Granted for:\\033[4m" + license.getName() + "\\u001B[0m\");");
            } else {
                printWriter.println("\t\tStringBuilder sb = new StringBuilder();");
                printWriter.println("\t\tsb.append(\"\\u001B[32mThe dependent module [\");");
                printWriter.println("\t\tsb.append(\"" + modelName + "\");");
                printWriter.println("\t\tsb.append(\"] has passed Feat License verification! License No: \\033[4m\\u001B[1m\");");
                printWriter.println("\t\tsb.append(\"" + license.getNum() + "\");");
                printWriter.println("\t\tsb.append(\"\\u001B[0m\\u001B[32m Granted for: \\033[4m\\u001B[1m\");");
                printWriter.println("\t\tsb.append(\"" + license.getName() + "\");");
                printWriter.println("\t\tsb.append(\"\\u001B[0m\");");
                printWriter.println("\t\tSystem.out.println(sb.toString());");
            }

        }
        printWriter.println("\t}");
        printWriter.println();
        printWriter.println("\tprivate List<" + CloudService.class.getSimpleName() + "> services = new " + ArrayList.class.getSimpleName() + "(" + services.size() + ");");
    }

    @Override
    public void serializeLoadBean() {
        for (Field field : ServerOptions.class.getDeclaredFields()) {
            Class<?> type = field.getType();
            if (type.isArray()) {
                type = type.getComponentType();
            }
            if (!availableTypes.contains(type.getName())) {
                continue;
            }
            Object obj = JSONPath.eval(config, "$.server." + field.getName());
            if (obj == null) {
                continue;
            }
            if (field.getType() == int.class) {
                printWriter.println("\t\tapplicationContext.getOptions()." + field.getName() + "(" + FeatUtils.class.getName() + ".toInt(System.getenv(\"FEAT_SERVER_" + field.getName().toUpperCase() + "\")," + obj + "));");
            } else {
                printWriter.println("\t\tapplicationContext.getOptions()." + field.getName() + "(" + obj + ");");
            }
        }

        //特殊的配置
        if (isAutoSSL()) {
            printWriter.println("\t\tapplicationContext.getOptions().addPlugin(new SslPlugin<>(new AutoServerSSLContextFactory()));");
        }

        for (String service : services) {
            String simpleClass = service.substring(service.lastIndexOf(".") + 1);
            printWriter.println("\t\tif (acceptService(applicationContext, \"" + service + "\")) {");
            printWriter.append("\t\t\t").append(CloudService.class.getSimpleName()).append(" service = new ").append(simpleClass).println("();");
            printWriter.println("\t\t\tservice.loadBean(applicationContext);");
            printWriter.println("\t\t\tservices.add(service);");
            printWriter.println("\t\t}");
        }

        extensions.forEach(serializer -> {
            printWriter.println("\t\t{");
            serializer.serializeLoadBean();
            printWriter.println("\t\t}");
        });
    }

    private boolean isAutoSSL() {
        Object obj = JSONPath.eval(config, "$.server.autoSSL");
        return obj == Boolean.TRUE || "true".equals(obj);
    }

    @Override
    public void serializeLoadMethodBean() {
        printWriter.println("\t\tfor (CloudService service : services) {");
        printWriter.println("\t\t\tservice.loadMethodBean(applicationContext);");
        printWriter.println("\t\t}");
    }

    @Override
    public void serializeAutowired() {
        printWriter.println("\t\tfor (CloudService service : services) {");
        printWriter.println("\t\t\tservice.autowired(applicationContext);");
        printWriter.println("\t\t}");
    }

    @Override
    public void serializeRouter() throws IOException {
        Object obj = JSONPath.eval(config, "$.server.session.timeout");
        if (redisSession) {
            printWriter.println("\t\tRedisun redisun = applicationContext.getBean(\"redisun\");");
            printWriter.println("\t\tSessionManager manager=new ClusterSessionManager(redisun);");
        } else {
            printWriter.println("\t\tSessionManager manager=new LocalSessionManager();");
        }
        if (obj != null) {
            printWriter.println("\t\tmanager.getOptions().setTimeout(" + obj + ");");
        }
        printWriter.println("\t\trouter.setSessionManager(manager);");

        printWriter.println("\t\tfor (CloudService service : services) {");
        printWriter.println("\t\t\tservice.router(applicationContext, router);");
        printWriter.println("\t\t}");
    }

    @Override
    public void serializePostConstruct() {
        printWriter.println("\t\tfor (CloudService service : services) {");
        printWriter.println("\t\t\tservice.postConstruct(applicationContext);");
        printWriter.println("\t\t}");
    }

    @Override
    public void serializeDestroy() {
        printWriter.println("\t\tfor (CloudService service : services) {");
        printWriter.println("\t\t\tservice.destroy();");
        printWriter.println("\t\t}");
    }
}

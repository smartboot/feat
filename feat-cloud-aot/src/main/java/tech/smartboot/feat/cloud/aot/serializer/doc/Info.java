package tech.smartboot.feat.cloud.aot.serializer.doc;

public class Info {
    private String title;
    private String version;
    private String description;
    private LicenseInfo license;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LicenseInfo getLicense() {
        return license;
    }

    public void setLicense(LicenseInfo license) {
        this.license = license;
    }
}
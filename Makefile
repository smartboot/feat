# 当需要升级版本时，执行该命令
version=2.3.0
update_version:
	sed -i  '' 's/public static final String VERSION = ".*";/public static final String VERSION = "v${version}";/' feat-core/src/main/java/tech/smartboot/feat/Feat.java
	mvn versions:set -DnewVersion=${version} versions:commit clean install -DskipTests

	@find demo -name pom.xml | xargs sed -i.bak \
		-E 's#(<feat.version>)[^<]+(</feat.version>)#\1$(version)\2#g'
	@find demo -name "*.bak" -delete


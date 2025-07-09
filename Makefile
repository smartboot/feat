# 当需要升级版本时，执行该命令
version=1.1.1-SNAPSHOT
update_version:
	sed -i  '' 's/public static final String VERSION = ".*";/public static final String VERSION = "v${version}";/' feat-core/src/main/java/tech/smartboot/feat/Feat.java
	mvn versions:set -DnewVersion=${version} versions:commit clean install -DskipTests
	mvn -f deploy/feat_static/pom.xml versions:set -DnewVersion=${version} versions:commit


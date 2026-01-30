# 当需要升级版本时，执行该命令
version=1.4.1
update_version:
	sed -i  '' 's/public static final String VERSION = ".*";/public static final String VERSION = "v${version}";/' feat-core/src/main/java/tech/smartboot/feat/Feat.java
	mvn versions:set -DnewVersion=${version} versions:commit clean install -DskipTests
	mvn -f demo/feat_static/pom.xml versions:set -DnewVersion=${version} versions:commit
	mvn -f demo/helloworld/pom.xml versions:set -DnewVersion=${version} versions:commit
	mvn -f demo/helloworld_docker/pom.xml versions:set -DnewVersion=${version} versions:commit
	#mvn -f demo/helloworld_native/pom.xml versions:set -DnewVersion=${version} versions:commit
	mvn -f demo/mybatis/pom.xml versions:set -DnewVersion=${version} versions:commit
	mvn -f demo/redis_session/pom.xml versions:set -DnewVersion=${version} versions:commit


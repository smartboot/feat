# 当需要升级版本时，执行该命令
version=2.4.1
update_version:
	sed -i  '' 's/public static final String VERSION = ".*";/public static final String VERSION = "v${version}";/' feat-core/src/main/java/tech/smartboot/feat/Feat.java
	sed -i  '' -E 's#(<span>v)[0-9]+\.[0-9]+\.[0-9]+( 已发布</span>)#\1${version}\2#' pages/src/components/home/HeroSection.astro
	sed -i  '' -E 's#(<version>)[0-9]+\.[0-9]+\.[0-9]+(</version>)#\1${version}\2#g' README.md README_zh.md
	mvn versions:set -DnewVersion=${version} versions:commit clean install -DskipTests

	@find demo -name pom.xml | xargs sed -i.bak \
		-E 's#(<feat.version>)[^<]+(</feat.version>)#\1$(version)\2#g'
	@find demo -name "*.bak" -delete


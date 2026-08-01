build_feat_static:
	java -version
	mvn clean install -f ../pom.xml -Dmaven.test.skip=true
	mvn clean package -f ./feat_static/pom.xml -Dmaven.test.skip=true


# feat-cloud-starter 目标版本
VERSION ?= 2.2.0
update-feat:
	@find . -name pom.xml | xargs sed -i.bak \
		-E 's#(<feat.version>)[^<]+(</feat.version>)#\1$(VERSION)\2#g'
	@find . -name "*.bak" -delete
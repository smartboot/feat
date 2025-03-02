#!/bin/sh
FEAT_HOME=$(dirname $(pwd))
BootStrapClass=tech.smartboot.feat.fileserver.HttpStaticResourceHandler
SERVLET_CLASSPATH=${FEAT_HOME}/lib/*
java -DFEAT_HOME_HOME=${FEAT_HOME} -cp "${SERVLET_CLASSPATH}" ${BootStrapClass}

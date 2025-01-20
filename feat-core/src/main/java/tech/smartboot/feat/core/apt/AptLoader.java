package tech.smartboot.feat.core.apt;

import tech.smartboot.feat.core.server.handler.Router;

public interface AptLoader {
    public void loadBean(ApplicationContext applicationContext) throws Throwable;

    public void autowired(ApplicationContext applicationContext);

    void postConstruct(ApplicationContext applicationContext) throws Throwable;

    void destroy() throws Throwable;

    void router(Router router);
}

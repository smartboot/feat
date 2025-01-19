package tech.smartboot.feat.core.apt;

import tech.smartboot.feat.core.server.handler.Router;

public interface AptLoader {
    public void loadBean(ApplicationContext applicationContext);

    public void autowired(ApplicationContext applicationContext);

    void postConstruct(ApplicationContext applicationContext);

    void destroy();

    void router(Router router);
}

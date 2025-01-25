package tech.smartboot.feat.cloud;


import tech.smartboot.feat.router.Router;

public interface ServiceLoader {
    void loadBean(ApplicationContext applicationContext) throws Throwable;

    void autowired(ApplicationContext applicationContext);

    void postConstruct(ApplicationContext applicationContext) throws Throwable;

    void destroy() throws Throwable;

    void router(Router router);
}

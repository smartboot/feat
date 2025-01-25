package tech.smartboot.feat.cloud;


import tech.smartboot.feat.router.Router;

public interface ServiceLoader {
    public void loadBean(ApplicationContext applicationContext) throws Throwable;

    public void autowired(ApplicationContext applicationContext);

    void postConstruct(ApplicationContext applicationContext) throws Throwable;

    void destroy() throws Throwable;

    void router(Router router);
}

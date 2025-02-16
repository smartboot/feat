package tech.smartboot.feat.cloud;


import tech.smartboot.feat.router.Router;

public interface CloudService {
    void loadBean(ApplicationContext context) throws Throwable;

    void autowired(ApplicationContext context);

    void postConstruct(ApplicationContext context) throws Throwable;

    void destroy() throws Throwable;

    void router(Router router);
}

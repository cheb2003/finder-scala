package my.finder.search.service;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

@Service
public class ServiceLocator implements ApplicationContextAware {
    private static ApplicationContext applicationContext;
    private static ServiceLocator serviceLocator;

    public static ServiceLocator getInstance() {
        if (serviceLocator == null) {
            if (applicationContext == null) {
                return null;
            }
            serviceLocator = applicationContext.getBean(ServiceLocator.class);
        }
        return serviceLocator;
    }

    public static <T> T getService(Class<T> clazz) {
        return applicationContext.getBean(clazz);
    }
    public static Object getService(String name) {
        return applicationContext.getBean(name);
    }

    public static String getProfile() {
        ApplicationContext ac = (ConfigurableApplicationContext) applicationContext;
        String[] activeProfiles = ac.getEnvironment().getActiveProfiles();
        return activeProfiles[0];
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        ServiceLocator.applicationContext = applicationContext;
    }
} 
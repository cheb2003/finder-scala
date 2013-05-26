package my.finder.search.web.util;

import my.finder.search.actor.SearchActorWrapper;
import my.finder.search.actor.SearchActorWrapper$;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 *
 */
public class InitScalaAppContextListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        SearchActorWrapper.init();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        SearchActorWrapper.destroy();
    }
}

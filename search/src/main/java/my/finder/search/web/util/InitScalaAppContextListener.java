package my.finder.search.web.util;

import my.finder.search.actor.ActorWrapper;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 *
 */
public class InitScalaAppContextListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ActorWrapper.init();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ActorWrapper.destroy();
    }
}

package monitor;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Starts the ExchangeRateMonitor when the web-app deploys
 * and stops it cleanly on un-deploy / server shutdown.
 */
public class RateMonitorListener implements ServletContextListener {

    public void contextInitialized(ServletContextEvent sce) {
        ExchangeRateMonitor.getInstance().start();
    }

    public void contextDestroyed(ServletContextEvent sce) {
        ExchangeRateMonitor.getInstance().stop();
    }
}

package com.depenses.listener;

import com.depenses.dao.DepenseDao;
import com.depenses.dao.DepenseRecurrenteDao;
import com.depenses.dao.JdbcDepenseDao;
import com.depenses.dao.JdbcDepenseRecurrenteDao;
import com.depenses.service.DepenseRecurrenteService;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DepenseRecurrenteSchedulerListener implements ServletContextListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(DepenseRecurrenteSchedulerListener.class);
    private ScheduledExecutorService scheduler;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        DepenseRecurrenteDao depenseRecurrenteDao = new JdbcDepenseRecurrenteDao();
        DepenseDao depenseDao = new JdbcDepenseDao();
        DepenseRecurrenteService service = new DepenseRecurrenteService(depenseRecurrenteDao, depenseDao);

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                service.traiterDepensesEchues();
            } catch (RuntimeException e) {
                LOGGER.error("Le traitement des dépenses récurrentes échues a échoué.", e);
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }
}

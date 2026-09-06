package vn.hcmute.jpaweb.config;

import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;
import jakarta.persistence.EntityManager;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

/**
 * Initializes JPA when the web application starts and releases it on shutdown.
 */
@WebListener
public class ApplicationLifecycleListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        EntityManager entityManager = JPAConfig.getEntityManager();
        try {
            sce.getServletContext().log("JPA persistence unit initialized successfully");
        } finally {
            entityManager.close();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        try {
            JPAConfig.close();
        } finally {
            // Connector/J creates a housekeeping thread and registers its JDBC
            // driver when Hibernate starts. Releasing both here prevents class
            // loader leaks when Tomcat redeploys the WAR. CRUD still uses JPA.
            AbandonedConnectionCleanupThread.checkedShutdown();
            deregisterApplicationDrivers(sce);
        }
    }

    private void deregisterApplicationDrivers(ServletContextEvent sce) {
        ClassLoader applicationClassLoader = getClass().getClassLoader();
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            if (driver.getClass().getClassLoader() == applicationClassLoader) {
                try {
                    DriverManager.deregisterDriver(driver);
                } catch (SQLException exception) {
                    sce.getServletContext().log(
                            "Không thể hủy đăng ký JDBC driver khi dừng ứng dụng.",
                            exception
                    );
                }
            }
        }
    }
}

package com.stony.reactor.jersey;

import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.spi.ExecutorServiceProvider;
import org.glassfish.jersey.spi.ScheduledExecutorServiceProvider;

import javax.ws.rs.core.Application;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * <p>reactor-netty-jersey
 * <p>com.stony.reactor.jersey
 *
 * @author stony
 * @version 下午4:45
 * @since 2018/12/11
 */
public class NettyHttpContainer implements Container {

    private volatile ApplicationHandler appHandler;

    public NettyHttpContainer(Application application) {
        this.appHandler = new ApplicationHandler(application);
        this.appHandler.onStartup(this);
    }

    @Override
    public ResourceConfig getConfiguration() {
        return appHandler.getConfiguration();
    }

    @Override
    public ApplicationHandler getApplicationHandler() {
        return appHandler;
    }

    @Override
    public void reload() {
        reload(appHandler.getConfiguration());
    }

    @Override
    public void reload(ResourceConfig configuration) {
        appHandler.onShutdown(this);

        appHandler = new ApplicationHandler(configuration);
        appHandler.onReload(this);
        appHandler.onStartup(this);
    }


    /**
     * Get {@link java.util.concurrent.ExecutorService}.
     *
     * @return Executor service associated with this container.
     */
    ExecutorService getExecutorService() {
        return appHandler.getInjectionManager().getInstance(ExecutorServiceProvider.class).getExecutorService();
    }

    /**
     * Get {@link ScheduledExecutorService}.
     *
     * @return Scheduled executor service associated with this container.
     */
    ScheduledExecutorService getScheduledExecutorService() {
        return appHandler.getInjectionManager().getInstance(ScheduledExecutorServiceProvider.class).getExecutorService();
    }
}
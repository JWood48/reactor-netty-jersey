package com.stony.reactor.jersey;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import javax.annotation.PreDestroy;
import javax.ws.rs.core.Application;

import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.server.SimpleHttpServerRoutes;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.http.server.HttpServerRoutes;

/**
 * <p>reactor-netty-jersey
 * <p>com.stony.reactor.jersey
 *
 * @author stony
 * @version 下午5:42
 * @since 2018/12/11
 */
public class JerseyBasedHandler implements BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>>, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(JerseyBasedHandler.class);
    volatile boolean isShutdown = false;

    final Application application;
    final NettyHttpContainer container;
    final NettyToJerseyBridge nettyToJerseyBridge;
    final ApplicationHandler applicationHandler;

    public JerseyBasedHandler(Application application) {
        this.application = application;

        this.container = new NettyHttpContainer(application);
        this.applicationHandler = container.getApplicationHandler();
        this.nettyToJerseyBridge = new NettyToJerseyBridge(container);

        logger.info("Started Jersey based request router.");
    }

    @Override
    public Publisher<Void> apply(HttpServerRequest httpServerRequest, HttpServerResponse httpServerResponse) {
        final InputStream requestData = new HttpContentInputStream(httpServerResponse.alloc(), httpServerRequest.receive());

        final ContainerRequest containerRequest = nettyToJerseyBridge.bridgeRequest(httpServerRequest, requestData);

        final ContainerResponseWriter containerResponse = nettyToJerseyBridge.bridgeResponse(httpServerResponse);

        containerRequest.setWriter(containerResponse);

        return Mono.<Void>create(sink -> {
            try {
                applicationHandler.handle(containerRequest);
                sink.success();
            } catch (Exception e) {
                logger.error("Failed to handle request.", e);
                sink.error(e);
            } finally {
                //close input stream and release all data we buffered, ignore errors
                try {
                    requestData.close();
                } catch (IOException e) {
                }
            }
        }).subscribeOn(Schedulers.elastic());
    }

    @Override
    public void close() throws Exception {

    }

    @PreDestroy
    public void stop() {
        if (isShutdown) return;
        logger.info("Stopped Jersey based request router.");
//        application.destroy();
        synchronized (this) {
            isShutdown = true;
        }
    }
    public static Builder builder() {
        return new Builder();
    }
    public static final class Builder {
        String appName;
        List<String> packages = new ArrayList<>();
        Set<Class<?>> providers = new HashSet<>(16);
        Map<String, Object> properties = new HashMap<>(64);

        Consumer<HttpServerRoutes> routesBuilder;

        public Builder withRouter(Consumer<HttpServerRoutes> routesBuilder) {
            this.routesBuilder = routesBuilder;
            return this;
        }

        public Builder register(Class<?> provider) {
            providers.add(provider);
            return this;
        }
        public Builder packages(final String... packs) {
            for (String pack : packs) {
                this.packages.add(pack);
            }
            return this;
        }
        public Builder property(final String name, final Object value) {
            properties.put(name, value);
            return this;
        }
        public Builder appName(String name) {
            this.appName = name;
            return this;
        }

        public BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> build() {
            final ResourceConfig config = new ResourceConfig().packages(packages.toArray(new String[packages.size()]));
            if(appName != null) {
                config.setApplicationName(appName);
            }
            if(providers != null) {
                providers.stream().forEach(config::register);
            }
            if(properties != null && !properties.isEmpty()) {
                config.addProperties(properties);
            }
            if (this.routesBuilder != null) {
                HttpServerRoutes routes = SimpleHttpServerRoutes.newRoutes(new JerseyBasedHandler(config));
                routesBuilder.accept(routes);
                return routes;
            }
            return new JerseyBasedHandler(config);
        }
    }
}

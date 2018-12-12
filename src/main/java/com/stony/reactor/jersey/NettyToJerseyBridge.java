package com.stony.reactor.jersey;

import org.glassfish.jersey.internal.PropertiesDelegate;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.ipc.netty.http.server.HttpServerRequest;
import reactor.ipc.netty.http.server.HttpServerResponse;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.SecurityContext;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>reactor-netty-jersey
 * <p>com.stony.reactor.jersey
 *
 * @author stony
 * @version 下午4:25
 * @since 2018/12/11
 */
public class NettyToJerseyBridge {
    protected static final Logger logger = LoggerFactory.getLogger(NettyToJerseyBridge.class);

    final NettyHttpContainer container;

    public NettyToJerseyBridge(NettyHttpContainer container) {
        this.container = container;
    }


    ContainerRequest bridgeRequest(final HttpServerRequest nettyRequest, InputStream requestData ) {
        try {
            URI baseUri = new URI("/"); // Since the netty server does not have a context path element as such, so base uri is always /
            URI requestUri = new URI(nettyRequest.uri());

            ContainerRequest requestContext = new ContainerRequest(
                    baseUri, requestUri, nettyRequest.method().name(), getSecurityContext(), getPropertiesDelegate()){
                @Override
                public Object getProperty(String name) {

                    return super.getProperty(name);
                }
            };

            for (Map.Entry<String, String> entry : nettyRequest.requestHeaders()) {
                requestContext.header(entry.getKey(), entry.getValue());
            }
            requestContext.setEntityStream(requestData);
            return requestContext;
        } catch (URISyntaxException e) {
            logger.error(String.format("Invalid request uri: %s", nettyRequest.uri()), e);
            throw new IllegalArgumentException(e);
        }
    }
    ContainerResponseWriter bridgeResponse(final HttpServerResponse serverResponse) {
        return new NettyContainerResponseWriter(container, serverResponse);
    }


    private SecurityContext getSecurityContext() {
        return new SecurityContext() {
            @Override
            public boolean isUserInRole(final String role) {
                return false;
            }

            @Override
            public boolean isSecure() {
                return false;
            }

            @Override
            public Principal getUserPrincipal() {
                return null;
            }

            @Override
            public String getAuthenticationScheme() {
                return null;
            }
        };
    }

    private PropertiesDelegate getPropertiesDelegate() {
        return new PropertiesDelegate() {

            private final Map<String, Object> properties = new HashMap<>();

            @Override
            public Object getProperty(String name) {
                return properties.get(name);
            }

            @Override
            public Collection<String> getPropertyNames() {
                return properties.keySet();
            }

            @Override
            public void setProperty(String name, Object object) {
                properties.put(name, object);
            }

            @Override
            public void removeProperty(String name) {
                properties.remove(name);
            }
        };
    }


}
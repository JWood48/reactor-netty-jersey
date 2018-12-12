package com.stony.reactor.jersey;

import org.glassfish.jersey.server.spi.ContainerProvider;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Application;

/**
 * <p>reactor-netty-jersey
 * <p>com.stony.reactor.jersey
 *
 * @author stony
 * @version 下午4:47
 * @since 2018/12/11
 */
public class NettyHttpContainerProvider implements ContainerProvider {
    @Override
    public <T> T createContainer(Class<T> type, Application application) throws ProcessingException {
        if (NettyHttpContainer.class == type) {
            return type.cast(new NettyHttpContainer(application));
        }
        return null;
    }
}

package com.stony.reactor.jersey;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.glassfish.jersey.server.ContainerException;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerResponse;

/**
 * <p>reactor-netty-jersey
 * <p>com.stony.reactor.jersey
 *
 * @author stony
 * @version 下午5:18
 * @since 2018/12/11
 */
public class NettyContainerResponseWriter implements ContainerResponseWriter {
    private static final Logger logger = LoggerFactory.getLogger(NettyContainerResponseWriter.class);

    private final NettyHttpContainer container;
    private final HttpServerResponse serverResponse;
    private final ByteBuf contentBuffer;

    private volatile ScheduledFuture<?> suspendTimeoutFuture;
    private volatile Runnable suspendTimeoutHandler;


    public NettyContainerResponseWriter(NettyHttpContainer container, HttpServerResponse serverResponse) {
        this.container = container;
        this.serverResponse = serverResponse;
        this.contentBuffer = serverResponse.alloc().buffer();
    }

    @Override
    public OutputStream writeResponseStatusAndHeaders(long contentLength, ContainerResponse response) throws ContainerException {
        if(logger.isTraceEnabled()) {
            logger.trace("entity = " + response.getEntity());
        }
        serverResponse.status(response.getStatus());
        HttpHeaders responseHeaders = new DefaultHttpHeaders();

        for (final Map.Entry<String, List<String>> e : response.getStringHeaders().entrySet()) {
            responseHeaders.add(e.getKey().toLowerCase(), e.getValue());
        }
        serverResponse.headers(responseHeaders);
        return new ByteBufOutputStream(contentBuffer);
    }

    @Override
    public void commit() {
        if(logger.isTraceEnabled()) {
            byte[] bytes = new byte[contentBuffer.readableBytes()];
            contentBuffer.readBytes(bytes);
            String str = new String(bytes, StandardCharsets.UTF_8);
            logger.trace("send buffer = {}", str);
        }
        CountDownLatch latch  = new CountDownLatch(1);
        serverResponse.send(Mono.just(contentBuffer)).subscribe(new Subscriber<Void>() {
            @Override
            public void onSubscribe(Subscription s) {
            }
            @Override
            public void onNext(Void aVoid) {
            }
            @Override
            public void onError(Throwable t) {
                logger.error("send msg error", t);
                latch.countDown();
            }

            @Override
            public void onComplete() {
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.error("send msg interrupted", e);
        }
        logger.trace("send finish ......");
    }

    @Override
    public void failure(Throwable error) {
        serverResponse.header("500", "Internal Server Error").send().then().log("error: "+ error.getMessage());
    }

    @Override
    public boolean suspend(long timeOut, TimeUnit timeUnit, TimeoutHandler timeoutHandler) {
        suspendTimeoutHandler = new Runnable() {
            @Override
            public void run() {
                timeoutHandler.onTimeout(NettyContainerResponseWriter.this);
            }
        };

        if (timeOut <= 0) {
            return true;
        }

        suspendTimeoutFuture =
                container.getScheduledExecutorService().schedule(suspendTimeoutHandler, timeOut, timeUnit);

        return true;
    }

    @Override
    public void setSuspendTimeout(long timeOut, TimeUnit timeUnit) throws IllegalStateException {
        // suspend(0, .., ..) was called, so suspendTimeoutFuture is null.
        if (suspendTimeoutFuture != null) {
            suspendTimeoutFuture.cancel(true);
        }

        if (timeOut <= 0) {
            return;
        }

        suspendTimeoutFuture =
                container.getScheduledExecutorService().schedule(suspendTimeoutHandler, timeOut, timeUnit);
    }



    @Override
    public boolean enableResponseBuffering() {
        return true;
    }
}

package com.stony.main;

import com.alibaba.fastjson.JSON;
import com.stony.controllers.UserTest;
import com.stony.reactor.jersey.JacksonProvider;
import com.stony.reactor.jersey.JerseyBasedHandler;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.glassfish.jersey.server.ServerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServer;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * <p>reactor-netty-jersey
 * <p>com.stony.controllers
 *
 * @author stony
 * @version 下午6:03
 * @since 2018/12/11
 */
public class MainTest {

    private static final Logger logger = LoggerFactory.getLogger(MainTest.class);
    public static void main(String[] args) throws Exception {

        final Path resource = Paths.get(MainTest.class.getResource("/public").toURI());

        HttpServer.create()
                .port(8084)
                .handle(JerseyBasedHandler.builder()
                        .property(ServerProperties.TRACING, "ON_DEMAND")
                        .property(ServerProperties.TRACING_THRESHOLD, "SUMMARY")
                        .register(JacksonProvider.class)
                        .packages("com.stony.controllers")
                        .appName("my-app-web")
                        .withRouter(routes -> {
                            routes.get("/v10/get", (req, resp) -> resp.sendString(Mono.just("asdfasdf")))
                                    .get("/v10/get2", (req, resp) -> {
                                        logger.info("json enter");
                                        return resp.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                                                .sendString(Mono.just(JSON.toJSONString(new UserTest("che", 10, "blue"))));
                                    })
                                    .directory("/res", resource)
                                    .get("/v9/get", (req, resp) -> {
                                        System.out.println(req.params());
                                        System.out.println(req.path());
                                        System.out.println(req.uri());
                                        System.out.println("id = " + req.param("id"));
                                        System.out.println("name = " + req.param("name"));
                                        return resp
                                                .header(HttpHeaderNames.CONTENT_TYPE, "text/plain;charset=utf-8")
                                                .chunkedTransfer(false)
                                                .sendString(Mono.just(req.params().toString()));
                                    });
                        })
                        .build())
                .bindNow();


                Thread.sleep(1000*1000);

            ;




    }
}
package reactor.netty.http.server;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;


import com.stony.reactor.jersey.MimeType;
import com.stony.reactor.jersey.MimeTypeUtil;

import org.reactivestreams.Publisher;

import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
//import reactor.netty.http.server.HttpPredicate;


/**
 * <p>reactor-netty-ext
 * <p>reactor.ipc.netty.http.server
 *
 * @author stony
 * @version 下午5:08
 * @since 2018/2/8
 */
public class SimpleHttpServerRoutes implements HttpServerRoutes {

    BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> handler;
    private volatile Path staticDirectory;

    private SimpleHttpServerRoutes(BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> handler) {
        this.handler = handler;
    }

    public static HttpServerRoutes newRoutes(BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> handler) {
        return new SimpleHttpServerRoutes(handler);
    }

    private final CopyOnWriteArrayList<HttpRouteHandler> handlers = new CopyOnWriteArrayList<>();

    @Override
    public HttpServerRoutes get(String path, BiFunction<? super HttpServerRequest, ? super HttpServerResponse, ? extends Publisher<Void>> handler) {
        return route(HttpPredicate.get(path), handler);
    }

    @Override
    public HttpServerRoutes directory(
            String uri,
            Path directory,
            Function<HttpServerResponse, HttpServerResponse> interceptor) {
        Objects.requireNonNull(directory, "directory");
        this.staticDirectory = directory;

        return route(HttpPredicate.prefix(uri), (req, resp) -> {

            String prefix = URI.create(req.uri())
                    .getPath()
                    .replaceFirst(uri, "");

            if (prefix.charAt(0) == '/') {
                prefix = prefix.substring(1);
            }

            Path p = directory.resolve(prefix);
            if (Files.isReadable(p)) {

                if (interceptor != null) {
                    return interceptor.apply(resp)
                            .sendFile(p);
                }
                return resp.sendFile(p);
            }

            return resp.sendNotFound();
        });
    }

    @Override
    public HttpServerRoutes route(Predicate<? super HttpServerRequest> condition,
                                  BiFunction<? super HttpServerRequest, ? super HttpServerResponse, ? extends Publisher<Void>> handler) {
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(handler, "handler");

        if (condition instanceof HttpPredicate) {
            handlers.add(new HttpRouteHandler(condition, handler, (HttpPredicate) condition));
        } else
        if (condition instanceof SimpleHttpPredicate) {
            handlers.add(new HttpRouteHandler(condition, handler, (SimpleHttpPredicate) condition));
        } else {
            handlers.add(new HttpRouteHandler(condition, handler, null));
        }
        return this;
    }


    @Override
    public Publisher<Void> apply(HttpServerRequest request, HttpServerResponse response) {
        final Iterator<HttpRouteHandler> iterator = handlers.iterator();
        HttpRouteHandler cursor;
        try {
            while (iterator.hasNext()) {
                cursor = iterator.next();
                if (cursor.test(request)) {
                    return cursor.apply(request, response);
                }
            }
            if(staticDirectory != null) {
                MimeType type = MimeTypeUtil.getInstance().getMimeTypeByPath(request.path());
                if(type != null) {
                    Path p = staticDirectory.resolve(type.getFilePath());
                    if (Files.isReadable(p)) {
                        return response.header("Content-Type", type.getContentType()).sendFile(p);
//                        return response.sendFile(p);
                    }
                }
            }
            if (handler != null) {
                return handler.apply(request, response);
            }
        } catch (Throwable t) {
            Exceptions.throwIfFatal(t);
            return Mono.error(t); //500
        }
        return response.sendNotFound();
    }

    /**
     */
    static final class HttpRouteHandler
            implements BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>>,
            Predicate<HttpServerRequest> {

        final Predicate<? super HttpServerRequest> condition;
        final BiFunction<? super HttpServerRequest, ? super HttpServerResponse, ? extends Publisher<Void>>
                handler;
        final Function<? super String, Map<String, String>> resolver;

        HttpRouteHandler(Predicate<? super HttpServerRequest> condition,
                         BiFunction<? super HttpServerRequest, ? super HttpServerResponse, ? extends Publisher<Void>> handler,
                         Function<? super String, Map<String, String>> resolver) {
            this.condition = Objects.requireNonNull(condition, "condition");
            this.handler = Objects.requireNonNull(handler, "handler");
            this.resolver = resolver;
        }

        @Override
        public Publisher<Void> apply(HttpServerRequest request,
                                     HttpServerResponse response) {
            return handler.apply(request.paramsResolver(resolver), response);
        }

        @Override
        public boolean test(HttpServerRequest o) {
            return condition.test(o);
        }
    }
}

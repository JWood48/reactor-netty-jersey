### reactor-netty-jersey
#### 1. [reactor-netty](https://github.com/reactor/reactor-netty)  jersey2.x的扩展
#### 2. 扩展get方法支持get?p=1 的方式访问
#### 3. 支持静态资源不加配置前缀（如不加: `/res` ）
#### 4. 增加对原有路由全桥接，支持静态文件访问
### 代码示例：
```
final Path resource = Paths.get(MainTest.class.getResource("/public").toURI());
HttpServer.create(opts -> opts.port(8084))
             .startAndAwait(JerseyBasedHandler.builder()
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
                     .build()
             );

public class UserTest {
    String name;
    int id;
    @JsonProperty("first_name")
    private String firstName;

    @JsonCreator
    public UserTest(@JsonProperty("name") String name, @JsonProperty("id") int id, @JsonProperty("first_name") String firstName) {
        this.name = name;
        this.id = id;
        this.firstName = firstName;
    }
}

@Path("/api")
public class ApiController {
    @POST()
    @Path("/post")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public UserTest post(UserTest body) {
        System.out.println("body = " + body);
        return new UserTest("bai", 220, "li");
    }
    @GET()
    @Path("/json")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public UserTest json() {
        logger.info("json enter");
        return new UserTest("bai", 200, "li");
    }
}



curl -X POST \
  http://localhost:8084/api/post \
  -H 'cache-control: no-cache' \
  -H 'content-type: application/json' \
  -H 'postman-token: f202fc59-94b9-f0e3-307d-35cb0fe885d6' \
  -d '{"first_name":"li","name":"bai","id":200}'



  {
      "name": "bai",
      "id": 200,
      "first_name": "li"
  }
```
##压测示例(没装wrk可以使用ab压测)：
```

wrk -H 'Connection: keep-alive' -t12 -c400 -d30s http://localhost:8084/v10/get

Running 30s test @ http://localhost:8084/v10/get
  12 threads and 400 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    18.51ms   88.31ms   1.65s    98.10%
    Req/Sec     4.38k     1.83k   16.25k    60.54%
  1535312 requests in 30.10s, 150.81MB read
Requests/sec:  51013.47
Transfer/sec:      5.01MB


wrk -H 'Connection: keep-alive' -t20 -c400 -d30s http://localhost:8084/api/json

Running 30s test @ http://localhost:8084/api/json
  20 threads and 400 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    54.04ms    9.31ms 142.88ms   71.88%
    Req/Sec   370.86     53.38   676.00     71.84%
  221881 requests in 30.08s, 107.92MB read
  Socket errors: connect 0, read 221881, write 0, timeout 0
Requests/sec:   7376.17
Transfer/sec:      3.59MB
```
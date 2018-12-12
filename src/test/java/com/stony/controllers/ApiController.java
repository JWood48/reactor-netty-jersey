package com.stony.controllers;

import org.glassfish.jersey.server.monitoring.MonitoringStatistics;
import org.glassfish.jersey.server.monitoring.TimeWindowStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>reactor-netty-jersey
 * <p>com.stony.con
 *
 * @author stony
 * @version 下午6:01
 * @since 2018/12/11
 */
@Path("/api")
public class ApiController {
    final String APPLICATION_JSON_UTF8 =  "application/json; charset=UTF-8";
    protected final Logger logger = LoggerFactory.getLogger(ApiController.class);

    @GET
    @Path("/sex")
    @Produces("text/plain")
    public String getSex(@QueryParam("name") String name){
        logger.info(name);
        return "male>" + name;
    }


    @GET()
    @Path("/json")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public UserTest json() {
        logger.info("json enter");
        return new UserTest("keke", 200, "li");
    }

    @GET()
    @Path("/list")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List list() {
        List list = new ArrayList<>(128);
        for (int i = 0; i < 100; i++) {
            list.add(new UserTest("中文"+i, 200+i, "li"+i));
        }
        return list;
    }


    @POST()
    @Path("/post")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public UserTest post(UserTest user) {
        logger.info("post enter : {}", user);
        user.setName(user.getName().toUpperCase());
        return user;
    }



    @POST()
    @Path("/post2")
    @Consumes(APPLICATION_JSON_UTF8)
    @Produces(APPLICATION_JSON_UTF8)
    public UserTest post2(UserTest user) {
        logger.info("post enter : {}", user);
        user.setName(user.getName().toUpperCase());
        return user;
    }


    @Inject
    Provider<MonitoringStatistics> statistics;

    @GET
    @Path("/uri")
    @Produces(MediaType.APPLICATION_JSON)
    public Map getTotalExceptionMappings() throws InterruptedException {
        final MonitoringStatistics monitoringStatistics = statistics.get();
//        final long totalExceptionMappings = monitoringStatistics.getExceptionMapperStatistics().getTotalMappings();


        return monitoringStatistics.getUriStatistics();
    }
    @GET
    @Path("/uri2")
    public String getSomething() {
        final MonitoringStatistics snapshot = statistics.get();

        final TimeWindowStatistics timeWindowStatistics
                = snapshot.getRequestStatistics()
                .getTimeWindowStatistics().get(0l);

        return "request count: " + timeWindowStatistics.getRequestCount()
                + ", average request processing [ms]: "
                + timeWindowStatistics.getAverageDuration();
    }


}

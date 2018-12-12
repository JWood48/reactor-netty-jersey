package com.stony.main;

import com.stony.controllers.UserTest;
import com.stony.reactor.jersey.JacksonProvider;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * <p>reactor-netty-jersey
 * <p>com.stony.main
 *
 * @author stony
 * @version 下午3:25
 * @since 2018/12/12
 */
public class ClientTest {

    Client client = ClientBuilder.newClient();
    WebTarget api = client.target("http://127.0.0.1:8084/api");

    @Test
    public void test_13(){
        WebTarget target = client.target("http://127.0.0.1:8084/api/list");

        System.out.println(target.request().get(String.class));
    }

    @Test
    public void test_30(){
        WebTarget target = api.path("sex").queryParam("name", "bule");
        System.out.println(target.request().get(String.class));
    }

    @Test
    public void test_36() {
        Response response = api.path("post").register(JacksonProvider.class)
                .request()
                .post(Entity.entity(new UserTest("le", 10, "jiu"), MediaType.APPLICATION_JSON_TYPE));
        System.out.println(response.getStatus());
        System.out.println(response.readEntity(UserTest.class));
    }
}

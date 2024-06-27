package org.jboss.as.quickstarts.tasksrs.service;

import com.openshift.service.DemoResource;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.jboss.as.quickstarts.tasksrs.DefaultDeployment;
import org.jboss.as.quickstarts.tasksrs.model.Resources;
import org.jboss.as.quickstarts.tasksrs.model.Task;
import org.jboss.as.quickstarts.tasksrs.model.TaskDao;
import org.jboss.as.quickstarts.tasksrs.model.TaskDaoImpl;
import org.jboss.as.quickstarts.tasksrs.model.User;
import org.jboss.as.quickstarts.tasksrs.model.UserDao;
import org.jboss.as.quickstarts.tasksrs.model.UserDaoImpl;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.Transferable;

import java.io.ByteArrayOutputStream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

public class UserResourceIntegrationTest {

    public static byte[] deployment() throws IllegalArgumentException {
        WebArchive webArchive = new DefaultDeployment().withOriginalPersistence().withImportedData().getArchive()
                .addClasses(Resources.class, User.class, UserDao.class, Task.class, TaskDao.class, TaskDaoImpl.class, UserDaoImpl.class, TaskResource.class, UserResource.class, DemoResource.class);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        webArchive.as(ZipExporter.class).exportTo(outputStream);
        return outputStream.toByteArray();
    }

    @ClassRule
    public static final GenericContainer<?> wildfly
            = new GenericContainer<>("jboss/wildfly:21.0.2.Final")
            .withExposedPorts(8080, 9990)
            .withCopyToContainer(
                    Transferable.of(deployment()),
                    "/opt/jboss/wildfly/standalone/deployments/test.war");

    @Test
    public void test() {
        RequestSpecification requestSpecification = RestAssured.given()
                .baseUri("http://" + wildfly.getHost() + ":" + wildfly.getMappedPort(8080) + "/test");
        requestSpecification
                .get("/ws/demo/healthcheck")
                .andReturn()
                .then()
                .statusCode(200)
                .body("health", equalTo(1))
                .body("response", equalTo("Health Status: 1"));

        requestSpecification
                .accept(ContentType.JSON)
                .get("/ws/users")
                .andReturn()
                .then()
                .statusCode(200)
                .body(".", hasSize(2));
    }

}

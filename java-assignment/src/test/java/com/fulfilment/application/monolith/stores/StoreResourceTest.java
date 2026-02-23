package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StoreResourceTest {

    @Test
    @Order(1)
    public void testListAllStores() {
        given()
                .when().get("/store")
                .then()
                .statusCode(200)
                .body(containsString("TONSTAD"), containsString("KALLAX"), containsString("BESTÅ"));
    }

    @Test
    @Order(2)
    public void testGetSingleStore() {
        given()
                .when().get("/store/1")
                .then()
                .statusCode(200)
                .body(containsString("TONSTAD"));
    }

    @Test
    @Order(3)
    public void testGetStoreNotFound() {
        given().when().get("/store/999").then().statusCode(404);
    }

    @Test
    @Order(4)
    public void testCreateStore() {
        String payload = "{\"name\": \"NEW_STORE\", \"quantityProductsInStock\": 50}";
        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/store")
                .then()
                .statusCode(201)
                .body(containsString("NEW_STORE"));
    }

    @Test
    @Order(5)
    public void testCreateStoreInvalidId() {
        String payload = "{\"id\": 10, \"name\": \"INVALID_ID\", \"quantityProductsInStock\": 10}";
        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/store")
                .then()
                .statusCode(422);
    }

    @Test
    @Order(6)
    public void testUpdateStore() {
        String payload = "{\"name\": \"UPDATED_TONSTAD\", \"quantityProductsInStock\": 20}";
        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .put("/store/1")
                .then()
                .statusCode(200)
                .body(containsString("UPDATED_TONSTAD"));
    }

    @Test
    @Order(7)
    public void testUpdateStoreMissingName() {
        String payload = "{\"quantityProductsInStock\": 20}";
        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .put("/store/1")
                .then()
                .statusCode(422);
    }

    @Test
    @Order(8)
    public void testPatchStore() {
        String payload = "{\"name\": \"PATCHED_STORE\"}";
        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .patch("/store/2")
                .then()
                .statusCode(200)
                .body(containsString("PATCHED_STORE"));
    }

    @Test
    @Order(9)
    public void testDeleteStore() {
        given().when().delete("/store/3").then().statusCode(204);
        given().when().get("/store/3").then().statusCode(404);
    }
}

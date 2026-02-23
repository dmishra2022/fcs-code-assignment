package com.fulfilment.application.monolith.products;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProductEndpointTest {

  private static final String PATH = "product";

  @Test
  @Order(1)
  public void testListAllProducts() {
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(containsString("TONSTAD"), containsString("KALLAX"), containsString("BESTÅ"));
  }

  @Test
  @Order(2)
  public void testGetSingleProduct() {
    given().when().get(PATH + "/1").then().statusCode(200).body(containsString("TONSTAD"));
  }

  @Test
  @Order(3)
  public void testGetProductNotFound() {
    given().when().get(PATH + "/999").then().statusCode(404);
  }

  @Test
  @Order(4)
  public void testCreateProduct() {
    String payload = "{\"name\": \"NEW_PRODUCT\", \"description\": \"Description\", \"price\": 10.0, \"stock\": 100}";
    given()
        .contentType(ContentType.JSON)
        .body(payload)
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .body(containsString("NEW_PRODUCT"));
  }

  @Test
  @Order(5)
  public void testUpdateProduct() {
    String payload = "{\"name\": \"UPDATED_PRODUCT\", \"description\": \"Updated\", \"price\": 15.0, \"stock\": 50}";
    given()
        .contentType(ContentType.JSON)
        .body(payload)
        .when()
        .put(PATH + "/2")
        .then()
        .statusCode(200)
        .body(containsString("UPDATED_PRODUCT"));
  }

  @Test
  @Order(6)
  public void testDeleteProduct() {
    given().when().delete(PATH + "/1").then().statusCode(204);
    given().when().get(PATH + "/1").then().statusCode(404);
  }

  @Test
  @Order(7)
  public void testListAllAfterDelete() {
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(not(containsString("TONSTAD")), containsString("UPDATED_PRODUCT"), containsString("BESTÅ"));
  }
}

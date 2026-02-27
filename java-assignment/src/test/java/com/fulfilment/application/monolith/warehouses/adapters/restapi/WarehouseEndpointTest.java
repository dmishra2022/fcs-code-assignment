package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WarehouseEndpointTest {

  private static final String BASE_PATH = "warehouse";

  @Test
  @Order(1)
  void testSimpleListWarehouses() {
    given()
        .when()
        .get(BASE_PATH)
        .then()
        .statusCode(200)
        .body(containsString("MWH.001"), containsString("MWH.012"), containsString("MWH.023"));
  }

  @Test
  @Order(2)
  void testGetWarehouseById() {
    given()
        .when()
        .get(BASE_PATH + "/1")
        .then()
        .statusCode(200)
        .body(containsString("MWH.001"), containsString("ZWOLLE-001"));
  }

  @Test
  @Order(3)
  void testGetWarehouseNotFound_returns404() {
    given().when().get(BASE_PATH + "/999").then().statusCode(404);
  }

  @Test
  @Order(4)
  void testCreateWarehouse_invalidLocation_returns400() {
    String payload = """
        {
          "businessUnitCode": "MWH.NEW",
          "location": "INVALID-999",
          "capacity": 30,
          "stock": 5
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(payload)
        .when()
        .post(BASE_PATH)
        .then()
        .statusCode(400);
  }

  @Test
  @Order(5)
  void testCreateWarehouse_duplicateBuCode_returns400() {
    String payload = """
        {
          "businessUnitCode": "MWH.001",
          "location": "AMSTERDAM-001",
          "capacity": 50,
          "stock": 5
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(payload)
        .when()
        .post(BASE_PATH)
        .then()
        .statusCode(400);
  }

  @Test
  @Order(6)
  void testSimpleCheckingArchivingWarehouses() {
    // List all, should have all 3 warehouses
    given()
        .when()
        .get(BASE_PATH)
        .then()
        .statusCode(200)
        .body(
            containsString("MWH.001"),
            containsString("MWH.012"),
            containsString("MWH.023"),
            containsString("ZWOLLE-001"),
            containsString("AMSTERDAM-001"),
            containsString("TILBURG-001"));

    // Archive warehouse with id=1 (MWH.001):
    given().when().delete(BASE_PATH + "/1").then().statusCode(204);

    // MWH.001 should no longer appear in active list:
    given()
        .when()
        .get(BASE_PATH)
        .then()
        .statusCode(200)
        .body(not(containsString("MWH.001")), containsString("MWH.012"), containsString("MWH.023"));
  }

  @Test
  @Order(7)
  void testCreateWarehouse_exceedCapacity_returns400() {
    String payload = """
        {
          "businessUnitCode": "MWH.TOO_BIG",
          "location": "ZWOLLE-001",
          "capacity": 999,
          "stock": 0
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(payload)
        .when()
        .post(BASE_PATH)
        .then()
        .statusCode(400)
        .body(containsString("exceeds the maximum allowed capacity"));
  }

  @Test
  @Order(8)
  void testCreateWarehouse_stockExceedCapacity_returns400() {
    String payload = """
        {
          "businessUnitCode": "MWH.OVERSTOCKED",
          "location": "AMSTERDAM-001",
          "capacity": 10,
          "stock": 100
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(payload)
        .when()
        .post(BASE_PATH)
        .then()
        .statusCode(400)
        .body(containsString("Stock 100 exceeds warehouse capacity 10"));
  }

  @Test
  @Order(9)
  void testReplaceWarehouse_success() {
    String payload = """
        {
          "location": "AMSTERDAM-001",
          "capacity": 80,
          "stock": 5
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(payload)
        .when()
        .post(BASE_PATH + "/MWH.012/replacement")
        .then()
        .statusCode(200)
        .body(containsString("MWH.012"), containsString("80"));
  }

  @Test
  @Order(10)
  void testReplaceWarehouse_stockMismatch_returns400() {
    String payload = """
        {
          "location": "AMSTERDAM-001",
          "capacity": 80,
          "stock": 99
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(payload)
        .when()
        .post(BASE_PATH + "/MWH.023/replacement")
        .then()
        .statusCode(400)
        .body(containsString("must match the current stock"));
  }
}

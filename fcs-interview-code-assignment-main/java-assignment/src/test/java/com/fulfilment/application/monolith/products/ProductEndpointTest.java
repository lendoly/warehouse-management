package com.fulfilment.application.monolith.products;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNot.not;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProductEndpointTest {

  private static Long createdProductId;

  // ── GET /product ─────────────────────────────────────────────────────────────

  @Test
  @Order(1)
  public void testListAll() {
    given().when().get("/product").then()
        .statusCode(200)
        .body(containsString("TONSTAD"), containsString("KALLAX"), containsString("BESTÅ"));
  }

  // ── GET /product/{id} ────────────────────────────────────────────────────────

  @Test
  @Order(2)
  public void testGetSingle() {
    given().when().get("/product/2").then()
        .statusCode(200)
        .body(containsString("KALLAX"));
  }

  @Test
  @Order(3)
  public void testGetSingleNotFound() {
    given().when().get("/product/9999").then().statusCode(404);
  }

  // ── POST /product ────────────────────────────────────────────────────────────

  @Test
  @Order(4)
  public void testCreateWithIdFails() {
    given().contentType(ContentType.JSON)
        .body(Map.of("id", 99, "name", "INVALID", "stock", 0))
        .when().post("/product").then().statusCode(422);
  }

  @Test
  @Order(5)
  public void testCreate() {
    createdProductId = given().contentType(ContentType.JSON)
        .body(Map.of("name", "NEWPRODUCT", "stock", 3))
        .when().post("/product").then()
        .statusCode(201)
        .body(containsString("NEWPRODUCT"))
        .extract().jsonPath().getLong("id");

    given().when().get("/product").then()
        .statusCode(200)
        .body(containsString("NEWPRODUCT"));
  }

  // ── PUT /product/{id} ────────────────────────────────────────────────────────

  @Test
  @Order(6)
  public void testUpdateNameNullFails() {
    given().contentType(ContentType.JSON)
        .body(Map.of("stock", 5))
        .when().put("/product/" + createdProductId).then().statusCode(422);
  }

  @Test
  @Order(7)
  public void testUpdateNotFound() {
    given().contentType(ContentType.JSON)
        .body(Map.of("name", "X", "stock", 1))
        .when().put("/product/9999").then().statusCode(404);
  }

  @Test
  @Order(8)
  public void testUpdate() {
    given().contentType(ContentType.JSON)
        .body(Map.of("name", "UPDATEDPRODUCT", "stock", 10))
        .when().put("/product/" + createdProductId).then()
        .statusCode(200)
        .body(containsString("UPDATEDPRODUCT"))
        .body("stock", is(10));
  }

  // ── DELETE /product/{id} ─────────────────────────────────────────────────────

  @Test
  @Order(9)
  public void testDelete() {
    given().when().delete("/product/1").then().statusCode(204);

    given().when().get("/product").then()
        .statusCode(200)
        .body(not(containsString("TONSTAD")), containsString("KALLAX"), containsString("BESTÅ"));
  }

  @Test
  @Order(10)
  public void testDeleteNotFound() {
    given().when().delete("/product/9999").then().statusCode(404);
  }
}

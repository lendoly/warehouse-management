package com.fulfilment.application.monolith.stores;

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
public class StoreEndpointTest {

  // Shared across ordered tests so create/update/delete chain works
  private static Long createdStoreId;

  // ── GET /store ──────────────────────────────────────────────────────────────

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
  public void testListStoresSortedByName() {
    // Seeded: TONSTAD(1), KALLAX(2), BESTÅ(3) — alphabetical order: BESTÅ < KALLAX < TONSTAD
    String body = given()
        .when().get("/store")
        .then()
        .statusCode(200)
        .extract().asString();

    int bestaIndex  = body.indexOf("BESTÅ");
    int kallaxIndex = body.indexOf("KALLAX");
    int tonstadIndex = body.indexOf("TONSTAD");

    assert bestaIndex < kallaxIndex : "BESTÅ should come before KALLAX";
    assert kallaxIndex < tonstadIndex : "KALLAX should come before TONSTAD";
  }

  // ── GET /store/{id} ─────────────────────────────────────────────────────────

  @Test
  @Order(3)
  public void testGetSingleStore() {
    given()
        .when().get("/store/1")
        .then()
        .statusCode(200)
        .body(containsString("TONSTAD"));
  }

  @Test
  @Order(4)
  public void testGetSingleStoreNotFound() {
    given()
        .when().get("/store/9999")
        .then()
        .statusCode(404);
  }

  // ── POST /store ──────────────────────────────────────────────────────────────

  @Test
  @Order(5)
  public void testCreateStoreWithIdFails() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"id\": 99, \"name\": \"INVALID\", \"quantityProductsInStock\": 1}")
        .when().post("/store")
        .then()
        .statusCode(422);
  }

  @Test
  @Order(6)
  public void testCreateStore() {
    createdStoreId = given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"NEWSTORE\", \"quantityProductsInStock\": 5}")
        .when().post("/store")
        .then()
        .statusCode(201)
        .body(containsString("NEWSTORE"))
        .extract().jsonPath().getLong("id");

    // Verify it now appears in the list
    given()
        .when().get("/store")
        .then()
        .statusCode(200)
        .body(containsString("NEWSTORE"));
  }

  // ── PUT /store/{id} ──────────────────────────────────────────────────────────

  @Test
  @Order(7)
  public void testUpdateStore() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"UPDATED\", \"quantityProductsInStock\": 10}")
        .when().put("/store/" + createdStoreId)
        .then()
        .statusCode(200)
        .body(containsString("UPDATED"))
        .body(containsString("10"));
  }

  @Test
  @Order(8)
  public void testUpdateStoreNameNullFails() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"quantityProductsInStock\": 10}")
        .when().put("/store/" + createdStoreId)
        .then()
        .statusCode(422);
  }

  @Test
  @Order(9)
  public void testUpdateStoreNotFound() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"X\", \"quantityProductsInStock\": 1}")
        .when().put("/store/9999")
        .then()
        .statusCode(404);
  }

  // ── PATCH /store/{id} ────────────────────────────────────────────────────────

  @Test
  @Order(10)
  public void testPatchStore() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"PATCHED\", \"quantityProductsInStock\": 7}")
        .when().patch("/store/" + createdStoreId)
        .then()
        .statusCode(200)
        .body(containsString("PATCHED"))
        .body(containsString("7"));
  }

  @Test
  @Order(11)
  public void testPatchStoreNameNullFails() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"quantityProductsInStock\": 5}")
        .when().patch("/store/" + createdStoreId)
        .then()
        .statusCode(422);
  }

  @Test
  @Order(12)
  public void testPatchStoreNotFound() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\": \"X\", \"quantityProductsInStock\": 1}")
        .when().patch("/store/9999")
        .then()
        .statusCode(404);
  }

  // ── DELETE /store/{id} ───────────────────────────────────────────────────────

  @Test
  @Order(13)
  public void testDeleteStore() {
    given()
        .when().delete("/store/" + createdStoreId)
        .then()
        .statusCode(204);

    // Verify it no longer appears in the list
    given()
        .when().get("/store")
        .then()
        .statusCode(200)
        .body(not(containsString("PATCHED")));
  }

  @Test
  @Order(14)
  public void testDeleteStoreNotFound() {
    given()
        .when().delete("/store/9999")
        .then()
        .statusCode(404);
  }
}

package com.fulfilment.application.monolith.fulfillment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FulfillmentEndpointTest {

  // Shared state across ordered tests
  private static Long firstUnitId;
  private static Long product4Id;
  private static Long product5Id;
  private static Long product6Id;

  // Seeded: stores 1,2,3 | products 1,2,3 | warehouses MWH.001,MWH.012,MWH.023
  // FulfillmentEndpointTest runs first (package "fulfillment" < "location" < "products" < "stores")
  // so product 1 is still present when these tests execute.

  private static Map<String, Object> unit(Long storeId, Long productId, String warehouse) {
    return Map.of("storeId", storeId, "productId", productId,
        "warehouseBusinessUnitCode", warehouse);
  }

  // ── GET before any fulfillment data ─────────────────────────────────────────

  @Test
  @Order(1)
  public void testListAllEmpty() {
    given().when().get("/fulfillment").then().statusCode(200).body("size()", is(0));
  }

  @Test
  @Order(2)
  public void testListByStoreIdEmpty() {
    given().when().get("/fulfillment?storeId=1").then().statusCode(200).body("size()", is(0));
  }

  @Test
  @Order(3)
  public void testListByProductIdEmpty() {
    given().when().get("/fulfillment?productId=2").then().statusCode(200).body("size()", is(0));
  }

  @Test
  @Order(4)
  public void testListByBothEmpty() {
    given().when().get("/fulfillment?storeId=1&productId=2").then().statusCode(200).body("size()", is(0));
  }

  // ── POST: 404 error cases ────────────────────────────────────────────────────

  @Test
  @Order(5)
  public void testCreateStoreNotFound() {
    given().contentType(ContentType.JSON).body(unit(9999L, 2L, "MWH.001"))
        .when().post("/fulfillment").then().statusCode(404);
  }

  @Test
  @Order(6)
  public void testCreateProductNotFound() {
    given().contentType(ContentType.JSON).body(unit(1L, 9999L, "MWH.001"))
        .when().post("/fulfillment").then().statusCode(404);
  }

  @Test
  @Order(7)
  public void testCreateWarehouseNotFound() {
    given().contentType(ContentType.JSON).body(unit(1L, 2L, "UNKNOWN"))
        .when().post("/fulfillment").then().statusCode(404);
  }

  // ── POST: happy path ─────────────────────────────────────────────────────────

  @Test
  @Order(8)
  public void testCreate() {
    firstUnitId = given().contentType(ContentType.JSON).body(unit(1L, 2L, "MWH.001"))
        .when().post("/fulfillment")
        .then().statusCode(201).extract().jsonPath().getLong("id");
  }

  @Test
  @Order(9)
  public void testCreateDuplicate() {
    // Same storeId + productId + warehouseBusinessUnitCode must be rejected
    given().contentType(ContentType.JSON).body(unit(1L, 2L, "MWH.001"))
        .when().post("/fulfillment").then().statusCode(400);
  }

  // ── GET: all filter combinations after first unit ────────────────────────────

  @Test
  @Order(10)
  public void testListAfterCreate() {
    given().when().get("/fulfillment").then().statusCode(200).body("size()", is(1));
  }

  @Test
  @Order(11)
  public void testListByStoreId() {
    given().when().get("/fulfillment?storeId=1").then().statusCode(200).body("size()", is(1));
    given().when().get("/fulfillment?storeId=2").then().statusCode(200).body("size()", is(0));
  }

  @Test
  @Order(12)
  public void testListByProductId() {
    given().when().get("/fulfillment?productId=2").then().statusCode(200).body("size()", is(1));
    given().when().get("/fulfillment?productId=3").then().statusCode(200).body("size()", is(0));
  }

  @Test
  @Order(13)
  public void testListByBoth() {
    given().when().get("/fulfillment?storeId=1&productId=2").then().statusCode(200).body("size()", is(1));
    given().when().get("/fulfillment?storeId=1&productId=3").then().statusCode(200).body("size()", is(0));
  }

  // ── Constraint 1: each product can be fulfilled by max 2 warehouses per store ─

  @Test
  @Order(14)
  public void testConstraint1_SecondWarehouseAllowed() {
    // product 2 in store 1 has 1 warehouse (MWH.001) — adding a second is allowed
    given().contentType(ContentType.JSON).body(unit(1L, 2L, "MWH.012"))
        .when().post("/fulfillment").then().statusCode(201);
  }

  @Test
  @Order(15)
  public void testConstraint1_ThirdWarehouseRejected() {
    // product 2 in store 1 now has 2 warehouses (MWH.001, MWH.012) — 3rd must fail
    given().contentType(ContentType.JSON).body(unit(1L, 2L, "MWH.023"))
        .when().post("/fulfillment").then().statusCode(400);
  }

  // ── Constraint 2: each store can be fulfilled by max 3 different warehouses ───

  @Test
  @Order(16)
  public void testConstraint2_FillStore2WithThreeWarehouses() {
    // Give store 2 three distinct warehouses across different products
    given().contentType(ContentType.JSON).body(unit(2L, 2L, "MWH.001"))
        .when().post("/fulfillment").then().statusCode(201); // warehouse 1 for store 2
    given().contentType(ContentType.JSON).body(unit(2L, 3L, "MWH.012"))
        .when().post("/fulfillment").then().statusCode(201); // warehouse 2 for store 2
    given().contentType(ContentType.JSON).body(unit(2L, 2L, "MWH.023"))
        .when().post("/fulfillment").then().statusCode(201); // warehouse 3 for store 2
    // store 2 now has 3 distinct warehouses: MWH.001, MWH.012, MWH.023
  }

  @Test
  @Order(17)
  public void testConstraint2_FourthWarehouseRejected() {
    // Create a 4th warehouse — AMSTERDAM-001 has room (maxWarehouses=5, maxCapacity=100)
    given().contentType(ContentType.JSON)
        .body(Map.of("businessUnitCode", "MWH.CTEST", "location", "AMSTERDAM-001",
            "capacity", 10, "stock", 0))
        .when().post("/warehouse").then().statusCode(200);

    // Assigning MWH.CTEST to store 2 must be rejected (already at 3 distinct warehouses)
    given().contentType(ContentType.JSON).body(unit(2L, 3L, "MWH.CTEST"))
        .when().post("/fulfillment").then().statusCode(400);
  }

  // ── Constraint 3: each warehouse can store max 5 product types ────────────────

  @Test
  @Order(18)
  public void testConstraint3_FillWarehouseToCapacity() {
    // Create 3 extra products to reach the 5-product limit in MWH.023
    // MWH.023 already has product 2 (added in constraint-2 setup, order 16)
    product4Id = given().contentType(ContentType.JSON)
        .body(Map.of("name", "PROD-C3-A", "stock", 0))
        .when().post("/product").then().statusCode(201).extract().jsonPath().getLong("id");
    product5Id = given().contentType(ContentType.JSON)
        .body(Map.of("name", "PROD-C3-B", "stock", 0))
        .when().post("/product").then().statusCode(201).extract().jsonPath().getLong("id");
    product6Id = given().contentType(ContentType.JSON)
        .body(Map.of("name", "PROD-C3-C", "stock", 0))
        .when().post("/product").then().statusCode(201).extract().jsonPath().getLong("id");

    // Use store 3 + MWH.023 only (avoids triggering constraints 1 and 2 for store 3)
    given().contentType(ContentType.JSON).body(unit(3L, 3L, "MWH.023"))
        .when().post("/fulfillment").then().statusCode(201); // 2nd distinct product in MWH.023
    given().contentType(ContentType.JSON).body(unit(3L, 1L, "MWH.023"))
        .when().post("/fulfillment").then().statusCode(201); // 3rd
    given().contentType(ContentType.JSON).body(unit(3L, product4Id, "MWH.023"))
        .when().post("/fulfillment").then().statusCode(201); // 4th
    given().contentType(ContentType.JSON).body(unit(3L, product5Id, "MWH.023"))
        .when().post("/fulfillment").then().statusCode(201); // 5th — limit reached
    // MWH.023 now holds 5 distinct product types: 2, 3, 1, product4, product5
  }

  @Test
  @Order(19)
  public void testConstraint3_SixthProductRejected() {
    // product6 would be the 6th distinct product type in MWH.023 — must fail
    given().contentType(ContentType.JSON).body(unit(3L, product6Id, "MWH.023"))
        .when().post("/fulfillment").then().statusCode(400);
  }

  // ── DELETE ───────────────────────────────────────────────────────────────────

  @Test
  @Order(20)
  public void testDelete() {
    given().when().delete("/fulfillment/" + firstUnitId).then().statusCode(204);
  }

  @Test
  @Order(21)
  public void testDeleteNotFound() {
    given().when().delete("/fulfillment/9999").then().statusCode(404);
  }
}

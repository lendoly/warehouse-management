package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Seeded active warehouses at the start of this class (package "warehouses" runs last):
 *   MWH.001  ZWOLLE-001     cap=100 stock=10
 *   MWH.012  AMSTERDAM-001  cap=50  stock=5
 *   MWH.023  TILBURG-001    cap=30  stock=27
 *   MWH.CTEST AMSTERDAM-001 cap=10  stock=0   (created by FulfillmentEndpointTest)
 *
 * AMSTERDAM-001 used capacity: 60 / 100  active count: 2 / 5
 * TILBURG-001   active count: 1 / 1 (at limit)
 * ZWOLLE-001    active count: 1 / 1 (at limit), used capacity: 100 / 40 (over limit from seed)
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WarehouseEndpointTest {

  private static final String PATH = "/warehouse";

  private static Map<String, Object> warehouseBody(String code, String location, int capacity, int stock) {
    return Map.of("businessUnitCode", code, "location", location, "capacity", capacity, "stock", stock);
  }

  // ── GET /warehouse ───────────────────────────────────────────────────────────

  @Test
  @Order(1)
  public void testListAll() {
    given().when().get(PATH).then()
        .statusCode(200)
        .body(containsString("MWH.001"), containsString("MWH.012"), containsString("MWH.023"));
  }

  // ── GET /warehouse/{id} ──────────────────────────────────────────────────────

  @Test
  @Order(2)
  public void testGetByCode() {
    given().when().get(PATH + "/MWH.001").then()
        .statusCode(200)
        .body(containsString("MWH.001"), containsString("ZWOLLE-001"));
  }

  @Test
  @Order(3)
  public void testGetByCodeNotFound() {
    given().when().get(PATH + "/UNKNOWN").then().statusCode(404);
  }

  // ── POST /warehouse — validation errors ──────────────────────────────────────

  @Test
  @Order(4)
  public void testCreate_DuplicateBusinessUnitCode() {
    // MWH.001 is already active — must be rejected
    given().contentType(ContentType.JSON)
        .body(warehouseBody("MWH.001", "AMSTERDAM-001", 10, 0))
        .when().post(PATH).then().statusCode(400);
  }

  @Test
  @Order(5)
  public void testCreate_StockExceedsCapacity() {
    // stock(50) > capacity(10)
    given().contentType(ContentType.JSON)
        .body(warehouseBody("MWH.WTEST", "AMSTERDAM-001", 10, 50))
        .when().post(PATH).then().statusCode(400);
  }

  @Test
  @Order(6)
  public void testCreate_UnknownLocation() {
    // LocationGateway throws IllegalArgumentException → 400
    given().contentType(ContentType.JSON)
        .body(warehouseBody("MWH.WTEST", "INVALID-LOC", 10, 0))
        .when().post(PATH).then().statusCode(400);
  }

  @Test
  @Order(7)
  public void testCreate_ExceedsMaxWarehouseCount() {
    // TILBURG-001 already has 1 active warehouse, maxNumberOfWarehouses = 1
    given().contentType(ContentType.JSON)
        .body(warehouseBody("MWH.WTEST", "TILBURG-001", 5, 0))
        .when().post(PATH).then().statusCode(400);
  }

  @Test
  @Order(8)
  public void testCreate_ExceedsMaxLocationCapacity() {
    // AMSTERDAM-001: used=60, max=100 → adding capacity=50 gives 110 > 100
    given().contentType(ContentType.JSON)
        .body(warehouseBody("MWH.WTEST", "AMSTERDAM-001", 50, 0))
        .when().post(PATH).then().statusCode(400);
  }

  // ── POST /warehouse — success ────────────────────────────────────────────────

  @Test
  @Order(9)
  public void testCreate_Success() {
    // AMSTERDAM-001: used=60, max=100 → capacity=30 gives 90 ≤ 100, count=2 < 5
    given().contentType(ContentType.JSON)
        .body(warehouseBody("MWH.WTEST", "AMSTERDAM-001", 30, 0))
        .when().post(PATH).then()
        .statusCode(200)
        .body(containsString("MWH.WTEST"), containsString("AMSTERDAM-001"));

    // Verify it is now in the active list
    given().when().get(PATH).then()
        .statusCode(200)
        .body(containsString("MWH.WTEST"));
  }

  // ── DELETE /warehouse/{id} — archive ─────────────────────────────────────────

  @Test
  @Order(10)
  public void testArchive_Success() {
    given().when().delete(PATH + "/MWH.WTEST").then().statusCode(204);
  }

  @Test
  @Order(11)
  public void testArchive_VerifyRemovedFromList() {
    given().when().get(PATH).then()
        .statusCode(200)
        .body(not(containsString("MWH.WTEST")));
  }

  @Test
  @Order(12)
  public void testArchive_VerifyGetReturns404() {
    given().when().get(PATH + "/MWH.WTEST").then().statusCode(404);
  }

  @Test
  @Order(13)
  public void testArchive_NotFound() {
    // Already archived — findWarehouseById returns null → 404
    given().when().delete(PATH + "/MWH.WTEST").then().statusCode(404);
  }

  // ── POST /warehouse/{businessUnitCode}/replacement — validation errors ────────

  @Test
  @Order(14)
  public void testReplace_NotFound() {
    given().contentType(ContentType.JSON)
        .body(warehouseBody("UNKNOWN", "AMSTERDAM-001", 10, 0))
        .when().post(PATH + "/UNKNOWN/replacement").then().statusCode(404);
  }

  @Test
  @Order(15)
  public void testReplace_CapacityTooSmallForExistingStock() {
    // MWH.001 stock=10; new capacity=5 < 10 → rejected
    given().contentType(ContentType.JSON)
        .body(Map.of("location", "ZWOLLE-001", "capacity", 5))
        .when().post(PATH + "/MWH.001/replacement").then().statusCode(400);
  }

  @Test
  @Order(16)
  public void testReplace_ExceedsLocationMaxCapacity() {
    // ZWOLLE-001 maxCapacity=40; excluding MWH.001 → usedCapacity=0; 0+50=50 > 40 → rejected
    given().contentType(ContentType.JSON)
        .body(Map.of("location", "ZWOLLE-001", "capacity", 50))
        .when().post(PATH + "/MWH.001/replacement").then().statusCode(400);
  }

  // ── POST /warehouse/{businessUnitCode}/replacement — success ─────────────────

  @Test
  @Order(17)
  public void testReplace_Success() {
    // ZWOLLE-001 usedCapacity (excluding MWH.001) = 0; 0+30=30 ≤ 40; capacity 30 ≥ stock 10
    given().contentType(ContentType.JSON)
        .body(Map.of("location", "ZWOLLE-001", "capacity", 30))
        .when().post(PATH + "/MWH.001/replacement").then()
        .statusCode(200)
        .body(containsString("MWH.001"), containsString("ZWOLLE-001"));
  }

  @Test
  @Order(18)
  public void testReplace_VerifyNewCapacityAndStockRetained() {
    // New warehouse has updated capacity; stock is transferred from the old one (10)
    given().when().get(PATH + "/MWH.001").then()
        .statusCode(200)
        .body("capacity", is(30))
        .body("stock", is(10));
  }

  @Test
  @Order(19)
  public void testReplace_OldEntryNoLongerActive() {
    // The list should still contain exactly one MWH.001 (the new one)
    given().when().get(PATH).then()
        .statusCode(200)
        .body(containsString("MWH.001"));
  }
}

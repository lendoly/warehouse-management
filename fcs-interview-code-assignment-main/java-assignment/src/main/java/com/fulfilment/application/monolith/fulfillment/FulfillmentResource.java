package com.fulfilment.application.monolith.fulfillment;

import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/fulfillment")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FulfillmentResource {

  @Inject WarehouseRepository warehouseRepository;
  @Inject ProductRepository productRepository;

  public static class FulfillmentRequest {
    public Long storeId;
    public Long productId;
    public String warehouseBusinessUnitCode;
  }

  @GET
  public List<FulfillmentUnit> list(@QueryParam("storeId") Long storeId,
      @QueryParam("productId") Long productId) {
    if (storeId != null && productId != null) {
      return FulfillmentUnit.list("storeId = ?1 and productId = ?2", storeId, productId);
    } else if (storeId != null) {
      return FulfillmentUnit.list("storeId", storeId);
    } else if (productId != null) {
      return FulfillmentUnit.list("productId", productId);
    }
    return FulfillmentUnit.listAll();
  }

  @POST
  @Transactional
  public Response create(FulfillmentRequest request) {
    if (Store.findById(request.storeId) == null) {
      throw new WebApplicationException("Store not found: " + request.storeId, 404);
    }
    if (productRepository.findById(request.productId) == null) {
      throw new WebApplicationException("Product not found: " + request.productId, 404);
    }
    if (warehouseRepository.findWarehouseById(request.warehouseBusinessUnitCode) == null) {
      throw new WebApplicationException("Warehouse not found: " + request.warehouseBusinessUnitCode, 404);
    }

    // Check duplicate
    if (FulfillmentUnit.count("storeId = ?1 and productId = ?2 and warehouseBusinessUnitCode = ?3",
        request.storeId, request.productId, request.warehouseBusinessUnitCode) > 0) {
      throw new WebApplicationException("This warehouse already fulfills this product in this store.", 400);
    }

    // Constraint 1: each Product can be fulfilled by max 2 different Warehouses per Store
    List<FulfillmentUnit> forProductInStore = FulfillmentUnit.list(
        "storeId = ?1 and productId = ?2", request.storeId, request.productId);
    long distinctWarehousesForProduct = forProductInStore.stream()
        .map(f -> f.warehouseBusinessUnitCode).distinct().count();
    if (distinctWarehousesForProduct >= 2) {
      throw new WebApplicationException(
          "Product " + request.productId + " already has 2 fulfillment warehouses in store " + request.storeId + ".", 400);
    }

    // Constraint 2: each Store can be fulfilled by max 3 different Warehouses
    List<FulfillmentUnit> forStore = FulfillmentUnit.list("storeId = ?1", request.storeId);
    boolean warehouseIsNewForStore = forStore.stream()
        .noneMatch(f -> f.warehouseBusinessUnitCode.equals(request.warehouseBusinessUnitCode));
    if (warehouseIsNewForStore) {
      long distinctWarehousesInStore = forStore.stream()
          .map(f -> f.warehouseBusinessUnitCode).distinct().count();
      if (distinctWarehousesInStore >= 3) {
        throw new WebApplicationException(
            "Store " + request.storeId + " already has 3 fulfillment warehouses.", 400);
      }
    }

    // Constraint 3: each Warehouse can store max 5 types of Products
    List<FulfillmentUnit> forWarehouse = FulfillmentUnit.list(
        "warehouseBusinessUnitCode = ?1", request.warehouseBusinessUnitCode);
    boolean productIsNewForWarehouse = forWarehouse.stream()
        .noneMatch(f -> f.productId.equals(request.productId));
    if (productIsNewForWarehouse) {
      long distinctProductsInWarehouse = forWarehouse.stream()
          .map(f -> f.productId).distinct().count();
      if (distinctProductsInWarehouse >= 5) {
        throw new WebApplicationException(
            "Warehouse " + request.warehouseBusinessUnitCode + " already stores 5 product types.", 400);
      }
    }

    FulfillmentUnit unit = new FulfillmentUnit();
    unit.storeId = request.storeId;
    unit.productId = request.productId;
    unit.warehouseBusinessUnitCode = request.warehouseBusinessUnitCode;
    unit.persist();

    return Response.status(Response.Status.CREATED).entity(unit).build();
  }

  @DELETE
  @Path("/{id}")
  @Transactional
  public void delete(@PathParam("id") Long id) {
    FulfillmentUnit unit = FulfillmentUnit.findById(id);
    if (unit == null) {
      throw new WebApplicationException("Fulfillment unit not found: " + id, 404);
    }
    unit.delete();
  }
}

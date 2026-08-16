package com.orderflow.inventory.controller;

import com.orderflow.inventory.dto.ReleaseRequest;
import com.orderflow.inventory.dto.ReserveRequest;
import com.orderflow.inventory.entity.Product;
import com.orderflow.inventory.service.InsufficientStockException;
import com.orderflow.inventory.service.InventoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/products")
    public List<Product> listProducts() {
        return inventoryService.listProducts();
    }

    @PostMapping("/reserve")
    public ResponseEntity<Void> reserve(@RequestBody ReserveRequest request) {
        inventoryService.reserve(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/release")
    public ResponseEntity<Void> release(@RequestBody ReleaseRequest request) {
        inventoryService.release(request);
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<String> handleInsufficientStock(InsufficientStockException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }
}

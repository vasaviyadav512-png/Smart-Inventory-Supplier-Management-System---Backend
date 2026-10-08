package com.smartinventory.controller;

import com.smartinventory.dto.Requests.StockUpdateRequest;
import com.smartinventory.model.Product;
import com.smartinventory.repository.ProductRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductRepository repo;

    @GetMapping
    public List<Product> all() { return repo.findAll(); }

    @GetMapping("/low-stock")
    public List<Product> lowStock() { return repo.findLowStock(); }

    @GetMapping("/{id}")
    public Product one(@PathVariable Long id) { return find(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@Valid @RequestBody Product p) {
        p.setId(null);
        return repo.save(p);
    }

    @PutMapping("/{id}")
    public Product update(@PathVariable Long id, @Valid @RequestBody Product in) {
        Product p = find(id);
        p.setName(in.getName());
        p.setSku(in.getSku());
        p.setCategory(in.getCategory());
        p.setUnitPrice(in.getUnitPrice());
        p.setStockQuantity(in.getStockQuantity());
        p.setReorderLevel(in.getReorderLevel());
        p.setLeadTimeDays(in.getLeadTimeDays());
        return repo.save(p);
    }

    /** Staff + Admin: set the counted stock quantity. */
    @PutMapping("/{id}/stock")
    public Product updateStock(@PathVariable Long id, @Valid @RequestBody StockUpdateRequest req) {
        Product p = find(id);
        p.setStockQuantity(req.quantity());
        return repo.save(p);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) { repo.delete(find(id)); }

    private Product find(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }
}

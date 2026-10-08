package com.smartinventory.controller;

import com.smartinventory.dto.Requests.SupplierPriceRequest;
import com.smartinventory.model.*;
import com.smartinventory.repository.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierController {
    private final SupplierRepository repo;
    private final ProductRepository productRepo;
    private final SupplierProductRepository spRepo;

    @GetMapping
    public List<Supplier> all() { return repo.findAll(); }

    @GetMapping("/{id}")
    public Supplier one(@PathVariable Long id) { return find(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Supplier create(@Valid @RequestBody Supplier s) {
        s.setId(null);
        return repo.save(s);
    }

    @PutMapping("/{id}")
    public Supplier update(@PathVariable Long id, @Valid @RequestBody Supplier in) {
        Supplier s = find(id);
        s.setName(in.getName());
        s.setEmail(in.getEmail());
        s.setPhone(in.getPhone());
        s.setAddress(in.getAddress());
        return repo.save(s);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        Supplier s = find(id);
        spRepo.deleteAll(spRepo.findBySupplierId(id));
        repo.delete(s);
    }

    // ---- price list (supplier <-> product quotes) ----
    @GetMapping("/{id}/products")
    public List<SupplierProduct> prices(@PathVariable Long id) { return spRepo.findBySupplierId(id); }

    @PostMapping("/{id}/products")
    public SupplierProduct setPrice(@PathVariable Long id, @Valid @RequestBody SupplierPriceRequest req) {
        Supplier s = find(id);
        Product p = productRepo.findById(req.productId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        SupplierProduct sp = spRepo.findBySupplierIdAndProductId(id, p.getId()).orElseGet(SupplierProduct::new);
        sp.setSupplier(s);
        sp.setProduct(p);
        sp.setPrice(req.price());
        return spRepo.save(sp);
    }

    @DeleteMapping("/{id}/products/{productId}")
    public void removePrice(@PathVariable Long id, @PathVariable Long productId) {
        spRepo.findBySupplierIdAndProductId(id, productId).ifPresent(spRepo::delete);
    }

    private Supplier find(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found"));
    }
}

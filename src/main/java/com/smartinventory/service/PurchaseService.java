package com.smartinventory.service;

import com.smartinventory.dto.Requests.PurchaseRequest;
import com.smartinventory.model.*;
import com.smartinventory.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PurchaseService {
    private final PurchaseRepository purchaseRepo;
    private final SupplierRepository supplierRepo;
    private final ProductRepository productRepo;
    private final SupplierProductRepository spRepo;

    public List<Purchase> findAll() { return purchaseRepo.findAllByOrderByOrderDateDescIdDesc(); }

    @Transactional
    public Purchase create(PurchaseRequest req) {
        Supplier supplier = supplierRepo.findById(req.supplierId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found"));
        Product product = productRepo.findById(req.productId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        BigDecimal price = req.unitPrice();
        if (price == null) {
            price = spRepo.findBySupplierIdAndProductId(supplier.getId(), product.getId())
                    .map(SupplierProduct::getPrice)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "No quoted price for this supplier/product - enter a unit price"));
        }

        Purchase p = new Purchase();
        p.setSupplier(supplier);
        p.setProduct(product);
        p.setQuantity(req.quantity());
        p.setUnitPrice(price);
        p.setOrderDate(LocalDate.now());
        p.setExpectedDate(LocalDate.now().plusDays(product.getLeadTimeDays()));
        p.setStatus(PurchaseStatus.ORDERED);
        return purchaseRepo.save(p);
    }

    /** Goods receipt: marks the order received, records delivery date + quality, and adds the quantity to stock. */
    @Transactional
    public Purchase receive(Long id, Integer qualityRating) {
        Purchase p = purchaseRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Purchase not found"));
        if (p.getStatus() == PurchaseStatus.RECEIVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Purchase already received");
        }
        p.setStatus(PurchaseStatus.RECEIVED);
        p.setDeliveredDate(LocalDate.now());
        p.setQualityRating(qualityRating == null ? 4 : qualityRating);

        Product product = p.getProduct();
        product.setStockQuantity(product.getStockQuantity() + p.getQuantity());
        productRepo.save(product);
        return purchaseRepo.save(p);
    }
}

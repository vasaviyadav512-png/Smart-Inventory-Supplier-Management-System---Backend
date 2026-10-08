package com.smartinventory.service;

import com.smartinventory.dto.Requests.SaleRequest;
import com.smartinventory.model.Product;
import com.smartinventory.model.Sale;
import com.smartinventory.repository.ProductRepository;
import com.smartinventory.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SaleService {
    private final SaleRepository saleRepo;
    private final ProductRepository productRepo;

    public List<Sale> findAll() { return saleRepo.findAllByOrderBySaleDateDescIdDesc(); }

    @Transactional
    public Sale record(SaleRequest req, String username) {
        Product product = productRepo.findById(req.productId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        if (product.getStockQuantity() < req.quantity()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Insufficient stock: only " + product.getStockQuantity() + " units of " + product.getName() + " available");
        }
        product.setStockQuantity(product.getStockQuantity() - req.quantity());
        productRepo.save(product);

        Sale sale = new Sale();
        sale.setProduct(product);
        sale.setQuantity(req.quantity());
        sale.setUnitPrice(product.getUnitPrice());
        sale.setSaleDate(LocalDate.now());
        sale.setSoldBy(username);
        return saleRepo.save(sale);
    }
}

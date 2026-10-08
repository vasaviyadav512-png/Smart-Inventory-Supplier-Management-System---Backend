package com.smartinventory.service;

import com.smartinventory.dto.Responses.PriceComparison;
import com.smartinventory.dto.Responses.SupplierScore;
import com.smartinventory.model.*;
import com.smartinventory.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {
    private final ProductRepository productRepo;
    private final SupplierRepository supplierRepo;
    private final SaleRepository saleRepo;
    private final PurchaseRepository purchaseRepo;
    private final SupplierProductRepository spRepo;
    private final SupplierScoreService scoreService;

    public Map<String, Object> dashboard() {
        List<Product> products = productRepo.findAll();
        List<Sale> recent = saleRepo.findBySaleDateGreaterThanEqual(LocalDate.now().minusDays(30));

        BigDecimal stockValue = products.stream()
                .map(p -> p.getUnitPrice().multiply(BigDecimal.valueOf(p.getStockQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal revenue = recent.stream()
                .map(s -> s.getUnitPrice().multiply(BigDecimal.valueOf(s.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Integer> byProduct = new HashMap<>();
        recent.forEach(s -> byProduct.merge(s.getProduct().getName(), s.getQuantity(), Integer::sum));
        List<Map<String, Object>> top = byProduct.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(5)
                .map(e -> Map.<String, Object>of("name", e.getKey(), "units", e.getValue()))
                .collect(Collectors.toList());

        Map<LocalDate, Integer> byDay = new TreeMap<>();
        for (int i = 13; i >= 0; i--) byDay.put(LocalDate.now().minusDays(i), 0);
        recent.forEach(s -> byDay.computeIfPresent(s.getSaleDate(), (d, v) -> v + s.getQuantity()));
        List<Map<String, Object>> daily = byDay.entrySet().stream()
                .map(e -> Map.<String, Object>of("date", e.getKey().toString(), "units", e.getValue()))
                .collect(Collectors.toList());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalProducts", products.size());
        out.put("totalSuppliers", supplierRepo.count());
        out.put("lowStockCount", productRepo.findLowStock().size());
        out.put("outOfStockCount", products.stream().filter(p -> p.getStockQuantity() == 0).count());
        out.put("stockValue", stockValue);
        out.put("revenueLast30Days", revenue);
        out.put("unitsSoldLast30Days", recent.stream().mapToInt(Sale::getQuantity).sum());
        out.put("pendingPurchases", purchaseRepo.findByStatus(PurchaseStatus.ORDERED).size());
        out.put("lowStockItems", productRepo.findLowStock());
        out.put("topProducts", top);
        out.put("dailySales", daily);
        return out;
    }

    public List<PriceComparison> priceComparison(Long productId) {
        Map<Long, Double> scores = scoreService.scoreAll().stream()
                .collect(Collectors.toMap(SupplierScore::supplierId, SupplierScore::overallScore));
        List<SupplierProduct> offers = spRepo.findByProductId(productId);
        BigDecimal min = offers.stream().map(SupplierProduct::getPrice).min(Comparator.naturalOrder()).orElse(null);
        return offers.stream()
                .sorted(Comparator.comparing(SupplierProduct::getPrice))
                .map(o -> new PriceComparison(o.getSupplier().getId(), o.getSupplier().getName(), o.getPrice(),
                        o.getPrice().compareTo(min) == 0, scores.getOrDefault(o.getSupplier().getId(), 50.0)))
                .toList();
    }
}

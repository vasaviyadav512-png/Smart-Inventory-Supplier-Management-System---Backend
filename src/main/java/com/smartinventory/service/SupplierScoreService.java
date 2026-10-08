package com.smartinventory.service;

import com.smartinventory.dto.Responses.SupplierScore;
import com.smartinventory.model.*;
import com.smartinventory.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * Supplier performance scoring (0-100):
 *   40% delivery time  (share of received orders delivered on/before the expected date)
 *   30% quality        (average 1-5 rating given at goods receipt, scaled to 100)
 *   30% price          (how close the supplier's purchase prices are to the cheapest quote for the same product)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupplierScoreService {
    private final SupplierRepository supplierRepo;
    private final PurchaseRepository purchaseRepo;
    private final SupplierProductRepository spRepo;

    public List<SupplierScore> scoreAll() {
        return supplierRepo.findAll().stream()
                .map(this::score)
                .sorted(Comparator.comparingDouble(SupplierScore::overallScore).reversed())
                .toList();
    }

    public SupplierScore score(Supplier s) {
        List<Purchase> done = purchaseRepo.findBySupplierIdAndStatus(s.getId(), PurchaseStatus.RECEIVED);
        if (done.isEmpty()) {
            return new SupplierScore(s.getId(), s.getName(), 0, 0, 0, 0, 50.0, "No history");
        }
        long onTime = done.stream()
                .filter(p -> p.getDeliveredDate() != null && p.getExpectedDate() != null
                        && !p.getDeliveredDate().isAfter(p.getExpectedDate()))
                .count();
        double onTimeRate = 100.0 * onTime / done.size();

        double avgQuality = done.stream().filter(p -> p.getQualityRating() != null)
                .mapToInt(Purchase::getQualityRating).average().orElse(3.0);

        double priceScore = done.stream().mapToDouble(p -> {
            if (p.getUnitPrice() == null || p.getUnitPrice().signum() == 0) return 1.0;
            BigDecimal min = spRepo.findByProductId(p.getProduct().getId()).stream()
                    .map(SupplierProduct::getPrice).min(Comparator.naturalOrder()).orElse(p.getUnitPrice());
            return Math.min(1.0, min.doubleValue() / p.getUnitPrice().doubleValue());
        }).average().orElse(1.0) * 100;

        double overall = 0.4 * onTimeRate + 0.3 * (avgQuality / 5.0 * 100) + 0.3 * priceScore;
        String grade = overall >= 85 ? "Excellent" : overall >= 70 ? "Good" : overall >= 50 ? "Average" : "Poor";
        return new SupplierScore(s.getId(), s.getName(), done.size(), r1(onTimeRate), r1(avgQuality),
                r1(priceScore), r1(overall), grade);
    }

    private static double r1(double v) { return Math.round(v * 10.0) / 10.0; }
}

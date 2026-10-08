package com.smartinventory.service;

import com.smartinventory.dto.Responses.*;
import com.smartinventory.model.*;
import com.smartinventory.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Automated reorder engine. For each product:
 *   reorder point = avgDailyDemand * leadTime + safetyStock   (safety = demand over half the lead time)
 *   reorder when stock <= max(reorder point, reorderLevel)
 *   suggested qty = forecast demand for the next 30 days + safety stock - current stock
 *   reorder-by date = today + (days of stock left - lead time)
 * The best supplier is the one with the highest blend of performance score (60%) and price competitiveness (40%).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReorderService {
    private static final int HORIZON_DAYS = 30;

    private final ProductRepository productRepo;
    private final SupplierProductRepository spRepo;
    private final DemandService demandService;
    private final SupplierScoreService scoreService;

    public List<ReorderSuggestion> suggestions() {
        Map<Long, SupplierScore> scores = new HashMap<>();
        scoreService.scoreAll().forEach(s -> scores.put(s.supplierId(), s));

        List<ReorderSuggestion> result = new ArrayList<>();
        for (Product p : productRepo.findAll()) {
            DemandForecast f = demandService.forecast(p, HORIZON_DAYS);
            double avg = f.avgDailyDemand();
            int lead = p.getLeadTimeDays();
            double safety = avg * Math.ceil(lead / 2.0);
            double reorderPoint = avg * lead + safety;
            int stock = p.getStockQuantity();

            boolean belowLevel = stock <= p.getReorderLevel();
            boolean belowPoint = avg > 0 && stock <= reorderPoint;
            if (!belowLevel && !belowPoint) continue;

            Double daysLeft = avg > 0 ? Math.round(stock / avg * 10.0) / 10.0 : null;
            int qty = (int) Math.ceil(f.forecastQuantity() + safety - stock);
            qty = Math.max(qty, Math.max(1, p.getReorderLevel() * 2 - stock));

            LocalDate reorderBy = LocalDate.now();
            if (daysLeft != null) {
                reorderBy = LocalDate.now().plusDays(Math.max(0, (long) Math.floor(daysLeft - lead)));
            }

            String urgency;
            if (stock == 0 || (daysLeft != null && daysLeft <= lead)) urgency = "CRITICAL";
            else if (belowLevel) urgency = "HIGH";
            else urgency = "MEDIUM";

            String reason = belowLevel
                    ? "Stock (" + stock + ") is at or below the reorder level (" + p.getReorderLevel() + ")"
                    : "Stock will not last through the " + lead + "-day lead time at current demand";

            // pick best supplier
            List<SupplierProduct> offers = spRepo.findByProductId(p.getId());
            BigDecimal minPrice = offers.stream().map(SupplierProduct::getPrice).min(Comparator.naturalOrder()).orElse(null);
            SupplierProduct best = null;
            double bestBlend = -1;
            for (SupplierProduct sp : offers) {
                double perf = scores.containsKey(sp.getSupplier().getId())
                        ? scores.get(sp.getSupplier().getId()).overallScore() : 50.0;
                double priceScore = sp.getPrice().signum() == 0 ? 100 : minPrice.doubleValue() / sp.getPrice().doubleValue() * 100;
                double blend = 0.6 * perf + 0.4 * priceScore;
                if (blend > bestBlend) { bestBlend = blend; best = sp; }
            }

            Long supId = null; String supName = null; BigDecimal supPrice = null; Double supScore = null; BigDecimal cost = null;
            if (best != null) {
                supId = best.getSupplier().getId();
                supName = best.getSupplier().getName();
                supPrice = best.getPrice();
                supScore = scores.containsKey(supId) ? scores.get(supId).overallScore() : 50.0;
                cost = supPrice.multiply(BigDecimal.valueOf(qty));
            }

            result.add(new ReorderSuggestion(p.getId(), p.getName(), p.getSku(), stock, p.getReorderLevel(), avg,
                    daysLeft, qty, reorderBy, urgency, supId, supName, supPrice, supScore, cost, reason));
        }
        List<String> order = List.of("CRITICAL", "HIGH", "MEDIUM");
        result.sort(Comparator.comparingInt((ReorderSuggestion s) -> order.indexOf(s.urgency()))
                .thenComparing(ReorderSuggestion::reorderBy));
        return result;
    }
}

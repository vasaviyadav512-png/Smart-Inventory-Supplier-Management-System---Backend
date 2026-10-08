package com.smartinventory.controller;

import com.smartinventory.dto.Responses.*;
import com.smartinventory.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {
    private final AnalyticsService analytics;
    private final DemandService demand;
    private final ReorderService reorder;
    private final SupplierScoreService scores;

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() { return analytics.dashboard(); }

    @GetMapping("/demand")
    public List<DemandForecast> demand(@RequestParam(defaultValue = "30") int days) {
        return demand.forecastAll(Math.max(1, Math.min(days, 180)));
    }

    @GetMapping("/demand/{productId}")
    public DemandForecast demandFor(@PathVariable Long productId, @RequestParam(defaultValue = "30") int days) {
        return demand.forecastFor(productId, Math.max(1, Math.min(days, 180)));
    }

    @GetMapping("/reorder-suggestions")
    public List<ReorderSuggestion> reorderSuggestions() { return reorder.suggestions(); }

    @GetMapping("/supplier-scores")
    public List<SupplierScore> supplierScores() { return scores.scoreAll(); }

    @GetMapping("/price-comparison/{productId}")
    public List<PriceComparison> priceComparison(@PathVariable Long productId) {
        return analytics.priceComparison(productId);
    }
}

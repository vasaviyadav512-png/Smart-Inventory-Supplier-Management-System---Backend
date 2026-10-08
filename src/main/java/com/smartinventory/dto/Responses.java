package com.smartinventory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Response payloads for the analytics / intelligence features. */
public final class Responses {
    private Responses() {}

    public record LoginResponse(String token, String username, String fullName, String role) {}

    public record DemandForecast(Long productId, String productName, String sku, int currentStock,
                                 double avgDailyDemand, int forecastQuantity, int horizonDays,
                                 String trend, List<Integer> weeklySales) {}

    public record ReorderSuggestion(Long productId, String productName, String sku, int currentStock,
                                    int reorderLevel, double avgDailyDemand, Double daysUntilStockout,
                                    int suggestedQuantity, LocalDate reorderBy, String urgency,
                                    Long supplierId, String supplierName, BigDecimal supplierPrice,
                                    Double supplierScore, BigDecimal estimatedCost, String reason) {}

    public record SupplierScore(Long supplierId, String supplierName, int deliveredOrders,
                                double onTimeRate, double avgQuality, double priceCompetitiveness,
                                double overallScore, String grade) {}

    public record PriceComparison(Long supplierId, String supplierName, BigDecimal price,
                                  boolean cheapest, double supplierScore) {}
}

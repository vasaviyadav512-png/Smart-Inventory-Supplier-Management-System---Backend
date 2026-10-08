package com.smartinventory.service;

import com.smartinventory.dto.Responses.DemandForecast;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Demand prediction using a weighted moving average over the last 4 weeks of sales.
 * Recent weeks carry more weight (1,2,3,4) so the forecast reacts to rising/falling demand.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DemandService {
    private static final int WEEKS = 4;

    private final ProductRepository productRepo;
    private final SaleRepository saleRepo;

    public List<DemandForecast> forecastAll(int horizonDays) {
        List<DemandForecast> out = new ArrayList<>();
        for (Product p : productRepo.findAll()) out.add(forecast(p, horizonDays));
        return out;
    }

    public DemandForecast forecastFor(Long productId, int horizonDays) {
        Product p = productRepo.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        return forecast(p, horizonDays);
    }

    public DemandForecast forecast(Product p, int horizonDays) {
        LocalDate today = LocalDate.now();
        List<Sale> sales = saleRepo.findByProductIdAndSaleDateGreaterThanEqual(p.getId(), today.minusDays(WEEKS * 7L));

        int[] weekly = new int[WEEKS]; // index 0 = oldest week, WEEKS-1 = most recent week
        for (Sale s : sales) {
            long daysAgo = Math.max(0, ChronoUnit.DAYS.between(s.getSaleDate(), today));
            int bucket = WEEKS - 1 - (int) Math.min(WEEKS - 1, daysAgo / 7);
            weekly[bucket] += s.getQuantity();
        }

        double weightedSum = 0, weightTotal = 0;
        for (int i = 0; i < WEEKS; i++) {
            weightedSum += weekly[i] * (i + 1);
            weightTotal += (i + 1);
        }
        double avgDaily = (weightedSum / weightTotal) / 7.0;
        int forecastQty = (int) Math.ceil(avgDaily * horizonDays);

        double early = weekly[0] + weekly[1], late = weekly[2] + weekly[3];
        String trend = "STABLE";
        if (early == 0 && late > 0) trend = "UP";
        else if (early > 0 && late > early * 1.1) trend = "UP";
        else if (early > 0 && late < early * 0.9) trend = "DOWN";

        List<Integer> weeks = new ArrayList<>();
        for (int w : weekly) weeks.add(w);

        return new DemandForecast(p.getId(), p.getName(), p.getSku(), p.getStockQuantity(),
                Math.round(avgDaily * 100.0) / 100.0, forecastQty, horizonDays, trend, weeks);
    }
}

package com.smartinventory.config;

import com.smartinventory.model.*;
import com.smartinventory.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Creates demo users, products, suppliers, quotes, purchase history and 60 days of sales on first run. */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private final UserRepository userRepo;
    private final ProductRepository productRepo;
    private final SupplierRepository supplierRepo;
    private final SupplierProductRepository spRepo;
    private final PurchaseRepository purchaseRepo;
    private final SaleRepository saleRepo;
    private final PasswordEncoder encoder;

    @Value("${app.seed-demo-data:true}")
    private boolean seed;

    private record Item(String name, String sku, String category, int price, int stock, int reorderLevel,
                        int lead, double baseDaily, double trend) {}

    @Override
    public void run(String... args) {
        if (userRepo.count() == 0) {
            userRepo.save(new User(null, "admin", encoder.encode("admin123"), "System Admin", Role.ADMIN));
            userRepo.save(new User(null, "staff", encoder.encode("staff123"), "Store Staff", Role.STAFF));
        }
        if (!seed || productRepo.count() > 0) return;

        Random rnd = new Random(42);
        LocalDate today = LocalDate.now();

        List<Item> items = List.of(
            new Item("Wireless Mouse",       "ELEC-001", "Electronics", 799,  14, 20, 5, 4.0,  0.6),
            new Item("Mechanical Keyboard",  "ELEC-002", "Electronics", 2499, 60, 15, 7, 1.5,  0.0),
            new Item("USB-C Cable (1m)",     "ELEC-003", "Electronics", 299,  25, 40, 4, 9.0,  0.3),
            new Item("Laptop Stand",         "ACC-001",  "Accessories", 1299, 45, 10, 6, 1.2, -0.2),
            new Item("A4 Notebook (200pg)",  "STA-001",  "Stationery",  120,  8,  50, 3, 14.0, 0.4),
            new Item("Gel Pen (Pack of 10)", "STA-002",  "Stationery",  150,  220, 60, 3, 6.0, 0.0),
            new Item("Desk Lamp (LED)",      "HOME-001", "Home",        899,  30, 12, 8, 1.0, -0.4),
            new Item("Water Bottle 1L",      "HOME-002", "Home",        349,  0,  25, 5, 5.0,  0.5),
            new Item("Bluetooth Speaker",    "ELEC-004", "Electronics", 1999, 12, 10, 9, 1.4,  0.2),
            new Item("Backpack (Laptop)",    "ACC-002",  "Accessories", 1599, 75, 12, 7, 1.1,  0.0));

        List<Product> products = new ArrayList<>();
        for (Item i : items) {
            Product p = new Product(null, i.name(), i.sku(), i.category(), BigDecimal.valueOf(i.price()),
                    i.stock(), i.reorderLevel(), i.lead());
            products.add(productRepo.save(p));
        }

        // name, email, phone, address, on-time probability, avg quality (1-5), price factor
        Object[][] sup = {
            {"TechSource Distributors", "sales@techsource.example", "+91 90000 11111", "Hyderabad", 0.92, 4.6, 1.00},
            {"BudgetBulk Traders",      "orders@budgetbulk.example", "+91 90000 22222", "Vijayawada", 0.55, 3.2, 0.88},
            {"PrimeSupply Co.",         "hello@primesupply.example", "+91 90000 33333", "Chennai",    0.80, 4.2, 0.96},
            {"QuickMart Wholesale",     "team@quickmart.example",    "+91 90000 44444", "Bengaluru",  0.70, 3.8, 1.04}};

        List<Supplier> suppliers = new ArrayList<>();
        List<List<SupplierProduct>> offersBySupplier = new ArrayList<>();
        for (int s = 0; s < sup.length; s++) {
            Supplier supplier = supplierRepo.save(new Supplier(null, (String) sup[s][0], (String) sup[s][1],
                    (String) sup[s][2], (String) sup[s][3]));
            suppliers.add(supplier);
            List<SupplierProduct> offers = new ArrayList<>();
            for (int pi = 0; pi < products.size(); pi++) {
                boolean guaranteed = pi % sup.length == s;       // every product has at least one supplier
                if (!guaranteed && rnd.nextDouble() < 0.25) continue;
                Product p = products.get(pi);
                double cost = p.getUnitPrice().doubleValue() * 0.6 * (double) sup[s][6] * (0.95 + rnd.nextDouble() * 0.10);
                offers.add(spRepo.save(new SupplierProduct(null, supplier, p,
                        BigDecimal.valueOf(cost).setScale(2, RoundingMode.HALF_UP))));
            }
            offersBySupplier.add(offers);
        }

        // Received purchase history (drives supplier scoring)
        for (int s = 0; s < suppliers.size(); s++) {
            List<SupplierProduct> offers = offersBySupplier.get(s);
            for (int k = 0; k < 8; k++) {
                SupplierProduct sp = offers.get(rnd.nextInt(offers.size()));
                LocalDate order = today.minusDays(15 + rnd.nextInt(70));
                LocalDate expected = order.plusDays(sp.getProduct().getLeadTimeDays());
                boolean onTime = rnd.nextDouble() < (double) sup[s][4];
                LocalDate delivered = onTime ? expected.minusDays(rnd.nextInt(2)) : expected.plusDays(1 + rnd.nextInt(5));
                int rating = (int) Math.max(1, Math.min(5, Math.round((double) sup[s][5] + rnd.nextGaussian() * 0.7)));
                Purchase p = new Purchase(null, suppliers.get(s), sp.getProduct(), 20 + rnd.nextInt(40),
                        sp.getPrice().multiply(BigDecimal.valueOf(0.97 + rnd.nextDouble() * 0.08)).setScale(2, RoundingMode.HALF_UP),
                        order, expected, delivered, rating, PurchaseStatus.RECEIVED);
                purchaseRepo.save(p);
            }
        }
        // Two pending orders
        for (int k = 0; k < 2; k++) {
            SupplierProduct sp = offersBySupplier.get(k).get(0);
            purchaseRepo.save(new Purchase(null, suppliers.get(k), sp.getProduct(), 40, sp.getPrice(),
                    today.minusDays(2), today.minusDays(2).plusDays(sp.getProduct().getLeadTimeDays()),
                    null, null, PurchaseStatus.ORDERED));
        }

        // 60 days of sales history with per-product trends
        List<Sale> sales = new ArrayList<>();
        for (int d = 60; d >= 1; d--) {
            for (int pi = 0; pi < products.size(); pi++) {
                Item it = items.get(pi);
                double progress = (60 - d) / 60.0;                 // 0 (oldest) -> 1 (recent)
                double mean = it.baseDaily() * (1 + it.trend() * progress);
                int qty = (int) Math.max(0, Math.round(mean + rnd.nextGaussian() * mean * 0.4));
                if (qty == 0) continue;
                sales.add(new Sale(null, products.get(pi), qty, products.get(pi).getUnitPrice(),
                        today.minusDays(d), "staff"));
            }
        }
        saleRepo.saveAll(sales);
        System.out.println(">>> Demo data loaded. Logins: admin/admin123  |  staff/staff123");
    }
}

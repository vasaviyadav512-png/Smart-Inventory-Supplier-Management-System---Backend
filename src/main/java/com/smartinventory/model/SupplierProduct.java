package com.smartinventory.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/** The price a given supplier quotes for a given product (basis for price comparison). */
@Entity
@Table(name = "supplier_products", uniqueConstraints = @UniqueConstraint(columnNames = {"supplier_id", "product_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class SupplierProduct {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false) @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @ManyToOne(optional = false) @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private BigDecimal price;
}

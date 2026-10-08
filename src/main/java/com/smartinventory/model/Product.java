package com.smartinventory.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    @NotBlank
    @Column(unique = true, nullable = false)
    private String sku;

    private String category;

    /** Selling price per unit. */
    @NotNull @DecimalMin("0.0")
    private BigDecimal unitPrice;

    @Min(0)
    private int stockQuantity;

    /** Low-stock alert threshold. */
    @Min(0)
    private int reorderLevel;

    /** Typical supplier lead time in days (used by the reorder engine). */
    @Min(1)
    private int leadTimeDays = 7;
}

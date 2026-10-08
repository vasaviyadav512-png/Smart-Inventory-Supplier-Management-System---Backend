package com.smartinventory.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "purchases")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Purchase {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false) private Supplier supplier;
    @ManyToOne(optional = false) private Product product;

    private int quantity;
    private BigDecimal unitPrice;

    private LocalDate orderDate;
    private LocalDate expectedDate;
    private LocalDate deliveredDate;

    /** 1-5 quality rating given when the order is received. */
    private Integer qualityRating;

    @Enumerated(EnumType.STRING)
    private PurchaseStatus status = PurchaseStatus.ORDERED;
}

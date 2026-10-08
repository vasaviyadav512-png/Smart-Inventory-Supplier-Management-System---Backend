package com.smartinventory.dto;

import com.smartinventory.model.Role;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/** Request payloads grouped in one file for brevity. */
public final class Requests {
    private Requests() {}

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    public record UserRequest(@NotBlank String username, @NotBlank @Size(min = 6) String password,
                              String fullName, @NotNull Role role) {}

    public record StockUpdateRequest(@Min(0) int quantity) {}

    public record PurchaseRequest(@NotNull Long supplierId, @NotNull Long productId, @Min(1) int quantity,
                                  BigDecimal unitPrice) {}

    public record ReceiveRequest(@Min(1) @Max(5) Integer qualityRating) {}

    public record SaleRequest(@NotNull Long productId, @Min(1) int quantity) {}

    public record SupplierPriceRequest(@NotNull Long productId, @NotNull @DecimalMin("0.0") BigDecimal price) {}
}

package com.smartinventory.repository;

import com.smartinventory.model.Purchase;
import com.smartinventory.model.PurchaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    List<Purchase> findAllByOrderByOrderDateDescIdDesc();
    List<Purchase> findBySupplierIdAndStatus(Long supplierId, PurchaseStatus status);
    List<Purchase> findByStatus(PurchaseStatus status);
}

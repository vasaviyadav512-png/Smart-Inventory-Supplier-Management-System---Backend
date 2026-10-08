package com.smartinventory.controller;

import com.smartinventory.dto.Requests.PurchaseRequest;
import com.smartinventory.dto.Requests.ReceiveRequest;
import com.smartinventory.model.Purchase;
import com.smartinventory.service.PurchaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchases")
@RequiredArgsConstructor
public class PurchaseController {
    private final PurchaseService service;

    @GetMapping
    public List<Purchase> all() { return service.findAll(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Purchase create(@Valid @RequestBody PurchaseRequest req) { return service.create(req); }

    @PutMapping("/{id}/receive")
    public Purchase receive(@PathVariable Long id, @Valid @RequestBody(required = false) ReceiveRequest req) {
        return service.receive(id, req == null ? null : req.qualityRating());
    }
}

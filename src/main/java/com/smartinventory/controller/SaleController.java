package com.smartinventory.controller;

import com.smartinventory.dto.Requests.SaleRequest;
import com.smartinventory.model.Sale;
import com.smartinventory.service.SaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SaleController {
    private final SaleService service;

    @GetMapping
    public List<Sale> all() { return service.findAll(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Sale record(@Valid @RequestBody SaleRequest req, Authentication auth) {
        return service.record(req, auth.getName());
    }
}

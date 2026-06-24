package com.shipmate.api.customer;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    @PostMapping
    CustomerResponse upsert(@Valid @RequestBody UpsertCustomerRequest request) {
        return service.upsert(request);
    }

    @GetMapping
    List<CustomerResponse> recent(@RequestParam(required = false) Integer limit) {
        return service.recent(limit);
    }

    @GetMapping("/{customerId}")
    CustomerResponse get(@PathVariable String customerId) {
        return service.get(customerId);
    }

    @DeleteMapping("/{customerId}")
    CustomerResponse delete(@PathVariable String customerId) {
        return service.delete(customerId);
    }
}

package com.shipmate.api.customer;

import com.shipmate.api.auth.RequestAuthContext;
import com.shipmate.api.common.ApiRequestException;
import com.shipmate.api.operation.audit.AuditLogService;
import com.shipmate.api.orderdraft.OrderDraftItemCandidate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CustomerService {
    private static final int DEFAULT_RECENT_LIMIT = 100;
    private static final int MAX_RECENT_LIMIT = 500;

    private final CustomerRepository repository;
    private final RequestAuthContext requestAuthContext;
    private final AuditLogService auditLogService;

    public CustomerService(
            CustomerRepository repository,
            RequestAuthContext requestAuthContext,
            AuditLogService auditLogService
    ) {
        this.repository = repository;
        this.requestAuthContext = requestAuthContext;
        this.auditLogService = auditLogService;
    }

    public CustomerResponse upsert(UpsertCustomerRequest request) {
        OffsetDateTime now = OffsetDateTime.now();
        Customer existing = request.customerId() == null || request.customerId().isBlank()
                ? null
                : repository.findById(request.customerId()).orElse(null);
        String customerId = existing == null
                ? valueOrNewId(request.customerId())
                : existing.customerId();

        Customer customer = new Customer(
                customerId,
                requestAuthContext.currentMerchantIdOr(request.merchantId().strip()),
                request.customerName().strip(),
                blankToNull(request.customerPhone()),
                blankToNull(request.defaultRecipientName()),
                blankToNull(request.defaultRecipientPhone()),
                blankToNull(request.defaultPostalCode()),
                blankToNull(request.defaultAddress()),
                blankToNull(request.defaultAddressDetail()),
                normalizeFavoriteItems(request.favoriteItemNames()),
                normalizePriceItems(request.priceItems()),
                null,
                existing == null ? now : existing.createdAt(),
                now
        );
        repository.save(customer);
        auditLogService.appendForCurrentActor(
                customer.merchantId(),
                null,
                existing == null ? "CUSTOMER_CREATED" : "CUSTOMER_UPDATED",
                "CUSTOMER",
                customer.customerId(),
                "고객/단가표 정보가 저장되었습니다."
        );
        return CustomerResponse.from(customer);
    }

    public List<CustomerResponse> recent(Integer limit) {
        int normalizedLimit = normalizeLimit(limit);
        return repository.findRecent(normalizedLimit).stream()
                .filter(this::canRead)
                .filter(customer -> customer.deletedAt() == null)
                .map(CustomerResponse::from)
                .toList();
    }

    public Optional<Customer> findModel(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            return Optional.empty();
        }
        return repository.findById(customerId)
                .filter(this::canRead);
    }

    public BigDecimal matchUnitPrice(String customerId, OrderDraftItemCandidate itemCandidate) {
        return findModel(customerId)
                .flatMap(customer -> customer.priceItems().stream()
                        .filter(priceItem -> matches(priceItem, itemCandidate))
                        .map(CustomerPriceItem::unitPrice)
                        .findFirst())
                .orElse(null);
    }

    public CustomerResponse get(String customerId) {
        return repository.findById(customerId)
                .filter(this::canRead)
                .filter(customer -> customer.deletedAt() == null)
                .map(CustomerResponse::from)
                .orElseThrow(() -> new ApiRequestException(
                        "SM-CUS-7001",
                        HttpStatus.NOT_FOUND,
                        "고객을 찾을 수 없습니다."
                ));
    }

    public CustomerResponse delete(String customerId) {
        Customer customer = repository.findById(customerId)
                .filter(this::canRead)
                .filter(savedCustomer -> savedCustomer.deletedAt() == null)
                .orElseThrow(() -> new ApiRequestException(
                        "SM-CUS-7001",
                        HttpStatus.NOT_FOUND,
                        "고객을 찾을 수 없습니다."
                ));
        Customer deleted = customer.markDeleted(OffsetDateTime.now());
        repository.save(deleted);
        auditLogService.appendForCurrentActor(
                deleted.merchantId(),
                null,
                "CUSTOMER_DELETED",
                "CUSTOMER",
                deleted.customerId(),
                "고객 정보가 보관 처리되었습니다."
        );
        return CustomerResponse.from(deleted);
    }

    private boolean matches(CustomerPriceItem priceItem, OrderDraftItemCandidate itemCandidate) {
        if (priceItem.unitPrice() == null) {
            return false;
        }
        String itemName = valueOrBlank(itemCandidate.itemName());
        String itemSpec = valueOrBlank(itemCandidate.itemSpec());
        String keyword = valueOrBlank(priceItem.itemNameKeyword());
        String specKeyword = valueOrBlank(priceItem.itemSpecKeyword());
        boolean nameMatches = !keyword.isBlank() && itemName.contains(keyword);
        boolean specMatches = specKeyword.isBlank() || itemSpec.contains(specKeyword);
        return nameMatches && specMatches;
    }

    private List<String> normalizeFavoriteItems(List<String> favoriteItemNames) {
        if (favoriteItemNames == null) {
            return List.of();
        }
        return favoriteItemNames.stream()
                .map(this::blankToNull)
                .filter(value -> value != null)
                .toList();
    }

    private List<CustomerPriceItem> normalizePriceItems(List<CustomerPriceItem> priceItems) {
        if (priceItems == null) {
            return List.of();
        }
        return priceItems.stream()
                .filter(priceItem -> priceItem.unitPrice() != null && !valueOrBlank(priceItem.itemNameKeyword()).isBlank())
                .map(priceItem -> new CustomerPriceItem(
                        valueOrNewId(priceItem.customerPriceItemId()),
                        blankToNull(priceItem.itemNameKeyword()),
                        blankToNull(priceItem.itemSpecKeyword()),
                        blankToNull(priceItem.itemUnit()),
                        priceItem.unitPrice()
                ))
                .toList();
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_RECENT_LIMIT;
        }
        return Math.min(limit, MAX_RECENT_LIMIT);
    }

    private String valueOrNewId(String value) {
        if (value != null && !value.isBlank()) {
            return value.strip();
        }
        return "cust_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private String valueOrBlank(String value) {
        return value == null ? "" : value;
    }

    private boolean canRead(Customer customer) {
        return requestAuthContext.currentUser()
                .map(user -> user.merchantId().equals(customer.merchantId()))
                .orElse(true);
    }
}

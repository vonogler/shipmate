package com.shipmate.api.shippingorder;

import com.shipmate.api.orderdraft.OrderDraftResponse;
import com.shipmate.api.orderdraft.OrderDraftService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/shipping-orders")
public class ShippingOrderController {
    private final OrderDraftService orderDraftService;
    private final CourierCsvService courierCsvService;

    public ShippingOrderController(OrderDraftService orderDraftService, CourierCsvService courierCsvService) {
        this.orderDraftService = orderDraftService;
        this.courierCsvService = courierCsvService;
    }

    @GetMapping
    List<OrderDraftResponse> submitted(@RequestParam(required = false) Integer limit) {
        return orderDraftService.submitted(limit);
    }

    @GetMapping("/{orderDraftId}")
    OrderDraftResponse get(@PathVariable String orderDraftId) {
        return orderDraftService.getSubmitted(orderDraftId);
    }

    @PostMapping("/{orderDraftId}/reopen-review")
    OrderDraftResponse reopenReview(
            @PathVariable String orderDraftId,
            @RequestBody(required = false) ReopenShippingOrderReviewRequest request
    ) {
        return orderDraftService.reopenSubmitted(
                orderDraftId,
                request == null ? null : request.reopenReason()
        );
    }

    @GetMapping("/{orderDraftId}/courier-csv")
    ResponseEntity<String> courierCsv(
            @PathVariable String orderDraftId,
            @RequestParam(required = false, defaultValue = "GENERIC") CourierCsvTemplate courierCsvTemplate
    ) {
        OrderDraftResponse shippingOrder = orderDraftService.getSubmitted(orderDraftId);
        CourierCsvResponse csv = courierCsvService.createSingleOrderCsv(shippingOrder, courierCsvTemplate);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv"))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(csv.fileName())
                                .build()
                                .toString()
                )
                .body(csv.content());
    }

    @PostMapping("/courier-csv")
    ResponseEntity<String> bulkCourierCsv(@RequestBody BulkCourierCsvRequest request) {
        List<OrderDraftResponse> shippingOrders = request.orderDraftIds().stream()
                .map(orderDraftService::getSubmitted)
                .toList();
        CourierCsvResponse csv = courierCsvService.createBulkOrderCsv(shippingOrders, request.courierCsvTemplate());
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv"))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(csv.fileName())
                                .build()
                                .toString()
                )
                .body(csv.content());
    }

    @PostMapping("/courier-csv/validate")
    CourierCsvValidationResponse validateBulkCourierCsv(@RequestBody BulkCourierCsvRequest request) {
        List<OrderDraftResponse> shippingOrders = request.orderDraftIds().stream()
                .map(orderDraftService::getSubmitted)
                .toList();
        return courierCsvService.validate(shippingOrders, request.courierCsvTemplate());
    }
}

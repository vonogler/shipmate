package com.shipmate.api.orderdraft;

import com.shipmate.api.inboundmessage.BusinessChannel;
import com.shipmate.api.inboundmessage.SourceChannel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Repository
public class OrderDraftRepository {
    private final JdbcTemplate jdbcTemplate;

    public OrderDraftRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public OrderDraft save(OrderDraft orderDraft) {
        jdbcTemplate.update("""
                        insert into tb_order_drafts (
                            order_draft_id, source_inbound_message_id, merchant_id, merchant_staff_id,
                            customer_id, source_channel, business_channel, raw_text,
                            delivery_recipient_name, delivery_recipient_phone_number, delivery_postal_code,
                            delivery_address, delivery_address_detail, delivery_request_memo,
                            delivery_memo, courier_memo, order_draft_status, created_at, updated_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        on duplicate key update
                            source_inbound_message_id = values(source_inbound_message_id),
                            merchant_id = values(merchant_id),
                            merchant_staff_id = values(merchant_staff_id),
                            customer_id = values(customer_id),
                            source_channel = values(source_channel),
                            business_channel = values(business_channel),
                            raw_text = values(raw_text),
                            delivery_recipient_name = values(delivery_recipient_name),
                            delivery_recipient_phone_number = values(delivery_recipient_phone_number),
                            delivery_postal_code = values(delivery_postal_code),
                            delivery_address = values(delivery_address),
                            delivery_address_detail = values(delivery_address_detail),
                            delivery_request_memo = values(delivery_request_memo),
                            delivery_memo = values(delivery_memo),
                            courier_memo = values(courier_memo),
                            order_draft_status = values(order_draft_status),
                            updated_at = values(updated_at)
                        """,
                orderDraft.orderDraftId(),
                orderDraft.sourceInboundMessageId(),
                orderDraft.merchantId(),
                orderDraft.merchantStaffId(),
                orderDraft.customerId(),
                orderDraft.sourceChannel().name(),
                orderDraft.businessChannel().name(),
                orderDraft.rawText(),
                orderDraft.deliveryRecipientName(),
                orderDraft.deliveryRecipientPhone(),
                orderDraft.deliveryPostalCode(),
                orderDraft.deliveryAddress(),
                orderDraft.deliveryAddressDetail(),
                orderDraft.deliveryRequestMemo(),
                orderDraft.deliveryMemo(),
                orderDraft.courierMemo(),
                orderDraft.orderDraftStatus().name(),
                toTimestamp(orderDraft.createdAt()),
                toTimestamp(orderDraft.updatedAt())
        );
        jdbcTemplate.update("delete from tb_order_draft_item_candidates where order_draft_id = ?", orderDraft.orderDraftId());
        for (int index = 0; index < orderDraft.itemCandidates().size(); index++) {
            OrderDraftItemCandidate item = orderDraft.itemCandidates().get(index);
            jdbcTemplate.update("""
                            insert into tb_order_draft_item_candidates (
                                order_draft_item_candidate_id, order_draft_id, item_name, item_spec,
                                item_quantity, item_unit, matched_unit_price, packaging_width,
                                packaging_depth, packaging_height, packaging_material_name,
                                print_required, design_file_provided, production_quantity,
                                requested_due_date, sample_requested, custom_product_type,
                                imprint_method, item_color, custom_option_text, proof_confirmed,
                                production_confirmed, line_memo, sort_order, created_at, updated_at
                            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    item.orderDraftItemCandidateId(),
                    orderDraft.orderDraftId(),
                    item.itemName(),
                    item.itemSpec(),
                    item.itemQuantity(),
                    item.itemUnit(),
                    item.matchedUnitPrice(),
                    item.packagingWidth(),
                    item.packagingDepth(),
                    item.packagingHeight(),
                    item.materialName(),
                    item.printRequired(),
                    item.designFileProvided(),
                    item.productionQuantity(),
                    toDate(item.requestedDueDate()),
                    item.sampleRequested(),
                    item.customProductType(),
                    item.imprintMethod(),
                    item.itemColor(),
                    item.customOptionText(),
                    item.proofConfirmed(),
                    item.productionConfirmed(),
                    item.lineMemo(),
                    index,
                    toTimestamp(orderDraft.updatedAt()),
                    toTimestamp(orderDraft.updatedAt())
            );
        }
        return orderDraft;
    }

    public Optional<OrderDraft> findById(String orderDraftId) {
        return jdbcTemplate.query(
                "select * from tb_order_drafts where order_draft_id = ?",
                orderDraftRowMapper(),
                orderDraftId
        ).stream().findFirst();
    }

    public List<OrderDraft> findRecent(int limit) {
        return jdbcTemplate.query(
                "select * from tb_order_drafts order by created_at desc limit ?",
                orderDraftRowMapper(),
                limit
        );
    }

    public List<OrderDraft> findRecentByStatus(OrderDraftStatus orderDraftStatus, int limit) {
        return jdbcTemplate.query(
                "select * from tb_order_drafts where order_draft_status = ? order by updated_at desc limit ?",
                orderDraftRowMapper(),
                orderDraftStatus.name(),
                limit
        );
    }

    public List<OrderDraft> findAll() {
        return jdbcTemplate.query(
                "select * from tb_order_drafts order by updated_at desc",
                orderDraftRowMapper()
        );
    }

    private RowMapper<OrderDraft> orderDraftRowMapper() {
        return (rs, rowNum) -> {
            String orderDraftId = rs.getString("order_draft_id");
            return new OrderDraft(
                    orderDraftId,
                    rs.getString("source_inbound_message_id"),
                    rs.getString("merchant_id"),
                    rs.getString("merchant_staff_id"),
                    rs.getString("customer_id"),
                    SourceChannel.valueOf(rs.getString("source_channel")),
                    BusinessChannel.valueOf(rs.getString("business_channel")),
                    rs.getString("raw_text"),
                    findItemCandidates(orderDraftId),
                    rs.getString("delivery_recipient_name"),
                    rs.getString("delivery_recipient_phone_number"),
                    rs.getString("delivery_postal_code"),
                    rs.getString("delivery_address"),
                    rs.getString("delivery_address_detail"),
                    rs.getString("delivery_request_memo"),
                    rs.getString("delivery_memo"),
                    rs.getString("courier_memo"),
                    OrderDraftStatus.valueOf(rs.getString("order_draft_status")),
                    toOffsetDateTime(rs.getTimestamp("created_at")),
                    toOffsetDateTime(rs.getTimestamp("updated_at"))
            );
        };
    }

    private List<OrderDraftItemCandidate> findItemCandidates(String orderDraftId) {
        return jdbcTemplate.query(
                """
                        select * from tb_order_draft_item_candidates
                        where order_draft_id = ?
                        order by sort_order asc, id asc
                        """,
                (rs, rowNum) -> new OrderDraftItemCandidate(
                        rs.getString("order_draft_item_candidate_id"),
                        rs.getString("item_name"),
                        rs.getString("item_spec"),
                        rs.getString("item_quantity"),
                        rs.getString("item_unit"),
                        rs.getBigDecimal("matched_unit_price"),
                        rs.getString("packaging_width"),
                        rs.getString("packaging_depth"),
                        rs.getString("packaging_height"),
                        rs.getString("packaging_material_name"),
                        rs.getObject("print_required", Boolean.class),
                        rs.getObject("design_file_provided", Boolean.class),
                        rs.getString("production_quantity"),
                        toDateString(rs.getDate("requested_due_date")),
                        rs.getObject("sample_requested", Boolean.class),
                        rs.getString("custom_product_type"),
                        rs.getString("imprint_method"),
                        rs.getString("item_color"),
                        rs.getString("custom_option_text"),
                        rs.getObject("proof_confirmed", Boolean.class),
                        rs.getObject("production_confirmed", Boolean.class),
                        rs.getString("line_memo")
                ),
                orderDraftId
        );
    }

    private static Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private static OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }

    private static Date toDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Date.valueOf(LocalDate.parse(value));
    }

    private static String toDateString(Date value) {
        return value == null ? null : value.toLocalDate().toString();
    }
}

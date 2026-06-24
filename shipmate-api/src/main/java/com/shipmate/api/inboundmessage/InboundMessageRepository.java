package com.shipmate.api.inboundmessage;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Repository
public class InboundMessageRepository {
    private static final RowMapper<InboundMessage> ROW_MAPPER = (rs, rowNum) -> new InboundMessage(
            rs.getString("inbound_message_id"),
            rs.getString("merchant_id"),
            rs.getString("merchant_staff_id"),
            rs.getString("customer_id"),
            SourceChannel.valueOf(rs.getString("source_channel")),
            BusinessChannel.valueOf(rs.getString("business_channel")),
            rs.getString("raw_text"),
            InboundMessageStatus.valueOf(rs.getString("inbound_message_status")),
            toOffsetDateTime(rs.getTimestamp("received_at")),
            toOffsetDateTime(rs.getTimestamp("created_at")),
            toOffsetDateTime(rs.getTimestamp("updated_at"))
    );

    private final JdbcTemplate jdbcTemplate;

    public InboundMessageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public InboundMessage save(InboundMessage inboundMessage) {
        jdbcTemplate.update("""
                        insert into tb_inbound_messages (
                            inbound_message_id, merchant_id, merchant_staff_id, customer_id,
                            source_channel, business_channel, raw_text, inbound_message_status,
                            received_at, created_at, updated_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        on duplicate key update
                            merchant_id = values(merchant_id),
                            merchant_staff_id = values(merchant_staff_id),
                            customer_id = values(customer_id),
                            source_channel = values(source_channel),
                            business_channel = values(business_channel),
                            raw_text = values(raw_text),
                            inbound_message_status = values(inbound_message_status),
                            received_at = values(received_at),
                            updated_at = values(updated_at)
                        """,
                inboundMessage.inboundMessageId(),
                inboundMessage.merchantId(),
                inboundMessage.merchantStaffId(),
                inboundMessage.customerId(),
                inboundMessage.sourceChannel().name(),
                inboundMessage.businessChannel().name(),
                inboundMessage.rawText(),
                inboundMessage.inboundMessageStatus().name(),
                toTimestamp(inboundMessage.receivedAt()),
                toTimestamp(inboundMessage.createdAt()),
                toTimestamp(inboundMessage.updatedAt())
        );
        return inboundMessage;
    }

    public Optional<InboundMessage> findById(String inboundMessageId) {
        return jdbcTemplate.query(
                "select * from tb_inbound_messages where inbound_message_id = ?",
                ROW_MAPPER,
                inboundMessageId
        ).stream().findFirst();
    }

    public List<InboundMessage> findRecent(int limit) {
        return jdbcTemplate.query(
                "select * from tb_inbound_messages order by created_at desc limit ?",
                ROW_MAPPER,
                limit
        );
    }

    public List<InboundMessage> findAll() {
        return jdbcTemplate.query(
                "select * from tb_inbound_messages order by created_at desc",
                ROW_MAPPER
        );
    }

    private static Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private static OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}

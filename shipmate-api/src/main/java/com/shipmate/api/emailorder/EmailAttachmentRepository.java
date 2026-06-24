package com.shipmate.api.emailorder;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Repository
public class EmailAttachmentRepository {
    private static final RowMapper<EmailAttachment> ROW_MAPPER = (rs, rowNum) -> new EmailAttachment(
            rs.getString("email_attachment_id"),
            rs.getString("merchant_id"),
            rs.getString("inbound_message_id"),
            com.shipmate.api.inboundmessage.SourceChannel.valueOf(rs.getString("source_channel")),
            rs.getString("source_message_id"),
            rs.getString("file_name"),
            rs.getString("content_type"),
            rs.getLong("file_size_bytes"),
            rs.getString("object_key"),
            toOffsetDateTime(rs.getTimestamp("created_at"))
    );

    private final JdbcTemplate jdbcTemplate;

    public EmailAttachmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(EmailAttachment attachment) {
        jdbcTemplate.update("""
                        insert into tb_email_attachments (
                            email_attachment_id, merchant_id, inbound_message_id,
                            source_channel, source_message_id, file_name, content_type,
                            file_size_bytes, object_key, created_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                attachment.emailAttachmentId(),
                attachment.merchantId(),
                attachment.inboundMessageId(),
                attachment.sourceChannel().name(),
                attachment.sourceMessageId(),
                attachment.fileName(),
                attachment.contentType(),
                attachment.fileSizeBytes(),
                attachment.objectKey(),
                Timestamp.from(attachment.createdAt().toInstant())
        );
    }

    public int countByInboundMessageId(String inboundMessageId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from tb_email_attachments where inbound_message_id = ?",
                Integer.class,
                inboundMessageId
        );
        return count == null ? 0 : count;
    }

    public List<EmailAttachment> findByInboundMessageId(String inboundMessageId) {
        return jdbcTemplate.query(
                "select * from tb_email_attachments where inbound_message_id = ? order by created_at asc",
                ROW_MAPPER,
                inboundMessageId
        );
    }

    public Optional<EmailAttachment> findById(String emailAttachmentId) {
        return jdbcTemplate.query(
                "select * from tb_email_attachments where email_attachment_id = ?",
                ROW_MAPPER,
                emailAttachmentId
        ).stream().findFirst();
    }

    public List<EmailAttachment> findExpired(OffsetDateTime cutoffCreatedAt, int limit) {
        return jdbcTemplate.query(
                "select * from tb_email_attachments where created_at < ? order by created_at asc limit ?",
                ROW_MAPPER,
                Timestamp.from(cutoffCreatedAt.toInstant()),
                Math.max(1, Math.min(limit, 500))
        );
    }

    public int deleteById(String emailAttachmentId) {
        return jdbcTemplate.update(
                "delete from tb_email_attachments where email_attachment_id = ?",
                emailAttachmentId
        );
    }

    private static OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}

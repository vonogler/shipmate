package com.shipmate.api.emailorder;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Repository
public class EmailMailboxConfigRepository {
    private static final RowMapper<EmailMailboxConfig> ROW_MAPPER = (rs, rowNum) -> new EmailMailboxConfig(
            rs.getString("merchant_id"),
            rs.getString("host"),
            rs.getInt("port"),
            rs.getString("username"),
            rs.getString("encrypted_password_memo"),
            rs.getBoolean("ssl_enabled"),
            rs.getString("collection_status"),
            toOffsetDateTime(rs.getTimestamp("updated_at"))
    );

    private final JdbcTemplate jdbcTemplate;

    public EmailMailboxConfigRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public EmailMailboxConfig save(EmailMailboxConfig config) {
        jdbcTemplate.update("""
                        insert into tb_email_mailbox_configs (
                            merchant_id, host, port, username, encrypted_password_memo,
                            ssl_enabled, collection_status, created_at, updated_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        on duplicate key update
                            host = values(host),
                            port = values(port),
                            username = values(username),
                            encrypted_password_memo = values(encrypted_password_memo),
                            ssl_enabled = values(ssl_enabled),
                            collection_status = values(collection_status),
                            updated_at = values(updated_at)
                        """,
                config.merchantId(),
                config.host(),
                config.port(),
                config.username(),
                config.encryptedPasswordMemo(),
                config.sslEnabled(),
                config.collectionStatus(),
                toTimestamp(config.updatedAt()),
                toTimestamp(config.updatedAt())
        );
        return config;
    }

    public void updateCollectionStatus(String merchantId, String collectionStatus, OffsetDateTime updatedAt) {
        jdbcTemplate.update(
                "update tb_email_mailbox_configs set collection_status = ?, updated_at = ? where merchant_id = ?",
                collectionStatus,
                toTimestamp(updatedAt),
                merchantId
        );
    }

    public Optional<EmailMailboxConfig> findByMerchantId(String merchantId) {
        return jdbcTemplate.query(
                "select * from tb_email_mailbox_configs where merchant_id = ?",
                ROW_MAPPER,
                merchantId
        ).stream().findFirst();
    }

    private static Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private static OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}

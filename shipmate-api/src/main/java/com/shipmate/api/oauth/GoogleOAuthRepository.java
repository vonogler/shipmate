package com.shipmate.api.oauth;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Repository
class GoogleOAuthRepository {
    private static final RowMapper<GoogleOAuthState> STATE_ROW_MAPPER = (rs, rowNum) -> new GoogleOAuthState(
            rs.getString("oauth_state"),
            rs.getString("merchant_id"),
            rs.getString("actor_id"),
            toOffsetDateTime(rs.getTimestamp("expires_at")),
            toOffsetDateTime(rs.getTimestamp("used_at")),
            toOffsetDateTime(rs.getTimestamp("created_at"))
    );
    private final JdbcTemplate jdbcTemplate;
    private final GoogleOAuthTokenCrypto tokenCrypto;

    GoogleOAuthRepository(JdbcTemplate jdbcTemplate, GoogleOAuthTokenCrypto tokenCrypto) {
        this.jdbcTemplate = jdbcTemplate;
        this.tokenCrypto = tokenCrypto;
    }

    void saveState(GoogleOAuthState state) {
        jdbcTemplate.update("""
                        insert into tb_google_oauth_states (
                            oauth_state, merchant_id, actor_id, expires_at, used_at, created_at
                        ) values (?, ?, ?, ?, ?, ?)
                        """,
                state.state(),
                state.merchantId(),
                state.actorId(),
                toTimestamp(state.expiresAt()),
                toTimestamp(state.usedAt()),
                toTimestamp(state.createdAt())
        );
    }

    Optional<GoogleOAuthState> findState(String state) {
        return jdbcTemplate.query(
                "select * from tb_google_oauth_states where oauth_state = ?",
                STATE_ROW_MAPPER,
                state
        ).stream().findFirst();
    }

    void markStateUsed(String state, OffsetDateTime usedAt) {
        jdbcTemplate.update(
                "update tb_google_oauth_states set used_at = ? where oauth_state = ?",
                toTimestamp(usedAt),
                state
        );
    }

    void saveToken(GoogleOAuthToken token) {
        jdbcTemplate.update("""
                        insert into tb_google_oauth_tokens (
                            merchant_id, google_email, access_token, refresh_token,
                            scope, token_type, expires_at, created_at, updated_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        on duplicate key update
                            google_email = values(google_email),
                            access_token = values(access_token),
                            refresh_token = coalesce(values(refresh_token), refresh_token),
                            scope = values(scope),
                            token_type = values(token_type),
                            expires_at = values(expires_at),
                            updated_at = values(updated_at)
                        """,
                token.merchantId(),
                token.googleEmail(),
                tokenCrypto.encrypt(token.accessToken()),
                tokenCrypto.encrypt(token.refreshToken()),
                token.scope(),
                token.tokenType(),
                toTimestamp(token.expiresAt()),
                toTimestamp(token.updatedAt()),
                toTimestamp(token.updatedAt())
        );
    }

    List<GoogleOAuthToken> findAllTokens() {
        return jdbcTemplate.query(
                "select * from tb_google_oauth_tokens order by updated_at desc",
                this::mapToken
        );
    }

    Optional<GoogleOAuthToken> findTokenByMerchantId(String merchantId) {
        return jdbcTemplate.query(
                "select * from tb_google_oauth_tokens where merchant_id = ?",
                this::mapToken,
                merchantId
        ).stream().findFirst();
    }

    void updateAccessToken(String merchantId, String accessToken, OffsetDateTime expiresAt, OffsetDateTime updatedAt) {
        jdbcTemplate.update(
                "update tb_google_oauth_tokens set access_token = ?, expires_at = ?, updated_at = ? where merchant_id = ?",
                tokenCrypto.encrypt(accessToken),
                toTimestamp(expiresAt),
                toTimestamp(updatedAt),
                merchantId
        );
    }

    boolean tokenNeedsEncryption(GoogleOAuthToken token) {
        return !tokenCrypto.isEncrypted(rawAccessToken(token.merchantId()))
                || (token.refreshToken() != null && !tokenCrypto.isEncrypted(rawRefreshToken(token.merchantId())));
    }

    private GoogleOAuthToken mapToken(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new GoogleOAuthToken(
                rs.getString("merchant_id"),
                rs.getString("google_email"),
                tokenCrypto.decrypt(rs.getString("access_token")),
                tokenCrypto.decrypt(rs.getString("refresh_token")),
                rs.getString("scope"),
                rs.getString("token_type"),
                toOffsetDateTime(rs.getTimestamp("expires_at")),
                toOffsetDateTime(rs.getTimestamp("updated_at"))
        );
    }

    private String rawAccessToken(String merchantId) {
        return jdbcTemplate.queryForObject(
                "select access_token from tb_google_oauth_tokens where merchant_id = ?",
                String.class,
                merchantId
        );
    }

    private String rawRefreshToken(String merchantId) {
        return jdbcTemplate.queryForObject(
                "select refresh_token from tb_google_oauth_tokens where merchant_id = ?",
                String.class,
                merchantId
        );
    }

    boolean isGmailMessageCollected(String merchantId, String googleMessageId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from tb_google_gmail_collected_messages where merchant_id = ? and google_message_id = ?",
                Integer.class,
                merchantId,
                googleMessageId
        );
        return count != null && count > 0;
    }

    void saveCollectedGmailMessage(
            String merchantId,
            String googleMessageId,
            String googleThreadId,
            String inboundMessageId,
            String subject,
            String fromEmail,
            OffsetDateTime receivedAt,
            OffsetDateTime collectedAt
    ) {
        jdbcTemplate.update("""
                        insert ignore into tb_google_gmail_collected_messages (
                            merchant_id, google_message_id, google_thread_id, inbound_message_id,
                            subject, from_email, received_at, collected_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                merchantId,
                googleMessageId,
                googleThreadId,
                inboundMessageId,
                subject,
                fromEmail,
                toTimestamp(receivedAt),
                toTimestamp(collectedAt)
        );
    }

    List<String> findLatestCollectedGmailMessageIds(String merchantId, int limit) {
        return jdbcTemplate.queryForList(
                """
                        select google_message_id
                        from tb_google_gmail_collected_messages
                        where merchant_id = ?
                        order by collected_at desc
                        limit ?
                        """,
                String.class,
                merchantId,
                limit
        );
    }

    Optional<String> latestCollectedInboundMessageId(String merchantId) {
        List<String> values = merchantId == null || merchantId.isBlank()
                ? jdbcTemplate.queryForList(
                """
                        select inbound_message_id
                        from tb_google_gmail_collected_messages
                        order by collected_at desc
                        limit 1
                        """,
                String.class
        )
                : jdbcTemplate.queryForList(
                """
                        select inbound_message_id
                        from tb_google_gmail_collected_messages
                        where merchant_id = ?
                        order by collected_at desc
                        limit 1
                        """,
                String.class,
                merchantId
        );
        return values.stream().findFirst();
    }

    int countTokens() {
        Integer count = jdbcTemplate.queryForObject("select count(*) from tb_google_oauth_tokens", Integer.class);
        return count == null ? 0 : count;
    }

    int countCollectedMessages() {
        Integer count = jdbcTemplate.queryForObject("select count(*) from tb_google_gmail_collected_messages", Integer.class);
        return count == null ? 0 : count;
    }

    int countStoredAttachments() {
        Integer count = jdbcTemplate.queryForObject("select count(*) from tb_email_attachments", Integer.class);
        return count == null ? 0 : count;
    }

    Optional<OffsetDateTime> latestCollectedAt() {
        Timestamp value = jdbcTemplate.queryForObject(
                "select max(collected_at) from tb_google_gmail_collected_messages",
                Timestamp.class
        );
        return Optional.ofNullable(toOffsetDateTime(value));
    }

    Optional<OffsetDateTime> latestReceivedAt() {
        Timestamp value = jdbcTemplate.queryForObject(
                "select max(received_at) from tb_google_gmail_collected_messages",
                Timestamp.class
        );
        return Optional.ofNullable(toOffsetDateTime(value));
    }

    private static Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private static OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}

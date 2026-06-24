package com.shipmate.api.auth;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Repository
public class MerchantUserRepository {
    private static final RowMapper<MerchantUser> ROW_MAPPER = (rs, rowNum) -> new MerchantUser(
            rs.getString("merchant_user_id"),
            rs.getString("merchant_id"),
            rs.getString("login_id"),
            rs.getString("password_hash"),
            rs.getString("display_name"),
            rs.getString("role"),
            toOffsetDateTime(rs.getTimestamp("locked_at")),
            toOffsetDateTime(rs.getTimestamp("created_at")),
            toOffsetDateTime(rs.getTimestamp("updated_at"))
    );

    private final JdbcTemplate jdbcTemplate;

    public MerchantUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public MerchantUser save(MerchantUser user) {
        jdbcTemplate.update("""
                        insert into tb_merchant_users (
                            merchant_user_id, merchant_id, login_id, password_hash, display_name, role, locked_at, created_at, updated_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        on duplicate key update
                            merchant_id = values(merchant_id),
                            login_id = values(login_id),
                            password_hash = values(password_hash),
                            display_name = values(display_name),
                            role = values(role),
                            locked_at = values(locked_at),
                            updated_at = values(updated_at)
                        """,
                user.merchantUserId(),
                user.merchantId(),
                user.loginId(),
                user.passwordHash(),
                user.displayName(),
                user.role(),
                toTimestamp(user.lockedAt()),
                toTimestamp(user.createdAt()),
                toTimestamp(user.updatedAt())
        );
        return user;
    }

    public Optional<MerchantUser> findByLoginId(String loginId) {
        return jdbcTemplate.query(
                "select * from tb_merchant_users where login_id = ?",
                ROW_MAPPER,
                loginId
        ).stream().findFirst();
    }

    public Optional<MerchantUser> findById(String merchantUserId) {
        return jdbcTemplate.query(
                "select * from tb_merchant_users where merchant_user_id = ?",
                ROW_MAPPER,
                merchantUserId
        ).stream().findFirst();
    }

    public List<MerchantUser> findRecent(int limit) {
        return jdbcTemplate.query(
                "select * from tb_merchant_users order by updated_at desc limit ?",
                ROW_MAPPER,
                limit
        );
    }

    public List<MerchantUser> findByMerchantId(String merchantId) {
        return jdbcTemplate.query(
                "select * from tb_merchant_users where merchant_id = ? order by updated_at desc",
                ROW_MAPPER,
                merchantId
        );
    }

    public int updateLockedAtByMerchantId(String merchantId, OffsetDateTime lockedAt, OffsetDateTime updatedAt) {
        return jdbcTemplate.update(
                "update tb_merchant_users set locked_at = ?, updated_at = ? where merchant_id = ?",
                toTimestamp(lockedAt),
                toTimestamp(updatedAt),
                merchantId
        );
    }

    public int updatePasswordByMerchantId(String merchantId, String passwordHash, OffsetDateTime updatedAt) {
        return jdbcTemplate.update(
                "update tb_merchant_users set password_hash = ?, locked_at = null, updated_at = ? where merchant_id = ?",
                passwordHash,
                toTimestamp(updatedAt),
                merchantId
        );
    }

    private static Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private static OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}

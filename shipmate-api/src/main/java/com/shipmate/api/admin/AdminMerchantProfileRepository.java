package com.shipmate.api.admin;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class AdminMerchantProfileRepository {
    private static final RowMapper<AdminMerchantProfile> ROW_MAPPER = (rs, rowNum) -> new AdminMerchantProfile(
            rs.getString("merchant_id"),
            rs.getString("merchant_status"),
            rs.getString("operation_memo"),
            toOffsetDateTime(rs.getTimestamp("updated_at"))
    );

    private final JdbcTemplate jdbcTemplate;

    public AdminMerchantProfileRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<AdminMerchantProfile> findByMerchantId(String merchantId) {
        return jdbcTemplate.query(
                "select * from tb_merchant_admin_profiles where merchant_id = ?",
                ROW_MAPPER,
                merchantId
        ).stream().findFirst();
    }

    public Map<String, AdminMerchantProfile> findAllAsMap() {
        List<AdminMerchantProfile> profiles = jdbcTemplate.query(
                "select * from tb_merchant_admin_profiles order by updated_at desc",
                ROW_MAPPER
        );
        return profiles.stream().collect(Collectors.toMap(AdminMerchantProfile::merchantId, profile -> profile));
    }

    public AdminMerchantProfile save(AdminMerchantProfile profile) {
        jdbcTemplate.update("""
                        insert into tb_merchant_admin_profiles (
                            merchant_id, merchant_status, operation_memo, created_at, updated_at
                        ) values (?, ?, ?, ?, ?)
                        on duplicate key update
                            merchant_status = values(merchant_status),
                            operation_memo = values(operation_memo),
                            updated_at = values(updated_at)
                        """,
                profile.merchantId(),
                profile.merchantStatus(),
                profile.operationMemo(),
                toTimestamp(profile.updatedAt()),
                toTimestamp(profile.updatedAt())
        );
        return profile;
    }

    private static Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private static OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}

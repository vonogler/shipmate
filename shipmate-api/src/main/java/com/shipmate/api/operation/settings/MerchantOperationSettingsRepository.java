package com.shipmate.api.operation.settings;

import com.shipmate.api.inboundmessage.BusinessChannel;
import com.shipmate.api.shippingorder.CourierCsvTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Repository
public class MerchantOperationSettingsRepository {
    private static final RowMapper<MerchantOperationSettings> ROW_MAPPER = (rs, rowNum) -> new MerchantOperationSettings(
            rs.getString("merchant_id"),
            rs.getString("default_merchant_staff_id"),
            CourierCsvTemplate.valueOf(rs.getString("default_courier_csv_template")),
            BusinessChannel.valueOf(rs.getString("default_business_channel")),
            toOffsetDateTime(rs.getTimestamp("updated_at"))
    );

    private final JdbcTemplate jdbcTemplate;

    public MerchantOperationSettingsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public MerchantOperationSettings save(MerchantOperationSettings setting) {
        jdbcTemplate.update("""
                        insert into tb_merchant_operation_settings (
                            merchant_id, default_merchant_staff_id, default_courier_csv_template,
                            default_business_channel, created_at, updated_at
                        ) values (?, ?, ?, ?, ?, ?)
                        on duplicate key update
                            default_merchant_staff_id = values(default_merchant_staff_id),
                            default_courier_csv_template = values(default_courier_csv_template),
                            default_business_channel = values(default_business_channel),
                            updated_at = values(updated_at)
                        """,
                setting.merchantId(),
                setting.defaultMerchantStaffId(),
                setting.defaultCourierCsvTemplate().name(),
                setting.defaultBusinessChannel().name(),
                toTimestamp(setting.updatedAt()),
                toTimestamp(setting.updatedAt())
        );
        return setting;
    }

    public Optional<MerchantOperationSettings> findByMerchantId(String merchantId) {
        return jdbcTemplate.query(
                "select * from tb_merchant_operation_settings where merchant_id = ?",
                ROW_MAPPER,
                merchantId
        ).stream().findFirst();
    }

    public List<MerchantOperationSettings> findAll() {
        return jdbcTemplate.query(
                "select * from tb_merchant_operation_settings order by updated_at desc",
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

package com.shipmate.api.shippingorder;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CourierCsvValidationRuleRepository {
    private static final RowMapper<CourierCsvValidationRule> ROW_MAPPER = (rs, rowNum) -> new CourierCsvValidationRule(
            rs.getString("courier_csv_validation_rule_id"),
            CourierCsvTemplate.valueOf(rs.getString("courier_csv_template")),
            rs.getString("field_name"),
            rs.getString("csv_column_name"),
            rs.getBoolean("required"),
            rs.getString("error_message"),
            rs.getInt("sort_order"),
            rs.getBoolean("active")
    );

    private final JdbcTemplate jdbcTemplate;

    public CourierCsvValidationRuleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<CourierCsvValidationRule> findActiveByTemplate(CourierCsvTemplate template) {
        return jdbcTemplate.query(
                """
                        select *
                        from tb_courier_csv_validation_rules
                        where courier_csv_template = ? and active = true
                        order by sort_order asc, field_name asc
                        """,
                ROW_MAPPER,
                template.name()
        );
    }

    public List<CourierCsvValidationRule> findAll() {
        return jdbcTemplate.query(
                """
                        select *
                        from tb_courier_csv_validation_rules
                        order by courier_csv_template asc, sort_order asc, field_name asc
                        """,
                ROW_MAPPER
        );
    }

    public Optional<CourierCsvValidationRule> findById(String ruleId) {
        return jdbcTemplate.query(
                """
                        select *
                        from tb_courier_csv_validation_rules
                        where courier_csv_validation_rule_id = ?
                        """,
                ROW_MAPPER,
                ruleId
        ).stream().findFirst();
    }

    public Optional<CourierCsvValidationRule> findByTemplateAndField(CourierCsvTemplate template, String fieldName) {
        return jdbcTemplate.query(
                """
                        select *
                        from tb_courier_csv_validation_rules
                        where courier_csv_template = ? and field_name = ?
                        """,
                ROW_MAPPER,
                template.name(),
                fieldName
        ).stream().findFirst();
    }

    public CourierCsvValidationRule save(CourierCsvValidationRule rule) {
        jdbcTemplate.update(
                """
                        insert into tb_courier_csv_validation_rules (
                            courier_csv_validation_rule_id, courier_csv_template, field_name,
                            csv_column_name, required, error_message, sort_order, active, updated_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, current_timestamp)
                        on duplicate key update
                            csv_column_name = values(csv_column_name),
                            required = values(required),
                            error_message = values(error_message),
                            sort_order = values(sort_order),
                            active = values(active),
                            updated_at = current_timestamp
                        """,
                rule.courierCsvValidationRuleId(),
                rule.courierCsvTemplate().name(),
                rule.fieldName(),
                rule.csvColumnName(),
                rule.required(),
                rule.errorMessage(),
                rule.sortOrder(),
                rule.active()
        );
        return findByTemplateAndField(rule.courierCsvTemplate(), rule.fieldName())
                .or(() -> findById(rule.courierCsvValidationRuleId()))
                .orElse(rule);
    }
}

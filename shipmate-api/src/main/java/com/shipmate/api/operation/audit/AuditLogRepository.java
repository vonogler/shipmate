package com.shipmate.api.operation.audit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Repository
public class AuditLogRepository {
    private static final RowMapper<AuditLog> ROW_MAPPER = (rs, rowNum) -> new AuditLog(
            rs.getString("audit_log_id"),
            rs.getString("merchant_id"),
            rs.getString("actor_id"),
            rs.getString("action_type"),
            rs.getString("target_type"),
            rs.getString("target_id"),
            rs.getString("summary"),
            rs.getString("metadata_json"),
            toOffsetDateTime(rs.getTimestamp("created_at"))
    );

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public AuditLogRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public AuditLog append(AuditLog auditLog) {
        jdbcTemplate.update("""
                        insert into tb_audit_logs (
                            audit_log_id, merchant_id, actor_id, action_type,
                            target_type, target_id, summary, metadata_json, created_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                auditLog.auditLogId(),
                auditLog.merchantId(),
                auditLog.actorId(),
                auditLog.actionType(),
                auditLog.targetType(),
                auditLog.targetId(),
                auditLog.summary(),
                auditLog.auditMetadataJson(),
                toTimestamp(auditLog.createdAt())
        );
        return auditLog;
    }

    public List<AuditLog> findRecent(int limit) {
        return jdbcTemplate.query(
                "select * from tb_audit_logs order by created_at desc limit ?",
                ROW_MAPPER,
                limit
        );
    }

    public List<AuditLog> search(AuditLogSearchCriteria criteria) {
        QueryParts queryParts = queryParts(criteria, false);
        return jdbcTemplate.query(queryParts.sql(), ROW_MAPPER, queryParts.parameters().toArray());
    }

    public long count(AuditLogSearchCriteria criteria) {
        QueryParts queryParts = queryParts(criteria, true);
        Long count = jdbcTemplate.queryForObject(queryParts.sql(), Long.class, queryParts.parameters().toArray());
        return count == null ? 0 : count;
    }

    public AuditLogFilterOptionsResponse filterOptions() {
        return new AuditLogFilterOptionsResponse(
                distinctValues("action_type"),
                distinctValues("target_type"),
                distinctValues("merchant_id"),
                metadataKeys()
        );
    }

    private List<String> metadataKeys() {
        List<String> metadataJsonRows = jdbcTemplate.queryForList(
                "select metadata_json from tb_audit_logs where metadata_json is not null and metadata_json <> '' order by created_at desc limit 500",
                String.class
        );
        Set<String> keys = new LinkedHashSet<>();
        for (String metadataJson : metadataJsonRows) {
            try {
                keys.addAll(objectMapper.readValue(metadataJson, new TypeReference<java.util.Map<String, Object>>() {
                }).keySet());
            } catch (Exception ignored) {
                // metadata_json may contain legacy plain text.
            }
        }
        return keys.stream().sorted().toList();
    }

    private List<String> distinctValues(String columnName) {
        return jdbcTemplate.queryForList(
                "select distinct " + columnName + " from tb_audit_logs where " + columnName + " is not null order by " + columnName + " asc",
                String.class
        );
    }

    private QueryParts queryParts(AuditLogSearchCriteria criteria, boolean countOnly) {
        StringBuilder sql = new StringBuilder(countOnly
                ? "select count(*) from tb_audit_logs where 1 = 1"
                : "select * from tb_audit_logs where 1 = 1");
        List<Object> parameters = new ArrayList<>();

        if (!isBlank(criteria.actionType())) {
            sql.append(" and action_type = ?");
            parameters.add(criteria.actionType().strip());
        }
        if (!isBlank(criteria.targetType())) {
            sql.append(" and target_type = ?");
            parameters.add(criteria.targetType().strip());
        }
        if (!isBlank(criteria.merchantId())) {
            sql.append(" and merchant_id = ?");
            parameters.add(criteria.merchantId().strip());
        }
        if (!isBlank(criteria.metadataKey())) {
            sql.append(" and json_valid(metadata_json) and json_contains_path(metadata_json, 'one', ?)");
            parameters.add(metadataJsonPath(criteria.metadataKey()));
        }
        if (criteria.days() != null && criteria.days() > 0) {
            sql.append(" and created_at >= ?");
            parameters.add(toTimestamp(OffsetDateTime.now().minusDays(criteria.days())));
        }
        if (!isBlank(criteria.searchText())) {
            String keyword = "%" + criteria.searchText().strip().toLowerCase(Locale.ROOT) + "%";
            sql.append("""
                     and (
                        lower(audit_log_id) like ?
                        or lower(merchant_id) like ?
                        or lower(coalesce(actor_id, '')) like ?
                        or lower(action_type) like ?
                        or lower(target_type) like ?
                        or lower(coalesce(target_id, '')) like ?
                        or lower(summary) like ?
                        or lower(coalesce(metadata_json, '')) like ?
                     )
                    """);
            for (int i = 0; i < 8; i++) {
                parameters.add(keyword);
            }
        }
        if (!countOnly) {
            sql.append(" order by created_at desc limit ? offset ?");
            parameters.add(criteria.limit());
            parameters.add(criteria.offset());
        }
        return new QueryParts(sql.toString(), parameters);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String metadataJsonPath(String metadataKey) {
        String escaped = metadataKey.strip()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
        return "$.\"" + escaped + "\"";
    }

    private static Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private static OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }

    private record QueryParts(String sql, List<Object> parameters) {
    }
}

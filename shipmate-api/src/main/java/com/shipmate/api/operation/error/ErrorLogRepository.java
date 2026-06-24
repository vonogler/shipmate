package com.shipmate.api.operation.error;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Repository
public class ErrorLogRepository {
    private static final RowMapper<ErrorLog> ROW_MAPPER = (rs, rowNum) -> new ErrorLog(
            rs.getString("error_log_id"),
            rs.getString("code"),
            rs.getInt("http_status"),
            rs.getString("message"),
            rs.getString("exception_type"),
            rs.getString("metadata_json"),
            rs.getString("handling_status"),
            rs.getString("assigned_actor_id"),
            rs.getString("handling_memo"),
            toOffsetDateTime(rs.getTimestamp("handled_at")),
            toOffsetDateTime(rs.getTimestamp("created_at"))
    );

    private final JdbcTemplate jdbcTemplate;

    public ErrorLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ErrorLog append(ErrorLog errorLog) {
        jdbcTemplate.update("""
                        insert into tb_error_logs (
                            error_log_id, code, http_status, message, exception_type,
                            metadata_json, handling_status, assigned_actor_id, handling_memo, handled_at, created_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                errorLog.errorLogId(),
                errorLog.code(),
                errorLog.status(),
                errorLog.message(),
                errorLog.exceptionType(),
                errorLog.errorMetadataJson(),
                errorLog.handlingStatus(),
                errorLog.assignedActorId(),
                errorLog.handlingMemo(),
                toTimestamp(errorLog.handledAt()),
                toTimestamp(errorLog.createdAt())
        );
        return errorLog;
    }

    public List<ErrorLog> findRecent(int limit) {
        return jdbcTemplate.query(
                "select * from tb_error_logs order by created_at desc limit ?",
                ROW_MAPPER,
                limit
        );
    }

    public List<ErrorLog> search(ErrorLogSearchCriteria criteria) {
        QueryParts queryParts = queryParts(criteria);
        return jdbcTemplate.query(queryParts.sql(), ROW_MAPPER, queryParts.parameters().toArray());
    }

    public long count(ErrorLogSearchCriteria criteria) {
        QueryParts queryParts = queryParts(criteria, true);
        Long count = jdbcTemplate.queryForObject(queryParts.sql(), Long.class, queryParts.parameters().toArray());
        return count == null ? 0 : count;
    }

    public ErrorLog updateHandling(
            String errorLogId,
            String handlingStatus,
            String assignedActorId,
            String handlingMemo,
            OffsetDateTime handledAt
    ) {
        jdbcTemplate.update("""
                        update tb_error_logs
                        set handling_status = ?,
                            assigned_actor_id = ?,
                            handling_memo = ?,
                            handled_at = ?
                        where error_log_id = ?
                        """,
                handlingStatus,
                assignedActorId,
                handlingMemo,
                toTimestamp(handledAt),
                errorLogId
        );
        return jdbcTemplate.query(
                "select * from tb_error_logs where error_log_id = ?",
                ROW_MAPPER,
                errorLogId
        ).stream().findFirst().orElseThrow();
    }

    private static Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private static OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }

    private QueryParts queryParts(ErrorLogSearchCriteria criteria) {
        return queryParts(criteria, false);
    }

    private QueryParts queryParts(ErrorLogSearchCriteria criteria, boolean countOnly) {
        StringBuilder sql = new StringBuilder(countOnly
                ? "select count(*) from tb_error_logs where 1 = 1"
                : "select * from tb_error_logs where 1 = 1");
        List<Object> parameters = new ArrayList<>();
        if (!isBlank(criteria.metadataKey())) {
            sql.append(" and json_valid(metadata_json) and json_contains_path(metadata_json, 'one', ?)");
            parameters.add(metadataJsonPath(criteria.metadataKey()));
        }
        if (!isBlank(criteria.handlingStatus())) {
            sql.append(" and handling_status = ?");
            parameters.add(criteria.handlingStatus().strip());
        }
        if (criteria.days() != null && criteria.days() > 0) {
            sql.append(" and created_at >= ?");
            parameters.add(toTimestamp(OffsetDateTime.now().minusDays(criteria.days())));
        }
        if (!isBlank(criteria.searchText())) {
            String keyword = "%" + criteria.searchText().strip().toLowerCase(Locale.ROOT) + "%";
            sql.append("""
                     and (
                        lower(error_log_id) like ?
                        or lower(code) like ?
                        or lower(message) like ?
                        or lower(coalesce(exception_type, '')) like ?
                        or lower(handling_status) like ?
                        or lower(coalesce(assigned_actor_id, '')) like ?
                        or lower(coalesce(handling_memo, '')) like ?
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

    private record QueryParts(String sql, List<Object> parameters) {
    }
}

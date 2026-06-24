package com.shipmate.api.oauth;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class GoogleGmailPollingRunRepository {
    private static final RowMapper<GoogleGmailPollingRun> ROW_MAPPER = (rs, rowNum) -> new GoogleGmailPollingRun(
            rs.getString("gmail_polling_run_id"),
            rs.getString("trigger_type"),
            rs.getString("merchant_id"),
            rs.getInt("account_count"),
            rs.getInt("scanned_count"),
            rs.getInt("collected_count"),
            rs.getInt("order_draft_created_count"),
            rs.getInt("attachment_stored_count"),
            rs.getInt("skipped_duplicate_count"),
            rs.getInt("skipped_non_order_count"),
            rs.getBoolean("success"),
            rs.getString("failure_code"),
            rs.getString("failure_message"),
            rs.getString("latest_inbound_message_id"),
            toOffsetDateTime(rs.getTimestamp("started_at")),
            toOffsetDateTime(rs.getTimestamp("finished_at"))
    );

    private final JdbcTemplate jdbcTemplate;

    public GoogleGmailPollingRunRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(GoogleGmailPollingRun run) {
        jdbcTemplate.update("""
                        insert into tb_gmail_polling_runs (
                            gmail_polling_run_id, trigger_type, merchant_id,
                            account_count, scanned_count, collected_count,
                            order_draft_created_count, attachment_stored_count,
                            skipped_duplicate_count, skipped_non_order_count, success, failure_code,
                            failure_message, latest_inbound_message_id, started_at, finished_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                run.gmailPollingRunId(),
                run.triggerType(),
                run.merchantId(),
                run.accountCount(),
                run.scannedCount(),
                run.collectedCount(),
                run.orderDraftCreatedCount(),
                run.attachmentStoredCount(),
                run.skippedDuplicateCount(),
                run.skippedNonOrderCount(),
                run.success(),
                run.failureCode(),
                run.failureMessage(),
                run.latestInboundMessageId(),
                toTimestamp(run.startedAt()),
                toTimestamp(run.finishedAt())
        );
    }

    public int countSince(OffsetDateTime from) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from tb_gmail_polling_runs where started_at >= ?",
                Integer.class,
                toTimestamp(from)
        );
        return count == null ? 0 : count;
    }

    public int countFailureSince(OffsetDateTime from) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from tb_gmail_polling_runs where success = false and started_at >= ?",
                Integer.class,
                toTimestamp(from)
        );
        return count == null ? 0 : count;
    }

    public int sumSkippedNonOrderSince(OffsetDateTime from) {
        Integer count = jdbcTemplate.queryForObject(
                "select coalesce(sum(skipped_non_order_count), 0) from tb_gmail_polling_runs where started_at >= ?",
                Integer.class,
                toTimestamp(from)
        );
        return count == null ? 0 : count;
    }

    public Optional<GoogleGmailPollingRun> latestSuccess() {
        return jdbcTemplate.query(
                "select * from tb_gmail_polling_runs where success = true order by finished_at desc limit 1",
                ROW_MAPPER
        ).stream().findFirst();
    }

    public Optional<GoogleGmailPollingRun> latestFailure() {
        return jdbcTemplate.query(
                "select * from tb_gmail_polling_runs where success = false order by finished_at desc limit 1",
                ROW_MAPPER
        ).stream().findFirst();
    }

    public Map<String, Long> failureCodeCountsSince(OffsetDateTime from) {
        return jdbcTemplate.query(
                        """
                                select coalesce(failure_code, 'UNKNOWN') as failure_code, count(*) as failure_count
                                from tb_gmail_polling_runs
                                where success = false and started_at >= ?
                                group by coalesce(failure_code, 'UNKNOWN')
                                order by failure_count desc
                                """,
                        (rs, rowNum) -> Map.entry(rs.getString("failure_code"), rs.getLong("failure_count")),
                        toTimestamp(from)
                ).stream()
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public List<GoogleGmailPollingRun> recent(int limit) {
        return jdbcTemplate.query(
                "select * from tb_gmail_polling_runs order by started_at desc limit ?",
                ROW_MAPPER,
                Math.max(1, Math.min(limit, 100))
        );
    }

    public List<GoogleGmailPollingRun> search(GoogleGmailPollingRunSearchCriteria criteria) {
        StringBuilder sql = new StringBuilder("select * from tb_gmail_polling_runs where 1 = 1");
        List<Object> parameters = new ArrayList<>();
        if (criteria.merchantId() != null && !criteria.merchantId().isBlank()) {
            sql.append(" and merchant_id = ?");
            parameters.add(criteria.merchantId().strip());
        }
        if (criteria.success() != null) {
            sql.append(" and success = ?");
            parameters.add(criteria.success());
        }
        if (criteria.days() != null && criteria.days() > 0) {
            sql.append(" and started_at >= ?");
            parameters.add(toTimestamp(OffsetDateTime.now().minusDays(criteria.days())));
        }
        sql.append(" order by started_at desc limit ?");
        parameters.add(Math.max(1, Math.min(criteria.limit(), 200)));
        return jdbcTemplate.query(sql.toString(), ROW_MAPPER, parameters.toArray());
    }

    private static Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private static OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}

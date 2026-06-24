alter table tb_gmail_polling_runs
    add column skipped_non_order_count integer not null default 0 after skipped_duplicate_count;

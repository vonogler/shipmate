alter table tb_error_logs
    add column metadata_json json null after exception_type;

alter table tb_gmail_polling_runs
    add column latest_inbound_message_id varchar(80) null after failure_message,
    add key ix_tb_gmail_polling_runs_latest_inbound (latest_inbound_message_id);

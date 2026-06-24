alter table tb_audit_logs
    add column metadata_json json null after summary,
    add key ix_tb_audit_logs_action_created (action_type, created_at),
    add key ix_tb_audit_logs_target_created (target_type, created_at),
    add key ix_tb_audit_logs_merchant_created (merchant_id, created_at);

create table if not exists tb_google_gmail_collected_messages (
    id bigint not null auto_increment,
    merchant_id varchar(80) not null,
    google_message_id varchar(255) not null,
    google_thread_id varchar(255) null,
    inbound_message_id varchar(80) not null,
    subject varchar(500) null,
    from_email varchar(500) null,
    received_at timestamp null,
    collected_at timestamp not null default current_timestamp,
    primary key (id),
    unique key uk_tb_google_gmail_collected_message (merchant_id, google_message_id),
    key ix_tb_google_gmail_collected_merchant_id (merchant_id),
    key ix_tb_google_gmail_collected_inbound_message_id (inbound_message_id),
    key ix_tb_google_gmail_collected_collected_at (collected_at)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

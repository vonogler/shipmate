create table if not exists tb_email_attachments (
    email_attachment_id varchar(64) primary key,
    merchant_id varchar(64) not null,
    inbound_message_id varchar(64) not null,
    source_channel varchar(40) not null,
    source_message_id varchar(200) null,
    file_name varchar(500) not null,
    content_type varchar(200) not null,
    file_size_bytes bigint not null,
    object_key varchar(800) not null,
    created_at datetime(6) not null,
    index ix_tb_email_attachments_inbound_message_id (inbound_message_id),
    index ix_tb_email_attachments_source_message (merchant_id, source_channel, source_message_id),
    constraint fk_tb_email_attachments_inbound_message
        foreign key (inbound_message_id) references tb_inbound_messages(inbound_message_id)
);

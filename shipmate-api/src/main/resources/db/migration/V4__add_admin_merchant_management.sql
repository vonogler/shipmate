create table if not exists tb_merchant_admin_profiles (
    id bigint not null auto_increment,
    merchant_id varchar(80) not null,
    merchant_status varchar(40) not null default 'ACTIVE',
    operation_memo varchar(1000) null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp on update current_timestamp,
    primary key (id),
    unique key uk_tb_merchant_admin_profiles_merchant_id (merchant_id),
    key ix_tb_merchant_admin_profiles_status (merchant_status)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

alter table tb_merchant_users
    add column locked_at timestamp null after role;

alter table tb_error_logs
    add column handling_status varchar(40) not null default 'OPEN' after exception_type,
    add column assigned_actor_id varchar(80) null after handling_status,
    add column handling_memo varchar(1000) null after assigned_actor_id,
    add column handled_at timestamp null after handling_memo;

create table if not exists tb_google_oauth_states (
    id bigint not null auto_increment,
    oauth_state varchar(160) not null,
    merchant_id varchar(80) not null,
    actor_id varchar(80) null,
    expires_at timestamp not null,
    used_at timestamp null,
    created_at timestamp not null default current_timestamp,
    primary key (id),
    unique key uk_tb_google_oauth_states_state (oauth_state),
    key ix_tb_google_oauth_states_merchant_id (merchant_id),
    key ix_tb_google_oauth_states_expires_at (expires_at)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table if not exists tb_google_oauth_tokens (
    id bigint not null auto_increment,
    merchant_id varchar(80) not null,
    google_email varchar(255) null,
    access_token text not null,
    refresh_token text null,
    scope text null,
    token_type varchar(80) null,
    expires_at timestamp null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp on update current_timestamp,
    primary key (id),
    unique key uk_tb_google_oauth_tokens_merchant_id (merchant_id),
    key ix_tb_google_oauth_tokens_google_email (google_email),
    key ix_tb_google_oauth_tokens_expires_at (expires_at)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

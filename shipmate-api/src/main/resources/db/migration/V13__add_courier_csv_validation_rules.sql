create table if not exists tb_courier_csv_validation_rules (
    courier_csv_validation_rule_id varchar(80) primary key,
    courier_csv_template varchar(40) not null,
    field_name varchar(120) not null,
    csv_column_name varchar(120) null,
    required boolean not null default true,
    error_message varchar(500) not null,
    sort_order integer not null default 0,
    active boolean not null default true,
    updated_at timestamp not null default current_timestamp,
    unique key uk_tb_courier_csv_validation_rules_template_field (courier_csv_template, field_name),
    key ix_tb_courier_csv_validation_rules_template_active (courier_csv_template, active)
);

insert into tb_courier_csv_validation_rules (
    courier_csv_validation_rule_id, courier_csv_template, field_name, csv_column_name,
    required, error_message, sort_order, active
) values
('ccvr_generic_recipient_name', 'GENERIC', 'deliveryRecipientName', '수취인', true, '수취인이 없습니다.', 10, true),
('ccvr_generic_recipient_phone', 'GENERIC', 'deliveryRecipientPhone', '연락처', true, '수취인 연락처가 없습니다.', 20, true),
('ccvr_generic_address', 'GENERIC', 'deliveryAddress', '주소', true, '주소가 없습니다.', 30, true),
('ccvr_generic_item_name', 'GENERIC', 'itemCandidates', '품목요약', true, '품목명이 없습니다.', 40, true),
('ccvr_cj_recipient_name', 'CJ_LOGISTICS', 'deliveryRecipientName', '받는분', true, '수취인이 없습니다.', 10, true),
('ccvr_cj_recipient_phone', 'CJ_LOGISTICS', 'deliveryRecipientPhone', '받는분전화번호', true, '수취인 연락처가 없습니다.', 20, true),
('ccvr_cj_postal_code', 'CJ_LOGISTICS', 'deliveryPostalCode', '받는분우편번호', true, '택배사 양식에 필요한 우편번호가 없습니다.', 30, true),
('ccvr_cj_address', 'CJ_LOGISTICS', 'deliveryAddress', '받는분주소', true, '주소가 없습니다.', 40, true),
('ccvr_cj_item_name', 'CJ_LOGISTICS', 'itemCandidates', '품목명', true, '품목명이 없습니다.', 50, true),
('ccvr_cj_item_quantity', 'CJ_LOGISTICS', 'itemCandidates.itemQuantity', '수량', true, '택배사 양식에 필요한 수량이 없습니다.', 60, true),
('ccvr_lotte_recipient_name', 'LOTTE', 'deliveryRecipientName', '수취인명', true, '수취인이 없습니다.', 10, true),
('ccvr_lotte_recipient_phone', 'LOTTE', 'deliveryRecipientPhone', '수취인전화번호', true, '수취인 연락처가 없습니다.', 20, true),
('ccvr_lotte_postal_code', 'LOTTE', 'deliveryPostalCode', '수취인우편번호', true, '택배사 양식에 필요한 우편번호가 없습니다.', 30, true),
('ccvr_lotte_address', 'LOTTE', 'deliveryAddress', '수취인주소', true, '주소가 없습니다.', 40, true),
('ccvr_lotte_item_name', 'LOTTE', 'itemCandidates', '상품명', true, '품목명이 없습니다.', 50, true),
('ccvr_lotte_item_quantity', 'LOTTE', 'itemCandidates.itemQuantity', '수량', true, '택배사 양식에 필요한 수량이 없습니다.', 60, true),
('ccvr_hanjin_recipient_name', 'HANJIN', 'deliveryRecipientName', '받는고객명', true, '수취인이 없습니다.', 10, true),
('ccvr_hanjin_recipient_phone', 'HANJIN', 'deliveryRecipientPhone', '받는고객전화번호', true, '수취인 연락처가 없습니다.', 20, true),
('ccvr_hanjin_postal_code', 'HANJIN', 'deliveryPostalCode', '받는고객우편번호', true, '택배사 양식에 필요한 우편번호가 없습니다.', 30, true),
('ccvr_hanjin_address', 'HANJIN', 'deliveryAddress', '받는고객주소', true, '주소가 없습니다.', 40, true),
('ccvr_hanjin_item_name', 'HANJIN', 'itemCandidates', '상품명', true, '품목명이 없습니다.', 50, true),
('ccvr_hanjin_item_quantity', 'HANJIN', 'itemCandidates.itemQuantity', '상품수량', true, '택배사 양식에 필요한 수량이 없습니다.', 60, true)
on duplicate key update
    csv_column_name = values(csv_column_name),
    required = values(required),
    error_message = values(error_message),
    sort_order = values(sort_order),
    active = values(active),
    updated_at = current_timestamp;

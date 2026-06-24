insert into tb_code (code_group, code, label_ko, description, active, sort_order)
values ('ORDER_DRAFT_STATUS', 'NEEDS_REVIEW', '검수 필요', 'AI 추출 신뢰도가 낮아 검수가 필요한 출고표 초안', true, 15)
on duplicate key update
    label_ko = values(label_ko),
    description = values(description),
    active = values(active),
    sort_order = values(sort_order);

import type { ReactNode } from 'react';
import {
  Alert,
  Button,
  Col,
  Descriptions,
  Divider,
  Form,
  Input,
  Row,
  Select,
  Space,
  Table,
  Tag,
  Typography
} from 'antd';
import type { FormInstance } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  CheckCircleOutlined,
  FileTextOutlined,
  PlusOutlined,
  SaveOutlined,
  SendOutlined
} from '@ant-design/icons';
import type {
  OrderDraft,
  OrderDraftItemCandidate,
  OrderDraftReviewFormValues,
  OrderDraftStatus
} from '../types';
import DraftReviewItemFields from './DraftReviewItemFields';

type DraftReviewListProps = {
  orderDraftStatusFilter: OrderDraftStatus | 'ALL';
  orderDraftSearchText: string;
  orderDraftColumns: ColumnsType<OrderDraft>;
  filteredOrderDrafts: OrderDraft[];
  loadingOrderDrafts: boolean;
  selectedOrderDraftId: string | null;
  onStatusFilterChange: (value: OrderDraftStatus | 'ALL') => void;
  onSearchTextChange: (value: string) => void;
  onSelectedOrderDraftChange: (orderDraftId: string | null) => void;
};

type DraftReviewDetailProps = {
  selectedOrderDraft: OrderDraft | null;
  selectedOrderDraftCustomerExists: boolean;
  selectedOrderDraftCustomerPriceItemCount: number;
  selectedOrderDraftMissingFields: string[];
  reviewForm: FormInstance<OrderDraftReviewFormValues>;
  draftAttachmentList: ReactNode;
};

type DraftReviewActionProps = {
  extractingAiOrderDraftId: string | null;
  savingOrderDraftReview: boolean;
  submittingOrderDraft: boolean;
  canOperate: boolean;
  onApplyCustomerDefaults: () => void;
  onApplyCustomerPriceCandidates: () => void;
  onExtractAiCandidates: () => void;
  onSaveOrderDraftReview: (values: OrderDraftReviewFormValues) => void;
  onSubmitOrderDraft: () => void;
  emptyItemCandidate: () => OrderDraftItemCandidate;
};

type DraftReviewScreenProps = {
  active: boolean;
  orderDraftStatusLabels: Record<OrderDraftStatus, string>;
  orderDraftStatusColors: Record<OrderDraftStatus, string>;
  list: DraftReviewListProps;
  detail: DraftReviewDetailProps;
  actions: DraftReviewActionProps;
  formatDate: (value: string | null) => string;
};

export default function DraftReviewScreen({
  active,
  orderDraftStatusLabels,
  orderDraftStatusColors,
  list,
  detail,
  actions,
  formatDate
}: DraftReviewScreenProps) {
  const {
    orderDraftStatusFilter,
    orderDraftSearchText,
    orderDraftColumns,
    filteredOrderDrafts,
    loadingOrderDrafts,
    selectedOrderDraftId,
    onStatusFilterChange,
    onSearchTextChange,
    onSelectedOrderDraftChange
  } = list;
  const {
    selectedOrderDraft,
    selectedOrderDraftCustomerExists,
    selectedOrderDraftCustomerPriceItemCount,
    selectedOrderDraftMissingFields,
    reviewForm,
    draftAttachmentList
  } = detail;
  const {
    extractingAiOrderDraftId,
    savingOrderDraftReview,
    submittingOrderDraft,
    canOperate,
    onApplyCustomerDefaults,
    onApplyCustomerPriceCandidates,
    onExtractAiCandidates,
    onSaveOrderDraftReview,
    onSubmitOrderDraft,
    emptyItemCandidate
  } = actions;

  return (
    <Row gutter={[16, 16]} align="stretch" className={`navigation-screen ${active ? 'is-active' : ''}`}>
      <Col xs={24} xl={14}>
        <section className="work-panel full-height">
          <div className="table-heading">
            <div>
              <Typography.Title level={2}>출고표 초안 목록</Typography.Title>
              <Typography.Text type="secondary">상태와 주문 내용으로 검수할 초안을 찾습니다.</Typography.Text>
            </div>
            <Space wrap>
              <Select
                value={orderDraftStatusFilter}
                onChange={onStatusFilterChange}
                className="courier-template-select"
                options={[
                  { value: 'ALL', label: '전체 상태' },
                  ...Object.entries(orderDraftStatusLabels).map(([value, label]) => ({ value, label }))
                ]}
              />
              <Input.Search
                allowClear
                value={orderDraftSearchText}
                onChange={(event) => onSearchTextChange(event.target.value)}
                placeholder="거래처, 수취인, 품목, 원문 검색"
              />
            </Space>
          </div>
          <Table
            rowKey="orderDraftId"
            columns={orderDraftColumns}
            dataSource={filteredOrderDrafts}
            loading={loadingOrderDrafts}
            size="middle"
            scroll={{ x: 800 }}
            pagination={{ pageSize: 8, showSizeChanger: false }}
            rowSelection={{
              type: 'radio',
              selectedRowKeys: selectedOrderDraftId ? [selectedOrderDraftId] : [],
              onChange: (keys) => onSelectedOrderDraftChange(String(keys[0]))
            }}
            onRow={(row) => ({
              onClick: () => onSelectedOrderDraftChange(row.orderDraftId)
            })}
          />
        </section>
      </Col>
      <Col xs={24} xl={10}>
        <section className="work-panel full-height">
          <Typography.Title level={2}>초안 검수</Typography.Title>
          {selectedOrderDraft ? (
            <>
              <Descriptions size="small" column={1} bordered>
                <Descriptions.Item label="출고표 초안 ID">{selectedOrderDraft.orderDraftId}</Descriptions.Item>
                <Descriptions.Item label="상태">
                  <Tag color={orderDraftStatusColors[selectedOrderDraft.orderDraftStatus]}>
                    {orderDraftStatusLabels[selectedOrderDraft.orderDraftStatus]}
                  </Tag>
                </Descriptions.Item>
                <Descriptions.Item label="원문 ID">{selectedOrderDraft.sourceInboundMessageId}</Descriptions.Item>
                <Descriptions.Item label="거래처">{selectedOrderDraft.customerId || '-'}</Descriptions.Item>
                <Descriptions.Item label="생성 시각">{formatDate(selectedOrderDraft.createdAt)}</Descriptions.Item>
              </Descriptions>
              <Space wrap className="settings-actions">
                <Button onClick={onApplyCustomerDefaults} disabled={!selectedOrderDraftCustomerExists}>
                  고객 기본 배송지 반영
                </Button>
                <Button onClick={onApplyCustomerPriceCandidates} disabled={!selectedOrderDraftCustomerPriceItemCount}>
                  고객 단가표 후보 반영
                </Button>
              </Space>
              <Divider />
              <Typography.Paragraph className="raw-text-block">{selectedOrderDraft.rawText}</Typography.Paragraph>
              {draftAttachmentList}
              {selectedOrderDraft.orderDraftStatus === 'NEEDS_REVIEW' ? (
                <Alert
                  className="status-alert"
                  type="warning"
                  message="검수 필요 초안"
                  description="AI 추출 신뢰도나 필수값을 확인한 뒤 검수 내용을 저장하면 제출할 수 있습니다."
                  showIcon
                />
              ) : null}
              {selectedOrderDraftMissingFields.length ? (
                <Alert
                  className="status-alert"
                  type="warning"
                  message="제출 전 필수값"
                  description={selectedOrderDraftMissingFields.join(', ')}
                  showIcon
                />
              ) : null}
              <Divider />
              <Form
                form={reviewForm}
                layout="vertical"
                className="review-form"
                onFinish={onSaveOrderDraftReview}
              >
                <div className="review-heading">
                  <div className="table-heading compact-heading">
                    <Typography.Title level={3}>품목 후보</Typography.Title>
                    <Button
                      icon={<SendOutlined />}
                      onClick={onExtractAiCandidates}
                      loading={extractingAiOrderDraftId === selectedOrderDraft.orderDraftId}
                    >
                      AI 후보 추출
                    </Button>
                  </div>
                  <Form.List name="itemCandidates">
                    {(fields, { add, remove }) => (
                      <>
                        <Button icon={<PlusOutlined />} onClick={() => add(emptyItemCandidate())}>
                          품목 추가
                        </Button>
                        <Space orientation="vertical" size={12} className="review-items">
                          {fields.map((field, index) => (
                            <DraftReviewItemFields
                              key={field.key}
                              field={field}
                              index={index}
                              onRemove={remove}
                            />
                          ))}
                        </Space>
                      </>
                    )}
                  </Form.List>
                </div>
                <Divider />
                <Typography.Title level={3} className="compact-title">배송 정보</Typography.Title>
                <Row gutter={12}>
                  <Col xs={24} md={12}>
                    <Form.Item name="deliveryRecipientName" label="수취인" rules={[{ required: true, message: '수취인을 입력하세요.' }]}>
                      <Input placeholder="홍길동" />
                    </Form.Item>
                  </Col>
                  <Col xs={24} md={12}>
                    <Form.Item name="deliveryRecipientPhone" label="연락처" rules={[{ required: true, message: '연락처를 입력하세요.' }]}>
                      <Input placeholder="010-0000-0000" />
                    </Form.Item>
                  </Col>
                  <Col xs={24} md={8}>
                    <Form.Item name="deliveryPostalCode" label="우편번호"><Input placeholder="12345" /></Form.Item>
                  </Col>
                  <Col xs={24} md={16}>
                    <Form.Item name="deliveryAddress" label="주소" rules={[{ required: true, message: '주소를 입력하세요.' }]}>
                      <Input placeholder="서울시 강남구 테헤란로 123" />
                    </Form.Item>
                  </Col>
                  <Col xs={24}>
                    <Form.Item name="deliveryAddressDetail" label="상세주소"><Input placeholder="4층 401호" /></Form.Item>
                  </Col>
                  <Col xs={24}>
                    <Form.Item name="deliveryRequestMemo" label="배송 요청사항">
                      <Input.TextArea rows={2} placeholder="부재 시 경비실 보관" />
                    </Form.Item>
                  </Col>
                </Row>
                <Divider />
                <Typography.Title level={3} className="compact-title">업무 메모</Typography.Title>
                <Row gutter={12}>
                  <Col xs={24} md={12}>
                    <Form.Item name="deliveryMemo" label="납품 메모">
                      <Input.TextArea rows={3} placeholder="다음 주 화요일 오전 납품 희망" />
                    </Form.Item>
                  </Col>
                  <Col xs={24} md={12}>
                    <Form.Item name="courierMemo" label="택배 메모">
                      <Input.TextArea rows={3} placeholder="파손주의 표시" />
                    </Form.Item>
                  </Col>
                </Row>
                <Space wrap>
                  <Button type="primary" htmlType="submit" icon={<SaveOutlined />} loading={savingOrderDraftReview}>
                    검수 내용 저장
                  </Button>
                  <Button
                    icon={<CheckCircleOutlined />}
                    onClick={onSubmitOrderDraft}
                    loading={submittingOrderDraft}
                    disabled={!canOperate || selectedOrderDraft.orderDraftStatus === 'SUBMITTED' || selectedOrderDraft.orderDraftStatus === 'NEEDS_REVIEW'}
                  >
                    출고표 제출
                  </Button>
                </Space>
              </Form>
            </>
          ) : (
            <div className="empty-panel">
              <FileTextOutlined />
              <span>선택된 출고표 초안이 없습니다.</span>
            </div>
          )}
        </section>
      </Col>
    </Row>
  );
}

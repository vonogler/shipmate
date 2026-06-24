import {
  Alert,
  Button,
  Col,
  Collapse,
  Descriptions,
  Divider,
  Input,
  Row,
  Select,
  Space,
  Table,
  Tag,
  Typography
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { DownloadOutlined, FileTextOutlined, PrinterOutlined } from '@ant-design/icons';
import type {
  CourierCsvTemplate,
  CourierCsvValidationError,
  ItemSnapshotChangeType,
  ItemSnapshotDiffRow,
  OrderDraftStatus,
  ShippingOrder,
  ShippingOrderAuditSection,
  SourceChannel
} from '../types';

type ShippingScreenProps = {
  active: boolean;
  shippingDaysFilter: number | 'ALL';
  shippingCsvFilter: 'ALL' | 'VALID' | 'MISSING';
  shippingSearchText: string;
  courierCsvValidationErrors: CourierCsvValidationError[];
  shippingOrderColumns: ColumnsType<ShippingOrder>;
  filteredShippingOrders: ShippingOrder[];
  loadingShippingOrders: boolean;
  selectedShippingOrderId: string | null;
  selectedShippingOrder: ShippingOrder | null;
  selectedShippingOrderMissingFields: string[];
  selectedShippingOrderSubmissionCount: number;
  selectedShippingOrderAuditSections: ShippingOrderAuditSection[];
  shippingOrderDiffFieldFilter: string;
  courierCsvTemplate: CourierCsvTemplate;
  courierCsvTemplateLabels: Record<CourierCsvTemplate, string>;
  downloadingCourierCsvId: string | null;
  reopeningShippingOrderId: string | null;
  bulkCsvDownloading: boolean;
  canOperate: boolean;
  onShippingDaysFilterChange: (value: number | 'ALL') => void;
  onShippingCsvFilterChange: (value: 'ALL' | 'VALID' | 'MISSING') => void;
  onShippingSearchTextChange: (value: string) => void;
  onBulkCourierCsvDownload: () => void;
  onCourierCsvValidate: () => void;
  onSelectedShippingOrderChange: (orderDraftId: string | null) => void;
  onCourierCsvTemplateChange: (value: CourierCsvTemplate) => void;
  onCourierCsvDownload: (shippingOrder: ShippingOrder) => void;
  onReopenShippingOrderReview: (shippingOrder: ShippingOrder) => void;
  onPrintView: (shippingOrder: ShippingOrder, printType: 'shipping' | 'work' | 'delivery') => void;
  onDiffFieldFilterChange: (value: string) => void;
  formatDate: (value: string | null) => string;
  formatCurrency: (value?: number | null) => string;
  auditActionText: (actionType: string) => string;
  auditMetadataText: (value: string | null | undefined) => string;
  filteredItemDiffRows: (rows: ItemSnapshotDiffRow[], fieldFilter: string) => ItemSnapshotDiffRow[];
  diffChangeTypeText: (changeType: ItemSnapshotChangeType) => string;
  diffChangeTypeColor: (changeType: ItemSnapshotChangeType) => string;
  orderDraftStatusLabels: Record<OrderDraftStatus, string>;
  orderDraftStatusColors: Record<OrderDraftStatus, string>;
  sourceChannelLabels: Record<SourceChannel, string>;
};

export default function ShippingScreen({
  active,
  shippingDaysFilter,
  shippingCsvFilter,
  shippingSearchText,
  courierCsvValidationErrors,
  shippingOrderColumns,
  filteredShippingOrders,
  loadingShippingOrders,
  selectedShippingOrderId,
  selectedShippingOrder,
  selectedShippingOrderMissingFields,
  selectedShippingOrderSubmissionCount,
  selectedShippingOrderAuditSections,
  shippingOrderDiffFieldFilter,
  courierCsvTemplate,
  courierCsvTemplateLabels,
  downloadingCourierCsvId,
  reopeningShippingOrderId,
  bulkCsvDownloading,
  canOperate,
  onShippingDaysFilterChange,
  onShippingCsvFilterChange,
  onShippingSearchTextChange,
  onBulkCourierCsvDownload,
  onCourierCsvValidate,
  onSelectedShippingOrderChange,
  onCourierCsvTemplateChange,
  onCourierCsvDownload,
  onReopenShippingOrderReview,
  onPrintView,
  onDiffFieldFilterChange,
  formatDate,
  formatCurrency,
  auditActionText,
  auditMetadataText,
  filteredItemDiffRows,
  diffChangeTypeText,
  diffChangeTypeColor,
  orderDraftStatusLabels,
  orderDraftStatusColors,
  sourceChannelLabels
}: ShippingScreenProps) {
  return (
    <Row gutter={[16, 16]} align="stretch" className={`navigation-screen ${active ? 'is-active' : ''}`}>
      <Col xs={24} xl={14}>
        <section className="work-panel full-height">
          <div className="table-heading">
            <div>
              <Typography.Title level={2}>제출된 출고표</Typography.Title>
              <Typography.Text type="secondary">거래처, 수취인, 품목, 기간, CSV 필수값 기준으로 조회합니다.</Typography.Text>
            </div>
            <Space wrap>
              <Select
                value={shippingDaysFilter}
                onChange={onShippingDaysFilterChange}
                className="courier-template-select"
                options={[
                  { value: 'ALL', label: '전체 기간' },
                  { value: 1, label: '오늘' },
                  { value: 7, label: '7일' },
                  { value: 30, label: '30일' }
                ]}
              />
              <Select
                value={shippingCsvFilter}
                onChange={onShippingCsvFilterChange}
                className="courier-template-select"
                options={[
                  { value: 'ALL', label: 'CSV 전체' },
                  { value: 'VALID', label: 'CSV 가능' },
                  { value: 'MISSING', label: 'CSV 필수값 누락' }
                ]}
              />
              <Input.Search
                allowClear
                value={shippingSearchText}
                onChange={(event) => onShippingSearchTextChange(event.target.value)}
                placeholder="거래처, 수취인, 품목 검색"
              />
              <Button icon={<DownloadOutlined />} onClick={onBulkCourierCsvDownload} loading={bulkCsvDownloading}>
                일괄 CSV
              </Button>
              <Button onClick={onCourierCsvValidate}>
                {selectedShippingOrder ? '선택 CSV 검증' : '목록 CSV 검증'}
              </Button>
            </Space>
          </div>
          {courierCsvValidationErrors.length ? (
            <Alert
              className="status-alert"
              type="warning"
              message="CSV 검증 결과"
              description={(
                <ul className="validation-list">
                  {courierCsvValidationErrors.map((validationError, index) => {
                    const key = [
                      validationError.orderDraftId ?? 'all',
                      validationError.fieldName,
                      validationError.message,
                      index
                    ].join(':');
                    return (
                      <li key={key}>
                        {validationError.orderDraftId ? (
                          <Button type="link" onClick={() => onSelectedShippingOrderChange(validationError.orderDraftId)}>
                            {validationError.orderDraftId}
                          </Button>
                        ) : null}
                        <Typography.Text>
                          {validationError.csvColumnName
                            ? ` ${validationError.csvColumnName}(${validationError.fieldName}): `
                            : validationError.fieldName !== '-' ? ` ${validationError.fieldName}: ` : ' '}
                          {validationError.message}
                        </Typography.Text>
                      </li>
                    );
                  })}
                </ul>
              )}
              showIcon
            />
          ) : null}
          <Table
            rowKey="orderDraftId"
            columns={shippingOrderColumns}
            dataSource={filteredShippingOrders}
            loading={loadingShippingOrders}
            size="middle"
            scroll={{ x: 780 }}
            pagination={{ pageSize: 8, showSizeChanger: false }}
            rowSelection={{
              type: 'radio',
              selectedRowKeys: selectedShippingOrderId ? [selectedShippingOrderId] : [],
              onChange: (keys) => onSelectedShippingOrderChange(String(keys[0]))
            }}
            onRow={(row) => ({
              onClick: () => onSelectedShippingOrderChange(row.orderDraftId)
            })}
          />
        </section>
      </Col>
      <Col xs={24} xl={10}>
        <section className="work-panel full-height">
          <Typography.Title level={2}>출고표 상세</Typography.Title>
          {selectedShippingOrder ? (
            <>
              <div className="table-heading compact-heading">
                <Select
                  value={courierCsvTemplate}
                  onChange={onCourierCsvTemplateChange}
                  className="courier-template-select"
                  options={Object.entries(courierCsvTemplateLabels).map(([value, label]) => ({ value, label }))}
                />
                <Button
                  icon={<DownloadOutlined />}
                  onClick={() => onCourierCsvDownload(selectedShippingOrder)}
                  loading={downloadingCourierCsvId === selectedShippingOrder.orderDraftId}
                >
                  택배 CSV
                </Button>
                <Button
                  onClick={() => onReopenShippingOrderReview(selectedShippingOrder)}
                  loading={reopeningShippingOrderId === selectedShippingOrder.orderDraftId}
                  disabled={!canOperate}
                >
                  재검수 전환
                </Button>
              </div>
              {selectedShippingOrderMissingFields.length ? (
                <Alert
                  className="status-alert"
                  type="warning"
                  message="CSV 필수값 누락 가능"
                  description={selectedShippingOrderMissingFields.join(', ')}
                  showIcon
                />
              ) : (
                <Alert
                  className="status-alert"
                  type="success"
                  message="CSV 필수값 확인 완료"
                  description="수취인, 연락처, 주소, 품목명, 수량, 단위가 입력되어 있습니다."
                  showIcon
                />
              )}
              <Alert
                className="status-alert"
                type="info"
                message="출고 처리 확인"
                description={shippingOperationGuide(selectedShippingOrderMissingFields.length, selectedShippingOrderSubmissionCount)}
                showIcon
              />
              <Space wrap className="print-actions">
                <Button icon={<PrinterOutlined />} onClick={() => onPrintView(selectedShippingOrder, 'shipping')}>
                  출고표 인쇄
                </Button>
                <Button icon={<PrinterOutlined />} onClick={() => onPrintView(selectedShippingOrder, 'work')}>
                  작업지시서
                </Button>
                <Button icon={<PrinterOutlined />} onClick={() => onPrintView(selectedShippingOrder, 'delivery')}>
                  납품서
                </Button>
              </Space>
              <Descriptions size="small" column={1} bordered>
                <Descriptions.Item label="출고표 ID">{selectedShippingOrder.orderDraftId}</Descriptions.Item>
                <Descriptions.Item label="상태">
                  <Tag color={orderDraftStatusColors[selectedShippingOrder.orderDraftStatus]}>
                    {orderDraftStatusLabels[selectedShippingOrder.orderDraftStatus]}
                  </Tag>
                </Descriptions.Item>
                <Descriptions.Item label="거래처">{selectedShippingOrder.customerId || '-'}</Descriptions.Item>
                <Descriptions.Item label="입력 채널">
                  {sourceChannelLabels[selectedShippingOrder.sourceChannel]}
                </Descriptions.Item>
                <Descriptions.Item label="수취인">{selectedShippingOrder.deliveryRecipientName || '-'}</Descriptions.Item>
                <Descriptions.Item label="연락처">{selectedShippingOrder.deliveryRecipientPhone || '-'}</Descriptions.Item>
                <Descriptions.Item label="우편번호">{selectedShippingOrder.deliveryPostalCode || '-'}</Descriptions.Item>
                <Descriptions.Item label="주소">
                  {[selectedShippingOrder.deliveryAddress, selectedShippingOrder.deliveryAddressDetail]
                    .filter(Boolean)
                    .join(' ') || '-'}
                </Descriptions.Item>
                <Descriptions.Item label="배송 요청사항">{selectedShippingOrder.deliveryRequestMemo || '-'}</Descriptions.Item>
                <Descriptions.Item label="제출 시각">{formatDate(selectedShippingOrder.updatedAt)}</Descriptions.Item>
                <Descriptions.Item label="제출 횟수">
                  {selectedShippingOrderSubmissionCount ? `${selectedShippingOrderSubmissionCount}회` : '-'}
                </Descriptions.Item>
              </Descriptions>
              <Divider />
              <div className="table-heading compact-heading">
                <Typography.Title level={3} className="compact-title">변경 이력</Typography.Title>
                <Select
                  size="small"
                  value={shippingOrderDiffFieldFilter}
                  onChange={onDiffFieldFilterChange}
                  className="filter-select"
                  options={[
                    { value: 'ALL', label: '전체 항목' },
                    { value: '최초 제출', label: '최초 제출' },
                    { value: '품목', label: '품목 추가/삭제' },
                    { value: '품목명', label: '품목명' },
                    { value: '규격', label: '규격' },
                    { value: '수량', label: '수량' },
                    { value: '단가', label: '단가' },
                    { value: '품목 순서', label: '품목 순서' }
                  ]}
                />
              </div>
              <Space id="shipping-order-item-diff" direction="vertical" size={8} className="review-items">
                {selectedShippingOrderAuditSections.length ? (
                  selectedShippingOrderAuditSections.map((section) => (
                    <div className="submitted-item-row" key={section.title}>
                      <Typography.Text strong>{section.title}</Typography.Text>
                      <Collapse
                        size="small"
                        ghost
                        items={[{
                          key: `${section.title}-diff`,
                          label: `품목 변경 ${filteredItemDiffRows(section.itemDiffRows, shippingOrderDiffFieldFilter).length}건`,
                          children: filteredItemDiffRows(section.itemDiffRows, shippingOrderDiffFieldFilter).length ? (
                            <Table
                              size="small"
                              rowKey="rowKey"
                              columns={[
                                { title: '품목', dataIndex: 'itemLabel', width: 160 },
                                {
                                  title: '항목',
                                  dataIndex: 'fieldLabel',
                                  width: 170,
                                  render: (fieldLabel: string, row: ItemSnapshotDiffRow) => (
                                    <Space>
                                      <Tag color={diffChangeTypeColor(row.changeType)}>{diffChangeTypeText(row.changeType)}</Tag>
                                      <Typography.Text>{fieldLabel}</Typography.Text>
                                    </Space>
                                  )
                                },
                                { title: '이전 값', dataIndex: 'previousValue' },
                                { title: '현재 값', dataIndex: 'nextValue' }
                              ]}
                              dataSource={filteredItemDiffRows(section.itemDiffRows, shippingOrderDiffFieldFilter)}
                              pagination={false}
                            />
                          ) : (
                            <Typography.Text type="secondary">품목 단위 변경이 없습니다.</Typography.Text>
                          )
                        }]}
                      />
                      {section.logs.map((auditLog) => (
                        <Space direction="vertical" size={2} key={auditLog.auditLogId}>
                          <Space wrap>
                            <Tag>{auditActionText(auditLog.actionType)}</Tag>
                            <Typography.Text type="secondary">{formatDate(auditLog.createdAt)}</Typography.Text>
                          </Space>
                          <Typography.Text>{auditLog.summary}</Typography.Text>
                          {auditLog.auditMetadataJson ? (
                            <Typography.Text type="secondary">
                              부가 정보: {auditMetadataText(auditLog.auditMetadataJson)}
                            </Typography.Text>
                          ) : null}
                        </Space>
                      ))}
                    </div>
                  ))
                ) : (
                  <Typography.Text type="secondary">표시할 변경 이력이 없습니다.</Typography.Text>
                )}
              </Space>
              <Divider />
              <Typography.Title level={3} className="compact-title">품목</Typography.Title>
              <Space orientation="vertical" size={8} className="review-items">
                {selectedShippingOrder.itemCandidates.length ? (
                  selectedShippingOrder.itemCandidates.map((itemCandidate, index) => (
                    <div className="submitted-item-row" key={itemCandidate.orderDraftItemCandidateId ?? index}>
                      <Typography.Text strong>{itemCandidate.itemName || `품목 ${index + 1}`}</Typography.Text>
                      <Typography.Text type="secondary">
                        {[itemCandidate.itemSpec, itemCandidate.itemQuantity, itemCandidate.itemUnit]
                          .filter(Boolean)
                          .join(' / ') || '-'}
                      </Typography.Text>
                      <Typography.Text>
                        단가 후보: {formatCurrency(itemCandidate.matchedUnitPrice)}
                      </Typography.Text>
                      <Typography.Text type="secondary">
                        포장 규격: {[itemCandidate.packagingWidth, itemCandidate.packagingDepth, itemCandidate.packagingHeight]
                          .filter(Boolean)
                          .join(' x ') || '-'}
                      </Typography.Text>
                      <Typography.Text type="secondary">
                        재질/제작: {[itemCandidate.materialName, itemCandidate.productionQuantity, itemCandidate.requestedDueDate]
                          .filter(Boolean)
                          .join(' / ') || '-'}
                      </Typography.Text>
                      <Typography.Text type="secondary">
                        맞춤 옵션: {[itemCandidate.customProductType, itemCandidate.imprintMethod, itemCandidate.itemColor, itemCandidate.customOptionText]
                          .filter(Boolean)
                          .join(' / ') || '-'}
                      </Typography.Text>
                      {itemCandidate.lineMemo ? (
                        <Typography.Text type="secondary">{itemCandidate.lineMemo}</Typography.Text>
                      ) : null}
                    </div>
                  ))
                ) : (
                  <Typography.Text type="secondary">등록된 품목이 없습니다.</Typography.Text>
                )}
              </Space>
              <Divider />
              <Descriptions size="small" column={1} bordered>
                <Descriptions.Item label="납품 메모">{selectedShippingOrder.deliveryMemo || '-'}</Descriptions.Item>
                <Descriptions.Item label="택배 메모">{selectedShippingOrder.courierMemo || '-'}</Descriptions.Item>
              </Descriptions>
            </>
          ) : (
            <div className="empty-panel">
              <FileTextOutlined />
              <span>선택된 출고표가 없습니다.</span>
            </div>
          )}
        </section>
      </Col>
    </Row>
  );
}

function shippingOperationGuide(missingFieldCount: number, submissionCount: number) {
  if (missingFieldCount > 0) {
    return '택배 CSV를 내려받기 전에 누락 항목을 재검수 전환으로 보정하세요.';
  }
  if (submissionCount > 1) {
    return '재제출된 출고표입니다. 변경 이력에서 이전 제출 대비 품목 차이를 확인한 뒤 출고하세요.';
  }
  return '택배 CSV, 출고표, 작업지시서, 납품서를 같은 상세 화면에서 확인할 수 있습니다.';
}

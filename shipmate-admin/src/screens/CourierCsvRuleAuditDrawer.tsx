import { Descriptions, Drawer, Space, Switch, Table, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { ReactNode } from 'react';
import type { AuditLog, CourierCsvValidationRule } from '../types';

type CourierCsvRuleAuditDrawerProps = {
  selectedCsvRule: CourierCsvValidationRule | null;
  auditLogs: AuditLog[];
  loading: boolean;
  changedOnly: boolean;
  onChangedOnlyChange: (checked: boolean) => void;
  onClose: () => void;
  formatDate: (value: string | null) => string;
  auditActionText: (actionType: string) => string;
  courierCsvRuleAuditDiffText: (value: string | null | undefined, changedOnly: boolean) => ReactNode;
  courierCsvRuleAuditMetadataRaw: (value: string | null | undefined) => ReactNode;
};

export default function CourierCsvRuleAuditDrawer({
  selectedCsvRule,
  auditLogs,
  loading,
  changedOnly,
  onChangedOnlyChange,
  onClose,
  formatDate,
  auditActionText,
  courierCsvRuleAuditDiffText,
  courierCsvRuleAuditMetadataRaw
}: CourierCsvRuleAuditDrawerProps) {
  const columns: ColumnsType<AuditLog> = [
    { title: '시각', dataIndex: 'createdAt', width: 170, render: formatDate },
    { title: '액션', dataIndex: 'actionType', width: 180, render: auditActionText },
    { title: '작업자', dataIndex: 'actorId', width: 140 },
    { title: '요약', dataIndex: 'summary' },
    {
      title: '변경 전/후',
      dataIndex: 'auditMetadataJson',
      width: 320,
      render: (value: string | null | undefined) => courierCsvRuleAuditDiffText(value, changedOnly)
    },
    {
      title: 'metadata 원문',
      dataIndex: 'auditMetadataJson',
      width: 260,
      render: courierCsvRuleAuditMetadataRaw
    }
  ];

  return (
    <Drawer
      title="택배 CSV 검증 규칙 변경 이력"
      open={selectedCsvRule !== null}
      onClose={onClose}
      width={720}
      extra={(
        <Space>
          <Typography.Text type="secondary">변경 항목만</Typography.Text>
          <Switch checked={changedOnly} onChange={onChangedOnlyChange} />
        </Space>
      )}
    >
      {selectedCsvRule ? (
        <Space direction="vertical" size={16} className="content-stack">
          <Descriptions column={1} size="small" bordered>
            <Descriptions.Item label="규칙 ID">{selectedCsvRule.courierCsvValidationRuleId}</Descriptions.Item>
            <Descriptions.Item label="택배 템플릿">{selectedCsvRule.courierCsvTemplate}</Descriptions.Item>
            <Descriptions.Item label="필드">{selectedCsvRule.fieldName}</Descriptions.Item>
            <Descriptions.Item label="CSV 컬럼">{selectedCsvRule.csvColumnName || '-'}</Descriptions.Item>
            <Descriptions.Item label="상태">{selectedCsvRule.active ? '사용' : '중지'}</Descriptions.Item>
          </Descriptions>
          <Table
            size="small"
            rowKey="auditLogId"
            loading={loading}
            columns={columns}
            dataSource={auditLogs}
            pagination={false}
          />
        </Space>
      ) : null}
    </Drawer>
  );
}

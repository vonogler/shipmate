import type { ReactNode } from 'react';
import { Space, Table, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';

type SettingsScreenProps<TSetting extends object, TCsvRule extends object> = {
  settings: TSetting[];
  settingColumns: ColumnsType<any>;
  courierCsvValidationRules: TCsvRule[];
  courierCsvValidationRuleColumns: ColumnsType<any>;
  courierCsvToolbar?: ReactNode;
};

export default function SettingsScreen<TSetting extends object, TCsvRule extends object>({
  settings,
  settingColumns,
  courierCsvValidationRules,
  courierCsvValidationRuleColumns,
  courierCsvToolbar
}: SettingsScreenProps<TSetting, TCsvRule>) {
  return (
    <Space direction="vertical" size={16} className="content-stack">
      <SettingsTable title="시스템 설정" rows={settings} columns={settingColumns} x={920} />
      <SettingsTable
        title="택배 CSV 검증 규칙"
        rows={courierCsvValidationRules}
        columns={courierCsvValidationRuleColumns}
        x={1380}
        toolbar={courierCsvToolbar}
      />
    </Space>
  );
}

function SettingsTable<T extends object>({
  title,
  rows,
  columns,
  x,
  toolbar
}: {
  title: string;
  rows: T[];
  columns: ColumnsType<any>;
  x: number;
  toolbar?: ReactNode;
}) {
  return (
    <section className="work-panel">
      <div className="table-heading">
        <Typography.Title level={2}>{title}</Typography.Title>
        <Space wrap>
          {toolbar}
          <Typography.Text type="secondary">총 {rows.length.toLocaleString()}건</Typography.Text>
        </Space>
      </div>
      <Table
        rowKey={settingsRowKey}
        columns={columns}
        dataSource={rows}
        scroll={{ x }}
        pagination={{ pageSize: 15, showSizeChanger: false }}
      />
    </section>
  );
}

function settingsRowKey(row: object) {
  const source = row as Record<string, unknown>;
  return String(
    source.merchantId
    ?? source.courierCsvValidationRuleId
    ?? JSON.stringify(source)
  );
}

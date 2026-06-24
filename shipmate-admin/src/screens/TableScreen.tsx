import type { ReactNode } from 'react';
import { Space, Table, Typography } from 'antd';
import type { TableProps } from 'antd';
import type { ColumnsType } from 'antd/es/table';

type TableScreenProps = {
  title: string;
  rows: object[];
  columns: ColumnsType<any>;
  x: number;
  toolbar?: ReactNode;
  total?: number;
  pagination?: TableProps<any>['pagination'];
  rowSelection?: TableProps<any>['rowSelection'];
};

export default function TableScreen({
  title,
  rows,
  columns,
  x,
  toolbar,
  total,
  pagination,
  rowSelection
}: TableScreenProps) {
  return (
    <section className="work-panel">
      <div className="table-heading">
        <Typography.Title level={2}>{title}</Typography.Title>
        <Space wrap>
          {toolbar}
          <Typography.Text type="secondary">총 {(total ?? rows.length).toLocaleString()}건</Typography.Text>
        </Space>
      </div>
      <Table
        rowKey={tableRowKey}
        columns={columns}
        dataSource={rows}
        scroll={{ x }}
        pagination={pagination ?? { pageSize: 15, showSizeChanger: false }}
        rowSelection={rowSelection}
      />
    </section>
  );
}

function tableRowKey(row: object) {
  const source = row as Record<string, unknown>;
  return String(
    source.merchantId
    ?? source.merchantUserId
    ?? source.inboundMessageId
    ?? source.orderDraftId
    ?? source.recordingId
    ?? source.errorLogId
    ?? source.auditLogId
    ?? source.gmailPollingRunId
    ?? JSON.stringify(source)
  );
}

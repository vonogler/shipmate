import { Input, Select, Space } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import TableScreen from './TableScreen';
import type { AdminMerchant, InboundMessage, OrderDraft } from '../types';

type PeriodFilter = number | 'ALL';

export function MerchantsScreen({
  rows,
  columns
}: {
  rows: AdminMerchant[];
  columns: ColumnsType<AdminMerchant>;
}) {
  return <TableScreen title="이용자/가맹점 관리" rows={rows} columns={columns} x={1720} />;
}

export function InboundMessagesScreen({
  rows,
  columns,
  searchText,
  onSearchTextChange
}: {
  rows: InboundMessage[];
  columns: ColumnsType<InboundMessage>;
  searchText: string;
  onSearchTextChange: (value: string) => void;
}) {
  return (
    <TableScreen
      title="주문 원문 관리"
      rows={rows}
      columns={columns}
      x={1420}
      toolbar={(
        <Input.Search
          allowClear
          value={searchText}
          onChange={(event) => onSearchTextChange(event.target.value)}
          placeholder="원문 ID, 이용자, 고객, 상태, 원문 검색"
        />
      )}
    />
  );
}

export function OrderDraftsScreen({
  rows,
  allRows,
  columns,
  statusFilter,
  daysFilter,
  searchText,
  onStatusFilterChange,
  onDaysFilterChange,
  onSearchTextChange
}: {
  rows: OrderDraft[];
  allRows: OrderDraft[];
  columns: ColumnsType<OrderDraft>;
  statusFilter: string;
  daysFilter: PeriodFilter;
  searchText: string;
  onStatusFilterChange: (value: string) => void;
  onDaysFilterChange: (value: PeriodFilter) => void;
  onSearchTextChange: (value: string) => void;
}) {
  return (
    <TableScreen
      title="출고표 초안 관리"
      rows={rows}
      columns={columns}
      x={1260}
      toolbar={(
        <Space wrap>
          <Select
            value={statusFilter}
            onChange={onStatusFilterChange}
            className="filter-select"
            options={[
              { value: 'ALL', label: '전체 상태' },
              ...uniqueValues(allRows.map((row) => row.orderDraftStatus)).map((value) => ({ value, label: value }))
            ]}
          />
          <Select
            value={daysFilter}
            onChange={onDaysFilterChange}
            className="filter-select"
            options={periodOptions()}
          />
          <Input.Search
            allowClear
            value={searchText}
            onChange={(event) => onSearchTextChange(event.target.value)}
            placeholder="이용자, 거래처, 수취인, 품목, 원문 검색"
          />
        </Space>
      )}
    />
  );
}

export function ShippingOrdersScreen({
  rows,
  columns,
  daysFilter,
  csvFilter,
  searchText,
  onDaysFilterChange,
  onCsvFilterChange,
  onSearchTextChange
}: {
  rows: OrderDraft[];
  columns: ColumnsType<OrderDraft>;
  daysFilter: PeriodFilter;
  csvFilter: 'ALL' | 'VALID' | 'MISSING';
  searchText: string;
  onDaysFilterChange: (value: PeriodFilter) => void;
  onCsvFilterChange: (value: 'ALL' | 'VALID' | 'MISSING') => void;
  onSearchTextChange: (value: string) => void;
}) {
  return (
    <TableScreen
      title="제출 출고표 관리"
      rows={rows}
      columns={columns}
      x={1260}
      toolbar={(
        <Space wrap>
          <Select
            value={daysFilter}
            onChange={onDaysFilterChange}
            className="filter-select"
            options={periodOptions()}
          />
          <Select
            value={csvFilter}
            onChange={onCsvFilterChange}
            className="filter-select"
            options={[
              { value: 'ALL', label: 'CSV 전체' },
              { value: 'VALID', label: 'CSV 가능' },
              { value: 'MISSING', label: 'CSV 필수값 누락' }
            ]}
          />
          <Input.Search
            allowClear
            value={searchText}
            onChange={(event) => onSearchTextChange(event.target.value)}
            placeholder="이용자, 거래처, 수취인, 품목, 원문 검색"
          />
        </Space>
      )}
    />
  );
}

function periodOptions() {
  return [
    { value: 'ALL', label: '전체 기간' },
    { value: 1, label: '오늘' },
    { value: 7, label: '7일' },
    { value: 30, label: '30일' }
  ];
}

function uniqueValues(values: string[]) {
  return Array.from(new Set(values.filter(Boolean))).sort();
}

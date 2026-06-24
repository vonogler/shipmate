import { Input, Select } from 'antd';
import type { CourierCsvValidationRule } from '../types';

type CourierCsvRuleInlineFieldProps = {
  row: CourierCsvValidationRule;
  loading: boolean;
  onUpdate: (row: CourierCsvValidationRule, values: Partial<CourierCsvValidationRule>) => void;
};

export function CourierCsvColumnNameField({ row, loading, onUpdate }: CourierCsvRuleInlineFieldProps) {
  const csvColumnName = row.csvColumnName ?? '';
  return (
    <Input
      size="small"
      defaultValue={csvColumnName}
      disabled={loading}
      onBlur={(event) => {
        const nextValue = event.currentTarget.value;
        if (nextValue !== csvColumnName) {
          onUpdate(row, { csvColumnName: nextValue });
        }
      }}
    />
  );
}

export function CourierCsvRequiredField({ row, loading, onUpdate }: CourierCsvRuleInlineFieldProps) {
  return (
    <Select
      size="small"
      value={row.required ? 'true' : 'false'}
      disabled={loading}
      onChange={(value) => onUpdate(row, { required: value === 'true' })}
      options={[
        { value: 'true', label: '필수' },
        { value: 'false', label: '선택' }
      ]}
    />
  );
}

export function CourierCsvErrorMessageField({ row, loading, onUpdate }: CourierCsvRuleInlineFieldProps) {
  return (
    <Input
      size="small"
      defaultValue={row.errorMessage}
      disabled={loading}
      onBlur={(event) => {
        const nextValue = event.currentTarget.value;
        if (nextValue && nextValue !== row.errorMessage) {
          onUpdate(row, { errorMessage: nextValue });
        }
      }}
    />
  );
}

export function CourierCsvSortOrderField({ row, loading, onUpdate }: CourierCsvRuleInlineFieldProps) {
  return (
    <Input
      size="small"
      type="number"
      defaultValue={row.sortOrder}
      disabled={loading}
      onBlur={(event) => {
        const nextValue = Number(event.currentTarget.value);
        if (Number.isFinite(nextValue) && nextValue !== row.sortOrder) {
          onUpdate(row, { sortOrder: nextValue });
        }
      }}
    />
  );
}

export function CourierCsvActiveField({ row, loading, onUpdate }: CourierCsvRuleInlineFieldProps) {
  return (
    <Select
      size="small"
      value={row.active ? 'true' : 'false'}
      disabled={loading}
      onChange={(value) => onUpdate(row, { active: value === 'true' })}
      options={[
        { value: 'true', label: '사용' },
        { value: 'false', label: '중지' }
      ]}
    />
  );
}

import { Input, Select } from 'antd';
import type { ErrorLog } from '../types';

type ErrorLogInlineFieldsProps = {
  row: ErrorLog;
  loading: boolean;
  onUpdate: (row: ErrorLog, values: Partial<Pick<ErrorLog, 'handlingStatus' | 'assignedActorId' | 'handlingMemo'>>) => void;
};

export function ErrorLogHandlingStatusField({ row, loading, onUpdate }: ErrorLogInlineFieldsProps) {
  return (
    <Select
      size="small"
      value={row.handlingStatus ?? 'OPEN'}
      disabled={loading}
      onChange={(value) => onUpdate(row, { handlingStatus: value })}
      options={[
        { value: 'OPEN', label: '미확인' },
        { value: 'ACKNOWLEDGED', label: '확인' },
        { value: 'RESOLVED', label: '조치 완료' }
      ]}
    />
  );
}

export function ErrorLogAssignedActorField({ row, loading, onUpdate }: ErrorLogInlineFieldsProps) {
  const assignedActorId = row.assignedActorId ?? '';
  return (
    <Input
      size="small"
      defaultValue={assignedActorId}
      disabled={loading}
      onBlur={(event) => {
        const nextActor = event.currentTarget.value;
        if (nextActor !== assignedActorId) {
          onUpdate(row, { assignedActorId: nextActor });
        }
      }}
    />
  );
}

export function ErrorLogHandlingMemoField({ row, loading, onUpdate }: ErrorLogInlineFieldsProps) {
  const handlingMemo = row.handlingMemo ?? '';
  return (
    <Input.TextArea
      autoSize={{ minRows: 1, maxRows: 3 }}
      defaultValue={handlingMemo}
      disabled={loading}
      onBlur={(event) => {
        const nextMemo = event.currentTarget.value;
        if (nextMemo !== handlingMemo) {
          onUpdate(row, { handlingMemo: nextMemo });
        }
      }}
    />
  );
}

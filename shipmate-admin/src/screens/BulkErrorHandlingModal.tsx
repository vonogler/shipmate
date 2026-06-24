import { Alert, Form, Input, Modal } from 'antd';
import type { FormInstance } from 'antd';
import type { BulkErrorHandlingFormValues } from '../types';

type BulkErrorHandlingPreview = {
  total: number;
  open: number;
  acknowledged: number;
  resolved: number;
};

type BulkErrorHandlingModalProps = {
  handlingStatus: string | null;
  loading: boolean;
  preview: BulkErrorHandlingPreview;
  form: FormInstance<BulkErrorHandlingFormValues>;
  onCancel: () => void;
  onSubmit: (values: BulkErrorHandlingFormValues) => void;
  errorHandlingStatusText: (status: string) => string;
};

export default function BulkErrorHandlingModal({
  handlingStatus,
  loading,
  preview,
  form,
  onCancel,
  onSubmit,
  errorHandlingStatusText
}: BulkErrorHandlingModalProps) {
  return (
    <Modal
      title={handlingStatus === 'RESOLVED' ? '선택 오류 조치 완료' : '선택 오류 확인'}
      open={handlingStatus !== null}
      confirmLoading={loading}
      okText="저장"
      cancelText="취소"
      onCancel={onCancel}
      onOk={() => form.validateFields().then(onSubmit)}
    >
      <Alert
        type="info"
        showIcon
        className="modal-preview"
        message={`선택 ${preview.total}건을 ${errorHandlingStatusText(handlingStatus ?? 'ACKNOWLEDGED')} 상태로 변경합니다.`}
        description={`현재 상태: 미확인 ${preview.open}건, 확인 ${preview.acknowledged}건, 조치 완료 ${preview.resolved}건`}
      />
      <Form form={form} layout="vertical">
        <Form.Item name="assignedActorId" label="담당자">
          <Input />
        </Form.Item>
        <Form.Item name="handlingMemo" label="처리 메모">
          <Input.TextArea autoSize={{ minRows: 3, maxRows: 6 }} />
        </Form.Item>
      </Form>
    </Modal>
  );
}

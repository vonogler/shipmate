import { Button, Col, Row, Space, Typography } from 'antd';

type OperationsScreenProps = {
  auditLogs: unknown[];
  errorLogs: unknown[];
  backupPreview: string;
  onLoadOperationLogs: () => void;
  onExportBackup: () => void;
  onApproveRestore: () => void;
};

export default function OperationsScreen({
  auditLogs,
  errorLogs,
  backupPreview,
  onLoadOperationLogs,
  onExportBackup,
  onApproveRestore
}: OperationsScreenProps) {
  return (
    <section id="operations" className="work-panel navigation-screen is-active">
      <div className="table-heading">
        <div>
          <Typography.Title level={2}>운영</Typography.Title>
          <Typography.Text type="secondary">변경 이력, 오류 로그, 백업 데이터를 확인합니다.</Typography.Text>
        </div>
        <Space wrap>
          <Button onClick={onLoadOperationLogs}>로그 조회</Button>
          <Button onClick={onExportBackup}>백업 보기</Button>
          <Button danger onClick={onApproveRestore}>복구 승인</Button>
        </Space>
      </div>
      <Row gutter={[16, 16]}>
        <Col xs={24} lg={8}>
          <Typography.Title level={3} className="compact-title">변경 이력</Typography.Title>
          <pre className="operation-json">{JSON.stringify(auditLogs, null, 2)}</pre>
        </Col>
        <Col xs={24} lg={8}>
          <Typography.Title level={3} className="compact-title">오류 로그</Typography.Title>
          <pre className="operation-json">{JSON.stringify(errorLogs, null, 2)}</pre>
        </Col>
        <Col xs={24} lg={8}>
          <Typography.Title level={3} className="compact-title">백업 미리보기</Typography.Title>
          <pre className="operation-json">{backupPreview}</pre>
        </Col>
      </Row>
    </section>
  );
}

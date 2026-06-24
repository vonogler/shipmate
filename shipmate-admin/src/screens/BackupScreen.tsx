import { Button, Descriptions, Typography } from 'antd';
import { DatabaseOutlined } from '@ant-design/icons';

type BackupResponse = {
  backupId: string;
  files: Record<string, unknown>;
};

type BackupScreenProps = {
  backup: BackupResponse | null;
  onLoadBackup: () => void;
};

export default function BackupScreen({ backup, onLoadBackup }: BackupScreenProps) {
  return (
    <section className="work-panel">
      <div className="table-heading">
        <div>
          <Typography.Title level={2}>백업/복구</Typography.Title>
          <Typography.Text type="secondary">운영 데이터 백업 내용을 미리 확인합니다.</Typography.Text>
        </div>
        <Button icon={<DatabaseOutlined />} onClick={onLoadBackup}>백업 미리보기</Button>
      </div>
      {backup ? (
        <Descriptions bordered size="small" column={1}>
          <Descriptions.Item label="백업 ID">{backup.backupId}</Descriptions.Item>
          <Descriptions.Item label="파일">
            <pre className="json-block">{JSON.stringify(backup.files, null, 2)}</pre>
          </Descriptions.Item>
        </Descriptions>
      ) : <div className="empty-panel">백업 미리보기를 실행하세요.</div>}
    </section>
  );
}

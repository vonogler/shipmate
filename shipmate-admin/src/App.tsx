import { useCallback, useEffect, useMemo, useState } from 'react';
import { Alert, Button, Layout, message, Popconfirm, Space, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { DeleteOutlined, ReloadOutlined } from '@ant-design/icons';

type CallRecordingStatus =
  | 'CREATED'
  | 'UPLOAD_URL_ISSUED'
  | 'UPLOADING'
  | 'UPLOADED'
  | 'ANALYZING'
  | 'ANALYSIS_DONE'
  | 'FAILED';

type RecentCallRecording = {
  recordingId: string;
  fileName: string;
  source: string | null;
  status: CallRecordingStatus;
  fileSize: number;
  objectKey: string;
  uploadedAt: string | null;
  createdAt: string;
  errorCode: string | null;
};

const apiBaseUrl = import.meta.env.VITE_SHIPMATE_API_BASE_URL ?? 'http://localhost:8080';

const statusColors: Record<CallRecordingStatus, string> = {
  CREATED: 'default',
  UPLOAD_URL_ISSUED: 'processing',
  UPLOADING: 'processing',
  UPLOADED: 'success',
  ANALYZING: 'blue',
  ANALYSIS_DONE: 'green',
  FAILED: 'red'
};

function App() {
  const [rows, setRows] = useState<RecentCallRecording[]>([]);
  const [loading, setLoading] = useState(false);
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const loadRecentUploads = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch(`${apiBaseUrl}/api/call-recordings/recent`);
      if (!response.ok) {
        throw new Error(`API returned HTTP ${response.status}`);
      }
      setRows(await response.json());
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Unable to load recent uploads.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadRecentUploads();
  }, [loadRecentUploads]);

  const deleteRecording = useCallback(
    async (recordingId: string) => {
      setDeletingId(recordingId);
      setError(null);

      try {
        const response = await fetch(`${apiBaseUrl}/api/call-recordings/${recordingId}`, {
          method: 'DELETE'
        });
        if (!response.ok) {
          throw new Error(`Delete returned HTTP ${response.status}`);
        }
        setRows((currentRows) => currentRows.filter((row) => row.recordingId !== recordingId));
        message.success('Call recording deleted.');
      } catch (cause) {
        const description = cause instanceof Error ? cause.message : 'Unable to delete recording.';
        setError(description);
        message.error(description);
      } finally {
        setDeletingId(null);
      }
    },
    []
  );

  const columns = useMemo<ColumnsType<RecentCallRecording>>(
    () => [
      {
        title: 'Recording ID',
        dataIndex: 'recordingId',
        width: 220,
        fixed: 'left'
      },
      {
        title: 'File',
        dataIndex: 'fileName',
        width: 220
      },
      {
        title: 'Status',
        dataIndex: 'status',
        width: 150,
        render: (status: CallRecordingStatus) => (
          <Tag color={statusColors[status]}>{status.split('_').join(' ')}</Tag>
        )
      },
      {
        title: 'Source',
        dataIndex: 'source',
        width: 150,
        render: (source: string | null) => source || '-'
      },
      {
        title: 'Size',
        dataIndex: 'fileSize',
        width: 110,
        render: (fileSize: number) => formatBytes(fileSize)
      },
      {
        title: 'Uploaded At',
        dataIndex: 'uploadedAt',
        width: 210,
        render: (uploadedAt: string | null, row) => formatDate(uploadedAt ?? row.createdAt)
      },
      {
        title: 'Object Key',
        dataIndex: 'objectKey',
        width: 520
      },
      {
        title: 'Error',
        dataIndex: 'errorCode',
        width: 140,
        render: (errorCode: string | null) => errorCode || '-'
      },
      {
        title: 'Actions',
        key: 'actions',
        width: 110,
        fixed: 'right',
        render: (_, row) => (
          <Popconfirm
            title="Delete recording?"
            description="This removes the admin row and MinIO object."
            okText="Delete"
            okButtonProps={{ danger: true }}
            cancelText="Cancel"
            onConfirm={() => deleteRecording(row.recordingId)}
          >
            <Button
              danger
              icon={<DeleteOutlined />}
              loading={deletingId === row.recordingId}
              aria-label={`Delete ${row.fileName}`}
            />
          </Popconfirm>
        )
      }
    ],
    [deleteRecording, deletingId]
  );

  return (
    <Layout className="app-shell">
      <Layout.Header className="topbar">
        <Typography.Title level={3} className="brand">
          ShipMate Admin
        </Typography.Title>
        <Button icon={<ReloadOutlined />} onClick={loadRecentUploads} loading={loading}>
          Refresh
        </Button>
      </Layout.Header>
      <Layout.Content className="content">
        <Space direction="vertical" size={16} className="content-stack">
          <div className="page-heading">
            <Typography.Title level={2}>Call Recordings</Typography.Title>
            <Typography.Text type="secondary">
              Verify Android uploads, MinIO object keys, status, file size, and errors.
            </Typography.Text>
          </div>
          {error ? <Alert type="error" message="Recent upload list failed" description={error} showIcon /> : null}
          <Table
            rowKey="recordingId"
            columns={columns}
            dataSource={rows}
            loading={loading}
            scroll={{ x: 1810 }}
            pagination={{ pageSize: 20, showSizeChanger: false }}
          />
        </Space>
      </Layout.Content>
    </Layout>
  );
}

function formatBytes(bytes: number) {
  if (!bytes || bytes < 0) return '-';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

function formatDate(value: string | null) {
  if (!value) return '-';
  return new Intl.DateTimeFormat(undefined, {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value));
}

export default App;

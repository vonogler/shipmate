import { Alert, Button, Col, Row, Select, Typography } from 'antd';
import type { OperationDashboard } from '../types';

type DashboardScreenProps = {
  dashboard: OperationDashboard;
  dashboardDays: number;
  active: boolean;
  onDaysChange: (days: number) => void;
  onShowRecentInbound: (inboundMessageId?: string | null) => void;
};

export default function DashboardScreen({
  dashboard,
  dashboardDays,
  active,
  onDaysChange,
  onShowRecentInbound
}: DashboardScreenProps) {
  return (
    <section
      id="dashboard"
      className={`work-panel navigation-screen ${active ? 'is-active' : ''}`}
    >
      <div className="table-heading compact-heading">
        <Typography.Title level={2}>운영 대시보드</Typography.Title>
        <Select
          value={dashboardDays}
          onChange={onDaysChange}
          className="courier-template-select"
          options={[
            { value: 1, label: '오늘' },
            { value: 7, label: '7일' },
            { value: 30, label: '30일' }
          ]}
        />
      </div>
      <Row gutter={[12, 12]}>
        <Col xs={12} md={6}><MetricCard label="주문 원문" value={dashboard.inboundMessageCount} /></Col>
        <Col xs={12} md={6}><MetricCard label="초안" value={dashboard.orderDraftCount} /></Col>
        <Col xs={12} md={6}><MetricCard label="제출 출고표" value={dashboard.submittedShippingOrderCount} /></Col>
        <Col xs={12} md={6}><MetricCard label="오류" value={dashboard.errorLogCount} /></Col>
        <Col xs={12} md={6}><MetricCard label="Gmail 수집 실행" value={dashboard.gmailPollingRecentRunCount} /></Col>
        <Col xs={12} md={6}><MetricCard label="Gmail 수집 실패" value={dashboard.gmailPollingRecentFailureCount} /></Col>
        <Col xs={12} md={6}><MetricCard label="비주문 제외" value={dashboard.gmailPollingRecentSkippedNonOrderCount} /></Col>
      </Row>
      <Row gutter={[12, 12]} className="dashboard-detail-row">
        <Col xs={24} md={4}><pre className="operation-json compact-json">{JSON.stringify(dashboard.sourceChannelCounts, null, 2)}</pre></Col>
        <Col xs={24} md={4}><pre className="operation-json compact-json">{JSON.stringify(dashboard.orderDraftStatusCounts, null, 2)}</pre></Col>
        <Col xs={24} md={4}><pre className="operation-json compact-json">{JSON.stringify(dashboard.errorCodeCounts, null, 2)}</pre></Col>
        <Col xs={24} md={6}><pre className="operation-json compact-json">{JSON.stringify(dashboard.gmailPollingFailureCodeCounts, null, 2)}</pre></Col>
      </Row>
      {dashboard.gmailPollingLatestFailureMessage ? (
        <Alert
          className="status-alert"
          type="warning"
          message="최근 Gmail 수집 실패"
          description={dashboard.gmailPollingLatestFailureMessage}
          action={(
            <Button
              size="small"
              onClick={() => onShowRecentInbound(dashboard.gmailPollingLatestFailureInboundMessageId)}
            >
              최근 원문 보기
            </Button>
          )}
        />
      ) : null}
    </section>
  );
}

function MetricCard({ label, value }: { label: string; value: number }) {
  return (
    <div className="metric-card">
      <Typography.Text type="secondary">{label}</Typography.Text>
      <Typography.Title level={3}>{value.toLocaleString()}</Typography.Title>
    </div>
  );
}

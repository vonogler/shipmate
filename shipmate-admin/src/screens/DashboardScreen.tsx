import { Alert, Button, Col, Row, Select, Typography } from 'antd';
import type { AdminOverview } from '../types';

type DashboardScreenProps = {
  overview: AdminOverview | null;
  days: number;
  onDaysChange: (days: number) => void;
  onShowRecentInbound: (inboundMessageId?: string | null) => void;
};

export default function DashboardScreen({
  overview,
  days,
  onDaysChange,
  onShowRecentInbound
}: DashboardScreenProps) {
  return (
    <section className="work-panel">
      <div className="section-heading">
        <div>
          <Typography.Title level={2}>운영 대시보드</Typography.Title>
          <Typography.Text type="secondary">전체 서비스의 주문, 출고, 오류 흐름을 요약합니다.</Typography.Text>
        </div>
        <Select
          value={days}
          onChange={onDaysChange}
          options={[
            { value: 1, label: '오늘' },
            { value: 7, label: '7일' },
            { value: 30, label: '30일' },
            { value: 90, label: '90일' }
          ]}
        />
      </div>
      <Row gutter={[12, 12]}>
        <Col xs={12} md={6}><MetricCard label="가맹점" value={overview?.merchantCount ?? 0} /></Col>
        <Col xs={12} md={6}><MetricCard label="직원 계정" value={overview?.userCount ?? 0} /></Col>
        <Col xs={12} md={6}><MetricCard label="주문 원문" value={overview?.inboundMessageCount ?? 0} /></Col>
        <Col xs={12} md={6}><MetricCard label="출고표 초안" value={overview?.orderDraftCount ?? 0} /></Col>
        <Col xs={12} md={6}><MetricCard label="제출 출고표" value={overview?.submittedShippingOrderCount ?? 0} /></Col>
        <Col xs={12} md={6}><MetricCard label="통화 녹음" value={overview?.callRecordingCount ?? 0} /></Col>
        <Col xs={12} md={6}><MetricCard label="오류" value={overview?.errorLogCount ?? 0} /></Col>
        <Col xs={12} md={6}><MetricCard label="Gmail 수집 실행" value={overview?.gmailPollingRecentRunCount ?? 0} /></Col>
        <Col xs={12} md={6}><MetricCard label="Gmail 수집 실패" value={overview?.gmailPollingRecentFailureCount ?? 0} /></Col>
        <Col xs={12} md={6}><MetricCard label="비주문 제외" value={overview?.gmailPollingRecentSkippedNonOrderCount ?? 0} /></Col>
      </Row>
      <Row gutter={[12, 12]} className="dashboard-detail-row">
        <Col xs={24} lg={6}><JsonPanel title="입력 채널" value={overview?.sourceChannelCounts} /></Col>
        <Col xs={24} lg={6}><JsonPanel title="업종 채널" value={overview?.businessChannelCounts} /></Col>
        <Col xs={24} lg={6}><JsonPanel title="초안 상태" value={overview?.orderDraftStatusCounts} /></Col>
        <Col xs={24} lg={6}><JsonPanel title="오류 코드" value={overview?.errorCodeCounts} /></Col>
        <Col xs={24} lg={6}><JsonPanel title="실패 입력 채널" value={overview?.failedSourceChannelCounts} /></Col>
        <Col xs={24} lg={6}><JsonPanel title="실패 원인 상세" value={overview?.failedReasonBySourceChannelCounts} /></Col>
        <Col xs={24} lg={6}><JsonPanel title="오류 처리 상태" value={overview?.errorHandlingStatusCounts} /></Col>
        <Col xs={24} lg={6}><JsonPanel title="이용자 활동" value={overview?.merchantActivityCounts} /></Col>
        <Col xs={24} lg={6}><JsonPanel title="Gmail 수집 실패 코드" value={overview?.gmailPollingFailureCodeCounts} /></Col>
      </Row>
      {overview?.gmailPollingLatestFailureMessage ? (
        <Alert
          className="status-alert"
          type="warning"
          message={`최근 Gmail 수집 실패: ${overview.gmailPollingLatestFailureCode || 'UNKNOWN'}`}
          description={overview.gmailPollingLatestFailureMessage}
          action={<Button size="small" onClick={() => onShowRecentInbound(overview.gmailPollingLatestFailureInboundMessageId)}>최근 원문 보기</Button>}
          showIcon
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

function JsonPanel({ title, value }: { title: string; value?: unknown }) {
  return (
    <div className="json-panel">
      <Typography.Text strong>{title}</Typography.Text>
      <pre className="json-block compact">{JSON.stringify(value ?? {}, null, 2)}</pre>
    </div>
  );
}

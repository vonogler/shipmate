import { Alert, Button, Col, Descriptions, Row, Select, Tag, Typography } from 'antd';
import type { AdminOverview } from '../types';

type DashboardScreenProps = {
  overview: AdminOverview | null;
  days: number;
  onDaysChange: (days: number) => void;
  onShowRecentInbound: (inboundMessageId?: string | null) => void;
  onOpenStagingSummary: () => void;
};

export default function DashboardScreen({
  overview,
  days,
  onDaysChange,
  onShowRecentInbound,
  onOpenStagingSummary
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
      {overview?.smokeReport ? (
        <div className="status-panel">
          <div className="status-panel-heading">
            <Typography.Text strong>최근 스테이징 smoke</Typography.Text>
            <div>
              <Button size="small" onClick={onOpenStagingSummary}>점검 요약 열기</Button>
              <Tag color={smokeStatusColor(overview.smokeReport.status)}>{smokeStatusText(overview.smokeReport.status)}</Tag>
            </div>
          </div>
          <Descriptions size="small" column={{ xs: 1, md: 2 }} bordered>
            <Descriptions.Item label="API 기준 URL">{overview.smokeReport.apiBaseUrl || '-'}</Descriptions.Item>
            <Descriptions.Item label="완료 시각">{overview.smokeReport.finishedAt || '-'}</Descriptions.Item>
            <Descriptions.Item label="마지막 단계">{overview.smokeReport.lastStep || '-'}</Descriptions.Item>
            <Descriptions.Item label="마지막 HTTP 상태">{overview.smokeReport.lastHttpStatus || '-'}</Descriptions.Item>
            <Descriptions.Item label="이용자 ID">{overview.smokeReport.merchantId || '-'}</Descriptions.Item>
            <Descriptions.Item label="주문 원문 ID">{overview.smokeReport.inboundMessageId || '-'}</Descriptions.Item>
            <Descriptions.Item label="출고표 ID">{overview.smokeReport.orderDraftId || '-'}</Descriptions.Item>
            <Descriptions.Item label="실패 상세" span={2}>{overview.smokeReport.failureDetail || '-'}</Descriptions.Item>
          </Descriptions>
        </div>
      ) : null}
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

function smokeStatusText(status: string) {
  if (status === 'ok') {
    return '통과';
  }
  if (status === 'none') {
    return '리포트 없음';
  }
  if (status === 'unreadable') {
    return '리포트 읽기 실패';
  }
  return '확인 필요';
}

function smokeStatusColor(status: string) {
  if (status === 'ok') {
    return 'green';
  }
  if (status === 'none') {
    return 'default';
  }
  return 'red';
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

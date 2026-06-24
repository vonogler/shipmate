import { Button, Descriptions, Drawer, Space, Typography } from 'antd';
import type { GoogleGmailPollingRun } from '../types';

type GmailRunFailureDrawerProps = {
  gmailRun: GoogleGmailPollingRun | null;
  onClose: () => void;
  onShowInboundMessage: (inboundMessageId: string | null | undefined) => void;
  onShowErrorLogs: (gmailRun: GoogleGmailPollingRun) => void;
  onOpenErrorHandling: (gmailRun: GoogleGmailPollingRun, handlingStatus: string) => void;
  onOpenChecklist: () => void;
  gmailFailureGuide: (failureCode?: string | null, failureMessage?: string | null) => string;
  formatDate: (value: string | null) => string;
};

export default function GmailRunFailureDrawer({
  gmailRun,
  onClose,
  onShowInboundMessage,
  onShowErrorLogs,
  onOpenErrorHandling,
  onOpenChecklist,
  gmailFailureGuide,
  formatDate
}: GmailRunFailureDrawerProps) {
  return (
    <Drawer
      title="Gmail 수집 실패 상세"
      open={gmailRun !== null}
      onClose={onClose}
      width={560}
      extra={(
        <Button
          size="small"
          onClick={() => onShowInboundMessage(gmailRun?.latestInboundMessageId)}
          disabled={!gmailRun?.latestInboundMessageId}
        >
          원문 보기
        </Button>
      )}
    >
      {gmailRun ? (
        <Descriptions column={1} size="small" bordered>
          <Descriptions.Item label="실행 ID">{gmailRun.gmailPollingRunId}</Descriptions.Item>
          <Descriptions.Item label="이용자 ID">{gmailRun.merchantId || '-'}</Descriptions.Item>
          <Descriptions.Item label="결과">{gmailRun.success ? '성공' : '실패'}</Descriptions.Item>
          <Descriptions.Item label="실패 코드">{gmailRun.failureCode || '-'}</Descriptions.Item>
          <Descriptions.Item label="실패 메시지">{gmailRun.failureMessage || '-'}</Descriptions.Item>
          <Descriptions.Item label="운영 조치">
            <Space direction="vertical" size={8}>
              <Typography.Text>{gmailFailureGuide(gmailRun.failureCode, gmailRun.failureMessage)}</Typography.Text>
              <Button size="small" onClick={onOpenChecklist}>
                이메일 수집 체크리스트 열기
              </Button>
              <Button size="small" onClick={() => onShowErrorLogs(gmailRun)}>
                관련 오류 로그 보기
              </Button>
              <Space>
                <Button size="small" onClick={() => onOpenErrorHandling(gmailRun, 'ACKNOWLEDGED')}>
                  관련 오류 확인 처리
                </Button>
                <Button size="small" onClick={() => onOpenErrorHandling(gmailRun, 'RESOLVED')}>
                  관련 오류 조치 완료
                </Button>
              </Space>
            </Space>
          </Descriptions.Item>
          <Descriptions.Item label="최근 원문 ID">{gmailRun.latestInboundMessageId || '-'}</Descriptions.Item>
          <Descriptions.Item label="조회/수집">{gmailRun.scannedCount} / {gmailRun.collectedCount}</Descriptions.Item>
          <Descriptions.Item label="중복/비주문 제외">{gmailRun.skippedDuplicateCount} / {gmailRun.skippedNonOrderCount}</Descriptions.Item>
          <Descriptions.Item label="시작">{formatDate(gmailRun.startedAt)}</Descriptions.Item>
          <Descriptions.Item label="종료">{formatDate(gmailRun.finishedAt)}</Descriptions.Item>
        </Descriptions>
      ) : null}
    </Drawer>
  );
}

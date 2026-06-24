import { Button, Tag } from 'antd';
import type { ReactNode } from 'react';
import type { ColumnsType } from 'antd/es/table';
import type {
  AdminUser,
  AuditLog,
  GoogleGmailPollingRun,
  InboundMessage,
  MerchantSetting,
  OrderDraft
} from './types';

type TextRenderer = (value: string | null | undefined) => string;

export function createUserColumns(formatDate: (value: string | null) => string): ColumnsType<AdminUser> {
  return [
    { title: '직원 ID', dataIndex: 'merchantUserId', width: 240 },
    { title: '이용자 ID', dataIndex: 'merchantId', width: 190 },
    { title: '로그인 ID', dataIndex: 'loginId', width: 160 },
    { title: '이름', dataIndex: 'displayName', width: 160 },
    { title: '권한', dataIndex: 'role', width: 120, render: (role: string) => <Tag>{role}</Tag> },
    { title: '생성 시각', dataIndex: 'createdAt', width: 180, render: formatDate }
  ];
}

export function createInboundColumns(
  statusTag: (status: string) => ReactNode,
  emptyText: TextRenderer,
  formatDate: (value: string | null) => string
): ColumnsType<InboundMessage> {
  return [
    { title: '원문 ID', dataIndex: 'inboundMessageId', width: 240 },
    { title: '이용자 ID', dataIndex: 'merchantId', width: 190 },
    { title: '상태', dataIndex: 'inboundMessageStatus', width: 110, render: statusTag },
    { title: '입력 채널', dataIndex: 'sourceChannel', width: 140 },
    { title: '업종 채널', dataIndex: 'businessChannel', width: 180 },
    { title: '고객 ID', dataIndex: 'customerId', width: 170, render: emptyText },
    { title: '원문', dataIndex: 'rawText', ellipsis: true },
    { title: '접수 시각', dataIndex: 'receivedAt', width: 180, render: formatDate }
  ];
}

export function createOrderDraftColumns(
  statusTag: (status: string) => ReactNode,
  emptyText: TextRenderer,
  formatDate: (value: string | null) => string
): ColumnsType<OrderDraft> {
  return [
    { title: '초안 ID', dataIndex: 'orderDraftId', width: 240 },
    { title: '이용자 ID', dataIndex: 'merchantId', width: 190 },
    { title: '상태', dataIndex: 'orderDraftStatus', width: 110, render: statusTag },
    { title: '고객 ID', dataIndex: 'customerId', width: 170, render: emptyText },
    { title: '품목 수', dataIndex: 'itemCandidates', width: 90, render: (items: unknown[]) => items?.length ?? 0 },
    { title: '원문', dataIndex: 'rawText', ellipsis: true },
    { title: '수정 시각', dataIndex: 'updatedAt', width: 180, render: formatDate }
  ];
}

export function createAuditColumns(
  auditActionLabel: (actionType: string) => ReactNode,
  auditTargetLabel: (targetType: string) => ReactNode,
  auditMetadataText: (value: string | null | undefined) => string,
  emptyText: TextRenderer,
  formatDate: (value: string | null) => string
): ColumnsType<AuditLog> {
  return [
    { title: '이력 ID', dataIndex: 'auditLogId', width: 230 },
    { title: '이용자 ID', dataIndex: 'merchantId', width: 180 },
    { title: '작업자', dataIndex: 'actorId', width: 190, render: emptyText },
    { title: '액션', dataIndex: 'actionType', width: 220, render: auditActionLabel },
    { title: '대상', dataIndex: 'targetType', width: 140, render: auditTargetLabel },
    { title: '대상 ID', dataIndex: 'targetId', width: 220 },
    { title: '요약', dataIndex: 'summary', ellipsis: true },
    { title: '부가 정보', dataIndex: 'auditMetadataJson', width: 300, ellipsis: true, render: auditMetadataText },
    { title: '시각', dataIndex: 'createdAt', width: 180, render: formatDate }
  ];
}

export function createGmailPollingRunColumns(
  emptyText: TextRenderer,
  formatDate: (value: string | null) => string,
  onSelect: (row: GoogleGmailPollingRun) => void
): ColumnsType<GoogleGmailPollingRun> {
  return [
    { title: '실행 ID', dataIndex: 'gmailPollingRunId', width: 240 },
    { title: '트리거', dataIndex: 'triggerType', width: 150 },
    { title: '이용자 ID', dataIndex: 'merchantId', width: 190, render: emptyText },
    { title: '성공', dataIndex: 'success', width: 90, render: (success: boolean) => <Tag color={success ? 'green' : 'red'}>{success ? '성공' : '실패'}</Tag> },
    { title: '계정', dataIndex: 'accountCount', width: 80 },
    { title: '조회', dataIndex: 'scannedCount', width: 80 },
    { title: '수집', dataIndex: 'collectedCount', width: 80 },
    { title: '초안', dataIndex: 'orderDraftCreatedCount', width: 80 },
    { title: '첨부', dataIndex: 'attachmentStoredCount', width: 80 },
    { title: '중복 제외', dataIndex: 'skippedDuplicateCount', width: 100 },
    { title: '비주문 제외', dataIndex: 'skippedNonOrderCount', width: 110 },
    { title: '실패 코드', dataIndex: 'failureCode', width: 150, render: emptyText },
    { title: '실패 메시지', dataIndex: 'failureMessage', ellipsis: true, render: emptyText },
    { title: '최근 원문 ID', dataIndex: 'latestInboundMessageId', width: 240, render: emptyText },
    { title: '시작', dataIndex: 'startedAt', width: 180, render: formatDate },
    { title: '종료', dataIndex: 'finishedAt', width: 180, render: formatDate },
    {
      title: '상세',
      width: 130,
      fixed: 'right',
      render: (_, row) => (
        <Button size="small" onClick={() => onSelect(row)}>
          상세
        </Button>
      )
    }
  ];
}

export function createSettingColumns(
  emptyText: TextRenderer,
  formatDate: (value: string | null) => string
): ColumnsType<MerchantSetting> {
  return [
    { title: '이용자 ID', dataIndex: 'merchantId', width: 220 },
    { title: '기본 담당자', dataIndex: 'defaultMerchantStaffId', width: 170, render: emptyText },
    { title: '기본 택배사', dataIndex: 'defaultCourierCsvTemplate', width: 150 },
    { title: '기본 업종', dataIndex: 'defaultBusinessChannel', width: 190 },
    { title: '수정 시각', dataIndex: 'updatedAt', width: 180, render: formatDate }
  ];
}

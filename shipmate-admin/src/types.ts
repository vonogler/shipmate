export type LoginResponse = {
  accessToken: string;
  merchantUserId: string;
  merchantId: string;
  loginId: string;
  displayName: string;
  role: string;
};

export type LoginFormValues = {
  loginId: string;
  password: string;
  displayName?: string;
  role?: string;
};

export type BulkErrorHandlingFormValues = {
  assignedActorId?: string;
  handlingMemo?: string;
};

export type CourierCsvValidationRuleFormValues = {
  courierCsvTemplate: string;
  fieldName: string;
  csvColumnName?: string;
  required: boolean;
  errorMessage: string;
  sortOrder: number;
  active: boolean;
};

export type AdminOverview = {
  days: number;
  merchantCount: number;
  userCount: number;
  inboundMessageCount: number;
  orderDraftCount: number;
  submittedShippingOrderCount: number;
  callRecordingCount: number;
  errorLogCount: number;
  sourceChannelCounts: Record<string, number>;
  businessChannelCounts: Record<string, number>;
  orderDraftStatusCounts: Record<string, number>;
  errorCodeCounts: Record<string, number>;
  failedSourceChannelCounts: Record<string, number>;
  failedReasonBySourceChannelCounts: Record<string, Record<string, number>>;
  errorHandlingStatusCounts: Record<string, number>;
  merchantActivityCounts: Record<string, number>;
  gmailPollingEnabled: boolean;
  gmailPollingDelayMs: number;
  gmailPollingAccountCount: number;
  gmailPollingRecentRunCount: number;
  gmailPollingRecentFailureCount: number;
  gmailPollingRecentSkippedNonOrderCount: number;
  gmailPollingLatestSuccessAt: string | null;
  gmailPollingLatestFailureAt: string | null;
  gmailPollingLatestFailureCode: string | null;
  gmailPollingLatestFailureMessage: string | null;
  gmailPollingLatestFailureInboundMessageId: string | null;
  gmailPollingFailureCodeCounts: Record<string, number>;
  smokeReport: AdminSmokeReport;
  stagingCheck: AdminStagingCheck;
};

export type AdminSmokeReport = {
  status: string;
  apiBaseUrl: string | null;
  merchantId: string | null;
  inboundMessageId: string | null;
  orderDraftId: string | null;
  lastStep: string | null;
  lastHttpStatus: string | null;
  failureDetail: string | null;
  finishedAt: string | null;
  reportPath: string | null;
};

export type AdminStagingCheck = {
  suiteStatus: string;
  suiteReason: string | null;
  latestSmokeSuccessAt: string | null;
  latestSmokeFailureAt: string | null;
  uiCheckStatus: string;
  suiteFinishedAt: string | null;
  cleanupStatus: string;
  cleanupMode: string | null;
  cleanupTotalRows: string | null;
  cleanupRemainingRows: string | null;
  cleanupFinishedAt: string | null;
};

export type AdminMerchant = {
  merchantId: string;
  merchantName: string;
  status: string;
  accountLocked: boolean;
  userCount: number;
  customerCount: number;
  inboundMessageCount: number;
  orderDraftCount: number;
  lastActivityAt: string | null;
  operationMemo: string;
};

export type AdminUser = {
  merchantUserId: string;
  merchantId: string;
  loginId: string;
  displayName: string;
  role: string;
  createdAt: string;
  updatedAt: string;
};

export type InboundMessage = {
  inboundMessageId: string;
  merchantId: string;
  merchantStaffId: string | null;
  customerId: string | null;
  sourceChannel: string;
  businessChannel: string;
  rawText: string;
  inboundMessageStatus: string;
  receivedAt: string;
  createdAt: string;
  updatedAt: string;
};

export type OrderDraft = {
  orderDraftId: string;
  sourceInboundMessageId: string;
  merchantId: string;
  merchantStaffId: string | null;
  customerId: string | null;
  sourceChannel: string;
  businessChannel: string;
  rawText: string;
  itemCandidates: OrderDraftItemCandidate[];
  deliveryRecipientName: string | null;
  deliveryRecipientPhone: string | null;
  deliveryAddress: string | null;
  deliveryAddressDetail: string | null;
  deliveryMemo: string | null;
  courierMemo: string | null;
  orderDraftStatus: string;
  createdAt: string;
  updatedAt: string;
};

export type OrderDraftItemCandidate = {
  itemName?: string | null;
  itemSpec?: string | null;
  itemQuantity?: string | null;
  itemUnit?: string | null;
  lineMemo?: string | null;
};

export type CallRecording = {
  recordingId: string;
  fileName: string;
  source: string | null;
  status: string;
  fileSize: number;
  objectKey: string;
  uploadedAt: string | null;
  createdAt: string;
  errorCode: string | null;
};

export type AuditLog = {
  auditLogId: string;
  merchantId: string;
  actorId: string | null;
  actionType: string;
  targetType: string;
  targetId: string;
  summary: string;
  auditMetadataJson: string | null;
  createdAt: string;
};

export type AuditLogPageResponse = {
  rows: AuditLog[];
  total: number;
  limit: number;
  offset: number;
};

export type AuditLogFilterOptionsResponse = {
  actionTypes: string[];
  targetTypes: string[];
  merchantIds: string[];
  metadataKeys: string[];
};

export type CsvRuleAuditDiffRow = {
  fieldLabel: string;
  beforeValue: string;
  afterValue: string;
};

export type ErrorLog = {
  errorLogId: string;
  code: string;
  status: number;
  message: string;
  exceptionType: string | null;
  errorMetadataJson: string | null;
  handlingStatus: string;
  assignedActorId: string | null;
  handlingMemo: string | null;
  handledAt: string | null;
  createdAt: string;
};

export type ErrorLogPageResponse = {
  rows: ErrorLog[];
  total: number;
  limit: number;
  offset: number;
};

export type GoogleGmailPollingRun = {
  gmailPollingRunId: string;
  triggerType: string;
  merchantId: string | null;
  accountCount: number;
  scannedCount: number;
  collectedCount: number;
  orderDraftCreatedCount: number;
  attachmentStoredCount: number;
  skippedDuplicateCount: number;
  skippedNonOrderCount: number;
  success: boolean;
  failureCode: string | null;
  failureMessage: string | null;
  latestInboundMessageId: string | null;
  startedAt: string;
  finishedAt: string;
};

export type MerchantSetting = {
  merchantId: string;
  defaultMerchantStaffId: string | null;
  defaultCourierCsvTemplate: string;
  defaultBusinessChannel: string;
  updatedAt: string;
};

export type CourierCsvValidationRule = {
  courierCsvValidationRuleId: string;
  courierCsvTemplate: string;
  fieldName: string;
  csvColumnName: string | null;
  required: boolean;
  errorMessage: string;
  sortOrder: number;
  active: boolean;
};

export type BackupResponse = {
  backupId: string;
  files: Record<string, unknown>;
};

export type ScreenKey =
  | 'dashboard'
  | 'merchants'
  | 'users'
  | 'inbound'
  | 'drafts'
  | 'shipping'
  | 'recordings'
  | 'errors'
  | 'gmailRuns'
  | 'audits'
  | 'backup'
  | 'settings';

export type SourceChannel = 'KAKAO_PASTE' | 'SMS_SHARE' | 'EMAIL' | 'ORDER_PAGE';

export type BusinessChannel =
  | 'PACKAGING_CUSTOM'
  | 'BEAUTY_SUPPLIES'
  | 'MRO_SUPPLIES'
  | 'APPAREL_WHOLESALE'
  | 'METAL_MATERIALS'
  | 'PHARMACY_SUPPLIES';

export type InboundMessageStatus = 'RECEIVED' | 'DRAFTING' | 'DRAFTED' | 'IGNORED' | 'FAILED';
export type OrderDraftStatus = 'CREATED' | 'NEEDS_REVIEW' | 'SUBMITTED' | 'CANCELED';
export type CourierCsvTemplate = 'GENERIC' | 'CJ_LOGISTICS' | 'LOTTE' | 'HANJIN';
export type ItemSnapshotChangeType = 'INITIAL' | 'ADDED' | 'REMOVED' | 'CHANGED' | 'ORDER';

export type OperationDashboard = {
  inboundMessageCount: number;
  orderDraftCount: number;
  submittedShippingOrderCount: number;
  errorLogCount: number;
  orderDraftStatusCounts: Record<string, number>;
  sourceChannelCounts: Record<string, number>;
  errorCodeCounts: Record<string, number>;
  gmailPollingEnabled: boolean;
  gmailPollingDelayMs: number;
  gmailPollingAccountCount: number;
  gmailPollingRecentRunCount: number;
  gmailPollingRecentFailureCount: number;
  gmailPollingRecentSkippedNonOrderCount: number;
  gmailPollingLatestFailureMessage: string | null;
  gmailPollingLatestFailureInboundMessageId: string | null;
  gmailPollingFailureCodeCounts: Record<string, number>;
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

export type CourierCsvValidationError = {
  orderDraftId: string | null;
  fieldName: string;
  csvColumnName: string | null;
  message: string;
};

export type InboundMessage = {
  inboundMessageId: string;
  merchantId: string;
  merchantStaffId: string | null;
  customerId: string | null;
  sourceChannel: SourceChannel;
  businessChannel: BusinessChannel;
  rawText: string;
  inboundMessageStatus: InboundMessageStatus;
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
  sourceChannel: SourceChannel;
  businessChannel: BusinessChannel;
  rawText: string;
  itemCandidates: OrderDraftItemCandidate[];
  deliveryRecipientName: string | null;
  deliveryRecipientPhone: string | null;
  deliveryPostalCode: string | null;
  deliveryAddress: string | null;
  deliveryAddressDetail: string | null;
  deliveryRequestMemo: string | null;
  deliveryMemo: string | null;
  courierMemo: string | null;
  orderDraftStatus: OrderDraftStatus;
  createdAt: string;
  updatedAt: string;
};

export type OrderDraftItemCandidate = {
  orderDraftItemCandidateId?: string;
  itemName?: string | null;
  itemSpec?: string | null;
  itemQuantity?: string | null;
  itemUnit?: string | null;
  matchedUnitPrice?: number | null;
  packagingWidth?: string | null;
  packagingDepth?: string | null;
  packagingHeight?: string | null;
  materialName?: string | null;
  printRequired?: boolean | null;
  designFileProvided?: boolean | null;
  productionQuantity?: string | null;
  requestedDueDate?: string | null;
  sampleRequested?: boolean | null;
  customProductType?: string | null;
  imprintMethod?: string | null;
  itemColor?: string | null;
  customOptionText?: string | null;
  proofConfirmed?: boolean | null;
  productionConfirmed?: boolean | null;
  lineMemo?: string | null;
};

export type OrderDraftReviewFormValues = {
  itemCandidates: OrderDraftItemCandidate[];
  deliveryRecipientName?: string;
  deliveryRecipientPhone?: string;
  deliveryPostalCode?: string;
  deliveryAddress?: string;
  deliveryAddressDetail?: string;
  deliveryRequestMemo?: string;
  deliveryMemo?: string;
  courierMemo?: string;
};

export type ItemSnapshotDiffRow = {
  rowKey: string;
  itemLabel: string;
  fieldLabel: string;
  changeType: ItemSnapshotChangeType;
  previousValue: string;
  nextValue: string;
};

export type ShippingOrderAuditSection = {
  title: string;
  logs: AuditLog[];
  itemDiffRows: ItemSnapshotDiffRow[];
};

export type ShippingOrder = OrderDraft;

import { lazy, Suspense, useCallback, useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Button,
  Checkbox,
  Col,
  Collapse,
  Descriptions,
  Divider,
  Form,
  Input,
  InputNumber,
  Layout,
  Menu,
  message,
  Row,
  Select,
  Space,
  Table,
  Tag,
  Typography
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  AppstoreOutlined,
  CheckCircleOutlined,
  DeleteOutlined,
  DownloadOutlined,
  FileTextOutlined,
  InboxOutlined,
  MailOutlined,
  PlusOutlined,
  PrinterOutlined,
  ReloadOutlined,
  SaveOutlined,
  SendOutlined,
  SettingOutlined,
  TeamOutlined
} from '@ant-design/icons';
import {
  initialNavigationKey,
  shippingOrderDiffFieldOptions,
  useShippingOrderDiffSectionScroll,
  useShippingOrderDiffUrlState
} from './hooks/useShippingOrderDiffUrlState';
import { useOrderDraftReviewActions } from './hooks/useOrderDraftReviewActions';
import { useOrderDraftReviewFormState } from './hooks/useOrderDraftReviewFormState';
import type {
  AuditLog,
  BusinessChannel,
  CourierCsvTemplate,
  CourierCsvValidationError,
  InboundMessage,
  InboundMessageStatus,
  ItemSnapshotDiffRow,
  OperationDashboard,
  OrderDraft,
  OrderDraftItemCandidate,
  OrderDraftReviewFormValues,
  OrderDraftStatus,
  SourceChannel
} from './types';

type LoginResponse = {
  accessToken: string;
  merchantUserId: string;
  merchantId: string;
  loginId: string;
  displayName: string;
  role: string;
};

type LoginFormValues = {
  merchantId?: string;
  loginId: string;
  password: string;
  displayName?: string;
  role?: string;
};

const DashboardScreen = lazy(() => import('./screens/DashboardScreen'));
const DraftReviewScreen = lazy(() => import('./screens/DraftReviewScreen'));
const OperationsScreen = lazy(() => import('./screens/OperationsScreen'));
const ShippingScreen = lazy(() => import('./screens/ShippingScreen'));

type EmailMailboxFormValues = {
  merchantId: string;
  host: string;
  port: number;
  username: string;
  encryptedPasswordMemo: string;
  sslEnabled?: boolean;
};

type GoogleOAuthStatus = {
  connected: boolean;
  merchantId: string;
  googleEmail: string | null;
  hasRefreshToken: boolean;
  expiresAt: string | null;
  updatedAt: string | null;
};

type GoogleGmailPollingResponse = {
  accountCount: number;
  scannedCount: number;
  collectedCount: number;
  orderDraftCreatedCount: number;
  attachmentStoredCount: number;
  skippedDuplicateCount: number;
  skippedNonOrderCount: number;
  status: string;
};

type GoogleGmailPollingStatus = {
  pollingEnabled: boolean;
  pollingDelayMs: number;
  pollingMaxMessages: number;
  autoCreateOrderDrafts: boolean;
  accountCount: number;
  collectedMessageCount: number;
  latestCollectedAt: string | null;
  latestReceivedAt: string | null;
  attachmentStoredCount: number;
  recentRunCount: number;
  recentFailureCount: number;
  recentSkippedNonOrderCount: number;
  latestSuccessAt: string | null;
  latestFailureAt: string | null;
  latestFailureCode: string | null;
  latestFailureMessage: string | null;
  latestFailureInboundMessageId: string | null;
  recentFailureCodeCounts: Record<string, number>;
};

type EmailAttachment = {
  emailAttachmentId: string;
  merchantId: string;
  inboundMessageId: string;
  sourceChannel: SourceChannel;
  sourceMessageId: string | null;
  fileName: string;
  contentType: string;
  fileSizeBytes: number;
  createdAt: string;
};

type EmailAttachmentDownloadUrl = {
  emailAttachmentId: string;
  fileName: string;
  downloadUrl: string;
  expiresInSeconds: number;
  retentionDays: number;
};

type EmailAttachmentCleanupResponse = {
  retentionDays: number;
  cutoffCreatedAt: string;
  expiredAttachmentCount: number;
  deletedObjectCount: number;
  deletedMetadataCount: number;
  failedCount: number;
  status: string;
};

type MerchantSettingsFormValues = {
  merchantId: string;
  defaultMerchantStaffId?: string;
  defaultCourierCsvTemplate: CourierCsvTemplate;
  defaultBusinessChannel: BusinessChannel;
};

type InboundMessageFormValues = {
  merchantId: string;
  merchantStaffId?: string;
  customerId?: string;
  sourceChannel: SourceChannel;
  businessChannel: BusinessChannel;
  rawText: string;
};

type CustomerPriceItem = {
  customerPriceItemId?: string;
  itemNameKeyword?: string | null;
  itemSpecKeyword?: string | null;
  itemUnit?: string | null;
  unitPrice?: number | null;
};

type Customer = {
  customerId: string;
  merchantId: string;
  customerName: string;
  customerPhone: string | null;
  defaultRecipientName: string | null;
  defaultRecipientPhone: string | null;
  defaultPostalCode: string | null;
  defaultAddress: string | null;
  defaultAddressDetail: string | null;
  favoriteItemNames: string[];
  priceItems: CustomerPriceItem[];
  createdAt: string;
  updatedAt: string;
};

type CustomerFormValues = {
  customerId?: string;
  merchantId: string;
  customerName: string;
  customerPhone?: string;
  defaultRecipientName?: string;
  defaultRecipientPhone?: string;
  defaultPostalCode?: string;
  defaultAddress?: string;
  defaultAddressDetail?: string;
  favoriteItemNamesText?: string;
  priceItems: CustomerPriceItem[];
};

const apiBaseUrl = import.meta.env.VITE_SHIPMATE_API_BASE_URL ?? 'http://localhost:8080';

const sourceChannelLabels: Record<SourceChannel, string> = {
  KAKAO_PASTE: '카카오톡 붙여넣기',
  SMS_SHARE: '문자 공유',
  EMAIL: '이메일',
  ORDER_PAGE: '주문 페이지'
};

const businessChannelLabels: Record<BusinessChannel, string> = {
  PACKAGING_CUSTOM: '포장재 주문제작',
  BEAUTY_SUPPLIES: '미용재료/화장품 도매',
  MRO_SUPPLIES: '소모성 자재',
  APPAREL_WHOLESALE: '의류 도매',
  METAL_MATERIALS: '금속 자재',
  PHARMACY_SUPPLIES: '약국/의약외품'
};

const inboundStatusLabels: Record<InboundMessageStatus, string> = {
  RECEIVED: '접수됨',
  DRAFTING: '초안 생성 중',
  DRAFTED: '초안 생성됨',
  IGNORED: '제외됨',
  FAILED: '실패'
};

const inboundStatusColors: Record<InboundMessageStatus, string> = {
  RECEIVED: 'processing',
  DRAFTING: 'blue',
  DRAFTED: 'success',
  IGNORED: 'default',
  FAILED: 'red'
};

const orderDraftStatusLabels: Record<OrderDraftStatus, string> = {
  CREATED: '초안',
  NEEDS_REVIEW: '검수 필요',
  SUBMITTED: '제출됨',
  CANCELED: '취소됨'
};

const orderDraftStatusColors: Record<OrderDraftStatus, string> = {
  CREATED: 'blue',
  NEEDS_REVIEW: 'warning',
  SUBMITTED: 'success',
  CANCELED: 'default'
};

const courierCsvTemplateLabels: Record<CourierCsvTemplate, string> = {
  GENERIC: '기본 양식',
  CJ_LOGISTICS: 'CJ대한통운',
  LOTTE: '롯데택배',
  HANJIN: '한진택배'
};

function App() {
  const [loginForm] = Form.useForm<LoginFormValues>();
  const [form] = Form.useForm<InboundMessageFormValues>();
  const [reviewForm] = Form.useForm<OrderDraftReviewFormValues>();
  const [customerForm] = Form.useForm<CustomerFormValues>();
  const [emailMailboxForm] = Form.useForm<EmailMailboxFormValues>();
  const [settingsForm] = Form.useForm<MerchantSettingsFormValues>();
  const [inboundMessages, setInboundMessages] = useState<InboundMessage[]>([]);
  const [orderDrafts, setOrderDrafts] = useState<OrderDraft[]>([]);
  const [orderDraftStatusFilter, setOrderDraftStatusFilter] = useState<OrderDraftStatus | 'ALL'>('ALL');
  const [orderDraftSearchText, setOrderDraftSearchText] = useState('');
  const [shippingOrders, setShippingOrders] = useState<OrderDraft[]>([]);
  const [shippingSearchText, setShippingSearchText] = useState('');
  const [shippingDaysFilter, setShippingDaysFilter] = useState<number | 'ALL'>('ALL');
  const [shippingCsvFilter, setShippingCsvFilter] = useState<'ALL' | 'VALID' | 'MISSING'>('ALL');
  const {
    fieldFilter: shippingOrderDiffFieldFilter,
    sectionAnchor: shippingOrderDiffSectionAnchor,
    updateFieldFilter: updateShippingOrderDiffFieldFilter,
    preserveShippingDiffQuery
  } = useShippingOrderDiffUrlState();
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [selectedInboundMessageId, setSelectedInboundMessageId] = useState<string | null>(null);
  const [selectedOrderDraftId, setSelectedOrderDraftId] = useState<string | null>(null);
  const [selectedShippingOrderId, setSelectedShippingOrderId] = useState<string | null>(null);
  const [loadingInboundMessages, setLoadingInboundMessages] = useState(false);
  const [loadingOrderDrafts, setLoadingOrderDrafts] = useState(false);
  const [loadingShippingOrders, setLoadingShippingOrders] = useState(false);
  const [loadingCustomers, setLoadingCustomers] = useState(false);
  const [creatingInboundMessage, setCreatingInboundMessage] = useState(false);
  const [savingCustomer, setSavingCustomer] = useState(false);
  const [creatingOrderDraft, setCreatingOrderDraft] = useState(false);
  const [reopeningShippingOrderId, setReopeningShippingOrderId] = useState<string | null>(null);
  const [downloadingCourierCsvId, setDownloadingCourierCsvId] = useState<string | null>(null);
  const [courierCsvTemplate, setCourierCsvTemplate] = useState<CourierCsvTemplate>('GENERIC');
  const [extractingAiOrderDraftId, setExtractingAiOrderDraftId] = useState<string | null>(null);
  const [bulkCsvDownloading, setBulkCsvDownloading] = useState(false);
  const [courierCsvValidationErrors, setCourierCsvValidationErrors] = useState<CourierCsvValidationError[]>([]);
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
  const [errorLogs, setErrorLogs] = useState<unknown[]>([]);
  const [backupPreview, setBackupPreview] = useState('');
  const [dashboard, setDashboard] = useState<OperationDashboard | null>(null);
  const [dashboardDays, setDashboardDays] = useState(1);
  const [authToken, setAuthToken] = useState(() => localStorage.getItem('shipmate_merchant_token') ?? '');
  const [loginUser, setLoginUser] = useState<LoginResponse | null>(() => {
    const saved = localStorage.getItem('shipmate_merchant_user');
    return saved ? JSON.parse(saved) as LoginResponse : null;
  });
  const [activeNavigationKey, setActiveNavigationKey] = useState(() => initialNavigationKey());
  const [authLoading, setAuthLoading] = useState(false);
  const [emailMailboxStatus, setEmailMailboxStatus] = useState('');
  const [googleOAuthStatus, setGoogleOAuthStatus] = useState<GoogleOAuthStatus | null>(null);
  const [googleGmailPollingSummary, setGoogleGmailPollingSummary] = useState<GoogleGmailPollingStatus | null>(null);
  const [googleGmailPollingStatus, setGoogleGmailPollingStatus] = useState('');
  const [googleOAuthWorking, setGoogleOAuthWorking] = useState(false);
  const [attachmentCleanupWorking, setAttachmentCleanupWorking] = useState(false);
  const [attachmentCleanupStatus, setAttachmentCleanupStatus] = useState('');
  const [inboundAttachments, setInboundAttachments] = useState<EmailAttachment[]>([]);
  const [draftAttachments, setDraftAttachments] = useState<EmailAttachment[]>([]);
  const [loadingInboundAttachments, setLoadingInboundAttachments] = useState(false);
  const [loadingDraftAttachments, setLoadingDraftAttachments] = useState(false);
  const [downloadingAttachmentId, setDownloadingAttachmentId] = useState<string | null>(null);
  const [settingsSaving, setSettingsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const isAdmin = loginUser?.role === 'ADMIN';
  const canOperate = loginUser?.role === 'ADMIN' || loginUser?.role === 'OPERATOR';
  const navigationItems = useMemo(() => [
    { key: 'dashboard', icon: <AppstoreOutlined />, label: '운영 대시보드' },
    { key: 'inbound', icon: <InboxOutlined />, label: '주문 원문' },
    { key: 'customers', icon: <TeamOutlined />, label: '고객/단가표' },
    { key: 'drafts', icon: <FileTextOutlined />, label: '초안 검수' },
    { key: 'shipping', icon: <CheckCircleOutlined />, label: '출고/문서' },
    { key: 'operations', icon: <SettingOutlined />, label: '운영 관리' },
    { key: 'settings', icon: <MailOutlined />, label: '설정' }
  ], []);

  const moveToNavigationSection = useCallback((key: string) => {
    const url = new URL(window.location.href);
    if (key === 'shipping') {
      preserveShippingDiffQuery(url);
    }
    window.history.replaceState(null, '', `${url.pathname}${url.search}${url.hash}`);
    setActiveNavigationKey(key);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }, [preserveShippingDiffQuery]);

  const selectedInboundMessage = useMemo(
    () => inboundMessages.find((row) => row.inboundMessageId === selectedInboundMessageId) ?? null,
    [inboundMessages, selectedInboundMessageId]
  );

  const selectedOrderDraft = useMemo(
    () => orderDrafts.find((row) => row.orderDraftId === selectedOrderDraftId) ?? null,
    [orderDrafts, selectedOrderDraftId]
  );

  const selectedOrderDraftMissingFields = useMemo(
    () => selectedOrderDraft ? orderDraftSubmissionMissingFields(selectedOrderDraft) : [],
    [selectedOrderDraft]
  );

  const filteredOrderDrafts = useMemo(
    () => orderDrafts.filter((row) =>
      (orderDraftStatusFilter === 'ALL' || row.orderDraftStatus === orderDraftStatusFilter)
      && orderDraftMatchesSearch(row, orderDraftSearchText)
    ),
    [orderDraftSearchText, orderDraftStatusFilter, orderDrafts]
  );

  const selectedShippingOrder = useMemo(
    () => shippingOrders.find((row) => row.orderDraftId === selectedShippingOrderId) ?? null,
    [shippingOrders, selectedShippingOrderId]
  );

  const selectedShippingOrderMissingFields = useMemo(
    () => selectedShippingOrder ? orderDraftSubmissionMissingFields(selectedShippingOrder) : [],
    [selectedShippingOrder]
  );

  const selectedShippingOrderAuditLogs = useMemo(
    () => selectedShippingOrder
      ? auditLogs.filter((row) => row.targetType === 'ORDER_DRAFT' && row.targetId === selectedShippingOrder.orderDraftId)
      : [],
    [auditLogs, selectedShippingOrder]
  );

  const selectedShippingOrderSubmissionCount = useMemo(
    () => selectedShippingOrderAuditLogs.filter((row) => row.actionType === 'ORDER_DRAFT_SUBMITTED').length,
    [selectedShippingOrderAuditLogs]
  );

  const selectedShippingOrderAuditSections = useMemo(
    () => groupShippingOrderAuditLogs(selectedShippingOrderAuditLogs),
    [selectedShippingOrderAuditLogs]
  );

  const selectedOrderDraftCustomer = useMemo(
    () => customers.find((row) => row.customerId === selectedOrderDraft?.customerId) ?? null,
    [customers, selectedOrderDraft?.customerId]
  );

  const filteredShippingOrders = useMemo(
    () => shippingOrders.filter((row) => {
      const csvMissing = orderDraftSubmissionMissingFields(row).length > 0;
      return orderDraftMatchesSearch(row, shippingSearchText)
        && orderDraftWithinDays(row, shippingDaysFilter)
        && (shippingCsvFilter === 'ALL'
          || (shippingCsvFilter === 'MISSING' && csvMissing)
          || (shippingCsvFilter === 'VALID' && !csvMissing));
    }),
    [shippingCsvFilter, shippingDaysFilter, shippingOrders, shippingSearchText]
  );

  const loadInboundMessages = useCallback(async () => {
    setLoadingInboundMessages(true);
    setError(null);

    try {
      const rows = await requestJson<InboundMessage[]>('/api/inbound-messages?limit=50');
      setInboundMessages(rows);
      setSelectedInboundMessageId((currentId) => currentId ?? rows[0]?.inboundMessageId ?? null);
    } catch (cause) {
      setError(errorMessage(cause, '주문 원문 목록을 불러오지 못했습니다.'));
    } finally {
      setLoadingInboundMessages(false);
    }
  }, []);

  const loadOrderDrafts = useCallback(async () => {
    setLoadingOrderDrafts(true);
    setError(null);

    try {
      const rows = await requestJson<OrderDraft[]>('/api/order-drafts?limit=50');
      setOrderDrafts(rows);
      setSelectedOrderDraftId((currentId) => currentId ?? rows[0]?.orderDraftId ?? null);
    } catch (cause) {
      setError(errorMessage(cause, '출고표 초안 목록을 불러오지 못했습니다.'));
    } finally {
      setLoadingOrderDrafts(false);
    }
  }, []);

  const loadShippingOrders = useCallback(async () => {
    setLoadingShippingOrders(true);
    setError(null);

    try {
      const rows = await requestJson<OrderDraft[]>('/api/shipping-orders?limit=50');
      setShippingOrders(rows);
      const queryShippingOrderId = new URLSearchParams(window.location.search).get('shippingOrderId');
      setSelectedShippingOrderId((currentId) => currentId ?? rows.find((row) => row.orderDraftId === queryShippingOrderId)?.orderDraftId ?? rows[0]?.orderDraftId ?? null);
    } catch (cause) {
      setError(errorMessage(cause, '제출된 출고표 목록을 불러오지 못했습니다.'));
    } finally {
      setLoadingShippingOrders(false);
    }
  }, []);

  const loadCustomers = useCallback(async () => {
    setLoadingCustomers(true);
    setError(null);

    try {
      const rows = await requestJson<Customer[]>('/api/customers?limit=100');
      setCustomers(rows);
    } catch (cause) {
      setError(errorMessage(cause, '고객 목록을 불러오지 못했습니다.'));
    } finally {
      setLoadingCustomers(false);
    }
  }, []);

  const loadDashboard = useCallback(async () => {
    try {
      setDashboard(await requestJson<OperationDashboard>(`/api/operation/dashboard?days=${dashboardDays}`));
    } catch (cause) {
      setError(errorMessage(cause, '운영 대시보드를 불러오지 못했습니다.'));
    }
  }, [dashboardDays]);

  const loadAuditLogs = useCallback(async () => {
    try {
      setAuditLogs(await requestJson<AuditLog[]>('/api/operation/audit-logs?limit=100'));
    } catch (cause) {
      setError(errorMessage(cause, '변경 이력을 불러오지 못했습니다.'));
    }
  }, []);

  const loadEmailAttachments = useCallback(
    async (inboundMessageId: string | null, target: 'inbound' | 'draft') => {
      const setRows = target === 'inbound' ? setInboundAttachments : setDraftAttachments;
      const setLoading = target === 'inbound' ? setLoadingInboundAttachments : setLoadingDraftAttachments;
      if (!inboundMessageId) {
        setRows([]);
        return;
      }
      setLoading(true);
      try {
        const rows = await requestJson<EmailAttachment[]>(
          `/api/email-orders/attachments?inboundMessageId=${encodeURIComponent(inboundMessageId)}`
        );
        setRows(rows);
      } catch (cause) {
        setRows([]);
        setError(errorMessage(cause, '이메일 첨부파일 목록을 불러오지 못했습니다.'));
      } finally {
        setLoading(false);
      }
    },
    []
  );

  const reloadAll = useCallback(async () => {
    if (!authToken) {
      return;
    }
    await Promise.all([
      loadInboundMessages(),
      loadOrderDrafts(),
      loadShippingOrders(),
      loadCustomers(),
      loadDashboard(),
      loadAuditLogs()
    ]);
  }, [authToken, loadAuditLogs, loadDashboard, loadInboundMessages, loadOrderDrafts, loadShippingOrders, loadCustomers]);

  useEffect(() => {
    reloadAll();
  }, [reloadAll]);

  useShippingOrderDiffSectionScroll(
    activeNavigationKey,
    shippingOrderDiffSectionAnchor,
    selectedShippingOrderAuditSections.length
  );

  const submitAuth = useCallback(async (values: LoginFormValues, mode: 'login' | 'register') => {
    setAuthLoading(true);
    setError(null);

    try {
      const response = await requestJson<LoginResponse>(mode === 'login' ? '/api/auth/login' : '/api/auth/register', {
        method: 'POST',
        body: JSON.stringify(mode === 'login'
          ? {
              loginId: values.loginId.trim(),
              password: values.password
            }
          : {
              merchantId: values.merchantId?.trim() || 'merchant_local_packaging',
              loginId: values.loginId.trim(),
              password: values.password,
              displayName: values.displayName?.trim() || values.loginId.trim(),
              role: values.role || 'ADMIN'
            })
      });
      localStorage.setItem('shipmate_merchant_token', response.accessToken);
      localStorage.setItem('shipmate_merchant_user', JSON.stringify(response));
      setAuthToken(response.accessToken);
      setLoginUser(response);
      message.success(mode === 'login' ? '로그인되었습니다.' : '계정이 생성되었습니다.');
    } catch (cause) {
      const description = errorMessage(cause, mode === 'login' ? '로그인하지 못했습니다.' : '계정을 생성하지 못했습니다.');
      setError(description);
      message.error(description);
    } finally {
      setAuthLoading(false);
    }
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('shipmate_merchant_token');
    localStorage.removeItem('shipmate_merchant_user');
    setAuthToken('');
    setLoginUser(null);
    setInboundMessages([]);
    setOrderDrafts([]);
    setShippingOrders([]);
    setCustomers([]);
    setDashboard(null);
  }, []);

  const updateSelectedShippingOrderId = useCallback((orderDraftId: string | null) => {
    const url = new URL(window.location.href);
    if (orderDraftId) {
      url.searchParams.set('shippingOrderId', orderDraftId);
    } else {
      url.searchParams.delete('shippingOrderId');
    }
    window.history.replaceState(null, '', `${url.pathname}${url.search}${url.hash}`);
    setSelectedShippingOrderId(orderDraftId);
  }, []);

  const {
    savingOrderDraftReview,
    submittingOrderDraft,
    saveOrderDraftReview,
    submitOrderDraft,
    applySelectedCustomerDefaults,
    applyCustomerPriceCandidates
  } = useOrderDraftReviewActions({
    reviewForm,
    selectedOrderDraft,
    selectedOrderDraftCustomer,
    requestJson,
    setError,
    setOrderDrafts,
    setShippingOrders,
    setSelectedOrderDraftId,
    updateSelectedShippingOrderId,
    loadAuditLogs
  });

  const saveEmailMailboxConfig = useCallback(async (values: EmailMailboxFormValues) => {
    try {
      const result = await requestJson<{ collectionStatus: string }>('/api/email-orders/mailbox-config', {
        method: 'POST',
        body: JSON.stringify({
          merchantId: values.merchantId,
          host: values.host,
          port: values.port,
          username: values.username,
          encryptedPasswordMemo: values.encryptedPasswordMemo,
          sslEnabled: values.sslEnabled ?? true
        })
      });
      setEmailMailboxStatus(result.collectionStatus);
      message.success('이메일 수집 설정을 저장했습니다.');
    } catch (cause) {
      const description = errorMessage(cause, '이메일 수집 설정을 저장하지 못했습니다.');
      setError(description);
      message.error(description);
    }
  }, []);

  const collectEmailMailbox = useCallback(async () => {
    const merchantId = emailMailboxForm.getFieldValue('merchantId') || loginUser?.merchantId || 'merchant_local_packaging';
    try {
      const result = await requestJson<{ collectionStatus: string }>('/api/email-orders/collect-mailbox', {
        method: 'POST',
        body: JSON.stringify({ merchantId })
      });
      setEmailMailboxStatus(result.collectionStatus);
      message.success('메일 서버 연결 확인을 실행했습니다.');
    } catch (cause) {
      const description = errorMessage(cause, '메일 서버 연결 확인에 실패했습니다.');
      setError(description);
      message.error(description);
    }
  }, [emailMailboxForm, loginUser?.merchantId]);

  const loadGoogleOAuthStatus = useCallback(async () => {
    if (!authToken) {
      return;
    }
    const merchantId = loginUser?.merchantId || emailMailboxForm.getFieldValue('merchantId') || 'merchant_local_packaging';
    try {
      const status = await requestJson<GoogleOAuthStatus>(`/api/oauth/google/status?merchantId=${encodeURIComponent(merchantId)}`);
      setGoogleOAuthStatus(status);
    } catch (cause) {
      const description = errorMessage(cause, 'Google Gmail 연결 상태를 확인하지 못했습니다.');
      setError(description);
      message.error(description);
    }
  }, [authToken, emailMailboxForm, loginUser?.merchantId]);

  const loadGoogleGmailPollingSummary = useCallback(async () => {
    if (!authToken) {
      return;
    }
    try {
      const status = await requestJson<GoogleGmailPollingStatus>('/api/oauth/google/gmail-polling/status');
      setGoogleGmailPollingSummary(status);
    } catch (cause) {
      const description = errorMessage(cause, 'Gmail polling 운영 상태를 확인하지 못했습니다.');
      setError(description);
      message.error(description);
    }
  }, [authToken]);

  const connectGoogleGmail = useCallback(async () => {
    const merchantId = loginUser?.merchantId || emailMailboxForm.getFieldValue('merchantId') || 'merchant_local_packaging';
    setGoogleOAuthWorking(true);
    try {
      const result = await requestJson<{ authorizationUrl: string }>('/api/oauth/google/authorize-url', {
        method: 'POST',
        body: JSON.stringify({ merchantId })
      });
      window.open(result.authorizationUrl, '_blank', 'noopener,noreferrer');
      message.success('Google Gmail 연결 창을 열었습니다.');
    } catch (cause) {
      const description = errorMessage(cause, 'Google Gmail 연결을 시작하지 못했습니다.');
      setError(description);
      message.error(description);
    } finally {
      setGoogleOAuthWorking(false);
    }
  }, [emailMailboxForm, loginUser?.merchantId]);

  const collectGoogleGmail = useCallback(async () => {
    const merchantId = loginUser?.merchantId || emailMailboxForm.getFieldValue('merchantId') || 'merchant_local_packaging';
    setGoogleOAuthWorking(true);
    try {
      const result = await requestJson<GoogleGmailPollingResponse>('/api/oauth/google/collect-gmail', {
        method: 'POST',
        body: JSON.stringify({ merchantId })
      });
      setGoogleGmailPollingStatus(
        `연결 계정 ${result.accountCount}개, 조회 ${result.scannedCount}건, 신규 수집 ${result.collectedCount}건, 초안 생성 ${result.orderDraftCreatedCount}건, 첨부 저장 ${result.attachmentStoredCount}건, 중복 제외 ${result.skippedDuplicateCount}건, 비주문 제외 ${result.skippedNonOrderCount}건`
      );
      await Promise.all([loadInboundMessages(), loadGoogleOAuthStatus(), loadGoogleGmailPollingSummary()]);
      message.success('Gmail 수집을 실행했습니다.');
    } catch (cause) {
      const description = errorMessage(cause, 'Gmail 수집을 실행하지 못했습니다.');
      setError(description);
      message.error(description);
    } finally {
      setGoogleOAuthWorking(false);
    }
  }, [emailMailboxForm, loadGoogleGmailPollingSummary, loadGoogleOAuthStatus, loadInboundMessages, loginUser?.merchantId]);

  const cleanupExpiredEmailAttachments = useCallback(async () => {
    setAttachmentCleanupWorking(true);
    setError(null);
    try {
      const result = await requestJson<EmailAttachmentCleanupResponse>('/api/email-orders/attachments/cleanup-expired', {
        method: 'POST'
      });
      const status = `보관 ${result.retentionDays}일 기준, 만료 ${result.expiredAttachmentCount}건, 객체 삭제 ${result.deletedObjectCount}건, 메타데이터 삭제 ${result.deletedMetadataCount}건, 실패 ${result.failedCount}건`;
      setAttachmentCleanupStatus(status);
      message.success('만료 첨부파일 정리를 실행했습니다.');
    } catch (cause) {
      const description = errorMessage(cause, '만료 첨부파일 정리를 실행하지 못했습니다.');
      setError(description);
      message.error(description);
    } finally {
      setAttachmentCleanupWorking(false);
    }
  }, []);

  useEffect(() => {
    loadGoogleOAuthStatus();
    loadGoogleGmailPollingSummary();
  }, [loadGoogleGmailPollingSummary, loadGoogleOAuthStatus]);

  useEffect(() => {
    loadEmailAttachments(selectedInboundMessage?.inboundMessageId ?? null, 'inbound');
  }, [loadEmailAttachments, selectedInboundMessage?.inboundMessageId]);

  useEffect(() => {
    loadEmailAttachments(selectedOrderDraft?.sourceInboundMessageId ?? null, 'draft');
  }, [loadEmailAttachments, selectedOrderDraft?.sourceInboundMessageId]);

  const saveMerchantSettings = useCallback(async (values: MerchantSettingsFormValues) => {
    setSettingsSaving(true);
    try {
      await requestJson('/api/operation/settings', {
        method: 'POST',
        body: JSON.stringify(values)
      });
      message.success('초기 설정을 저장했습니다.');
    } catch (cause) {
      const description = errorMessage(cause, '초기 설정을 저장하지 못했습니다.');
      setError(description);
      message.error(description);
    } finally {
      setSettingsSaving(false);
    }
  }, []);

  const deleteCustomer = useCallback(async (customerId: string) => {
    try {
      await requestJson(`/api/customers/${customerId}`, { method: 'DELETE' });
      await loadCustomers();
      message.success('고객을 보관 처리했습니다.');
    } catch (cause) {
      const description = errorMessage(cause, '고객을 보관 처리하지 못했습니다.');
      setError(description);
      message.error(description);
    }
  }, [loadCustomers]);

  const cancelOrderDraft = useCallback(async (orderDraftId: string) => {
    try {
      const canceled = await requestJson<OrderDraft>(`/api/order-drafts/${orderDraftId}`, { method: 'DELETE' });
      setOrderDrafts((currentRows) =>
        currentRows.map((row) => (row.orderDraftId === canceled.orderDraftId ? canceled : row))
      );
      message.success('출고표 초안을 취소 처리했습니다.');
    } catch (cause) {
      const description = errorMessage(cause, '출고표 초안을 취소 처리하지 못했습니다.');
      setError(description);
      message.error(description);
    }
  }, []);

  useOrderDraftReviewFormState(reviewForm, selectedOrderDraft, emptyItemCandidate);

  const createInboundMessage = useCallback(
    async (values: InboundMessageFormValues) => {
      setCreatingInboundMessage(true);
      setError(null);

      try {
        const created = await requestJson<InboundMessage>('/api/inbound-messages', {
          method: 'POST',
          body: JSON.stringify({
            merchantId: values.merchantId.trim(),
            merchantStaffId: trimToNull(values.merchantStaffId),
            customerId: trimToNull(values.customerId),
            sourceChannel: values.sourceChannel,
            businessChannel: values.businessChannel,
            rawText: values.rawText.trim()
          })
        });
        setInboundMessages((currentRows) => [created, ...currentRows]);
        setSelectedInboundMessageId(created.inboundMessageId);
        form.setFieldValue('rawText', '');
        message.success('주문 원문이 접수되었습니다.');
      } catch (cause) {
        const description = errorMessage(cause, '주문 원문을 접수하지 못했습니다.');
        setError(description);
        message.error(description);
      } finally {
        setCreatingInboundMessage(false);
      }
    },
    [form]
  );

  const saveCustomer = useCallback(
    async (values: CustomerFormValues) => {
      setSavingCustomer(true);
      setError(null);

      try {
        const saved = await requestJson<Customer>('/api/customers', {
          method: 'POST',
          body: JSON.stringify({
            customerId: trimToNull(values.customerId),
            merchantId: values.merchantId.trim(),
            customerName: values.customerName.trim(),
            customerPhone: trimToNull(values.customerPhone),
            defaultRecipientName: trimToNull(values.defaultRecipientName),
            defaultRecipientPhone: trimToNull(values.defaultRecipientPhone),
            defaultPostalCode: trimToNull(values.defaultPostalCode),
            defaultAddress: trimToNull(values.defaultAddress),
            defaultAddressDetail: trimToNull(values.defaultAddressDetail),
            favoriteItemNames: splitCommaText(values.favoriteItemNamesText),
            priceItems: (values.priceItems ?? []).map((priceItem) => ({
              customerPriceItemId: trimToNull(priceItem.customerPriceItemId),
              itemNameKeyword: trimToNull(priceItem.itemNameKeyword),
              itemSpecKeyword: trimToNull(priceItem.itemSpecKeyword),
              itemUnit: trimToNull(priceItem.itemUnit),
              unitPrice: priceItem.unitPrice ?? null
            }))
          })
        });
        setCustomers((currentRows) => [saved, ...currentRows.filter((row) => row.customerId !== saved.customerId)]);
        customerForm.setFieldsValue({ customerId: saved.customerId });
        message.success('고객 정보가 저장되었습니다.');
      } catch (cause) {
        const description = errorMessage(cause, '고객 정보를 저장하지 못했습니다.');
        setError(description);
        message.error(description);
      } finally {
        setSavingCustomer(false);
      }
    },
    [customerForm]
  );

  const createOrderDraft = useCallback(async () => {
    if (!selectedInboundMessage) {
      message.warning('출고표 초안을 만들 주문 원문을 선택하세요.');
      return;
    }

    setCreatingOrderDraft(true);
    setError(null);

    try {
      const created = await requestJson<OrderDraft>('/api/order-drafts/from-inbound-message', {
        method: 'POST',
        body: JSON.stringify({
          sourceInboundMessageId: selectedInboundMessage.inboundMessageId
        })
      });
      setOrderDrafts((currentRows) => [created, ...currentRows]);
      setSelectedOrderDraftId(created.orderDraftId);
      await loadInboundMessages();
      message.success('출고표 초안이 생성되었습니다.');
    } catch (cause) {
      const description = errorMessage(cause, '출고표 초안을 생성하지 못했습니다.');
      setError(description);
      message.error(description);
    } finally {
      setCreatingOrderDraft(false);
    }
  }, [loadInboundMessages, selectedInboundMessage]);

  const reopenShippingOrderReview = useCallback(async (shippingOrder: OrderDraft) => {
    const reopenReason = window.prompt('재검수 전환 사유를 입력하세요.');
    if (reopenReason == null) {
      return;
    }
    if (!reopenReason.trim()) {
      message.warning('재검수 전환 사유를 입력하세요.');
      return;
    }
    setReopeningShippingOrderId(shippingOrder.orderDraftId);
    setError(null);
    try {
      const reopened = await requestJson<OrderDraft>(`/api/shipping-orders/${shippingOrder.orderDraftId}/reopen-review`, {
        method: 'POST',
        body: JSON.stringify({ reopenReason: reopenReason.trim() })
      });
      setShippingOrders((currentRows) => currentRows.filter((row) => row.orderDraftId !== reopened.orderDraftId));
      setOrderDrafts((currentRows) => [
        reopened,
        ...currentRows.filter((row) => row.orderDraftId !== reopened.orderDraftId)
      ]);
      updateSelectedShippingOrderId(null);
      setSelectedOrderDraftId(reopened.orderDraftId);
      setActiveNavigationKey('drafts');
      await loadAuditLogs();
      message.success('출고표를 재검수 상태로 전환했습니다.');
    } catch (cause) {
      const description = errorMessage(cause, '출고표를 재검수 상태로 전환하지 못했습니다.');
      setError(description);
      message.error(description);
    } finally {
      setReopeningShippingOrderId(null);
    }
  }, [loadAuditLogs, updateSelectedShippingOrderId]);

  const downloadCourierCsv = useCallback(async (shippingOrder: OrderDraft) => {
    setDownloadingCourierCsvId(shippingOrder.orderDraftId);
    setError(null);

    try {
      const response = await fetch(
        `${apiBaseUrl}/api/shipping-orders/${shippingOrder.orderDraftId}/courier-csv?courierCsvTemplate=${courierCsvTemplate}`,
        { headers: authHeaders() }
      );
      if (!response.ok) {
        throw new Error(`API가 HTTP ${response.status}를 반환했습니다.`);
      }

      const blob = await response.blob();
      const downloadUrl = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = downloadUrl;
      link.download = `courier-${courierCsvTemplate.toLowerCase()}-${shippingOrder.orderDraftId}.csv`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(downloadUrl);
      message.success('택배 CSV를 다운로드했습니다.');
    } catch (cause) {
      const description = errorMessage(cause, '택배 CSV를 다운로드하지 못했습니다.');
      setError(description);
      message.error(description);
    } finally {
      setDownloadingCourierCsvId(null);
    }
  }, [courierCsvTemplate]);

  const downloadEmailAttachment = useCallback(async (attachment: EmailAttachment) => {
    setDownloadingAttachmentId(attachment.emailAttachmentId);
    setError(null);
    try {
      const result = await requestJson<EmailAttachmentDownloadUrl>(
        `/api/email-orders/attachments/${attachment.emailAttachmentId}/download-url`
      );
      window.open(result.downloadUrl, '_blank', 'noopener,noreferrer');
      message.success(`이메일 첨부파일 다운로드 링크를 열었습니다. 보관 기간 ${result.retentionDays}일`);
    } catch (cause) {
      const description = errorMessage(cause, '이메일 첨부파일 다운로드 URL을 생성하지 못했습니다.');
      setError(description);
      message.error(description);
    } finally {
      setDownloadingAttachmentId(null);
    }
  }, []);

  const extractAiCandidates = useCallback(async () => {
    if (!selectedOrderDraft) {
      message.warning('AI 추출할 초안을 선택하세요.');
      return;
    }

    setExtractingAiOrderDraftId(selectedOrderDraft.orderDraftId);
    setError(null);

    try {
      const result = await requestJson<{ extractionMode: string; orderDraft: OrderDraft }>(
        `/api/order-drafts/${selectedOrderDraft.orderDraftId}/ai-extract`,
        { method: 'POST' }
      );
      setOrderDrafts((currentRows) =>
        currentRows.map((row) => (row.orderDraftId === result.orderDraft.orderDraftId ? result.orderDraft : row))
      );
      setSelectedOrderDraftId(result.orderDraft.orderDraftId);
      message.success(result.extractionMode === 'AI' ? 'AI 후보 추출을 반영했습니다.' : '규칙 기반 후보를 반영했습니다.');
    } catch (cause) {
      const description = errorMessage(cause, '주문 후보 추출에 실패했습니다.');
      setError(description);
      message.error(description);
    } finally {
      setExtractingAiOrderDraftId(null);
    }
  }, [selectedOrderDraft]);

  const downloadBulkCourierCsv = useCallback(async () => {
    if (!filteredShippingOrders.length) {
      message.warning('다운로드할 출고표가 없습니다.');
      return;
    }

    setBulkCsvDownloading(true);
    setError(null);

    try {
      const response = await fetch(`${apiBaseUrl}/api/shipping-orders/courier-csv`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...authHeaders() },
        body: JSON.stringify({
          orderDraftIds: filteredShippingOrders.map((row) => row.orderDraftId),
          courierCsvTemplate
        })
      });
      if (!response.ok) {
        throw new Error(`API가 HTTP ${response.status}를 반환했습니다.`);
      }
      const blob = await response.blob();
      const downloadUrl = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = downloadUrl;
      link.download = `courier-${courierCsvTemplate.toLowerCase()}-bulk.csv`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(downloadUrl);
      message.success('일괄 택배 CSV를 다운로드했습니다.');
    } catch (cause) {
      const description = errorMessage(cause, '일괄 택배 CSV를 다운로드하지 못했습니다.');
      setError(description);
      message.error(description);
    } finally {
      setBulkCsvDownloading(false);
    }
  }, [courierCsvTemplate, filteredShippingOrders]);

  const validateCourierCsv = useCallback(async () => {
    const validationTargets = selectedShippingOrder ? [selectedShippingOrder] : filteredShippingOrders;
    if (!validationTargets.length) {
      message.warning('검증할 출고표가 없습니다.');
      return;
    }
    try {
      const result = await requestJson<{ valid: boolean; errors: string[]; errorDetails?: CourierCsvValidationError[] }>('/api/shipping-orders/courier-csv/validate', {
        method: 'POST',
        body: JSON.stringify({
          orderDraftIds: validationTargets.map((row) => row.orderDraftId),
          courierCsvTemplate
        })
      });
      if (result.valid) {
        setCourierCsvValidationErrors([]);
        message.success('택배 CSV 필수값 검증을 통과했습니다.');
      } else {
        setCourierCsvValidationErrors(result.errorDetails ?? result.errors.map((messageText) => ({
          orderDraftId: orderDraftIdFromValidationError(messageText),
          fieldName: '-',
          csvColumnName: null,
          message: messageText
        })));
        message.warning(result.errors.join(' / '));
      }
    } catch (cause) {
      const description = errorMessage(cause, '택배 CSV 필수값 검증에 실패했습니다.');
      setError(description);
      message.error(description);
    }
  }, [courierCsvTemplate, filteredShippingOrders, selectedShippingOrder]);

  const loadOperationLogs = useCallback(async () => {
    try {
      const [audits, errors] = await Promise.all([
        requestJson<AuditLog[]>('/api/operation/audit-logs?limit=20'),
        requestJson<unknown[]>('/api/operation/error-logs?limit=20')
      ]);
      setAuditLogs(audits);
      setErrorLogs(errors);
      message.success('운영 로그를 불러왔습니다.');
    } catch (cause) {
      const description = errorMessage(cause, '운영 로그를 불러오지 못했습니다.');
      setError(description);
      message.error(description);
    }
  }, []);

  const exportBackup = useCallback(async () => {
    try {
      const backup = await requestJson<unknown>('/api/operation/backups/export');
      setBackupPreview(JSON.stringify(backup, null, 2).slice(0, 4000));
      message.success('백업 데이터를 불러왔습니다.');
    } catch (cause) {
      const description = errorMessage(cause, '백업 데이터를 불러오지 못했습니다.');
      setError(description);
      message.error(description);
    }
  }, []);

  const approveRestore = useCallback(async () => {
    try {
      const result = await requestJson<unknown>('/api/operation/backups/restore-approve', { method: 'POST' });
      setBackupPreview(JSON.stringify(result, null, 2));
      message.success('staging 복구를 승인했습니다.');
    } catch (cause) {
      const description = errorMessage(cause, 'staging 복구를 승인하지 못했습니다.');
      setError(description);
      message.error(description);
    }
  }, []);

  const openPrintView = useCallback((shippingOrder: OrderDraft, printType: 'shipping' | 'work' | 'delivery') => {
    const titleMap = {
      shipping: '출고표',
      work: '작업지시서',
      delivery: '납품서'
    };
    const printWindow = window.open('', '_blank', 'width=920,height=720');
    if (!printWindow) {
      message.error('인쇄 창을 열지 못했습니다.');
      return;
    }

    printWindow.document.write(printDocumentHtml(titleMap[printType], shippingOrder));
    printWindow.document.close();
    printWindow.focus();
    printWindow.print();
  }, []);

  const inboundMessageColumns = useMemo<ColumnsType<InboundMessage>>(
    () => [
      {
        title: '상태',
        dataIndex: 'inboundMessageStatus',
        width: 120,
        render: (status: InboundMessageStatus) => (
          <Tag color={inboundStatusColors[status]}>{inboundStatusLabels[status]}</Tag>
        )
      },
      {
        title: '입력 채널',
        dataIndex: 'sourceChannel',
        width: 150,
        render: (sourceChannel: SourceChannel) => sourceChannelLabels[sourceChannel]
      },
      {
        title: '거래처',
        dataIndex: 'customerId',
        width: 170,
        render: (customerId: string | null) => customerId || '-'
      },
      {
        title: '주문 원문',
        dataIndex: 'rawText',
        ellipsis: true,
        render: (rawText: string) => rawText
      },
      {
        title: '접수 시각',
        dataIndex: 'receivedAt',
        width: 180,
        render: formatDate
      }
    ],
    []
  );

  const orderDraftColumns = useMemo<ColumnsType<OrderDraft>>(
    () => [
      {
        title: '상태',
        dataIndex: 'orderDraftStatus',
        width: 100,
        render: (status: OrderDraftStatus) => (
          <Tag color={orderDraftStatusColors[status]}>{orderDraftStatusLabels[status]}</Tag>
        )
      },
      {
        title: '입력 채널',
        dataIndex: 'sourceChannel',
        width: 150,
        render: (sourceChannel: SourceChannel) => sourceChannelLabels[sourceChannel]
      },
      {
        title: '거래처',
        dataIndex: 'customerId',
        width: 170,
        render: (customerId: string | null) => customerId || '-'
      },
      {
        title: '초안 원문',
        dataIndex: 'rawText',
        ellipsis: true,
        render: (rawText: string) => rawText
      },
      {
        title: '생성 시각',
        dataIndex: 'createdAt',
        width: 180,
        render: formatDate
      },
      {
        title: '처리',
        width: 110,
        render: (_: unknown, row) => (
          <Button
            danger
            size="small"
            disabled={!isAdmin || row.orderDraftStatus === 'CANCELED'}
            onClick={(event) => {
              event.stopPropagation();
              cancelOrderDraft(row.orderDraftId);
            }}
          >
            취소
          </Button>
        )
      }
    ],
    [cancelOrderDraft, isAdmin]
  );

  const shippingOrderColumns = useMemo<ColumnsType<OrderDraft>>(
    () => [
      {
        title: '상태',
        dataIndex: 'orderDraftStatus',
        width: 100,
        render: (status: OrderDraftStatus) => (
          <Tag color={orderDraftStatusColors[status]}>{orderDraftStatusLabels[status]}</Tag>
        )
      },
      {
        title: '거래처',
        dataIndex: 'customerId',
        width: 170,
        render: (customerId: string | null) => customerId || '-'
      },
      {
        title: '품목 수',
        dataIndex: 'itemCandidates',
        width: 100,
        render: (itemCandidates: OrderDraftItemCandidate[]) => itemCandidates?.length ?? 0
      },
      {
        title: '납품 메모',
        dataIndex: 'deliveryMemo',
        ellipsis: true,
        render: (deliveryMemo: string | null) => deliveryMemo || '-'
      },
      {
        title: '제출 시각',
        dataIndex: 'updatedAt',
        width: 180,
        render: formatDate
      }
    ],
    []
  );

  const customerColumns = useMemo<ColumnsType<Customer>>(
    () => [
      {
        title: '고객 ID',
        dataIndex: 'customerId',
        width: 180
      },
      {
        title: '고객명',
        dataIndex: 'customerName',
        width: 160
      },
      {
        title: '연락처',
        dataIndex: 'customerPhone',
        width: 150,
        render: (customerPhone: string | null) => customerPhone || '-'
      },
      {
        title: '기본 주소',
        dataIndex: 'defaultAddress',
        ellipsis: true,
        render: (_: string | null, row) => [row.defaultAddress, row.defaultAddressDetail].filter(Boolean).join(' ') || '-'
      },
      {
        title: '단가표',
        dataIndex: 'priceItems',
        width: 100,
        render: (priceItems: CustomerPriceItem[]) => priceItems?.length ?? 0
      },
      {
        title: '처리',
        width: 110,
        render: (_: unknown, row) => (
          <Button
            danger
            size="small"
            disabled={!isAdmin}
            onClick={(event) => {
              event.stopPropagation();
              deleteCustomer(row.customerId);
            }}
          >
            보관
          </Button>
        )
      }
    ],
    [deleteCustomer, isAdmin]
  );

  return (
    <Layout className="app-shell">
      <Layout.Sider className="sidebar" width={264} breakpoint="lg" collapsedWidth={0}>
        <div className="sidebar-brand">
          <Typography.Title level={3} className="brand">
            출고비서
          </Typography.Title>
          <Typography.Text className="sidebar-subtitle">가맹점 주문 처리</Typography.Text>
        </div>
        <Menu
          mode="inline"
          className="sidebar-menu"
          selectedKeys={[activeNavigationKey]}
          items={navigationItems}
          onClick={({ key }) => moveToNavigationSection(String(key))}
        />
      </Layout.Sider>
      <Layout className="main-shell">
        <Layout.Header className="topbar">
          <div>
            <Typography.Title level={3} className="brand">
              포장재 주문제작
            </Typography.Title>
            <Typography.Text type="secondary">주문 원문부터 출고표와 택배 CSV까지 한 화면에서 처리합니다.</Typography.Text>
          </div>
          <Space wrap>
            {loginUser ? <Typography.Text>{loginUser.displayName} / {loginUser.role}</Typography.Text> : null}
            <Button
              icon={<ReloadOutlined />}
              onClick={reloadAll}
              disabled={!authToken}
              loading={loadingInboundMessages || loadingOrderDrafts || loadingShippingOrders || loadingCustomers}
            >
              새로고침
            </Button>
            {authToken ? <Button onClick={logout}>로그아웃</Button> : null}
          </Space>
        </Layout.Header>
        <Layout.Content className="content">
        <Space orientation="vertical" size={16} className="content-stack">
          {!authToken ? (
            <section id="login" className="work-panel">
              <div className="section-heading">
                <Typography.Title level={2}>로그인</Typography.Title>
                <Typography.Text type="secondary">운영 화면을 사용하려면 이용자 계정으로 로그인하세요.</Typography.Text>
              </div>
              <Form
                form={loginForm}
                layout="vertical"
                initialValues={{ merchantId: 'merchant_local_packaging', role: 'ADMIN' }}
              >
                <Row gutter={12}>
                  <Col xs={24} md={8}>
                    <Form.Item name="merchantId" label="이용자 ID">
                      <Input placeholder="merchant_local_packaging" />
                    </Form.Item>
                  </Col>
                  <Col xs={24} md={8}>
                    <Form.Item name="loginId" label="로그인 ID" rules={[{ required: true, message: '로그인 ID를 입력하세요.' }]}>
                      <Input placeholder="owner" />
                    </Form.Item>
                  </Col>
                  <Col xs={24} md={8}>
                    <Form.Item name="password" label="비밀번호" rules={[{ required: true, message: '비밀번호를 입력하세요.' }]}>
                      <Input.Password />
                    </Form.Item>
                  </Col>
                  <Col xs={24} md={8}>
                    <Form.Item name="displayName" label="표시 이름">
                      <Input placeholder="대표 관리자" />
                    </Form.Item>
                  </Col>
                  <Col xs={24} md={8}>
                    <Form.Item name="role" label="권한">
                      <Select options={[
                        { value: 'ADMIN', label: '관리자' },
                        { value: 'OPERATOR', label: '운영자' }
                      ]} />
                    </Form.Item>
                  </Col>
                </Row>
                <Space wrap>
                  <Button
                    type="primary"
                    loading={authLoading}
                    onClick={() => loginForm.validateFields().then((values) => submitAuth(values, 'login'))}
                  >
                    로그인
                  </Button>
                  <Button
                    loading={authLoading}
                    onClick={() => loginForm.validateFields().then((values) => submitAuth(values, 'register'))}
                  >
                    계정 생성
                  </Button>
                </Space>
              </Form>
            </section>
          ) : null}
          {error ? <Alert type="error" title="요청 실패" description={error} showIcon /> : null}

          {dashboard ? (
            <Suspense fallback={<div className="empty-panel">화면을 불러오는 중입니다.</div>}>
              <DashboardScreen
                dashboard={dashboard}
                dashboardDays={dashboardDays}
                active={Boolean(authToken) && activeNavigationKey === 'dashboard'}
                onDaysChange={setDashboardDays}
                onShowRecentInbound={(inboundMessageId) => {
                  if (inboundMessageId) {
                    setSelectedInboundMessageId(inboundMessageId);
                  }
                  moveToNavigationSection('inbound');
                }}
              />
            </Suspense>
          ) : null}

          <section
            id="inbound"
            className={`work-panel navigation-screen ${authToken && activeNavigationKey === 'inbound' ? 'is-active' : ''}`}
          >
            <div className="section-heading">
              <Typography.Title level={2}>주문 원문 접수</Typography.Title>
              <Typography.Text type="secondary">
                카카오톡, 문자, 이메일에서 받은 주문 내용을 출고표 초안으로 넘깁니다.
              </Typography.Text>
            </div>
            <Form
              form={form}
              layout="vertical"
              initialValues={{
                merchantId: 'merchant_local_packaging',
                merchantStaffId: 'staff_local_001',
                sourceChannel: 'KAKAO_PASTE',
                businessChannel: 'PACKAGING_CUSTOM'
              }}
              onFinish={createInboundMessage}
            >
              <Row gutter={12}>
                <Col xs={24} md={8}>
                  <Form.Item name="merchantId" label="이용자 ID" rules={[{ required: true, message: '이용자 ID를 입력하세요.' }]}>
                    <Input placeholder="merchant_local_packaging" />
                  </Form.Item>
                </Col>
                <Col xs={24} md={8}>
                  <Form.Item name="merchantStaffId" label="담당자 ID">
                    <Input placeholder="staff_local_001" />
                  </Form.Item>
                </Col>
                <Col xs={24} md={8}>
                  <Form.Item name="customerId" label="고객 ID">
                    <Input placeholder="customer_sample_001" />
                  </Form.Item>
                </Col>
                <Col xs={24} md={8}>
                  <Form.Item name="sourceChannel" label="입력 채널" rules={[{ required: true, message: '입력 채널을 선택하세요.' }]}>
                    <Select
                      options={Object.entries(sourceChannelLabels).map(([value, label]) => ({ value, label }))}
                    />
                  </Form.Item>
                </Col>
                <Col xs={24} md={8}>
                  <Form.Item name="businessChannel" label="업종 채널" rules={[{ required: true, message: '업종 채널을 선택하세요.' }]}>
                    <Select
                      options={Object.entries(businessChannelLabels).map(([value, label]) => ({ value, label }))}
                    />
                  </Form.Item>
                </Col>
                <Col xs={24}>
                  <Form.Item name="rawText" label="주문 원문" rules={[{ required: true, message: '주문 원문을 입력하세요.' }]}>
                    <Input.TextArea
                      rows={4}
                      maxLength={20000}
                      showCount
                      placeholder="예: 종이박스 300x200x150 200장, 로고 스티커 1,000장 부탁드립니다."
                    />
                  </Form.Item>
                </Col>
              </Row>
              <Button type="primary" htmlType="submit" icon={<PlusOutlined />} loading={creatingInboundMessage}>
                주문 원문 접수
              </Button>
            </Form>
          </section>

          <Row
            gutter={[16, 16]}
            align="stretch"
            className={`navigation-screen ${authToken && activeNavigationKey === 'customers' ? 'is-active' : ''}`}
          >
            <Col xs={24} xl={10}>
              <section id="customers" className="work-panel full-height">
                <Typography.Title level={2}>고객/단가표</Typography.Title>
                <Form
                  form={customerForm}
                  layout="vertical"
                  initialValues={{
                    merchantId: 'merchant_local_packaging',
                    priceItems: [emptyCustomerPriceItem()]
                  }}
                  onFinish={saveCustomer}
                >
                  <Row gutter={8}>
                    <Col xs={24} md={12}>
                      <Form.Item name="customerId" label="고객 ID">
                        <Input placeholder="customer_sample_001" />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={12}>
                      <Form.Item name="merchantId" label="이용자 ID" rules={[{ required: true, message: '이용자 ID를 입력하세요.' }]}>
                        <Input placeholder="merchant_local_packaging" />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={12}>
                      <Form.Item name="customerName" label="고객명" rules={[{ required: true, message: '고객명을 입력하세요.' }]}>
                        <Input placeholder="한국포장" />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={12}>
                      <Form.Item name="customerPhone" label="고객 연락처">
                        <Input placeholder="02-000-0000" />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={12}>
                      <Form.Item name="defaultRecipientName" label="기본 수취인">
                        <Input placeholder="홍길동" />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={12}>
                      <Form.Item name="defaultRecipientPhone" label="기본 수취인 연락처">
                        <Input placeholder="010-0000-0000" />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={8}>
                      <Form.Item name="defaultPostalCode" label="우편번호">
                        <Input placeholder="12345" />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={16}>
                      <Form.Item name="defaultAddress" label="주소">
                        <Input placeholder="서울시 강남구 테헤란로 123" />
                      </Form.Item>
                    </Col>
                    <Col xs={24}>
                      <Form.Item name="defaultAddressDetail" label="상세주소">
                        <Input placeholder="4층 401호" />
                      </Form.Item>
                    </Col>
                    <Col xs={24}>
                      <Form.Item name="favoriteItemNamesText" label="자주 쓰는 품목">
                        <Input placeholder="종이박스, 로고 스티커" />
                      </Form.Item>
                    </Col>
                  </Row>
                  <Form.List name="priceItems">
                    {(fields, { add, remove }) => (
                      <Space orientation="vertical" size={8} className="review-items">
                        <div className="table-heading compact-heading">
                          <Typography.Title level={3} className="compact-title">고객별 단가표</Typography.Title>
                          <Button icon={<PlusOutlined />} onClick={() => add(emptyCustomerPriceItem())}>
                            단가 추가
                          </Button>
                        </div>
                        {fields.map((field, index) => (
                          <div className="review-item-row" key={field.key}>
                            <div className="review-item-title">
                              <Typography.Text strong>단가 {index + 1}</Typography.Text>
                              <Button
                                danger
                                icon={<DeleteOutlined />}
                                onClick={() => remove(field.name)}
                                aria-label={`단가 ${index + 1} 삭제`}
                              />
                            </div>
                            <Form.Item name={[field.name, 'customerPriceItemId']} hidden>
                              <Input />
                            </Form.Item>
                            <Row gutter={8}>
                              <Col xs={24} md={12}>
                                <Form.Item name={[field.name, 'itemNameKeyword']} label="품목 키워드">
                                  <Input placeholder="종이박스" />
                                </Form.Item>
                              </Col>
                              <Col xs={24} md={12}>
                                <Form.Item name={[field.name, 'itemSpecKeyword']} label="규격 키워드">
                                  <Input placeholder="300x200x150" />
                                </Form.Item>
                              </Col>
                              <Col xs={12}>
                                <Form.Item name={[field.name, 'itemUnit']} label="단위">
                                  <Input placeholder="장" />
                                </Form.Item>
                              </Col>
                              <Col xs={12}>
                                <Form.Item name={[field.name, 'unitPrice']} label="단가">
                                  <InputNumber min={0} precision={0} className="full-width" placeholder="450" />
                                </Form.Item>
                              </Col>
                            </Row>
                          </div>
                        ))}
                      </Space>
                    )}
                  </Form.List>
                  <Button type="primary" htmlType="submit" icon={<SaveOutlined />} loading={savingCustomer}>
                    고객 저장
                  </Button>
                </Form>
              </section>
            </Col>
            <Col xs={24} xl={14}>
              <section className="work-panel full-height">
                <Typography.Title level={2}>고객 목록</Typography.Title>
                <Table
                  rowKey="customerId"
                  columns={customerColumns}
                  dataSource={customers}
                  loading={loadingCustomers}
                  size="middle"
                  scroll={{ x: 760 }}
                  pagination={{ pageSize: 8, showSizeChanger: false }}
                  onRow={(row) => ({
                    onClick: () => {
                      customerForm.setFieldsValue({
                        customerId: row.customerId,
                        merchantId: row.merchantId,
                        customerName: row.customerName,
                        customerPhone: row.customerPhone ?? '',
                        defaultRecipientName: row.defaultRecipientName ?? '',
                        defaultRecipientPhone: row.defaultRecipientPhone ?? '',
                        defaultPostalCode: row.defaultPostalCode ?? '',
                        defaultAddress: row.defaultAddress ?? '',
                        defaultAddressDetail: row.defaultAddressDetail ?? '',
                        favoriteItemNamesText: row.favoriteItemNames.join(', '),
                        priceItems: row.priceItems.length ? row.priceItems : [emptyCustomerPriceItem()]
                      });
                    }
                  })}
                />
              </section>
            </Col>
          </Row>

          <Row
            gutter={[16, 16]}
            align="stretch"
            className={`navigation-screen ${authToken && activeNavigationKey === 'inbound' ? 'is-active' : ''}`}
          >
            <Col xs={24} xl={14}>
              <section className="work-panel full-height">
                <div className="table-heading">
                  <div>
                    <Typography.Title level={2}>주문 원문 목록</Typography.Title>
                    <Typography.Text type="secondary">선택한 원문으로 출고표 초안을 생성합니다.</Typography.Text>
                  </div>
                  <Button
                    type="primary"
                    icon={<SendOutlined />}
                    onClick={createOrderDraft}
                    loading={creatingOrderDraft}
                    disabled={!selectedInboundMessage}
                  >
                    초안 생성
                  </Button>
                </div>
                <Table
                  rowKey="inboundMessageId"
                  columns={inboundMessageColumns}
                  dataSource={inboundMessages}
                  loading={loadingInboundMessages}
                  size="middle"
                  scroll={{ x: 850 }}
                  pagination={{ pageSize: 8, showSizeChanger: false }}
                  rowSelection={{
                    type: 'radio',
                    selectedRowKeys: selectedInboundMessageId ? [selectedInboundMessageId] : [],
                    onChange: (keys) => setSelectedInboundMessageId(String(keys[0]))
                  }}
                  onRow={(row) => ({
                    onClick: () => setSelectedInboundMessageId(row.inboundMessageId)
                  })}
                />
              </section>
            </Col>
            <Col xs={24} xl={10}>
              <section className="work-panel full-height">
                <Typography.Title level={2}>선택 원문</Typography.Title>
                {selectedInboundMessage ? (
                  <>
                    <Descriptions size="small" column={1} bordered>
                      <Descriptions.Item label="주문 원문 ID">{selectedInboundMessage.inboundMessageId}</Descriptions.Item>
                      <Descriptions.Item label="상태">
                        <Tag color={inboundStatusColors[selectedInboundMessage.inboundMessageStatus]}>
                          {inboundStatusLabels[selectedInboundMessage.inboundMessageStatus]}
                        </Tag>
                      </Descriptions.Item>
                      <Descriptions.Item label="입력 채널">
                        {sourceChannelLabels[selectedInboundMessage.sourceChannel]}
                      </Descriptions.Item>
                      <Descriptions.Item label="업종 채널">
                        {businessChannelLabels[selectedInboundMessage.businessChannel]}
                      </Descriptions.Item>
                      <Descriptions.Item label="접수 시각">{formatDate(selectedInboundMessage.receivedAt)}</Descriptions.Item>
                    </Descriptions>
                    <Divider />
                    <Typography.Paragraph className="raw-text-block">{selectedInboundMessage.rawText}</Typography.Paragraph>
                    <EmailAttachmentList
                      attachments={inboundAttachments}
                      loading={loadingInboundAttachments}
                      downloadingAttachmentId={downloadingAttachmentId}
                      onDownload={downloadEmailAttachment}
                    />
                  </>
                ) : (
                  <div className="empty-panel">
                    <FileTextOutlined />
                    <span>선택된 주문 원문이 없습니다.</span>
                  </div>
                )}
              </section>
            </Col>
          </Row>

          {authToken ? (
            <Suspense fallback={<section className="work-panel navigation-screen is-active"><div className="empty-panel">초안 검수 화면을 불러오는 중입니다.</div></section>}>
              <DraftReviewScreen
                active={activeNavigationKey === 'drafts'}
                orderDraftStatusLabels={orderDraftStatusLabels}
                orderDraftStatusColors={orderDraftStatusColors}
                list={{
                  orderDraftStatusFilter,
                  orderDraftSearchText,
                  orderDraftColumns,
                  filteredOrderDrafts,
                  loadingOrderDrafts,
                  selectedOrderDraftId,
                  onStatusFilterChange: setOrderDraftStatusFilter,
                  onSearchTextChange: setOrderDraftSearchText,
                  onSelectedOrderDraftChange: setSelectedOrderDraftId
                }}
                detail={{
                  selectedOrderDraft,
                  selectedOrderDraftCustomerExists: Boolean(selectedOrderDraftCustomer),
                  selectedOrderDraftCustomerPriceItemCount: selectedOrderDraftCustomer?.priceItems.length ?? 0,
                  selectedOrderDraftMissingFields,
                  reviewForm,
                  draftAttachmentList: <EmailAttachmentList attachments={draftAttachments} loading={loadingDraftAttachments} downloadingAttachmentId={downloadingAttachmentId} onDownload={downloadEmailAttachment} />
                }}
                actions={{
                  extractingAiOrderDraftId,
                  savingOrderDraftReview,
                  submittingOrderDraft,
                  canOperate,
                  onApplyCustomerDefaults: applySelectedCustomerDefaults,
                  onApplyCustomerPriceCandidates: applyCustomerPriceCandidates,
                  onExtractAiCandidates: extractAiCandidates,
                  onSaveOrderDraftReview: saveOrderDraftReview,
                  onSubmitOrderDraft: submitOrderDraft,
                  emptyItemCandidate
                }}
                formatDate={formatDate}
              />
            </Suspense>
          ) : null}

          {authToken ? (
            <Suspense fallback={<section className="work-panel navigation-screen is-active"><div className="empty-panel">출고/문서 화면을 불러오는 중입니다.</div></section>}>
              <ShippingScreen
                active={activeNavigationKey === 'shipping'}
                shippingDaysFilter={shippingDaysFilter}
                shippingCsvFilter={shippingCsvFilter}
                shippingSearchText={shippingSearchText}
                courierCsvValidationErrors={courierCsvValidationErrors}
                shippingOrderColumns={shippingOrderColumns}
                filteredShippingOrders={filteredShippingOrders}
                loadingShippingOrders={loadingShippingOrders}
                selectedShippingOrderId={selectedShippingOrderId}
                selectedShippingOrder={selectedShippingOrder}
                selectedShippingOrderMissingFields={selectedShippingOrderMissingFields}
                selectedShippingOrderSubmissionCount={selectedShippingOrderSubmissionCount}
                selectedShippingOrderAuditSections={selectedShippingOrderAuditSections}
                shippingOrderDiffFieldFilter={shippingOrderDiffFieldFilter}
                courierCsvTemplate={courierCsvTemplate}
                courierCsvTemplateLabels={courierCsvTemplateLabels}
                downloadingCourierCsvId={downloadingCourierCsvId}
                reopeningShippingOrderId={reopeningShippingOrderId}
                bulkCsvDownloading={bulkCsvDownloading}
                canOperate={canOperate}
                onShippingDaysFilterChange={setShippingDaysFilter}
                onShippingCsvFilterChange={setShippingCsvFilter}
                onShippingSearchTextChange={setShippingSearchText}
                onBulkCourierCsvDownload={downloadBulkCourierCsv}
                onCourierCsvValidate={validateCourierCsv}
                onSelectedShippingOrderChange={updateSelectedShippingOrderId}
                onCourierCsvTemplateChange={setCourierCsvTemplate}
                onCourierCsvDownload={downloadCourierCsv}
                onReopenShippingOrderReview={reopenShippingOrderReview}
                onPrintView={openPrintView}
                onDiffFieldFilterChange={updateShippingOrderDiffFieldFilter}
                formatDate={formatDate}
                formatCurrency={formatCurrency}
                auditActionText={auditActionText}
                auditMetadataText={auditMetadataText}
                filteredItemDiffRows={filteredItemDiffRows}
                diffChangeTypeText={diffChangeTypeText}
                diffChangeTypeColor={diffChangeTypeColor}
                orderDraftStatusLabels={orderDraftStatusLabels}
                orderDraftStatusColors={orderDraftStatusColors}
                sourceChannelLabels={sourceChannelLabels}
              />
            </Suspense>
          ) : null}

          {authToken && activeNavigationKey === 'operations' ? (
            <Suspense fallback={<section className="work-panel navigation-screen is-active"><div className="empty-panel">운영 화면을 불러오는 중입니다.</div></section>}>
              <OperationsScreen
                auditLogs={auditLogs}
                errorLogs={errorLogs}
                backupPreview={backupPreview}
                onLoadOperationLogs={loadOperationLogs}
                onExportBackup={exportBackup}
                onApproveRestore={approveRestore}
              />
            </Suspense>
          ) : null}

          <Row
            gutter={[16, 16]}
            className={`navigation-screen ${authToken && activeNavigationKey === 'settings' ? 'is-active' : ''}`}
          >
            <Col xs={24} lg={12}>
              <section id="settings" className="work-panel full-height">
                <Typography.Title level={2}>이메일 수집 설정</Typography.Title>
                <Form
                  form={emailMailboxForm}
                  layout="vertical"
                  initialValues={{
                    merchantId: loginUser?.merchantId ?? 'merchant_local_packaging',
                    host: 'imap.gmail.com',
                    port: 993,
                    sslEnabled: true
                  }}
                  onFinish={saveEmailMailboxConfig}
                >
                  <Row gutter={8}>
                    <Col xs={24} md={12}>
                      <Form.Item name="merchantId" label="이용자 ID" rules={[{ required: true, message: '이용자 ID를 입력하세요.' }]}>
                        <Input />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={12}>
                      <Form.Item name="host" label="메일 서버" rules={[{ required: true, message: '메일 서버를 입력하세요.' }]}>
                        <Input />
                      </Form.Item>
                    </Col>
                    <Col xs={12}>
                      <Form.Item name="port" label="포트" rules={[{ required: true, message: '포트를 입력하세요.' }]}>
                        <InputNumber className="full-width" min={1} max={65535} />
                      </Form.Item>
                    </Col>
                    <Col xs={12}>
                      <Form.Item name="sslEnabled" valuePropName="checked" label="보안 연결">
                        <Checkbox>SSL 사용</Checkbox>
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={12}>
                      <Form.Item name="username" label="메일 계정" rules={[{ required: true, message: '메일 계정을 입력하세요.' }]}>
                        <Input />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={12}>
                      <Form.Item name="encryptedPasswordMemo" label="비밀번호 보관 메모" rules={[{ required: true, message: '비밀번호 보관 메모를 입력하세요.' }]}>
                        <Input.Password placeholder="운영 비밀 저장소 키 또는 앱 비밀번호 메모" />
                      </Form.Item>
                    </Col>
                  </Row>
                  <Space wrap>
                    <Button type="primary" htmlType="submit" disabled={!isAdmin}>설정 저장</Button>
                    <Button onClick={collectEmailMailbox}>연결 확인</Button>
                  </Space>
                </Form>
                {emailMailboxStatus ? <Alert className="status-alert" type="info" message={emailMailboxStatus} /> : null}
                <Divider />
                <Typography.Title level={3} className="compact-title">Google Gmail 연결</Typography.Title>
                <Descriptions size="small" column={1} bordered>
                  <Descriptions.Item label="연결 상태">
                    {googleOAuthStatus?.connected ? <Tag color="success">연결됨</Tag> : <Tag>미연결</Tag>}
                  </Descriptions.Item>
                  <Descriptions.Item label="Google 계정">
                    {googleOAuthStatus?.googleEmail || '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="갱신 토큰">
                    {googleOAuthStatus?.hasRefreshToken ? '보유' : '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="토큰 만료">
                    {googleOAuthStatus?.expiresAt ? formatDate(googleOAuthStatus.expiresAt) : '-'}
                  </Descriptions.Item>
                </Descriptions>
                <Descriptions size="small" column={1} bordered className="status-descriptions">
                  <Descriptions.Item label="자동 수집">
                    {googleGmailPollingSummary?.pollingEnabled ? <Tag color="success">사용</Tag> : <Tag>중지</Tag>}
                  </Descriptions.Item>
                  <Descriptions.Item label="수집 주기">
                    {googleGmailPollingSummary ? `${Math.round(googleGmailPollingSummary.pollingDelayMs / 1000)}초` : '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="조회 최대 건수">
                    {googleGmailPollingSummary?.pollingMaxMessages ?? '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="초안 자동 생성">
                    {googleGmailPollingSummary?.autoCreateOrderDrafts ? '사용' : '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="수집 이력">
                    {googleGmailPollingSummary
                      ? `${googleGmailPollingSummary.collectedMessageCount}건, 첨부 ${googleGmailPollingSummary.attachmentStoredCount}건`
                      : '-'}
                  </Descriptions.Item>
	                  <Descriptions.Item label="최근 실행">
	                    {googleGmailPollingSummary
	                      ? `${googleGmailPollingSummary.recentRunCount}회, 실패 ${googleGmailPollingSummary.recentFailureCount}회, 비주문 제외 ${googleGmailPollingSummary.recentSkippedNonOrderCount}건`
	                      : '-'}
	                  </Descriptions.Item>
                  <Descriptions.Item label="마지막 성공">
                    {googleGmailPollingSummary?.latestSuccessAt ? formatDate(googleGmailPollingSummary.latestSuccessAt) : '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="마지막 실패">
                    {googleGmailPollingSummary?.latestFailureAt ? formatDate(googleGmailPollingSummary.latestFailureAt) : '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="최근 실패 코드">
                    {googleGmailPollingSummary?.latestFailureCode || '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="마지막 수집">
                    {googleGmailPollingSummary?.latestCollectedAt ? formatDate(googleGmailPollingSummary.latestCollectedAt) : '-'}
                  </Descriptions.Item>
                </Descriptions>
                {googleGmailPollingSummary?.latestFailureMessage ? (
                  <Alert
                    className="status-alert"
                    type="warning"
                    message="최근 Gmail 수집 실패"
                    description={googleGmailPollingSummary.latestFailureMessage}
                    action={(
                      <Button
                        size="small"
                        onClick={() => {
                          if (googleGmailPollingSummary.latestFailureInboundMessageId) {
                            setSelectedInboundMessageId(googleGmailPollingSummary.latestFailureInboundMessageId);
                          }
                          moveToNavigationSection('inbound');
                        }}
                      >
                        최근 원문 보기
                      </Button>
                    )}
                  />
                ) : null}
                <Space wrap className="settings-actions">
                  <Button type="primary" icon={<MailOutlined />} loading={googleOAuthWorking} onClick={connectGoogleGmail}>
                    Google Gmail 연결
                  </Button>
                  <Button
                    icon={<ReloadOutlined />}
                    loading={googleOAuthWorking}
                    onClick={() => Promise.all([loadGoogleOAuthStatus(), loadGoogleGmailPollingSummary()])}
                  >
                    연결 상태 확인
                  </Button>
	                  <Button icon={<InboxOutlined />} loading={googleOAuthWorking} disabled={!googleOAuthStatus?.connected} onClick={collectGoogleGmail}>
	                    Gmail 수집 실행
	                  </Button>
	                  <Button
	                    danger
	                    icon={<DeleteOutlined />}
	                    loading={attachmentCleanupWorking}
	                    disabled={!isAdmin}
	                    onClick={cleanupExpiredEmailAttachments}
	                  >
	                    만료 첨부 정리
	                  </Button>
	                </Space>
	                {googleGmailPollingStatus ? <Alert className="status-alert" type="success" message={googleGmailPollingStatus} /> : null}
	                {attachmentCleanupStatus ? <Alert className="status-alert" type="info" message={attachmentCleanupStatus} /> : null}
	              </section>
            </Col>
            <Col xs={24} lg={12}>
              <section className="work-panel full-height">
                <Typography.Title level={2}>파일럿 초기 설정</Typography.Title>
                <Form
                  form={settingsForm}
                  layout="vertical"
                  initialValues={{
                    merchantId: loginUser?.merchantId ?? 'merchant_local_packaging',
                    defaultMerchantStaffId: 'staff_local_001',
                    defaultCourierCsvTemplate: 'GENERIC',
                    defaultBusinessChannel: 'PACKAGING_CUSTOM'
                  }}
                  onFinish={saveMerchantSettings}
                >
                  <Row gutter={8}>
                    <Col xs={24} md={12}>
                      <Form.Item name="merchantId" label="이용자 ID" rules={[{ required: true, message: '이용자 ID를 입력하세요.' }]}>
                        <Input />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={12}>
                      <Form.Item name="defaultMerchantStaffId" label="기본 담당자 ID">
                        <Input />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={12}>
                      <Form.Item name="defaultCourierCsvTemplate" label="기본 택배사">
                        <Select options={Object.entries(courierCsvTemplateLabels).map(([value, label]) => ({ value, label }))} />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={12}>
                      <Form.Item name="defaultBusinessChannel" label="기본 업종 채널">
                        <Select options={Object.entries(businessChannelLabels).map(([value, label]) => ({ value, label }))} />
                      </Form.Item>
                    </Col>
                  </Row>
                  <Button type="primary" htmlType="submit" loading={settingsSaving} disabled={!isAdmin}>초기 설정 저장</Button>
                </Form>
              </section>
            </Col>
          </Row>
        </Space>
        </Layout.Content>
      </Layout>
    </Layout>
  );
}

async function requestJson<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders(),
      ...init?.headers
    }
  });

  if (!response.ok) {
    throw new Error(`API가 HTTP ${response.status}를 반환했습니다.`);
  }

  return response.json();
}

function authHeaders(): Record<string, string> {
  const token = localStorage.getItem('shipmate_merchant_token');
  return token ? { Authorization: `Bearer ${token}` } : {};
}

function EmailAttachmentList({
  attachments,
  loading,
  downloadingAttachmentId,
  onDownload
}: {
  attachments: EmailAttachment[];
  loading: boolean;
  downloadingAttachmentId: string | null;
  onDownload: (attachment: EmailAttachment) => void;
}) {
  if (loading) {
    return <Alert className="attachment-list" type="info" message="첨부파일을 확인하고 있습니다." />;
  }
  if (!attachments.length) {
    return null;
  }
  return (
    <div className="attachment-list">
      <Typography.Title level={3}>첨부파일</Typography.Title>
      <Space direction="vertical" size={8} className="full-width">
        {attachments.map((attachment) => (
          <div className="attachment-row" key={attachment.emailAttachmentId}>
            <div className="attachment-meta">
              <Typography.Text strong>{attachment.fileName}</Typography.Text>
              <Typography.Text type="secondary">
                {attachment.contentType} · {formatFileSize(attachment.fileSizeBytes)}
              </Typography.Text>
            </div>
            <Button
              icon={<DownloadOutlined />}
              loading={downloadingAttachmentId === attachment.emailAttachmentId}
              onClick={() => onDownload(attachment)}
            >
              다운로드
            </Button>
          </div>
        ))}
      </Space>
    </div>
  );
}

function MetricCard({ label, value }: { label: string; value: number }) {
  return (
    <div className="metric-card">
      <Typography.Text type="secondary">{label}</Typography.Text>
      <Typography.Title level={3}>{value}</Typography.Title>
    </div>
  );
}

function trimToNull(value?: string | null) {
  if (!value || !value.trim()) {
    return null;
  }
  return value.trim();
}

function emptyItemCandidate(): OrderDraftItemCandidate {
  return {
    itemName: '',
    itemSpec: '',
    itemQuantity: '',
    itemUnit: '',
    matchedUnitPrice: null,
    packagingWidth: '',
    packagingDepth: '',
    packagingHeight: '',
    materialName: '',
    printRequired: false,
    designFileProvided: false,
    productionQuantity: '',
    requestedDueDate: '',
    sampleRequested: false,
    customProductType: '',
    imprintMethod: '',
    itemColor: '',
    customOptionText: '',
    proofConfirmed: false,
    productionConfirmed: false,
    lineMemo: ''
  };
}

function emptyCustomerPriceItem(): CustomerPriceItem {
  return {
    itemNameKeyword: '',
    itemSpecKeyword: '',
    itemUnit: '',
    unitPrice: null
  };
}

function orderDraftMatchesSearch(orderDraft: OrderDraft, searchText: string) {
  const keyword = searchText.trim().toLowerCase();
  if (!keyword) {
    return true;
  }
  const searchableText = [
    orderDraft.orderDraftId,
    orderDraft.sourceInboundMessageId,
    orderDraft.customerId,
    orderDraft.deliveryRecipientName,
    orderDraft.deliveryRecipientPhone,
    orderDraft.deliveryAddress,
    orderDraft.rawText,
    ...orderDraft.itemCandidates.flatMap((itemCandidate) => [
      itemCandidate.itemName,
      itemCandidate.itemSpec,
      itemCandidate.itemQuantity,
      itemCandidate.itemUnit,
      itemCandidate.lineMemo
    ])
  ]
    .filter(Boolean)
    .join(' ')
    .toLowerCase();
  return searchableText.includes(keyword);
}

function orderDraftWithinDays(orderDraft: OrderDraft, days: number | 'ALL') {
  if (days === 'ALL') {
    return true;
  }
  const updatedAt = new Date(orderDraft.updatedAt).getTime();
  if (!Number.isFinite(updatedAt)) {
    return false;
  }
  const threshold = Date.now() - days * 24 * 60 * 60 * 1000;
  return updatedAt >= threshold;
}

function orderDraftSubmissionMissingFields(orderDraft: OrderDraft) {
  const missingFields: string[] = [];
  if (!orderDraft.deliveryRecipientName?.trim()) {
    missingFields.push('수취인');
  }
  if (!orderDraft.deliveryRecipientPhone?.trim()) {
    missingFields.push('연락처');
  }
  if (!orderDraft.deliveryAddress?.trim()) {
    missingFields.push('주소');
  }
  if (!orderDraft.itemCandidates.length) {
    missingFields.push('품목 후보');
  }
  orderDraft.itemCandidates.forEach((itemCandidate, index) => {
    const prefix = `품목 ${index + 1}`;
    if (!itemCandidate.itemName?.trim()) {
      missingFields.push(`${prefix} 품목명`);
    }
    if (!itemCandidate.itemQuantity?.trim()) {
      missingFields.push(`${prefix} 수량`);
    }
    if (!itemCandidate.itemUnit?.trim()) {
      missingFields.push(`${prefix} 단위`);
    }
  });
  return missingFields;
}

function splitCommaText(value?: string) {
  if (!value) {
    return [];
  }
  return value
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
}

function errorMessage(cause: unknown, fallback: string) {
  return cause instanceof Error ? cause.message : fallback;
}

function auditActionText(actionType: string) {
  const labels: Record<string, string> = {
    ORDER_DRAFT_CREATED: '초안 생성',
    ORDER_DRAFT_REVIEW_UPDATED: '검수 저장',
    ORDER_DRAFT_SUBMITTED: '출고표 제출',
    ORDER_DRAFT_REOPENED: '재검수 전환',
    ORDER_DRAFT_CANCELED: '초안 취소',
    ORDER_DRAFT_NEEDS_REVIEW: '검수 필요 전환',
    AI_EXTRACTION_APPLIED: 'AI 후보 반영',
    EMAIL_ATTACHMENT_DOWNLOAD_URL_CREATED: '첨부 다운로드 URL 발급',
    CUSTOMER_SAVED: '고객 저장',
    CUSTOMER_ARCHIVED: '고객 보관'
  };
  return labels[actionType] ?? actionType;
}

type AuditItemCandidateSnapshot = {
  itemName: string;
  itemSpec: string;
  itemQuantity: string;
  itemUnit: string;
  matchedUnitPrice: string;
};

function groupShippingOrderAuditLogs(auditLogs: AuditLog[]) {
  const sortedLogs = [...auditLogs].sort((left, right) => new Date(left.createdAt).getTime() - new Date(right.createdAt).getTime());
  const sections: { title: string; logs: AuditLog[]; itemDiffs: string[]; itemDiffRows: ItemSnapshotDiffRow[] }[] = [];
  let submitCount = 0;
  let previousItemSnapshot: AuditItemCandidateSnapshot[] = [];

  sortedLogs.forEach((auditLog) => {
    if (auditLog.actionType === 'ORDER_DRAFT_SUBMITTED') {
      submitCount += 1;
      const itemSnapshot = auditItemSnapshot(auditLog.auditMetadataJson);
      const itemDiffRows = itemSnapshot.length ? itemSnapshotDiffRows(previousItemSnapshot, itemSnapshot) : [];
      sections.push({
        title: `${submitCount}차 제출`,
        logs: [auditLog],
        itemDiffs: itemDiffRows.length ? itemDiffRows.map(itemDiffRowText) : itemSnapshot.length ? itemSnapshotDiff(previousItemSnapshot, itemSnapshot) : [],
        itemDiffRows
      });
      previousItemSnapshot = itemSnapshot;
      return;
    }
    if (auditLog.actionType === 'ORDER_DRAFT_REOPENED') {
      sections.push({ title: `${submitCount + 1}차 제출 전 재검수`, logs: [auditLog], itemDiffs: [], itemDiffRows: [] });
      return;
    }
    const lastSection = sections[sections.length - 1];
    if (lastSection) {
      lastSection.logs.push(auditLog);
      return;
    }
    sections.push({ title: '제출 전 변경', logs: [auditLog], itemDiffs: [], itemDiffRows: [] });
  });

  return sections.reverse();
}

function auditItemSnapshot(value: string | null | undefined): AuditItemCandidateSnapshot[] {
  if (!value) {
    return [];
  }
  try {
    const parsed = JSON.parse(value) as Record<string, unknown>;
    const candidates = parsed.itemCandidates;
    const parsedCandidates = typeof candidates === 'string' ? JSON.parse(candidates) : candidates;
    if (!Array.isArray(parsedCandidates)) {
      return [];
    }
    return parsedCandidates.map((item) => {
      const source = item as Record<string, unknown>;
      return {
        itemName: String(source.itemName ?? ''),
        itemSpec: String(source.itemSpec ?? ''),
        itemQuantity: String(source.itemQuantity ?? ''),
        itemUnit: String(source.itemUnit ?? ''),
        matchedUnitPrice: String(source.matchedUnitPrice ?? '')
      };
    });
  } catch {
    return [];
  }
}

function itemSnapshotDiff(previousSnapshot: AuditItemCandidateSnapshot[], nextSnapshot: AuditItemCandidateSnapshot[]) {
  if (!previousSnapshot.length) {
    return ['품목 변경: 최초 제출 기준'];
  }
  if (JSON.stringify(previousSnapshot) === JSON.stringify(nextSnapshot)) {
    return ['품목 변경: 이전 제출과 동일'];
  }
  const maxLength = Math.max(previousSnapshot.length, nextSnapshot.length);
  const diffs: string[] = [];
  if (sameItemSet(previousSnapshot, nextSnapshot) && itemOrderSignature(previousSnapshot) !== itemOrderSignature(nextSnapshot)) {
    diffs.push(`품목 순서 변경: ${itemOrderText(previousSnapshot)} -> ${itemOrderText(nextSnapshot)}`);
  }
  for (let index = 0; index < maxLength; index += 1) {
    const previousItem = previousSnapshot[index];
    const nextItem = nextSnapshot[index];
    const label = itemSnapshotLabel(nextItem ?? previousItem, index);
    if (!previousItem && nextItem) {
      diffs.push(`${label}: 품목 추가`);
      continue;
    }
    if (previousItem && !nextItem) {
      diffs.push(`${label}: 품목 삭제`);
      continue;
    }
    if (!previousItem || !nextItem) {
      continue;
    }
    if (previousItem.itemName !== nextItem.itemName) {
      diffs.push(`${label}: 품목명 ${previousItem.itemName || '-'} -> ${nextItem.itemName || '-'}`);
    }
    if (previousItem.itemSpec !== nextItem.itemSpec) {
      diffs.push(`${label}: 규격 ${previousItem.itemSpec || '-'} -> ${nextItem.itemSpec || '-'}`);
    }
    const previousQuantity = [previousItem.itemQuantity, previousItem.itemUnit].filter(Boolean).join(' ');
    const nextQuantity = [nextItem.itemQuantity, nextItem.itemUnit].filter(Boolean).join(' ');
    if (previousQuantity !== nextQuantity) {
      diffs.push(`${label}: 수량 ${previousQuantity || '-'} -> ${nextQuantity || '-'}`);
    }
    if (previousItem.matchedUnitPrice !== nextItem.matchedUnitPrice) {
      diffs.push(`${label}: 단가 ${previousItem.matchedUnitPrice || '-'} -> ${nextItem.matchedUnitPrice || '-'}`);
    }
  }
  return diffs.length ? diffs : ['품목 변경: 이전 제출과 동일'];
}

function itemSnapshotDiffRows(previousSnapshot: AuditItemCandidateSnapshot[], nextSnapshot: AuditItemCandidateSnapshot[]) {
  const rows: ItemSnapshotDiffRow[] = [];
  if (!previousSnapshot.length) {
    nextSnapshot.forEach((item, index) => {
      rows.push({
        rowKey: `initial-${index}`,
        itemLabel: itemSnapshotLabel(item, index),
        fieldLabel: '최초 제출',
        changeType: 'INITIAL',
        previousValue: '-',
        nextValue: itemSnapshotSummary(item)
      });
    });
    return rows;
  }
  const maxLength = Math.max(previousSnapshot.length, nextSnapshot.length);
  if (sameItemSet(previousSnapshot, nextSnapshot) && itemOrderSignature(previousSnapshot) !== itemOrderSignature(nextSnapshot)) {
    rows.push({
      rowKey: 'order',
      itemLabel: '전체 품목',
      fieldLabel: '품목 순서',
      changeType: 'ORDER',
      previousValue: itemOrderText(previousSnapshot),
      nextValue: itemOrderText(nextSnapshot)
    });
  }
  for (let index = 0; index < maxLength; index += 1) {
    const previousItem = previousSnapshot[index];
    const nextItem = nextSnapshot[index];
    const label = itemSnapshotLabel(nextItem ?? previousItem, index);
    if (!previousItem && nextItem) {
      rows.push({
        rowKey: `add-${index}`,
        itemLabel: label,
        fieldLabel: '품목',
        changeType: 'ADDED',
        previousValue: '-',
        nextValue: itemSnapshotSummary(nextItem)
      });
      continue;
    }
    if (previousItem && !nextItem) {
      rows.push({
        rowKey: `delete-${index}`,
        itemLabel: label,
        fieldLabel: '품목',
        changeType: 'REMOVED',
        previousValue: itemSnapshotSummary(previousItem),
        nextValue: '-'
      });
      continue;
    }
    if (!previousItem || !nextItem) {
      continue;
    }
    appendItemDiffRow(rows, index, label, '품목명', previousItem.itemName, nextItem.itemName);
    appendItemDiffRow(rows, index, label, '규격', previousItem.itemSpec, nextItem.itemSpec);
    appendItemDiffRow(
      rows,
      index,
      label,
      '수량',
      [previousItem.itemQuantity, previousItem.itemUnit].filter(Boolean).join(' '),
      [nextItem.itemQuantity, nextItem.itemUnit].filter(Boolean).join(' ')
    );
    appendItemDiffRow(rows, index, label, '단가', previousItem.matchedUnitPrice, nextItem.matchedUnitPrice);
  }
  return rows;
}

function appendItemDiffRow(rows: ItemSnapshotDiffRow[], index: number, itemLabel: string, fieldLabel: string, previousValue: string, nextValue: string) {
  if (previousValue === nextValue) {
    return;
  }
  rows.push({
    rowKey: `${index}-${fieldLabel}`,
    itemLabel,
    fieldLabel,
    changeType: 'CHANGED',
    previousValue: previousValue || '-',
    nextValue: nextValue || '-'
  });
}

function filteredItemDiffRows(rows: ItemSnapshotDiffRow[], fieldFilter: string) {
  return fieldFilter === 'ALL' ? rows : rows.filter((row) => row.fieldLabel === fieldFilter);
}

function diffChangeTypeText(changeType: ItemSnapshotDiffRow['changeType']) {
  const labels: Record<ItemSnapshotDiffRow['changeType'], string> = {
    INITIAL: '최초',
    ADDED: '추가',
    REMOVED: '삭제',
    CHANGED: '수정',
    ORDER: '순서'
  };
  return labels[changeType];
}

function diffChangeTypeColor(changeType: ItemSnapshotDiffRow['changeType']) {
  const colors: Record<ItemSnapshotDiffRow['changeType'], string> = {
    INITIAL: 'blue',
    ADDED: 'green',
    REMOVED: 'red',
    CHANGED: 'gold',
    ORDER: 'purple'
  };
  return colors[changeType];
}

function itemDiffRowText(row: ItemSnapshotDiffRow) {
  return `${row.itemLabel}: ${row.fieldLabel} ${row.previousValue} -> ${row.nextValue}`;
}

function itemSnapshotSummary(item: AuditItemCandidateSnapshot) {
  return [
    item.itemName,
    item.itemSpec,
    [item.itemQuantity, item.itemUnit].filter(Boolean).join(' '),
    item.matchedUnitPrice ? `단가 ${item.matchedUnitPrice}` : ''
  ].filter(Boolean).join(' / ') || '-';
}

function sameItemSet(previousSnapshot: AuditItemCandidateSnapshot[], nextSnapshot: AuditItemCandidateSnapshot[]) {
  if (previousSnapshot.length !== nextSnapshot.length) {
    return false;
  }
  return sortedItemKeys(previousSnapshot).join('|') === sortedItemKeys(nextSnapshot).join('|');
}

function sortedItemKeys(snapshot: AuditItemCandidateSnapshot[]) {
  return snapshot.map(itemKey).sort();
}

function itemOrderSignature(snapshot: AuditItemCandidateSnapshot[]) {
  return snapshot.map(itemKey).join('|');
}

function itemOrderText(snapshot: AuditItemCandidateSnapshot[]) {
  return snapshot.map((item, index) => itemSnapshotLabel(item, index)).join(' > ');
}

function itemKey(item: AuditItemCandidateSnapshot) {
  return [
    item.itemName,
    item.itemSpec,
    item.itemQuantity,
    item.itemUnit,
    item.matchedUnitPrice
  ].join('\u001f');
}

function itemSnapshotLabel(item: AuditItemCandidateSnapshot | undefined, index: number) {
  return item?.itemName || `품목 ${index + 1}`;
}

function auditMetadataText(value: string | null | undefined) {
  if (!value) {
    return '-';
  }
  try {
    const parsed = JSON.parse(value) as Record<string, unknown>;
    return Object.entries(parsed)
      .map(([key, item]) => `${key}: ${String(item)}`)
      .join(' / ');
  } catch {
    return value;
  }
}

function orderDraftIdFromValidationError(validationError: string) {
  const separatorIndex = validationError.indexOf(':');
  if (separatorIndex < 1) {
    return null;
  }
  const candidate = validationError.slice(0, separatorIndex).trim();
  return candidate.startsWith('odft_') ? candidate : null;
}

function formatDate(value: string | null) {
  if (!value) return '-';
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value));
}

function formatCurrency(value?: number | null) {
  if (value == null) {
    return '-';
  }
  return new Intl.NumberFormat('ko-KR').format(value);
}

function formatFileSize(value: number) {
  if (value < 1024) {
    return `${value} B`;
  }
  if (value < 1024 * 1024) {
    return `${(value / 1024).toFixed(1)} KB`;
  }
  return `${(value / 1024 / 1024).toFixed(1)} MB`;
}

function printDocumentHtml(title: string, shippingOrder: OrderDraft) {
  const rows = shippingOrder.itemCandidates.map((itemCandidate, index) => `
    <tr>
      <td>${index + 1}</td>
      <td>${escapeHtml(itemCandidate.itemName || '')}</td>
      <td>${escapeHtml(itemCandidate.itemSpec || '')}</td>
      <td>${escapeHtml([itemCandidate.itemQuantity, itemCandidate.itemUnit].filter(Boolean).join(''))}</td>
      <td>${escapeHtml(formatCurrency(itemCandidate.matchedUnitPrice))}</td>
      <td>${escapeHtml(itemCandidate.lineMemo || '')}</td>
    </tr>
  `).join('');

  return `<!doctype html>
<html lang="ko">
<head>
  <meta charset="utf-8">
  <title>${escapeHtml(title)}</title>
  <style>
    body { font-family: Arial, sans-serif; color: #111827; margin: 32px; }
    h1 { margin: 0 0 20px; font-size: 26px; }
    .meta { display: grid; grid-template-columns: 140px 1fr; gap: 8px 12px; margin-bottom: 20px; }
    .label { color: #4b5563; font-weight: 700; }
    table { width: 100%; border-collapse: collapse; margin: 16px 0; }
    th, td { border: 1px solid #d1d5db; padding: 8px; text-align: left; font-size: 13px; }
    th { background: #f3f4f6; }
    .memo { white-space: pre-wrap; border: 1px solid #d1d5db; padding: 10px; min-height: 48px; }
  </style>
</head>
<body>
  <h1>${escapeHtml(title)}</h1>
  <div class="meta">
    <div class="label">출고표 ID</div><div>${escapeHtml(shippingOrder.orderDraftId)}</div>
    <div class="label">거래처</div><div>${escapeHtml(shippingOrder.customerId || '')}</div>
    <div class="label">수취인</div><div>${escapeHtml(shippingOrder.deliveryRecipientName || '')}</div>
    <div class="label">연락처</div><div>${escapeHtml(shippingOrder.deliveryRecipientPhone || '')}</div>
    <div class="label">주소</div><div>${escapeHtml([shippingOrder.deliveryAddress, shippingOrder.deliveryAddressDetail].filter(Boolean).join(' '))}</div>
  </div>
  <table>
    <thead><tr><th>No</th><th>품목</th><th>규격</th><th>수량</th><th>단가</th><th>메모</th></tr></thead>
    <tbody>${rows}</tbody>
  </table>
  <h2>메모</h2>
  <div class="memo">${escapeHtml([shippingOrder.deliveryMemo, shippingOrder.courierMemo].filter(Boolean).join('\\n'))}</div>
</body>
</html>`;
}

function escapeHtml(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

export default App;

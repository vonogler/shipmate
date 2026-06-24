import { lazy, Suspense, useCallback, useEffect, useMemo, useState } from 'react';
import type { Key, ReactNode } from 'react';
import {
  Alert,
  Button,
  Col,
  Collapse,
  Form,
  Input,
  Layout,
  Menu,
  message,
  Modal,
  Popconfirm,
  Row,
  Select,
  Space,
  Table,
  Tag,
  Typography
} from 'antd';
import type { TableProps } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  AppstoreOutlined,
  AuditOutlined,
  CheckCircleOutlined,
  CopyOutlined,
  DatabaseOutlined,
  DeleteOutlined,
  FileTextOutlined,
  InboxOutlined,
  LoginOutlined,
  PhoneOutlined,
  ReloadOutlined,
  SettingOutlined,
  TeamOutlined,
  UserOutlined,
  WarningOutlined
} from '@ant-design/icons';
import {
  createAuditColumns,
  createGmailPollingRunColumns,
  createInboundColumns,
  createOrderDraftColumns,
  createSettingColumns,
  createUserColumns
} from './adminTableColumns';
import BulkErrorHandlingModal from './screens/BulkErrorHandlingModal';
import CourierCsvRuleAuditDrawer from './screens/CourierCsvRuleAuditDrawer';
import {
  CourierCsvActiveField,
  CourierCsvColumnNameField,
  CourierCsvErrorMessageField,
  CourierCsvRequiredField,
  CourierCsvSortOrderField
} from './screens/CourierCsvRuleInlineFields';
import {
  ErrorLogAssignedActorField,
  ErrorLogHandlingMemoField,
  ErrorLogHandlingStatusField
} from './screens/ErrorLogInlineFields';
import GmailRunFailureDrawer from './screens/GmailRunFailureDrawer';
import type {
  AdminMerchant,
  AdminOverview,
  AdminUser,
  AuditLog,
  AuditLogFilterOptionsResponse,
  AuditLogPageResponse,
  BackupResponse,
  BulkErrorHandlingFormValues,
  CallRecording,
  CourierCsvValidationRule,
  CourierCsvValidationRuleFormValues,
  CsvRuleAuditDiffRow,
  ErrorLog,
  ErrorLogPageResponse,
  GoogleGmailPollingRun,
  InboundMessage,
  LoginFormValues,
  LoginResponse,
  MerchantSetting,
  OrderDraft,
  ScreenKey
} from './types';

const apiBaseUrl = import.meta.env.VITE_SHIPMATE_API_BASE_URL ?? 'http://localhost:8080';
const gmailChecklistUrl = '../shipmate-doc/shipmate-common/html/email-order-ingestion.html';

const screenTitles: Record<ScreenKey, string> = {
  dashboard: '운영 대시보드',
  merchants: '이용자/가맹점 관리',
  users: '내부 직원 계정',
  inbound: '주문 원문 관리',
  drafts: '출고표 초안 관리',
  shipping: '제출 출고표 관리',
  recordings: '통화 녹음 관리',
  errors: '오류 로그',
  gmailRuns: 'Gmail 수집 이력',
  audits: '변경 이력',
  backup: '백업/복구',
  settings: '시스템 설정'
};

const Dashboard = lazy(() => import('./screens/DashboardScreen'));
const BackupScreen = lazy(() => import('./screens/BackupScreen'));
const SettingsScreen = lazy(() => import('./screens/SettingsScreen'));
const TableScreen = lazy(() => import('./screens/TableScreen'));
const UsersScreen = lazy(() => import('./screens/UsersScreen'));
const MerchantsScreen = lazy(() => import('./screens/AdminDataScreens').then((module) => ({ default: module.MerchantsScreen })));
const InboundMessagesScreen = lazy(() => import('./screens/AdminDataScreens').then((module) => ({ default: module.InboundMessagesScreen })));
const OrderDraftsScreen = lazy(() => import('./screens/AdminDataScreens').then((module) => ({ default: module.OrderDraftsScreen })));
const ShippingOrdersScreen = lazy(() => import('./screens/AdminDataScreens').then((module) => ({ default: module.ShippingOrdersScreen })));

function App() {
  const [loginForm] = Form.useForm<LoginFormValues>();
  const [userForm] = Form.useForm<LoginFormValues>();
  const [bulkErrorForm] = Form.useForm<BulkErrorHandlingFormValues>();
  const [csvRuleForm] = Form.useForm<CourierCsvValidationRuleFormValues>();
  const [activeScreen, setActiveScreen] = useState<ScreenKey>('dashboard');
  const [loginUser, setLoginUser] = useState<LoginResponse | null>(() => {
    const saved = localStorage.getItem('shipmate_admin_user');
    return saved ? JSON.parse(saved) as LoginResponse : null;
  });
  const [authToken, setAuthToken] = useState(() => localStorage.getItem('shipmate_admin_token') ?? '');
  const [authLoading, setAuthLoading] = useState(false);
  const [loading, setLoading] = useState(false);
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [merchantActionId, setMerchantActionId] = useState<string | null>(null);
  const [errorActionId, setErrorActionId] = useState<string | null>(null);
  const [bulkErrorActionLoading, setBulkErrorActionLoading] = useState(false);
  const [bulkErrorHandlingStatus, setBulkErrorHandlingStatus] = useState<string | null>(null);
  const [selectedErrorLogIds, setSelectedErrorLogIds] = useState<Key[]>([]);
  const [csvRuleActionId, setCsvRuleActionId] = useState<string | null>(null);
  const [csvRuleModalOpen, setCsvRuleModalOpen] = useState(false);
  const [selectedCsvRule, setSelectedCsvRule] = useState<CourierCsvValidationRule | null>(null);
  const [csvRuleAuditLogs, setCsvRuleAuditLogs] = useState<AuditLog[]>([]);
  const [csvRuleAuditLoading, setCsvRuleAuditLoading] = useState(false);
  const [csvRuleAuditChangedOnly, setCsvRuleAuditChangedOnly] = useState(true);
  const [creatingUser, setCreatingUser] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [dashboardDays, setDashboardDays] = useState(7);
  const [overview, setOverview] = useState<AdminOverview | null>(null);
  const [merchants, setMerchants] = useState<AdminMerchant[]>([]);
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [inboundMessages, setInboundMessages] = useState<InboundMessage[]>([]);
  const [adminInboundSearchText, setAdminInboundSearchText] = useState('');
  const [orderDrafts, setOrderDrafts] = useState<OrderDraft[]>([]);
  const [shippingOrders, setShippingOrders] = useState<OrderDraft[]>([]);
  const [adminDraftStatusFilter, setAdminDraftStatusFilter] = useState('ALL');
  const [adminDraftSearchText, setAdminDraftSearchText] = useState('');
  const [adminDraftDaysFilter, setAdminDraftDaysFilter] = useState<number | 'ALL'>('ALL');
  const [adminShippingSearchText, setAdminShippingSearchText] = useState('');
  const [adminShippingDaysFilter, setAdminShippingDaysFilter] = useState<number | 'ALL'>('ALL');
  const [adminShippingCsvFilter, setAdminShippingCsvFilter] = useState<'ALL' | 'VALID' | 'MISSING'>('ALL');
  const [adminAuditActionFilter, setAdminAuditActionFilter] = useState('ALL');
  const [adminAuditTargetFilter, setAdminAuditTargetFilter] = useState('ALL');
  const [adminAuditMerchantFilter, setAdminAuditMerchantFilter] = useState('ALL');
  const [adminAuditMetadataKeyFilter, setAdminAuditMetadataKeyFilter] = useState('ALL');
  const [adminAuditDaysFilter, setAdminAuditDaysFilter] = useState<number | 'ALL'>('ALL');
  const [adminAuditSearchText, setAdminAuditSearchText] = useState('');
  const [adminErrorMetadataKeyFilter, setAdminErrorMetadataKeyFilter] = useState('ALL');
  const [adminErrorSearchText, setAdminErrorSearchText] = useState('');
  const [adminErrorHandlingStatusFilter, setAdminErrorHandlingStatusFilter] = useState('ALL');
  const [adminErrorDaysFilter, setAdminErrorDaysFilter] = useState<number | 'ALL'>('ALL');
  const [adminErrorPage, setAdminErrorPage] = useState(1);
  const [adminErrorTotal, setAdminErrorTotal] = useState(0);
  const [gmailRunMerchantFilter, setGmailRunMerchantFilter] = useState('ALL');
  const [gmailRunSuccessFilter, setGmailRunSuccessFilter] = useState<'ALL' | 'SUCCESS' | 'FAILURE'>('ALL');
  const [gmailRunDaysFilter, setGmailRunDaysFilter] = useState<number | 'ALL'>('ALL');
  const [selectedGmailRun, setSelectedGmailRun] = useState<GoogleGmailPollingRun | null>(null);
  const [adminAuditPage, setAdminAuditPage] = useState(1);
  const [adminAuditTotal, setAdminAuditTotal] = useState(0);
  const [adminAuditFilterOptions, setAdminAuditFilterOptions] = useState<AuditLogFilterOptionsResponse>({
    actionTypes: [],
    targetTypes: [],
    merchantIds: [],
    metadataKeys: []
  });
  const [recordings, setRecordings] = useState<CallRecording[]>([]);
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
  const [errorLogs, setErrorLogs] = useState<ErrorLog[]>([]);
  const [gmailPollingRuns, setGmailPollingRuns] = useState<GoogleGmailPollingRun[]>([]);
  const [settings, setSettings] = useState<MerchantSetting[]>([]);
  const [courierCsvValidationRules, setCourierCsvValidationRules] = useState<CourierCsvValidationRule[]>([]);
  const [backup, setBackup] = useState<BackupResponse | null>(null);

  const isAdmin = loginUser?.role === 'ADMIN';
  const canSeeOperationSettings = loginUser?.role === 'ADMIN' || loginUser?.role === 'OPERATOR';

  const filteredAdminOrderDrafts = useMemo(
    () => orderDrafts.filter((row) =>
      (adminDraftStatusFilter === 'ALL' || row.orderDraftStatus === adminDraftStatusFilter)
      && adminOrderDraftMatchesSearch(row, adminDraftSearchText)
      && adminOrderDraftWithinDays(row, adminDraftDaysFilter)
    ),
    [adminDraftDaysFilter, adminDraftSearchText, adminDraftStatusFilter, orderDrafts]
  );

  const filteredAdminInboundMessages = useMemo(
    () => inboundMessages.filter((row) => adminInboundMessageMatchesSearch(row, adminInboundSearchText)),
    [adminInboundSearchText, inboundMessages]
  );

  const adminErrorMetadataKeys = useMemo(
    () => uniqueValues(errorLogs.flatMap((row) => metadataKeys(row.errorMetadataJson))),
    [errorLogs]
  );

  const filteredAdminShippingOrders = useMemo(
    () => shippingOrders.filter((row) => {
      const csvMissing = adminOrderDraftMissingFields(row).length > 0;
      return adminOrderDraftMatchesSearch(row, adminShippingSearchText)
        && adminOrderDraftWithinDays(row, adminShippingDaysFilter)
        && (adminShippingCsvFilter === 'ALL'
          || (adminShippingCsvFilter === 'MISSING' && csvMissing)
          || (adminShippingCsvFilter === 'VALID' && !csvMissing));
    }),
    [adminShippingCsvFilter, adminShippingDaysFilter, adminShippingSearchText, shippingOrders]
  );

  const adminAuditLimit = 50;
  const adminErrorLimit = 50;
  const bulkErrorPreview = selectedErrorHandlingPreview(errorLogs, selectedErrorLogIds);

  const menuItems = useMemo(() => [
    { key: 'dashboard', icon: <AppstoreOutlined />, label: '대시보드' },
    { key: 'merchants', icon: <TeamOutlined />, label: '가맹점 관리' },
    isAdmin ? { key: 'users', icon: <UserOutlined />, label: '직원 계정' } : null,
    { key: 'inbound', icon: <InboxOutlined />, label: '주문 원문' },
    { key: 'drafts', icon: <FileTextOutlined />, label: '출고표 초안' },
    { key: 'shipping', icon: <CheckCircleOutlined />, label: '제출 출고표' },
    { key: 'recordings', icon: <PhoneOutlined />, label: '통화 녹음' },
    { key: 'errors', icon: <WarningOutlined />, label: '오류 로그' },
    { key: 'gmailRuns', icon: <ReloadOutlined />, label: 'Gmail 수집 이력' },
    { key: 'audits', icon: <AuditOutlined />, label: '변경 이력' },
    isAdmin ? { key: 'backup', icon: <DatabaseOutlined />, label: '백업/복구' } : null,
    canSeeOperationSettings ? { key: 'settings', icon: <SettingOutlined />, label: '시스템 설정' } : null
  ].filter((item): item is Exclude<typeof item, null> => item !== null), [canSeeOperationSettings, isAdmin]);

  const submitAuth = useCallback(async (values: LoginFormValues, mode: 'login' | 'register') => {
    setAuthLoading(true);
    setError(null);
    try {
      const response = await requestJson<LoginResponse>(mode === 'login' ? '/api/admin/auth/login' : '/api/admin/auth/register', {
        method: 'POST',
        body: JSON.stringify(mode === 'login'
          ? {
              loginId: values.loginId.trim(),
              password: values.password
            }
          : {
              loginId: values.loginId.trim(),
              password: values.password,
              displayName: values.displayName?.trim() || values.loginId.trim(),
              role: values.role || 'ADMIN'
            })
      });
      localStorage.setItem('shipmate_admin_token', response.accessToken);
      localStorage.setItem('shipmate_admin_user', JSON.stringify(response));
      setAuthToken(response.accessToken);
      setLoginUser(response);
      message.success(mode === 'login' ? '로그인되었습니다.' : '내부관리자 계정이 생성되었습니다.');
    } catch (cause) {
      const description = errorMessage(cause, mode === 'login' ? '로그인하지 못했습니다.' : '계정을 생성하지 못했습니다.');
      setError(description);
      message.error(description);
    } finally {
      setAuthLoading(false);
    }
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('shipmate_admin_token');
    localStorage.removeItem('shipmate_admin_user');
    setAuthToken('');
    setLoginUser(null);
  }, []);

  const loadAll = useCallback(async () => {
    if (!localStorage.getItem('shipmate_admin_token')) {
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const errorLogParams = new URLSearchParams({
        limit: String(adminErrorLimit),
        offset: String((adminErrorPage - 1) * adminErrorLimit)
      });
      if (adminErrorMetadataKeyFilter !== 'ALL') errorLogParams.set('metadataKey', adminErrorMetadataKeyFilter);
      if (adminErrorSearchText.trim()) errorLogParams.set('searchText', adminErrorSearchText.trim());
      if (adminErrorHandlingStatusFilter !== 'ALL') errorLogParams.set('handlingStatus', adminErrorHandlingStatusFilter);
      if (adminErrorDaysFilter !== 'ALL') errorLogParams.set('days', String(adminErrorDaysFilter));
      const gmailRunParams = new URLSearchParams({ limit: '100' });
      if (gmailRunMerchantFilter !== 'ALL') gmailRunParams.set('merchantId', gmailRunMerchantFilter);
      if (gmailRunSuccessFilter !== 'ALL') gmailRunParams.set('success', gmailRunSuccessFilter === 'SUCCESS' ? 'true' : 'false');
      if (gmailRunDaysFilter !== 'ALL') gmailRunParams.set('days', String(gmailRunDaysFilter));

      const [
        overviewRows,
        merchantRows,
        inboundRows,
        draftRows,
        shippingRows,
        recordingRows,
        errorRows,
        gmailRunRows
      ] = await Promise.all([
        requestJson<AdminOverview>(`/api/admin/overview?days=${dashboardDays}`),
        requestJson<AdminMerchant[]>('/api/admin/merchants'),
        requestJson<InboundMessage[]>('/api/admin/inbound-messages?limit=500'),
        requestJson<OrderDraft[]>('/api/admin/order-drafts?limit=500'),
        requestJson<OrderDraft[]>('/api/admin/shipping-orders?limit=500'),
        requestJson<CallRecording[]>('/api/admin/call-recordings?limit=500'),
        requestJson<ErrorLogPageResponse>(`/api/admin/error-logs?${errorLogParams.toString()}`),
        requestJson<GoogleGmailPollingRun[]>(`/api/admin/gmail-polling-runs?${gmailRunParams.toString()}`).catch(() => [])
      ]);
      setOverview(overviewRows);
      setMerchants(merchantRows);
      setUsers(isAdmin ? await requestJson<AdminUser[]>('/api/admin/users?limit=500') : []);
      setInboundMessages(inboundRows);
      setOrderDrafts(draftRows);
      setShippingOrders(shippingRows);
      setRecordings(recordingRows);
      setErrorLogs(errorRows.rows);
      setAdminErrorTotal(errorRows.total);
      setGmailPollingRuns(gmailRunRows);
      setSettings(canSeeOperationSettings ? await requestJson<MerchantSetting[]>('/api/admin/settings') : []);
      setCourierCsvValidationRules(canSeeOperationSettings
        ? await requestJson<CourierCsvValidationRule[]>('/api/admin/settings/courier-csv-validation-rules')
        : []);
    } catch (cause) {
      setError(errorMessage(cause, '내부관리자 데이터를 불러오지 못했습니다.'));
    } finally {
      setLoading(false);
    }
  }, [
    adminErrorDaysFilter,
    adminErrorHandlingStatusFilter,
    adminErrorMetadataKeyFilter,
    adminErrorPage,
    adminErrorSearchText,
    canSeeOperationSettings,
    dashboardDays,
    gmailRunDaysFilter,
    gmailRunMerchantFilter,
    gmailRunSuccessFilter,
    isAdmin
  ]);

  const loadGmailPollingRuns = useCallback(async () => {
    const gmailRunParams = new URLSearchParams({ limit: '100' });
    if (gmailRunMerchantFilter !== 'ALL') gmailRunParams.set('merchantId', gmailRunMerchantFilter);
    if (gmailRunSuccessFilter !== 'ALL') gmailRunParams.set('success', gmailRunSuccessFilter === 'SUCCESS' ? 'true' : 'false');
    if (gmailRunDaysFilter !== 'ALL') gmailRunParams.set('days', String(gmailRunDaysFilter));

    const rows = await requestJson<GoogleGmailPollingRun[]>(`/api/admin/gmail-polling-runs?${gmailRunParams.toString()}`);
    setGmailPollingRuns(rows);
    setSelectedGmailRun((current) => {
      if (!current) {
        return current;
      }
      return rows.find((row) => row.gmailPollingRunId === current.gmailPollingRunId) ?? current;
    });
    return rows;
  }, [gmailRunDaysFilter, gmailRunMerchantFilter, gmailRunSuccessFilter]);

  const loadAuditLogs = useCallback(async () => {
    if (!localStorage.getItem('shipmate_admin_token')) {
      return;
    }
    const params = new URLSearchParams({
      limit: String(adminAuditLimit),
      offset: String((adminAuditPage - 1) * adminAuditLimit)
    });
    if (adminAuditActionFilter !== 'ALL') params.set('actionType', adminAuditActionFilter);
    if (adminAuditTargetFilter !== 'ALL') params.set('targetType', adminAuditTargetFilter);
    if (adminAuditMerchantFilter !== 'ALL') params.set('merchantId', adminAuditMerchantFilter);
    if (adminAuditMetadataKeyFilter !== 'ALL') params.set('metadataKey', adminAuditMetadataKeyFilter);
    if (adminAuditDaysFilter !== 'ALL') params.set('days', String(adminAuditDaysFilter));
    if (adminAuditSearchText.trim()) params.set('searchText', adminAuditSearchText.trim());

    try {
      const response = await requestJson<AuditLogPageResponse>(`/api/admin/audit-logs?${params.toString()}`);
      setAuditLogs(response.rows);
      setAdminAuditTotal(response.total);
    } catch (cause) {
      setError(errorMessage(cause, '변경 이력을 불러오지 못했습니다.'));
    }
  }, [
    adminAuditActionFilter,
    adminAuditDaysFilter,
    adminAuditMetadataKeyFilter,
    adminAuditMerchantFilter,
    adminAuditPage,
    adminAuditSearchText,
    adminAuditTargetFilter
  ]);

  const loadAuditFilterOptions = useCallback(async () => {
    if (!localStorage.getItem('shipmate_admin_token')) {
      return;
    }
    try {
      setAdminAuditFilterOptions(await requestJson<AuditLogFilterOptionsResponse>('/api/admin/audit-logs/filter-options'));
    } catch (cause) {
      setError(errorMessage(cause, '변경 이력 필터 옵션을 불러오지 못했습니다.'));
    }
  }, []);

  useEffect(() => {
    loadAll();
  }, [loadAll, authToken]);

  useEffect(() => {
    loadAuditLogs();
  }, [loadAuditLogs, authToken]);

  useEffect(() => {
    loadAuditFilterOptions();
  }, [loadAuditFilterOptions, authToken]);

  useEffect(() => {
    setAdminAuditPage(1);
  }, [
    adminAuditActionFilter,
    adminAuditDaysFilter,
    adminAuditMetadataKeyFilter,
    adminAuditMerchantFilter,
    adminAuditSearchText,
    adminAuditTargetFilter
  ]);

  useEffect(() => {
    setAdminErrorPage(1);
  }, [
    adminErrorDaysFilter,
    adminErrorHandlingStatusFilter,
    adminErrorMetadataKeyFilter,
    adminErrorSearchText
  ]);

  useEffect(() => {
    if (!isAdmin && (activeScreen === 'users' || activeScreen === 'backup')) {
      setActiveScreen('dashboard');
      return;
    }
    if (!canSeeOperationSettings && activeScreen === 'settings') {
      setActiveScreen('dashboard');
    }
  }, [activeScreen, canSeeOperationSettings, isAdmin]);

  const createUser = useCallback(async (values: LoginFormValues) => {
    setCreatingUser(true);
    setError(null);
    try {
      await requestJson<LoginResponse>('/api/admin/users', {
        method: 'POST',
        body: JSON.stringify({
          loginId: values.loginId.trim(),
          password: values.password,
          displayName: values.displayName?.trim() || values.loginId.trim(),
          role: values.role || 'OPERATOR'
        })
      });
      userForm.resetFields();
      await loadAll();
      message.success('직원 계정을 생성했습니다.');
    } catch (cause) {
      const description = errorMessage(cause, '직원 계정을 생성하지 못했습니다.');
      setError(description);
      message.error(description);
    } finally {
      setCreatingUser(false);
    }
  }, [loadAll, userForm]);

  const deleteRecording = useCallback(async (recordingId: string) => {
    setDeletingId(recordingId);
    setError(null);
    try {
      await requestJson(`/api/call-recordings/${recordingId}`, { method: 'DELETE' });
      setRecordings((currentRows) => currentRows.filter((row) => row.recordingId !== recordingId));
      message.success('통화 녹음을 삭제했습니다.');
    } catch (cause) {
      const description = errorMessage(cause, '통화 녹음을 삭제하지 못했습니다.');
      setError(description);
      message.error(description);
    } finally {
      setDeletingId(null);
    }
  }, []);

  const loadBackup = useCallback(async () => {
    try {
      setBackup(await requestJson<BackupResponse>('/api/admin/backup'));
      message.success('백업 미리보기를 불러왔습니다.');
    } catch (cause) {
      const description = errorMessage(cause, '백업 미리보기를 불러오지 못했습니다.');
      setError(description);
      message.error(description);
    }
  }, []);

  const openStagingSummary = useCallback(async () => {
    try {
      const html = await requestText('/api/admin/staging-summary.html');
      const url = URL.createObjectURL(new Blob([html], { type: 'text/html' }));
      window.open(url, '_blank', 'noopener,noreferrer');
      window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
    } catch (cause) {
      const description = errorMessage(cause, '스테이징 점검 요약을 열지 못했습니다.');
      setError(description);
      message.error(description);
    }
  }, []);

  const updateMerchant = useCallback(async (merchantId: string, status: string, operationMemo: string) => {
    setMerchantActionId(merchantId);
    setError(null);
    try {
      await requestJson(`/api/admin/merchants/${encodeURIComponent(merchantId)}`, {
        method: 'PUT',
        body: JSON.stringify({ status, operationMemo })
      });
      setMerchants((currentRows) => currentRows.map((row) => row.merchantId === merchantId
        ? { ...row, status, operationMemo }
        : row));
      message.success('가맹점 운영 정보를 저장했습니다.');
    } catch (cause) {
      const description = errorMessage(cause, '가맹점 운영 정보를 저장하지 못했습니다.');
      setError(description);
      message.error(description);
    } finally {
      setMerchantActionId(null);
    }
  }, []);

  const updateMerchantLock = useCallback(async (merchantId: string, locked: boolean) => {
    setMerchantActionId(merchantId);
    setError(null);
    try {
      await requestJson(`/api/admin/merchants/${encodeURIComponent(merchantId)}/${locked ? 'lock' : 'unlock'}`, {
        method: 'POST'
      });
      setMerchants((currentRows) => currentRows.map((row) => row.merchantId === merchantId
        ? { ...row, accountLocked: locked }
        : row));
      message.success(locked ? '가맹점 계정을 잠금 처리했습니다.' : '가맹점 계정 잠금을 해제했습니다.');
    } catch (cause) {
      const description = errorMessage(cause, '가맹점 계정 상태를 변경하지 못했습니다.');
      setError(description);
      message.error(description);
    } finally {
      setMerchantActionId(null);
    }
  }, []);

  const resetMerchantPassword = useCallback(async (merchantId: string) => {
    setMerchantActionId(merchantId);
    setError(null);
    try {
      const response = await requestJson<{ temporaryPassword: string }>(`/api/admin/merchants/${encodeURIComponent(merchantId)}/reset-password`, {
        method: 'POST'
      });
      setMerchants((currentRows) => currentRows.map((row) => row.merchantId === merchantId
        ? { ...row, accountLocked: false }
        : row));
      message.success(`임시 비밀번호: ${response.temporaryPassword}`);
    } catch (cause) {
      const description = errorMessage(cause, '비밀번호를 초기화하지 못했습니다.');
      setError(description);
      message.error(description);
    } finally {
      setMerchantActionId(null);
    }
  }, []);

  const updateErrorHandling = useCallback(async (
    row: ErrorLog,
    values: Partial<Pick<ErrorLog, 'handlingStatus' | 'assignedActorId' | 'handlingMemo'>>
  ) => {
    setErrorActionId(row.errorLogId);
    setError(null);
    const next = {
      handlingStatus: values.handlingStatus ?? row.handlingStatus ?? 'OPEN',
      assignedActorId: values.assignedActorId ?? row.assignedActorId ?? '',
      handlingMemo: values.handlingMemo ?? row.handlingMemo ?? ''
    };
    try {
      const updated = await requestJson<ErrorLog>(`/api/admin/error-logs/${encodeURIComponent(row.errorLogId)}/handling`, {
        method: 'PUT',
        body: JSON.stringify(next)
      });
      setErrorLogs((currentRows) => currentRows.map((currentRow) => currentRow.errorLogId === row.errorLogId ? updated : currentRow));
      message.success('오류 처리 정보를 저장했습니다.');
    } catch (cause) {
      const description = errorMessage(cause, '오류 처리 정보를 저장하지 못했습니다.');
      setError(description);
      message.error(description);
    } finally {
      setErrorActionId(null);
    }
  }, []);

  const openBulkErrorHandlingModal = useCallback((handlingStatus: string) => {
    const ids = selectedErrorLogIds.map(String);
    if (!ids.length) {
      message.warning('처리할 오류 로그를 선택하세요.');
      return;
    }
    setBulkErrorHandlingStatus(handlingStatus);
    bulkErrorForm.setFieldsValue({
      assignedActorId: loginUser?.loginId ?? '',
      handlingMemo: handlingStatus === 'RESOLVED' ? '일괄 조치 완료' : '일괄 확인'
    });
  }, [bulkErrorForm, loginUser?.loginId, selectedErrorLogIds]);

  const updateSelectedErrorHandling = useCallback(async (values: BulkErrorHandlingFormValues) => {
    const ids = selectedErrorLogIds.map(String);
    if (!ids.length) {
      message.warning('처리할 오류 로그를 선택하세요.');
      return;
    }
    const handlingStatus = bulkErrorHandlingStatus ?? 'ACKNOWLEDGED';
    setBulkErrorActionLoading(true);
    setError(null);
    try {
      const updatedRows = await requestJson<ErrorLog[]>('/api/admin/error-logs/handling', {
        method: 'PUT',
        body: JSON.stringify({
          errorLogIds: ids,
          handlingStatus,
          assignedActorId: values.assignedActorId ?? '',
          handlingMemo: values.handlingMemo ?? ''
        })
      });
      setErrorLogs((currentRows) => currentRows.map((row) => updatedRows.find((updated) => updated.errorLogId === row.errorLogId) ?? row));
      setSelectedErrorLogIds([]);
      setBulkErrorHandlingStatus(null);
      if (selectedGmailRun) {
        try {
          await loadGmailPollingRuns();
        } catch (cause) {
          setError(errorMessage(cause, 'Gmail 수집 이력을 새로고침하지 못했습니다.'));
        }
      }
      message.success('선택한 오류 로그 처리 상태를 저장했습니다.');
    } catch (cause) {
      const description = errorMessage(cause, '오류 로그를 일괄 처리하지 못했습니다.');
      setError(description);
      message.error(description);
    } finally {
      setBulkErrorActionLoading(false);
    }
  }, [bulkErrorHandlingStatus, loadGmailPollingRuns, selectedErrorLogIds, selectedGmailRun]);

  const openCreateCourierCsvRule = useCallback(() => {
    csvRuleForm.setFieldsValue({
      courierCsvTemplate: 'GENERIC',
      fieldName: '',
      csvColumnName: '',
      required: true,
      errorMessage: '필수값이 없습니다.',
      sortOrder: 100,
      active: true
    });
    setCsvRuleModalOpen(true);
  }, [csvRuleForm]);

  const openCloneCourierCsvRule = useCallback((row: CourierCsvValidationRule) => {
    csvRuleForm.setFieldsValue({
      courierCsvTemplate: row.courierCsvTemplate,
      fieldName: `${row.fieldName}.copy`,
      csvColumnName: row.csvColumnName ?? '',
      required: row.required,
      errorMessage: row.errorMessage,
      sortOrder: row.sortOrder + 1,
      active: row.active
    });
    setCsvRuleModalOpen(true);
  }, [csvRuleForm]);

  const createCourierCsvValidationRule = useCallback(async (values: CourierCsvValidationRuleFormValues) => {
    setCsvRuleActionId('create');
    setError(null);
    try {
      const created = await requestJson<CourierCsvValidationRule>('/api/admin/settings/courier-csv-validation-rules', {
        method: 'POST',
        body: JSON.stringify(values)
      });
      setCourierCsvValidationRules((currentRows) => [...currentRows.filter((row) => row.courierCsvValidationRuleId !== created.courierCsvValidationRuleId), created]
        .sort(compareCourierCsvValidationRule));
      setCsvRuleModalOpen(false);
      message.success('CSV 검증 규칙을 추가했습니다.');
    } catch (cause) {
      const description = errorMessage(cause, 'CSV 검증 규칙을 추가하지 못했습니다.');
      setError(description);
      message.error(description);
    } finally {
      setCsvRuleActionId(null);
    }
  }, []);

  const updateCourierCsvValidationRule = useCallback(async (
    row: CourierCsvValidationRule,
    values: Partial<Pick<CourierCsvValidationRule, 'csvColumnName' | 'required' | 'errorMessage' | 'sortOrder' | 'active'>>
  ) => {
    setCsvRuleActionId(row.courierCsvValidationRuleId);
    setError(null);
    const next = {
      csvColumnName: values.csvColumnName ?? row.csvColumnName ?? '',
      required: values.required ?? row.required,
      errorMessage: values.errorMessage ?? row.errorMessage,
      sortOrder: values.sortOrder ?? row.sortOrder,
      active: values.active ?? row.active
    };
    try {
      const updated = await requestJson<CourierCsvValidationRule>(`/api/admin/settings/courier-csv-validation-rules/${encodeURIComponent(row.courierCsvValidationRuleId)}`, {
        method: 'PUT',
        body: JSON.stringify(next)
      });
      setCourierCsvValidationRules((currentRows) => currentRows.map((currentRow) => currentRow.courierCsvValidationRuleId === updated.courierCsvValidationRuleId ? updated : currentRow));
      message.success('CSV 검증 규칙을 저장했습니다.');
    } catch (cause) {
      const description = errorMessage(cause, 'CSV 검증 규칙을 저장하지 못했습니다.');
      setError(description);
      message.error(description);
    } finally {
      setCsvRuleActionId(null);
    }
  }, []);

  const openCsvRuleAuditDrawer = useCallback(async (row: CourierCsvValidationRule) => {
    setSelectedCsvRule(row);
    setCsvRuleAuditLogs([]);
    setCsvRuleAuditLoading(true);
    try {
      const params = new URLSearchParams({
        targetType: 'COURIER_CSV_VALIDATION_RULE',
        searchText: row.courierCsvValidationRuleId,
        limit: '50',
        offset: '0'
      });
      const response = await requestJson<AuditLogPageResponse>(`/api/admin/audit-logs?${params.toString()}`);
      setCsvRuleAuditLogs(response.rows);
    } catch (cause) {
      const description = errorMessage(cause, '택배 CSV 검증 규칙 변경 이력을 불러오지 못했습니다.');
      setError(description);
      message.error(description);
    } finally {
      setCsvRuleAuditLoading(false);
    }
  }, []);

  const merchantColumns = useMemo<ColumnsType<AdminMerchant>>(() => [
    { title: '이용자 ID', dataIndex: 'merchantId', width: 220 },
    { title: '이용자명', dataIndex: 'merchantName', width: 180 },
    {
      title: '상태',
      dataIndex: 'status',
      width: 150,
      render: (status: string, row) => (
        <Select
          size="small"
          value={status}
          disabled={merchantActionId === row.merchantId}
          onChange={(value) => updateMerchant(row.merchantId, value, row.operationMemo)}
          options={[
            { value: 'ACTIVE', label: '활성' },
            { value: 'PAUSED', label: '중지' },
            { value: 'TERMINATED', label: '해지' },
            { value: 'REVIEWING', label: '검토중' }
          ]}
        />
      )
    },
    { title: '잠금', dataIndex: 'accountLocked', width: 90, render: (locked: boolean) => <Tag color={locked ? 'red' : 'green'}>{locked ? '잠금' : '정상'}</Tag> },
    { title: '직원', dataIndex: 'userCount', width: 90 },
    { title: '고객', dataIndex: 'customerCount', width: 90 },
    { title: '주문 원문', dataIndex: 'inboundMessageCount', width: 110 },
    { title: '초안', dataIndex: 'orderDraftCount', width: 90 },
    { title: '최근 활동', dataIndex: 'lastActivityAt', width: 180, render: formatDate },
    {
      title: '운영 메모',
      dataIndex: 'operationMemo',
      width: 260,
      render: (operationMemo: string, row) => (
        <Input.TextArea
          autoSize={{ minRows: 1, maxRows: 3 }}
          defaultValue={operationMemo}
          onBlur={(event) => {
            const nextMemo = event.currentTarget.value;
            if (nextMemo !== operationMemo) {
              updateMerchant(row.merchantId, row.status, nextMemo);
            }
          }}
        />
      )
    },
    {
      title: '작업',
      width: 220,
      fixed: 'right',
      render: (_, row) => (
        <Space wrap>
          <Button size="small" loading={merchantActionId === row.merchantId} onClick={() => updateMerchantLock(row.merchantId, !row.accountLocked)}>
            {row.accountLocked ? '잠금 해제' : '잠금'}
          </Button>
          <Popconfirm
            title="비밀번호를 초기화할까요?"
            okText="초기화"
            cancelText="취소"
            onConfirm={() => resetMerchantPassword(row.merchantId)}
          >
            <Button size="small" loading={merchantActionId === row.merchantId}>비밀번호 초기화</Button>
          </Popconfirm>
        </Space>
      )
    }
  ], [merchantActionId, resetMerchantPassword, updateMerchant, updateMerchantLock]);

  const userColumns = useMemo(() => createUserColumns(formatDate), []);
  const inboundColumns = useMemo(() => createInboundColumns(statusTag, emptyText, formatDate), []);
  const draftColumns = useMemo(() => createOrderDraftColumns(statusTag, emptyText, formatDate), []);

  const recordingColumns = useMemo<ColumnsType<CallRecording>>(() => [
    { title: '녹음 ID', dataIndex: 'recordingId', width: 230 },
    { title: '파일', dataIndex: 'fileName', width: 220 },
    { title: '상태', dataIndex: 'status', width: 130, render: statusTag },
    { title: '출처', dataIndex: 'source', width: 130, render: emptyText },
    { title: '크기', dataIndex: 'fileSize', width: 110, render: formatBytes },
    { title: '객체 키', dataIndex: 'objectKey', width: 420 },
    { title: '오류', dataIndex: 'errorCode', width: 130, render: emptyText },
    { title: '생성 시각', dataIndex: 'createdAt', width: 180, render: formatDate },
    {
      title: '작업',
      width: 90,
      fixed: 'right',
      render: (_, row) => (
        <Popconfirm
          title="녹음을 삭제할까요?"
          okText="삭제"
          cancelText="취소"
          okButtonProps={{ danger: true }}
          onConfirm={() => deleteRecording(row.recordingId)}
        >
          <Button danger icon={<DeleteOutlined />} loading={deletingId === row.recordingId} aria-label="녹음 삭제" />
        </Popconfirm>
      )
    }
  ], [deleteRecording, deletingId]);

  const showAuditSearchForError = useCallback((errorLog: ErrorLog) => {
    setAdminAuditSearchText(errorLog.errorLogId);
    setAdminAuditActionFilter('ALL');
    setAdminAuditTargetFilter('ALL');
    setAdminAuditMerchantFilter('ALL');
    setAdminAuditMetadataKeyFilter('ALL');
    setAdminAuditDaysFilter('ALL');
    setAdminAuditPage(1);
    setActiveScreen('audits');
  }, []);

  const showInboundMessage = useCallback((inboundMessageId: string | null | undefined) => {
    if (!inboundMessageId) {
      message.warning('이동할 주문 원문 ID가 없습니다.');
      return;
    }
    setAdminInboundSearchText(inboundMessageId);
    setActiveScreen('inbound');
  }, []);

  const showErrorLogsForGmailRun = useCallback((row: GoogleGmailPollingRun) => {
    setAdminErrorMetadataKeyFilter('ALL');
    setAdminErrorHandlingStatusFilter('ALL');
    setAdminErrorDaysFilter('ALL');
    setAdminErrorSearchText(row.failureCode || row.failureMessage || row.gmailPollingRunId);
    setAdminErrorPage(1);
    setActiveScreen('errors');
  }, []);

  const openGmailRunErrorHandlingModal = useCallback(async (row: GoogleGmailPollingRun, handlingStatus: string) => {
    let matchedRows = errorLogs.filter((errorLog) => isGmailRunRelatedErrorLog(errorLog, row));
    if (!matchedRows.length) {
      const searchTexts = gmailRunErrorSearchTexts(row);
      for (const searchText of searchTexts) {
        try {
          const params = new URLSearchParams({
            searchText,
            limit: '200',
            offset: '0'
          });
          const response = await requestJson<ErrorLogPageResponse>(`/api/admin/error-logs?${params.toString()}`);
          matchedRows = response.rows.filter((errorLog) => isGmailRunRelatedErrorLog(errorLog, row));
          if (matchedRows.length) {
            setErrorLogs(response.rows);
            setAdminErrorTotal(response.total);
            break;
          }
        } catch (cause) {
          const description = errorMessage(cause, '관련 오류 로그를 검색하지 못했습니다.');
          setError(description);
          message.error(description);
          break;
        }
      }
    }
    const matchedIds = matchedRows.map((errorLog) => errorLog.errorLogId);
    if (!matchedIds.length) {
      message.warning('현재 오류 로그 목록에서 관련 오류를 찾지 못했습니다. 오류 로그 화면에서 검색 조건을 확인하세요.');
      showErrorLogsForGmailRun(row);
      return;
    }
    setSelectedErrorLogIds(matchedIds);
    setBulkErrorHandlingStatus(handlingStatus);
    bulkErrorForm.setFieldsValue({
      assignedActorId: loginUser?.loginId ?? '',
      handlingMemo: handlingStatus === 'RESOLVED' ? 'Gmail 실패 일괄 조치 완료' : 'Gmail 실패 일괄 확인'
    });
  }, [bulkErrorForm, errorLogs, loginUser?.loginId, showErrorLogsForGmailRun]);

  const errorColumns = useMemo<ColumnsType<ErrorLog>>(() => [
    { title: '오류 ID', dataIndex: 'errorLogId', width: 230 },
    { title: '코드', dataIndex: 'code', width: 130 },
    { title: 'HTTP', dataIndex: 'status', width: 90 },
    { title: '메시지', dataIndex: 'message', ellipsis: true },
    { title: '예외', dataIndex: 'exceptionType', width: 220, render: emptyText },
    { title: '요청 정보', dataIndex: 'errorMetadataJson', width: 300, ellipsis: true, render: auditMetadataText },
    { title: '운영 조치', width: 300, render: (_, row) => operationFailureGuide(row.code, row.message) },
    {
      title: '처리 상태',
      dataIndex: 'handlingStatus',
      width: 150,
      render: (_, row) => (
        <ErrorLogHandlingStatusField
          row={row}
          loading={errorActionId === row.errorLogId}
          onUpdate={updateErrorHandling}
        />
      )
    },
    {
      title: '담당자',
      dataIndex: 'assignedActorId',
      width: 180,
      render: (_, row) => (
        <ErrorLogAssignedActorField
          row={row}
          loading={errorActionId === row.errorLogId}
          onUpdate={updateErrorHandling}
        />
      )
    },
    {
      title: '처리 메모',
      dataIndex: 'handlingMemo',
      width: 260,
      render: (_, row) => (
        <ErrorLogHandlingMemoField
          row={row}
          loading={errorActionId === row.errorLogId}
          onUpdate={updateErrorHandling}
        />
      )
    },
    { title: '처리 시각', dataIndex: 'handledAt', width: 180, render: formatDate },
    { title: '발생 시각', dataIndex: 'createdAt', width: 180, render: formatDate },
    {
      title: '변경 이력',
      width: 110,
      fixed: 'right',
      render: (_, row) => <Button size="small" onClick={() => showAuditSearchForError(row)}>검색</Button>
    }
  ], [errorActionId, showAuditSearchForError, updateErrorHandling]);

  const auditColumns = useMemo(() => createAuditColumns(auditActionLabel, auditTargetLabel, auditMetadataText, emptyText, formatDate), []);
  const gmailPollingRunColumns = useMemo(() => createGmailPollingRunColumns(emptyText, formatDate, setSelectedGmailRun), []);
  const settingColumns = useMemo(() => createSettingColumns(emptyText, formatDate), []);

  const courierCsvValidationRuleColumns = useMemo<ColumnsType<CourierCsvValidationRule>>(() => [
    { title: '규칙 ID', dataIndex: 'courierCsvValidationRuleId', width: 230 },
    { title: '택배 템플릿', dataIndex: 'courierCsvTemplate', width: 150 },
    { title: '필드', dataIndex: 'fieldName', width: 190 },
    {
      title: 'CSV 컬럼명',
      dataIndex: 'csvColumnName',
      width: 180,
      render: (_, row) => (
        <CourierCsvColumnNameField
          row={row}
          loading={csvRuleActionId === row.courierCsvValidationRuleId}
          onUpdate={updateCourierCsvValidationRule}
        />
      )
    },
    {
      title: '필수',
      dataIndex: 'required',
      width: 110,
      render: (_, row) => (
        <CourierCsvRequiredField
          row={row}
          loading={csvRuleActionId === row.courierCsvValidationRuleId}
          onUpdate={updateCourierCsvValidationRule}
        />
      )
    },
    {
      title: '오류 메시지',
      dataIndex: 'errorMessage',
      width: 320,
      render: (_, row) => (
        <CourierCsvErrorMessageField
          row={row}
          loading={csvRuleActionId === row.courierCsvValidationRuleId}
          onUpdate={updateCourierCsvValidationRule}
        />
      )
    },
    {
      title: '순서',
      dataIndex: 'sortOrder',
      width: 100,
      render: (_, row) => (
        <CourierCsvSortOrderField
          row={row}
          loading={csvRuleActionId === row.courierCsvValidationRuleId}
          onUpdate={updateCourierCsvValidationRule}
        />
      )
    },
    {
      title: '사용',
      dataIndex: 'active',
      width: 110,
      render: (_, row) => (
        <CourierCsvActiveField
          row={row}
          loading={csvRuleActionId === row.courierCsvValidationRuleId}
          onUpdate={updateCourierCsvValidationRule}
        />
      )
    },
    {
      title: '작업',
      width: 150,
      fixed: 'right',
      render: (_, row) => (
        <Space>
          <Button size="small" onClick={() => openCloneCourierCsvRule(row)}>
            복제
          </Button>
          <Button size="small" onClick={() => openCsvRuleAuditDrawer(row)}>
            이력
          </Button>
        </Space>
      )
    }
  ], [csvRuleActionId, openCloneCourierCsvRule, openCsvRuleAuditDrawer, updateCourierCsvValidationRule]);

  return (
    <Layout className="app-shell">
      <Layout.Sider className="sidebar" width={260} breakpoint="lg" collapsedWidth={0}>
        <div className="sidebar-brand">
          <Typography.Title level={3} className="brand">출고비서</Typography.Title>
          <Typography.Text className="sidebar-subtitle">내부관리자</Typography.Text>
        </div>
        <Menu
          mode="inline"
          className="sidebar-menu"
          selectedKeys={[activeScreen]}
          items={menuItems}
          onClick={({ key }) => setActiveScreen(key as ScreenKey)}
        />
      </Layout.Sider>
      <Layout className="main-shell">
        <Layout.Header className="topbar">
          <div>
            <Typography.Title level={3} className="brand">{screenTitles[activeScreen]}</Typography.Title>
            <Typography.Text type="secondary">운영자와 사원이 출고비서 전체 상태를 관리합니다.</Typography.Text>
          </div>
          <Space wrap>
            {loginUser ? <Typography.Text>{loginUser.displayName} / {loginUser.role}</Typography.Text> : null}
            <Button icon={<ReloadOutlined />} onClick={loadAll} loading={loading} disabled={!authToken}>새로고침</Button>
            {authToken ? <Button onClick={logout}>로그아웃</Button> : null}
          </Space>
        </Layout.Header>
        <Layout.Content className="content">
          <Space direction="vertical" size={16} className="content-stack">
            {!authToken ? (
              <section className="work-panel">
                <div className="section-heading">
                  <Typography.Title level={2}>내부관리자 로그인</Typography.Title>
                  <Typography.Text type="secondary">처음 실행할 때는 내부 계정을 생성한 뒤 로그인합니다.</Typography.Text>
                </div>
                <Form form={loginForm} layout="vertical" initialValues={{ role: 'ADMIN' }}>
                  <Row gutter={12}>
                    <Col xs={24} md={8}><Form.Item name="loginId" label="로그인 ID" rules={[{ required: true, message: '로그인 ID를 입력하세요.' }]}><Input /></Form.Item></Col>
                    <Col xs={24} md={8}><Form.Item name="password" label="비밀번호" rules={[{ required: true, message: '비밀번호를 입력하세요.' }]}><Input.Password /></Form.Item></Col>
                    <Col xs={24} md={8}><Form.Item name="displayName" label="표시 이름"><Input /></Form.Item></Col>
                    <Col xs={24} md={8}>
                      <Form.Item name="role" label="권한">
                        <Select options={[
                          { value: 'ADMIN', label: '관리자' },
                          { value: 'OPERATOR', label: '운영자' },
                          { value: 'VIEWER', label: '조회자' }
                        ]} />
                      </Form.Item>
                    </Col>
                  </Row>
                  <Space wrap>
                    <Button type="primary" icon={<LoginOutlined />} loading={authLoading} onClick={() => loginForm.validateFields().then((values) => submitAuth(values, 'login'))}>로그인</Button>
                    <Button loading={authLoading} onClick={() => loginForm.validateFields().then((values) => submitAuth(values, 'register'))}>내부 계정 생성</Button>
                  </Space>
                </Form>
              </section>
            ) : null}
            {error ? <Alert type="error" message="요청 실패" description={error} showIcon /> : null}
            {authToken && activeScreen === 'dashboard' ? (
              <Suspense fallback={<div className="empty-panel">화면을 불러오는 중입니다.</div>}>
                <Dashboard
                  overview={overview}
                  days={dashboardDays}
                  onDaysChange={setDashboardDays}
                  onShowRecentInbound={(inboundMessageId) => {
                    setAdminInboundSearchText(inboundMessageId ?? '');
                    setActiveScreen('inbound');
                  }}
                  onOpenStagingSummary={openStagingSummary}
                />
              </Suspense>
            ) : null}
            {authToken && activeScreen === 'merchants' ? (
              <Suspense fallback={<section className="work-panel"><div className="empty-panel">이용자/가맹점 화면을 불러오는 중입니다.</div></section>}>
                <MerchantsScreen rows={merchants} columns={merchantColumns} />
              </Suspense>
            ) : null}
            {authToken && activeScreen === 'users' ? (
              <Suspense fallback={<section className="work-panel"><div className="empty-panel">직원 계정 화면을 불러오는 중입니다.</div></section>}>
                <UsersScreen
                  form={userForm}
                  users={users}
                  columns={userColumns}
                  loading={loading}
                  creatingUser={creatingUser}
                  onCreateUser={createUser}
                />
              </Suspense>
            ) : null}
            {authToken && activeScreen === 'inbound' ? (
              <Suspense fallback={<section className="work-panel"><div className="empty-panel">주문 원문 화면을 불러오는 중입니다.</div></section>}>
                <InboundMessagesScreen
                  rows={filteredAdminInboundMessages}
                  columns={inboundColumns}
                  searchText={adminInboundSearchText}
                  onSearchTextChange={setAdminInboundSearchText}
                />
              </Suspense>
            ) : null}
            {authToken && activeScreen === 'drafts' ? (
              <Suspense fallback={<section className="work-panel"><div className="empty-panel">출고표 초안 화면을 불러오는 중입니다.</div></section>}>
                <OrderDraftsScreen
                  rows={filteredAdminOrderDrafts}
                  allRows={orderDrafts}
                  columns={draftColumns}
                  statusFilter={adminDraftStatusFilter}
                  daysFilter={adminDraftDaysFilter}
                  searchText={adminDraftSearchText}
                  onStatusFilterChange={setAdminDraftStatusFilter}
                  onDaysFilterChange={setAdminDraftDaysFilter}
                  onSearchTextChange={setAdminDraftSearchText}
                />
              </Suspense>
            ) : null}
            {authToken && activeScreen === 'shipping' ? (
              <Suspense fallback={<section className="work-panel"><div className="empty-panel">제출 출고표 화면을 불러오는 중입니다.</div></section>}>
                <ShippingOrdersScreen
                  rows={filteredAdminShippingOrders}
                  columns={draftColumns}
                  daysFilter={adminShippingDaysFilter}
                  csvFilter={adminShippingCsvFilter}
                  searchText={adminShippingSearchText}
                  onDaysFilterChange={setAdminShippingDaysFilter}
                  onCsvFilterChange={setAdminShippingCsvFilter}
                  onSearchTextChange={setAdminShippingSearchText}
                />
              </Suspense>
            ) : null}
            {authToken && activeScreen === 'recordings' ? <DataPanel title="통화 녹음 관리" rows={recordings} columns={recordingColumns} x={1780} /> : null}
            {authToken && activeScreen === 'errors' ? (
              <Suspense fallback={<section className="work-panel"><div className="empty-panel">오류 로그 화면을 불러오는 중입니다.</div></section>}>
                <TableScreen
                  title="오류 로그"
                  rows={errorLogs}
                  columns={errorColumns}
                  x={2280}
                  total={adminErrorTotal}
                  pagination={{
                    current: adminErrorPage,
                    pageSize: adminErrorLimit,
                    total: adminErrorTotal,
                    showSizeChanger: false,
                    onChange: setAdminErrorPage
                  }}
                  toolbar={(
                    <Space wrap>
                      <Button
                        size="small"
                        loading={bulkErrorActionLoading}
                        onClick={() => openBulkErrorHandlingModal('ACKNOWLEDGED')}
                      >
                        선택 확인
                      </Button>
                      <Button
                        size="small"
                        loading={bulkErrorActionLoading}
                        onClick={() => openBulkErrorHandlingModal('RESOLVED')}
                      >
                        선택 조치 완료
                      </Button>
                      <Select
                        value={adminErrorMetadataKeyFilter}
                        onChange={setAdminErrorMetadataKeyFilter}
                        className="filter-select"
                        options={[
                          { value: 'ALL', label: '전체 요청 정보' },
                          ...adminErrorMetadataKeys.map((value) => ({ value, label: value }))
                        ]}
                      />
                      <Input.Search
                        allowClear
                        value={adminErrorSearchText}
                        onChange={(event) => setAdminErrorSearchText(event.target.value)}
                        placeholder="오류 ID, 코드, 요청 경로, 이용자, 작업자 검색"
                      />
                      <Select
                        value={adminErrorHandlingStatusFilter}
                        onChange={setAdminErrorHandlingStatusFilter}
                        className="filter-select"
                        options={[
                          { value: 'ALL', label: '전체 처리 상태' },
                          { value: 'OPEN', label: '미확인' },
                          { value: 'ACKNOWLEDGED', label: '확인' },
                          { value: 'RESOLVED', label: '조치 완료' }
                        ]}
                      />
                      <Select
                        value={adminErrorDaysFilter}
                        onChange={setAdminErrorDaysFilter}
                        className="filter-select"
                        options={[
                          { value: 'ALL', label: '전체 기간' },
                          { value: 1, label: '오늘' },
                          { value: 7, label: '7일' },
                          { value: 30, label: '30일' }
                        ]}
                      />
                    </Space>
                  )}
                  rowSelection={{
                    selectedRowKeys: selectedErrorLogIds,
                    onChange: setSelectedErrorLogIds
                  }}
                />
              </Suspense>
            ) : null}
            {authToken && activeScreen === 'gmailRuns' ? (
              <Suspense fallback={<section className="work-panel"><div className="empty-panel">Gmail 수집 이력 화면을 불러오는 중입니다.</div></section>}>
                <TableScreen
                  title="Gmail 수집 이력"
                  rows={gmailPollingRuns}
                  columns={gmailPollingRunColumns}
                  x={1840}
                  toolbar={(
                  <Space wrap>
                    <Select
                      value={gmailRunMerchantFilter}
                      onChange={setGmailRunMerchantFilter}
                      className="filter-select"
                      options={[
                        { value: 'ALL', label: '전체 이용자' },
                        ...uniqueValues(gmailPollingRuns.map((row) => row.merchantId ?? '')).map((value) => ({ value, label: value }))
                      ]}
                    />
                    <Select
                      value={gmailRunSuccessFilter}
                      onChange={setGmailRunSuccessFilter}
                      className="filter-select"
                      options={[
                        { value: 'ALL', label: '전체 결과' },
                        { value: 'SUCCESS', label: '성공' },
                        { value: 'FAILURE', label: '실패' }
                      ]}
                    />
                    <Select
                      value={gmailRunDaysFilter}
                      onChange={setGmailRunDaysFilter}
                      className="filter-select"
                      options={[
                        { value: 'ALL', label: '전체 기간' },
                        { value: 1, label: '오늘' },
                        { value: 7, label: '7일' },
                        { value: 30, label: '30일' }
                      ]}
                    />
                  </Space>
                  )}
                />
              </Suspense>
            ) : null}
            {authToken && activeScreen === 'audits' ? (
              <Suspense fallback={<section className="work-panel"><div className="empty-panel">변경 이력 화면을 불러오는 중입니다.</div></section>}>
                <TableScreen
                  title="변경 이력"
                  rows={auditLogs}
                  columns={auditColumns}
                  x={1560}
                  total={adminAuditTotal}
                  pagination={{
                    current: adminAuditPage,
                    pageSize: adminAuditLimit,
                    total: adminAuditTotal,
                    showSizeChanger: false,
                    onChange: setAdminAuditPage
                  }}
                  toolbar={(
                  <Space wrap>
                    <Select
                      value={adminAuditActionFilter}
                      onChange={setAdminAuditActionFilter}
                      className="filter-select"
                      options={[
                        { value: 'ALL', label: '전체 액션' },
                        ...adminAuditFilterOptions.actionTypes.map((value) => ({
                          value,
                          label: auditActionText(value)
                        }))
                      ]}
                    />
                    <Select
                      value={adminAuditTargetFilter}
                      onChange={setAdminAuditTargetFilter}
                      className="filter-select"
                      options={[
                        { value: 'ALL', label: '전체 대상' },
                        ...adminAuditFilterOptions.targetTypes.map((value) => ({
                          value,
                          label: auditTargetText(value)
                        }))
                      ]}
                    />
                    <Select
                      value={adminAuditMerchantFilter}
                      onChange={setAdminAuditMerchantFilter}
                      className="filter-select"
                      options={[
                        { value: 'ALL', label: '전체 이용자' },
                        ...adminAuditFilterOptions.merchantIds.map((value) => ({ value, label: value }))
                      ]}
                    />
                    <Select
                      value={adminAuditMetadataKeyFilter}
                      onChange={setAdminAuditMetadataKeyFilter}
                      className="filter-select"
                      options={[
                        { value: 'ALL', label: '전체 metadata' },
                        ...adminAuditFilterOptions.metadataKeys.map((value) => ({ value, label: value }))
                      ]}
                    />
                    <Select
                      value={adminAuditDaysFilter}
                      onChange={setAdminAuditDaysFilter}
                      className="filter-select"
                      options={[
                        { value: 'ALL', label: '전체 기간' },
                        { value: 1, label: '오늘' },
                        { value: 7, label: '7일' },
                        { value: 30, label: '30일' }
                      ]}
                    />
                    <Input.Search
                      allowClear
                      value={adminAuditSearchText}
                      onChange={(event) => setAdminAuditSearchText(event.target.value)}
                      placeholder="작업자, 대상 ID, 요약 검색"
                    />
                    <Typography.Text type="secondary">
                      {adminAuditPage}페이지 / 총 {adminAuditTotal.toLocaleString()}건
                    </Typography.Text>
                  </Space>
                  )}
                />
              </Suspense>
            ) : null}
            {authToken && activeScreen === 'backup' ? (
              <Suspense fallback={<section className="work-panel"><div className="empty-panel">백업/복구 화면을 불러오는 중입니다.</div></section>}>
                <BackupScreen backup={backup} onLoadBackup={loadBackup} />
              </Suspense>
            ) : null}
            {authToken && activeScreen === 'settings' ? (
              <Suspense fallback={<section className="work-panel"><div className="empty-panel">시스템 설정 화면을 불러오는 중입니다.</div></section>}>
                <SettingsScreen
                  settings={settings}
                  settingColumns={settingColumns}
                  courierCsvValidationRules={courierCsvValidationRules}
                  courierCsvValidationRuleColumns={courierCsvValidationRuleColumns}
                  courierCsvToolbar={(
                    <Button type="primary" size="small" onClick={openCreateCourierCsvRule}>
                      규칙 추가
                    </Button>
                  )}
                />
              </Suspense>
            ) : null}
          </Space>
          <BulkErrorHandlingModal
            handlingStatus={bulkErrorHandlingStatus}
            loading={bulkErrorActionLoading}
            preview={bulkErrorPreview}
            form={bulkErrorForm}
            onCancel={() => setBulkErrorHandlingStatus(null)}
            onSubmit={updateSelectedErrorHandling}
            errorHandlingStatusText={errorHandlingStatusText}
          />
          <Modal
            title="택배 CSV 검증 규칙 추가"
            open={csvRuleModalOpen}
            confirmLoading={csvRuleActionId === 'create'}
            okText="저장"
            cancelText="취소"
            onCancel={() => setCsvRuleModalOpen(false)}
            onOk={() => csvRuleForm.validateFields().then(createCourierCsvValidationRule)}
          >
            <Form form={csvRuleForm} layout="vertical">
              <Form.Item name="courierCsvTemplate" label="택배 템플릿" rules={[{ required: true, message: '택배 템플릿을 선택하세요.' }]}>
                <Select options={[
                  { value: 'GENERIC', label: '공통' },
                  { value: 'CJ_LOGISTICS', label: 'CJ대한통운' },
                  { value: 'LOTTE', label: '롯데택배' },
                  { value: 'HANJIN', label: '한진택배' }
                ]} />
              </Form.Item>
              <Form.Item name="fieldName" label="필드" rules={[{ required: true, message: '필드를 입력하세요.' }]}>
                <Input placeholder="deliveryRecipientName" />
              </Form.Item>
              <Form.Item name="csvColumnName" label="CSV 컬럼명">
                <Input />
              </Form.Item>
              <Form.Item name="required" label="필수 여부" rules={[{ required: true, message: '필수 여부를 선택하세요.' }]}>
                <Select options={[
                  { value: true, label: '필수' },
                  { value: false, label: '선택' }
                ]} />
              </Form.Item>
              <Form.Item name="errorMessage" label="오류 메시지" rules={[{ required: true, message: '오류 메시지를 입력하세요.' }]}>
                <Input />
              </Form.Item>
              <Form.Item name="sortOrder" label="순서" rules={[{ required: true, message: '순서를 입력하세요.' }]}>
                <Input type="number" />
              </Form.Item>
              <Form.Item name="active" label="사용 여부" rules={[{ required: true, message: '사용 여부를 선택하세요.' }]}>
                <Select options={[
                  { value: true, label: '사용' },
                  { value: false, label: '중지' }
                ]} />
              </Form.Item>
            </Form>
          </Modal>
          <GmailRunFailureDrawer
            gmailRun={selectedGmailRun}
            onClose={() => setSelectedGmailRun(null)}
            onShowInboundMessage={showInboundMessage}
            onShowErrorLogs={showErrorLogsForGmailRun}
            onOpenErrorHandling={openGmailRunErrorHandlingModal}
            onOpenChecklist={() => window.open(gmailChecklistUrl, '_blank', 'noopener,noreferrer')}
            gmailFailureGuide={gmailFailureGuide}
            formatDate={formatDate}
          />
          <CourierCsvRuleAuditDrawer
            selectedCsvRule={selectedCsvRule}
            auditLogs={csvRuleAuditLogs}
            loading={csvRuleAuditLoading}
            changedOnly={csvRuleAuditChangedOnly}
            onChangedOnlyChange={setCsvRuleAuditChangedOnly}
            onClose={() => setSelectedCsvRule(null)}
            formatDate={formatDate}
            auditActionText={auditActionText}
            courierCsvRuleAuditDiffText={courierCsvRuleAuditDiffText}
            courierCsvRuleAuditMetadataRaw={courierCsvRuleAuditMetadataRaw}
          />
        </Layout.Content>
      </Layout>
    </Layout>
  );
}

function DataPanel<T extends object>({
  title,
  rows,
  columns,
  x,
  toolbar,
  total,
  pagination,
  rowSelection
}: {
  title: string;
  rows: T[];
  columns: ColumnsType<T>;
  x: number;
  toolbar?: ReactNode;
  total?: number;
  pagination?: TableProps<T>['pagination'];
  rowSelection?: TableProps<T>['rowSelection'];
}) {
  return (
    <section className="work-panel">
      <div className="table-heading">
        <Typography.Title level={2}>{title}</Typography.Title>
        <Space wrap>
          {toolbar}
          <Typography.Text type="secondary">총 {(total ?? rows.length).toLocaleString()}건</Typography.Text>
        </Space>
      </div>
      <Table
        rowKey={rowKey}
        columns={columns}
        dataSource={rows}
        loading={false}
        scroll={{ x }}
        pagination={pagination ?? { pageSize: 15, showSizeChanger: false }}
        rowSelection={rowSelection}
      />
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

async function requestText(path: string, init?: RequestInit): Promise<string> {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    ...init,
    headers: {
      ...authHeaders(),
      ...init?.headers
    }
  });
  if (!response.ok) {
    throw new Error(`API가 HTTP ${response.status}를 반환했습니다.`);
  }
  return response.text();
}

function authHeaders(): Record<string, string> {
  const token = localStorage.getItem('shipmate_admin_token');
  return token ? { Authorization: `Bearer ${token}` } : {};
}

function errorMessage(cause: unknown, fallback: string) {
  return cause instanceof Error ? cause.message : fallback;
}

function compareCourierCsvValidationRule(left: CourierCsvValidationRule, right: CourierCsvValidationRule) {
  return left.courierCsvTemplate.localeCompare(right.courierCsvTemplate)
    || left.sortOrder - right.sortOrder
    || left.fieldName.localeCompare(right.fieldName);
}

function gmailFailureGuide(failureCode?: string | null, failureMessage?: string | null) {
  return operationFailureGuide(failureCode, failureMessage);
}

function operationFailureGuide(failureCode?: string | null, failureMessage?: string | null) {
  const code = failureCode ?? '';
  const messageText = (failureMessage ?? '').toLowerCase();
  if (code === 'SM-GML-9102' || code === 'SM-GML-9103' || messageText.includes('refresh token')) {
    return 'Google Gmail 연결을 다시 진행하고 refresh token 발급 여부를 확인합니다.';
  }
  if (code === 'SM-GML-9108' || messageText.includes('unread') || messageText.includes('modify')) {
    return 'OAuth scope가 gmail.modify인지 확인한 뒤 Gmail 연결을 다시 승인합니다.';
  }
  if (code === 'SM-GML-9107' || messageText.includes('attachment')) {
    return '객체 저장소 상태와 첨부 용량을 확인한 뒤 재수집합니다.';
  }
  if (code === 'SM-GML-9104' || code === 'SM-GML-9105') {
    return 'Google API 응답 상태, 네트워크, OAuth client 설정을 확인합니다.';
  }
  if (code === 'SM-GML-9106') {
    return '원문은 저장되었으므로 주문 원문 화면에서 초안 생성 로그와 원문 내용을 확인합니다.';
  }
  return '연결 계정, Google API 상태, 최근 주문 원문 저장 여부를 순서대로 확인합니다.';
}

function courierCsvRuleAuditDiffText(value: string | null | undefined, changedOnly: boolean) {
  const rows = courierCsvRuleAuditDiffRows(value, changedOnly);
  if (!rows.length) {
    return '변경 전/후 metadata가 없는 기존 이력입니다.';
  }
  return (
    <Space direction="vertical" size={2}>
      {rows.map((row) => (
        <Typography.Text key={row.fieldLabel}>
          {row.fieldLabel}: {row.beforeValue} → {row.afterValue}
        </Typography.Text>
      ))}
    </Space>
  );
}

function courierCsvRuleAuditMetadataRaw(value: string | null | undefined) {
  if (!value) {
    return '-';
  }
  return (
    <Collapse
      size="small"
      ghost
      items={[{
        key: 'metadata',
        label: 'metadata 보기',
        children: (
          <Space direction="vertical" size={8} className="content-stack">
            <Button
              size="small"
              icon={<CopyOutlined />}
              onClick={() => copyCourierCsvRuleAuditMetadata(value)}
            >
              원문 복사
            </Button>
            <pre className="json-block compact">{formatJsonText(value)}</pre>
          </Space>
        )
      }]}
    />
  );
}

async function copyCourierCsvRuleAuditMetadata(value: string) {
  try {
    await navigator.clipboard.writeText(formatJsonText(value));
    message.success('metadata 원문을 복사했습니다.');
  } catch {
    message.error('metadata 원문을 복사하지 못했습니다.');
  }
}

function formatJsonText(value: string) {
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}

function courierCsvRuleAuditDiffRows(value: string | null | undefined, changedOnly: boolean): CsvRuleAuditDiffRow[] {
  if (!value) {
    return [];
  }
  try {
    const parsed = JSON.parse(value) as { before?: Record<string, unknown> | null; after?: Record<string, unknown> | null };
    const before = parsed.before ?? {};
    const after = parsed.after ?? {};
    const fields: Array<[string, string]> = [
      ['courierCsvTemplate', '택배 템플릿'],
      ['fieldName', '필드'],
      ['csvColumnName', 'CSV 컬럼'],
      ['required', '필수 여부'],
      ['errorMessage', '오류 메시지'],
      ['sortOrder', '순서'],
      ['active', '사용 여부']
    ];
    return fields
      .map(([fieldName, fieldLabel]) => ({
        fieldLabel,
        beforeValue: csvRuleValueText(before[fieldName]),
        afterValue: csvRuleValueText(after[fieldName])
      }))
      .filter((row) => !changedOnly || row.beforeValue !== row.afterValue);
  } catch {
    return [];
  }
}

function csvRuleValueText(value: unknown) {
  if (value === null || value === undefined || value === '') {
    return '-';
  }
  if (typeof value === 'boolean') {
    return value ? '예' : '아니오';
  }
  return String(value);
}

function gmailRunRelatedErrorLogIds(errorLogs: ErrorLog[], gmailRun: GoogleGmailPollingRun) {
  return errorLogs
    .filter((errorLog) => isGmailRunRelatedErrorLog(errorLog, gmailRun))
    .map((errorLog) => errorLog.errorLogId);
}

function gmailRunErrorSearchTexts(gmailRun: GoogleGmailPollingRun) {
  return uniqueValues([
    gmailRun.failureCode ?? '',
    gmailRun.latestInboundMessageId ?? '',
    gmailRun.failureMessage ?? '',
    gmailRun.gmailPollingRunId
  ].filter((value) => value.trim()).map((value) => value.trim()));
}

function isGmailRunRelatedErrorLog(errorLog: ErrorLog, gmailRun: GoogleGmailPollingRun) {
  const code = gmailRun.failureCode ?? '';
  const messageText = gmailRun.failureMessage ?? '';
  const latestInboundMessageId = gmailRun.latestInboundMessageId ?? '';
  const metadataText = errorLog.errorMetadataJson ?? '';
  return Boolean(
    (code && errorLog.code === code)
    || (code && metadataText.includes(code))
    || (messageText && errorLog.message.includes(messageText))
    || (latestInboundMessageId && metadataText.includes(latestInboundMessageId))
    || (latestInboundMessageId && errorLog.message.includes(latestInboundMessageId))
  );
}

function errorHandlingStatusText(status: string) {
  const labels: Record<string, string> = {
    OPEN: '미확인',
    ACKNOWLEDGED: '확인',
    RESOLVED: '조치 완료'
  };
  return labels[status] ?? status;
}

function selectedErrorHandlingPreview(errorLogs: ErrorLog[], selectedErrorLogIds: Key[]) {
  const selectedIds = new Set(selectedErrorLogIds.map(String));
  const rows = errorLogs.filter((row) => selectedIds.has(row.errorLogId));
  return {
    total: rows.length,
    open: rows.filter((row) => (row.handlingStatus ?? 'OPEN') === 'OPEN').length,
    acknowledged: rows.filter((row) => row.handlingStatus === 'ACKNOWLEDGED').length,
    resolved: rows.filter((row) => row.handlingStatus === 'RESOLVED').length
  };
}

function statusTag(status: string) {
  const color = status.includes('FAILED') || status.includes('ERROR') ? 'red'
    : status.includes('SUBMITTED') || status.includes('DONE') || status.includes('UPLOADED') ? 'green'
      : 'blue';
  return <Tag color={color}>{status}</Tag>;
}

function auditActionLabel(actionType: string) {
  return <Tag>{auditActionText(actionType)}</Tag>;
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
    CUSTOMER_ARCHIVED: '고객 보관',
    ADMIN_COURIER_CSV_RULE_CREATED: '택배 CSV 규칙 생성',
    ADMIN_COURIER_CSV_RULE_UPDATED: '택배 CSV 규칙 수정'
  };
  return labels[actionType] ?? actionType;
}

function auditTargetLabel(targetType: string) {
  return auditTargetText(targetType);
}

function auditTargetText(targetType: string) {
  const labels: Record<string, string> = {
    ORDER_DRAFT: '출고표',
    INBOUND_MESSAGE: '주문 원문',
    CUSTOMER: '고객',
    EMAIL_ATTACHMENT: '이메일 첨부',
    AI_EXTRACTION: 'AI 추출',
    COURIER_CSV_VALIDATION_RULE: '택배 CSV 검증 규칙'
  };
  return labels[targetType] ?? targetType;
}

function emptyText(value: string | null | undefined) {
  return value || '-';
}

function formatBytes(bytes: number) {
  if (!bytes || bytes < 0) return '-';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

function formatDate(value: string | null | undefined) {
  if (!value) return '-';
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value));
}

function adminInboundMessageMatchesSearch(inboundMessage: InboundMessage, searchText: string) {
  const keyword = searchText.trim().toLowerCase();
  if (!keyword) {
    return true;
  }
  return [
    inboundMessage.inboundMessageId,
    inboundMessage.merchantId,
    inboundMessage.customerId ?? '',
    inboundMessage.sourceChannel,
    inboundMessage.businessChannel,
    inboundMessage.inboundMessageStatus,
    inboundMessage.rawText
  ].some((value) => value.toLowerCase().includes(keyword));
}

function adminErrorLogMatchesSearch(errorLog: ErrorLog, searchText: string) {
  const keyword = searchText.trim().toLowerCase();
  if (!keyword) {
    return true;
  }
  return [
    errorLog.errorLogId,
    errorLog.code,
    String(errorLog.status),
    errorLog.message,
    errorLog.exceptionType ?? '',
    errorLog.handlingStatus,
    errorLog.assignedActorId ?? '',
    errorLog.handlingMemo ?? '',
    errorLog.errorMetadataJson ?? '',
    ...metadataValues(errorLog.errorMetadataJson)
  ].some((value) => value.toLowerCase().includes(keyword));
}

function metadataKeys(value: string | null | undefined) {
  if (!value) {
    return [];
  }
  try {
    return Object.keys(JSON.parse(value) as Record<string, unknown>);
  } catch {
    return [];
  }
}

function metadataValues(value: string | null | undefined) {
  if (!value) {
    return [];
  }
  try {
    return Object.values(JSON.parse(value) as Record<string, unknown>).map((item) => String(item ?? ''));
  } catch {
    return [];
  }
}

function adminOrderDraftMatchesSearch(orderDraft: OrderDraft, searchText: string) {
  const keyword = searchText.trim().toLowerCase();
  if (!keyword) {
    return true;
  }
  return [
    orderDraft.orderDraftId,
    orderDraft.sourceInboundMessageId,
    orderDraft.merchantId,
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
    .toLowerCase()
    .includes(keyword);
}

function adminOrderDraftWithinDays(orderDraft: OrderDraft, days: number | 'ALL') {
  if (days === 'ALL') {
    return true;
  }
  const updatedAt = new Date(orderDraft.updatedAt).getTime();
  if (!Number.isFinite(updatedAt)) {
    return false;
  }
  return updatedAt >= Date.now() - days * 24 * 60 * 60 * 1000;
}

function adminOrderDraftMissingFields(orderDraft: OrderDraft) {
  const missingFields: string[] = [];
  if (!orderDraft.deliveryRecipientName?.trim()) missingFields.push('수취인');
  if (!orderDraft.deliveryRecipientPhone?.trim()) missingFields.push('연락처');
  if (!orderDraft.deliveryAddress?.trim()) missingFields.push('주소');
  if (!orderDraft.itemCandidates.length) missingFields.push('품목 후보');
  orderDraft.itemCandidates.forEach((itemCandidate, index) => {
    const prefix = `품목 ${index + 1}`;
    if (!itemCandidate.itemName?.trim()) missingFields.push(`${prefix} 품목명`);
    if (!itemCandidate.itemQuantity?.trim()) missingFields.push(`${prefix} 수량`);
    if (!itemCandidate.itemUnit?.trim()) missingFields.push(`${prefix} 단위`);
  });
  return missingFields;
}

function adminAuditLogMatchesSearch(auditLog: AuditLog, searchText: string) {
  const keyword = searchText.trim().toLowerCase();
  if (!keyword) {
    return true;
  }
  return [
    auditLog.auditLogId,
    auditLog.merchantId,
    auditLog.actorId,
    auditLog.actionType,
    auditActionText(auditLog.actionType),
    auditLog.targetType,
    auditTargetText(auditLog.targetType),
    auditLog.targetId,
    auditLog.summary
  ].filter(Boolean).some((value) => String(value).toLowerCase().includes(keyword));
}

function adminAuditLogWithinDays(auditLog: AuditLog, days: number | 'ALL') {
  if (days === 'ALL') {
    return true;
  }
  const createdAt = new Date(auditLog.createdAt).getTime();
  if (!Number.isFinite(createdAt)) {
    return false;
  }
  return createdAt >= Date.now() - days * 24 * 60 * 60 * 1000;
}

function uniqueValues(values: string[]) {
  return Array.from(new Set(values.filter(Boolean))).sort();
}

function rowKey(row: object) {
  const source = row as Record<string, unknown>;
  return String(
    source.merchantId
    ?? source.merchantUserId
    ?? source.inboundMessageId
    ?? source.orderDraftId
    ?? source.recordingId
    ?? source.errorLogId
    ?? source.auditLogId
    ?? JSON.stringify(source)
  );
}

export default App;

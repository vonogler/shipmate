const { chromium } = require('playwright');

const now = new Date().toISOString();
const adminBaseUrl = process.env.ADMIN_BASE_URL || 'https://127.0.0.1/admin/';
const merchantBaseUrl = process.env.MERCHANT_BASE_URL || 'https://127.0.0.1/merchant/';

async function main() {
  const browser = await launchChromium();
  try {
    await checkAdminLogin(browser);
    await checkAdmin(browser);
    await checkMerchantLoginAndDraftReview(browser);
    await checkMerchant(browser);
    console.log('PASS shipmate-ui-playwright');
  } finally {
    await browser.close();
  }
}

async function launchChromium() {
  try {
    return await chromium.launch({ headless: true, chromiumSandbox: false });
  } catch (cause) {
    const message = cause instanceof Error ? cause.message : String(cause);
    if (message.includes('libnspr4.so')) {
      throw new Error([
        'Playwright Chromium 실행에 필요한 WSL 시스템 라이브러리가 없습니다: libnspr4.so',
        '사용자 실행 위치: WSL 터미널',
        '실행 명령: sudo apt-get update && sudo apt-get install -y libnspr4',
        `설치 후 다시 실행: cd ${process.cwd()} && npm run check:ui`
      ].join('\n'));
    }
    throw cause;
  }
}

async function checkAdminLogin(browser) {
  const context = await browser.newContext({
    baseURL: adminBaseUrl,
    ignoreHTTPSErrors: true,
    viewport: { width: 1440, height: 1000 }
  });
  const page = await context.newPage();
  let loginCalled = false;
  await page.route('**/api/admin/**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname;
    if (path === '/api/admin/auth/login' && request.method() === 'POST') {
      loginCalled = true;
      return json(route, adminLoginResponse());
    }
    if (path === '/api/admin/overview') return json(route, adminOverview());
    if (path === '/api/admin/merchants') return json(route, []);
    if (path === '/api/admin/users') return json(route, []);
    if (path === '/api/admin/inbound-messages') return json(route, []);
    if (path === '/api/admin/order-drafts') return json(route, []);
    if (path === '/api/admin/shipping-orders') return json(route, []);
    if (path === '/api/admin/call-recordings') return json(route, []);
    if (path === '/api/admin/error-logs') return json(route, { rows: [], total: 0 });
    if (path === '/api/admin/gmail-polling-runs') return json(route, []);
    if (path === '/api/admin/settings') return json(route, []);
    if (path === '/api/admin/settings/courier-csv-validation-rules') return json(route, []);
    return json(route, {});
  });

  await page.goto('./');
  await page.getByLabel('로그인 ID').fill('shipmate');
  await page.getByRole('textbox', { name: '* 비밀번호' }).fill('shipmate');
  await page.getByRole('button', { name: '로그인' }).click();
  await page.getByText('운영 대시보드').first().waitFor({ timeout: 5000 });
  if (!loginCalled) {
    throw new Error('Admin 로그인 API 호출 검증 실패');
  }
  await context.close();
  console.log('PASS admin login');
}

async function checkAdmin(browser) {
  const context = await browser.newContext({
    baseURL: adminBaseUrl,
    ignoreHTTPSErrors: true,
    permissions: ['clipboard-read', 'clipboard-write'],
    viewport: { width: 1440, height: 1000 }
  });
  const page = await context.newPage();
  let gmailRunLoadCount = 0;
  let errorLogState = gmailErrorLog();
  let csvRuleState = csvRule();
  const errorInlineUpdates = [];
  const csvRuleInlineUpdates = [];

  await page.route('**/api/admin/**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname;
    if (path === '/api/admin/overview') return json(route, adminOverview());
    if (path === '/api/admin/merchants') return json(route, []);
    if (path === '/api/admin/users') return json(route, []);
    if (path === '/api/admin/inbound-messages') return json(route, []);
    if (path === '/api/admin/order-drafts') return json(route, []);
    if (path === '/api/admin/shipping-orders') return json(route, []);
    if (path === '/api/admin/call-recordings') return json(route, []);
    if (path === '/api/admin/settings') return json(route, []);
    if (path === '/api/admin/settings/courier-csv-validation-rules') return json(route, [csvRuleState]);
    if (path === '/api/admin/settings/courier-csv-validation-rules/ccvr_mock_1' && request.method() === 'PUT') {
      const payload = request.postDataJSON();
      csvRuleInlineUpdates.push(payload);
      csvRuleState = { ...csvRuleState, ...payload, updatedAt: now };
      return json(route, csvRuleState);
    }
    if (path === '/api/admin/audit-logs/filter-options') {
      return json(route, { actionTypes: [], targetTypes: [], merchantIds: [], metadataKeys: [] });
    }
    if (path === '/api/admin/audit-logs') {
      return json(route, { rows: [csvRuleAuditLog()], total: 1 });
    }
    if (path === '/api/admin/gmail-polling-runs') {
      gmailRunLoadCount += 1;
      return json(route, [gmailRun(gmailRunLoadCount > 1 ? '갱신된 실패 메시지' : '초기 실패 메시지')]);
    }
    if (path === '/api/admin/error-logs' && request.method() === 'GET') {
      return json(route, { rows: [errorLogState], total: 1 });
    }
    if (path === '/api/admin/error-logs/err_mock_1/handling' && request.method() === 'PUT') {
      const payload = request.postDataJSON();
      errorInlineUpdates.push(payload);
      errorLogState = { ...errorLogState, ...payload, handledAt: now };
      return json(route, errorLogState);
    }
    if (path === '/api/admin/error-logs/handling' && request.method() === 'PUT') {
      const payload = request.postDataJSON();
      errorLogState = { ...errorLogState, handlingStatus: payload.handlingStatus, assignedActorId: payload.assignedActorId, handlingMemo: payload.handlingMemo, handledAt: now };
      return json(route, [errorLogState]);
    }
    return json(route, {});
  });

  await page.addInitScript(() => {
    localStorage.setItem('shipmate_admin_token', 'mock-token');
    localStorage.setItem('shipmate_admin_user', JSON.stringify({
      accessToken: 'mock-token',
      merchantUserId: 'admin_mock',
      loginId: 'shipmate',
      displayName: '관리자',
      role: 'ADMIN'
    }));
  });

  await page.goto('./');
  await page.waitForLoadState('networkidle');
  await clickMenuItem(page, 'Gmail 수집 이력');
  await page.getByRole('button', { name: '상세' }).click();
  await page.getByRole('button', { name: '관련 오류 확인 처리' }).click();
  await page.getByRole('button', { name: '저장' }).click();
  await page.getByLabel('Gmail 수집 실패 상세').getByText('갱신된 실패 메시지').waitFor({ timeout: 5000 });
  if (errorLogState.handlingMemo !== 'Gmail 실패 일괄 확인' || gmailRunLoadCount < 2) {
    throw new Error(`Gmail drawer 갱신 검증 실패: handlingMemo=${errorLogState.handlingMemo}, gmailRunLoadCount=${gmailRunLoadCount}`);
  }

  await page.locator('.ant-modal-wrap').waitFor({ state: 'hidden', timeout: 5000 });
  await page.locator('.ant-drawer .ant-drawer-close').first().click();
  await page.getByLabel('Gmail 수집 실패 상세').waitFor({ state: 'hidden', timeout: 5000 });
  await clickMenuItem(page, '오류 로그');
  await page.locator('.ant-table textarea').first().waitFor({ timeout: 5000 });
  const handlingMemo = await page.locator('.ant-table textarea').first().inputValue();
  if (handlingMemo !== 'Gmail 실패 일괄 확인') {
    throw new Error(`오류 로그 처리 메모 갱신 검증 실패: ${handlingMemo}`);
  }
  await checkAdminErrorInlineEdit(page, errorInlineUpdates);
  await clickMenuItem(page, '시스템 설정');
  await checkAdminCsvRuleInlineEdit(page, csvRuleInlineUpdates);
  await page.getByRole('button', { name: '이력' }).click();
  await page.getByText('metadata 보기').click();
  await page.getByRole('button', { name: '원문 복사' }).click();
  await page.getByText('metadata 원문을 복사했습니다.').waitFor({ timeout: 5000 });
  const copied = await page.evaluate(() => navigator.clipboard.readText());
  if (!copied.includes('csvColumnName') || !copied.includes('수취인')) {
    throw new Error(`CSV metadata 복사 검증 실패: ${copied}`);
  }
  await page.evaluate(() => {
    Object.defineProperty(navigator, 'clipboard', {
      configurable: true,
      value: {
        writeText: () => Promise.reject(new Error('mock clipboard failure')),
        readText: () => Promise.resolve('')
      }
    });
  });
  await page.getByRole('button', { name: '원문 복사' }).click();
  await page.getByText('metadata 원문을 복사하지 못했습니다.').waitFor({ timeout: 5000 });
  await context.close();
  console.log('PASS admin drawer, inline edit and csv copy');
}

async function checkAdminErrorInlineEdit(page, errorInlineUpdates) {
  const row = page.locator('.ant-table-row').filter({ hasText: 'err_mock_1' }).first();
  await row.locator('.ant-select').first().click();
  await page.getByTitle('조치 완료').click();
  await page.getByText('오류 처리 정보를 저장했습니다.').first().waitFor({ timeout: 5000 });

  const assignedActorInput = row.locator('input.ant-input').first();
  await assignedActorInput.fill('ops-admin');
  await assignedActorInput.blur();
  await page.getByText('오류 처리 정보를 저장했습니다.').first().waitFor({ timeout: 5000 });

  const memoInput = row.locator('textarea').first();
  await memoInput.fill('개별 조치 완료');
  await memoInput.blur();
  await page.getByText('오류 처리 정보를 저장했습니다.').first().waitFor({ timeout: 5000 });

  assertPayloadSeen(errorInlineUpdates, (payload) => payload.handlingStatus === 'RESOLVED', '오류 로그 처리 상태 inline 저장');
  assertPayloadSeen(errorInlineUpdates, (payload) => payload.assignedActorId === 'ops-admin', '오류 로그 담당자 inline 저장');
  assertPayloadSeen(errorInlineUpdates, (payload) => payload.handlingMemo === '개별 조치 완료', '오류 로그 처리 메모 inline 저장');
}

async function checkAdminCsvRuleInlineEdit(page, csvRuleInlineUpdates) {
  const row = page.locator('.ant-table-row').filter({ hasText: 'ccvr_mock_1' }).first();
  await row.waitFor({ timeout: 5000 });

  const inputs = row.locator('input.ant-input');
  await inputs.nth(0).fill('받는분');
  await inputs.nth(0).blur();
  await page.getByText('CSV 검증 규칙을 저장했습니다.').first().waitFor({ timeout: 5000 });

  await row.locator('.ant-select').nth(0).click();
  await page.getByTitle('선택').click();
  await page.getByText('CSV 검증 규칙을 저장했습니다.').first().waitFor({ timeout: 5000 });

  await inputs.nth(1).fill('받는분을 입력하세요.');
  await inputs.nth(1).blur();
  await page.getByText('CSV 검증 규칙을 저장했습니다.').first().waitFor({ timeout: 5000 });

  await inputs.nth(2).fill('3');
  await inputs.nth(2).blur();
  await page.getByText('CSV 검증 규칙을 저장했습니다.').first().waitFor({ timeout: 5000 });

  await row.locator('.ant-select').nth(1).click();
  await page.getByTitle('중지').click();
  await page.getByText('CSV 검증 규칙을 저장했습니다.').first().waitFor({ timeout: 5000 });

  assertPayloadSeen(csvRuleInlineUpdates, (payload) => payload.csvColumnName === '받는분', 'CSV 컬럼명 inline 저장');
  assertPayloadSeen(csvRuleInlineUpdates, (payload) => payload.required === false, 'CSV 필수 여부 inline 저장');
  assertPayloadSeen(csvRuleInlineUpdates, (payload) => payload.errorMessage === '받는분을 입력하세요.', 'CSV 오류 메시지 inline 저장');
  assertPayloadSeen(csvRuleInlineUpdates, (payload) => payload.sortOrder === 3, 'CSV 순서 inline 저장');
  assertPayloadSeen(csvRuleInlineUpdates, (payload) => payload.active === false, 'CSV 사용 여부 inline 저장');
}

function assertPayloadSeen(payloads, predicate, label) {
  if (!payloads.some(predicate)) {
    throw new Error(`${label} payload 검증 실패: ${JSON.stringify(payloads)}`);
  }
}

async function checkMerchantLoginAndDraftReview(browser) {
  const context = await browser.newContext({
    baseURL: merchantBaseUrl,
    ignoreHTTPSErrors: true,
    viewport: { width: 1440, height: 1000 }
  });
  const page = await context.newPage();
  let loginCalled = false;
  let reviewSaved = false;
  await page.route('**/api/**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname;
    if (path === '/api/auth/login' && request.method() === 'POST') {
      loginCalled = true;
      return json(route, merchantLoginResponse());
    }
    if (path === '/api/order-drafts/odft_review_1/review' && request.method() === 'PUT') {
      reviewSaved = true;
      return json(route, orderDraft());
    }
    if (path === '/api/inbound-messages') return json(route, []);
    if (path === '/api/order-drafts') return json(route, [orderDraft()]);
    if (path === '/api/shipping-orders') return json(route, [shippingOrder()]);
    if (path === '/api/customers') return json(route, []);
    if (path === '/api/operation/dashboard') return json(route, merchantDashboard());
    if (path === '/api/operation/audit-logs') return json(route, shippingAuditLogs());
    if (path === '/api/email-orders/attachments') return json(route, []);
    return json(route, {});
  });

  await page.goto('./');
  await page.getByLabel('로그인 ID').fill('shipmate');
  await page.getByRole('textbox', { name: '* 비밀번호' }).fill('shipmate');
  await page.getByRole('button', { name: '로그인' }).click();
  await clickMenuItem(page, '초안 검수');
  await page.getByText('출고표 초안 목록').waitFor({ timeout: 5000 });
  await page.getByRole('heading', { name: '초안 검수' }).waitFor({ timeout: 5000 });
  await page.getByText('품목 후보').waitFor({ timeout: 5000 });
  await page.locator('input[value="박스"]').waitFor({ timeout: 5000 });
  await page.getByRole('button', { name: '검수 내용 저장' }).click();
  await page.getByText('초안 검수 내용이 저장되었습니다.').waitFor({ timeout: 5000 });
  if (!loginCalled) {
    throw new Error('Merchant 로그인 API 호출 검증 실패');
  }
  if (!reviewSaved) {
    throw new Error('Merchant 초안 검수 저장 API 호출 검증 실패');
  }
  await context.close();
  console.log('PASS merchant login and draft review');
}

async function checkMerchant(browser) {
  const context = await browser.newContext({
    baseURL: merchantBaseUrl,
    ignoreHTTPSErrors: true,
    viewport: { width: 1440, height: 1000 }
  });
  const page = await context.newPage();
  await page.route('**/api/**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname;
    if (path === '/api/inbound-messages') return json(route, []);
    if (path === '/api/order-drafts') return json(route, []);
    if (path === '/api/shipping-orders') return json(route, [shippingOrder()]);
    if (path === '/api/shipping-orders/courier-csv/validate' && request.method() === 'POST') {
      return json(route, {
        valid: false,
        errors: ['수취인을 입력하세요.'],
        errorDetails: [{
          orderDraftId: 'odft_shipping_1',
          fieldName: 'deliveryRecipientName',
          csvColumnName: '수취인',
          message: '수취인을 입력하세요.'
        }]
      });
    }
    if (path === '/api/customers') return json(route, []);
    if (path === '/api/operation/dashboard') return json(route, merchantDashboard());
    if (path === '/api/operation/audit-logs') return json(route, shippingAuditLogs());
    return json(route, {});
  });
  await page.addInitScript(() => {
    localStorage.setItem('shipmate_merchant_token', 'mock-token');
    localStorage.setItem('shipmate_merchant_user', JSON.stringify({
      accessToken: 'mock-token',
      merchantUserId: 'merchant_user_mock',
      merchantId: 'merchant_mock',
      loginId: 'shipmate',
      displayName: '가맹점',
      role: 'ADMIN'
    }));
  });
  await page.goto('./?shippingDiffField=%EC%88%98%EB%9F%89&shippingOrderId=odft_shipping_1&shippingDiffSection=item-diff');
  await page.getByText('출고표 상세').waitFor({ timeout: 5000 });
  await page.getByText('품목 변경 1건').waitFor({ timeout: 5000 });
  const activeMenu = await page.locator('.ant-menu-item-selected').textContent();
  if (!activeMenu || !activeMenu.includes('출고/문서')) {
    throw new Error(`Merchant query 진입 메뉴 검증 실패: ${activeMenu}`);
  }
  const diffBox = page.locator('#shipping-order-item-diff');
  await diffBox.waitFor({ timeout: 5000 });
  const box = await waitForBoxNearTop(page, diffBox);
  if (!box || box.y > 420) {
    throw new Error(`Merchant diff section 위치 검증 실패: ${JSON.stringify(box)}`);
  }
  await page.getByRole('button', { name: '선택 CSV 검증' }).click();
  await page.getByText('CSV 검증 결과').waitFor({ timeout: 5000 });
  await page.locator('.validation-list').getByText('수취인을 입력하세요.').waitFor({ timeout: 5000 });
  await context.close();
  console.log('PASS merchant diff query');
}

async function waitForBoxNearTop(page, locator) {
  let lastBox = null;
  for (let index = 0; index < 12; index += 1) {
    lastBox = await locator.boundingBox();
    if (lastBox && lastBox.y <= 420) {
      return lastBox;
    }
    await page.waitForTimeout(300);
  }
  return lastBox;
}

async function clickMenuItem(page, text) {
  const menuItem = page.locator('.ant-menu-item').filter({ hasText: text }).first();
  try {
    await menuItem.click({ timeout: 10000 });
  } catch (cause) {
    const bodyText = await page.locator('body').innerText().catch(() => '');
    throw new Error(`메뉴 클릭 실패: ${text}\n${bodyText.slice(0, 1200)}`);
  }
}

function json(route, body) {
  return route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify(body)
  });
}

function adminOverview() {
  return {
    days: 7,
    merchantCount: 0,
    userCount: 0,
    inboundMessageCount: 0,
    orderDraftCount: 0,
    shippingOrderCount: 0,
    callRecordingCount: 0,
    failedCallRecordingCount: 0,
    errorLogCount: 1,
    openErrorLogCount: 1,
    acknowledgedErrorLogCount: 0,
    resolvedErrorLogCount: 0,
    gmailPollingRunCount: 1,
    gmailPollingFailureCount: 1,
    smokeReport: {
      status: 'ok',
      apiBaseUrl: 'http://localhost:18080',
      merchantId: 'merchant_smoke_mock',
      inboundMessageId: 'inmsg_smoke_mock',
      orderDraftId: 'odft_smoke_mock',
      lastStep: '[12/12] 변경 이력 metadata 확인',
      lastHttpStatus: '200',
      failureDetail: '',
      finishedAt: now,
      reportPath: 'shipmate-infra/smoke-report.json'
    },
    stagingCheck: {
      suiteStatus: 'ok',
      suiteReason: '-',
      latestSmokeSuccessAt: now,
      latestSmokeFailureAt: '',
      uiCheckStatus: 'ok',
      uiCheckDetail: '-',
      suiteFinishedAt: now,
      cleanupStatus: 'ok',
      cleanupMode: 'apply',
      cleanupTotalRows: '15',
      cleanupRemainingRows: '0',
      cleanupFinishedAt: now
    }
  };
}

function adminLoginResponse() {
  return {
    accessToken: 'mock-token',
    merchantUserId: 'admin_mock',
    merchantId: 'shipmate_internal',
    loginId: 'shipmate',
    displayName: '관리자',
    role: 'ADMIN'
  };
}

function merchantLoginResponse() {
  return {
    accessToken: 'mock-token',
    merchantUserId: 'merchant_user_mock',
    merchantId: 'merchant_mock',
    loginId: 'shipmate',
    displayName: '가맹점',
    role: 'ADMIN'
  };
}

function gmailRun(message) {
  return {
    gmailPollingRunId: 'gpr_mock_1',
    triggerType: 'MANUAL',
    merchantId: 'merchant_mock',
    success: false,
    accountCount: 1,
    scannedCount: 1,
    collectedCount: 0,
    orderDraftCreatedCount: 0,
    attachmentStoredCount: 0,
    skippedDuplicateCount: 0,
    skippedNonOrderCount: 0,
    failureCode: 'SM-GML-9108',
    failureMessage: message,
    latestInboundMessageId: 'inb_mock_1',
    startedAt: now,
    finishedAt: now
  };
}

function gmailErrorLog() {
  return {
    errorLogId: 'err_mock_1',
    code: 'SM-GML-9108',
    status: 500,
    message: '초기 실패 메시지',
    exceptionType: 'GmailException',
    requestMethod: 'POST',
    requestPath: '/api/oauth/google/poll',
    merchantId: 'merchant_mock',
    actorId: 'shipmate',
    handlingStatus: 'OPEN',
    assignedActorId: '',
    handlingMemo: '',
    handledAt: null,
    errorMetadataJson: JSON.stringify({ gmailPollingRunId: 'gpr_mock_1', latestInboundMessageId: 'inb_mock_1' }),
    createdAt: now
  };
}

function csvRule() {
  return {
    courierCsvValidationRuleId: 'ccvr_mock_1',
    courierCsvTemplate: 'GENERIC',
    fieldName: 'deliveryRecipientName',
    csvColumnName: '수취인',
    required: true,
    errorMessage: '수취인을 입력하세요.',
    sortOrder: 1,
    active: true,
    updatedAt: now
  };
}

function csvRuleAuditLog() {
  return {
    auditLogId: 'aud_csv_1',
    merchantId: 'shipmate_internal',
    actorId: 'shipmate',
    actionType: 'ADMIN_COURIER_CSV_RULE_UPDATED',
    targetType: 'COURIER_CSV_VALIDATION_RULE',
    targetId: 'ccvr_mock_1',
    summary: 'CSV 규칙 수정',
    auditMetadataJson: JSON.stringify({
      before: { csvColumnName: '받는분' },
      after: { csvColumnName: '수취인' }
    }),
    createdAt: now
  };
}

function shippingOrder() {
  return {
    orderDraftId: 'odft_shipping_1',
    sourceInboundMessageId: 'inb_shipping_1',
    merchantId: 'merchant_mock',
    merchantStaffId: null,
    customerId: 'cust_mock',
    sourceChannel: 'EMAIL',
    businessChannel: 'PACKAGING_CUSTOM',
    rawText: '테스트 주문',
    orderDraftStatus: 'SUBMITTED',
    deliveryRecipientName: '홍길동',
    deliveryRecipientPhone: '010-0000-0000',
    deliveryPostalCode: '12345',
    deliveryAddress: '서울시',
    deliveryAddressDetail: '1층',
    deliveryRequestMemo: '',
    deliveryMemo: '',
    courierMemo: '',
    itemCandidates: [
      {
        orderDraftItemCandidateId: 'item_1',
        itemName: '박스',
        itemSpec: '대',
        itemQuantity: '2',
        itemUnit: '개',
        matchedUnitPrice: '1000',
        lineMemo: ''
      }
    ],
    createdAt: now,
    updatedAt: now
  };
}

function orderDraft() {
  return {
    ...shippingOrder(),
    orderDraftId: 'odft_review_1',
    sourceInboundMessageId: 'inb_review_1',
    orderDraftStatus: 'REVIEWING'
  };
}

function shippingAuditLogs() {
  return [
    {
      auditLogId: 'aud_submit_1',
      merchantId: 'merchant_mock',
      actorId: 'shipmate',
      actionType: 'ORDER_DRAFT_SUBMITTED',
      targetType: 'ORDER_DRAFT',
      targetId: 'odft_shipping_1',
      summary: '1차 제출',
      auditMetadataJson: JSON.stringify({
        itemCandidates: [
          { itemName: '박스', itemSpec: '대', itemQuantity: '1', itemUnit: '개', matchedUnitPrice: '1000' }
        ]
      }),
      createdAt: '2026-06-18T05:00:00.000Z'
    },
    {
      auditLogId: 'aud_submit_2',
      merchantId: 'merchant_mock',
      actorId: 'shipmate',
      actionType: 'ORDER_DRAFT_SUBMITTED',
      targetType: 'ORDER_DRAFT',
      targetId: 'odft_shipping_1',
      summary: '2차 제출',
      auditMetadataJson: JSON.stringify({
        itemCandidates: [
          { itemName: '박스', itemSpec: '대', itemQuantity: '2', itemUnit: '개', matchedUnitPrice: '1000' }
        ]
      }),
      createdAt: '2026-06-18T05:10:00.000Z'
    }
  ];
}

function merchantDashboard() {
  return {
    inboundMessageCount: 0,
    orderDraftCount: 0,
    submittedShippingOrderCount: 1,
    errorLogCount: 0,
    orderDraftStatusCounts: {},
    sourceChannelCounts: {},
    errorCodeCounts: {},
    gmailPollingEnabled: true,
    gmailPollingDelayMs: 300000,
    gmailPollingAccountCount: 0,
    gmailPollingRecentRunCount: 0,
    gmailPollingRecentFailureCount: 0,
    gmailPollingRecentSkippedNonOrderCount: 0,
    gmailPollingLatestFailureMessage: null,
    gmailPollingLatestFailureInboundMessageId: null,
    gmailPollingFailureCodeCounts: {}
  };
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});

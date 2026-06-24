import { useCallback, useState } from 'react';
import type { Dispatch, SetStateAction } from 'react';
import type { FormInstance } from 'antd';
import { Descriptions, message, Modal, Space, Typography } from 'antd';

type OrderDraftItemCandidate = {
  orderDraftItemCandidateId?: string | null;
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

type OrderDraftReviewFormValues = {
  itemCandidates?: OrderDraftItemCandidate[];
  deliveryRecipientName?: string;
  deliveryRecipientPhone?: string;
  deliveryPostalCode?: string;
  deliveryAddress?: string;
  deliveryAddressDetail?: string;
  deliveryRequestMemo?: string;
  deliveryMemo?: string;
  courierMemo?: string;
};

type OrderDraft = {
  orderDraftId: string;
  itemCandidates: OrderDraftItemCandidate[];
};

type CustomerPriceItem = {
  itemNameKeyword?: string | null;
  itemSpecKeyword?: string | null;
  itemUnit?: string | null;
  unitPrice?: number | null;
};

type Customer = {
  defaultRecipientName: string | null;
  defaultRecipientPhone: string | null;
  defaultPostalCode: string | null;
  defaultAddress: string | null;
  defaultAddressDetail: string | null;
  priceItems: CustomerPriceItem[];
};

type ReviewActionsOptions<TOrderDraft extends OrderDraft, TCustomer extends Customer> = {
  reviewForm: FormInstance;
  selectedOrderDraft: TOrderDraft | null;
  selectedOrderDraftCustomer: TCustomer | null;
  requestJson: <T>(path: string, init?: RequestInit) => Promise<T>;
  setError: (error: string | null) => void;
  setOrderDrafts: Dispatch<SetStateAction<TOrderDraft[]>>;
  setShippingOrders: Dispatch<SetStateAction<TOrderDraft[]>>;
  setSelectedOrderDraftId: (orderDraftId: string | null) => void;
  updateSelectedShippingOrderId: (orderDraftId: string | null) => void;
  loadAuditLogs: () => Promise<void>;
};

export function useOrderDraftReviewActions<TOrderDraft extends OrderDraft, TCustomer extends Customer>({
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
}: ReviewActionsOptions<TOrderDraft, TCustomer>) {
  const [savingOrderDraftReview, setSavingOrderDraftReview] = useState(false);
  const [submittingOrderDraft, setSubmittingOrderDraft] = useState(false);

  const saveOrderDraftReview = useCallback(
    async (values: OrderDraftReviewFormValues, options?: { silent?: boolean }) => {
      if (!selectedOrderDraft) {
        message.warning('저장할 출고표 초안을 선택하세요.');
        return null;
      }

      setSavingOrderDraftReview(true);
      setError(null);

      try {
        const updated = await requestJson<TOrderDraft>(`/api/order-drafts/${selectedOrderDraft.orderDraftId}/review`, {
          method: 'PUT',
          body: JSON.stringify({
            itemCandidates: (values.itemCandidates ?? []).map((itemCandidate) => ({
              orderDraftItemCandidateId: trimToNull(itemCandidate.orderDraftItemCandidateId),
              itemName: trimToNull(itemCandidate.itemName),
              itemSpec: trimToNull(itemCandidate.itemSpec),
              itemQuantity: trimToNull(itemCandidate.itemQuantity),
              itemUnit: trimToNull(itemCandidate.itemUnit),
              matchedUnitPrice: itemCandidate.matchedUnitPrice ?? null,
              packagingWidth: trimToNull(itemCandidate.packagingWidth),
              packagingDepth: trimToNull(itemCandidate.packagingDepth),
              packagingHeight: trimToNull(itemCandidate.packagingHeight),
              materialName: trimToNull(itemCandidate.materialName),
              printRequired: itemCandidate.printRequired ?? null,
              designFileProvided: itemCandidate.designFileProvided ?? null,
              productionQuantity: trimToNull(itemCandidate.productionQuantity),
              requestedDueDate: trimToNull(itemCandidate.requestedDueDate),
              sampleRequested: itemCandidate.sampleRequested ?? null,
              customProductType: trimToNull(itemCandidate.customProductType),
              imprintMethod: trimToNull(itemCandidate.imprintMethod),
              itemColor: trimToNull(itemCandidate.itemColor),
              customOptionText: trimToNull(itemCandidate.customOptionText),
              proofConfirmed: itemCandidate.proofConfirmed ?? null,
              productionConfirmed: itemCandidate.productionConfirmed ?? null,
              lineMemo: trimToNull(itemCandidate.lineMemo)
            })),
            deliveryRecipientName: trimToNull(values.deliveryRecipientName),
            deliveryRecipientPhone: trimToNull(values.deliveryRecipientPhone),
            deliveryPostalCode: trimToNull(values.deliveryPostalCode),
            deliveryAddress: trimToNull(values.deliveryAddress),
            deliveryAddressDetail: trimToNull(values.deliveryAddressDetail),
            deliveryRequestMemo: trimToNull(values.deliveryRequestMemo),
            deliveryMemo: trimToNull(values.deliveryMemo),
            courierMemo: trimToNull(values.courierMemo)
          })
        });
        setOrderDrafts((currentRows) =>
          currentRows.map((row) => (row.orderDraftId === updated.orderDraftId ? updated : row))
        );
        setSelectedOrderDraftId(updated.orderDraftId);
        if (!options?.silent) {
          message.success('초안 검수 내용이 저장되었습니다.');
        }
        return updated;
      } catch (cause) {
        const description = errorMessage(cause, '초안 검수 내용을 저장하지 못했습니다.');
        setError(description);
        message.error(description);
        return null;
      } finally {
        setSavingOrderDraftReview(false);
      }
    },
    [requestJson, selectedOrderDraft, setError, setOrderDrafts, setSelectedOrderDraftId]
  );

  const submitOrderDraft = useCallback(async () => {
    if (!selectedOrderDraft) {
      message.warning('제출할 출고표 초안을 선택하세요.');
      return;
    }

    setSubmittingOrderDraft(true);
    setError(null);

    try {
      const values = await reviewForm.validateFields();
      const saved = await saveOrderDraftReview(values, { silent: true });
      if (!saved) {
        return;
      }

      const submitted = await requestJson<TOrderDraft>(`/api/order-drafts/${saved.orderDraftId}/submit`, {
        method: 'POST'
      });
      setOrderDrafts((currentRows) =>
        currentRows.map((row) => (row.orderDraftId === submitted.orderDraftId ? submitted : row))
      );
      setShippingOrders((currentRows) => [
        submitted,
        ...currentRows.filter((row) => row.orderDraftId !== submitted.orderDraftId)
      ]);
      setSelectedOrderDraftId(submitted.orderDraftId);
      updateSelectedShippingOrderId(submitted.orderDraftId);
      await loadAuditLogs();
      message.success('출고표가 제출되었습니다.');
    } catch (cause) {
      const description = errorMessage(cause, '출고표를 제출하지 못했습니다.');
      setError(description);
      message.error(description);
    } finally {
      setSubmittingOrderDraft(false);
    }
  }, [
    loadAuditLogs,
    requestJson,
    reviewForm,
    saveOrderDraftReview,
    selectedOrderDraft,
    setError,
    setOrderDrafts,
    setSelectedOrderDraftId,
    setShippingOrders,
    updateSelectedShippingOrderId
  ]);

  const applySelectedCustomerDefaults = useCallback(() => {
    if (!selectedOrderDraftCustomer) {
      message.warning('연결된 고객 기본 배송지가 없습니다.');
      return;
    }
    const nextValues = {
      deliveryRecipientName: selectedOrderDraftCustomer.defaultRecipientName ?? '',
      deliveryRecipientPhone: selectedOrderDraftCustomer.defaultRecipientPhone ?? '',
      deliveryPostalCode: selectedOrderDraftCustomer.defaultPostalCode ?? '',
      deliveryAddress: selectedOrderDraftCustomer.defaultAddress ?? '',
      deliveryAddressDetail: selectedOrderDraftCustomer.defaultAddressDetail ?? ''
    };
    const currentValues = reviewForm.getFieldsValue([
      'deliveryRecipientName',
      'deliveryRecipientPhone',
      'deliveryPostalCode',
      'deliveryAddress',
      'deliveryAddressDetail'
    ]);
    Modal.confirm({
      title: '고객 기본 배송지 반영',
      content: (
        <Descriptions size="small" column={1}>
          <Descriptions.Item label="수취인">
            {comparePreviewText(currentValues.deliveryRecipientName, nextValues.deliveryRecipientName)}
          </Descriptions.Item>
          <Descriptions.Item label="연락처">
            {comparePreviewText(currentValues.deliveryRecipientPhone, nextValues.deliveryRecipientPhone)}
          </Descriptions.Item>
          <Descriptions.Item label="우편번호">
            {comparePreviewText(currentValues.deliveryPostalCode, nextValues.deliveryPostalCode)}
          </Descriptions.Item>
          <Descriptions.Item label="주소">
            {comparePreviewText(
              [currentValues.deliveryAddress, currentValues.deliveryAddressDetail].filter(Boolean).join(' '),
              [nextValues.deliveryAddress, nextValues.deliveryAddressDetail].filter(Boolean).join(' ')
            )}
          </Descriptions.Item>
        </Descriptions>
      ),
      okText: '반영',
      cancelText: '취소',
      onOk: () => {
        reviewForm.setFieldsValue(nextValues);
        message.success('고객 기본 배송지를 반영했습니다.');
      }
    });
  }, [reviewForm, selectedOrderDraftCustomer]);

  const applyCustomerPriceCandidates = useCallback(() => {
    if (!selectedOrderDraftCustomer?.priceItems.length) {
      message.warning('연결된 고객 단가표가 없습니다.');
      return;
    }
    const currentItems = reviewForm.getFieldValue('itemCandidates') ?? [];
    const matchedItems: { key: string; itemName: string; currentUnitPrice: number | null; nextUnitPrice: number; keywordText: string }[] = [];
    const unmatchedItems: string[] = [];
    const nextItems = currentItems.map((itemCandidate: OrderDraftItemCandidate) => {
      const matchedPrice = findCustomerPriceMatch(itemCandidate, selectedOrderDraftCustomer.priceItems);
      if (matchedPrice?.unitPrice != null) {
        matchedItems.push({
          key: [
            itemCandidate.orderDraftItemCandidateId,
            itemCandidate.itemName,
            itemCandidate.itemSpec,
            matchedPrice.unitPrice
          ].filter(Boolean).join(':') || String(matchedItems.length),
          itemName: itemCandidate.itemName || '품목',
          currentUnitPrice: itemCandidate.matchedUnitPrice ?? null,
          nextUnitPrice: matchedPrice.unitPrice,
          keywordText: priceMatchKeywordText(matchedPrice)
        });
      } else {
        unmatchedItems.push(itemCandidate.itemName || '이름 없는 품목');
      }
      return matchedPrice?.unitPrice == null
        ? itemCandidate
        : {
            ...itemCandidate,
            itemUnit: itemCandidate.itemUnit || matchedPrice.itemUnit || '',
            matchedUnitPrice: matchedPrice.unitPrice
          };
    });
    if (!matchedItems.length) {
      message.warning('반영할 단가표 후보가 없습니다.');
      return;
    }
    Modal.confirm({
      title: '단가표 후보 반영',
      content: (
        <Space direction="vertical" size={4}>
          <Typography.Text strong>매칭 품목</Typography.Text>
          {matchedItems.map((item) => (
            <Space wrap key={item.key}>
              <Typography.Text>{item.itemName}</Typography.Text>
              <Typography.Text type="secondary">현재 단가: {formatCurrency(item.currentUnitPrice)}</Typography.Text>
              <Typography.Text mark={item.currentUnitPrice !== item.nextUnitPrice}>
                반영 단가: {formatCurrency(item.nextUnitPrice)}
              </Typography.Text>
              <Typography.Text type="secondary">키워드: {item.keywordText}</Typography.Text>
            </Space>
          ))}
          {unmatchedItems.length ? (
            <>
              <Typography.Text strong>미매칭 품목</Typography.Text>
              {unmatchedItems.map((item) => <Typography.Text type="secondary" key={item}>{item}</Typography.Text>)}
            </>
          ) : null}
        </Space>
      ),
      okText: '반영',
      cancelText: '취소',
      onOk: () => {
        reviewForm.setFieldValue('itemCandidates', nextItems);
        message.success('고객 단가표 후보를 반영했습니다.');
      }
    });
  }, [reviewForm, selectedOrderDraftCustomer]);

  return {
    savingOrderDraftReview,
    submittingOrderDraft,
    saveOrderDraftReview,
    submitOrderDraft,
    applySelectedCustomerDefaults,
    applyCustomerPriceCandidates
  };
}

function trimToNull(value?: string | null) {
  if (!value || !value.trim()) {
    return null;
  }
  return value.trim();
}

function findCustomerPriceMatch(
  itemCandidate: OrderDraftItemCandidate,
  priceItems: CustomerPriceItem[]
) {
  const itemName = itemCandidate.itemName?.trim().toLowerCase() ?? '';
  const itemSpec = itemCandidate.itemSpec?.trim().toLowerCase() ?? '';
  const itemUnit = itemCandidate.itemUnit?.trim().toLowerCase() ?? '';
  return priceItems.find((priceItem) => {
    const nameKeyword = priceItem.itemNameKeyword?.trim().toLowerCase();
    const specKeyword = priceItem.itemSpecKeyword?.trim().toLowerCase();
    const unitKeyword = priceItem.itemUnit?.trim().toLowerCase();
    if (priceItem.unitPrice == null || (!nameKeyword && !specKeyword && !unitKeyword)) {
      return false;
    }
    return (!nameKeyword || itemName.includes(nameKeyword))
      && (!specKeyword || itemSpec.includes(specKeyword))
      && (!unitKeyword || !itemUnit || itemUnit === unitKeyword);
  }) ?? null;
}

function comparePreviewText(currentValue?: string | null, nextValue?: string | null) {
  const currentText = currentValue || '-';
  const nextText = nextValue || '-';
  if (currentText === nextText) {
    return <Typography.Text>{nextText}</Typography.Text>;
  }
  return (
    <Space direction="vertical" size={0}>
      <Typography.Text type="secondary">현재: {currentText}</Typography.Text>
      <Typography.Text mark>반영: {nextText}</Typography.Text>
    </Space>
  );
}

function priceMatchKeywordText(priceItem: CustomerPriceItem) {
  return [
    priceItem.itemNameKeyword ? `품목 ${priceItem.itemNameKeyword}` : null,
    priceItem.itemSpecKeyword ? `규격 ${priceItem.itemSpecKeyword}` : null,
    priceItem.itemUnit ? `단위 ${priceItem.itemUnit}` : null
  ].filter(Boolean).join(', ') || '-';
}

function formatCurrency(value?: number | null) {
  if (value == null) {
    return '-';
  }
  return `${value.toLocaleString()}원`;
}

function errorMessage(cause: unknown, fallback: string) {
  return cause instanceof Error ? cause.message : fallback;
}

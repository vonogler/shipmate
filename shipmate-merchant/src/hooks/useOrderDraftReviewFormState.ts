import { useEffect } from 'react';
import type { FormInstance } from 'antd';

type OrderDraftReviewFormShape = {
  itemCandidates?: unknown[];
  deliveryRecipientName?: string | null;
  deliveryRecipientPhone?: string | null;
  deliveryPostalCode?: string | null;
  deliveryAddress?: string | null;
  deliveryAddressDetail?: string | null;
  deliveryRequestMemo?: string | null;
  deliveryMemo?: string | null;
  courierMemo?: string | null;
};

type ReviewableOrderDraft = Partial<OrderDraftReviewFormShape> & {
  itemCandidates?: unknown[];
};

export function useOrderDraftReviewFormState(
  form: FormInstance,
  selectedOrderDraft: ReviewableOrderDraft | null,
  emptyItemCandidate: () => unknown
) {
  useEffect(() => {
    if (!selectedOrderDraft) {
      form.resetFields();
      return;
    }

    form.setFieldsValue({
      itemCandidates: selectedOrderDraft.itemCandidates?.length
        ? selectedOrderDraft.itemCandidates
        : [emptyItemCandidate()],
      deliveryRecipientName: selectedOrderDraft.deliveryRecipientName ?? '',
      deliveryRecipientPhone: selectedOrderDraft.deliveryRecipientPhone ?? '',
      deliveryPostalCode: selectedOrderDraft.deliveryPostalCode ?? '',
      deliveryAddress: selectedOrderDraft.deliveryAddress ?? '',
      deliveryAddressDetail: selectedOrderDraft.deliveryAddressDetail ?? '',
      deliveryRequestMemo: selectedOrderDraft.deliveryRequestMemo ?? '',
      deliveryMemo: selectedOrderDraft.deliveryMemo ?? '',
      courierMemo: selectedOrderDraft.courierMemo ?? ''
    });
  }, [emptyItemCandidate, form, selectedOrderDraft]);
}

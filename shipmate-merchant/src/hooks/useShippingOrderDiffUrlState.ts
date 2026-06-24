import { useCallback, useEffect, useState } from 'react';

export type ShippingOrderDiffSectionAnchor = 'item-diff' | null;

export const shippingOrderDiffFieldOptions = ['ALL', '최초 제출', '품목', '품목명', '규격', '수량', '단가', '품목 순서'];

export function useShippingOrderDiffUrlState() {
  const [fieldFilter, setFieldFilter] = useState(() => initialShippingOrderDiffFieldFilter());
  const [sectionAnchor, setSectionAnchor] = useState<ShippingOrderDiffSectionAnchor>(
    () => initialShippingOrderDiffSectionAnchor()
  );

  const updateFieldFilter = useCallback((nextFieldFilter: string) => {
    localStorage.setItem('shipmate_shipping_order_diff_field_filter', nextFieldFilter);
    const url = new URL(window.location.href);
    if (nextFieldFilter === 'ALL') {
      url.searchParams.delete('shippingDiffField');
      url.searchParams.delete('shippingDiffSection');
      setSectionAnchor(null);
    } else {
      url.searchParams.set('shippingDiffField', nextFieldFilter);
      url.searchParams.set('shippingDiffSection', 'item-diff');
      setSectionAnchor('item-diff');
    }
    window.history.replaceState(null, '', `${url.pathname}${url.search}${url.hash}`);
    setFieldFilter(nextFieldFilter);
  }, []);

  const preserveShippingDiffQuery = useCallback((url: URL) => {
    if (fieldFilter !== 'ALL') {
      url.searchParams.set('shippingDiffField', fieldFilter);
      url.searchParams.set('shippingDiffSection', 'item-diff');
    }
  }, [fieldFilter]);

  return {
    fieldFilter,
    sectionAnchor,
    updateFieldFilter,
    preserveShippingDiffQuery
  };
}

export function initialNavigationKey() {
  const queryValue = new URLSearchParams(window.location.search).get('shippingDiffField');
  return isShippingOrderDiffFieldOption(queryValue) ? 'shipping' : 'dashboard';
}

export function useShippingOrderDiffSectionScroll(
  activeNavigationKey: string,
  sectionAnchor: ShippingOrderDiffSectionAnchor,
  dependencyCount: number
) {
  useEffect(() => {
    if (activeNavigationKey !== 'shipping' || sectionAnchor !== 'item-diff') {
      return;
    }
    const scrollToDiffSection = () => {
      document.getElementById('shipping-order-item-diff')?.scrollIntoView({ behavior: 'auto', block: 'start' });
    };
    window.requestAnimationFrame(scrollToDiffSection);
    const timeoutIds = [100, 300, 700, 1200, 1800, 2400].map((delayMs) => window.setTimeout(scrollToDiffSection, delayMs));
    return () => timeoutIds.forEach((timeoutId) => window.clearTimeout(timeoutId));
  }, [activeNavigationKey, dependencyCount, sectionAnchor]);
}

function initialShippingOrderDiffFieldFilter(): string {
  const queryValue = new URLSearchParams(window.location.search).get('shippingDiffField');
  if (isShippingOrderDiffFieldOption(queryValue)) {
    return queryValue;
  }
  const savedValue = localStorage.getItem('shipmate_shipping_order_diff_field_filter') ?? 'ALL';
  return isShippingOrderDiffFieldOption(savedValue) ? savedValue : 'ALL';
}

function initialShippingOrderDiffSectionAnchor(): ShippingOrderDiffSectionAnchor {
  return new URLSearchParams(window.location.search).get('shippingDiffSection') === 'item-diff'
    ? 'item-diff'
    : null;
}

function isShippingOrderDiffFieldOption(value: string | null): value is string {
  return value !== null && shippingOrderDiffFieldOptions.includes(value);
}

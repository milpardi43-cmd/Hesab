import {
  dateFa, escapeHtml, money, num, parseAmount, quantity, setPersianDigits, todayFa, todayIso
} from './format.js';
import { icon, mobileNav, navItems } from './icons.js';
import {
  addExpense, addPayment, addPurchase, addReminder, addSale, adjustCash, contactTransactions,
  dashboardStats, exportJson, getAccount, getContact, getProduct, importJson, load, productMovements,
  removeReminder, reset, saveContact, saveProduct, save, setSettings, state, toggleReminder, transfer
} from './store.js';
import { meta, txRow, txTitle, viewFor } from './views.js';

const viewEl = document.querySelector('#view');
const modalRoot = document.querySelector('#modal-root');
const toastEl = document.querySelector('#toast');
const pageTitle = document.querySelector('#page-title');
const pageSubtitle = document.querySelector('#page-subtitle');
const dateLabel = document.querySelector('#top-date');
const navEls = [...document.querySelectorAll('[data-nav]')];

let currentView = 'dashboard';
let toastTimer = null;

load();
setPersianDigits(state.settings.persianDigits);

function applySettings() {
  setPersianDigits(state.settings.persianDigits);
  document.documentElement.style.setProperty('--font-scale', String(state.settings.fontSize));
}

function route() {
  const hash = location.hash.replace('#', '').trim();
  const next = navItems.some((item) => item.id === hash) ? hash : 'dashboard';
  currentView = next;
  render();
  window.scrollTo({ top: 0, behavior: 'instant' in window ? 'instant' : 'auto' });
}

function render() {
  applySettings();
  viewEl.innerHTML = viewFor(currentView);
  const item = navItems.find((entry) => entry.id === currentView);
  pageTitle.textContent = state.business.name;
  pageSubtitle.textContent = 'پنل مدیریت حسابیار';
  dateLabel.textContent = todayFa();
  navEls.forEach((element) => {
    const active = element.dataset.nav === currentView;
    element.classList.toggle('is-active', active);
    if (active) element.setAttribute('aria-current', 'page');
    else element.removeAttribute('aria-current');
  });
  document.title = `${item?.label ?? 'داشبورد'} — حسابیار`;
}

function subtitleFor(view) {
  switch (view) {
    case 'sales': return 'فاکتورهای فروش و مانده مشتریان';
    case 'purchases': return 'خرید کالا و بدهی تأمین‌کنندگان';
    case 'expenses': return 'هزینه‌های عملیاتی فروشگاه';
    case 'products': return 'موجودی و گردش انبار';
    case 'contacts': return 'مشتریان و تأمین‌کنندگان';
    case 'accounts': return 'صندوق، بانک و انتقال وجه';
    case 'reports': return 'گزارش فروش، سود و مانده‌ها';
    case 'reminders': return 'سررسیدها و کارهای مالی';
    case 'settings': return 'تنظیمات و داده‌های محلی';
    default: return 'نمای کلی فروشگاه شما';
  }
}

function toast(message, tone = 'success') {
  clearTimeout(toastTimer);
  toastEl.innerHTML = `${icon(tone === 'error' ? 'alert' : 'check')}<span>${escapeHtml(message)}</span>`;
  toastEl.className = `toast is-visible toast-${tone}`;
  toastTimer = setTimeout(() => {
    toastEl.className = 'toast';
  }, 3400);
}

function closeModal() {
  modalRoot.innerHTML = '';
  document.body.classList.remove('modal-open');
}

function openModal({ title, subtitle = '', body, footer = '', wide = false }) {
  modalRoot.innerHTML = `
    <div class="modal-backdrop" data-action="close-modal">
      <section class="modal ${wide ? 'modal-wide' : ''}" role="dialog" aria-modal="true" aria-labelledby="modal-title">
        <header class="modal-head">
          <div>
            <h2 id="modal-title">${title}</h2>
            ${subtitle ? `<p>${subtitle}</p>` : ''}
          </div>
          <button class="icon-btn" data-action="close-modal" aria-label="بستن">${icon('close')}</button>
        </header>
        <div class="modal-body">${body}</div>
        ${footer ? `<footer class="modal-foot">${footer}</footer>` : ''}
      </section>
    </div>`;
  document.body.classList.add('modal-open');
  const focusTarget = modalRoot.querySelector('input:not([type="hidden"]), select, textarea, button');
  focusTarget?.focus({ preventScroll: true });
}

function formError(form, message) {
  let box = form.querySelector('.form-error');
  if (!box) {
    box = document.createElement('p');
    box.className = 'form-error';
    form.prepend(box);
  }
  box.hidden = false;
  box.textContent = message;
  box.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
}

function selectOptions(items, selected, placeholder = 'انتخاب کنید') {
  return `<option value="">${placeholder}</option>${items.map((item) => `<option value="${item.id}" ${selected === item.id ? 'selected' : ''}>${escapeHtml(item.name ?? item.title)}</option>`).join('')}`;
}

function accountOptions(selected = 'cash') {
  return state.accounts.map((account) => `<option value="${account.id}" ${selected === account.id ? 'selected' : ''}>${escapeHtml(account.title)} — ${money(account.balance)}</option>`).join('');
}

function customerOptions(selected = null) {
  return selectOptions(state.contacts.filter((contact) => contact.type !== 'supplier'), selected, 'انتخاب مشتری');
}

function supplierOptions(selected = null) {
  return selectOptions(state.contacts.filter((contact) => contact.type !== 'customer'), selected, 'انتخاب تأمین‌کننده');
}

function todayLabel() {
  return dateFa(todayIso());
}

// ---------------------------------------------------------------- Modals

function openSaleModal(presetContact = null) {
  const productRows = state.products.map((product) => `
    <div class="pick-row" data-search-text="${escapeHtml(product.name + ' ' + product.category)}">
      <div class="pick-info">
        <strong>${escapeHtml(product.name)}</strong>
        <small>${money(product.sellingPrice)} · موجودی ${quantity(product.stock)}</small>
      </div>
      <div class="qty-control">
        <button type="button" class="qty-btn" data-action="qty-minus" data-target="sale-qty-${product.id}">−</button>
        <input id="sale-qty-${product.id}" type="number" min="0" step="1" value="0" data-sale-qty data-id="${product.id}" aria-label="تعداد ${escapeHtml(product.name)}">
        <button type="button" class="qty-btn" data-action="qty-plus" data-target="sale-qty-${product.id}">+</button>
      </div>
    </div>`).join('');

  openModal({
    title: 'ثبت فاکتور فروش',
    subtitle: `تاریخ ${todayLabel()} · مبلغ به تومان`,
    wide: true,
    body: `
      <form id="sale-form" class="modal-form">
        <div class="form-error" hidden></div>
        <div class="sale-layout">
          <div class="sale-products">
            <div class="mini-search"><label class="search">${icon('search')}<input type="search" placeholder="جست‌وجوی کالا..." data-modal-search></label></div>
            <div class="pick-list">${productRows}</div>
          </div>
          <div class="sale-side">
            <div class="form-grid">
              <label>روش پرداخت
                <select name="mode" id="sale-mode">
                  <option value="cash">نقدی</option>
                  <option value="credit">نسیه</option>
                  <option value="mixed">ترکیبی</option>
                </select>
              </label>
              <label id="sale-contact-row">مشتری
                <select name="contactId">${customerOptions(presetContact)}</select>
              </label>
              <label>حساب دریافت
                <select name="accountId">${accountOptions('cash')}</select>
              </label>
              <label>تخفیف فاکتور (تومان)
                <input name="discount" inputmode="numeric" value="0" data-amount>
              </label>
              <label id="sale-paid-row" hidden>مبلغ پرداخت‌شده
                <input name="paid" inputmode="numeric" value="0" data-amount>
              </label>
              <label class="span-2">توضیحات
                <input name="note" placeholder="اختیاری">
              </label>
            </div>
            <div class="sale-summary">
              <div><span>جمع کالاها</span><strong id="sale-subtotal">${money(0)}</strong></div>
              <div><span>تخفیف</span><strong id="sale-discount">${money(0)}</strong></div>
              <div class="total"><span>مبلغ فاکتور</span><strong id="sale-total">${money(0)}</strong></div>
              <div><span>مانده نسیه</span><strong id="sale-remaining">${money(0)}</strong></div>
            </div>
            <button class="btn btn-primary btn-block" type="submit">${icon('check')} ثبت فروش</button>
          </div>
        </div>
      </form>`,
    footer: ''
  });

  const form = modalRoot.querySelector('#sale-form');
  const mode = form.querySelector('#sale-mode');
  const paidRow = form.querySelector('#sale-paid-row');
  const contactRow = form.querySelector('#sale-contact-row');

  const syncMode = () => {
    paidRow.hidden = mode.value !== 'mixed';
    contactRow.hidden = mode.value === 'cash';
  };
  mode.addEventListener('change', () => { syncMode(); updateSaleSummary(); });
  syncMode();

  form.addEventListener('input', updateSaleSummary);
  form.addEventListener('submit', (event) => {
    event.preventDefault();
    try {
      const items = [...form.querySelectorAll('[data-sale-qty]')]
        .map((input) => ({ productId: input.dataset.id, qty: Number(input.value) || 0 }))
        .filter((item) => item.qty > 0)
        .map((item) => {
          const product = getProduct(item.productId);
          return { productId: item.productId, qty: item.qty, price: product.sellingPrice, name: product.name };
        });
      if (!items.length) throw new Error('حداقل یک کالا با تعداد مشخص انتخاب کنید');
      const data = new FormData(form);
      addSale({
        items,
        discount: parseAmount(data.get('discount')),
        mode: data.get('mode'),
        paid: parseAmount(data.get('paid')),
        contactId: data.get('contactId') || null,
        accountId: data.get('accountId') || 'cash',
        note: String(data.get('note') ?? '').trim()
      });
      closeModal();
      render();
      toast('فاکتور فروش ثبت شد و موجودی به‌روزرسانی شد');
    } catch (error) {
      formError(form, error.message);
    }
  });
}

function updateSaleSummary() {
  const form = modalRoot.querySelector('#sale-form');
  if (!form) return;
  const subtotal = [...form.querySelectorAll('[data-sale-qty]')].reduce((sum, input) => {
    const product = getProduct(input.dataset.id);
    return sum + (Number(input.value) || 0) * product.sellingPrice;
  }, 0);
  const discount = Math.min(subtotal, parseAmount(new FormData(form).get('discount')));
  const total = subtotal - discount;
  const mode = form.querySelector('#sale-mode').value;
  const paid = mode === 'cash' ? total : mode === 'credit' ? 0 : Math.min(total, parseAmount(new FormData(form).get('paid')));
  form.querySelector('#sale-subtotal').textContent = money(subtotal);
  form.querySelector('#sale-discount').textContent = money(discount);
  form.querySelector('#sale-total').textContent = money(total);
  form.querySelector('#sale-remaining').textContent = money(total - paid);
}

function openPurchaseModal() {
  const rows = state.products.map((product) => `
    <div class="pick-row" data-search-text="${escapeHtml(product.name)}">
      <div class="pick-info"><strong>${escapeHtml(product.name)}</strong><small>موجودی ${quantity(product.stock)} · بهای فعلی ${money(product.purchasePrice)}</small></div>
      <div class="purchase-inputs">
        <label>تعداد<input type="number" min="0" step="1" value="0" data-purchase-qty data-id="${product.id}" aria-label="تعداد ${escapeHtml(product.name)}"></label>
        <label>قیمت خرید<input inputmode="numeric" value="${product.purchasePrice}" data-purchase-price data-id="${product.id}" data-amount aria-label="قیمت ${escapeHtml(product.name)}"></label>
      </div>
    </div>`).join('');
  openModal({
    title: 'ثبت فاکتور خرید',
    subtitle: `تاریخ ${todayLabel()} · افزایش موجودی و بدهی تأمین‌کننده`,
    wide: true,
    body: `
      <form id="purchase-form" class="modal-form">
        <div class="form-error" hidden></div>
        <div class="sale-layout">
          <div class="sale-products">
            <div class="mini-search"><label class="search">${icon('search')}<input type="search" placeholder="جست‌وجوی کالا..." data-modal-search></label></div>
            <div class="pick-list">${rows}</div>
          </div>
          <div class="sale-side">
            <div class="form-grid">
              <label>تأمین‌کننده
                <select name="contactId">${supplierOptions()}</select>
              </label>
              <label>حساب پرداخت
                <select name="accountId">${accountOptions('bank')}</select>
              </label>
              <label>کرایه حمل (تومان)
                <input name="freight" inputmode="numeric" value="0" data-amount>
              </label>
              <label>تخفیف (تومان)
                <input name="discount" inputmode="numeric" value="0" data-amount>
              </label>
              <label>مبلغ پرداخت‌شده (تومان)
                <input name="paid" inputmode="numeric" value="0" data-amount>
              </label>
              <label>توضیحات
                <input name="note" placeholder="اختیاری">
              </label>
            </div>
            <div class="sale-summary">
              <div><span>جمع کالاها</span><strong id="purchase-subtotal">${money(0)}</strong></div>
              <div><span>کرایه + تخفیف</span><strong id="purchase-extras">${money(0)}</strong></div>
              <div class="total"><span>مبلغ خرید</span><strong id="purchase-total">${money(0)}</strong></div>
              <div><span>مانده بدهی</span><strong id="purchase-remaining">${money(0)}</strong></div>
            </div>
            <button class="btn btn-primary btn-block" type="submit">${icon('check')} ثبت خرید</button>
          </div>
        </div>
      </form>`
  });
  const form = modalRoot.querySelector('#purchase-form');
  form.addEventListener('input', () => updatePurchaseSummary());
  updatePurchaseSummary();
  form.addEventListener('submit', (event) => {
    event.preventDefault();
    try {
      const items = [...form.querySelectorAll('[data-purchase-qty]')].map((input) => {
        const priceInput = form.querySelector(`[data-purchase-price][data-id="${input.dataset.id}"]`);
        return { productId: input.dataset.id, qty: Number(input.value) || 0, price: parseAmount(priceInput.value) };
      }).filter((item) => item.qty > 0);
      if (!items.length) throw new Error('حداقل یک کالا با تعداد مشخص اضافه کنید');
      const data = new FormData(form);
      addPurchase({
        items,
        discount: parseAmount(data.get('discount')),
        freight: parseAmount(data.get('freight')),
        paid: parseAmount(data.get('paid')),
        contactId: data.get('contactId') || null,
        accountId: data.get('accountId') || 'bank',
        note: String(data.get('note') ?? '').trim()
      });
      closeModal();
      render();
      toast('فاکتور خرید ثبت شد و میانگین بهای تمام‌شده به‌روز شد');
    } catch (error) {
      formError(form, error.message);
    }
  });
}

function updatePurchaseSummary() {
  const form = modalRoot.querySelector('#purchase-form');
  if (!form) return;
  const subtotal = [...form.querySelectorAll('[data-purchase-qty]')].reduce((sum, input) => {
    const price = parseAmount(form.querySelector(`[data-purchase-price][data-id="${input.dataset.id}"]`).value);
    return sum + (Number(input.value) || 0) * price;
  }, 0);
  const data = new FormData(form);
  const freight = parseAmount(data.get('freight'));
  const discount = Math.min(subtotal + freight, parseAmount(data.get('discount')));
  const total = subtotal + freight - discount;
  const paid = Math.min(total, parseAmount(data.get('paid')));
  form.querySelector('#purchase-subtotal').textContent = money(subtotal);
  form.querySelector('#purchase-extras').textContent = money(freight - discount);
  form.querySelector('#purchase-total').textContent = money(total);
  form.querySelector('#purchase-remaining').textContent = money(total - paid);
}

function openExpenseModal() {
  openModal({
    title: 'ثبت هزینه',
    subtitle: `تاریخ ${todayLabel()}`,
    body: `
      <form id="expense-form" class="modal-form">
        <div class="form-error" hidden></div>
        <div class="form-grid">
          <label class="span-2">عنوان هزینه<input name="title" placeholder="مثلاً قبض برق یا کرایه حمل" required></label>
          <label>دسته
            <select name="category">
              ${['اجاره', 'حمل‌ونقل', 'آب و برق و گاز', 'اینترنت و تلفن', 'تبلیغات', 'لوازم مصرفی', 'حقوق و دستمزد', 'متفرقه'].map((category) => `<option>${category}</option>`).join('')}
            </select>
          </label>
          <label>مبلغ (تومان)<input name="amount" inputmode="numeric" data-amount required></label>
          <label>حساب پرداخت<select name="accountId">${accountOptions('cash')}</select></label>
          <label>توضیحات<input name="note" placeholder="اختیاری"></label>
        </div>
        <div class="modal-actions"><button class="btn btn-primary" type="submit">${icon('check')} ثبت هزینه</button></div>
      </form>`
  });
  const form = modalRoot.querySelector('#expense-form');
  form.addEventListener('submit', (event) => {
    event.preventDefault();
    try {
      const data = new FormData(form);
      addExpense({
        title: String(data.get('title')),
        category: String(data.get('category')),
        amount: parseAmount(data.get('amount')),
        accountId: String(data.get('accountId')),
        note: String(data.get('note') ?? '').trim()
      });
      closeModal();
      render();
      toast('هزینه ثبت و از حساب کسر شد');
    } catch (error) {
      formError(form, error.message);
    }
  });
}

function openPaymentModal(mode = 'receive', contactId = null, presetAmount = null) {
  const isReceive = mode === 'receive';
  const contacts = state.contacts.filter((contact) => isReceive ? contact.type !== 'supplier' && contact.balance > 0 : contact.type !== 'customer' && contact.balance < 0);
  const selectedContact = contactId ?? contacts[0]?.id ?? null;
  const contact = getContact(selectedContact);
  const suggested = presetAmount ?? Math.abs(contact?.balance ?? 0);
  openModal({
    title: isReceive ? 'ثبت دریافت از مشتری' : 'پرداخت به تأمین‌کننده',
    subtitle: isReceive ? 'کاهش بدهی مشتری و افزایش موجودی حساب' : 'کاهش بدهی ما و موجودی حساب',
    body: `
      <form id="payment-form" class="modal-form">
        <div class="form-error" hidden></div>
        <div class="form-grid">
          <label class="span-2">${isReceive ? 'مشتری' : 'تأمین‌کننده'}
            <select name="contactId" id="payment-contact">
              ${selectOptions(contacts, selectedContact, isReceive ? 'انتخاب مشتری بدهکار' : 'انتخاب تأمین‌کننده')}
            </select>
          </label>
          <label class="span-2">مبلغ (تومان)<input name="amount" inputmode="numeric" value="${suggested || ''}" data-amount required></label>
          <label>حساب ${isReceive ? 'دریافت' : 'پرداخت'}<select name="accountId">${accountOptions(isReceive ? 'cash' : 'bank')}</select></label>
          <label>توضیحات<input name="note" placeholder="اختیاری"></label>
        </div>
        <p class="muted small" id="payment-hint">${contact ? `مانده فعلی ${escapeHtml(contact.name)}: ${money(Math.abs(contact.balance))}` : ''}</p>
        <div class="modal-actions"><button class="btn btn-primary" type="submit">${icon('check')} ${isReceive ? 'ثبت دریافت' : 'ثبت پرداخت'}</button></div>
      </form>`
  });
  const form = modalRoot.querySelector('#payment-form');
  const contactSelect = form.querySelector('#payment-contact');
  contactSelect.addEventListener('change', () => {
    const selected = getContact(contactSelect.value);
    form.querySelector('[name="amount"]').value = selected ? Math.abs(selected.balance) : '';
    form.querySelector('#payment-hint').textContent = selected ? `مانده فعلی ${selected.name}: ${money(Math.abs(selected.balance))}` : '';
  });
  form.addEventListener('submit', (event) => {
    event.preventDefault();
    try {
      const data = new FormData(form);
      addPayment({
        mode,
        contactId: String(data.get('contactId')),
        amount: parseAmount(data.get('amount')),
        accountId: String(data.get('accountId')),
        note: String(data.get('note') ?? '').trim()
      });
      closeModal();
      render();
      toast(isReceive ? 'دریافت ثبت شد' : 'پرداخت ثبت شد');
    } catch (error) {
      formError(form, error.message);
    }
  });
}

function openTransferModal(fromId = null) {
  openModal({
    title: 'انتقال بین حساب‌ها',
    subtitle: 'جابه‌جایی وجه بین صندوق، بانک و کارتخوان',
    body: `
      <form id="transfer-form" class="modal-form">
        <div class="form-error" hidden></div>
        <div class="form-grid">
          <label>از حساب<select name="fromId">${accountOptions(fromId ?? 'bank')}</select></label>
          <label>به حساب<select name="toId">${accountOptions('cash')}</select></label>
          <label class="span-2">مبلغ (تومان)<input name="amount" inputmode="numeric" data-amount required></label>
          <label class="span-2">توضیحات<input name="note" placeholder="اختیاری"></label>
        </div>
        <div class="modal-actions"><button class="btn btn-primary" type="submit">${icon('transfer')} ثبت انتقال</button></div>
      </form>`
  });
  const form = modalRoot.querySelector('#transfer-form');
  form.addEventListener('submit', (event) => {
    event.preventDefault();
    try {
      const data = new FormData(form);
      transfer({
        fromId: String(data.get('fromId')),
        toId: String(data.get('toId')),
        amount: parseAmount(data.get('amount')),
        note: String(data.get('note') ?? '').trim()
      });
      closeModal();
      render();
      toast('انتقال وجه با موفقیت ثبت شد');
    } catch (error) {
      formError(form, error.message);
    }
  });
}

function openAdjustModal() {
  openModal({
    title: 'اصلاح صندوق',
    subtitle: 'موجودی شمارش‌شده را وارد کنید؛ اختلاف به‌صورت خودکار ثبت می‌شود',
    body: `
      <form id="adjust-form" class="modal-form">
        <div class="form-error" hidden></div>
        <div class="form-grid">
          <label class="span-2">حساب<select name="accountId">${accountOptions('cash')}</select></label>
          <label class="span-2">موجودی شمارش‌شده (تومان)<input name="actual" inputmode="numeric" data-amount required></label>
          <label class="span-2">دلیل اختلاف<input name="reason" placeholder="مثلاً اشتباه در محاسبه"></label>
        </div>
        <div class="modal-actions"><button class="btn btn-primary" type="submit">${icon('sliders')} ثبت اصلاح</button></div>
      </form>`
  });
  const form = modalRoot.querySelector('#adjust-form');
  form.addEventListener('submit', (event) => {
    event.preventDefault();
    try {
      const data = new FormData(form);
      adjustCash({
        accountId: String(data.get('accountId')),
        actual: parseAmount(data.get('actual')),
        reason: String(data.get('reason') ?? '').trim()
      });
      closeModal();
      render();
      toast('اصلاح صندوق ثبت شد');
    } catch (error) {
      formError(form, error.message);
    }
  });
}

function openProductModal(id = null) {
  const product = id ? getProduct(id) : null;
  openModal({
    title: product ? 'ویرایش کالا' : 'کالای جدید',
    subtitle: product ? 'تغییرات در قیمت‌ها و حداقل موجودی' : 'افزودن کالا به انبار فروشگاه',
    body: `
      <form id="product-form" class="modal-form">
        <div class="form-error" hidden></div>
        <div class="form-grid">
          <label class="span-2">نام کالا<input name="name" value="${escapeHtml(product?.name ?? '')}" required></label>
          <label>دسته<input name="category" value="${escapeHtml(product?.category ?? '')}" placeholder="مثلاً لبنیات"></label>
          <label>بارکد<input name="barcode" value="${escapeHtml(product?.barcode ?? '')}" dir="ltr"></label>
          <label>قیمت خرید (تومان)<input name="purchasePrice" inputmode="numeric" value="${product?.purchasePrice ?? ''}" data-amount></label>
          <label>قیمت فروش (تومان)<input name="sellingPrice" inputmode="numeric" value="${product?.sellingPrice ?? ''}" data-amount></label>
          <label>میانگین بهای تمام‌شده<input name="averageCost" inputmode="numeric" value="${product?.averageCost ?? ''}" data-amount></label>
          <label>موجودی<input name="stock" type="number" min="0" step="0.001" value="${product?.stock ?? 0}"></label>
          <label>حداقل موجودی<input name="minimumStock" type="number" min="0" step="0.001" value="${product?.minimumStock ?? 0}"></label>
        </div>
        <div class="modal-actions"><button class="btn btn-primary" type="submit">${icon('check')} ${product ? 'ذخیره تغییرات' : 'افزودن کالا'}</button></div>
      </form>`
  });
  const form = modalRoot.querySelector('#product-form');
  form.addEventListener('submit', (event) => {
    event.preventDefault();
    try {
      const data = new FormData(form);
      saveProduct({
        name: String(data.get('name')),
        category: String(data.get('category') || 'عمومی'),
        barcode: String(data.get('barcode') ?? ''),
        purchasePrice: parseAmount(data.get('purchasePrice')),
        sellingPrice: parseAmount(data.get('sellingPrice')),
        averageCost: parseAmount(data.get('averageCost')) || parseAmount(data.get('purchasePrice')),
        stock: Math.max(0, Number(data.get('stock')) || 0),
        minimumStock: Math.max(0, Number(data.get('minimumStock')) || 0)
      }, id);
      closeModal();
      render();
      toast(product ? 'کالا ویرایش شد' : 'کالای جدید به انبار اضافه شد');
    } catch (error) {
      formError(form, error.message);
    }
  });
}

function openContactModal(id = null) {
  const contact = id ? getContact(id) : null;
  openModal({
    title: contact ? 'ویرایش شخص' : 'شخص جدید',
    subtitle: contact ? 'اطلاعات مشتری یا تأمین‌کننده' : 'افزودن مشتری یا تأمین‌کننده',
    body: `
      <form id="contact-form" class="modal-form">
        <div class="form-error" hidden></div>
        <div class="form-grid">
          <label class="span-2">نام<input name="name" value="${escapeHtml(contact?.name ?? '')}" required></label>
          <label>موبایل<input name="phone" value="${escapeHtml(contact?.phone ?? '')}" inputmode="tel" dir="ltr"></label>
          <label>نوع
            <select name="type">
              <option value="customer" ${contact?.type === 'customer' ? 'selected' : ''}>مشتری</option>
              <option value="supplier" ${contact?.type === 'supplier' ? 'selected' : ''}>تأمین‌کننده</option>
              <option value="both" ${contact?.type === 'both' ? 'selected' : ''}>مشتری و تأمین‌کننده</option>
            </select>
          </label>
          ${contact ? '' : '<label class="span-2">مانده اولیه (مثبت = بدهکار به ما، منفی = بستانکار)<input name="balance" inputmode="numeric" value="0"></label>'}
          <label class="span-2">توضیحات<input name="note" value="${escapeHtml(contact?.note ?? '')}" placeholder="اختیاری"></label>
        </div>
        <div class="modal-actions"><button class="btn btn-primary" type="submit">${icon('check')} ${contact ? 'ذخیره تغییرات' : 'افزودن شخص'}</button></div>
      </form>`
  });
  const form = modalRoot.querySelector('#contact-form');
  form.addEventListener('submit', (event) => {
    event.preventDefault();
    try {
      const data = new FormData(form);
      saveContact({
        name: String(data.get('name')),
        phone: String(data.get('phone') ?? ''),
        type: String(data.get('type')),
        balance: contact ? contact.balance : Number(String(data.get('balance') ?? '0').replace(/[^\d-]/g, '')) || 0,
        note: String(data.get('note') ?? '')
      }, id);
      closeModal();
      render();
      toast(contact ? 'اطلاعات شخص ذخیره شد' : 'شخص جدید اضافه شد');
    } catch (error) {
      formError(form, error.message);
    }
  });
}

function openReminderModal() {
  const options = [
    { value: 0, label: 'امروز' },
    { value: 1, label: 'فردا' },
    { value: 3, label: 'سه روز آینده' },
    { value: 7, label: 'یک هفته آینده' },
    { value: 14, label: 'دو هفته آینده' },
    { value: 30, label: 'یک ماه آینده' }
  ];
  openModal({
    title: 'یادآوری جدید',
    subtitle: 'برای سررسید بدهی یا کار مالی یادآوری بسازید',
    body: `
      <form id="reminder-form" class="modal-form">
        <div class="form-error" hidden></div>
        <div class="form-grid">
          <label class="span-2">عنوان<input name="title" placeholder="مثلاً تسویه با تأمین‌کننده" required></label>
          <label>شخص مرتبط<select name="contactId">${selectOptions(state.contacts, null, 'اختیاری')}</select></label>
          <label>مبلغ (تومان)<input name="amount" inputmode="numeric" value="0" data-amount></label>
          <label class="span-2">سررسید
            <select name="offset">${options.map((option) => `<option value="${option.value}">${option.label}</option>`).join('')}</select>
          </label>
        </div>
        <div class="modal-actions"><button class="btn btn-primary" type="submit">${icon('check')} ثبت یادآوری</button></div>
      </form>`
  });
  const form = modalRoot.querySelector('#reminder-form');
  form.addEventListener('submit', (event) => {
    event.preventDefault();
    try {
      const data = new FormData(form);
      const offset = Number(data.get('offset')) || 0;
      const due = new Date();
      due.setDate(due.getDate() + offset);
      due.setHours(10, 0, 0, 0);
      addReminder({
        title: String(data.get('title')),
        contactId: data.get('contactId') || null,
        amount: parseAmount(data.get('amount')),
        due: due.toISOString()
      });
      closeModal();
      render();
      toast('یادآوری ثبت شد');
    } catch (error) {
      formError(form, error.message);
    }
  });
}

function openTxDetail(id) {
  const tx = state.transactions.find((item) => item.id === id);
  if (!tx) return;
  const info = meta(tx.type);
  const contact = getContact(tx.contactId);
  const account = getAccount(tx.accountId);
  const items = tx.items ?? [];
  const remaining = tx.type === 'sale' ? tx.total - tx.paid : tx.type === 'purchase' ? tx.total - tx.paid : 0;
  openModal({
    title: txTitle(tx),
    subtitle: `${tx.code} · ${dateFa(tx.date)}`,
    body: `
      <div class="tx-detail">
        <div class="detail-amount tone-${info.tone}">
          <span>${info.label}</span>
          <strong>${money(tx.type === 'sale' ? tx.total : tx.type === 'purchase' ? tx.total : tx.amount)}</strong>
          ${remaining > 0 ? `<small>مانده: ${money(remaining)}</small>` : '<small>تسویه‌شده</small>'}
        </div>
        <dl class="detail-list">
          ${contact ? `<div><dt>شخص</dt><dd>${escapeHtml(contact.name)}</dd></div>` : ''}
          ${account ? `<div><dt>حساب</dt><dd>${escapeHtml(account.title)}</dd></div>` : ''}
          ${tx.mode ? `<div><dt>روش پرداخت</dt><dd>${tx.mode === 'cash' ? 'نقدی' : tx.mode === 'credit' ? 'نسیه' : 'ترکیبی'}</dd></div>` : ''}
          ${tx.type === 'sale' || tx.type === 'purchase' ? `<div><dt>پرداخت‌شده</dt><dd>${money(tx.paid)}</dd></div>` : ''}
          ${tx.note ? `<div><dt>توضیحات</dt><dd>${escapeHtml(tx.note)}</dd></div>` : ''}
        </dl>
        ${items.length ? `<div class="table-wrap"><table class="table table-compact"><thead><tr><th>کالا</th><th>تعداد</th><th>قیمت واحد</th><th>جمع</th></tr></thead><tbody>
          ${items.map((item) => `<tr><td>${escapeHtml(item.name)}</td><td>${quantity(item.qty)}</td><td>${money(item.price)}</td><td>${money(item.qty * item.price)}</td></tr>`).join('')}
        </tbody></table></div>` : ''}
        <div class="modal-actions">
          ${tx.type === 'sale' && remaining > 0 && contact ? `<button class="btn btn-primary" data-action="receive-from" data-id="${contact.id}">${icon('arrow-down')} ثبت دریافت</button>` : ''}
          ${tx.type === 'purchase' && remaining > 0 && contact ? `<button class="btn btn-primary" data-action="pay-to" data-id="${contact.id}">${icon('arrow-up')} پرداخت</button>` : ''}
          <button class="btn btn-ghost" data-action="close-modal">بستن</button>
        </div>
      </div>`
  });
}

function openContactDetail(id) {
  const contact = getContact(id);
  if (!contact) return;
  const transactions = contactTransactions(id);
  openModal({
    title: contact.name,
    subtitle: `${contact.phone || 'بدون شماره'} · ${contact.type === 'supplier' ? 'تأمین‌کننده' : contact.type === 'both' ? 'مشتری و تأمین‌کننده' : 'مشتری'}`,
    wide: true,
    body: `
      <div class="detail-amount ${contact.balance > 0 ? 'tone-income' : contact.balance < 0 ? 'tone-expense' : 'tone-neutral'}">
        <span>${contact.balance > 0 ? 'مانده بدهی مشتری' : contact.balance < 0 ? 'مانده بدهی ما' : 'تسویه'}</span>
        <strong>${money(Math.abs(contact.balance))}</strong>
      </div>
      ${contact.note ? `<p class="muted">${escapeHtml(contact.note)}</p>` : ''}
      <div class="modal-actions">
        <button class="btn btn-soft" data-action="edit-contact" data-id="${id}">${icon('edit')} ویرایش</button>
        ${contact.balance > 0 ? `<button class="btn btn-primary" data-action="receive-from" data-id="${id}">${icon('arrow-down')} ثبت دریافت</button>` : ''}
        ${contact.balance < 0 ? `<button class="btn btn-primary" data-action="pay-to" data-id="${id}">${icon('arrow-up')} ثبت پرداخت</button>` : ''}
      </div>
      <h3 class="modal-subtitle">گردش حساب</h3>
      <div class="tx-list">${transactions.map((tx) => txRow(tx)).join('') || '<p class="muted">گردشی ثبت نشده است.</p>'}</div>`
  });
}

function openProductMovements(id) {
  const product = getProduct(id);
  if (!product) return;
  const movements = productMovements(id);
  openModal({
    title: `گردش انبار ${product.name}`,
    subtitle: `موجودی فعلی ${quantity(product.stock)} · میانگین بهای تمام‌شده ${money(product.averageCost)}`,
    wide: true,
    body: `<div class="table-wrap"><table class="table table-compact"><thead><tr><th>تاریخ</th><th>سند</th><th>نوع</th><th>تعداد</th><th>قیمت واحد</th></tr></thead><tbody>
      ${movements.map((movement) => `<tr><td>${dateFa(movement.date)}</td><td>${movement.code}</td><td>${movement.direction === 'in' ? '<span class="text-income">ورود</span>' : '<span class="text-expense">خروج</span>'}</td><td>${movement.direction === 'in' ? '+' : '−'}${quantity(movement.qty)}</td><td>${money(movement.price)}</td></tr>`).join('') || '<tr><td colspan="5" class="muted">گردشی ثبت نشده است.</td></tr>'}
    </tbody></table></div>`
  });
}

function openConfirm({ title, message, confirmLabel, onConfirm, danger = false }) {
  openModal({
    title,
    body: `<p class="confirm-message">${message}</p>`,
    footer: `
      <button class="btn btn-ghost" data-action="close-modal">انصراف</button>
      <button class="btn ${danger ? 'btn-danger' : 'btn-primary'}" data-action="confirm-action">${confirmLabel}</button>`
  });
  modalRoot.querySelector('[data-action="confirm-action"]').addEventListener('click', () => {
    onConfirm();
    closeModal();
  });
}

// ---------------------------------------------------------------- Events

function handleAction(action, element) {
  switch (action) {
    case 'close-modal': closeModal(); break;
    case 'open-sale': closeModal(); openSaleModal(); break;
    case 'open-purchase': closeModal(); openPurchaseModal(); break;
    case 'open-expense': closeModal(); openExpenseModal(); break;
    case 'open-receipt': closeModal(); openPaymentModal('receive'); break;
    case 'open-pay': closeModal(); openPaymentModal('pay'); break;
    case 'open-transfer': closeModal(); openTransferModal(); break;
    case 'open-adjust': closeModal(); openAdjustModal(); break;
    case 'new-product': openProductModal(); break;
    case 'edit-product': closeModal(); openProductModal(element.dataset.id); break;
    case 'product-movements': openProductMovements(element.dataset.id); break;
    case 'new-contact': openContactModal(); break;
    case 'edit-contact': closeModal(); openContactModal(element.dataset.id); break;
    case 'contact-detail': openContactDetail(element.dataset.id); break;
    case 'receive-from': closeModal(); openPaymentModal('receive', element.dataset.id); break;
    case 'pay-to': closeModal(); openPaymentModal('pay', element.dataset.id); break;
    case 'tx-detail': openTxDetail(element.dataset.id); break;
    case 'new-reminder': openReminderModal(); break;
    case 'toggle-reminder': toggleReminder(element.dataset.id); render(); break;
    case 'delete-reminder': removeReminder(element.dataset.id); render(); toast('یادآوری حذف شد'); break;
    case 'contact-tab':
      state.ui.contactTab = element.dataset.type;
      render();
      break;
    case 'report-period':
      state.ui.reportDays = Number(element.dataset.days);
      render();
      break;
    case 'filter-sales': {
      const filter = element.dataset.filter;
      element.parentElement.querySelectorAll('.pill').forEach((pill) => pill.classList.toggle('is-active', pill === element));
      document.querySelectorAll('[data-row-type]').forEach((row) => {
        row.hidden = filter !== 'all' && row.dataset.rowType !== filter;
      });
      break;
    }
    case 'export': downloadBackup(); break;
    case 'reset':
      openConfirm({
        title: 'بازنشانی داده‌های نمونه',
        message: 'همه تغییرات شما در این مرورگر پاک و داده‌های نمونه اولیه بازگردانی می‌شود. این عمل قابل بازگشت نیست.',
        confirmLabel: 'بله، بازنشانی کن',
        danger: true,
        onConfirm: () => {
          reset();
          render();
          toast('داده‌های نمونه بازنشانی شد');
        }
      });
      break;
    case 'qty-plus':
    case 'qty-minus': {
      const input = document.getElementById(element.dataset.target);
      if (!input) return;
      const delta = action === 'qty-plus' ? 1 : -1;
      input.value = String(Math.max(0, (Number(input.value) || 0) + delta));
      input.dispatchEvent(new Event('input', { bubbles: true }));
      break;
    }
    default: break;
  }
}

function downloadBackup() {
  const blob = new Blob([exportJson()], { type: 'application/json;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  const now = new Date();
  link.href = url;
  link.download = `hesabyar-demo-backup-${now.toISOString().slice(0, 10)}.json`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
  toast('فایل پشتیبان دانلود شد');
}

document.addEventListener('click', (event) => {
  const actionEl = event.target.closest('[data-action]');
  if (actionEl) {
    if (actionEl.dataset.action === 'close-modal' && actionEl.classList.contains('modal-backdrop') && event.target !== actionEl) return;
    handleAction(actionEl.dataset.action, actionEl);
    return;
  }
  const navLink = event.target.closest('[data-nav]');
  if (navLink) {
    event.preventDefault();
    location.hash = navLink.dataset.nav;
  }
});

document.addEventListener('keydown', (event) => {
  if (event.key === 'Escape' && modalRoot.innerHTML) closeModal();
});

document.addEventListener('input', (event) => {
  const target = event.target;
  if (target.matches('[data-search]')) {
    const query = target.value.trim().toLowerCase();
    document.querySelectorAll('[data-search-text]').forEach((element) => {
      const row = element.closest('tr') ?? element;
      row.hidden = query.length > 0 && !element.dataset.searchText.toLowerCase().includes(query);
    });
  }
  if (target.matches('[data-modal-search]')) {
    const query = target.value.trim().toLowerCase();
    modalRoot.querySelectorAll('[data-search-text]').forEach((element) => {
      element.hidden = query.length > 0 && !element.dataset.searchText.toLowerCase().includes(query);
    });
  }
  if (target.matches('[data-sale-qty]') || target.matches('#sale-form [name="discount"]') || target.matches('#sale-form [name="paid"]')) {
    updateSaleSummary();
  }
  if (target.matches('[data-purchase-qty]') || target.matches('[data-purchase-price]') || target.matches('#purchase-form [name="freight"]') || target.matches('#purchase-form [name="discount"]') || target.matches('#purchase-form [name="paid"]')) {
    updatePurchaseSummary();
  }
  if (target.matches('[data-setting="fontSize"]')) {
    const value = Number(target.value);
    state.settings.fontSize = value;
    applySettings();
    const label = document.querySelector('#font-size-label');
    if (label) label.textContent = `${num(Math.round(value * 100))}٪`;
  }
});

document.addEventListener('change', (event) => {
  const target = event.target;
  if (target.matches('[data-setting]') && target.type === 'checkbox') {
    setSettings({ [target.dataset.setting]: target.checked });
    toast('تنظیمات ذخیره شد');
  }
  if (target.matches('[data-setting="fontSize"]')) {
    setSettings({ fontSize: Number(target.value) });
  }
  if (target.matches('#import-file')) {
    const file = target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      try {
        const text = String(reader.result ?? '');
        openConfirm({
          title: 'بازیابی داده‌ها',
          message: 'داده‌های فعلی با محتوای فایل پشتیبان جایگزین می‌شود. ادامه می‌دهید؟',
          confirmLabel: 'بله، بازیابی کن',
          danger: true,
          onConfirm: () => {
            try {
              importJson(text);
              render();
              toast('داده‌ها بازیابی شد');
            } catch (error) {
              toast(error.message, 'error');
            }
          }
        });
      } catch (error) {
        toast('خواندن فایل ناموفق بود', 'error');
      }
    };
    reader.readAsText(file);
    target.value = '';
  }
});

document.addEventListener('submit', (event) => {
  if (event.target.matches('#business-form')) {
    event.preventDefault();
    const data = new FormData(event.target);
    state.business = {
      name: String(data.get('name')).trim(),
      owner: String(data.get('owner') ?? '').trim(),
      phone: String(data.get('phone') ?? '').trim(),
      city: String(data.get('city') ?? '').trim(),
      type: String(data.get('type') ?? '').trim()
    };
    save();
    render();
    toast('اطلاعات فروشگاه ذخیره شد');
  }
});

// ---------------------------------------------------------------- Boot

document.querySelector('#top-notifications')?.addEventListener('click', () => { location.hash = 'reminders'; });
document.querySelector('#top-settings')?.addEventListener('click', () => { location.hash = 'settings'; });
document.querySelector('#mobile-sale')?.addEventListener('click', () => openSaleModal());

window.addEventListener('hashchange', route);
route();

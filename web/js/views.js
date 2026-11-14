import {
  dateFa, dateShort, daysUntil, escapeHtml, isSameMonth, isToday, money, monthFa,
  num, quantity, todayFa
} from './format.js';
import { icon } from './icons.js';
import {
  accountTransactions, contactTransactions, dashboardStats, getAccount, getContact,
  getProduct, monthlySeries, productMovements, salesSeries, state, topProducts
} from './store.js';

const TYPE_META = {
  sale: { label: 'فروش', icon: 'cart', tone: 'income' },
  purchase: { label: 'خرید', icon: 'truck', tone: 'warning' },
  expense: { label: 'هزینه', icon: 'receipt', tone: 'expense' },
  receipt: { label: 'دریافت', icon: 'arrow-down', tone: 'income' },
  payment: { label: 'پرداخت', icon: 'arrow-up', tone: 'expense' },
  transfer: { label: 'انتقال', icon: 'transfer', tone: 'neutral' },
  adjust: { label: 'اصلاح صندوق', icon: 'sliders', tone: 'neutral' }
};

const PAYMENT_LABEL = { cash: 'نقدی', credit: 'نسیه', mixed: 'ترکیبی' };

function meta(type) {
  return TYPE_META[type] ?? TYPE_META.adjust;
}

function toneColor(tone) {
  return {
    income: 'var(--income)',
    expense: 'var(--expense)',
    warning: 'var(--warning)',
    neutral: 'var(--text-2)'
  }[tone] ?? 'var(--text-2)';
}

function badge(text, tone = 'neutral') {
  return `<span class="badge badge-${tone}">${escapeHtml(text)}</span>`;
}

function pageHead(title, subtitle, actions = '') {
  return `
    <header class="page-head">
      <div>
        <p class="eyebrow">${todayFa()}</p>
        <h1>${title}</h1>
        <p class="page-sub">${subtitle}</p>
      </div>
      <div class="page-actions">${actions}</div>
    </header>`;
}

function statCard(label, value, hint, iconName, tone = 'neutral') {
  return `
    <article class="stat-card">
      <span class="stat-icon tone-${tone}">${icon(iconName)}</span>
      <div class="stat-body">
        <span class="stat-label">${label}</span>
        <strong class="stat-value">${value}</strong>
        ${hint ? `<span class="stat-hint">${hint}</span>` : ''}
      </div>
    </article>`;
}

function panel(title, body, action = '') {
  return `
    <section class="panel">
      <div class="panel-head">
        <h2>${title}</h2>
        ${action}
      </div>
      ${body}
    </section>`;
}

function emptyState(iconName, title, message, action = '') {
  return `
    <div class="empty">
      <span class="empty-icon">${icon(iconName)}</span>
      <h3>${title}</h3>
      <p>${message}</p>
      ${action}
    </div>`;
}

function txTitle(tx) {
  if (tx.type === 'sale') return tx.contactId ? `فروش به ${getContact(tx.contactId)?.name ?? 'مشتری'}` : 'فروش نقدی';
  if (tx.type === 'purchase') return `خرید از ${getContact(tx.contactId)?.name ?? 'تأمین‌کننده'}`;
  if (tx.type === 'expense') return tx.title;
  if (tx.type === 'receipt') return `دریافت از ${getContact(tx.contactId)?.name ?? 'مشتری'}`;
  if (tx.type === 'payment') return `پرداخت به ${getContact(tx.contactId)?.name ?? 'تأمین‌کننده'}`;
  if (tx.type === 'transfer') return `انتقال از ${getAccount(tx.fromId)?.title ?? '—'} به ${getAccount(tx.toId)?.title ?? '—'}`;
  if (tx.type === 'adjust') return tx.note || 'اصلاح صندوق';
  return 'تراکنش';
}

function txSubtitle(tx) {
  const account = getAccount(tx.accountId)?.title;
  if (tx.type === 'sale' || tx.type === 'purchase') {
    const count = tx.items.reduce((sum, item) => sum + item.qty, 0);
    const remaining = tx.total - tx.paid;
    return `${quantity(count)} قلم · ${PAYMENT_LABEL[tx.mode] ?? (remaining > 0 ? 'نسیه' : 'نقدی')}`;
  }
  return account ? `از ${account}` : dateFa(tx.date);
}

function txAmount(tx) {
  if (['sale', 'receipt'].includes(tx.type)) return { value: tx.type === 'sale' ? tx.total : tx.amount, tone: 'income' };
  if (['purchase', 'expense', 'payment'].includes(tx.type)) return { value: tx.type === 'purchase' ? tx.total : tx.amount, tone: 'expense' };
  return { value: tx.amount, tone: 'neutral' };
}

function txRow(tx, compact = false) {
  const info = meta(tx.type);
  const amount = txAmount(tx);
  const remaining = tx.type === 'sale' ? tx.total - tx.paid : tx.type === 'purchase' ? tx.total - tx.paid : 0;
  return `
    <button class="tx-row ${compact ? 'tx-row-compact' : ''}" data-action="tx-detail" data-id="${tx.id}">
      <span class="tx-icon tone-${info.tone}">${icon(info.icon)}</span>
      <span class="tx-main">
        <span class="tx-title">${escapeHtml(txTitle(tx))}</span>
        <span class="tx-meta">${escapeHtml(txSubtitle(tx))} · ${dateShort(tx.date)}</span>
      </span>
      <span class="tx-side">
        <span class="tx-amount" style="color:${toneColor(amount.tone)}">${money(amount.value)}</span>
        ${remaining > 0 ? badge(`${money(remaining)} نسیه`, 'warning') : badge(info.label, info.tone)}
      </span>
    </button>`;
}

function barChart(series, height = 180) {
  const max = Math.max(...series.map((point) => point.total), 1);
  const width = 640;
  const gap = 14;
  const barWidth = Math.max(14, (width - gap * (series.length - 1)) / series.length);
  const bars = series.map((point, index) => {
    const barHeight = Math.max(4, Math.round((point.total / max) * (height - 34)));
    const x = index * (barWidth + gap);
    return `
      <g>
        <rect x="${x}" y="${height - 24 - barHeight}" width="${barWidth}" height="${barHeight}" rx="7" fill="url(#barGradient)">
          <title>${dateFa(point.date)}: ${money(point.total)}</title>
        </rect>
        <text x="${x + barWidth / 2}" y="${height - 6}" text-anchor="middle" class="chart-label">${dateShort(point.date)}</text>
      </g>`;
  }).join('');
  return `
    <div class="chart-wrap">
      <svg class="chart" viewBox="0 0 ${width} ${height}" role="img" aria-label="نمودار فروش">
        <defs>
          <linearGradient id="barGradient" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stop-color="#22A06B"/>
            <stop offset="100%" stop-color="#0E5A43"/>
          </linearGradient>
        </defs>
        ${bars}
      </svg>
    </div>`;
}

function monthlyChart(series) {
  const height = 210;
  const width = 720;
  const group = width / series.length;
  const max = Math.max(...series.flatMap((point) => [point.sales, point.purchases, point.expenses]), 1);
  const colors = { sales: '#0E5A43', purchases: '#E08A00', expenses: '#D64545' };
  const bars = series.map((point, index) => {
    const values = [point.sales, point.purchases, point.expenses];
    const barWidth = 16;
    const base = index * group;
    return values.map((value, valueIndex) => {
      const barHeight = Math.max(2, Math.round((value / max) * (height - 40)));
      const x = base + (group - barWidth * 3 - 10) / 2 + valueIndex * (barWidth + 5);
      return `<rect x="${x}" y="${height - 28 - barHeight}" width="${barWidth}" height="${barHeight}" rx="5" fill="${colors[Object.keys(colors)[valueIndex]]}"><title>${monthFa(point.date)}: ${money(value)}</title></rect>`;
    }).join('');
  }).join('');
  const labels = series.map((point, index) => `<text x="${index * group + group / 2}" y="${height - 6}" text-anchor="middle" class="chart-label">${monthFa(point.date).split(' ')[0]}</text>`).join('');
  return `
    <div class="chart-wrap">
      <svg class="chart" viewBox="0 0 ${width} ${height}" role="img" aria-label="نمودار ماهانه">
        ${bars}${labels}
      </svg>
      <div class="chart-legend">
        <span><i style="background:#0E5A43"></i>فروش</span>
        <span><i style="background:#E08A00"></i>خرید</span>
        <span><i style="background:#D64545"></i>هزینه</span>
      </div>
    </div>`;
}

// ---------------------------------------------------------------- Dashboard

export function dashboardView() {
  const stats = dashboardStats();
  const series = salesSeries(7);
  const recent = state.transactions.slice(0, 6);
  const lowStock = stats.lowStock.slice(0, 3);
  const reminders = state.reminders.filter((reminder) => !reminder.done).slice(0, 3);
  const topDebtors = state.contacts.filter((contact) => contact.balance > 0).sort((a, b) => b.balance - a.balance).slice(0, 3);

  return `
    <section class="page">
      ${pageHead('داشبورد', `سلام ${escapeHtml(state.business.owner)}، امروز فروشگاه چطور پیش می‌رود؟`,
        `<button class="btn btn-primary" data-action="open-sale">${icon('plus')} ثبت فروش</button>`)}
      <div class="hero-card">
        <div class="hero-main">
          <span class="hero-kicker">موجودی کل حساب‌ها</span>
          <strong class="hero-value">${money(stats.cash)}</strong>
          <span class="hero-note">${state.accounts.length} حساب فعال · به‌روزرسانی لحظه‌ای</span>
        </div>
        <div class="hero-accounts">
          ${state.accounts.map((account) => `
            <div class="hero-account">
              <span>${icon(account.type === 'بانک' ? 'bank' : account.type === 'کارتخوان' ? 'card' : 'wallet')} ${escapeHtml(account.title)}</span>
              <strong>${money(account.balance)}</strong>
            </div>`).join('')}
        </div>
      </div>
      <div class="quick-grid">
        <button class="quick" data-action="open-sale"><span class="quick-icon tone-income">${icon('cart')}</span><span>فروش</span></button>
        <button class="quick" data-action="open-purchase"><span class="quick-icon tone-warning">${icon('truck')}</span><span>خرید</span></button>
        <button class="quick" data-action="open-expense"><span class="quick-icon tone-expense">${icon('receipt')}</span><span>هزینه</span></button>
        <button class="quick" data-action="open-receipt"><span class="quick-icon tone-income">${icon('arrow-down')}</span><span>دریافت</span></button>
        <button class="quick" data-action="open-transfer"><span class="quick-icon tone-neutral">${icon('transfer')}</span><span>انتقال وجه</span></button>
      </div>
      <div class="stats-grid">
        ${statCard('فروش امروز', money(stats.salesTotal), `${num(stats.salesCount)} فاکتور`, 'chart', 'income')}
        ${statCard('دریافت امروز', money(stats.receivedTotal), 'نقد و دریافت بدهی', 'arrow-down', 'income')}
        ${statCard('هزینه امروز', money(stats.expenseTotal), 'هزینه‌های عملیاتی', 'receipt', 'expense')}
        ${statCard('سود امروز', money(stats.profit), 'پس از کسر بهای کالا', 'spark', stats.profit >= 0 ? 'income' : 'expense')}
        ${statCard('طلب از مشتریان', money(stats.receivable), `${num(state.contacts.filter((c) => c.balance > 0).length)} نفر بدهکار`, 'users', 'warning')}
        ${statCard('بدهی به تأمین‌کنندگان', money(stats.payable), `${num(state.contacts.filter((c) => c.balance < 0).length)} تأمین‌کننده`, 'truck', 'expense')}
      </div>
      <div class="dashboard-grid">
        ${panel('روند فروش ۷ روز اخیر', barChart(series), '<a class="link" href="#reports">گزارش کامل</a>')}
        ${panel('آخرین تراکنش‌ها', `<div class="tx-list">${recent.map((tx) => txRow(tx, true)).join('')}</div>`, '<a class="link" href="#sales">همه تراکنش‌ها</a>')}
        ${panel('هشدار موجودی', lowStock.length ? `<div class="mini-list">${lowStock.map((product) => `
          <button class="mini-row" data-action="edit-product" data-id="${product.id}">
            <span class="mini-icon tone-warning">${icon('box')}</span>
            <span class="mini-body"><strong>${escapeHtml(product.name)}</strong><small>موجودی ${quantity(product.stock)} · حداقل ${quantity(product.minimumStock)}</small></span>
            <span class="badge badge-warning">سفارش خرید</span>
          </button>`).join('')}</div>` : emptyState('check', 'موجودی‌ها سالم است', 'هیچ کالایی زیر حداقل موجودی نیست.'), '<a class="link" href="#products">کالاها</a>')}
        ${panel('یادآوری‌های نزدیک', reminders.length ? `<div class="mini-list">${reminders.map((reminder) => `
          <button class="mini-row" data-action="toggle-reminder" data-id="${reminder.id}">
            <span class="mini-icon tone-warning">${icon('bell')}</span>
            <span class="mini-body"><strong>${escapeHtml(reminder.title)}</strong><small>${dateFa(reminder.due)}${reminder.amount ? ` · ${money(reminder.amount)}` : ''}</small></span>
          </button>`).join('')}</div>` : emptyState('bell', 'یادآوری فعالی نیست', 'برای سررسیدها یادآوری بسازید.'), '<a class="link" href="#reminders">همه</a>')}
      </div>
      <div class="split-bottom">
        ${panel('بیشترین طلب‌ها', topDebtors.length ? `<div class="mini-list">${topDebtors.map((contact) => `
          <button class="mini-row" data-action="contact-detail" data-id="${contact.id}">
            <span class="avatar">${escapeHtml(contact.name.slice(0, 1))}</span>
            <span class="mini-body"><strong>${escapeHtml(contact.name)}</strong><small>${escapeHtml(contact.phone || 'بدون شماره')}</small></span>
            <strong class="text-warning">${money(contact.balance)}</strong>
          </button>`).join('')}</div>` : emptyState('users', 'بدهکاری نیست', 'همه حساب‌ها تسویه‌اند.'), '<a class="link" href="#contacts">اشخاص</a>')}
        <div class="insight-card">
          <span class="eyebrow">پیشنهاد حسابیار</span>
          <h3>${stats.receivable > 0 ? 'امروز یک پیگیری بدهی انجام دهید' : 'فروش نقدی را تقویت کنید'}</h3>
          <p>${stats.receivable > 0 ? `از ${money(stats.receivable)} طلب، بخشی قابل وصول است. با یک تماس کوتاه، جریان نقدی فروشگاه را بهبود دهید.` : 'با ثبت سریع فروش و دریافت نقدی، موجودی صندوق را به‌روز نگه دارید.'}</p>
          <button class="btn btn-soft" data-action="open-receipt">${icon('arrow-down')} ثبت دریافت</button>
        </div>
      </div>
    </section>`;
}

// ---------------------------------------------------------------- Sales

export function salesView() {
  const sales = state.transactions.filter((tx) => tx.type === 'sale');
  const monthSales = sales.filter((tx) => isSameMonth(tx.date));
  const total = monthSales.reduce((sum, tx) => sum + tx.total, 0);
  const credit = monthSales.reduce((sum, tx) => sum + (tx.total - tx.paid), 0);
  return `
    <section class="page">
      ${pageHead('فروش', 'فاکتورهای فروش، نقد و نسیه، در یک نگاه',
        `<button class="btn btn-primary" data-action="open-sale">${icon('plus')} فاکتور فروش</button>`)}
      <div class="stats-grid stats-grid-3">
        ${statCard('فروش این ماه', money(total), `${num(monthSales.length)} فاکتور`, 'cart', 'income')}
        ${statCard('مانده نسیه این ماه', money(credit), 'در حساب مشتریان', 'clock', 'warning')}
        ${statCard('میانگین هر فاکتور', money(monthSales.length ? total / monthSales.length : 0), 'فروش ماه جاری', 'chart', 'neutral')}
      </div>
      ${panel('فاکتورهای فروش', `
        <div class="toolbar">
          <label class="search">${icon('search')}<input type="search" placeholder="جست‌وجوی مشتری، کد فاکتور..." data-search></label>
          <div class="filter-pills">
            <button class="pill is-active" data-action="filter-sales" data-filter="all">همه</button>
            <button class="pill" data-action="filter-sales" data-filter="credit">نسیه</button>
            <button class="pill" data-action="filter-sales" data-filter="cash">تسویه‌شده</button>
          </div>
        </div>
        <div class="table-wrap">
          <table class="table">
            <thead><tr><th>فاکتور</th><th>مشتری</th><th>تاریخ</th><th>مبلغ</th><th>مانده</th><th>وضعیت</th></tr></thead>
            <tbody>
              ${sales.map((tx) => {
                const remaining = tx.total - tx.paid;
                return `<tr data-action="tx-detail" data-id="${tx.id}" data-row-type="${remaining > 0 ? 'credit' : 'cash'}" data-search-text="${escapeHtml((getContact(tx.contactId)?.name ?? 'فروش نقدی') + ' ' + tx.code)}">
                  <td><strong>${escapeHtml(tx.code)}</strong><small>${num(tx.items.length)} قلم کالا</small></td>
                  <td>${escapeHtml(getContact(tx.contactId)?.name ?? 'مشتری نقدی')}</td>
                  <td>${dateFa(tx.date)}</td>
                  <td><strong>${money(tx.total)}</strong></td>
                  <td>${remaining > 0 ? `<span class="text-warning">${money(remaining)}</span>` : '—'}</td>
                  <td>${remaining > 0 ? badge('نسیه', 'warning') : badge('تسویه‌شده', 'income')}</td>
                </tr>`;
              }).join('') || `<tr><td colspan="6">${emptyState('cart', 'فاکتور فروشی ثبت نشده', 'اولین فاکتور را ثبت کنید.', '<button class="btn btn-primary" data-action="open-sale">ثبت فروش</button>')}</td></tr>`}
            </tbody>
          </table>
        </div>`)}
    </section>`;
}

// ---------------------------------------------------------------- Purchases

export function purchasesView() {
  const purchases = state.transactions.filter((tx) => tx.type === 'purchase');
  const total = purchases.filter((tx) => isSameMonth(tx.date)).reduce((sum, tx) => sum + tx.total, 0);
  const payable = purchases.reduce((sum, tx) => sum + (tx.total - tx.paid), 0);
  return `
    <section class="page">
      ${pageHead('خرید', 'خرید کالا، به‌روزرسانی انبار و بدهی تأمین‌کنندگان',
        `<button class="btn btn-primary" data-action="open-purchase">${icon('plus')} ثبت خرید</button>`)}
      <div class="stats-grid stats-grid-3">
        ${statCard('خرید این ماه', money(total), `${num(purchases.filter((tx) => isSameMonth(tx.date)).length)} فاکتور`, 'truck', 'warning')}
        ${statCard('مانده بدهی خریدها', money(payable), 'بدهی جاری تأمین‌کنندگان', 'clock', 'expense')}
        ${statCard('ارزش موجودی انبار', money(state.products.reduce((sum, p) => sum + p.stock * p.averageCost, 0)), 'با میانگین بهای تمام‌شده', 'box', 'neutral')}
      </div>
      ${panel('فاکتورهای خرید', `
        <div class="table-wrap">
          <table class="table">
            <thead><tr><th>فاکتور</th><th>تأمین‌کننده</th><th>تاریخ</th><th>مبلغ</th><th>پرداخت‌شده</th><th>مانده</th></tr></thead>
            <tbody>
              ${purchases.map((tx) => `<tr data-action="tx-detail" data-id="${tx.id}">
                <td><strong>${escapeHtml(tx.code)}</strong><small>${num(tx.items.length)} قلم</small></td>
                <td>${escapeHtml(getContact(tx.contactId)?.name ?? '—')}</td>
                <td>${dateFa(tx.date)}</td>
                <td><strong>${money(tx.total)}</strong></td>
                <td>${money(tx.paid)}</td>
                <td>${tx.total - tx.paid > 0 ? `<span class="text-expense">${money(tx.total - tx.paid)}</span>` : badge('تسویه', 'income')}</td>
              </tr>`).join('') || `<tr><td colspan="6">${emptyState('truck', 'خریدی ثبت نشده', 'اولین خرید را ثبت کنید.', '<button class="btn btn-primary" data-action="open-purchase">ثبت خرید</button>')}</td></tr>`}
            </tbody>
          </table>
        </div>`)}
    </section>`;
}

// ---------------------------------------------------------------- Expenses

export function expensesView() {
  const expenses = state.transactions.filter((tx) => tx.type === 'expense');
  const monthExpenses = expenses.filter((tx) => isSameMonth(tx.date));
  const total = monthExpenses.reduce((sum, tx) => sum + tx.amount, 0);
  const byCategory = monthExpenses.reduce((map, tx) => {
    map[tx.category] = (map[tx.category] ?? 0) + tx.amount;
    return map;
  }, {});
  const categories = Object.entries(byCategory).sort((a, b) => b[1] - a[1]);
  return `
    <section class="page">
      ${pageHead('هزینه‌ها', 'همه هزینه‌های عملیاتی، دسته‌بندی‌شده و قابل پیگیری',
        `<button class="btn btn-primary" data-action="open-expense">${icon('plus')} ثبت هزینه</button>`)}
      <div class="stats-grid stats-grid-3">
        ${statCard('هزینه این ماه', money(total), `${num(monthExpenses.length)} سند`, 'receipt', 'expense')}
        ${statCard('بیشترین دسته', categories.length ? categories[0][0] : '—', categories.length ? money(categories[0][1]) : 'داده‌ای نیست', 'chart', 'warning')}
        ${statCard('میانگین هر هزینه', money(monthExpenses.length ? total / monthExpenses.length : 0), 'ماه جاری', 'wallet', 'neutral')}
      </div>
      <div class="dashboard-grid">
        ${panel('سوابق هزینه', `<div class="tx-list">${expenses.map((tx) => txRow(tx)).join('') || emptyState('receipt', 'هزینه‌ای ثبت نشده', 'هزینه‌های فروشگاه را اینجا ثبت کنید.', '<button class="btn btn-primary" data-action="open-expense">ثبت هزینه</button>')}</div>`)}
        ${panel('ترکیب هزینه‌ها', categories.length ? `<div class="category-list">${categories.map(([name, amount]) => `
          <div class="category-row">
            <span>${escapeHtml(name)}</span>
            <div class="progress"><i style="width:${Math.round((amount / total) * 100)}%"></i></div>
            <strong>${money(amount)}</strong>
          </div>`).join('')}</div>` : emptyState('chart', 'داده‌ای نیست', 'پس از ثبت هزینه، ترکیب آن را ببینید.'))}
      </div>
    </section>`;
}

// ---------------------------------------------------------------- Products

export function productsView() {
  const lowStock = state.products.filter((product) => product.minimumStock > 0 && product.stock <= product.minimumStock);
  return `
    <section class="page">
      ${pageHead('کالاها و انبار', 'موجودی، قیمت‌ها و گردش ورود و خروج کالا',
        `<button class="btn btn-primary" data-action="new-product">${icon('plus')} کالای جدید</button>`)}
      <div class="stats-grid stats-grid-3">
        ${statCard('تعداد کالا', num(state.products.length), `${num(state.products.filter((p) => p.category).length)} دسته`, 'box', 'neutral')}
        ${statCard('ارزش انبار', money(state.products.reduce((sum, p) => sum + p.stock * p.averageCost, 0)), 'بهای تمام‌شده', 'wallet', 'income')}
        ${statCard('کالاهای کم‌موجود', num(lowStock.length), 'نیازمند سفارش خرید', 'alert', lowStock.length ? 'warning' : 'income')}
      </div>
      <div class="toolbar">
        <label class="search">${icon('search')}<input type="search" placeholder="جست‌وجوی نام یا دسته کالا..." data-search></label>
      </div>
      <div class="product-grid">
        ${state.products.map((product) => {
          const percent = product.minimumStock > 0 ? Math.min(100, Math.round((product.stock / (product.minimumStock * 3)) * 100)) : 100;
          return `
          <article class="product-card" data-search-text="${escapeHtml(product.name + ' ' + product.category)}">
            <div class="product-head">
              <span class="product-icon">${icon('box')}</span>
              ${product.stock <= product.minimumStock ? badge('کم‌موجود', 'warning') : badge(product.category, 'neutral')}
            </div>
            <h3>${escapeHtml(product.name)}</h3>
            <p class="product-price">${money(product.sellingPrice)}</p>
            <div class="stock-line">
              <div class="progress"><i style="width:${percent}%;${product.stock <= product.minimumStock ? 'background:var(--warning)' : ''}"></i></div>
              <span>${quantity(product.stock)} عدد</span>
            </div>
            <dl class="product-facts">
              <div><dt>خرید</dt><dd>${money(product.purchasePrice)}</dd></div>
              <div><dt>میانگین بهای تمام‌شده</dt><dd>${money(product.averageCost)}</dd></div>
            </dl>
            <div class="product-actions">
              <button class="btn btn-soft btn-sm" data-action="edit-product" data-id="${product.id}">${icon('edit')} ویرایش</button>
              <button class="btn btn-ghost btn-sm" data-action="product-movements" data-id="${product.id}">${icon('list')} گردش</button>
            </div>
          </article>`;
        }).join('')}
      </div>
    </section>`;
}

// ---------------------------------------------------------------- Contacts

export function contactsView() {
  const tab = state.ui.contactTab || 'all';
  const contacts = state.contacts.filter((contact) => tab === 'all' || contact.type === tab || (tab === 'customer' && contact.type === 'both'));
  const receivable = state.contacts.filter((c) => c.balance > 0).reduce((sum, c) => sum + c.balance, 0);
  const payable = state.contacts.filter((c) => c.balance < 0).reduce((sum, c) => sum - c.balance, 0);
  return `
    <section class="page">
      ${pageHead('اشخاص', 'مشتریان، تأمین‌کنندگان و مانده حساب هرکدام',
        `<button class="btn btn-primary" data-action="new-contact">${icon('plus')} شخص جدید</button>`)}
      <div class="stats-grid stats-grid-3">
        ${statCard('طلب از مشتریان', money(receivable), `${num(state.contacts.filter((c) => c.balance > 0).length)} نفر`, 'users', 'income')}
        ${statCard('بدهی به تأمین‌کنندگان', money(payable), `${num(state.contacts.filter((c) => c.balance < 0).length)} تأمین‌کننده`, 'truck', 'expense')}
        ${statCard('تعداد اشخاص', num(state.contacts.length), 'مشتری و تأمین‌کننده', 'people', 'neutral')}
      </div>
      <div class="toolbar">
        <div class="filter-pills">
          <button class="pill ${tab === 'all' ? 'is-active' : ''}" data-action="contact-tab" data-type="all">همه</button>
          <button class="pill ${tab === 'customer' ? 'is-active' : ''}" data-action="contact-tab" data-type="customer">مشتریان</button>
          <button class="pill ${tab === 'supplier' ? 'is-active' : ''}" data-action="contact-tab" data-type="supplier">تأمین‌کنندگان</button>
        </div>
        <label class="search">${icon('search')}<input type="search" placeholder="جست‌وجوی نام یا موبایل..." data-search></label>
      </div>
      <div class="contact-grid">
        ${contacts.map((contact) => `
          <article class="contact-card" data-search-text="${escapeHtml(contact.name + ' ' + (contact.phone || ''))}">
            <div class="contact-head">
              <span class="avatar avatar-lg">${escapeHtml(contact.name.slice(0, 1))}</span>
              <div>
                <h3>${escapeHtml(contact.name)}</h3>
                <p>${contact.phone ? `<span dir="ltr">${escapeHtml(contact.phone)}</span>` : 'بدون شماره'} · ${contact.type === 'supplier' ? 'تأمین‌کننده' : contact.type === 'both' ? 'مشتری و تأمین‌کننده' : 'مشتری'}</p>
              </div>
            </div>
            <div class="balance-row ${contact.balance > 0 ? 'is-receivable' : contact.balance < 0 ? 'is-payable' : ''}">
              <span>${contact.balance > 0 ? 'بدهکار به ما' : contact.balance < 0 ? 'بستانکار از ما' : 'تسویه'}</span>
              <strong>${money(Math.abs(contact.balance))}</strong>
            </div>
            <div class="contact-actions">
              <button class="btn btn-soft btn-sm" data-action="contact-detail" data-id="${contact.id}">${icon('list')} گردش حساب</button>
              ${contact.balance > 0
                ? `<button class="btn btn-primary btn-sm" data-action="receive-from" data-id="${contact.id}">${icon('arrow-down')} دریافت</button>`
                : contact.balance < 0
                  ? `<button class="btn btn-primary btn-sm" data-action="pay-to" data-id="${contact.id}">${icon('arrow-up')} پرداخت</button>`
                  : ''}
            </div>
          </article>`).join('') || emptyState('users', 'شخصی پیدا نشد', 'فیلتر یا عبارت جست‌وجو را تغییر دهید.')}
      </div>
    </section>`;
}

// ---------------------------------------------------------------- Accounts

export function accountsView() {
  const total = state.accounts.reduce((sum, account) => sum + account.balance, 0);
  return `
    <section class="page">
      ${pageHead('حساب‌ها', 'صندوق، بانک و کارتخوان؛ همه در یک نگاه',
        `<button class="btn btn-primary" data-action="open-transfer">${icon('transfer')} انتقال وجه</button>`)}
      <div class="hero-card hero-card-soft">
        <div class="hero-main">
          <span class="hero-kicker">جمع دارایی نقدی</span>
          <strong class="hero-value">${money(total)}</strong>
          <span class="hero-note">${state.accounts.map((account) => account.title).join(' · ')}</span>
        </div>
        <button class="btn btn-light" data-action="open-adjust">${icon('sliders')} اصلاح صندوق</button>
      </div>
      <div class="account-grid">
        ${state.accounts.map((account) => {
          const movements = accountTransactions(account.id).slice(0, 4);
          return `
          <article class="account-card">
            <div class="account-head">
              <span class="account-icon">${icon(account.type === 'بانک' ? 'bank' : account.type === 'کارتخوان' ? 'card' : 'wallet')}</span>
              <div><h3>${escapeHtml(account.title)}</h3><p>${escapeHtml(account.type)}</p></div>
              <span class="badge badge-neutral">فعال</span>
            </div>
            <strong class="account-balance">${money(account.balance)}</strong>
            <div class="tx-list tx-list-tight">
              ${movements.map((tx) => `
                <button class="tx-row tx-row-compact" data-action="tx-detail" data-id="${tx.id}">
                  <span class="tx-icon tone-${meta(tx.type).tone}">${icon(meta(tx.type).icon)}</span>
                  <span class="tx-main"><span class="tx-title">${escapeHtml(txTitle(tx))}</span><span class="tx-meta">${dateShort(tx.date)}</span></span>
                  <span class="tx-amount">${money(tx.type === 'expense' ? tx.amount : tx.paid || tx.amount)}</span>
                </button>`).join('') || '<p class="muted">هنوز گردشی ثبت نشده است.</p>'}
            </div>
          </article>`;
        }).join('')}
      </div>
    </section>`;
}

// ---------------------------------------------------------------- Reports

export function reportsView() {
  const days = Number(state.ui.reportDays) || 30;
  const stats = dashboardStats();
  const series = salesSeries(Math.min(days, 30));
  const months = monthlySeries();
  const products = topProducts(5);
  const monthSales = state.transactions.filter((tx) => tx.type === 'sale' && isSameMonth(tx.date)).reduce((sum, tx) => sum + tx.total, 0);
  const monthExpenses = state.transactions.filter((tx) => tx.type === 'expense' && isSameMonth(tx.date)).reduce((sum, tx) => sum + tx.amount, 0);
  const monthCogs = state.transactions.filter((tx) => tx.type === 'sale' && isSameMonth(tx.date)).reduce((sum, tx) => sum + tx.items.reduce((line, item) => line + item.qty * item.cost, 0), 0);
  return `
    <section class="page">
      ${pageHead('گزارش‌ها', 'تصمیم‌های بهتر با گزارش‌های فروش، سود و مانده‌ها', `
        <div class="filter-pills">
          ${[7, 30, 90].map((value) => `<button class="pill ${days === value ? 'is-active' : ''}" data-action="report-period" data-days="${value}">${num(value)} روز</button>`).join('')}
        </div>
        <button class="btn btn-soft" data-action="export">${icon('download')} خروجی</button>`)}
      <div class="stats-grid stats-grid-4">
        ${statCard('فروش ماه', money(monthSales), monthFa(new Date().toISOString()), 'cart', 'income')}
        ${statCard('هزینه ماه', money(monthExpenses), 'عملیاتی', 'receipt', 'expense')}
        ${statCard('سود ناخالص ماه', money(monthSales - monthCogs), 'فروش منهای بهای کالا', 'chart', 'income')}
        ${statCard('سود خالص ماه', money(monthSales - monthCogs - monthExpenses), 'پس از هزینه‌ها', 'spark', monthSales - monthCogs - monthExpenses >= 0 ? 'income' : 'expense')}
      </div>
      <div class="dashboard-grid">
        ${panel('روند فروش', barChart(series))}
        ${panel('عملکرد شش ماه', monthlyChart(months))}
        ${panel('پرفروش‌ترین کالاها', products.length ? `<div class="category-list">${products.map((product, index) => `
          <div class="category-row">
            <span class="rank">${num(index + 1)}</span>
            <span class="category-name">${escapeHtml(product.name)}<small>${quantity(product.qty)} عدد فروش</small></span>
            <strong>${money(product.amount)}</strong>
          </div>`).join('')}</div>` : emptyState('chart', 'هنوز فروشی نیست', 'با ثبت فروش، گزارش کالاها ساخته می‌شود.'))}
        ${panel('مانده اشخاص', `<div class="mini-list">${state.contacts.filter((c) => c.balance !== 0).sort((a, b) => Math.abs(b.balance) - Math.abs(a.balance)).map((contact) => `
          <button class="mini-row" data-action="contact-detail" data-id="${contact.id}">
            <span class="avatar">${escapeHtml(contact.name.slice(0, 1))}</span>
            <span class="mini-body"><strong>${escapeHtml(contact.name)}</strong><small>${contact.balance > 0 ? 'طلب ما' : 'بدهی ما'}</small></span>
            <strong class="${contact.balance > 0 ? 'text-warning' : 'text-expense'}">${money(Math.abs(contact.balance))}</strong>
          </button>`).join('') || emptyState('users', 'مانده‌ای نیست', 'همه حساب‌ها تسویه‌اند.')}</div>`)}
      </div>
      ${panel('خلاصه امروز', `<div class="summary-grid">
        <div><span>فروش امروز</span><strong>${money(stats.salesTotal)}</strong></div>
        <div><span>دریافت امروز</span><strong>${money(stats.receivedTotal)}</strong></div>
        <div><span>هزینه امروز</span><strong>${money(stats.expenseTotal)}</strong></div>
        <div><span>سود امروز</span><strong>${money(stats.profit)}</strong></div>
        <div><span>طلب کل</span><strong>${money(stats.receivable)}</strong></div>
        <div><span>بدهی کل</span><strong>${money(stats.payable)}</strong></div>
      </div>`)}
    </section>`;
}

// ---------------------------------------------------------------- Reminders

export function remindersView() {
  const active = state.reminders.filter((reminder) => !reminder.done).sort((a, b) => new Date(a.due) - new Date(b.due));
  const done = state.reminders.filter((reminder) => reminder.done);
  return `
    <section class="page">
      ${pageHead('یادآوری‌ها', 'سررسید بدهی‌ها، مطالبات و کارهای مالی را فراموش نکنید',
        `<button class="btn btn-primary" data-action="new-reminder">${icon('plus')} یادآوری جدید</button>`)}
      <div class="stats-grid stats-grid-3">
        ${statCard('در انتظار', num(active.length), 'کار انجام‌نشده', 'bell', 'warning')}
        ${statCard('سررسید گذشته', num(active.filter((reminder) => daysUntil(reminder.due) < 0).length), 'نیازمند پیگیری فوری', 'alert', 'expense')}
        ${statCard('انجام‌شده', num(done.length), 'کارهای بسته‌شده', 'check', 'income')}
      </div>
      ${panel('در انتظار انجام', `<div class="reminder-list">${active.map((reminder) => {
        const overdue = daysUntil(reminder.due) < 0;
        return `
          <div class="reminder-row">
            <button class="check" data-action="toggle-reminder" data-id="${reminder.id}" aria-label="انجام شد">${icon('circle')}</button>
            <div class="reminder-body">
              <strong>${escapeHtml(reminder.title)}</strong>
              <span>${dateFa(reminder.due)}${reminder.amount ? ` · ${money(reminder.amount)}` : ''}${reminder.contactId ? ` · ${escapeHtml(getContact(reminder.contactId)?.name ?? '')}` : ''}</span>
            </div>
            ${overdue ? badge('سررسید گذشته', 'expense') : badge(daysUntil(reminder.due) === 0 ? 'امروز' : `${num(daysUntil(reminder.due))} روز مانده`, 'warning')}
            <button class="icon-btn" data-action="delete-reminder" data-id="${reminder.id}" aria-label="حذف">${icon('trash')}</button>
          </div>`;
      }).join('') || emptyState('bell', 'یادآوری فعالی نیست', 'برای پیگیری سررسیدها یک یادآوری بسازید.', '<button class="btn btn-primary" data-action="new-reminder">یادآوری جدید</button>')}</div>`)}
      ${done.length ? panel('انجام‌شده‌ها', `<div class="reminder-list">${done.map((reminder) => `
        <div class="reminder-row is-done">
          <button class="check is-checked" data-action="toggle-reminder" data-id="${reminder.id}" aria-label="بازگشت">${icon('check')}</button>
          <div class="reminder-body"><strong>${escapeHtml(reminder.title)}</strong><span>${dateFa(reminder.due)}</span></div>
          ${badge('انجام شد', 'income')}
        </div>`).join('')}</div>`) : ''}
    </section>`;
}

// ---------------------------------------------------------------- Settings

export function settingsView() {
  const business = state.business;
  return `
    <section class="page">
      ${pageHead('تنظیمات', 'اطلاعات فروشگاه، نمایش و مدیریت داده‌های نسخه نمایشی')}
      <div class="settings-grid">
        ${panel('اطلاعات فروشگاه', `
          <form id="business-form" class="form-grid">
            <label>نام فروشگاه<input name="name" value="${escapeHtml(business.name)}" required></label>
            <label>نام مالک<input name="owner" value="${escapeHtml(business.owner)}"></label>
            <label>موبایل<input name="phone" value="${escapeHtml(business.phone)}" inputmode="tel"></label>
            <label>شهر<input name="city" value="${escapeHtml(business.city)}"></label>
            <label class="span-2">نوع کسب‌وکار<input name="type" value="${escapeHtml(business.type)}"></label>
            <div class="form-actions span-2"><button class="btn btn-primary" type="submit">${icon('check')} ذخیره اطلاعات</button></div>
          </form>`)}
        ${panel('نمایش', `
          <div class="switch-list">
            <label class="switch-row"><span><strong>اعداد فارسی</strong><small>نمایش مبالغ با ارقام فارسی</small></span><input type="checkbox" data-setting="persianDigits" ${state.settings.persianDigits ? 'checked' : ''}></label>
            <label class="switch-row"><span><strong>اعلان‌ها</strong><small>یادآوری سررسیدها و هشدار موجودی</small></span><input type="checkbox" data-setting="notifications" ${state.settings.notifications ? 'checked' : ''}></label>
            <label class="switch-row"><span><strong>هشدار کمبود موجودی</strong><small>نمایش هشدار برای کالاهای کم‌موجود</small></span><input type="checkbox" data-setting="lowStockAlerts" ${state.settings.lowStockAlerts ? 'checked' : ''}></label>
            <label class="range-row"><span>اندازه متن <strong id="font-size-label">${num(Math.round(state.settings.fontSize * 100))}٪</strong></span><input type="range" min="0.85" max="1.2" step="0.05" value="${state.settings.fontSize}" data-setting="fontSize"></label>
          </div>`)}
        ${panel('داده‌های نسخه نمایشی', `
          <p class="muted">همه داده‌ها فقط در همین مرورگر و بدون ارسال به سرور ذخیره می‌شوند. برای انتقال یا نگهداری، فایل پشتیبان بگیرید.</p>
          <div class="stack-actions">
            <button class="btn btn-soft" data-action="export">${icon('download')} دانلود پشتیبان JSON</button>
            <label class="btn btn-soft file-btn">${icon('upload')} بازیابی از فایل<input type="file" accept="application/json" id="import-file"></label>
            <button class="btn btn-danger-soft" data-action="reset">${icon('refresh')} بازنشانی داده‌های نمونه</button>
          </div>
          <p class="muted small">حسابیار نسخه ۱٫۰٫۰ — نسخه نمایشی مرورگر. نسخه اندروید با همین طراحی و دیتابیس محلی عرضه شده است.</p>`)}
      </div>
      <div class="about-strip">
        <span class="brand-mark">${icon('ledger')}</span>
        <div><strong>حسابیار</strong><p>حساب‌وکتاب کسب‌وکار شما، ساده و دقیق. ساخته‌شده برای فروشگاه‌های کوچک ایران.</p></div>
        <span class="badge badge-neutral">نسخه ۱٫۰٫۰</span>
      </div>
    </section>`;
}

export function viewFor(name) {
  switch (name) {
    case 'sales': return salesView();
    case 'purchases': return purchasesView();
    case 'expenses': return expensesView();
    case 'products': return productsView();
    case 'contacts': return contactsView();
    case 'accounts': return accountsView();
    case 'reports': return reportsView();
    case 'reminders': return remindersView();
    case 'settings': return settingsView();
    default: return dashboardView();
  }
}

export { TYPE_META, meta, txTitle, txSubtitle, txRow };

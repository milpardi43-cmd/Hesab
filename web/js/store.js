import { daysAgo, jalaliParts, todayIso } from './format.js';

const STORAGE_KEY = 'hesabyar-browser-demo-v1';

export const state = {
  business: null,
  accounts: [],
  products: [],
  contacts: [],
  transactions: [],
  reminders: [],
  settings: {},
  ui: { query: '' }
};

function uid(prefix) {
  return `${prefix}_${Math.random().toString(36).slice(2, 9)}${Date.now().toString(36).slice(-4)}`;
}

function seed() {
  const accounts = [
    { id: 'cash', title: 'صندوق', type: 'نقدی', balance: 18_450_000 },
    { id: 'bank', title: 'بانک ملت', type: 'بانک', balance: 42_800_000 },
    { id: 'pos', title: 'کارتخوان', type: 'کارتخوان', balance: 6_250_000 }
  ];

  const products = [
    { id: 'p1', name: 'روغن آفتابگردان ۱٫۸ لیتری', category: 'خواربار', purchasePrice: 720_000, sellingPrice: 890_000, averageCost: 725_000, stock: 18, minimumStock: 5, barcode: '6260101234567' },
    { id: 'p2', name: 'برنج ایرانی هاشمی ۵ کیلویی', category: 'خواربار', purchasePrice: 3_850_000, sellingPrice: 4_450_000, averageCost: 3_870_000, stock: 7, minimumStock: 4, barcode: '6260107654321' },
    { id: 'p3', name: 'چای کیسه‌ای ۱۰۰ عددی', category: 'نوشیدنی', purchasePrice: 480_000, sellingPrice: 620_000, averageCost: 485_000, stock: 3, minimumStock: 6, barcode: '6260101112223' },
    { id: 'p4', name: 'شیر پرچرب ۱ لیتری', category: 'لبنیات', purchasePrice: 320_000, sellingPrice: 395_000, averageCost: 322_000, stock: 24, minimumStock: 10, barcode: '6260103334445' },
    { id: 'p5', name: 'دستمال کاغذی ۳۰۰ برگ', category: 'بهداشتی', purchasePrice: 210_000, sellingPrice: 285_000, averageCost: 212_000, stock: 42, minimumStock: 12, barcode: '6260105556667' },
    { id: 'p6', name: 'پنیر سفید ۴۰۰ گرمی', category: 'لبنیات', purchasePrice: 265_000, sellingPrice: 340_000, averageCost: 268_000, stock: 9, minimumStock: 8, barcode: '6260107778889' }
  ];

  const contacts = [
    { id: 'c1', name: 'علی رضایی', phone: '09121234567', type: 'customer', balance: 4_250_000, note: 'مشتری قدیمی محله' },
    { id: 'c2', name: 'مریم احمدی', phone: '09351112233', type: 'customer', balance: 1_180_000, note: '' },
    { id: 'c3', name: 'سارا محمدی', phone: '09197778899', type: 'customer', balance: 4_450_000, note: 'خرید نسیه دو روز پیش' },
    { id: 'c4', name: 'پخش مواد غذایی البرز', phone: '02155443322', type: 'supplier', balance: -8_900_000, note: 'تسویه هفتگی' },
    { id: 'c5', name: 'شرکت لبنیات دامداران', phone: '02177889900', type: 'supplier', balance: -2_350_000, note: '' }
  ];

  const transactions = [
    {
      id: 't1', code: 'T-14050823-0006', type: 'sale', date: daysAgo(0, 12),
      items: [{ productId: 'p1', name: products[0].name, qty: 2, price: 890_000, cost: 725_000 }],
      subtotal: 1_780_000, discount: 0, total: 1_780_000, paid: 1_780_000,
      accountId: 'cash', contactId: null, mode: 'cash', note: 'فروش روزانه'
    },
    {
      id: 't2', code: 'T-14050823-0005', type: 'sale', date: daysAgo(0, 10),
      items: [{ productId: 'p2', name: products[1].name, qty: 1, price: 4_450_000, cost: 3_870_000 }],
      subtotal: 4_450_000, discount: 0, total: 4_450_000, paid: 500_000,
      accountId: 'cash', contactId: 'c1', mode: 'mixed', note: 'خرید خانوادگی'
    },
    {
      id: 't3', code: 'E-14050823-0004', type: 'expense', date: daysAgo(0, 9),
      title: 'کرایه حمل بار', category: 'حمل‌ونقل', amount: 350_000, paid: 350_000,
      accountId: 'cash', note: ''
    },
    {
      id: 't4', code: 'R-14050823-0003', type: 'receipt', date: daysAgo(0, 8),
      amount: 1_000_000, paid: 1_000_000, accountId: 'cash', contactId: 'c2', note: 'تسویه بخشی از بدهی'
    },
    {
      id: 't5', code: 'P-14050822-0007', type: 'purchase', date: daysAgo(1, 16),
      items: [
        { productId: 'p1', name: products[0].name, qty: 12, price: 720_000 },
        { productId: 'p4', name: products[3].name, qty: 20, price: 320_000 }
      ],
      subtotal: 15_040_000, discount: 0, freight: 240_000, total: 15_280_000, paid: 3_600_000,
      accountId: 'bank', contactId: 'c4', note: 'خرید هفتگی'
    },
    {
      id: 't6', code: 'T-14050822-0006', type: 'sale', date: daysAgo(1, 13),
      items: [{ productId: 'p5', name: products[4].name, qty: 3, price: 285_000, cost: 212_000 }],
      subtotal: 855_000, discount: 0, total: 855_000, paid: 855_000,
      accountId: 'pos', contactId: null, mode: 'cash', note: ''
    },
    {
      id: 't7', code: 'E-14050822-0005', type: 'expense', date: daysAgo(1, 11),
      title: 'اجاره ماهانه مغازه', category: 'اجاره', amount: 12_000_000, paid: 12_000_000,
      accountId: 'bank', note: 'اجاره آبان'
    },
    {
      id: 't8', code: 'R-14050822-0004', type: 'receipt', date: daysAgo(1, 10),
      amount: 2_000_000, paid: 2_000_000, accountId: 'bank', contactId: 'c1', note: ''
    },
    {
      id: 't9', code: 'T-14050821-0003', type: 'sale', date: daysAgo(2, 15),
      items: [{ productId: 'p2', name: products[1].name, qty: 1, price: 4_450_000, cost: 3_870_000 }],
      subtotal: 4_450_000, discount: 0, total: 4_450_000, paid: 0,
      accountId: null, contactId: 'c3', mode: 'credit', note: 'نسیه تا آخر هفته'
    },
    {
      id: 't10', code: 'S-14050821-0002', type: 'payment', date: daysAgo(2, 12),
      amount: 3_000_000, paid: 3_000_000, accountId: 'bank', contactId: 'c5', note: 'پرداخت به تأمین‌کننده'
    },
    {
      id: 't11', code: 'X-14050820-0001', type: 'transfer', date: daysAgo(3, 17),
      amount: 5_000_000, paid: 5_000_000, fromId: 'bank', toId: 'cash', note: 'تقویت صندوق'
    }
  ];

  const reminders = [
    { id: 'r1', title: 'تسویه بدهی علی رضایی', contactId: 'c1', amount: 4_250_000, due: daysAgo(-2, 10), done: false },
    { id: 'r2', title: 'پرداخت به پخش البرز', contactId: 'c4', amount: 8_900_000, due: daysAgo(-5, 10), done: false },
    { id: 'r3', title: 'تماس با مریم احمدی برای مانده حساب', contactId: 'c2', amount: 1_180_000, due: daysAgo(1, 10), done: false },
    { id: 'r4', title: 'سفارش شیر و لبنیات هفته آینده', contactId: 'c5', amount: 0, due: daysAgo(-7, 10), done: false }
  ];

  return {
    business: { name: 'فروشگاه آفتاب', owner: 'رضا کریمی', phone: '09123456789', city: 'تهران', type: 'فروشگاه مواد غذایی' },
    accounts,
    products,
    contacts,
    transactions,
    reminders,
    settings: { persianDigits: true, fontSize: 1, lowStockAlerts: true, notifications: true }
  };
}

export function load() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) {
      const parsed = JSON.parse(raw);
      Object.assign(state, seed(), parsed, { ui: { query: '' } });
      return;
    }
  } catch (error) {
    console.warn('خواندن داده‌های محلی ناموفق بود', error);
  }
  Object.assign(state, seed(), { ui: { query: '' } });
  save();
}

export function save() {
  try {
    const { ui, ...persisted } = state;
    localStorage.setItem(STORAGE_KEY, JSON.stringify(persisted));
  } catch (error) {
    console.warn('ذخیره‌سازی محلی ناموفق بود', error);
  }
}

export function reset() {
  Object.assign(state, seed(), { ui: { query: '' } });
  save();
}

export function exportJson() {
  const { ui, ...persisted } = state;
  return JSON.stringify({ app: 'hesabyar-demo', version: 1, exportedAt: new Date().toISOString(), data: persisted }, null, 2);
}

export function importJson(text) {
  const parsed = JSON.parse(text);
  const payload = parsed?.data ?? parsed;
  if (!payload || !Array.isArray(payload.transactions) || !Array.isArray(payload.products)) {
    throw new Error('ساختار فایل پشتیبان معتبر نیست');
  }
  Object.assign(state, seed(), payload, { ui: { query: '' } });
  save();
}

export function getAccount(id) {
  return state.accounts.find((account) => account.id === id);
}

export function getContact(id) {
  return state.contacts.find((contact) => contact.id === id);
}

export function getProduct(id) {
  return state.products.find((product) => product.id === id);
}

function nextCode(prefix, date = new Date()) {
  const parts = jalaliParts(date.toISOString());
  const dayPrefix = `${prefix}-${parts.year}${parts.month}${parts.day}-`;
  const last = state.transactions
    .filter((tx) => tx.code.startsWith(dayPrefix))
    .map((tx) => Number(tx.code.split('-').at(-1)))
    .filter((value) => Number.isFinite(value));
  const next = (last.length ? Math.max(...last) : 0) + 1;
  return `${dayPrefix}${String(next).padStart(4, '0')}`;
}

function addTransaction(tx) {
  state.transactions.unshift(tx);
  save();
  return tx;
}

export function addSale({ items, discount = 0, mode = 'cash', paid = 0, contactId = null, accountId = 'cash', note = '', date = todayIso() }) {
  if (!items.length) throw new Error('حداقل یک کالا انتخاب کنید');
  const subtotal = items.reduce((sum, item) => sum + item.qty * item.price, 0);
  const safeDiscount = Math.min(subtotal, Math.max(0, discount));
  const total = subtotal - safeDiscount;
  const paidAmount = mode === 'cash' ? total : mode === 'credit' ? 0 : Math.min(total, Math.max(0, paid));
  const remaining = total - paidAmount;
  if (mode !== 'cash' && remaining > 0 && !contactId) throw new Error('برای فروش نسیه، مشتری را انتخاب کنید');
  const account = getAccount(accountId);
  if (paidAmount > 0 && !account) throw new Error('حساب دریافت را انتخاب کنید');

  items.forEach((item) => {
    const product = getProduct(item.productId);
    if (product && item.qty > product.stock) throw new Error(`موجودی «${product.name}» کافی نیست`);
  });
  items.forEach((item) => {
    const product = getProduct(item.productId);
    if (!product) return;
    product.stock = Math.max(0, Math.round((product.stock - item.qty) * 1000) / 1000);
  });

  if (account) account.balance += paidAmount;
  const contact = getContact(contactId);
  if (contact) contact.balance += remaining;

  return addTransaction({
    id: uid('t'),
    code: nextCode('T'),
    type: 'sale',
    date,
    items: items.map((item) => ({
      productId: item.productId,
      name: getProduct(item.productId)?.name ?? item.name,
      qty: item.qty,
      price: item.price,
      cost: getProduct(item.productId)?.averageCost ?? 0
    })),
    subtotal,
    discount: safeDiscount,
    total,
    paid: paidAmount,
    accountId: paidAmount > 0 ? accountId : null,
    contactId,
    mode,
    note
  });
}

export function addPurchase({ items, discount = 0, freight = 0, paid = 0, contactId = null, accountId = 'bank', note = '', date = todayIso() }) {
  if (!items.length) throw new Error('حداقل یک کالا اضافه کنید');
  if (!contactId) throw new Error('تأمین‌کننده را انتخاب کنید');
  const subtotal = items.reduce((sum, item) => sum + item.qty * item.price, 0);
  const safeDiscount = Math.min(subtotal, Math.max(0, discount));
  const total = subtotal + freight - safeDiscount;
  const paidAmount = Math.min(total, Math.max(0, paid));
  const remaining = total - paidAmount;
  const account = getAccount(accountId);
  if (paidAmount > 0 && !account) throw new Error('حساب پرداخت را انتخاب کنید');

  items.forEach((item) => {
    const product = getProduct(item.productId);
    if (!product) return;
    const lineShare = subtotal > 0 ? (item.qty * item.price) / subtotal : 0;
    const landedCost = item.qty * item.price + freight * lineShare;
    const oldValue = product.stock * product.averageCost;
    const newStock = product.stock + item.qty;
    product.averageCost = newStock > 0 ? Math.round((oldValue + landedCost) / newStock) : item.price;
    product.stock = Math.round(newStock * 1000) / 1000;
    product.purchasePrice = item.price;
  });

  if (account) account.balance -= paidAmount;
  const contact = getContact(contactId);
  if (contact) contact.balance -= remaining;

  return addTransaction({
    id: uid('t'),
    code: nextCode('P'),
    type: 'purchase',
    date,
    items: items.map((item) => ({
      productId: item.productId,
      name: getProduct(item.productId)?.name ?? item.name,
      qty: item.qty,
      price: item.price
    })),
    subtotal,
    discount: safeDiscount,
    freight,
    total,
    paid: paidAmount,
    accountId: paidAmount > 0 ? accountId : null,
    contactId,
    note
  });
}

export function addExpense({ title, category, amount, accountId = 'cash', note = '', date = todayIso() }) {
  if (!title.trim()) throw new Error('عنوان هزینه را وارد کنید');
  if (amount <= 0) throw new Error('مبلغ هزینه را وارد کنید');
  const account = getAccount(accountId);
  if (!account) throw new Error('حساب پرداخت را انتخاب کنید');
  account.balance -= amount;
  return addTransaction({
    id: uid('t'),
    code: nextCode('E'),
    type: 'expense',
    date,
    title,
    category,
    amount,
    paid: amount,
    accountId,
    note
  });
}

export function addPayment({ mode, contactId, amount, accountId = 'cash', note = '', date = todayIso() }) {
  const contact = getContact(contactId);
  if (!contact) throw new Error('شخص را انتخاب کنید');
  if (amount <= 0) throw new Error('مبلغ را وارد کنید');
  const account = getAccount(accountId);
  if (!account) throw new Error('حساب را انتخاب کنید');
  if (mode === 'receive') {
    account.balance += amount;
    contact.balance -= amount;
  } else {
    account.balance -= amount;
    contact.balance += amount;
  }
  return addTransaction({
    id: uid('t'),
    code: nextCode(mode === 'receive' ? 'R' : 'S'),
    type: mode === 'receive' ? 'receipt' : 'payment',
    date,
    amount,
    paid: amount,
    accountId,
    contactId,
    note
  });
}

export function transfer({ fromId, toId, amount, note = '', date = todayIso() }) {
  if (fromId === toId) throw new Error('حساب مبدأ و مقصد نباید یکی باشند');
  if (amount <= 0) throw new Error('مبلغ انتقال را وارد کنید');
  const from = getAccount(fromId);
  const to = getAccount(toId);
  if (!from || !to) throw new Error('حساب‌ها را انتخاب کنید');
  from.balance -= amount;
  to.balance += amount;
  return addTransaction({
    id: uid('t'),
    code: nextCode('X'),
    type: 'transfer',
    date,
    amount,
    paid: amount,
    fromId,
    toId,
    note
  });
}

export function adjustCash({ accountId, actual, reason = '', date = todayIso() }) {
  const account = getAccount(accountId);
  if (!account) throw new Error('حساب را انتخاب کنید');
  const delta = actual - account.balance;
  if (delta === 0) throw new Error('موجودی شمارش‌شده با موجودی فعلی برابر است');
  account.balance = actual;
  return addTransaction({
    id: uid('t'),
    code: nextCode('A'),
    type: 'adjust',
    date,
    amount: Math.abs(delta),
    paid: 0,
    accountId,
    note: reason || (delta > 0 ? 'اضافه‌ی صندوق' : 'کسری صندوق'),
    delta
  });
}

export function saveProduct(data, id = null) {
  if (!data.name?.trim()) throw new Error('نام کالا را وارد کنید');
  if (id) {
    const product = getProduct(id);
    if (!product) throw new Error('کالا پیدا نشد');
    Object.assign(product, data, { id });
  } else {
    state.products.unshift({
      id: uid('p'),
      name: data.name.trim(),
      category: data.category || 'عمومی',
      purchasePrice: data.purchasePrice || 0,
      sellingPrice: data.sellingPrice || 0,
      averageCost: data.averageCost || data.purchasePrice || 0,
      stock: data.stock || 0,
      minimumStock: data.minimumStock || 0,
      barcode: data.barcode || ''
    });
  }
  save();
}

export function saveContact(data, id = null) {
  if (!data.name?.trim()) throw new Error('نام شخص را وارد کنید');
  if (id) {
    const contact = getContact(id);
    if (!contact) throw new Error('شخص پیدا نشد');
    Object.assign(contact, data, { id });
  } else {
    state.contacts.unshift({
      id: uid('c'),
      name: data.name.trim(),
      phone: data.phone || '',
      type: data.type || 'customer',
      balance: data.balance || 0,
      note: data.note || ''
    });
  }
  save();
}

export function addReminder({ title, contactId = null, amount = 0, due }) {
  if (!title.trim()) throw new Error('عنوان یادآوری را وارد کنید');
  state.reminders.unshift({ id: uid('r'), title: title.trim(), contactId, amount, due, done: false });
  save();
}

export function toggleReminder(id) {
  const reminder = state.reminders.find((item) => item.id === id);
  if (reminder) {
    reminder.done = !reminder.done;
    save();
  }
}

export function removeReminder(id) {
  state.reminders = state.reminders.filter((item) => item.id !== id);
  save();
}

export function setSettings(patch) {
  Object.assign(state.settings, patch);
  save();
}

export function totals() {
  const today = state.transactions.filter((tx) => tx.type === 'sale' && tx.date.startsWith(new Date().toISOString().slice(0, 10)));
  return today;
}

export function dashboardStats() {
  const salesToday = state.transactions.filter((tx) => tx.type === 'sale' && isDateToday(tx.date));
  const receivedToday = state.transactions.filter((tx) => tx.type === 'receipt' && isDateToday(tx.date));
  const expensesToday = state.transactions.filter((tx) => tx.type === 'expense' && isDateToday(tx.date));
  const salesTotal = salesToday.reduce((sum, tx) => sum + tx.total, 0);
  const receivedTotal = receivedToday.reduce((sum, tx) => sum + tx.amount, 0) +
    salesToday.reduce((sum, tx) => sum + tx.paid, 0);
  const expenseTotal = expensesToday.reduce((sum, tx) => sum + tx.amount, 0);
  const cogs = salesToday.reduce((sum, tx) => sum + tx.items.reduce((line, item) => line + item.qty * item.cost, 0), 0);
  return {
    salesTotal,
    receivedTotal,
    expenseTotal,
    profit: salesTotal - cogs - expenseTotal,
    receivable: state.contacts.filter((contact) => contact.balance > 0).reduce((sum, contact) => sum + contact.balance, 0),
    payable: state.contacts.filter((contact) => contact.balance < 0).reduce((sum, contact) => sum - contact.balance, 0),
    cash: state.accounts.reduce((sum, account) => sum + account.balance, 0),
    lowStock: state.products.filter((product) => product.minimumStock > 0 && product.stock <= product.minimumStock),
    salesCount: salesToday.length
  };
}

function isDateToday(iso) {
  const date = new Date(iso);
  const now = new Date();
  return date.getFullYear() === now.getFullYear() && date.getMonth() === now.getMonth() && date.getDate() === now.getDate();
}

export function salesSeries(days = 7) {
  const series = [];
  for (let offset = days - 1; offset >= 0; offset -= 1) {
    const date = new Date();
    date.setDate(date.getDate() - offset);
    date.setHours(0, 0, 0, 0);
    const next = new Date(date);
    next.setDate(next.getDate() + 1);
    const total = state.transactions
      .filter((tx) => tx.type === 'sale')
      .filter((tx) => {
        const time = new Date(tx.date).getTime();
        return time >= date.getTime() && time < next.getTime();
      })
      .reduce((sum, tx) => sum + tx.total, 0);
    series.push({ date: date.toISOString(), total });
  }
  return series;
}

export function monthlySeries() {
  const series = [];
  const now = new Date();
  for (let offset = 5; offset >= 0; offset -= 1) {
    const date = new Date(now.getFullYear(), now.getMonth() - offset, 1);
    const next = new Date(now.getFullYear(), now.getMonth() - offset + 1, 1);
    const sales = state.transactions.filter((tx) => tx.type === 'sale').filter((tx) => {
      const time = new Date(tx.date).getTime();
      return time >= date.getTime() && time < next.getTime();
    });
    const purchases = state.transactions.filter((tx) => tx.type === 'purchase').filter((tx) => {
      const time = new Date(tx.date).getTime();
      return time >= date.getTime() && time < next.getTime();
    });
    const expenses = state.transactions.filter((tx) => tx.type === 'expense').filter((tx) => {
      const time = new Date(tx.date).getTime();
      return time >= date.getTime() && time < next.getTime();
    });
    series.push({
      date: date.toISOString(),
      sales: sales.reduce((sum, tx) => sum + tx.total, 0),
      purchases: purchases.reduce((sum, tx) => sum + tx.total, 0),
      expenses: expenses.reduce((sum, tx) => sum + tx.amount, 0)
    });
  }
  return series;
}

export function topProducts(limit = 5) {
  const map = new Map();
  state.transactions.filter((tx) => tx.type === 'sale').forEach((tx) => {
    tx.items.forEach((item) => {
      const current = map.get(item.productId) ?? { name: item.name, qty: 0, amount: 0, profit: 0 };
      current.qty += item.qty;
      current.amount += item.qty * item.price;
      current.profit += item.qty * (item.price - item.cost);
      map.set(item.productId, current);
    });
  });
  return [...map.values()].sort((a, b) => b.qty - a.qty).slice(0, limit);
}

export function contactTransactions(contactId) {
  return state.transactions.filter((tx) => tx.contactId === contactId);
}

export function accountTransactions(accountId) {
  return state.transactions.filter((tx) => tx.accountId === accountId || tx.fromId === accountId || tx.toId === accountId);
}

export function productMovements(productId) {
  return state.transactions
    .filter((tx) => ['sale', 'purchase'].includes(tx.type) && tx.items?.some((item) => item.productId === productId))
    .flatMap((tx) => tx.items.filter((item) => item.productId === productId).map((item) => ({
      id: `${tx.id}-${item.productId}`,
      date: tx.date,
      code: tx.code,
      type: tx.type,
      qty: item.qty,
      price: item.price,
      direction: tx.type === 'sale' ? 'out' : 'in'
    })))
    .sort((a, b) => new Date(b.date) - new Date(a.date));
}

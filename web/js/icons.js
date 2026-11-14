export function icon(name, className = '') {
  return `<svg class="icon ${className}" aria-hidden="true"><use href="#i-${name}"></use></svg>`;
}

export const navItems = [
  { id: 'dashboard', label: 'داشبورد', icon: 'home' },
  { id: 'sales', label: 'فروش', icon: 'cart' },
  { id: 'purchases', label: 'خرید', icon: 'truck' },
  { id: 'expenses', label: 'هزینه‌ها', icon: 'receipt' },
  { id: 'products', label: 'کالاها و انبار', icon: 'box' },
  { id: 'contacts', label: 'اشخاص', icon: 'users' },
  { id: 'accounts', label: 'حساب‌ها', icon: 'wallet' },
  { id: 'reports', label: 'گزارش‌ها', icon: 'chart' },
  { id: 'reminders', label: 'یادآوری‌ها', icon: 'bell' },
  { id: 'settings', label: 'تنظیمات', icon: 'settings' }
];

export const mobileNav = ['dashboard', 'sales', 'products', 'contacts', 'reports'];

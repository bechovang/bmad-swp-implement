/**
 * StorageHub "Control Room" Design Tokens
 * Source of truth: docs/planning/ux-designs/ux-swp391-2026-09-11/DESIGN.md
 */

export const SH_COLORS = {
  // Surface & structure ramp (6 levels + divider)
  appBg: '#F4F6F9',
  surface: '#FFFFFF',
  surfaceSubtle: '#F8FAFC',
  surfaceMuted: '#F1F5F9',
  border: '#E2E8F0',
  borderStrong: '#CBD5E1',
  divider: '#F1F5F9',

  // Text ramp (4 levels)
  ink: '#0F172A',
  inkSecondary: '#334155',
  muted: '#64748B',
  faint: '#94A3B8',

  // Brand
  primary: '#4F46E5',
  primaryTint: '#EEF2FF',
  primaryBorder: '#E0E7FF',
  primaryOutlineBorder: '#C7D2FE',

  // Unit lifecycle status semantics
  statusAvailable: '#059669',
  statusAvailableTint: '#ECFDF5',
  statusAvailableBorder: '#D1FAE5',

  statusBuffer: '#B45309',
  statusBufferBar: '#F59E0B',
  statusBufferTint: '#FFFBEB',
  statusBufferBorder: '#FDE68A',

  statusReserved: '#4F46E5',
  statusReservedTint: '#EEF2FF',
  statusReservedBorder: '#E0E7FF',

  statusRented: '#334155',
  statusRentedTint: '#F1F5F9',
  statusRentedBorder: '#E2E8F0',

  statusPreparing: '#0284C7',
  statusPreparingTint: '#F0F9FF',
  statusPreparingBorder: '#E0F2FE',

  statusMaintenance: '#C2410C',
  statusMaintenanceTint: '#FFF7ED',
  statusMaintenanceBorder: '#FFEDD5',

  statusRetired: '#94A3B8',
  statusRetiredTint: '#F8FAFC',
  statusRetiredBorder: '#E2E8F0',

  // System feedback
  success: '#059669',
  successTint: '#ECFDF5',
  warning: '#B45309',
  warningBar: '#F59E0B',
  warningTint: '#FFFBEB',
  error: '#DC2626',
  errorTint: '#FEF2F2',
  errorBorder: '#FECACA',
} as const

export const SH_TYPOGRAPHY = {
  display: {
    fontSize: '21px',
    fontWeight: '600',
    lineHeight: '1.3',
    letterSpacing: '-0.01em',
  },
  kpiValue: {
    fontSize: '24px',
    fontWeight: '600',
    lineHeight: '1.2',
    letterSpacing: '-0.01em',
  },
  headline: {
    fontSize: '18px',
    fontWeight: '600',
    lineHeight: '1.35',
  },
  body: {
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif',
    fontSize: '13px',
    fontWeight: '400',
    lineHeight: '1.5',
  },
  bodyStrong: {
    fontSize: '13px',
    fontWeight: '600',
    lineHeight: '1.5',
  },
  meta: {
    fontSize: '11.5px',
    fontWeight: '500',
    lineHeight: '1.45',
  },
  label: {
    fontSize: '10.5px',
    fontWeight: '600',
    lineHeight: '1.4',
  },
  button: {
    fontSize: '13px',
    fontWeight: '600',
  },
  nav: {
    fontSize: '13px',
    fontWeight: '500',
  },
  price: {
    fontSize: '15px',
    fontWeight: '600',
    lineHeight: '1.2',
    letterSpacing: '-0.01em',
  },
  priceLg: {
    fontSize: '19px',
    fontWeight: '600',
    lineHeight: '1.2',
    letterSpacing: '-0.01em',
  },
  code: {
    fontFamily: 'ui-monospace, SFMono-Regular, Menlo, Consolas, monospace',
    fontSize: '13px',
    fontWeight: '600',
  },
  codeSm: {
    fontFamily: 'ui-monospace, SFMono-Regular, Menlo, Consolas, monospace',
    fontSize: '11px',
    fontWeight: '600',
  },
} as const

export const SH_RADII = {
  sm: '6px',
  md: '8px',
  full: '9999px',
} as const

export const SH_SPACING = {
  '1': '4px',
  '2': '8px',
  '3': '12px',
  '4': '16px',
  '5': '24px',
  '6': '32px',
  '7': '48px',
  pageX: '24px',
  navHeight: '54px',
} as const

export const SH_SHADOWS = {
  card: '0 1px 2px rgba(15,23,42,.05)',
  buttonPrimary: '0 1px 2px rgba(79,70,229,.35)',
  overlay: '0 12px 32px rgba(15,23,42,.14)',
  scrim: 'rgba(15,23,42,.4)',
} as const

export type UnitStatus =
  | 'AVAILABLE'
  | 'BUFFER'
  | 'RESERVED'
  | 'RENTED'
  | 'PREPARING'
  | 'MAINTENANCE'
  | 'RETIRED'

export type UnitStatusKey =
  | 'available'
  | 'buffer'
  | 'reserved'
  | 'rented'
  | 'preparing'
  | 'maintenance'
  | 'retired'

export interface StatusColorInfo {
  key: UnitStatusKey
  label: string
  dot: string
  bar: string
  text: string
  bg: string
  border: string
}

export const STATUS_MAP: Record<UnitStatusKey, StatusColorInfo> = {
  available: {
    key: 'available',
    label: 'Available',
    dot: SH_COLORS.statusAvailable,
    bar: SH_COLORS.statusAvailable,
    text: SH_COLORS.statusAvailable,
    bg: SH_COLORS.statusAvailableTint,
    border: SH_COLORS.statusAvailableBorder,
  },
  buffer: {
    key: 'buffer',
    label: 'Available soon (buffer)',
    dot: SH_COLORS.statusBufferBar,
    bar: SH_COLORS.statusBufferBar,
    text: SH_COLORS.statusBuffer,
    bg: SH_COLORS.statusBufferTint,
    border: SH_COLORS.statusBufferBorder,
  },
  reserved: {
    key: 'reserved',
    label: 'Reserved',
    dot: SH_COLORS.statusReserved,
    bar: SH_COLORS.statusReserved,
    text: SH_COLORS.statusReserved,
    bg: SH_COLORS.statusReservedTint,
    border: SH_COLORS.statusReservedBorder,
  },
  rented: {
    key: 'rented',
    label: 'Rented',
    dot: SH_COLORS.statusRented,
    bar: SH_COLORS.statusRented,
    text: SH_COLORS.statusRented,
    bg: SH_COLORS.statusRentedTint,
    border: SH_COLORS.statusRentedBorder,
  },
  preparing: {
    key: 'preparing',
    label: 'Preparing',
    dot: SH_COLORS.statusPreparing,
    bar: SH_COLORS.statusPreparing,
    text: SH_COLORS.statusPreparing,
    bg: SH_COLORS.statusPreparingTint,
    border: SH_COLORS.statusPreparingBorder,
  },
  maintenance: {
    key: 'maintenance',
    label: 'Maintenance',
    dot: SH_COLORS.statusMaintenance,
    bar: SH_COLORS.statusMaintenance,
    text: SH_COLORS.statusMaintenance,
    bg: SH_COLORS.statusMaintenanceTint,
    border: SH_COLORS.statusMaintenanceBorder,
  },
  retired: {
    key: 'retired',
    label: 'Retired',
    dot: SH_COLORS.statusRetired,
    bar: SH_COLORS.statusRetired,
    text: SH_COLORS.statusRetired,
    bg: SH_COLORS.statusRetiredTint,
    border: SH_COLORS.statusRetiredBorder,
  },
} as const

export function normalizeStatus(
  status: string | null | undefined
): UnitStatusKey | undefined {
  if (!status || typeof status !== 'string') return undefined
  const s = status.toLowerCase().trim()
  if (s.includes('buffer')) return 'buffer'
  if (s.includes('avail')) return 'available'
  if (s.includes('reserv')) return 'reserved'
  if (s.includes('rent')) return 'rented'
  if (s.includes('prep')) return 'preparing'
  if (s.includes('maint')) return 'maintenance'
  if (s.includes('retir')) return 'retired'
  return undefined
}

export function getStatusColorInfo(
  status: string | null | undefined
): StatusColorInfo | undefined {
  const key = normalizeStatus(status)
  return key ? STATUS_MAP[key] : undefined
}

export type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'destructive'
export type ButtonSize = 'default' | 'page'

export type AppRole =
  | 'CUSTOMER'
  | 'STAFF'
  | 'FACILITY_MANAGER'
  | 'BUSINESS_OPS'
  | 'SYSTEM_ADMINISTRATOR'

export interface RoleConfig {
  role: AppRole
  label: string
  landing: string
  screen: string
}

export const APP_ROLES: Record<AppRole, RoleConfig> = {
  CUSTOMER: {
    role: 'CUSTOMER',
    label: 'Customer',
    landing: '/units',
    screen: 'Browse Units',
  },
  STAFF: {
    role: 'STAFF',
    label: 'Staff',
    landing: '/tasks',
    screen: 'Task Board',
  },
  FACILITY_MANAGER: {
    role: 'FACILITY_MANAGER',
    label: 'Facility Manager',
    landing: '/overview',
    screen: 'Facility Overview',
  },
  BUSINESS_OPS: {
    role: 'BUSINESS_OPS',
    label: 'Business Ops',
    landing: '/business-overview',
    screen: 'Business Overview',
  },
  SYSTEM_ADMINISTRATOR: {
    role: 'SYSTEM_ADMINISTRATOR',
    label: 'System Administrator',
    landing: '/users',
    screen: 'User Management',
  },
} as const

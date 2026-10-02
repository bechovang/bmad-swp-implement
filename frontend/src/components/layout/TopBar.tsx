import { useState, useRef, useEffect } from 'react'
import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'
import { RoleChip } from '../ui/RoleChip'

import { ROLE_NAV_ITEMS } from '../../constants/navigation'

export function TopBar() {
  const { user, logout, getRoleLanding } = useAuth()
  const navigate = useNavigate()
  const [menuOpen, setMenuOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)

  const landingPath = user ? getRoleLanding(user.role) : '/login'
  const navItems = user?.role ? ROLE_NAV_ITEMS[user.role] || [] : []

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setMenuOpen(false)
      }
    }
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setMenuOpen(false)
      }
    }
    if (menuOpen) {
      document.addEventListener('mousedown', handleClickOutside)
      document.addEventListener('keydown', handleKeyDown)
    }
    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
      document.removeEventListener('keydown', handleKeyDown)
    }
  }, [menuOpen])

  const handleLogout = () => {
    setMenuOpen(false)
    logout()
    navigate('/login')
  }

  const initial = user?.fullName?.trim() ? user.fullName.trim().charAt(0).toUpperCase() : 'U'

  return (
    <header className="h-[54px] bg-sh-surface border-b border-sh-border px-sh-page-x flex items-center justify-between sticky top-0 z-40">
      <div className="flex items-center gap-8 h-full min-w-0">
        {/* Brand Logo */}
        <Link
          to={landingPath}
          className="flex items-center gap-2 text-sh-ink font-semibold typography-headline hover:opacity-90 transition-opacity shrink-0"
        >
          <span className="w-7 h-7 rounded-sh-md bg-sh-primary flex items-center justify-center text-white text-xs font-bold">
            SH
          </span>
          <span>StorageHub</span>
        </Link>

        {/* Dynamic Per-role Nav */}
        <nav aria-label="Main Navigation" className="flex items-center gap-6 h-full overflow-x-auto">
          {navItems.map((item) => (
            <NavLink
              key={item.id}
              to={item.path}
              className={({ isActive }) =>
                isActive
                  ? 'text-sh-primary font-semibold typography-nav flex items-center h-full relative after:absolute after:bottom-0 after:left-0 after:right-0 after:h-[2px] after:bg-sh-primary shrink-0'
                  : 'text-sh-muted hover:text-sh-ink font-medium typography-nav flex items-center h-full transition-colors shrink-0'
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </div>

      {/* Right controls: RoleChip, Notifications Bell, Avatar Menu */}
      <div className="flex items-center gap-4 shrink-0">
        {user && <RoleChip role={user.role} />}

        {/* Notifications Bell Placeholder */}
        <Link
          to="/notifications"
          aria-label="Notifications"
          className="p-1.5 text-sh-muted hover:text-sh-ink rounded-sh-md transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sh-primary"
        >
          <svg
            className="w-5 h-5"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            strokeWidth="2"
            aria-hidden="true"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"
            />
          </svg>
        </Link>

        {/* Avatar Menu Popover */}
        <div className="relative" ref={menuRef}>
          <button
            type="button"
            aria-label="User menu"
            aria-haspopup="true"
            aria-expanded={menuOpen}
            onClick={() => setMenuOpen((prev) => !prev)}
            className="w-8 h-8 rounded-sh-md bg-sh-surface-muted text-sh-ink font-semibold flex items-center justify-center text-xs border border-sh-border hover:border-sh-border-strong focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sh-primary cursor-pointer transition-colors"
          >
            {initial}
          </button>

          {menuOpen && (
            <div
              role="menu"
              aria-orientation="vertical"
              className="absolute right-0 mt-2 w-56 bg-sh-surface border border-sh-border rounded-sh-md shadow-sh-overlay py-2 z-50 animate-in fade-in-0 zoom-in-95 duration-100"
            >
              <div className="px-3 py-2 border-b border-sh-divider">
                <p className="typography-body-strong text-sh-ink truncate">{user?.fullName}</p>
                <p className="typography-meta text-sh-muted truncate">{user?.email}</p>
              </div>

              <div className="py-1">
                <Link
                  to="/profile"
                  role="menuitem"
                  onClick={() => setMenuOpen(false)}
                  className="block px-3 py-1.5 text-[13px] text-sh-ink-secondary hover:bg-sh-surface-subtle hover:text-sh-ink transition-colors"
                >
                  Profile
                </Link>
              </div>

              <div className="pt-1 border-t border-sh-divider">
                <button
                  type="button"
                  role="menuitem"
                  onClick={handleLogout}
                  className="w-full text-left px-3 py-1.5 text-[13px] text-sh-error hover:bg-sh-error-tint transition-colors font-medium cursor-pointer"
                >
                  Log out
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </header>
  )
}

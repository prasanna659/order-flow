import { useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import {
  ShoppingCart, LogOut, LayoutDashboard, Network, Package,
  ActivitySquare, Menu, X, User, ClipboardList
} from 'lucide-react'

const NAV_LINKS = [
  { to: '/',             label: 'Dashboard',    icon: LayoutDashboard, auth: false },
  { to: '/architecture', label: 'Architecture', icon: Network,         auth: false },
  { to: '/catalog',      label: 'Catalog',      icon: Package,         auth: true  },
  { to: '/orders',       label: 'Orders',       icon: ClipboardList,   auth: true  },
  { to: '/health',       label: 'System Health',icon: ActivitySquare,  auth: false },
]

export default function Navbar({ username, cartCount, onCartClick, onLogout }) {
  const location = useLocation()
  const [mobileOpen, setMobileOpen] = useState(false)

  const visibleLinks = username
    ? NAV_LINKS
    : NAV_LINKS.filter((l) => !l.auth)

  return (
    <nav className="sticky top-0 z-40 border-b border-border bg-ink/90 backdrop-blur-md">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 h-14 flex items-center justify-between gap-4">

        {/* Logo */}
        <Link to="/" className="font-display font-bold text-base tracking-tight flex items-center gap-2 shrink-0">
          <div className="w-7 h-7 rounded-lg bg-gradient-to-br from-primary to-primarylight flex items-center justify-center shadow-glow">
            <span className="font-display font-bold text-white text-xs">O</span>
          </div>
          <span>OrderFlow</span>
          <span className="hidden sm:inline text-[10px] font-mono text-textdim bg-surface2 border border-border px-1.5 py-0.5 rounded ml-1">
            v2.0
          </span>
        </Link>

        {/* Desktop nav */}
        <div className="hidden md:flex items-center gap-1 flex-1 justify-center">
          {visibleLinks.map((link) => {
            const active = location.pathname === link.to
            return (
              <Link
                key={link.to}
                to={link.to}
                className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all flex items-center gap-1.5 ${
                  active
                    ? 'bg-primary/15 text-primary border border-primary/25'
                    : 'text-textdim hover:text-white hover:bg-surface2'
                }`}
              >
                <link.icon size={13} />
                {link.label}
              </Link>
            )
          })}
        </div>

        {/* Right section */}
        <div className="flex items-center gap-3">
          {username ? (
            <>
              {/* Cart button */}
              <button
                onClick={onCartClick}
                className="relative p-1.5 hover:bg-surface2 rounded-lg transition-colors"
                aria-label="Open cart"
              >
                <ShoppingCart size={18} className="text-white" />
                <AnimatePresence>
                  {cartCount > 0 && (
                    <motion.span
                      key={cartCount}
                      initial={{ scale: 0 }}
                      animate={{ scale: 1 }}
                      exit={{ scale: 0 }}
                      className="absolute -top-1 -right-1 bg-primary text-white text-[9px] font-mono font-bold w-4 h-4 rounded-full flex items-center justify-center"
                    >
                      {cartCount}
                    </motion.span>
                  )}
                </AnimatePresence>
              </button>

              {/* Username badge */}
              <div className="hidden sm:flex items-center gap-2 px-2.5 py-1 bg-surface2 border border-border rounded-lg">
                <User size={13} className="text-textdim" />
                <span className="text-xs font-mono text-textdim">{username}</span>
              </div>

              {/* Logout */}
              <button
                onClick={onLogout}
                className="p-1.5 text-textdim hover:text-danger hover:bg-danger/10 rounded-lg transition-colors"
                aria-label="Logout"
              >
                <LogOut size={16} />
              </button>
            </>
          ) : (
            <Link
              to="/auth"
              className="px-3 py-1.5 bg-primary text-white text-xs font-medium rounded-lg hover:brightness-110 transition-all shadow-glow"
            >
              Sign in
            </Link>
          )}

          {/* Mobile hamburger */}
          <button
            onClick={() => setMobileOpen((v) => !v)}
            className="md:hidden p-1.5 text-textdim hover:text-white"
            aria-label="Toggle menu"
          >
            {mobileOpen ? <X size={18} /> : <Menu size={18} />}
          </button>
        </div>
      </div>

      {/* Mobile menu */}
      <AnimatePresence>
        {mobileOpen && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            className="md:hidden border-t border-border bg-ink overflow-hidden"
          >
            <div className="px-4 py-3 space-y-1">
              {visibleLinks.map((link) => {
                const active = location.pathname === link.to
                return (
                  <Link
                    key={link.to}
                    to={link.to}
                    onClick={() => setMobileOpen(false)}
                    className={`flex items-center gap-2 px-3 py-2.5 rounded-lg text-sm transition-all ${
                      active ? 'bg-primary/15 text-primary' : 'text-textdim hover:text-white'
                    }`}
                  >
                    <link.icon size={15} />
                    {link.label}
                  </Link>
                )
              })}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </nav>
  )
}

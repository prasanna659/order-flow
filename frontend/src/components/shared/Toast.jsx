import { useEffect, useState, useCallback } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { CheckCircle2, XCircle, AlertTriangle, Info, X } from 'lucide-react'

/* ── Toast types ──────────────────────────────────────────────────────── */
const VARIANTS = {
  success: {
    icon: CheckCircle2,
    iconClass: 'text-success',
    bar: 'bg-success',
    border: 'border-success/25',
    bg: 'bg-success/8',
  },
  error: {
    icon: XCircle,
    iconClass: 'text-danger',
    bar: 'bg-danger',
    border: 'border-danger/25',
    bg: 'bg-danger/8',
  },
  warning: {
    icon: AlertTriangle,
    iconClass: 'text-yellow-400',
    bar: 'bg-yellow-400',
    border: 'border-yellow-400/25',
    bg: 'bg-yellow-400/8',
  },
  info: {
    icon: Info,
    iconClass: 'text-primary',
    bar: 'bg-primary',
    border: 'border-primary/25',
    bg: 'bg-primary/8',
  },
}

/* ── Single toast item ────────────────────────────────────────────────── */
function ToastItem({ toast, onDismiss }) {
  const v = VARIANTS[toast.type] ?? VARIANTS.info
  const Icon = v.icon

  useEffect(() => {
    const t = setTimeout(() => onDismiss(toast.id), toast.duration ?? 4000)
    return () => clearTimeout(t)
  }, [toast.id, toast.duration, onDismiss])

  return (
    <motion.div
      layout
      initial={{ opacity: 0, x: 60, scale: 0.95 }}
      animate={{ opacity: 1, x: 0,  scale: 1    }}
      exit={{    opacity: 0, x: 60, scale: 0.95 }}
      transition={{ type: 'spring', damping: 26, stiffness: 300 }}
      className={`relative flex items-start gap-3 w-full max-w-sm bg-surface border ${v.border} ${v.bg} rounded-xl px-4 py-3 shadow-card overflow-hidden`}
      role="alert"
    >
      {/* accent bar */}
      <div className={`absolute left-0 top-0 bottom-0 w-1 ${v.bar} rounded-l-xl`} />

      <Icon size={16} className={`${v.iconClass} flex-shrink-0 mt-0.5`} />

      <div className="flex-1 min-w-0">
        {toast.title && (
          <p className="text-sm font-medium text-white leading-snug">{toast.title}</p>
        )}
        {toast.message && (
          <p className={`text-xs text-textdim leading-relaxed ${toast.title ? 'mt-0.5' : ''}`}>
            {toast.message}
          </p>
        )}
      </div>

      <button
        onClick={() => onDismiss(toast.id)}
        className="text-textdim hover:text-white flex-shrink-0 mt-0.5"
        aria-label="Dismiss"
      >
        <X size={14} />
      </button>
    </motion.div>
  )
}

/* ── Toast container (portal-rendered at bottom-right) ───────────────── */
export function ToastContainer({ toasts, onDismiss }) {
  return (
    <div
      className="fixed bottom-5 right-5 z-[100] flex flex-col gap-2 items-end pointer-events-none"
      aria-live="polite"
    >
      <AnimatePresence mode="popLayout">
        {toasts.map((t) => (
          <div key={t.id} className="pointer-events-auto w-full max-w-sm">
            <ToastItem toast={t} onDismiss={onDismiss} />
          </div>
        ))}
      </AnimatePresence>
    </div>
  )
}

/* ── useToast hook ────────────────────────────────────────────────────── */
let _id = 0

export function useToast() {
  const [toasts, setToasts] = useState([])

  const dismiss = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id))
  }, [])

  const toast = useCallback(({ type = 'info', title, message, duration = 4000 }) => {
    const id = ++_id
    setToasts((prev) => [...prev, { id, type, title, message, duration }])
    return id
  }, [])

  const success = useCallback((title, message, duration)  => toast({ type: 'success', title, message, duration }), [toast])
  const error   = useCallback((title, message, duration)  => toast({ type: 'error',   title, message, duration }), [toast])
  const warning = useCallback((title, message, duration)  => toast({ type: 'warning', title, message, duration }), [toast])
  const info    = useCallback((title, message, duration)  => toast({ type: 'info',    title, message, duration }), [toast])

  return { toasts, toast, success, error, warning, info, dismiss }
}

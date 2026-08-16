import { useEffect, useState, useRef } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import {
  PackageCheck, CreditCard, CheckCircle2, XCircle, X,
  Loader2, RefreshCw, AlertTriangle, Zap, Bell, Clock,
  ChevronDown, ChevronUp
} from 'lucide-react'
import { orderApi } from '../api'

/* ── Saga stations (matches backend OrderStatus state machine) ──────── */
const STATIONS = [
  { key: 'RESERVING', label: 'Reserve stock',  icon: PackageCheck, service: 'Inventory Service' },
  { key: 'CHARGING',  label: 'Charge payment', icon: CreditCard,   service: 'Payment Service'   },
  { key: 'CONFIRMED', label: 'Confirmed',       icon: CheckCircle2, service: 'Order Service'     },
]

function stationIndexFor(status) {
  switch (status) {
    case 'PENDING':    return -1
    case 'RESERVING':  return 0
    case 'CHARGING':   return 1
    case 'CONFIRMED':  return 2
    default:           return -1
  }
}

/* ── Build an event timeline from the order status progression ──────── */
function buildTimeline(statusHistory) {
  return statusHistory.map((entry) => ({
    time: new Date(entry.timestamp).toLocaleTimeString('en-US', { hour12: false }),
    label: eventLabel(entry.status),
    service: eventService(entry.status),
    type: entry.status === 'CANCELLED' ? 'error' : entry.status === 'CONFIRMED' ? 'success' : 'info',
  }))
}

function eventLabel(status) {
  switch (status) {
    case 'PENDING':    return 'Order Created'
    case 'RESERVING':  return 'Reserving Inventory'
    case 'CHARGING':   return 'Charging Payment'
    case 'CONFIRMED':  return 'Order Confirmed — Kafka event published'
    case 'CANCELLED':  return 'Compensation executed — Order Cancelled'
    default:           return status
  }
}

function eventService(status) {
  switch (status) {
    case 'PENDING':    return 'Order Service'
    case 'RESERVING':  return 'Inventory Service'
    case 'CHARGING':   return 'Payment Service'
    case 'CONFIRMED':  return 'Kafka → Notification'
    case 'CANCELLED':  return 'Saga Compensation'
    default:           return ''
  }
}

function getCancellationType(reason) {
  if (!reason) return 'OTHER'
  const r = reason.toLowerCase()
  if (r.includes('payment') || r.includes('card') || r.includes('charge')) return 'PAYMENT'
  if (r.includes('inventory') || r.includes('stock') || r.includes('reserve')) return 'INVENTORY'
  return 'OTHER'
}

/* ── Component ─────────────────────────────────────────────────────── */
export default function OrderTracker({ orderId, onClose, onRetry }) {
  const [order, setOrder]                         = useState(null)
  const [pollError, setPollError]                 = useState(null)
  const [startTime]                               = useState(() => Date.now())
  const [elapsedMs, setElapsedMs]                 = useState(0)
  const [isCompensating, setIsCompensating]       = useState(false)
  const [showCompAnim, setShowCompAnim]           = useState(false)
  const [statusHistory, setStatusHistory]         = useState([])   // [{status, timestamp}]
  const [showTimeline, setShowTimeline]           = useState(false)
  const intervalRef                               = useRef(null)
  const timerRef                                  = useRef(null)
  const prevStatusRef                             = useRef(null)

  useEffect(() => {
    if (!orderId) return

    // Reset state for new order
    setOrder(null)
    setPollError(null)
    setIsCompensating(false)
    setShowCompAnim(false)
    setStatusHistory([])
    prevStatusRef.current = null

    async function poll() {
      try {
        const res = await orderApi.getOrder(orderId)
        const newOrder = res.data

        // Track status transitions for the timeline
        if (newOrder.status !== prevStatusRef.current) {
          setStatusHistory((prev) => {
            const alreadyHas = prev.some((e) => e.status === newOrder.status)
            if (alreadyHas) return prev
            return [...prev, { status: newOrder.status, timestamp: Date.now() }]
          })

          // Detect saga compensation: had progress, now cancelled
          if (
            prevStatusRef.current &&
            prevStatusRef.current !== 'CANCELLED' &&
            newOrder.status === 'CANCELLED' &&
            stationIndexFor(prevStatusRef.current) >= 0
          ) {
            setIsCompensating(true)
            setShowCompAnim(true)
            setTimeout(() => setShowCompAnim(false), 2200)
          }

          prevStatusRef.current = newOrder.status
        }

        setOrder(newOrder)
        setPollError(null)

        if (newOrder.status === 'CONFIRMED' || newOrder.status === 'CANCELLED') {
          clearInterval(intervalRef.current)
          clearInterval(timerRef.current)
        }
      } catch (e) {
        setPollError(e.response?.data?.message || e.message || 'Failed to fetch order status')
      }
    }

    poll()
    intervalRef.current = setInterval(poll, 1000)
    timerRef.current    = setInterval(() => setElapsedMs(Date.now() - startTime), 500)

    return () => {
      clearInterval(intervalRef.current)
      clearInterval(timerRef.current)
    }
  }, [orderId])

  if (!orderId) return null

  const cancelled    = order?.status === 'CANCELLED'
  const confirmed    = order?.status === 'CONFIRMED'
  const activeIndex  = order ? stationIndexFor(order.status) : -1
  const inProgress   = !cancelled && !confirmed
  const elapsed      = (elapsedMs / 1000).toFixed(1)
  const cancelType   = getCancellationType(order?.failureReason)
  const timeline     = buildTimeline(statusHistory)

  return (
    <AnimatePresence>
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        className="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-center justify-center p-4"
      >
        <motion.div
          initial={{ opacity: 0, scale: 0.95, y: 12 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          className="bg-surface border border-border rounded-2xl w-full max-w-lg relative shadow-card"
        >
          {/* Header */}
          <div className="flex items-start justify-between p-5 pb-4 border-b border-border">
            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-2 mb-1">
                <span className="text-[10px] font-mono text-textdim uppercase tracking-widest">
                  {confirmed ? 'Order Confirmed' : cancelled ? 'Order Cancelled' : 'Processing Order'}
                </span>
                {inProgress && (
                  <span className="flex items-center gap-1 text-[10px] font-mono text-primary">
                    <span className="w-1.5 h-1.5 rounded-full bg-primary animate-pulse" />
                    {elapsed}s
                  </span>
                )}
              </div>
              <p className="font-mono text-xs text-textdim truncate">{orderId}</p>
            </div>
            <button
              onClick={onClose}
              className="text-textdim hover:text-white p-1 rounded-lg hover:bg-surface2 transition-colors ml-2"
            >
              <X size={16} />
            </button>
          </div>

          <div className="p-5 space-y-5">

            {/* Poll error */}
            {pollError && !cancelled && (
              <motion.div
                initial={{ opacity: 0 }} animate={{ opacity: 1 }}
                className="flex items-center gap-2 bg-danger/10 border border-danger/30 rounded-lg px-4 py-2.5"
              >
                <AlertTriangle size={15} className="text-danger flex-shrink-0" />
                <p className="text-xs text-danger flex-1">{pollError}</p>
                <button onClick={() => setPollError(null)} className="text-textdim hover:text-white">
                  <X size={12} />
                </button>
              </motion.div>
            )}

            {/* ── Happy path rail ─────────────────────────────── */}
            {!cancelled && (
              <div className="relative">
                {/* track */}
                <div className="absolute top-5 left-5 right-5 h-0.5 bg-border" />
                {/* filled */}
                <motion.div
                  className="absolute top-5 left-5 h-0.5 bg-primary"
                  initial={{ width: '0%' }}
                  animate={{
                    width: confirmed
                      ? 'calc(100% - 40px)'
                      : activeIndex < 0
                      ? '0%'
                      : `calc(${(activeIndex / (STATIONS.length - 1)) * 100}% )`
                  }}
                  style={{ maxWidth: 'calc(100% - 40px)' }}
                  transition={{ duration: 0.6, ease: 'easeInOut' }}
                />
                <div className="relative flex justify-between">
                  {STATIONS.map((station, i) => {
                    const Icon     = station.icon
                    const isDone   = confirmed || i < activeIndex
                    const isActive = i === activeIndex && !confirmed
                    return (
                      <div key={station.key} className="flex flex-col items-center gap-2 w-10">
                        <motion.div
                          animate={isActive ? { scale: [1, 1.15, 1] } : { scale: 1 }}
                          transition={isActive ? { repeat: Infinity, duration: 1.2 } : {}}
                          className={`w-10 h-10 rounded-full flex items-center justify-center border-2 z-10 relative ${
                            isDone
                              ? 'bg-success border-success text-ink'
                              : isActive
                              ? 'bg-primary border-primary text-white'
                              : 'bg-surface2 border-border text-textdim'
                          }`}
                        >
                          {isActive ? (
                            <Loader2 size={16} className="animate-spin" />
                          ) : (
                            <Icon size={16} />
                          )}
                          {isActive && (
                            <motion.div
                              className="absolute inset-0 rounded-full border-2 border-primary"
                              initial={{ scale: 1, opacity: 1 }}
                              animate={{ scale: 1.5, opacity: 0 }}
                              transition={{ duration: 1, repeat: Infinity }}
                            />
                          )}
                        </motion.div>
                        <div className="text-center">
                          <p className="text-[10px] font-mono text-textdim leading-tight">{station.label}</p>
                          <p className="text-[9px] text-textdim/50 leading-tight">{station.service}</p>
                        </div>
                      </div>
                    )
                  })}
                </div>
              </div>
            )}

            {/* ── Confirmed banner ────────────────────────────── */}
            {confirmed && (
              <motion.div
                initial={{ opacity: 0, scale: 0.9 }}
                animate={{ opacity: 1, scale: 1 }}
                className="flex flex-col items-center gap-2 py-2"
              >
                <div className="flex items-center gap-2">
                  <CheckCircle2 size={22} className="text-success" />
                  <span className="text-success font-semibold">Order confirmed</span>
                </div>
                <p className="text-sm font-mono text-primary">
                  Total: ${order.totalAmount?.toFixed(2)}
                </p>
                <div className="flex items-center gap-3 mt-1 text-[10px] font-mono text-textdim">
                  <span className="flex items-center gap-1">
                    <Zap size={10} className="text-yellow-400" /> Kafka event published
                  </span>
                  <span className="flex items-center gap-1">
                    <Bell size={10} className="text-primary" /> Email sent to inbox
                  </span>
                  <span className="flex items-center gap-1">
                    <Clock size={10} /> {elapsed}s total
                  </span>
                </div>
              </motion.div>
            )}

            {/* ── Cancelled / compensation ────────────────────── */}
            {cancelled && (
              <motion.div
                initial={{ opacity: 0, scale: 0.9 }}
                animate={{ opacity: 1, scale: 1 }}
                className="flex flex-col items-center gap-3 py-2"
              >
                {showCompAnim ? (
                  <>
                    <motion.div
                      animate={{ rotate: 360 }}
                      transition={{ duration: 1, repeat: Infinity, ease: 'linear' }}
                    >
                      <RefreshCw size={36} className="text-yellow-400" />
                    </motion.div>
                    <p className="text-yellow-400 font-medium text-sm">Executing compensation…</p>
                    <p className="text-xs text-textdim font-mono">Rolling back inventory reservation</p>
                  </>
                ) : (
                  <>
                    <XCircle size={36} className="text-danger" />
                    <p className="text-danger font-medium">Order cancelled</p>

                    {order?.failureReason && (
                      <div className="flex items-start gap-2 bg-danger/10 border border-danger/30 rounded-lg px-4 py-2.5 w-full">
                        <AlertTriangle size={14} className="text-danger flex-shrink-0 mt-0.5" />
                        <div>
                          <p className="text-xs text-danger font-medium">
                            {cancelType === 'PAYMENT'   ? 'Payment declined' :
                             cancelType === 'INVENTORY' ? 'Inventory unavailable' :
                             'Order failed'}
                          </p>
                          <p className="text-[11px] text-textdim font-mono mt-0.5">{order.failureReason}</p>
                        </div>
                      </div>
                    )}

                    {isCompensating && (
                      <div className="flex items-center gap-2 text-xs text-yellow-400 font-mono bg-yellow-400/10 border border-yellow-400/25 rounded-lg px-3 py-2 w-full justify-center">
                        <RefreshCw size={12} />
                        Compensating transaction executed — stock released
                      </div>
                    )}

                    <p className="text-[11px] text-textdim text-center max-w-xs">
                      Reserved inventory was automatically returned to stock.
                    </p>

                    <div className="flex items-center gap-1 text-[10px] font-mono text-textdim">
                      <Bell size={10} className="text-danger/70" /> Cancellation email sent to your inbox
                    </div>

                    {onRetry && order?.items?.length > 0 && (
                      <button
                        onClick={() => onRetry(order.items)}
                        className="flex items-center gap-2 px-4 py-2 bg-primary text-white rounded-lg text-sm font-medium hover:brightness-110 hover:-translate-y-0.5 transition-all shadow-glow"
                      >
                        <RefreshCw size={14} /> Retry order
                      </button>
                    )}
                  </>
                )}
              </motion.div>
            )}

            {/* ── In-progress status line ──────────────────────── */}
            {inProgress && !pollError && (
              <p className="text-center text-xs text-textdim font-mono">
                saga in progress — polling every second…
              </p>
            )}

            {/* ── Event Timeline (collapsible) ─────────────────── */}
            {timeline.length > 0 && (
              <div className="border-t border-border pt-4">
                <button
                  onClick={() => setShowTimeline((v) => !v)}
                  className="flex items-center justify-between w-full text-xs font-mono text-textdim hover:text-white transition-colors mb-3"
                >
                  <span className="flex items-center gap-1.5">
                    <Clock size={11} /> Event Timeline
                    <span className="bg-primary/20 text-primary px-1.5 py-0.5 rounded text-[9px]">
                      {timeline.length}
                    </span>
                  </span>
                  {showTimeline ? <ChevronUp size={13} /> : <ChevronDown size={13} />}
                </button>

                <AnimatePresence>
                  {showTimeline && (
                    <motion.div
                      initial={{ height: 0, opacity: 0 }}
                      animate={{ height: 'auto', opacity: 1 }}
                      exit={{ height: 0, opacity: 0 }}
                      className="overflow-hidden"
                    >
                      <div className="space-y-2">
                        {timeline.map((event, i) => (
                          <motion.div
                            key={i}
                            initial={{ opacity: 0, x: -8 }}
                            animate={{ opacity: 1, x: 0 }}
                            transition={{ delay: i * 0.05 }}
                            className="flex items-start gap-3"
                          >
                            <span className="font-mono text-[10px] text-textdim/60 w-16 flex-shrink-0 pt-0.5">
                              {event.time}
                            </span>
                            <div className="flex items-start gap-2 min-w-0">
                              <span className={`w-1.5 h-1.5 rounded-full flex-shrink-0 mt-1.5 ${
                                event.type === 'success' ? 'bg-success' :
                                event.type === 'error'   ? 'bg-danger'  :
                                'bg-primary'
                              }`} />
                              <div className="min-w-0">
                                <p className={`text-xs ${
                                  event.type === 'success' ? 'text-success' :
                                  event.type === 'error'   ? 'text-danger'  :
                                  'text-white'
                                }`}>{event.label}</p>
                                <p className="text-[10px] text-textdim font-mono">{event.service}</p>
                              </div>
                            </div>
                          </motion.div>
                        ))}
                      </div>
                    </motion.div>
                  )}
                </AnimatePresence>
              </div>
            )}
          </div>
        </motion.div>
      </motion.div>
    </AnimatePresence>
  )
}

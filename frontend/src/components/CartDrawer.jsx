import { motion, AnimatePresence } from 'framer-motion'
import { X, Minus, Plus, Trash2, ShoppingCart, Zap, ArrowRight } from 'lucide-react'

export default function CartDrawer({ open, items, onClose, onUpdateQty, onRemove, onCheckout, checkingOut }) {
  const total    = items.reduce((sum, i) => sum + i.price * i.quantity, 0)
  const itemCount = items.reduce((sum, i) => sum + i.quantity, 0)

  return (
    <AnimatePresence>
      {open && (
        <>
          {/* Backdrop */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.2 }}
            onClick={onClose}
            className="fixed inset-0 bg-black/65 backdrop-blur-sm z-40"
          />

          {/* Drawer panel */}
          <motion.div
            initial={{ x: '100%' }}
            animate={{ x: 0 }}
            exit={{ x: '100%' }}
            transition={{ type: 'spring', damping: 30, stiffness: 320 }}
            className="fixed right-0 top-0 h-full w-full max-w-[380px] bg-surface border-l border-border z-50 flex flex-col"
          >
            {/* ── Header ──────────────────────────────────────── */}
            <div className="flex items-center justify-between px-5 py-4 border-b border-border bg-surface">
              <div className="flex items-center gap-3">
                <div className="w-8 h-8 rounded-lg bg-primary/15 border border-primary/25 flex items-center justify-center">
                  <ShoppingCart size={15} className="text-primary" />
                </div>
                <div>
                  <h2 className="font-display font-semibold text-sm">Your Cart</h2>
                  <p className="text-[10px] font-mono text-textdim">
                    {itemCount === 0 ? 'Empty' : `${itemCount} item${itemCount !== 1 ? 's' : ''}`}
                  </p>
                </div>
              </div>
              <button
                onClick={onClose}
                className="w-7 h-7 flex items-center justify-center rounded-lg text-textdim hover:text-white hover:bg-surface2 transition-all"
              >
                <X size={15} />
              </button>
            </div>

            {/* ── Items list ──────────────────────────────────── */}
            <div className="flex-1 overflow-y-auto px-5 py-4 space-y-3">
              <AnimatePresence initial={false}>
                {items.length === 0 ? (
                  <motion.div
                    key="empty"
                    initial={{ opacity: 0, y: 8 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="flex flex-col items-center justify-center py-16 text-center"
                  >
                    <div className="w-14 h-14 rounded-2xl bg-surface2 border border-border flex items-center justify-center mb-4">
                      <ShoppingCart size={22} className="text-textdim/40" />
                    </div>
                    <p className="text-textdim font-medium text-sm">Nothing here yet</p>
                    <p className="text-textdim/50 text-xs mt-1 font-mono">
                      Add items from the catalog
                    </p>
                  </motion.div>
                ) : (
                  items.map((item) => (
                    <motion.div
                      key={item.productId}
                      initial={{ opacity: 0, x: 20, height: 0 }}
                      animate={{ opacity: 1, x: 0, height: 'auto' }}
                      exit={{ opacity: 0, x: 20, height: 0 }}
                      transition={{ duration: 0.22 }}
                      className="flex items-center gap-3 bg-surface2 border border-border rounded-xl p-3 group"
                    >
                      {/* Colour swatch fallback thumbnail */}
                      <div className="w-10 h-10 rounded-lg bg-primary/10 border border-primary/20 flex-shrink-0 flex items-center justify-center overflow-hidden">
                        <ShoppingCart size={14} className="text-primary/50" />
                      </div>

                      {/* Name + price */}
                      <div className="flex-1 min-w-0">
                        <p className="text-xs font-medium truncate leading-snug">{item.productName}</p>
                        <p className="text-[11px] font-mono text-primary mt-0.5">
                          ${(item.price * item.quantity).toFixed(2)}
                          {item.quantity > 1 && (
                            <span className="text-textdim ml-1">(${item.price.toFixed(2)} ea)</span>
                          )}
                        </p>
                      </div>

                      {/* Qty stepper */}
                      <div className="flex items-center gap-1 bg-surface border border-border rounded-lg px-1 py-1 flex-shrink-0">
                        <button
                          onClick={() => onUpdateQty(item.productId, item.quantity - 1)}
                          className="w-5 h-5 flex items-center justify-center text-textdim hover:text-white hover:bg-surface2 rounded transition-all"
                        >
                          <Minus size={10} />
                        </button>
                        <span className="font-mono text-xs w-5 text-center tabular-nums">{item.quantity}</span>
                        <button
                          onClick={() => onUpdateQty(item.productId, item.quantity + 1)}
                          className="w-5 h-5 flex items-center justify-center text-textdim hover:text-white hover:bg-surface2 rounded transition-all"
                        >
                          <Plus size={10} />
                        </button>
                      </div>

                      {/* Remove */}
                      <button
                        onClick={() => onRemove(item.productId)}
                        className="opacity-0 group-hover:opacity-100 w-6 h-6 flex items-center justify-center text-textdim hover:text-danger transition-all flex-shrink-0"
                      >
                        <Trash2 size={13} />
                      </button>
                    </motion.div>
                  ))
                )}
              </AnimatePresence>
            </div>

            {/* ── Footer ──────────────────────────────────────── */}
            <div className="px-5 py-5 border-t border-border bg-surface space-y-4">
              {/* Order summary */}
              {items.length > 0 && (
                <div className="space-y-1.5">
                  <div className="flex justify-between text-xs text-textdim font-mono">
                    <span>Subtotal ({itemCount} item{itemCount !== 1 ? 's' : ''})</span>
                    <span>${total.toFixed(2)}</span>
                  </div>
                  <div className="flex justify-between text-xs text-textdim font-mono">
                    <span>Processing fee</span>
                    <span className="text-success">Free</span>
                  </div>
                  <div className="flex justify-between items-center pt-2 border-t border-border">
                    <span className="text-sm font-semibold">Total</span>
                    <span className="font-mono font-bold text-lg text-primary">${total.toFixed(2)}</span>
                  </div>
                </div>
              )}

              {/* Saga hint */}
              {items.length > 0 && (
                <div className="flex items-center gap-2 bg-primary/8 border border-primary/20 rounded-lg px-3 py-2">
                  <Zap size={11} className="text-primary flex-shrink-0" />
                  <p className="text-[10px] font-mono text-textdim/80 leading-relaxed">
                    Placing order triggers a distributed Saga — stock reserve → payment charge → Kafka event
                  </p>
                </div>
              )}

              {/* CTA */}
              <motion.button
                whileTap={items.length === 0 || checkingOut ? {} : { scale: 0.98 }}
                onClick={onCheckout}
                disabled={items.length === 0 || checkingOut}
                className="w-full flex items-center justify-center gap-2 bg-primary text-white font-semibold py-3 rounded-xl shadow-glow hover:brightness-110 hover:-translate-y-0.5 transition-all disabled:opacity-40 disabled:translate-y-0 disabled:shadow-none disabled:cursor-not-allowed text-sm"
              >
                {checkingOut ? (
                  <>
                    <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                    Placing order…
                  </>
                ) : (
                  <>
                    <ShoppingCart size={15} />
                    Place order
                    <ArrowRight size={14} className="ml-auto" />
                  </>
                )}
              </motion.button>

              <p className="text-[9px] font-mono text-textdim/40 text-center">
                ~15% chance of payment decline to demo saga compensation
              </p>
            </div>
          </motion.div>
        </>
      )}
    </AnimatePresence>
  )
}

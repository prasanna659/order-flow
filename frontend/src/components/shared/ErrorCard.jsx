import { AlertTriangle, RefreshCw } from 'lucide-react'
import { motion } from 'framer-motion'

export default function ErrorCard({ title, message, context, onRetry }) {
  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.97 }}
      animate={{ opacity: 1, scale: 1 }}
      className="bg-danger/8 border border-danger/25 rounded-xl p-5"
    >
      <div className="flex items-start gap-3">
        <div className="w-8 h-8 rounded-lg bg-danger/15 border border-danger/30 flex items-center justify-center flex-shrink-0 mt-0.5">
          <AlertTriangle size={16} className="text-danger" />
        </div>
        <div className="flex-1 min-w-0">
          <p className="font-medium text-danger text-sm">{title}</p>
          {message && <p className="text-textdim text-xs mt-1 font-mono">{message}</p>}
          {context && (
            <p className="text-textdim/70 text-xs mt-2 italic">{context}</p>
          )}
        </div>
      </div>
      {onRetry && (
        <button
          onClick={onRetry}
          className="mt-4 flex items-center gap-2 px-3 py-1.5 bg-surface2 border border-border rounded-lg text-xs hover:border-primary/50 transition-colors"
        >
          <RefreshCw size={12} />
          Retry
        </button>
      )}
    </motion.div>
  )
}

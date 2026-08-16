import { motion } from 'framer-motion'

export default function EmptyState({ icon: Icon, title, description, action }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      className="flex flex-col items-center justify-center py-20 px-6 text-center"
    >
      {Icon && (
        <div className="w-16 h-16 rounded-2xl bg-surface2 border border-border flex items-center justify-center mb-4">
          <Icon size={28} className="text-textdim" />
        </div>
      )}
      <h3 className="font-display font-semibold text-lg mb-2">{title}</h3>
      {description && (
        <p className="text-textdim text-sm max-w-sm leading-relaxed">{description}</p>
      )}
      {action && (
        <div className="mt-6">{action}</div>
      )}
    </motion.div>
  )
}

import { useState, useMemo } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import {
  Plus, ShoppingCart, Headphones, Monitor, Watch,
  Zap, Camera, Cpu, SlidersHorizontal, Search, X
} from 'lucide-react'

/* ── Category definition ─────────────────────────────────────────────── */
const CATEGORIES = [
  { id: 'all',       label: 'All',        icon: SlidersHorizontal },
  { id: 'audio',     label: 'Audio',      icon: Headphones },
  { id: 'computing', label: 'Computing',  icon: Cpu },
  { id: 'wearables', label: 'Wearables',  icon: Watch },
  { id: 'cameras',   label: 'Cameras',    icon: Camera },
  { id: 'power',     label: 'Power',      icon: Zap },
  { id: 'displays',  label: 'Displays',   icon: Monitor },
]

/* Maps product name keywords → category id */
function resolveCategory(name) {
  const n = name.toLowerCase()
  if (n.includes('headphone') || n.includes('earbud') || n.includes('speaker') || n.includes('audio') || n.includes('wh-') || n.includes('quietcomfort') || n.includes('marshall') || n.includes('emberton') || n.includes('bose')) return 'audio'
  if (n.includes('watch') || n.includes('tracker') || n.includes('band') || n.includes('fitness') || n.includes('ring') || n.includes('oura') || n.includes('garmin') || n.includes('forerunner') || n.includes('fitbit')) return 'wearables'
  if (n.includes('webcam') || n.includes('camera') || n.includes('cam') || n.includes('osmo') || n.includes('dji') || n.includes('gopro') || n.includes('gimbal') || n.includes('facecam') || n.includes('zv-')) return 'cameras'
  if (n.includes('charger') || n.includes('power bank') || n.includes('gan') || n.includes('cable') || n.includes('power station') || n.includes('solix') || n.includes('wireless charging') || n.includes('pad') || n.includes('belkin') || n.includes('ugreen') || n.includes('anker')) return 'power'
  if (n.includes('monitor') || n.includes('display') || n.includes('led') || n.includes('light') || n.includes('screen') || n.includes('screenbar') || n.includes('stream deck') || n.includes('elgato') || n.includes('benq')) return 'displays'
  if (n.includes('keyboard') || n.includes('mouse') || n.includes('hub') || n.includes('ssd') || n.includes('stand') || n.includes('laptop') || n.includes('dock') || n.includes('thunderbolt') || n.includes('caldigit') || n.includes('keychron') || n.includes('logitech') || n.includes('mx keys') || n.includes('mx master') || n.includes('samsung t') || n.includes('twelve south')) return 'computing'
  return 'computing'
}

/* Category accent colours — matched to design system */
const CAT_ACCENT = {
  audio:     { text: 'text-[#8B7CFF]', bg: 'bg-[#6D5CF5]/10',  border: 'border-[#6D5CF5]/25' },
  computing: { text: 'text-[#2FD98A]', bg: 'bg-[#2FD98A]/10',  border: 'border-[#2FD98A]/25' },
  wearables: { text: 'text-[#34B4FF]', bg: 'bg-[#34B4FF]/10',  border: 'border-[#34B4FF]/25' },
  cameras:   { text: 'text-[#F59E0B]', bg: 'bg-[#F59E0B]/10',  border: 'border-[#F59E0B]/25' },
  power:     { text: 'text-[#FB5570]', bg: 'bg-[#FB5570]/10',  border: 'border-[#FB5570]/25' },
  displays:  { text: 'text-[#A78BFA]', bg: 'bg-[#A78BFA]/10',  border: 'border-[#A78BFA]/25' },
}

/* Framer Motion variants */
const gridVariants = {
  hidden: {},
  show:   { transition: { staggerChildren: 0.06 } },
}
const cardVariants = {
  hidden: { opacity: 0, y: 18, scale: 0.97 },
  show:   { opacity: 1, y: 0,  scale: 1,    transition: { duration: 0.35, ease: [0.22, 1, 0.36, 1] } },
  exit:   { opacity: 0, y: -8, scale: 0.96, transition: { duration: 0.2 } },
}

/* Image fallback — gradient placeholder by category */
function ProductImage({ src, name, category }) {
  const [errored, setErrored] = useState(false)
  const accent = CAT_ACCENT[category] || CAT_ACCENT.computing

  if (!src || errored) {
    return (
      <div className={`w-full h-full flex flex-col items-center justify-center gap-2 ${accent.bg}`}>
        <ShoppingCart size={32} className={`${accent.text} opacity-40`} />
        <span className="text-[10px] font-mono text-textdim/50 px-2 text-center leading-tight">{name}</span>
      </div>
    )
  }
  return (
    <img
      src={src}
      alt={name}
      className="w-full h-full object-cover group-hover:scale-[1.06] transition-transform duration-500 ease-out"
      onError={() => setErrored(true)}
    />
  )
}

/* ── Single product card ─────────────────────────────────────────────── */
function ProductCard({ product, onAddToCart, index }) {
  const [added, setAdded] = useState(false)
  const category = resolveCategory(product.name)
  const accent   = CAT_ACCENT[category] || CAT_ACCENT.computing
  const outOfStock = product.stockQuantity === 0
  const lowStock   = product.stockQuantity > 0 && product.stockQuantity <= 5
  const catMeta    = CATEGORIES.find((c) => c.id === category)

  function handleAdd() {
    if (outOfStock) return
    onAddToCart(product)
    setAdded(true)
    setTimeout(() => setAdded(false), 1200)
  }

  return (
    <motion.div
      variants={cardVariants}
      layout
      className={`group relative bg-surface border rounded-2xl overflow-hidden shadow-card transition-all duration-300
        ${outOfStock ? 'border-border opacity-60' : 'border-border hover:border-primary/50 hover:-translate-y-1 hover:shadow-[0_8px_32px_-8px_rgba(109,92,245,0.35)]'}`}
    >
      {/* Image */}
      <div className="aspect-[4/3] overflow-hidden bg-surface2 relative">
        <ProductImage src={product.imageUrl} name={product.name} category={category} />

        {/* Overlay badges */}
        {outOfStock && (
          <div className="absolute inset-0 bg-ink/60 flex items-center justify-center backdrop-blur-[1px]">
            <span className="text-xs font-mono font-semibold text-danger bg-ink/90 px-3 py-1.5 rounded-full border border-danger/40 tracking-wide">
              Sold Out
            </span>
          </div>
        )}
        {lowStock && !outOfStock && (
          <motion.span
            initial={{ opacity: 0, x: 8 }}
            animate={{ opacity: 1, x: 0 }}
            className="absolute top-2.5 right-2.5 text-[10px] font-mono font-semibold text-danger bg-ink/90 px-2 py-1 rounded-full border border-danger/40"
          >
            Only {product.stockQuantity} left
          </motion.span>
        )}

        {/* Category pill */}
        {catMeta && (
          <div className={`absolute top-2.5 left-2.5 flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-mono border ${accent.bg} ${accent.border} ${accent.text}`}>
            <catMeta.icon size={9} />
            {catMeta.label}
          </div>
        )}
      </div>

      {/* Body */}
      <div className="p-4">
        <div className="flex items-start justify-between gap-2 mb-1.5">
          <h3 className="font-semibold text-sm leading-snug line-clamp-2 flex-1">{product.name}</h3>
          <span className={`font-mono font-bold text-base whitespace-nowrap ${accent.text}`}>
            ${product.price.toFixed(2)}
          </span>
        </div>

        <p className="text-[11px] text-textdim leading-relaxed line-clamp-2 mb-3">
          {product.description}
        </p>

        <div className="flex items-center justify-between">
          <span className={`text-[10px] font-mono ${outOfStock ? 'text-danger' : lowStock ? 'text-danger' : 'text-textdim'}`}>
            {outOfStock ? 'Out of stock' : `${product.stockQuantity} in stock`}
          </span>

          <motion.button
            whileTap={outOfStock ? {} : { scale: 0.9 }}
            onClick={handleAdd}
            disabled={outOfStock}
            className={`relative flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-semibold transition-all duration-200 overflow-hidden
              ${outOfStock
                ? 'bg-surface2 border border-border text-textdim/40 cursor-not-allowed'
                : added
                ? 'bg-success border border-success/40 text-ink'
                : 'bg-surface2 border border-border text-white hover:bg-primary hover:border-primary hover:shadow-glow'
              }`}
          >
            <AnimatePresence mode="wait" initial={false}>
              {added ? (
                <motion.span
                  key="check"
                  initial={{ opacity: 0, y: 6 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -6 }}
                  className="flex items-center gap-1"
                >
                  ✓ Added
                </motion.span>
              ) : (
                <motion.span
                  key="add"
                  initial={{ opacity: 0, y: 6 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -6 }}
                  className="flex items-center gap-1"
                >
                  <Plus size={12} /> Add
                </motion.span>
              )}
            </AnimatePresence>
          </motion.button>
        </div>
      </div>
    </motion.div>
  )
}

/* ── ProductGrid ─────────────────────────────────────────────────────── */
export default function ProductGrid({ products, onAddToCart }) {
  const [activeCategory, setActiveCategory] = useState('all')
  const [search, setSearch]                 = useState('')

  const enriched = useMemo(() =>
    products.map((p) => ({ ...p, _category: resolveCategory(p.name) })),
    [products]
  )

  const filtered = useMemo(() => {
    let list = enriched
    if (activeCategory !== 'all') list = list.filter((p) => p._category === activeCategory)
    if (search.trim()) {
      const q = search.toLowerCase()
      list = list.filter((p) =>
        p.name.toLowerCase().includes(q) || p.description?.toLowerCase().includes(q)
      )
    }
    return list
  }, [enriched, activeCategory, search])

  /* Category counts */
  const counts = useMemo(() => {
    const map = {}
    enriched.forEach((p) => { map[p._category] = (map[p._category] || 0) + 1 })
    return map
  }, [enriched])

  return (
    <div>
      {/* ── Controls bar ──────────────────────────────────────────── */}
      <div className="flex flex-col sm:flex-row gap-3 mb-6">
        {/* Search */}
        <div className="relative flex-1 max-w-xs">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-textdim" />
          <input
            type="text"
            placeholder="Search products…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full bg-surface border border-border rounded-lg pl-8 pr-8 py-2 text-xs text-white placeholder:text-textdim/60 focus:outline-none focus:border-primary/50 transition-colors"
          />
          {search && (
            <button
              onClick={() => setSearch('')}
              className="absolute right-2.5 top-1/2 -translate-y-1/2 text-textdim hover:text-white transition-colors"
            >
              <X size={13} />
            </button>
          )}
        </div>

        {/* Category pills — scrollable on small screens */}
        <div className="flex gap-2 overflow-x-auto pb-1 scrollbar-none flex-shrink-0">
          {CATEGORIES.map((cat) => {
            const active  = activeCategory === cat.id
            const count   = cat.id === 'all' ? enriched.length : (counts[cat.id] || 0)
            const accent  = CAT_ACCENT[cat.id]
            return (
              <motion.button
                key={cat.id}
                whileTap={{ scale: 0.94 }}
                onClick={() => setActiveCategory(cat.id)}
                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium whitespace-nowrap border transition-all duration-200
                  ${active
                    ? accent
                      ? `${accent.bg} ${accent.border} ${accent.text}`
                      : 'bg-primary/15 border-primary/30 text-primary'
                    : 'bg-surface border-border text-textdim hover:text-white hover:border-border/60'
                  }`}
              >
                <cat.icon size={12} />
                {cat.label}
                <span className={`text-[9px] font-mono px-1 rounded ${active ? 'opacity-70' : 'opacity-40'}`}>
                  {count}
                </span>
              </motion.button>
            )
          })}
        </div>
      </div>

      {/* ── Results summary ───────────────────────────────────────── */}
      <AnimatePresence mode="wait">
        {filtered.length === 0 ? (
          <motion.div
            key="empty"
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0 }}
            className="flex flex-col items-center justify-center py-20 text-center"
          >
            <Search size={36} className="text-textdim/30 mb-3" />
            <p className="text-textdim font-medium">No products found</p>
            <p className="text-textdim/50 text-xs mt-1 font-mono">
              Try a different category or clear the search
            </p>
            <button
              onClick={() => { setSearch(''); setActiveCategory('all') }}
              className="mt-4 px-4 py-2 bg-surface border border-border rounded-lg text-xs text-textdim hover:text-white hover:border-primary/40 transition-all"
            >
              Clear filters
            </button>
          </motion.div>
        ) : (
          <motion.div
            key={`${activeCategory}-${search}`}
            variants={gridVariants}
            initial="hidden"
            animate="show"
            className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5"
          >
            {filtered.map((product, i) => (
              <ProductCard
                key={product.id}
                product={product}
                onAddToCart={onAddToCart}
                index={i}
              />
            ))}
          </motion.div>
        )}
      </AnimatePresence>

      {/* Result count */}
      {filtered.length > 0 && (
        <p className="text-[11px] font-mono text-textdim/40 text-right mt-4">
          {filtered.length} product{filtered.length !== 1 ? 's' : ''}
          {activeCategory !== 'all' || search ? ' · filtered' : ''}
        </p>
      )}
    </div>
  )
}

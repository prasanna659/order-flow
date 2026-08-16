export default function SkeletonCard({ lines = 3, className = '' }) {
  return (
    <div className={`bg-surface border border-border rounded-xl p-4 animate-pulse ${className}`}>
      <div className="h-4 bg-border rounded w-3/4 mb-3" />
      {Array.from({ length: lines - 1 }).map((_, i) => (
        <div key={i} className={`h-3 bg-border rounded mb-2 ${i === lines - 2 ? 'w-1/2' : 'w-full'}`} />
      ))}
    </div>
  )
}

export function SkeletonProductCard() {
  return (
    <div className="bg-surface border border-border rounded-xl overflow-hidden animate-pulse">
      <div className="aspect-[4/3] bg-surface2" />
      <div className="p-4 space-y-2">
        <div className="h-4 bg-border rounded w-3/4" />
        <div className="h-3 bg-border rounded w-full" />
        <div className="h-3 bg-border rounded w-2/3" />
        <div className="flex justify-between mt-3">
          <div className="h-3 bg-border rounded w-16" />
          <div className="h-7 bg-border rounded w-14" />
        </div>
      </div>
    </div>
  )
}

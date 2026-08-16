import { useEffect, useState, useCallback } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import {
  Globe, Shield, Server, Package, CreditCard, Zap, Bell, Activity,
  Settings, Database, RefreshCw, CheckCircle2, XCircle, AlertCircle,
  Clock, Wifi, ExternalLink
} from 'lucide-react'
import axios from 'axios'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

// All health checks go through the API Gateway via the /health/* routes.
// These map to each service's /actuator/health endpoint.
const HEALTH_SERVICES = [
  {
    id: 'gateway',
    label: 'API Gateway',
    sublabel: 'Request routing & JWT validation',
    icon: Shield,
    color: '#8B7CFF',
    url: `${BASE_URL}/actuator/health`,
    port: 8080,
    swaggerUrl: null, // gateway has no business API to document
  },
  {
    id: 'auth',
    label: 'Auth Service',
    sublabel: 'JWT issuance & user management',
    icon: Shield,
    color: '#2FD98A',
    url: `${BASE_URL}/health/auth`,
    port: 8081,
    swaggerUrl: `${BASE_URL}/docs/auth/swagger-ui.html`,
  },
  {
    id: 'order',
    label: 'Order Service',
    sublabel: 'Saga orchestrator',
    icon: Server,
    color: '#6D5CF5',
    url: `${BASE_URL}/health/orders`,
    port: 8083,
    swaggerUrl: `${BASE_URL}/docs/orders/swagger-ui.html`,
  },
  {
    id: 'inventory',
    label: 'Inventory Service',
    sublabel: 'Stock management & reservation',
    icon: Package,
    color: '#2FD98A',
    url: `${BASE_URL}/health/inventory`,
    port: 8082,
    swaggerUrl: `${BASE_URL}/docs/inventory/swagger-ui.html`,
  },
  {
    id: 'payment',
    label: 'Payment Service',
    sublabel: 'Payment processing (~15% failure sim)',
    icon: CreditCard,
    color: '#2FD98A',
    url: `${BASE_URL}/health/payment`,
    port: 8084,
    swaggerUrl: `${BASE_URL}/docs/payment/swagger-ui.html`,
  },
  {
    id: 'notification',
    label: 'Notification Service',
    sublabel: 'Kafka consumer — async email',
    icon: Bell,
    color: '#8E95AB',
    url: `${BASE_URL}/health/notification`,
    port: 8085,
    swaggerUrl: `${BASE_URL}/docs/notification/swagger-ui.html`,
  },
]

// Infrastructure can't be health-checked from the browser directly.
// We infer their status from the services that depend on them.
const INFRA_SERVICES = [
  { id: 'kafka',    label: 'Apache Kafka',  sublabel: 'KRaft mode — event broker',    icon: Zap,      color: '#F59E0B', port: 9092, dependsOn: 'order' },
  { id: 'mysql',    label: 'MySQL',         sublabel: 'Auth & Order persistence',     icon: Database, color: '#F59E0B', port: 3306, dependsOn: 'order' },
  { id: 'postgres', label: 'PostgreSQL',    sublabel: 'Inventory persistence',        icon: Database, color: '#34B4FF', port: 5432, dependsOn: 'inventory' },
]

function statusBadge(status) {
  if (status === 'UP')      return { text: 'Healthy',    color: 'text-success', bg: 'bg-success/10 border-success/25',  dot: 'bg-success' }
  if (status === 'DOWN')    return { text: 'Down',       color: 'text-danger',  bg: 'bg-danger/10 border-danger/25',    dot: 'bg-danger'  }
  if (status === 'LOADING') return { text: 'Checking…', color: 'text-primary', bg: 'bg-primary/10 border-primary/25',  dot: 'bg-primary animate-pulse' }
  return                           { text: 'Unknown',    color: 'text-textdim', bg: 'bg-surface2 border-border',        dot: 'bg-textdim' }
}

async function checkService(svc) {
  const start = Date.now()
  try {
    const res = await axios.get(svc.url, { timeout: 5000 })
    const ms = Date.now() - start
    // Spring Boot actuator returns { status: "UP" }
    const isUp = res.status < 400 && res.data?.status === 'UP'
    return { status: isUp ? 'UP' : 'DOWN', ms }
  } catch {
    return { status: 'DOWN', ms: null }
  }
}

function ServiceCard({ service, state, simulated }) {
  const Icon = service.icon
  const badge = statusBadge(state?.status ?? 'LOADING')

  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      className="bg-surface border border-border rounded-xl p-4 hover:border-border/60 transition-all"
    >
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-start gap-3 min-w-0">
          <div
            className="w-9 h-9 rounded-lg flex items-center justify-center flex-shrink-0 mt-0.5"
            style={{ background: `${service.color}18`, border: `1px solid ${service.color}30` }}
          >
            <Icon size={16} style={{ color: service.color }} />
          </div>
          <div className="min-w-0">
            <div className="flex items-center gap-2 flex-wrap">
              <h3 className="font-medium text-sm">{service.label}</h3>
              {simulated && (
                <span className="text-[9px] font-mono text-textdim/50 bg-surface2 border border-border px-1.5 py-0.5 rounded">
                  inferred
                </span>
              )}
              {service.swaggerUrl && state?.status === 'UP' && (
                <a
                  href={service.swaggerUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-[9px] font-mono text-primary/70 hover:text-primary flex items-center gap-0.5 transition-colors"
                  title="Open Swagger UI"
                >
                  <ExternalLink size={9} /> API docs
                </a>
              )}
            </div>
            <p className="text-xs text-textdim truncate">{service.sublabel}</p>
            <p className="text-[10px] font-mono text-textdim/50 mt-0.5">:{service.port}</p>
          </div>
        </div>

        <div className="flex flex-col items-end gap-1 flex-shrink-0">
          <span className={`inline-flex items-center gap-1.5 text-[10px] font-mono px-2 py-1 rounded-full border ${badge.bg} ${badge.color}`}>
            <span className={`w-1.5 h-1.5 rounded-full ${badge.dot}`} />
            {badge.text}
          </span>
          {state?.ms != null && (
            <span className="text-[10px] font-mono text-textdim flex items-center gap-1">
              <Clock size={9} /> {state.ms}ms
            </span>
          )}
        </div>
      </div>
    </motion.div>
  )
}

export default function SystemHealthPage() {
  const [health, setHealth] = useState({})
  const [lastChecked, setLastChecked] = useState(null)
  const [checking, setChecking] = useState(false)

  const check = useCallback(async () => {
    setChecking(true)
    // Set all to LOADING first
    setHealth(() => {
      const next = {}
      HEALTH_SERVICES.forEach((s) => { next[s.id] = { status: 'LOADING', ms: null } })
      INFRA_SERVICES.forEach((s) => { next[s.id] = { status: 'LOADING', ms: null } })
      return next
    })

    const results = await Promise.allSettled(
      HEALTH_SERVICES.map((svc) => checkService(svc).then((r) => ({ id: svc.id, ...r })))
    )

    const next = {}
    results.forEach((r) => {
      if (r.status === 'fulfilled') {
        next[r.value.id] = { status: r.value.status, ms: r.value.ms }
      }
    })

    // Infer infrastructure status from dependent app services
    INFRA_SERVICES.forEach((s) => {
      const depStatus = next[s.dependsOn]?.status
      next[s.id] = {
        status: depStatus === 'UP' ? 'UP' : depStatus === 'DOWN' ? 'DOWN' : 'DOWN',
        ms: null,
      }
    })

    setHealth(next)
    setLastChecked(new Date())
    setChecking(false)
  }, [])

  useEffect(() => {
    check()
    const id = setInterval(check, 30000)
    return () => clearInterval(id)
  }, [check])

  const upCount   = Object.values(health).filter((v) => v?.status === 'UP').length
  const downCount = Object.values(health).filter((v) => v?.status === 'DOWN').length
  const total = HEALTH_SERVICES.length + INFRA_SERVICES.length

  const overallStatus =
    downCount === 0 && upCount === total ? 'operational' :
    downCount > 0  && upCount > 0        ? 'degraded'    :
    upCount === 0  && downCount > 0      ? 'down'        :
    'checking'

  return (
    <div className="min-h-screen py-12 px-4">
      <div className="max-w-5xl mx-auto">

        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-8">
          <div>
            <span className="text-xs font-mono text-success bg-success/10 border border-success/20 px-3 py-1 rounded-full">
              SYSTEM MONITORING
            </span>
            <h1 className="font-display font-bold text-3xl mt-3 mb-1">System Health</h1>
            <p className="text-textdim text-sm">Live status of all OrderFlow services. Links to Swagger UI when healthy.</p>
          </div>

          <div className="flex items-center gap-3">
            {lastChecked && (
              <span className="text-xs font-mono text-textdim flex items-center gap-1">
                <Clock size={11} /> {lastChecked.toLocaleTimeString()}
              </span>
            )}
            <button
              onClick={check}
              disabled={checking}
              className="flex items-center gap-2 px-4 py-2 bg-surface border border-border rounded-lg text-xs hover:border-primary/40 transition-all disabled:opacity-50"
            >
              <RefreshCw size={13} className={checking ? 'animate-spin' : ''} />
              Refresh
            </button>
          </div>
        </div>

        {/* Overall status banner */}
        <AnimatePresence mode="wait">
          <motion.div
            key={overallStatus}
            initial={{ opacity: 0, scale: 0.98 }}
            animate={{ opacity: 1, scale: 1 }}
            className={`flex items-center gap-4 p-4 rounded-xl border mb-8 ${
              overallStatus === 'operational' ? 'bg-success/8 border-success/25' :
              overallStatus === 'degraded'    ? 'bg-yellow-500/8 border-yellow-500/25' :
              overallStatus === 'down'        ? 'bg-danger/8 border-danger/25' :
              'bg-surface border-border'
            }`}
          >
            {overallStatus === 'operational' ? <CheckCircle2 size={20} className="text-success" /> :
             overallStatus === 'degraded'    ? <AlertCircle  size={20} className="text-yellow-400" /> :
             overallStatus === 'down'        ? <XCircle      size={20} className="text-danger" /> :
             <Wifi size={20} className="text-textdim animate-pulse" />}
            <div>
              <p className="font-medium text-sm">
                {overallStatus === 'operational' ? 'All systems operational' :
                 overallStatus === 'degraded'    ? 'Partial outage — some services unreachable' :
                 overallStatus === 'down'        ? 'All services unreachable — is Docker running?' :
                 'Checking service health…'}
              </p>
              <p className="text-xs text-textdim font-mono">
                {upCount} / {total} services up
                {overallStatus === 'down' && ' — run: docker compose up'}
              </p>
            </div>
          </motion.div>
        </AnimatePresence>

        {/* Application services */}
        <h2 className="font-display font-semibold text-xs text-textdim uppercase tracking-widest mb-3">
          Application Services
        </h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 mb-8">
          {HEALTH_SERVICES.map((svc) => (
            <ServiceCard key={svc.id} service={svc} state={health[svc.id]} simulated={false} />
          ))}
        </div>

        {/* Infrastructure */}
        <h2 className="font-display font-semibold text-xs text-textdim uppercase tracking-widest mb-3">
          Infrastructure
        </h2>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-10">
          {INFRA_SERVICES.map((svc) => (
            <ServiceCard key={svc.id} service={svc} state={health[svc.id]} simulated={true} />
          ))}
        </div>

        {/* Footer note */}
        <div className="bg-surface border border-border rounded-xl p-4 flex items-start gap-3">
          <AlertCircle size={15} className="text-textdim flex-shrink-0 mt-0.5" />
          <p className="text-xs text-textdim leading-relaxed">
            Health checks go through the API Gateway to each service's <code className="font-mono text-textdim/80">/actuator/health</code> endpoint.
            Infrastructure services (Kafka, MySQL, PostgreSQL) are inferred from their dependent application services.
            Checks auto-refresh every 30 seconds. "API docs" links open Swagger UI in a new tab.
          </p>
        </div>
      </div>
    </div>
  )
}

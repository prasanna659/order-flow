import { useState, useEffect, useRef } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import {
  Globe, Shield, Server, Package, CreditCard, Zap, Bell, Database,
  Settings, Activity, ChevronRight, Info, ArrowRight
} from 'lucide-react'

/* ─── Service definitions ─────────────────────────────────────────────────── */
const SERVICES = {
  frontend:      { id: 'frontend',      label: 'React Frontend',      port: '3000', icon: Globe,     color: '#6D5CF5', desc: 'Vite + React 18 SPA. Communicates exclusively through the API Gateway. Never calls backend services directly.', tech: ['React 18', 'Vite', 'Framer Motion', 'Tailwind CSS'] },
  gateway:       { id: 'gateway',       label: 'API Gateway',          port: '8080', icon: Shield,    color: '#8B7CFF', desc: 'Spring Cloud Gateway. Validates JWT tokens, routes requests to registered services via Eureka. Circuit breaker integration.', tech: ['Spring Cloud Gateway', 'Resilience4j', 'JWT Filter'] },
  auth:          { id: 'auth',          label: 'Auth Service',         port: '8081', icon: Shield,    color: '#2FD98A', desc: 'Issues and validates JWT tokens. User registration and login. Stores credentials in MySQL with bcrypt hashing.', tech: ['Spring Security', 'JWT', 'MySQL', 'BCrypt'] },
  order:         { id: 'order',         label: 'Order Service',        port: '8083', icon: Server,    color: '#6D5CF5', desc: 'Saga orchestrator. Coordinates inventory reservation and payment charging. Publishes Kafka events on completion.', tech: ['Spring Boot', 'Saga Pattern', 'Kafka Producer', 'MySQL'] },
  inventory:     { id: 'inventory',     label: 'Inventory Service',    port: '8082', icon: Package,   color: '#2FD98A', desc: 'Manages product stock. Participates in the saga as a resource manager. Supports reservation rollback for compensating transactions.', tech: ['Spring Boot', 'PostgreSQL', 'Reserve/Release API'] },
  payment:       { id: 'payment',       label: 'Payment Service',      port: '8084', icon: CreditCard,color: '#2FD98A', desc: 'Simulates payment processing. Random failure injection for demonstrating compensation. Called synchronously by Order Service.', tech: ['Spring Boot', 'Failure Simulation', 'REST'] },
  kafka:         { id: 'kafka',         label: 'Kafka',                port: '9092', icon: Zap,       color: '#F59E0B', desc: 'Apache Kafka in KRaft mode (no Zookeeper). Carries order-confirmed and order-cancelled events. Enables async notification delivery.', tech: ['Apache Kafka 3.7', 'KRaft Mode', 'Event Streaming'] },
  notification:  { id: 'notification',  label: 'Notification Service', port: '8085', icon: Bell,      color: '#8E95AB', desc: 'Kafka consumer that listens for order events and simulates sending email/SMS notifications. Fully async — decoupled from the order flow.', tech: ['Spring Boot', 'Kafka Consumer', 'Async Processing'] },
  eureka:        { id: 'eureka',        label: 'Eureka Server',        port: '8761', icon: Activity,  color: '#6D5CF5', desc: 'Netflix Eureka service registry. All services register themselves on startup and discover peers by logical name — no hardcoded IPs.', tech: ['Netflix Eureka', 'Service Registry', 'Health Monitoring'] },
  config:        { id: 'config',        label: 'Config Server',        port: '8888', icon: Settings,  color: '#6D5CF5', desc: 'Spring Cloud Config Server. Reads configuration from the /config-repo directory and serves it to all microservices at startup.', tech: ['Spring Cloud Config', 'Centralized Config', 'Environment Profiles'] },
  mysql:         { id: 'mysql',         label: 'MySQL',                port: '3306', icon: Database,  color: '#F59E0B', desc: 'Relational database for Auth Service (users table) and Order Service (orders, order_items tables). Each service owns its schema.', tech: ['MySQL 8', 'JPA', 'Flyway'] },
  postgres:      { id: 'postgres',      label: 'PostgreSQL',           port: '5432', icon: Database,  color: '#34B4FF', desc: 'Relational database dedicated to Inventory Service. Product catalog and stock quantities. Separate from MySQL — Database Per Service pattern.', tech: ['PostgreSQL 16', 'Spring Data JPA'] },
}

/* ─── Saga animation steps ────────────────────────────────────────────────── */
const SAGA_STEPS = [
  { services: ['frontend'], label: 'User adds items to cart' },
  { services: ['frontend', 'gateway'], label: 'Request hits API Gateway' },
  { services: ['gateway', 'order'], label: 'Gateway routes to Order Service' },
  { services: ['order', 'inventory'], label: 'Order Service reserves inventory' },
  { services: ['order', 'payment'], label: 'Order Service charges payment' },
  { services: ['order', 'kafka'], label: 'Order confirmed event published to Kafka' },
  { services: ['kafka', 'notification'], label: 'Notification Service consumes event' },
  { services: ['order', 'mysql'], label: 'Order status persisted in MySQL' },
]

/* ─── Layout rows for the diagram ────────────────────────────────────────── */
const DIAGRAM_ROWS = [
  { label: 'Client', ids: ['frontend'] },
  { label: 'Platform', ids: ['gateway', 'eureka', 'config'] },
  { label: 'Business', ids: ['auth', 'order', 'inventory', 'payment', 'notification'] },
  { label: 'Messaging', ids: ['kafka'] },
  { label: 'Data', ids: ['mysql', 'postgres'] },
]

function ServiceNode({ service, active, onClick, isSelected }) {
  const Icon = service.icon
  return (
    <motion.button
      onClick={() => onClick(service.id)}
      animate={active ? {
        boxShadow: [`0 0 0 1px ${service.color}40`, `0 0 24px 2px ${service.color}60`, `0 0 0 1px ${service.color}40`]
      } : { boxShadow: 'none' }}
      transition={active ? { duration: 1, repeat: Infinity } : {}}
      whileHover={{ scale: 1.04 }}
      whileTap={{ scale: 0.97 }}
      className={`flex flex-col items-center gap-1.5 p-2.5 rounded-xl border transition-all cursor-pointer min-w-[80px] ${
        isSelected
          ? 'border-white/40 bg-surface2'
          : active
          ? 'border-opacity-50 bg-surface2'
          : 'border-border bg-surface hover:border-border/80 hover:bg-surface2'
      }`}
      style={active ? { borderColor: `${service.color}60` } : undefined}
    >
      <div
        className="w-8 h-8 rounded-lg flex items-center justify-center"
        style={{ background: `${service.color}18`, border: `1px solid ${service.color}35` }}
      >
        <Icon size={15} style={{ color: service.color }} />
      </div>
      <span className="text-[10px] font-mono text-center leading-tight text-textdim">{service.label}</span>
      <span
        className="text-[9px] font-mono px-1 py-0.5 rounded"
        style={{ background: `${service.color}15`, color: service.color }}
      >
        :{service.port}
      </span>
    </motion.button>
  )
}

export default function ArchitecturePage() {
  const [selected, setSelected] = useState(null)
  const [sagaStep, setSagaStep] = useState(null)
  const [running, setRunning]   = useState(false)

  // ── Auto-play setting — persisted in localStorage ──────────────────
  const [autoPlay, setAutoPlay] = useState(() => {
    try {
      const stored = localStorage.getItem('autoPlaySaga')
      return stored === null ? false : stored === 'true'
    } catch {
      return false
    }
  })

  function toggleAutoPlay() {
    setAutoPlay((prev) => {
      const next = !prev
      try { localStorage.setItem('autoPlaySaga', String(next)) } catch {}
      return next
    })
  }

  // ── Trigger on mount if auto-play is enabled ───────────────────────
  const hasAutoPlayed = useRef(false)
  useEffect(() => {
    if (!autoPlay || hasAutoPlayed.current) return
    hasAutoPlayed.current = true
    const id = setTimeout(() => { runSaga() }, 500)
    return () => clearTimeout(id)
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  function selectService(id) {
    setSelected((prev) => (prev === id ? null : id))
  }

  async function runSaga() {
    if (running) return
    setRunning(true)
    setSelected(null)
    for (let i = 0; i < SAGA_STEPS.length; i++) {
      setSagaStep(i)
      await new Promise((r) => setTimeout(r, 900))
    }
    await new Promise((r) => setTimeout(r, 600))
    setSagaStep(null)
    setRunning(false)
  }

  const activeServices = sagaStep !== null ? SAGA_STEPS[sagaStep].services : []

  return (
    <div className="min-h-screen py-12 px-4">
      <div className="max-w-6xl mx-auto">

        {/* Header */}
        <div className="mb-10 text-center">
          <span className="text-xs font-mono text-primary bg-primary/10 border border-primary/20 px-3 py-1 rounded-full">
            SYSTEM DESIGN
          </span>
          <h1 className="font-display font-bold text-3xl mt-4 mb-2">Architecture Overview</h1>
          <p className="text-textdim text-sm max-w-lg mx-auto">
            Click any service to see its role. Run the saga animation to watch how an order flows through the system.
          </p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

          {/* ── Diagram ────────────────────────────────────────────── */}
          <div className="lg:col-span-2 bg-surface border border-border rounded-2xl p-6">
            <div className="flex items-center justify-between mb-6">
              <h2 className="font-display font-semibold text-sm">Service Map</h2>
              <div className="flex items-center gap-3">
                {/* Auto-play toggle */}
                <label className="flex items-center gap-2 cursor-pointer select-none group">
                  <span className="text-[10px] font-mono text-textdim group-hover:text-white transition-colors">
                    Auto-play on load
                  </span>
                  <button
                    role="switch"
                    aria-checked={autoPlay}
                    onClick={toggleAutoPlay}
                    className={`relative w-8 h-4 rounded-full border transition-all duration-200 flex-shrink-0 ${
                      autoPlay
                        ? 'bg-primary/30 border-primary/50'
                        : 'bg-surface2 border-border'
                    }`}
                  >
                    <span
                      className={`absolute top-0.5 left-0.5 w-3 h-3 rounded-full transition-all duration-200 ${
                        autoPlay ? 'translate-x-4 bg-primary' : 'translate-x-0 bg-textdim/40'
                      }`}
                    />
                  </button>
                </label>

                {/* Animate Saga button */}
                <button
                  onClick={runSaga}
                  disabled={running}
                  className="flex items-center gap-2 px-4 py-2 bg-primary/15 border border-primary/30 text-primary rounded-lg text-xs font-medium hover:bg-primary/25 transition-all disabled:opacity-50"
                >
                  {running ? (
                    <>
                      <span className="w-2 h-2 rounded-full bg-primary animate-pulse" />
                      Simulating…
                    </>
                  ) : (
                    <>
                      <ChevronRight size={14} /> Animate Saga
                    </>
                  )}
                </button>
              </div>
            </div>

            {/* Step label */}
            <AnimatePresence mode="wait">
              {sagaStep !== null && (
                <motion.div
                  key={sagaStep}
                  initial={{ opacity: 0, y: -6 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0 }}
                  className="mb-4 flex items-center gap-2 bg-primary/10 border border-primary/25 rounded-lg px-3 py-2"
                >
                  <span className="text-xs font-mono text-primary">Step {sagaStep + 1}/{SAGA_STEPS.length}</span>
                  <ArrowRight size={12} className="text-primary" />
                  <span className="text-xs text-white">{SAGA_STEPS[sagaStep].label}</span>
                </motion.div>
              )}
            </AnimatePresence>

            {/* Rows */}
            <div className="space-y-4">
              {DIAGRAM_ROWS.map((row) => (
                <div key={row.label}>
                  <p className="text-[10px] font-mono text-textdim/50 uppercase tracking-widest mb-2">{row.label} Layer</p>
                  <div className="flex flex-wrap gap-2">
                    {row.ids.map((id) => (
                      <ServiceNode
                        key={id}
                        service={SERVICES[id]}
                        active={activeServices.includes(id)}
                        isSelected={selected === id}
                        onClick={selectService}
                      />
                    ))}
                  </div>
                  {row !== DIAGRAM_ROWS[DIAGRAM_ROWS.length - 1] && (
                    <div className="mt-3 border-b border-dashed border-border/40" />
                  )}
                </div>
              ))}
            </div>

            {/* Legend */}
            <div className="mt-6 pt-4 border-t border-border flex flex-wrap gap-4 text-[10px] font-mono text-textdim">
              <span className="flex items-center gap-1.5"><span className="w-2.5 h-2.5 rounded border border-primary/40 bg-primary/15" /> Active in saga</span>
              <span className="flex items-center gap-1.5"><span className="w-2.5 h-2.5 rounded border border-border bg-surface" /> Idle</span>
              <span className="flex items-center gap-1.5"><span className="w-2.5 h-2.5 rounded border border-white/30 bg-surface2" /> Selected</span>
            </div>
          </div>

          {/* ── Detail Panel ────────────────────────────────────────── */}
          <div className="space-y-4">
            <AnimatePresence mode="wait">
              {selected ? (
                <motion.div
                  key={selected}
                  initial={{ opacity: 0, x: 16 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: -16 }}
                  className="bg-surface border border-border rounded-2xl p-5"
                >
                  {(() => {
                    const svc = SERVICES[selected]
                    const Icon = svc.icon
                    return (
                      <>
                        <div className="flex items-center gap-3 mb-4">
                          <div
                            className="w-10 h-10 rounded-xl flex items-center justify-center"
                            style={{ background: `${svc.color}18`, border: `1px solid ${svc.color}35` }}
                          >
                            <Icon size={18} style={{ color: svc.color }} />
                          </div>
                          <div>
                            <h3 className="font-display font-semibold text-sm">{svc.label}</h3>
                            <span className="text-xs font-mono text-textdim">:{svc.port}</span>
                          </div>
                        </div>
                        <p className="text-textdim text-xs leading-relaxed mb-4">{svc.desc}</p>
                        <div>
                          <p className="text-[10px] font-mono text-textdim/60 uppercase tracking-widest mb-2">Technologies</p>
                          <div className="flex flex-wrap gap-1.5">
                            {svc.tech.map((t) => (
                              <span
                                key={t}
                                className="text-[10px] font-mono px-2 py-0.5 rounded border border-border bg-surface2 text-textdim"
                              >
                                {t}
                              </span>
                            ))}
                          </div>
                        </div>
                      </>
                    )
                  })()}
                </motion.div>
              ) : (
                <motion.div
                  key="hint"
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  exit={{ opacity: 0 }}
                  className="bg-surface border border-border rounded-2xl p-5 flex flex-col items-center justify-center text-center gap-3 min-h-[160px]"
                >
                  <Info size={22} className="text-textdim/40" />
                  <p className="text-textdim text-xs">Click any service in the diagram to see its role and technologies</p>
                </motion.div>
              )}
            </AnimatePresence>

            {/* Saga steps list */}
            <div className="bg-surface border border-border rounded-2xl p-5">
              <h3 className="font-display font-semibold text-sm mb-4">Saga Steps</h3>
              <div className="space-y-2">
                {SAGA_STEPS.map((step, i) => (
                  <motion.div
                    key={i}
                    animate={sagaStep === i
                      ? { backgroundColor: 'rgba(109,92,245,0.12)' }
                      : { backgroundColor: 'rgba(109,92,245,0)' }
                    }
                    className="flex items-start gap-2.5 px-2.5 py-2 rounded-lg"
                  >
                    <span className={`text-[10px] font-mono mt-0.5 w-4 text-right flex-shrink-0 ${
                      sagaStep !== null && i < sagaStep ? 'text-success' :
                      sagaStep === i ? 'text-primary' : 'text-textdim/40'
                    }`}>
                      {sagaStep !== null && i < sagaStep ? '✓' : String(i + 1).padStart(2, '0')}
                    </span>
                    <span className={`text-xs transition-colors ${
                      sagaStep !== null && i < sagaStep ? 'text-success' :
                      sagaStep === i ? 'text-white' : 'text-textdim'
                    }`}>{step.label}</span>
                  </motion.div>
                ))}
              </div>
            </div>

            {/* Data flows */}
            <div className="bg-surface border border-border rounded-2xl p-5">
              <h3 className="font-display font-semibold text-sm mb-4">Data Flows</h3>
              <div className="space-y-2 text-xs text-textdim">
                {[
                  { from: 'Frontend', to: 'Gateway', proto: 'REST/JWT' },
                  { from: 'Gateway', to: 'Services', proto: 'HTTP/Eureka' },
                  { from: 'Order Svc', to: 'Inventory', proto: 'REST (sync)' },
                  { from: 'Order Svc', to: 'Payment', proto: 'REST (sync)' },
                  { from: 'Order Svc', to: 'Kafka', proto: 'Event Publish' },
                  { from: 'Kafka', to: 'Notification', proto: 'Event Consume' },
                ].map((flow) => (
                  <div key={`${flow.from}-${flow.to}`} className="flex items-center gap-2">
                    <span className="font-mono text-[10px] w-20 text-right">{flow.from}</span>
                    <ArrowRight size={10} className="text-primary flex-shrink-0" />
                    <span className="font-mono text-[10px] w-20">{flow.to}</span>
                    <span className="text-[9px] font-mono text-primary/60 bg-primary/10 px-1.5 py-0.5 rounded">{flow.proto}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

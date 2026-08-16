import { motion } from 'framer-motion'
import { Link } from 'react-router-dom'
import {
  GitBranch, Zap, Shield, Server, Database, RefreshCw,
  ArrowRight, Activity, Network, CheckCircle2, Layers, Cpu
} from 'lucide-react'

const TECH_CARDS = [
  {
    icon: GitBranch,
    title: 'Saga Pattern',
    color: 'primary',
    description: 'Distributed transactions split into a chain of local transactions. Each step publishes an event that triggers the next service.',
  },
  {
    icon: RefreshCw,
    title: 'Compensating Transactions',
    color: 'danger',
    description: 'When payment fails, the system automatically rolls back the inventory reservation — no manual intervention needed.',
  },
  {
    icon: Zap,
    title: 'Kafka Event Streaming',
    color: 'success',
    description: 'Services communicate asynchronously through Kafka topics. Order confirmation triggers notification events in real time.',
  },
  {
    icon: Activity,
    title: 'Circuit Breakers',
    color: 'primary',
    description: 'Resilience4j circuit breakers protect upstream services from cascading failures when downstream services become unavailable.',
  },
  {
    icon: Shield,
    title: 'JWT Authentication',
    color: 'success',
    description: 'Stateless JWT tokens validated at the API Gateway layer. No session state stored on the server side.',
  },
  {
    icon: Server,
    title: 'Service Discovery',
    color: 'primary',
    description: 'Netflix Eureka registry allows services to discover and communicate with each other without hardcoded URLs.',
  },
  {
    icon: Network,
    title: 'API Gateway',
    color: 'primary',
    description: 'Spring Cloud Gateway handles routing, JWT validation, and cross-cutting concerns for all downstream services.',
  },
  {
    icon: Database,
    title: 'Database Per Service',
    color: 'success',
    description: 'Auth/Order use MySQL; Inventory uses PostgreSQL. Each service owns its data — no shared database coupling.',
  },
  {
    icon: Layers,
    title: 'Config Server',
    color: 'primary',
    description: 'Centralized Spring Cloud Config Server serves environment-specific configuration to all microservices at startup.',
  },
]

const SERVICES = [
  { name: 'React Frontend', type: 'UI', color: 'text-primary' },
  { name: 'API Gateway :8080', type: 'Gateway', color: 'text-primarylight' },
  { name: 'Auth Service :8081', type: 'Auth', color: 'text-success' },
  { name: 'Order Service :8083', type: 'Orchestrator', color: 'text-primary' },
  { name: 'Inventory Service :8082', type: 'Participant', color: 'text-success' },
  { name: 'Payment Service :8084', type: 'Participant', color: 'text-success' },
  { name: 'Kafka :9092', type: 'Broker', color: 'text-yellow-400' },
  { name: 'Notification :8085', type: 'Consumer', color: 'text-textdim' },
]

const COLOR_MAP = {
  primary: { bg: 'bg-primary/10', border: 'border-primary/25', text: 'text-primary', icon: 'bg-primary/15' },
  success: { bg: 'bg-success/10', border: 'border-success/25', text: 'text-success', icon: 'bg-success/15' },
  danger:  { bg: 'bg-danger/10',  border: 'border-danger/25',  text: 'text-danger',  icon: 'bg-danger/15'  },
}

const container = {
  hidden: { opacity: 0 },
  show: { opacity: 1, transition: { staggerChildren: 0.07 } },
}
const item = {
  hidden: { opacity: 0, y: 16 },
  show:   { opacity: 1, y: 0, transition: { duration: 0.4 } },
}

export default function LandingPage() {
  return (
    <div className="min-h-screen overflow-x-hidden">

      {/* ── Hero ─────────────────────────────────────────────────── */}
      <section className="relative pt-20 pb-24 px-4 overflow-hidden">
        {/* Ambient glows */}
        <div className="pointer-events-none absolute top-0 left-1/2 -translate-x-1/2 w-[900px] h-[500px] rounded-full bg-primary/8 blur-3xl" />
        <div className="pointer-events-none absolute top-20 right-0 w-64 h-64 rounded-full bg-success/5 blur-3xl" />

        {/* Grid dots */}
        <div className="absolute inset-0 bg-grid opacity-40 pointer-events-none" />

        <div className="relative max-w-5xl mx-auto text-center">
          {/* Badge */}
          <motion.div
            initial={{ opacity: 0, y: -8 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5 }}
            className="inline-flex items-center gap-2 bg-surface border border-primary/25 rounded-full px-4 py-1.5 text-xs font-mono text-primary mb-8"
          >
            <span className="w-2 h-2 rounded-full bg-primary animate-pulse" />
            Distributed Systems Demo Platform
          </motion.div>

          {/* Title */}
          <motion.h1
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1, duration: 0.6 }}
            className="font-display font-bold text-5xl sm:text-6xl lg:text-7xl tracking-tight mb-6 leading-[1.08]"
          >
            Order
            <span className="bg-gradient-to-r from-primary to-primarylight bg-clip-text text-transparent">Flow</span>
          </motion.h1>

          <motion.p
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2, duration: 0.5 }}
            className="text-xl text-textdim max-w-2xl mx-auto mb-4 leading-relaxed"
          >
            Event-Driven Order Processing System
          </motion.p>

          <motion.p
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.3, duration: 0.5 }}
            className="text-sm text-textdim/70 max-w-xl mx-auto mb-12 font-mono"
          >
            A production-grade microservices architecture demonstrating the Saga Pattern,
            Kafka event streaming, and compensating transactions — built with Spring Boot 3.2 &amp; Java 17.
          </motion.p>

          {/* CTA Buttons */}
          <motion.div
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.4, duration: 0.4 }}
            className="flex flex-wrap justify-center gap-3 mb-16"
          >
            <Link
              to="/catalog"
              className="flex items-center gap-2 px-6 py-3 bg-primary text-white font-semibold rounded-xl shadow-glow hover:brightness-110 hover:-translate-y-0.5 transition-all text-sm"
            >
              Explore Demo <ArrowRight size={16} />
            </Link>
            <Link
              to="/architecture"
              className="flex items-center gap-2 px-6 py-3 bg-surface border border-border text-white font-medium rounded-xl hover:border-primary/50 hover:-translate-y-0.5 transition-all text-sm"
            >
              <Network size={16} /> View Architecture
            </Link>
            <Link
              to="/health"
              className="flex items-center gap-2 px-6 py-3 bg-surface border border-border text-textdim font-medium rounded-xl hover:border-success/40 hover:text-success hover:-translate-y-0.5 transition-all text-sm"
            >
              <Activity size={16} /> System Health
            </Link>
          </motion.div>

          {/* Service badges ticker */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.5 }}
            className="overflow-hidden"
          >
            <div className="flex gap-3 ticker-content">
              {[...SERVICES, ...SERVICES].map((s, i) => (
                <div
                  key={i}
                  className="flex items-center gap-2 px-3 py-1.5 bg-surface border border-border rounded-full text-xs whitespace-nowrap flex-shrink-0"
                >
                  <span className={`w-1.5 h-1.5 rounded-full bg-current ${s.color}`} />
                  <span className="font-mono text-textdim">{s.name}</span>
                  <span className={`text-[10px] ${s.color}`}>{s.type}</span>
                </div>
              ))}
            </div>
          </motion.div>
        </div>
      </section>

      {/* ── Stats Strip ──────────────────────────────────────────── */}
      <section className="border-y border-border bg-surface/40 py-6 px-4">
        <div className="max-w-5xl mx-auto grid grid-cols-2 sm:grid-cols-4 gap-4 text-center">
          {[
            { value: '9', label: 'Microservices',    unit: 'services' },
            { value: '3', label: 'Saga Steps',       unit: 'per order' },
            { value: '2', label: 'Databases',        unit: 'MySQL + PG' },
            { value: '∞', label: 'Event-Driven',     unit: 'Kafka KRaft' },
          ].map((stat) => (
            <div key={stat.label} className="py-2">
              <div className="font-display font-bold text-3xl text-white">{stat.value}</div>
              <div className="text-xs text-textdim mt-0.5">{stat.label}</div>
              <div className="text-[10px] font-mono text-primary mt-0.5">{stat.unit}</div>
            </div>
          ))}
        </div>
      </section>

      {/* ── Saga Flow Explainer ───────────────────────────────────── */}
      <section className="py-20 px-4">
        <div className="max-w-5xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 12 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-center mb-12"
          >
            <span className="text-xs font-mono text-primary bg-primary/10 border border-primary/20 px-3 py-1 rounded-full">
              CORE PATTERN
            </span>
            <h2 className="font-display font-bold text-3xl mt-4 mb-3">The Saga Workflow</h2>
            <p className="text-textdim text-sm max-w-lg mx-auto">
              Every order triggers a choreographed sequence of distributed transactions.
              Failures at any step trigger automatic compensation.
            </p>
          </motion.div>

          {/* Happy path */}
          <div className="mb-10">
            <p className="text-xs font-mono text-success mb-4 text-center uppercase tracking-widest">Happy Path</p>
            <div className="flex flex-wrap justify-center gap-0">
              {[
                { step: '01', label: 'Order Created',      service: 'Order Service',     status: 'success' },
                { step: '02', label: 'Stock Reserved',     service: 'Inventory Service', status: 'success' },
                { step: '03', label: 'Payment Charged',    service: 'Payment Service',   status: 'success' },
                { step: '04', label: 'Event Published',    service: 'Kafka',             status: 'success' },
                { step: '05', label: 'Notification Sent',  service: 'Notification Svc',  status: 'success' },
                { step: '06', label: 'Order Confirmed',    service: 'Order Service',     status: 'confirm' },
              ].map((node, i, arr) => (
                <div key={node.step} className="flex items-center">
                  <motion.div
                    initial={{ opacity: 0, scale: 0.9 }}
                    whileInView={{ opacity: 1, scale: 1 }}
                    viewport={{ once: true }}
                    transition={{ delay: i * 0.1 }}
                    className={`flex flex-col items-center gap-1.5 px-3 py-3 rounded-xl border text-center min-w-[100px] ${
                      node.status === 'confirm'
                        ? 'bg-success/10 border-success/30'
                        : 'bg-surface border-border'
                    }`}
                  >
                    <span className="text-[10px] font-mono text-textdim">{node.step}</span>
                    <CheckCircle2 size={16} className={node.status === 'confirm' ? 'text-success' : 'text-success'} />
                    <span className="text-xs font-medium text-white leading-tight">{node.label}</span>
                    <span className="text-[10px] font-mono text-textdim">{node.service}</span>
                  </motion.div>
                  {i < arr.length - 1 && (
                    <div className="w-4 h-px bg-success/40 mx-0.5 flex-shrink-0" />
                  )}
                </div>
              ))}
            </div>
          </div>

          {/* Failure path */}
          <div>
            <p className="text-xs font-mono text-danger mb-4 text-center uppercase tracking-widest">Compensation Path (Payment Failure)</p>
            <div className="flex flex-wrap justify-center gap-0">
              {[
                { step: '01', label: 'Order Created',         service: 'Order Service',     type: 'done'    },
                { step: '02', label: 'Stock Reserved',        service: 'Inventory Service', type: 'done'    },
                { step: '03', label: 'Payment Failed',        service: 'Payment Service',   type: 'fail'    },
                { step: '↩',  label: 'Release Stock',         service: 'Inventory Service', type: 'comp'    },
                { step: '✓',  label: 'Order Cancelled',       service: 'Order Service',     type: 'cancel'  },
              ].map((node, i, arr) => (
                <div key={`comp-${i}`} className="flex items-center">
                  <motion.div
                    initial={{ opacity: 0, scale: 0.9 }}
                    whileInView={{ opacity: 1, scale: 1 }}
                    viewport={{ once: true }}
                    transition={{ delay: i * 0.1 }}
                    className={`flex flex-col items-center gap-1.5 px-3 py-3 rounded-xl border text-center min-w-[100px] ${
                      node.type === 'fail'   ? 'bg-danger/10 border-danger/30' :
                      node.type === 'comp'   ? 'bg-yellow-500/10 border-yellow-500/30' :
                      node.type === 'cancel' ? 'bg-danger/10 border-danger/30' :
                      'bg-surface border-border'
                    }`}
                  >
                    <span className="text-[10px] font-mono text-textdim">{node.step}</span>
                    <span className={`text-base ${
                      node.type === 'fail' ? 'text-danger' :
                      node.type === 'comp' ? 'text-yellow-400' :
                      node.type === 'cancel' ? 'text-danger' :
                      'text-success'
                    }`}>
                      {node.type === 'done' ? '✓' : node.type === 'fail' ? '✗' : node.type === 'comp' ? '↩' : '✗'}
                    </span>
                    <span className="text-xs font-medium text-white leading-tight">{node.label}</span>
                    <span className="text-[10px] font-mono text-textdim">{node.service}</span>
                  </motion.div>
                  {i < arr.length - 1 && (
                    <div className={`w-4 h-px mx-0.5 flex-shrink-0 ${
                      i >= 2 ? 'bg-yellow-500/40' : 'bg-success/40'
                    }`} />
                  )}
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* ── Tech Cards ───────────────────────────────────────────── */}
      <section className="py-20 px-4 bg-surface/20">
        <div className="max-w-5xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 12 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-center mb-12"
          >
            <span className="text-xs font-mono text-success bg-success/10 border border-success/20 px-3 py-1 rounded-full">
              WHAT THIS DEMONSTRATES
            </span>
            <h2 className="font-display font-bold text-3xl mt-4 mb-3">Engineering Patterns</h2>
            <p className="text-textdim text-sm max-w-lg mx-auto">
              Each purchase exercises a full suite of distributed systems patterns.
              These are production-grade techniques, not textbook examples.
            </p>
          </motion.div>

          <motion.div
            variants={container}
            initial="hidden"
            whileInView="show"
            viewport={{ once: true }}
            className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4"
          >
            {TECH_CARDS.map((card) => {
              const c = COLOR_MAP[card.color]
              return (
                <motion.div
                  key={card.title}
                  variants={item}
                  className={`bg-surface border ${c.border} rounded-xl p-5 hover:-translate-y-1 hover:shadow-card transition-all duration-300 group`}
                >
                  <div className={`w-9 h-9 rounded-lg ${c.icon} border ${c.border} flex items-center justify-center mb-3`}>
                    <card.icon size={17} className={c.text} />
                  </div>
                  <h3 className={`font-display font-semibold text-sm mb-1.5 ${c.text}`}>{card.title}</h3>
                  <p className="text-textdim text-xs leading-relaxed">{card.description}</p>
                </motion.div>
              )
            })}
          </motion.div>
        </div>
      </section>

      {/* ── Tech Stack Strip ─────────────────────────────────────── */}
      <section className="py-16 px-4 border-t border-border">
        <div className="max-w-5xl mx-auto">
          <p className="text-center text-xs font-mono text-textdim/60 uppercase tracking-widest mb-8">Built With</p>
          <div className="flex flex-wrap justify-center gap-3">
            {[
              'Spring Boot 3.2', 'Java 17', 'Spring Cloud Gateway',
              'Netflix Eureka', 'Apache Kafka', 'Resilience4j',
              'MySQL 8', 'PostgreSQL 16', 'Spring Cloud Config',
              'Docker', 'React 18', 'Vite', 'Tailwind CSS', 'Framer Motion'
            ].map((tech) => (
              <span
                key={tech}
                className="px-3 py-1.5 bg-surface border border-border rounded-lg text-xs font-mono text-textdim hover:text-white hover:border-primary/40 transition-colors"
              >
                {tech}
              </span>
            ))}
          </div>
        </div>
      </section>

      {/* ── CTA Footer ───────────────────────────────────────────── */}
      <section className="py-20 px-4 text-center">
        <div className="max-w-xl mx-auto">
          <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-primary to-primarylight mx-auto mb-4 flex items-center justify-center shadow-glow">
            <Cpu size={22} className="text-white" />
          </div>
          <h2 className="font-display font-bold text-2xl mb-3">See it in action</h2>
          <p className="text-textdim text-sm mb-8">
            Sign in and place an order to watch the Saga Pattern execute in real time — including automatic compensation if payment fails.
          </p>
          <div className="flex justify-center gap-3">
            <Link
              to="/auth"
              className="px-6 py-3 bg-primary text-white font-semibold rounded-xl shadow-glow hover:brightness-110 hover:-translate-y-0.5 transition-all text-sm flex items-center gap-2"
            >
              Get Started <ArrowRight size={16} />
            </Link>
            <Link
              to="/architecture"
              className="px-6 py-3 bg-surface border border-border text-textdim rounded-xl hover:border-primary/40 hover:text-white transition-all text-sm flex items-center gap-2"
            >
              <Network size={16} /> Architecture Diagram
            </Link>
          </div>
        </div>
      </section>
    </div>
  )
}

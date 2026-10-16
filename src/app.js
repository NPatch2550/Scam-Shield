/**
 * Scam Shield – Express Application
 */
const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const rateLimit = require('express-rate-limit');
const path = require('path');

// Route modules
const verifyRoutes = require('./routes/verify');
const callRoutes = require('./routes/call');
const reportRoutes = require('./routes/report');
const adminRoutes = require('./routes/admin');
const dashboardRoutes = require('./routes/dashboard');
const { errorHandler } = require('./middleware/errorHandler');

const app = express();

// ── Security & Utilities ────────────────────────────────────────────────────
app.use(helmet({ contentSecurityPolicy: false }));
app.use(cors({ origin: '*' }));
app.use(morgan('dev'));
app.use(express.json({ limit: '2mb' }));
app.use(express.urlencoded({ extended: true }));

// ── Rate Limiting ────────────────────────────────────────────────────────────
const limiter = rateLimit({
  windowMs: 60 * 1000,   // 1 minute
  max: 120,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'Too many requests, please slow down.' },
});
app.use('/api/', limiter);

// ── Static Admin & Mobile App Portals ────────────────────────────────────────
app.use('/admin', express.static(path.join(__dirname, '../public/admin')));
app.use('/app',   express.static(path.join(__dirname, '../public/app')));

// ── API Routes ───────────────────────────────────────────────────────────────
app.use('/api/v1/verify',    verifyRoutes);
app.use('/api/v1/call',      callRoutes);
app.use('/api/v1/report',    reportRoutes);
app.use('/api/v1/admin',     adminRoutes);
app.use('/api/v1/dashboard', dashboardRoutes);

// ── Health check ─────────────────────────────────────────────────────────────
app.get('/health', (req, res) => res.json({ status: 'ok', ts: new Date() }));

// ── Root landing page ────────────────────────────────────────────────────────
app.get('/', (req, res) => {
  if (req.accepts('html')) {
    return res.redirect('/app');
  }
  res.json({
    name: 'Scam Shield API',
    version: '1.0.0',
    android_app: '/app',
    admin_portal: '/admin',
    endpoints: {
      verify_search:   'POST /api/v1/verify/search',
      verify_domain:   'POST /api/v1/verify/domain',
      verify_account:  'POST /api/v1/verify/account',
      call_analyze:    'POST /api/v1/call/analyze',
      call_transcript: 'POST /api/v1/call/transcript',
      report_submit:   'POST /api/v1/report/submit',
      admin_reports:   'GET  /api/v1/admin/reports',
      admin_approve:   'PUT  /api/v1/admin/reports/:id/approve',
      dashboard:       'GET  /api/v1/dashboard',
    },
  });
});

app.use(errorHandler);
module.exports = app;

/**
 * Scam Shield – Entry Point
 * Bootstraps MongoDB (memory or real), auto-seeds example data, and starts Socket.io server.
 */
require('dotenv').config();
const http = require('http');
const app = require('./app');
const { connectDB } = require('./db');
const { initSocket } = require('./socket');
const logger = require('./utils/logger');

const PORT = process.env.PORT || 3000;

async function autoSeedIfEmpty() {
  const ScamEntity = require('./models/ScamEntity');
  const count = await ScamEntity.countDocuments();
  if (count === 0) {
    logger.info('📦  Database is empty – auto-seeding example data...');
    // Dynamically require seed data arrays and insert them
    const LegitCompany = require('./models/LegitCompany');
    const Report = require('./models/Report');
    // Inline seed arrays (kept here so server.js is self-contained)
    const { SCAM_ENTITIES, LEGIT_COMPANIES, SAMPLE_REPORTS } = require('./scripts/seedData');
    await ScamEntity.insertMany(SCAM_ENTITIES);
    await LegitCompany.insertMany(LEGIT_COMPANIES);
    await Report.insertMany(SAMPLE_REPORTS);
    logger.info(`✅  Auto-seeded: ${SCAM_ENTITIES.length} scam entities, ${LEGIT_COMPANIES.length} legit companies, ${SAMPLE_REPORTS.length} reports`);
  } else {
    logger.info(`ℹ️   Database has ${count} scam entities – skipping auto-seed`);
  }
}

(async () => {
  try {
    await connectDB();
    await autoSeedIfEmpty();
    const server = http.createServer(app);
    initSocket(server);
    server.listen(PORT, '0.0.0.0', () => {
      logger.info(`🛡️  Scam Shield Backend running on http://localhost:${PORT}`);
      logger.info(`📱  Android Emulator (AVD): http://10.0.2.2:${PORT}/app`);
      logger.info(`📱  Android Emulator / LAN: http://192.168.0.113:${PORT}/app`);
      logger.info(`📊  Dashboard:  http://localhost:${PORT}/api/v1/dashboard`);
      logger.info(`🔍  Search:     POST http://localhost:${PORT}/api/v1/verify/search`);
      logger.info(`🌐  Admin:      http://localhost:${PORT}/admin`);
      logger.info(`🔑  Admin Key:  scamshield-admin-2024`);
    });
  } catch (err) {
    logger.error('Failed to start server:', err);
    process.exit(1);
  }
})();

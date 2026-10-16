/**
 * Scam Shield – Crowdsourced Report Routes
 */
const express = require('express');
const router = express.Router();
const Report = require('../models/Report');
const domainAnalyzer = require('../services/domainAnalyzer');
const logger = require('../utils/logger');

/**
 * POST /api/v1/report/submit
 * User submits a scam report from the app or Floating Bubble.
 */
router.post('/submit', async (req, res, next) => {
  try {
    const {
      submittedBy, entityType, entityValue,
      description, scamCategory, evidenceText,
    } = req.body;

    if (!entityType || !entityValue) {
      return res.status(400).json({ error: 'entityType and entityValue are required' });
    }

    // Auto-enrich domain reports
    let enrichment = {};
    if (entityType === 'domain' || entityType === 'url') {
      try {
        const domainInfo = await domainAnalyzer.analyze(entityValue);
        enrichment = {
          domainAge: domainInfo.ageMonths,
          isTyposquat: domainInfo.isTyposquat,
          inBlacklist: domainInfo.inBlacklist,
        };
      } catch (_) {}
    }

    const report = await Report.create({
      submittedBy: submittedBy || 'anonymous',
      entityType,
      entityValue,
      description,
      scamCategory: scamCategory || 'unknown',
      evidenceText,
      enrichment,
      status: 'pending',
    });

    logger.info(`[Report] New report submitted: ${entityType}:${entityValue}`);
    res.status(201).json({ reportId: report._id, status: 'pending', message: 'Report received. Our team will review within 24 hours.' });
  } catch (err) {
    next(err);
  }
});

/**
 * GET /api/v1/report/status/:id
 * Check report status.
 */
router.get('/status/:id', async (req, res, next) => {
  try {
    const report = await Report.findById(req.params.id).select('-evidenceText').lean();
    if (!report) return res.status(404).json({ error: 'Report not found' });
    res.json(report);
  } catch (err) {
    next(err);
  }
});

module.exports = router;

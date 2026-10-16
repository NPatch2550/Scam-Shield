/**
 * Scam Shield – Admin Portal Routes
 * Manage pending reports and verified scammer database.
 */
const express = require('express');
const router = express.Router();
const Report = require('../models/Report');
const ScamEntity = require('../models/ScamEntity');
const LegitCompany = require('../models/LegitCompany');

// Simple API key guard for demo
const ADMIN_KEY = process.env.ADMIN_KEY || 'scamshield-admin-2024';
function adminAuth(req, res, next) {
  const key = req.headers['x-admin-key'] || req.query.adminKey;
  if (key !== ADMIN_KEY) return res.status(401).json({ error: 'Unauthorized' });
  next();
}

/**
 * GET /api/v1/admin/reports?status=pending
 */
router.get('/reports', adminAuth, async (req, res, next) => {
  try {
    const { status = 'pending', limit = 20, skip = 0 } = req.query;
    const reports = await Report.find({ status })
      .sort({ createdAt: -1 })
      .limit(parseInt(limit))
      .skip(parseInt(skip))
      .lean();
    const total = await Report.countDocuments({ status });
    res.json({ total, reports });
  } catch (err) {
    next(err);
  }
});

/**
 * PUT /api/v1/admin/reports/:id/approve
 * Approve report and promote to verified_scammers (scam_entities collection).
 */
router.put('/reports/:id/approve', adminAuth, async (req, res, next) => {
  try {
    const { adminNotes, category, riskScore } = req.body;
    const report = await Report.findById(req.params.id);
    if (!report) return res.status(404).json({ error: 'Report not found' });

    report.status = 'approved';
    report.adminNotes = adminNotes || '';
    await report.save();

    // Promote to scam_entities
    const existing = await ScamEntity.findOne({ value: report.entityValue });
    if (!existing) {
      await ScamEntity.create({
        type: report.entityType,
        value: report.entityValue,
        displayName: report.entityValue,
        riskScore: riskScore || 85,
        category: category || report.scamCategory || 'phishing',
        description: report.description,
        reportCount: 1,
        verified: true,
        sources: ['ADMIN_VERIFIED', 'COMMUNITY_REPORT'],
      });
    } else {
      existing.reportCount += 1;
      existing.verified = true;
      await existing.save();
    }

    res.json({ message: 'Report approved and promoted to verified scam database' });
  } catch (err) {
    next(err);
  }
});

/**
 * PUT /api/v1/admin/reports/:id/reject
 */
router.put('/reports/:id/reject', adminAuth, async (req, res, next) => {
  try {
    const report = await Report.findByIdAndUpdate(
      req.params.id,
      { status: 'rejected', adminNotes: req.body.adminNotes || '' },
      { new: true }
    );
    if (!report) return res.status(404).json({ error: 'Report not found' });
    res.json({ message: 'Report rejected', report });
  } catch (err) {
    next(err);
  }
});

/**
 * GET /api/v1/admin/scam-entities
 */
router.get('/scam-entities', adminAuth, async (req, res, next) => {
  try {
    const { type, category, limit = 50 } = req.query;
    const filter = {};
    if (type) filter.type = type;
    if (category) filter.category = category;
    const entities = await ScamEntity.find(filter).sort({ reportCount: -1 }).limit(parseInt(limit)).lean();
    res.json({ total: entities.length, entities });
  } catch (err) {
    next(err);
  }
});

/**
 * GET /api/v1/admin/legit-companies
 */
router.get('/legit-companies', adminAuth, async (req, res, next) => {
  try {
    const { limit = 50 } = req.query;
    const companies = await LegitCompany.find({}).limit(parseInt(limit)).lean();
    res.json({ total: companies.length, companies });
  } catch (err) {
    next(err);
  }
});

module.exports = router;

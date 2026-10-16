/**
 * Scam Shield – Dashboard Stats Route
 */
const express = require('express');
const router = express.Router();
const ScamEntity = require('../models/ScamEntity');
const LegitCompany = require('../models/LegitCompany');
const Report = require('../models/Report');
const CallSession = require('../models/CallSession');

/**
 * GET /api/v1/dashboard
 * Returns aggregated stats for the admin dashboard.
 */
router.get('/', async (req, res, next) => {
  try {
    const [totalScamEntities, totalLegit, pendingReports, approvedReports, activeSessions,
           categoryBreakdown, typeBreakdown, recentReports] = await Promise.all([
      ScamEntity.countDocuments(),
      LegitCompany.countDocuments({ isActive: true }),
      Report.countDocuments({ status: 'pending' }),
      Report.countDocuments({ status: 'approved' }),
      CallSession.countDocuments({ status: 'active' }),
      ScamEntity.aggregate([
        { $group: { _id: '$category', count: { $sum: 1 } } },
        { $sort: { count: -1 } },
      ]),
      ScamEntity.aggregate([
        { $group: { _id: '$type', count: { $sum: 1 } } },
        { $sort: { count: -1 } },
      ]),
      Report.find({ status: 'pending' }).sort({ createdAt: -1 }).limit(5).lean(),
    ]);

    res.json({
      summary: {
        totalScamEntities,
        totalVerifiedLegitCompanies: totalLegit,
        pendingReports,
        approvedReports,
        activeCallSessions: activeSessions,
      },
      categoryBreakdown,
      typeBreakdown,
      recentPendingReports: recentReports,
      generatedAt: new Date(),
    });
  } catch (err) {
    next(err);
  }
});

module.exports = router;

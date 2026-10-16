/**
 * Scam Shield – Verify Routes
 * Core "Before You Invest" search portal endpoints.
 */
const express = require('express');
const router = express.Router();
const ScamEntity = require('../models/ScamEntity');
const LegitCompany = require('../models/LegitCompany');
const riskScorer = require('../services/riskScorer');
const nlpEngine = require('../services/nlpEngine');
const entityExtractor = require('../services/entityExtractor');
const domainAnalyzer = require('../services/domainAnalyzer');

/**
 * POST /api/v1/verify/search
 * Universal search: company name, URL, phone, bank account, or raw text.
 * Returns a Company Trust Scorecard.
 */
router.post('/search', async (req, res, next) => {
  try {
    const { query, rawText, type } = req.body;
    if (!query && !rawText) return res.status(400).json({ error: 'query or rawText is required' });

    const q = (query || '').trim();
    const text = rawText || q;

    // Extract entities from raw text if provided
    const entities = entityExtractor.extract(text);

    // Identify what kind of query we have
    const isDomain = /^[a-z0-9.-]+\.[a-z]{2,}$/i.test(q);
    const isPhone = /^0[6-9]\d{8}$/.test(q);
    const isBankAccount = /^\d{10,15}$/.test(q);
    const isUrl = /^https?:\/\//.test(q);

    // Search scam database
    const scamMatches = await ScamEntity.find({
      $or: [
        { value: new RegExp(escapeRegex(q), 'i') },
        { displayName: new RegExp(escapeRegex(q), 'i') },
      ],
    }).limit(5).lean();

    // Search legit company database
    const legitMatches = await LegitCompany.find({
      $or: [
        { name: new RegExp(escapeRegex(q), 'i') },
        { nameTh: new RegExp(escapeRegex(q), 'i') },
        { licenseNumber: new RegExp(escapeRegex(q), 'i') },
      ],
    }).limit(3).lean();

    // NLP analysis of raw text
    const nlpResult = rawText ? await nlpEngine.analyzeText(rawText) : null;

    // Domain analysis
    const domainInfo = (isDomain || isUrl)
      ? await domainAnalyzer.analyze(q)
      : null;

    // Risk scoring
    const scoring = await riskScorer.scoreEntity({
      companyName: (!isDomain && !isPhone && !isBankAccount && !isUrl) ? q : undefined,
      domain: isDomain || isUrl ? q : undefined,
      phoneNumber: isPhone ? q : undefined,
      bankAccount: isBankAccount ? q : undefined,
      rawText: text,
      nlpFlags: nlpResult?.flags?.map(f => f.flag) || [],
    });

    // Build Trust Scorecard
    const scorecard = {
      query: q,
      verdict: scoring.riskLevel,
      riskScore: scoring.riskScore,
      pillars: scoring.pillars,
      scamDatabaseMatches: scamMatches.map(m => ({
        type: m.type,
        category: m.category,
        description: m.description,
        reportCount: m.reportCount,
        sources: m.sources,
        verified: m.verified,
      })),
      legitimateCompanyMatches: legitMatches.map(l => ({
        name: l.name,
        nameTh: l.nameTh,
        licenseNumber: l.licenseNumber,
        licenseType: l.licenseType,
        regulator: l.regulator,
        officialDomain: l.officialDomain,
        officialPhone: l.officialPhone,
        isActive: l.isActive,
      })),
      domainAnalysis: domainInfo,
      nlpAnalysis: nlpResult,
      entitiesExtracted: entities,
      recommendation: buildRecommendation(scoring.riskLevel, scamMatches, legitMatches),
      timestamp: new Date(),
    };

    res.json(scorecard);
  } catch (err) {
    next(err);
  }
});

/**
 * POST /api/v1/verify/domain
 * Deep domain analysis (WHOIS age, typosquat, fake PR).
 */
router.post('/domain', async (req, res, next) => {
  try {
    const { domain } = req.body;
    if (!domain) return res.status(400).json({ error: 'domain is required' });
    const result = await domainAnalyzer.analyze(domain);
    res.json(result);
  } catch (err) {
    next(err);
  }
});

/**
 * POST /api/v1/verify/account
 * Bank account / PromptPay mule check.
 */
router.post('/account', async (req, res, next) => {
  try {
    const { accountNumber } = req.body;
    if (!accountNumber) return res.status(400).json({ error: 'accountNumber is required' });

    const scamMatch = await ScamEntity.findOne({
      type: 'bank_account',
      value: new RegExp(escapeRegex(accountNumber.toString()), 'i'),
    }).lean();

    res.json({
      accountNumber,
      isMuleAccount: !!scamMatch,
      riskScore: scamMatch ? 100 : 10,
      riskLevel: scamMatch ? 'critical' : 'safe',
      record: scamMatch || null,
    });
  } catch (err) {
    next(err);
  }
});

/**
 * POST /api/v1/verify/text
 * Analyze raw OCR or chat text.
 */
router.post('/text', async (req, res, next) => {
  try {
    const { text } = req.body;
    if (!text) return res.status(400).json({ error: 'text is required' });
    const result = await nlpEngine.analyzeText(text);
    res.json(result);
  } catch (err) {
    next(err);
  }
});

function buildRecommendation(riskLevel, scamMatches, legitMatches) {
  if (riskLevel === 'critical') {
    return {
      action: 'STOP_IMMEDIATELY',
      message: '🚨 CRITICAL ALERT: This entity is associated with known investment scams. Do NOT transfer any money or share personal information. Report to SEC Thailand (1207) immediately.',
      color: '#FF0000',
    };
  }
  if (riskLevel === 'suspicious') {
    return {
      action: 'PROCEED_WITH_EXTREME_CAUTION',
      message: '⚠️ HIGH RISK: Multiple red flags detected. Verify directly with the official regulator before proceeding.',
      color: '#FF6600',
    };
  }
  if (riskLevel === 'caution' && legitMatches.length === 0) {
    return {
      action: 'VERIFY_FIRST',
      message: '⚡ CAUTION: This entity is not in our verified registry. Call the regulator hotline to verify before investing.',
      color: '#FFD700',
    };
  }
  if (legitMatches.length > 0) {
    return {
      action: 'VERIFY_CONTACT_CHANNELS',
      message: '✅ Company found in regulatory registry. Always verify you are contacting them via their official channels listed above — not via LINE/Telegram.',
      color: '#00CC44',
    };
  }
  return {
    action: 'INVESTIGATE',
    message: '🔍 No records found. This may be a new or unregistered entity. Exercise caution.',
    color: '#888888',
  };
}

function escapeRegex(str) {
  return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

module.exports = router;

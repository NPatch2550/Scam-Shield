/**
 * Scam Shield – Investment Risk Scorer
 * Calculates a weighted 0–100 risk score from three pillars:
 *   40% Regulatory & DB Match
 *   30% SEO/AEO Footprint Authenticity
 *   30% Behavioral & Script Intent
 */
const ScamEntity = require('../models/ScamEntity');
const LegitCompany = require('../models/LegitCompany');
const domainAnalyzer = require('./domainAnalyzer');
const logger = require('../utils/logger');

async function scoreEntity({
  companyName,
  domain,
  phoneNumber,
  bankAccount,
  rawText,
  nlpFlags = [],
}) {
  let regulatoryScore = 0;   // 0-40
  let seoScore = 0;          // 0-30
  let behaviorScore = 0;     // 0-30
  const breakdown = { regulatory: [], seo: [], behavioral: [] };

  // ── PILLAR 1: Regulatory & Database Match (40 pts) ───────────────────────
  const searchValues = [companyName, domain, phoneNumber, bankAccount].filter(Boolean);

  for (const val of searchValues) {
    const scamRec = await ScamEntity.findOne({ value: new RegExp(escapeRegex(val), 'i') });
    if (scamRec) {
      regulatoryScore = Math.min(40, regulatoryScore + 40);
      breakdown.regulatory.push(`DB_BLACKLIST_MATCH: ${val} (${scamRec.category})`);
      break;
    }
  }

  if (companyName) {
    const legit = await LegitCompany.findOne({ name: new RegExp(escapeRegex(companyName), 'i') });
    if (!legit && regulatoryScore === 0) {
      regulatoryScore = Math.min(40, regulatoryScore + 20);
      breakdown.regulatory.push('NOT_IN_REGULATORY_WHITELIST');
    } else if (legit && !legit.isActive) {
      regulatoryScore = Math.min(40, regulatoryScore + 30);
      breakdown.regulatory.push('LICENSE_REVOKED');
    }
  }

  if (bankAccount) {
    // If they asked to send to a personal account name (heuristic: <25 chars, no 'Co.'/'Ltd.')
    if (bankAccount.length >= 10 && !/co\.|ltd|pcl|corp|บริษัท/i.test(rawText || '')) {
      regulatoryScore = Math.min(40, regulatoryScore + 20);
      breakdown.regulatory.push('POSSIBLE_MULE_ACCOUNT');
    }
  }

  // ── PILLAR 2: SEO/AEO Footprint (30 pts) ─────────────────────────────────
  if (domain) {
    const domainInfo = await domainAnalyzer.analyze(domain);
    if (domainInfo.ageMonths !== null && domainInfo.ageMonths < 6) {
      seoScore = Math.min(30, seoScore + 20);
      breakdown.seo.push(`YOUNG_DOMAIN: ${domainInfo.ageMonths} months old`);
    }
    if (domainInfo.isTyposquat) {
      seoScore = Math.min(30, seoScore + 25);
      breakdown.seo.push(`TYPOSQUAT_DETECTED: resembles ${domainInfo.resembles}`);
    }
    if (domainInfo.fakePRSaturation) {
      seoScore = Math.min(30, seoScore + 15);
      breakdown.seo.push('FAKE_PR_SATURATION_DETECTED');
    }
  }

  // ── PILLAR 3: Behavioral & Script Intent (30 pts) ─────────────────────────
  const highRiskFlags = [
    'GUARANTEED_RETURN', 'GUARANTEED_RETURN_TH', 'WITHDRAWAL_FEE_EXTORTION',
    'REG_IMPERSONATION', 'APK_SIDELOAD_REQUEST', 'INSIDER_TRADING_CLAIM',
  ];
  const medRiskFlags = [
    'UNREALISTIC_ROI', 'UNREALISTIC_ROI_TH', 'ZERO_RISK_CLAIM',
    'FOMO_PRESSURE', 'PRIVATE_GROUP_LURE', 'PIG_BUTCHERING_SCRIPT', 'TASK_SCAM',
  ];

  for (const flag of nlpFlags) {
    if (highRiskFlags.includes(flag)) {
      behaviorScore = Math.min(30, behaviorScore + 15);
      breakdown.behavioral.push(flag);
    } else if (medRiskFlags.includes(flag)) {
      behaviorScore = Math.min(30, behaviorScore + 8);
      breakdown.behavioral.push(flag);
    }
  }

  const totalScore = Math.min(100, regulatoryScore + seoScore + behaviorScore);
  const riskLevel = totalScore >= 70 ? 'critical' : totalScore >= 40 ? 'suspicious' : totalScore >= 15 ? 'caution' : 'safe';

  logger.info(`[Scorer] ${companyName || domain || bankAccount} → ${totalScore}% (${riskLevel})`);

  return {
    riskScore: totalScore,
    riskLevel,
    pillars: {
      regulatory: { score: regulatoryScore, max: 40, breakdown: breakdown.regulatory },
      seo: { score: seoScore, max: 30, breakdown: breakdown.seo },
      behavioral: { score: behaviorScore, max: 30, breakdown: breakdown.behavioral },
    },
  };
}

function escapeRegex(str) {
  return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

module.exports = { scoreEntity };

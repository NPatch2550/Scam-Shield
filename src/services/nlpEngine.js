/**
 * Scam Shield – NLP Threat Engine
 * Analyzes text for investment scam red flags using rule-based NLP.
 * No external API required – fully local.
 */
const ScamEntity = require('../models/ScamEntity');
const riskScorer = require('./riskScorer');
const entityExtractor = require('./entityExtractor');
const logger = require('../utils/logger');

// In-memory session store for ongoing calls (ephemeral, never persisted as-is)
const sessionStore = new Map();

// ── Scam script patterns (Thai + English) ────────────────────────────────────
const RED_FLAG_PATTERNS = [
  // Guaranteed returns
  { pattern: /guaranteed?\s*(return|profit|yield|income|ผลตอบแทน)/i, flag: 'GUARANTEED_RETURN', weight: 25 },
  { pattern: /รับรองกำไร|การันตีผลตอบแทน|รับประกันผลกำไร/i, flag: 'GUARANTEED_RETURN_TH', weight: 25 },
  // Unrealistic ROI
  { pattern: /(\d{2,3})%\s*(per\s*(day|week|month)|profit|return|กำไร)/i, flag: 'UNREALISTIC_ROI', weight: 20 },
  { pattern: /กำไร\s*\d+%|ผลตอบแทน\s*\d+%/i, flag: 'UNREALISTIC_ROI_TH', weight: 20 },
  // Zero risk promises
  { pattern: /no\s*risk|zero[-\s]risk|risk[-\s]free|ไม่มีความเสี่ยง|ปลอดภัย\s*100/i, flag: 'ZERO_RISK_CLAIM', weight: 20 },
  // FOMO urgency
  { pattern: /limited\s*(time|slots?|seats?)|act\s*now|don'?t\s*miss|last\s*chance|โอกาสสุดท้าย|ที่นั่งจำกัด/i, flag: 'FOMO_PRESSURE', weight: 15 },
  // Private group luring
  { pattern: /join\s*(our|the|private)\s*(telegram|line|vip|group)|เข้ากลุ่ม|กลุ่ม\s*vip|vip\s*room/i, flag: 'PRIVATE_GROUP_LURE', weight: 20 },
  // APK sideloading
  { pattern: /download\s*(our|the)?\s*app|install\s*(apk|the\s*app)|โหลดแอป|ดาวน์โหลด\s*แอป/i, flag: 'APK_SIDELOAD_REQUEST', weight: 20 },
  // Withdrawal fee extortion
  { pattern: /(pay|transfer|deposit)\s*(tax|fee|unlock|ค่า(ภาษี|ปลดล็อก|ค้ำประกัน))|ค่าภาษีก่อนถอน/i, flag: 'WITHDRAWAL_FEE_EXTORTION', weight: 35 },
  // Insider tips
  { pattern: /insider\s*(info|tip|trade|trading)|ข้อมูลภายใน|หุ้นเด็ด|หุ้นปั่น/i, flag: 'INSIDER_TRADING_CLAIM', weight: 25 },
  // Regulatory impersonation
  { pattern: /(ก\.ล\.ต|SEC|BOT|กนง|financial\s*regulatory|authorized\s*by).*?(license|ใบอนุญาต)/i, flag: 'REG_IMPERSONATION', weight: 30 },
  // Pig butchering romance angle
  { pattern: /ชอบคุณมาก|อยากช่วยคุณ|เพื่อนแนะนำ|i want to help you|my friend told me/i, flag: 'PIG_BUTCHERING_SCRIPT', weight: 15 },
  // Task/commission scam
  { pattern: /ทำงานออนไลน์|รับค่าคอมมิชชั่น|กด(ไลค์|ลิงก์)|complete\s*task|earn per task|งาน\s*online/i, flag: 'TASK_SCAM', weight: 20 },
];

// PII patterns to protect user from sharing
const PII_PATTERNS = [
  { pattern: /\b\d{13}\b/, flag: 'NATIONAL_ID_SHARED', severity: 'critical' },
  { pattern: /\b\d{6}\b/, flag: 'OTP_SHARED', severity: 'critical' },
  { pattern: /seed\s*phrase|recovery\s*phrase|mnemonic|คำช่วยจำ/i, flag: 'SEED_PHRASE_SHARED', severity: 'critical' },
  { pattern: /password|รหัสผ่าน/i, flag: 'PASSWORD_SHARED', severity: 'high' },
];

/**
 * Analyze a transcript chunk from a 3-way call.
 * @param {string} sessionId
 * @param {string} speaker - 'user' | 'caller'
 * @param {string} text
 */
async function analyzeChunk(sessionId, speaker, text) {
  let session = sessionStore.get(sessionId) || { flags: new Set(), score: 0, entities: [] };

  const newFlags = [];
  let chunkScore = 0;

  // 1. Scan for red flag patterns
  for (const { pattern, flag, weight } of RED_FLAG_PATTERNS) {
    if (pattern.test(text)) {
      if (!session.flags.has(flag)) {
        session.flags.add(flag);
        chunkScore += weight;
        newFlags.push(flag);
      }
    }
  }

  // 2. PII guard (user speaker only)
  const piiAlerts = [];
  if (speaker === 'user') {
    for (const { pattern, flag, severity } of PII_PATTERNS) {
      if (pattern.test(text)) {
        piiAlerts.push({ flag, severity });
        chunkScore += severity === 'critical' ? 30 : 15;
      }
    }
  }

  // 3. Entity extraction
  const extracted = entityExtractor.extract(text);
  const dbMatches = await checkEntitiesInDB(extracted);
  dbMatches.forEach(m => { chunkScore += 40; session.flags.add('DB_SCAM_MATCH'); });
  session.entities.push(...extracted);

  // Update cumulative score (capped at 100)
  session.score = Math.min(100, session.score + chunkScore);
  sessionStore.set(sessionId, session);

  const riskLevel = session.score >= 70 ? 'critical' : session.score >= 35 ? 'suspicious' : 'safe';
  logger.info(`[NLP] session=${sessionId} score=${session.score} flags=${[...session.flags].join(',')}`);

  return {
    sessionId,
    riskScore: session.score,
    riskLevel,
    newFlags,
    piiAlerts,
    entitiesDetected: extracted,
    dbMatches,
    timestamp: new Date(),
  };
}

/**
 * Analyze a block of text (chat / OCR).
 */
async function analyzeText(text) {
  const flags = [];
  let score = 0;

  for (const { pattern, flag, weight } of RED_FLAG_PATTERNS) {
    if (pattern.test(text)) {
      flags.push({ flag, weight });
      score += weight;
    }
  }

  const piiAlerts = [];
  for (const { pattern, flag, severity } of PII_PATTERNS) {
    if (pattern.test(text)) {
      piiAlerts.push({ flag, severity });
      score += severity === 'critical' ? 30 : 15;
    }
  }

  const extracted = entityExtractor.extract(text);
  const dbMatches = await checkEntitiesInDB(extracted);
  dbMatches.forEach(() => { score += 40; });

  score = Math.min(100, score);
  const riskLevel = score >= 70 ? 'critical' : score >= 35 ? 'suspicious' : 'safe';

  return { riskScore: score, riskLevel, flags, piiAlerts, entitiesDetected: extracted, dbMatches };
}

async function checkEntitiesInDB(entities) {
  const matches = [];
  for (const { type, value } of entities) {
    const found = await ScamEntity.findOne({ value: new RegExp(escapeRegex(value), 'i') });
    if (found) matches.push({ type, value, record: found });
  }
  return matches;
}

function escapeRegex(str) {
  return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function clearSession(sessionId) {
  sessionStore.delete(sessionId);
}

module.exports = { analyzeChunk, analyzeText, clearSession };

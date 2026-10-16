/**
 * Scam Shield – Domain Analyzer
 * Checks domain age, typosquatting, and fake PR saturation.
 * Uses a local heuristic approach (no external API required for demo).
 */
const LegitCompany = require('../models/LegitCompany');
const logger = require('../utils/logger');

// Levenshtein distance for typosquat detection
function levenshtein(a, b) {
  const dp = Array.from({ length: a.length + 1 }, (_, i) =>
    Array.from({ length: b.length + 1 }, (_, j) => (i === 0 ? j : j === 0 ? i : 0))
  );
  for (let i = 1; i <= a.length; i++)
    for (let j = 1; j <= b.length; j++)
      dp[i][j] = a[i-1] === b[j-1]
        ? dp[i-1][j-1]
        : 1 + Math.min(dp[i-1][j], dp[i][j-1], dp[i-1][j-1]);
  return dp[a.length][b.length];
}

// Extract domain name without TLD for comparison
function domainCore(domain) {
  return domain.replace(/^www\./, '').split('.')[0].toLowerCase();
}

async function analyze(domainInput) {
  const domain = domainInput.toLowerCase().replace(/^https?:\/\//, '').replace(/\/.*$/, '');
  const core = domainCore(domain);

  // Heuristic: domains in our fake-domain blacklist seed data
  const scamDomainRecord = await require('../models/ScamEntity').findOne({
    type: 'domain',
    value: new RegExp(escapeRegex(domain), 'i'),
  });

  // Typosquat check against legit companies
  const legitCompanies = await LegitCompany.find({ officialDomain: { $exists: true, $ne: '' } }).lean();
  let isTyposquat = false;
  let resembles = null;

  for (const lc of legitCompanies) {
    if (!lc.officialDomain) continue;
    const legitCore = domainCore(lc.officialDomain);
    const dist = levenshtein(core, legitCore);
    // If very similar but not identical
    if (dist > 0 && dist <= 2 && core !== legitCore) {
      isTyposquat = true;
      resembles = lc.officialDomain;
      break;
    }
  }

  // Domain age heuristic: seeded records have meta.domainAgeMonths
  const ageMonths = scamDomainRecord?.meta?.domainAgeMonths ?? null;

  // Fake PR saturation: flag if scam record exists and has fakePR flag in meta
  const fakePRSaturation = scamDomainRecord?.meta?.fakePR ?? false;

  return {
    domain,
    ageMonths,
    isTyposquat,
    resembles,
    fakePRSaturation,
    inBlacklist: !!scamDomainRecord,
  };
}

function escapeRegex(str) {
  return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

module.exports = { analyze };

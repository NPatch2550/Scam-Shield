/**
 * Scam Shield – Entity Extractor
 * Extracts company names, URLs, bank accounts, phone numbers,
 * crypto wallet addresses, and PromptPay IDs from raw text.
 */

const PATTERNS = {
  url: /https?:\/\/[^\s,<>"'()]+/gi,
  domain: /\b([a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?\.)+[a-z]{2,}\b/gi,
  phone_th: /\b0[6-9]\d{8}\b/g,
  bank_account: /\b\d{10,15}\b/g,
  promptpay: /\b0[6-9]\d{8}\b|\b\d{13}\b/g,
  crypto_btc: /\b[13][a-km-zA-HJ-NP-Z1-9]{25,34}\b/g,
  crypto_eth: /\b0x[a-fA-F0-9]{40}\b/g,
  stock_ticker: /\b[A-Z]{2,5}\b/g,
  apk_url: /https?:\/\/[^\s]+\.apk/gi,
  line_id: /@[a-zA-Z0-9._-]+/g,
  telegram: /t\.me\/[a-zA-Z0-9_]+/gi,
};

// Keyword-based company name heuristic
const COMPANY_NAME_REGEX = /([A-Z][a-zA-Z]{1,20}\s)*(Capital|Securities|Investment|Trading|Finance|Broker|Asset|Fund|Crypto|Exchange|Bank|Holdings|Group|Co\.?,?\s*Ltd\.?|PCL|Inc\.?|Corp\.?)/g;
const COMPANY_NAME_TH = /บริษัท[^จก]{1,30}จำกัด|[^\s]{2,15}\s*(หลักทรัพย์|ทุน|การลงทุน|บล\.|บลจ\.)/g;

function extract(text) {
  const results = [];

  const add = (type, value) => results.push({ type, value: value.trim() });

  const urls = text.match(PATTERNS.url) || [];
  urls.forEach(v => add('url', v));
  const apks = text.match(PATTERNS.apk_url) || [];
  apks.forEach(v => add('apk_url', v));

  const phones = [...new Set(text.match(PATTERNS.phone_th) || [])];
  phones.forEach(v => add('phone', v));

  // Bank accounts: 10-15 digits NOT already captured as phone
  const accounts = (text.match(PATTERNS.bank_account) || []).filter(v => v.length >= 10 && !phones.includes(v));
  [...new Set(accounts)].forEach(v => add('bank_account', v));

  const btc = text.match(PATTERNS.crypto_btc) || [];
  btc.forEach(v => add('crypto_wallet', v));
  const eth = text.match(PATTERNS.crypto_eth) || [];
  eth.forEach(v => add('crypto_wallet', v));

  const telegram = text.match(PATTERNS.telegram) || [];
  telegram.forEach(v => add('telegram_group', v));
  const lineIds = text.match(PATTERNS.line_id) || [];
  lineIds.forEach(v => add('line_group', v));

  const companies = text.match(COMPANY_NAME_REGEX) || [];
  companies.forEach(v => add('company', v));
  const companiesTh = text.match(COMPANY_NAME_TH) || [];
  companiesTh.forEach(v => add('company', v));

  return results;
}

module.exports = { extract };

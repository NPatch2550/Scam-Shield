/**
 * Report – User-submitted scam reports (crowdsourced).
 * Collection: reports
 */
const mongoose = require('mongoose');

const ReportSchema = new mongoose.Schema({
  submittedBy: { type: String, default: 'anonymous' }, // device ID or anon
  entityType: {
    type: String,
    enum: ['company', 'domain', 'phone', 'bank_account', 'crypto_wallet', 'telegram_group', 'line_group', 'apk_url', 'screenshot'],
  },
  entityValue: { type: String, required: true },
  description: String,
  scamCategory: {
    type: String,
    enum: ['pig_butchering', 'fake_broker', 'fake_ipo', 'task_scam', 'crypto_scam', 'mule_account', 'phishing', 'clone_site', 'unknown'],
    default: 'unknown',
  },
  evidenceText: String,  // OCR or chat text extracted on device
  status: {
    type: String,
    enum: ['pending', 'approved', 'rejected'],
    default: 'pending',
    index: true,
  },
  adminNotes: String,
  enrichment: {
    domainAge: Number,
    whoisRegistrar: String,
    secMatch: Boolean,
    dbdMatch: Boolean,
  },
}, { timestamps: true, collection: 'reports' });

module.exports = mongoose.model('Report', ReportSchema);

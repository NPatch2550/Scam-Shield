/**
 * ScamEntity – Known scam companies, fake brokers, mule accounts,
 * clone domains, and phishing numbers.
 * Collection: scam_entities
 */
const mongoose = require('mongoose');

const ScamEntitySchema = new mongoose.Schema({
  type: {
    type: String,
    enum: ['company', 'domain', 'phone', 'bank_account', 'crypto_wallet', 'telegram_group', 'line_group', 'apk_url'],
    required: true,
  },
  value: { type: String, required: true, index: true }, // the entity itself
  displayName: String,
  riskScore: { type: Number, default: 100, min: 0, max: 100 },
  category: {
    type: String,
    enum: ['pig_butchering', 'fake_broker', 'fake_ipo', 'task_scam', 'crypto_scam', 'mule_account', 'phishing', 'clone_site'],
  },
  description: String,
  reportCount: { type: Number, default: 1 },
  verified: { type: Boolean, default: false },
  sources: [String], // e.g. 'SEC_ALERT_LIST', 'COMMUNITY_REPORT', 'ADMIN_VERIFIED'
  relatedEntities: [{ type: mongoose.Schema.Types.ObjectId, ref: 'ScamEntity' }],
  meta: mongoose.Schema.Types.Mixed,
}, { timestamps: true, collection: 'scam_entities' });

ScamEntitySchema.index({ value: 'text', displayName: 'text' });

module.exports = mongoose.model('ScamEntity', ScamEntitySchema);

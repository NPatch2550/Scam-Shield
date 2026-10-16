/**
 * LegitCompany – Verified licensed financial entities from official registries.
 * Collection: legit_companies
 */
const mongoose = require('mongoose');

const LegitCompanySchema = new mongoose.Schema({
  name: { type: String, required: true, index: true },
  nameTh: String,
  licenseNumber: { type: String, index: true },
  licenseType: {
    type: String,
    enum: ['securities_broker', 'investment_advisor', 'fund_manager', 'bank', 'insurance', 'crypto_exchange', 'other'],
  },
  regulator: {
    type: String,
    enum: ['SEC_TH', 'BOT', 'OIC', 'DBD', 'SEC_US', 'MAS', 'other'],
    default: 'SEC_TH',
  },
  officialDomain: String,
  officialPhone: [String],
  officialEmail: String,
  registrationDate: Date,
  isActive: { type: Boolean, default: true },
  trustScore: { type: Number, default: 90, min: 0, max: 100 },
  verifiedCustodianAccounts: [{
    bankName: String,
    accountName: String,
    accountNumber: String, // partial, last 4 digits for PII
  }],
  meta: mongoose.Schema.Types.Mixed,
}, { timestamps: true, collection: 'legit_companies' });

LegitCompanySchema.index({ name: 'text', nameTh: 'text' });

module.exports = mongoose.model('LegitCompany', LegitCompanySchema);

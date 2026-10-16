/**
 * CallSession – Ephemeral 3-way call analysis session.
 * Stored in MongoDB only for the session duration; wiped on disconnect.
 * Collection: call_sessions
 */
const mongoose = require('mongoose');

const CallSessionSchema = new mongoose.Schema({
  sessionId: { type: String, required: true, unique: true, index: true },
  deviceId: String,
  callerNumber: String,
  status: {
    type: String,
    enum: ['active', 'completed', 'wiped'],
    default: 'active',
  },
  riskScore: { type: Number, default: 0 },
  riskLevel: { type: String, enum: ['safe', 'suspicious', 'critical'], default: 'safe' },
  flags: [String],
  entitiesDetected: [{
    type: { type: String },
    value: String,
    matchedScamRecord: Boolean,
  }],
  // Raw transcript is NEVER stored – ephemeral only
  transcriptChunkCount: { type: Number, default: 0 },
  startedAt: { type: Date, default: Date.now },
  endedAt: Date,
}, { timestamps: true, collection: 'call_sessions' });

// Auto-expire sessions after 24 hours
CallSessionSchema.index({ startedAt: 1 }, { expireAfterSeconds: 86400 });

module.exports = mongoose.model('CallSession', CallSessionSchema);

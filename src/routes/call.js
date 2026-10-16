/**
 * Scam Shield – Call Defense Routes
 * Manages 3-way call analysis sessions.
 */
const express = require('express');
const router = express.Router();
const { v4: uuidv4 } = require('uuid');
const CallSession = require('../models/CallSession');
const ScamEntity = require('../models/ScamEntity');
const nlpEngine = require('../services/nlpEngine');
const { getIO } = require('../socket');
const logger = require('../utils/logger');

/**
 * POST /api/v1/call/session/start
 * Start a new 3-way call analysis session.
 */
router.post('/session/start', async (req, res, next) => {
  try {
    const { deviceId, callerNumber } = req.body;
    const sessionId = uuidv4();

    // Pre-check caller number
    let callerRisk = { riskScore: 0, riskLevel: 'safe', inBlacklist: false };
    if (callerNumber) {
      const scamNum = await ScamEntity.findOne({ type: 'phone', value: callerNumber }).lean();
      if (scamNum) {
        callerRisk = { riskScore: 100, riskLevel: 'critical', inBlacklist: true, record: scamNum };
      }
    }

    const session = await CallSession.create({
      sessionId,
      deviceId,
      callerNumber,
      riskScore: callerRisk.riskScore,
      riskLevel: callerRisk.riskLevel,
    });

    logger.info(`[Call] New session: ${sessionId} from ${callerNumber}`);
    res.json({ sessionId, callerRisk, message: 'Session started. Stream audio chunks to /api/v1/call/transcript' });
  } catch (err) {
    next(err);
  }
});

/**
 * POST /api/v1/call/transcript
 * Receive a transcript chunk from the Cloud PBX STT engine.
 * Runs NLP and pushes real-time result via Socket.io.
 */
router.post('/transcript', async (req, res, next) => {
  try {
    const { sessionId, speaker, text, deviceId } = req.body;
    if (!sessionId || !text) return res.status(400).json({ error: 'sessionId and text required' });

    const result = await nlpEngine.analyzeChunk(sessionId, speaker || 'caller', text);

    // Update session in DB
    await CallSession.findOneAndUpdate(
      { sessionId },
      {
        riskScore: result.riskScore,
        riskLevel: result.riskLevel,
        $addToSet: { flags: { $each: result.newFlags } },
        $inc: { transcriptChunkCount: 1 },
        $push: { entitiesDetected: { $each: result.entitiesDetected.slice(0, 3) } },
      }
    );

    // Push to Android client via Socket.io
    if (deviceId) {
      try {
        getIO().to(`device:${deviceId}`).emit('risk:update', result);
      } catch (_) { /* Socket.io may not be up in test mode */ }
    }

    res.json(result);
  } catch (err) {
    next(err);
  }
});

/**
 * POST /api/v1/call/session/end
 * End session and wipe ephemeral data.
 */
router.post('/session/end', async (req, res, next) => {
  try {
    const { sessionId } = req.body;
    if (!sessionId) return res.status(400).json({ error: 'sessionId required' });

    nlpEngine.clearSession(sessionId);

    const session = await CallSession.findOneAndUpdate(
      { sessionId },
      { status: 'wiped', endedAt: new Date() },
      { new: true }
    ).lean();

    logger.info(`[Call] Session wiped: ${sessionId}`);
    res.json({ sessionId, status: 'wiped', finalRiskScore: session?.riskScore, finalFlags: session?.flags });
  } catch (err) {
    next(err);
  }
});

/**
 * GET /api/v1/call/screen/:number
 * CallScreeningService pre-call check.
 */
router.get('/screen/:number', async (req, res, next) => {
  try {
    const { number } = req.params;
    const scam = await ScamEntity.findOne({ type: 'phone', value: number }).lean();
    if (scam) {
      return res.json({
        action: 'REJECT',
        riskScore: 100,
        reason: scam.description || 'Known scam number',
        category: scam.category,
      });
    }
    res.json({ action: 'ALLOW', riskScore: 0 });
  } catch (err) {
    next(err);
  }
});

module.exports = router;

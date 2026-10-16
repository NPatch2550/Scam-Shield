/**
 * Scam Shield – Socket.io Real-Time Layer
 * Pushes live risk score updates and call analysis events to Android clients.
 */
const { Server } = require('socket.io');
const logger = require('./utils/logger');

let io;

function initSocket(server) {
  io = new Server(server, {
    cors: { origin: '*' },
    pingTimeout: 30000,
  });

  io.on('connection', (socket) => {
    logger.info(`📱  Client connected: ${socket.id}`);

    // Android client sends device ID to join personal room
    socket.on('register', ({ deviceId }) => {
      socket.join(`device:${deviceId}`);
      logger.info(`   Device ${deviceId} registered`);
      socket.emit('registered', { status: 'ok', deviceId });
    });

    // Call transcript chunk arrives from Cloud PBX STT engine
    socket.on('call:chunk', async (data) => {
      const { sessionId, speaker, text, deviceId } = data;
      const { analyzeChunk } = require('./services/nlpEngine');
      const result = await analyzeChunk(sessionId, speaker, text);
      io.to(`device:${deviceId}`).emit('risk:update', result);
    });

    socket.on('disconnect', () => {
      logger.info(`📵  Client disconnected: ${socket.id}`);
    });
  });

  return io;
}

function getIO() {
  if (!io) throw new Error('Socket.io not initialized');
  return io;
}

module.exports = { initSocket, getIO };

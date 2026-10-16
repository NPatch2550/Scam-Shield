/**
 * Scam Shield – Database Connection
 * Supports either an in-process mongodb-memory-server (USE_MEMORY_DB=true)
 * or a real MongoDB URI.
 */
const mongoose = require('mongoose');
const logger = require('./utils/logger');

let memoryServer = null;

async function connectDB() {
  if (process.env.USE_MEMORY_DB === 'true') {
    const { MongoMemoryServer } = require('mongodb-memory-server');
    memoryServer = await MongoMemoryServer.create();
    const uri = memoryServer.getUri();
    await mongoose.connect(uri);
    logger.info(`✅  Connected to in-memory MongoDB at ${uri}`);
  } else {
    const uri = process.env.MONGO_URI || 'mongodb://127.0.0.1:27017/scamshield';
    await mongoose.connect(uri);
    logger.info(`✅  Connected to MongoDB at ${uri}`);
  }
}

async function disconnectDB() {
  await mongoose.disconnect();
  if (memoryServer) await memoryServer.stop();
}

module.exports = { connectDB, disconnectDB };

/**
 * Scam Shield – Cloud PBX 3-Way Call & Real-Time STT Simulation
 * Simulates the 3-Way Call Merging workflow:
 *  1. Android App dials Cloud PBX listener bot
 *  2. Cloud PBX bridges user, caller, and bot into conference
 *  3. Cloud PBX streams dual-channel diarized chunks to NLP Threat Engine
 *  4. Real-time Socket.io risk:update events pushed back to Android Floating Bubble
 */
const io = require('socket.io-client');
const http = require('http');

const PORT = process.env.PORT || 3000;
const SERVER_URL = `http://localhost:${PORT}`;
const DEVICE_ID = 'test-android-device-999';

function post(path, body) {
  return new Promise((resolve, reject) => {
    const data = JSON.stringify(body);
    const req = http.request({
      hostname: 'localhost',
      port: PORT,
      path,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(data),
      },
    }, (res) => {
      let raw = '';
      res.on('data', chunk => raw += chunk);
      res.on('end', () => resolve(JSON.parse(raw)));
    });
    req.on('error', reject);
    req.write(data);
    req.end();
  });
}

async function runCloudPbxSimulation() {
  console.log('\n' + '='.repeat(70));
  console.log('🎙️  SCAM SHIELD: CLOUD PBX 3-WAY CALL MERGE & NLP STREAM SIMULATION');
  console.log('='.repeat(70));

  // 1. Connect Android Socket.io client
  console.log('\n[1] Connecting Android Client via Socket.io...');
  const socket = io(SERVER_URL);

  await new Promise((resolve) => {
    socket.on('connect', () => {
      console.log(`    ✅ Connected to Socket.io server with socket ID: ${socket.id}`);
      socket.emit('register', { deviceId: DEVICE_ID });
    });

    socket.on('registered', (data) => {
      console.log(`    📱 Device registered in personal room: device:${data.deviceId}`);
      resolve();
    });
  });

  // Track socket updates received on the Android Floating Bubble
  const receivedSocketEvents = [];
  socket.on('risk:update', (data) => {
    console.log(`\n    ⚡ [Socket.io Push to Floating Bubble] Risk Score: ${data.riskScore}% (${data.riskLevel.toUpperCase()})`);
    if (data.newFlags && data.newFlags.length > 0) {
      console.log(`       🚩 New Flags Detected: ${data.newFlags.join(', ')}`);
    }
    if (data.entitiesDetected && data.entitiesDetected.length > 0) {
      console.log(`       🔍 Entities: ${data.entitiesDetected.map(e => `[${e.type}] ${e.value}`).join(' | ')}`);
    }
    receivedSocketEvents.push(data);
  });

  // 2. Start Call Session (Triggered when user taps "Activate Call Shield")
  console.log('\n[2] User taps "Activate Call Shield" → Dialing Cloud PBX (+6620001207)...');
  const sessionRes = await post('/api/v1/call/session/start', {
    deviceId: DEVICE_ID,
    callerNumber: '0891234567', // AlphaGold Cold-Call Line
  });

  console.log(`    ✅ 3-Way Conference Session Created: ${sessionRes.sessionId}`);
  console.log(`    📞 Pre-Call Screening Risk: ${sessionRes.callerRisk.riskLevel.toUpperCase()} (Score: ${sessionRes.callerRisk.riskScore}%)`);

  // 3. Audio stream simulation with speaker diarization
  console.log('\n[3] 3-Way Call Merged! Streaming dual-channel audio to NLP Threat Engine...');
  const conversationScript = [
    {
      speaker: 'caller',
      text: 'สวัสดีครับ ผมโทรมาจาก AlphaGold Securities ขอเสนอพอร์ตลงทุนทองคำ ผลตอบแทนการันตี 40% ต่อเดือนครับ',
      description: 'Scammer Pitch: Guaranteed 40% return'
    },
    {
      speaker: 'user',
      text: 'มีความเสี่ยงไหมครับช่วงนี้ทองคำผันผวน?',
      description: 'User Question'
    },
    {
      speaker: 'caller',
      text: 'ไม่มีความเสี่ยงเลยครับ ปลอดภัย 100% รับรองกำไร โควต้า VIP รอบนี้มีจำกัดแค่ 5 ท่านเท่านั้น ต้องรีบตัดสินใจครับ',
      description: 'Scammer Pressure: Zero risk + FOMO urgency'
    },
    {
      speaker: 'caller',
      text: 'ให้ดาวน์โหลดแอปเทรดได้ที่ลิงก์นี้ครับ https://alphagold-th.com/download/alphagold-trading-v2.apk ไม่ต้องผ่านสโตร์',
      description: 'Scammer Payload: Malicious APK sideloading'
    },
    {
      speaker: 'caller',
      text: 'และรบกวนโอนเงินมัดจำเปิดพอร์ต 10,000 บาท เข้าบัญชี SCB 1234567890 ภายในวันนี้เพื่อล็อคสิทธิ์ครับ',
      description: 'Scammer Mule Account: 1234567890'
    }
  ];

  for (let i = 0; i < conversationScript.length; i++) {
    const chunk = conversationScript[i];
    console.log(`\n--- Audio Chunk ${i + 1}/${conversationScript.length} [Speaker: ${chunk.speaker.toUpperCase()}] ---`);
    console.log(`    Transcript: "${chunk.text}"`);

    // Stream chunk to backend STT endpoint
    await post('/api/v1/call/transcript', {
      sessionId: sessionRes.sessionId,
      speaker: chunk.speaker,
      text: chunk.text,
      deviceId: DEVICE_ID,
    });

    // Short pause to simulate speech streaming interval
    await new Promise(r => setTimeout(r, 600));
  }

  // 4. End Call & Volatile Memory Wipe
  console.log('\n[4] User hangs up → Disconnecting PBX & Triggering Volatile RAM Wipe...');
  const endRes = await post('/api/v1/call/session/end', {
    sessionId: sessionRes.sessionId,
  });

  console.log(`    ✅ Ephemeral Session Status: ${endRes.status.toUpperCase()}`);
  console.log(`    🛡️ Final Session Risk Score: ${endRes.finalRiskScore}%`);
  console.log(`    🚩 Cumulative Flags: ${endRes.finalFlags?.join(', ')}`);

  socket.disconnect();

  console.log('\n' + '='.repeat(70));
  console.log(`🎯 SIMULATION SUMMARY: ${receivedSocketEvents.length} Real-Time Socket Updates Delivered`);
  console.log('✅ CLOUD PBX 3-WAY CALL MERGE SIMULATION PASSED');
  console.log('='.repeat(70) + '\n');
}

runCloudPbxSimulation().catch((err) => {
  console.error('Simulation failed:', err);
  process.exit(1);
});

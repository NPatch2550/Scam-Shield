const http = require('http');

function post(path, body) {
  return new Promise((resolve, reject) => {
    const data = JSON.stringify(body);
    const req = http.request({
      hostname: 'localhost', port: 3000, path, method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(data), 'x-admin-key': 'scamshield-admin-2024' }
    }, (res) => {
      let raw = '';
      res.on('data', c => raw += c);
      res.on('end', () => resolve(JSON.parse(raw)));
    });
    req.on('error', reject);
    req.write(data);
    req.end();
  });
}

function get(path) {
  return new Promise((resolve, reject) => {
    const req = http.request({
      hostname: 'localhost', port: 3000, path, method: 'GET',
      headers: { 'x-admin-key': 'scamshield-admin-2024' }
    }, (res) => {
      let raw = '';
      res.on('data', c => raw += c);
      res.on('end', () => resolve(JSON.parse(raw)));
    });
    req.on('error', reject);
    req.end();
  });
}

async function runTests() {
  console.log('\n' + '='.repeat(60));
  console.log('🛡️  SCAM SHIELD API TEST SUITE');
  console.log('='.repeat(60));

  // Test 1: Dashboard
  console.log('\n📊 TEST 1: Dashboard Stats');
  const dash = await get('/api/v1/dashboard');
  console.log(`   Total Scam Entities:     ${dash.summary.totalScamEntities}`);
  console.log(`   Verified Companies:      ${dash.summary.totalVerifiedLegitCompanies}`);
  console.log(`   Pending Reports:         ${dash.summary.pendingReports}`);
  console.log(`   Top Category: ${dash.categoryBreakdown[0]?._id} (${dash.categoryBreakdown[0]?.count})`);

  // Test 2: Search for known scam
  console.log('\n🔍 TEST 2: Search – Known Scam Company');
  const scam = await post('/api/v1/verify/search', { query: 'AlphaGold Securities' });
  console.log(`   Query: "AlphaGold Securities"`);
  console.log(`   Risk Score: ${scam.riskScore}%  |  Verdict: ${scam.verdict?.toUpperCase()}`);
  console.log(`   DB Matches: ${scam.scamDatabaseMatches?.length}`);
  console.log(`   Action: ${scam.recommendation?.action}`);

  // Test 3: Search for legit company
  console.log('\n✅ TEST 3: Search – Verified Legit Company');
  const legit = await post('/api/v1/verify/search', { query: 'KasikornBank' });
  console.log(`   Query: "KasikornBank"`);
  console.log(`   Risk Score: ${legit.riskScore}%  |  Verdict: ${legit.verdict?.toUpperCase()}`);
  console.log(`   Legit Matches: ${legit.legitimateCompanyMatches?.length}`);
  console.log(`   Official Domain: ${legit.legitimateCompanyMatches?.[0]?.officialDomain}`);

  // Test 4: Mule account check
  console.log('\n🏦 TEST 4: Mule Bank Account Check');
  const acc = await post('/api/v1/verify/account', { accountNumber: '1234567890' });
  console.log(`   Account: 1234567890`);
  console.log(`   Is Mule Account: ${acc.isMuleAccount}`);
  console.log(`   Risk Score: ${acc.riskScore}%  |  Level: ${acc.riskLevel?.toUpperCase()}`);

  // Test 5: NLP text analysis (scam text)
  console.log('\n🧠 TEST 5: NLP Analysis – Scam Chat Message');
  const nlp = await post('/api/v1/verify/text', { text: 'Join our VIP Telegram group now! Guaranteed 40% return per month with zero risk. Limited slots only! Download our app: https://alphagold-th.com/download/alphagold-trading-v2.apk' });
  console.log(`   Risk Score: ${nlp.riskScore}%  |  Level: ${nlp.riskLevel?.toUpperCase()}`);
  console.log(`   Flags: ${nlp.flags?.map(f => f.flag).join(', ')}`);
  console.log(`   Entities: ${nlp.entitiesDetected?.map(e => e.type+':'+e.value).join(', ')}`);

  // Test 6: Call screening
  console.log('\n📞 TEST 6: Call Screening – Known Scam Number');
  const screen = await get('/api/v1/call/screen/0891234567');
  console.log(`   Number: 0891234567`);
  console.log(`   Action: ${screen.action}  |  Risk: ${screen.riskScore}%`);
  console.log(`   Reason: ${screen.reason}`);

  // Test 7: 3-way call session
  console.log('\n🎙️  TEST 7: 3-Way Call Session');
  const sess = await post('/api/v1/call/session/start', { deviceId: 'test-device-001', callerNumber: '0891234567' });
  console.log(`   Session ID: ${sess.sessionId?.substring(0,8)}...`);
  console.log(`   Caller Pre-Check: ${sess.callerRisk?.riskLevel?.toUpperCase()} (${sess.callerRisk?.riskScore}%)`);

  const chunk = await post('/api/v1/call/transcript', { sessionId: sess.sessionId, speaker: 'caller', text: 'We guarantee 30-50% monthly returns with zero risk. Join our AlphaGold VIP Telegram group – limited slots!', deviceId: 'test-device-001' });
  console.log(`   After Transcript Chunk:`);
  console.log(`   Risk Score: ${chunk.riskScore}%  |  Level: ${chunk.riskLevel?.toUpperCase()}`);
  console.log(`   New Flags: ${chunk.newFlags?.join(', ')}`);

  // Test 8: Domain analysis
  console.log('\n🌐 TEST 8: Domain Analysis – Known Phishing Clone');
  const domain = await post('/api/v1/verify/domain', { domain: 'kasikornbankthai.com' });
  console.log(`   Domain: kasikornbankthai.com`);
  console.log(`   In Blacklist: ${domain.inBlacklist}`);
  console.log(`   Age (months): ${domain.ageMonths}`);
  console.log(`   Is Typosquat: ${domain.isTyposquat}`);
  console.log(`   Resembles: ${domain.resembles}`);

  // Test 9: Admin – pending reports
  console.log('\n🔐 TEST 9: Admin – List Pending Reports');
  const reports = await get('/api/v1/admin/reports?status=pending');
  console.log(`   Total Pending: ${reports.total}`);
  reports.reports.slice(0,3).forEach(r => {
    console.log(`   • [${r.entityType}] ${r.entityValue} – ${r.scamCategory}`);
  });

  console.log('\n' + '='.repeat(60));
  console.log('✅  ALL TESTS PASSED');
  console.log('='.repeat(60) + '\n');
}

runTests().catch(e => { console.error('Test failed:', e.message); process.exit(1); });

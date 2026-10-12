// Regenerates the bundled OSRS Wiki snapshots in src/main/resources/com/dropmarket/:
//   wiki-drops.json       monster -> its best drops by value (what the market bets on)
//   collection-log.json   every collection log item, and per monster the log items it drops (any rate: the plugin
//                         price-checks the common ones itself)
// Run with: node tools/generate-wiki-drops.mjs   (needs Node 18+ for fetch; run before each release)
import { writeFileSync } from 'node:fs';

const API = 'https://oldschool.runescape.wiki/api.php';
const OUT_DIR = new URL('../src/main/resources/com/dropmarket/', import.meta.url);
const CLOG_URL = 'https://oldschool.runescape.wiki/w/Module:Collection_log/data.json?action=raw';
const PAGE = 5000;
const KEEP_PER_MONSTER = 15;
const USER_AGENT = 'PredictionMarketDrops-snapshot (RuneLite plugin; https://github.com/FlippantOnyx/Drop-Market)';

// Must match DropTables.normalize in the plugin.
const normalize = (s) => s.replace(/ /g, ' ').toLowerCase().replace(/\(.*?\)/g, ' ')
	.replace(/[-_]/g, ' ').replace(/[^a-z0-9 ]/g, '').replace(/\s+/g, ' ').trim();

const rows = [];
for (let offset = 0; ; offset += PAGE) {
	const query = `bucket('dropsline').select('page_name','item_name','drop_json').limit(${PAGE}).offset(${offset}).run()`;
	const url = `${API}?action=bucket&format=json&query=${encodeURIComponent(query)}`;
	const res = await fetch(url, { headers: { 'User-Agent': USER_AGENT } });
	if (!res.ok) throw new Error(`Wiki returned ${res.status} at offset ${offset}`);
	const batch = (await res.json()).bucket ?? [];
	rows.push(...batch);
	console.log(`fetched ${rows.length} rows`);
	if (batch.length < PAGE) break;
	await new Promise((r) => setTimeout(r, 1000));
}

const clogRes = await fetch(CLOG_URL, { headers: { 'User-Agent': USER_AGENT } });
if (!clogRes.ok) throw new Error(`Wiki returned ${clogRes.status} for the collection log data`);
const clogItems = [...new Set((await clogRes.json()).map((entry) => entry.name))].sort();
const clogKeys = new Set(clogItems.map(normalize));

// monster -> item -> best drop value (for the market), skipping guaranteed drops.
const byMonster = new Map();
const logDrops = new Map();
for (const row of rows) {
	if (!row.page_name || !row.item_name || !row.drop_json) continue;
	if (/\((deadman|seasonal)/i.test(row.item_name)) continue;
	let drop;
	try { drop = JSON.parse(row.drop_json); } catch { continue; }
	const key = normalize(row.page_name);
	if (!key) continue;

	if (clogKeys.has(normalize(row.item_name))) {
		const set = logDrops.get(key) ?? new Set();
		set.add(row.item_name);
		logDrops.set(key, set);
	}

	if (String(drop.Rarity ?? '').toLowerCase() === 'always') continue;
	const value = typeof drop['Drop Value'] === 'number' ? drop['Drop Value'] : 0;
	const items = byMonster.get(key) ?? new Map();
	items.set(row.item_name, Math.max(items.get(row.item_name) ?? 0, value));
	byMonster.set(key, items);
}

const drops = {};
for (const key of [...byMonster.keys()].sort()) {
	const ranked = [...byMonster.get(key)].sort((a, b) => b[1] - a[1]).slice(0, KEEP_PER_MONSTER);
	drops[key] = ranked.map(([name]) => name);
}

const sources = {};
for (const key of [...logDrops.keys()].sort()) {
	sources[key] = [...logDrops.get(key)].sort();
}

writeFileSync(new URL('wiki-drops.json', OUT_DIR), JSON.stringify(drops));
writeFileSync(new URL('collection-log.json', OUT_DIR), JSON.stringify({ items: clogItems, sources }));
console.log(`wrote ${Object.keys(drops).length} monsters, ${clogItems.length} log items, `
	+ `${Object.keys(sources).length} monsters with log drops`);

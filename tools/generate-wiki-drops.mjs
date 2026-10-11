// Regenerates src/main/resources/com/dropmarket/wiki-drops.json from the OSRS Wiki drops bucket.
// Run with: node tools/generate-wiki-drops.mjs   (needs Node 18+ for fetch; run before each release)
import { writeFileSync } from 'node:fs';

const API = 'https://oldschool.runescape.wiki/api.php';
const OUT = new URL('../src/main/resources/com/dropmarket/wiki-drops.json', import.meta.url);
const PAGE = 5000;
const KEEP_PER_MONSTER = 15;
const USER_AGENT = 'DropPredictionMarket-snapshot (RuneLite plugin; https://github.com/FlippantOnyx/Drop-Market)';

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

// monster -> item -> best drop value, skipping guaranteed drops (same rules as WikiDropTableClient.parse).
const byMonster = new Map();
for (const row of rows) {
	if (!row.page_name || !row.item_name || !row.drop_json) continue;
	let drop;
	try { drop = JSON.parse(row.drop_json); } catch { continue; }
	if (String(drop.Rarity ?? '').toLowerCase() === 'always') continue;
	if (/\((deadman|seasonal)/i.test(row.item_name)) continue;
	const value = typeof drop['Drop Value'] === 'number' ? drop['Drop Value'] : 0;
	const key = normalize(row.page_name);
	if (!key) continue;
	const items = byMonster.get(key) ?? new Map();
	items.set(row.item_name, Math.max(items.get(row.item_name) ?? 0, value));
	byMonster.set(key, items);
}

const out = {};
for (const key of [...byMonster.keys()].sort()) {
	const ranked = [...byMonster.get(key)].sort((a, b) => b[1] - a[1]).slice(0, KEEP_PER_MONSTER);
	out[key] = ranked.map(([name]) => name);
}
writeFileSync(OUT, JSON.stringify(out));
console.log(`wrote ${Object.keys(out).length} monsters`);

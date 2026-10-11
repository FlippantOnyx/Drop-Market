// Regenerates the bundled OSRS Wiki snapshots in src/main/resources/com/dropmarket/:
//   wiki-drops.json       monster -> its best drops by value (what the market bets on)
//   collection-log.json   every collection log item, and per monster the log items that drop rarely enough to
//                         count as a "unique" win (not the super common ones like demon tears or bolt racks)
// Run with: node tools/generate-wiki-drops.mjs   (needs Node 18+ for fetch; run before each release)
import { writeFileSync } from 'node:fs';

const API = 'https://oldschool.runescape.wiki/api.php';
const OUT_DIR = new URL('../src/main/resources/com/dropmarket/', import.meta.url);
const CLOG_URL = 'https://oldschool.runescape.wiki/w/Module:Collection_log/data.json?action=raw';
const PAGE = 5000;
const KEEP_PER_MONSTER = 15;
// A collection log item is a "unique" for a monster only if its best drop rate there is rarer than this.
const UNIQUE_BELOW = 1 / 100;
const USER_AGENT = 'DropPredictionMarket-snapshot (RuneLite plugin; https://github.com/FlippantOnyx/Drop-Market)';

// Must match DropTables.normalize in the plugin.
const normalize = (s) => s.replace(/ /g, ' ').toLowerCase().replace(/\(.*?\)/g, ' ')
	.replace(/[-_]/g, ' ').replace(/[^a-z0-9 ]/g, '').replace(/\s+/g, ' ').trim();

// "1/128", "125/1,012", "Always", "Common"... -> chance per kill. Unknown wording counts as common.
function chance(rarity) {
	const s = String(rarity ?? '').toLowerCase().replace(/,/g, '').trim();
	const m = s.match(/^(\d+(?:\.\d+)?)\s*\/\s*(\d+(?:\.\d+)?)/);
	if (m) return Number(m[1]) / Number(m[2]);
	return s === 'rare' || s === 'very rare' ? 0 : 1;
}

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

// monster -> item -> best drop value (for the market) and best chance (for uniques), skipping guaranteed drops.
const byMonster = new Map();
const bestChance = new Map();
for (const row of rows) {
	if (!row.page_name || !row.item_name || !row.drop_json) continue;
	if (/\((deadman|seasonal)/i.test(row.item_name)) continue;
	let drop;
	try { drop = JSON.parse(row.drop_json); } catch { continue; }
	const key = normalize(row.page_name);
	if (!key) continue;

	const itemChance = new Map(bestChance.get(key) ?? []);
	itemChance.set(row.item_name, Math.max(itemChance.get(row.item_name) ?? 0, chance(drop.Rarity)));
	bestChance.set(key, itemChance);

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

const uniques = {};
for (const key of [...bestChance.keys()].sort()) {
	const rare = [...bestChance.get(key)]
		.filter(([name, p]) => p < UNIQUE_BELOW && clogKeys.has(normalize(name)))
		.map(([name]) => name)
		.sort();
	if (rare.length) uniques[key] = rare;
}

writeFileSync(new URL('wiki-drops.json', OUT_DIR), JSON.stringify(drops));
writeFileSync(new URL('collection-log.json', OUT_DIR), JSON.stringify({ items: clogItems, uniques }));
console.log(`wrote ${Object.keys(drops).length} monsters, ${clogItems.length} log items, `
	+ `${Object.keys(uniques).length} monsters with uniques`);

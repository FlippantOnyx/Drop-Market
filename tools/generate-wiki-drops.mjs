// Regenerates the bundled OSRS Wiki snapshots in src/main/resources/com/dropmarket/:
//   wiki-drops.json       monster -> its best drops by value (what the market bets on)
//   collection-log.json   every collection log item, and per monster the log items it drops (any rate: the plugin
//                         price-checks the common ones itself)
//   valuable-components.json   untradeable DROPS used to make items worth 5m+ (celebrated despite having no GE price)
// Run with: node tools/generate-wiki-drops.mjs   (needs Node 18+ for fetch; run before each release)
import { writeFileSync } from 'node:fs';
import { PAUSE_MS, USER_AGENT, sleep, wikiApi } from './wiki-api.mjs';

const OUT_DIR = new URL('../src/main/resources/com/dropmarket/', import.meta.url);
const PAGE = 5000;
const KEEP_PER_MONSTER = 15;
// Must match DropTables.normalize in the plugin.
const normalize = (s) => s.replace(/ /g, ' ').toLowerCase().replace(/\(.*?\)/g, ' ')
	.replace(/[-_]/g, ' ').replace(/[^a-z0-9 ]/g, '').replace(/\s+/g, ' ').trim();

const rows = [];
for (let offset = 0; ; offset += PAGE) {
	const query = `bucket('dropsline').select('page_name','item_name','drop_json').limit(${PAGE}).offset(${offset}).run()`;
	const batch = (await wikiApi({ action: 'bucket', query })).bucket ?? [];
	rows.push(...batch);
	console.log(`fetched ${rows.length} rows`);
	if (batch.length < PAGE) break;
	await sleep(PAUSE_MS);
}

await sleep(PAUSE_MS);
// The collection log item list lives in a wiki data module; read it through the API, not by fetching the page.
const clogPage = (await wikiApi({ action: 'query', prop: 'revisions', rvprop: 'content', rvslots: 'main',
	titles: 'Module:Collection log/data.json', formatversion: '2' })).query.pages[0];
const clogItems = [...new Set(JSON.parse(clogPage.revisions[0].slots.main.content).map((entry) => entry.name))].sort();
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

// Untradeable items that go into expensive tradeable ones (icons, fangs, cloth...): worth celebrating even though they
// have no GE price. Recipes come from the wiki API; prices from the wiki's real-time prices API (made for tools).
const PRICES = 'https://prices.runescape.wiki/api/v1/osrs';
const MIN_COMPONENT_OUTPUT_VALUE = 5_000_000;
async function prices(path) {
	await sleep(PAUSE_MS);
	const res = await fetch(`${PRICES}/${path}`, { headers: { 'User-Agent': USER_AGENT } });
	if (!res.ok) throw new Error(`Prices API returned ${res.status}`);
	return res.json();
}
const mapping = await prices('mapping');
const latest = (await prices('latest')).data;
const tradeable = new Set(mapping.map((m) => m.name));
const worth = new Map();
for (const m of mapping) {
	const p = latest[m.id];
	if (p) worth.set(m.name, Math.max(p.high ?? 0, p.low ?? 0));
}
const recipes = [];
for (let offset = 0; ; offset += PAGE) {
	await sleep(PAUSE_MS);
	const query = `bucket('recipe').select('page_name','production_json').limit(${PAGE}).offset(${offset}).run()`;
	const batch = (await wikiApi({ action: 'bucket', query })).bucket ?? [];
	recipes.push(...batch);
	if (batch.length < PAGE) break;
}
// Only items that actually drop from something count: parts you make (icons, the inactive Saeldor blade...) never drop
// themselves, and their drop-side ingredients (vestiges, oathplate shards...) are tradeable and priced normally.
const dropped = new Set(rows.map((row) => row.item_name));
const components = new Set();
for (const row of recipes) {
	let recipe;
	try { recipe = JSON.parse(row.production_json); } catch { continue; }
	const output = recipe.output?.name;
	if (!output || !Array.isArray(recipe.materials) || (worth.get(output) ?? 0) < MIN_COMPONENT_OUTPUT_VALUE) continue;
	for (const material of recipe.materials) {
		if (material.name && material.name !== 'Coins' && dropped.has(material.name) && !tradeable.has(material.name)) {
			components.add(material.name);
		}
	}
}

writeFileSync(new URL('wiki-drops.json', OUT_DIR), JSON.stringify(drops));
writeFileSync(new URL('collection-log.json', OUT_DIR), JSON.stringify({ items: clogItems, sources }));
writeFileSync(new URL('valuable-components.json', OUT_DIR), JSON.stringify([...components].sort()));
console.log(`wrote ${Object.keys(drops).length} monsters, ${clogItems.length} log items, ${components.size} valuable components, `
	+ `${Object.keys(sources).length} monsters with log drops`);

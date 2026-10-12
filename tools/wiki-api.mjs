// A polite client for the OSRS Wiki's documented API, shared by the tools in this folder.
// Rules we follow (details in CLAUDE.md): informative User-Agent with a contact URL, one request at a time with a
// pause, the API only (never crawling pages), maxlag so we back off when the wiki is busy, gzip (fetch does this).
export const API = 'https://oldschool.runescape.wiki/api.php';
export const USER_AGENT = 'PredictionMarketDrops-tools (RuneLite plugin; https://github.com/TaikiShuttleRS/Drop-Market)';
export const PAUSE_MS = 1000;
const MAX_LAG = 5;

export const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

/** One GET against the wiki API; waits and retries a few times if the wiki reports lag. */
export async function wikiApi(params) {
	const url = `${API}?${new URLSearchParams({ format: 'json', maxlag: String(MAX_LAG), ...params })}`;
	for (let attempt = 0; attempt < 5; attempt++) {
		const res = await fetch(url, { headers: { 'User-Agent': USER_AGENT } });
		if (!res.ok) throw new Error(`Wiki returned ${res.status}`);
		const json = await res.json();
		if (json.error?.code === 'maxlag') {
			await sleep(5000 * (attempt + 1));
			continue;
		}
		if (json.error) throw new Error(`Wiki API error: ${json.error.code}`);
		return json;
	}
	throw new Error('Wiki stayed lagged; try again later');
}

// Lists new OSRS game update pages from the last N days (default 14) so a human or Claude can check whether the
// plugin needs anything: new bosses/raids (drop tables, region or varbit detection), new collection log items, etc.
// Run with: node tools/check-game-updates.mjs [days]      Prints markdown; prints nothing if there is no news.
import { wikiApi } from './wiki-api.mjs';

const UPDATE_NAMESPACE = 112; // "Update:" on the OSRS Wiki
const days = Number(process.argv[2] ?? 14);
const since = new Date(Date.now() - days * 24 * 3600 * 1000).toISOString();

const changes = (await wikiApi({
	action: 'query', list: 'recentchanges', rcnamespace: String(UPDATE_NAMESPACE), rctype: 'new',
	rcend: since, rclimit: '50', rcprop: 'title|timestamp'
})).query.recentchanges;

const lines = changes
	.filter((c) => !/^Update:.*\/(Transcript|Poll)/i.test(c.title))
	.map((c) => `- [${c.title}](https://oldschool.runescape.wiki/w/${encodeURIComponent(c.title.replace(/ /g, '_'))
		.replace(/%3A/g, ':')}) (${c.timestamp.slice(0, 10)})`);

if (lines.length) {
	console.log(`New OSRS game update pages in the last ${days} days:\n`);
	console.log(lines.join('\n'));
	console.log('\nCheck for: new bosses/raids (add a drop table, region or varbit detection in DropMarketPlugin.activitySource),'
		+ ' new collection log items, new drop sources. Then run `node tools/generate-wiki-drops.mjs`.');
}

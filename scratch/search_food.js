const https = require('https');

function search(q) {
    return new Promise((resolve) => {
        const url = 'https://openclipart.org/search/json/?query=' + encodeURIComponent(q) + '&amount=10';
        https.get(url, { headers: { 'User-Agent': 'Mozilla/5.0' } }, res => {
            let raw = '';
            res.on('data', c => raw += c);
            res.on('end', () => {
                try {
                    const j = JSON.parse(raw);
                    console.log('Query:', q, 'Total:', j.total);
                    const items = (j.payload || []).map(p => ({
                        id: p.id,
                        title: p.title,
                        uploader: p.uploader,
                        filesize: p.filesize,
                        svgUrl: p.svg ? p.svg.url : null,
                        detailUrl: 'https://openclipart.org/detail/' + p.id
                    }));
                    resolve(items);
                } catch (e) {
                    console.log('Err:', e.message);
                    resolve([]);
                }
            });
        }).on('error', e => {
            console.log('Req err:', e.message);
            resolve([]);
        });
    });
}

async function run() {
    const r1 = await search('cake coloring');
    console.log(r1.slice(0, 5));
    const r2 = await search('cupcake coloring');
    console.log(r2.slice(0, 5));
    const r3 = await search('pizza coloring');
    console.log(r3.slice(0, 5));
    const r4 = await search('fruit coloring');
    console.log(r4.slice(0, 5));
}
run();


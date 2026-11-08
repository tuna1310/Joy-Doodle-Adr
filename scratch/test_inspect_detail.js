const https = require('https');

https.get('https://openclipart.org/detail/8534', { headers: { 'User-Agent': 'Mozilla/5.0' } }, res => {
    let b = '';
    res.on('data', c => b += c);
    res.on('end', () => {
        const links = b.match(/href="[^"]*download[^"]*"/gi);
        console.log('Download links on page:', links);
    });
});

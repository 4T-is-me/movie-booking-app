const fs = require('fs');
const content = fs.readFileSync('app/src/main/res/values/strings.xml', 'utf8');
const search = 'auto_str_lumina_cinema_admin';
console.log(`Contains '${search}': ${content.includes(search)}`);
console.log(`Length of strings.xml: ${content.length}`);

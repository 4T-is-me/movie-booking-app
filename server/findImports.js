const fs = require('fs');
const path = require('path');

function walk(dir) {
    let results = [];
    const list = fs.readdirSync(dir);
    list.forEach(function(file) {
        file = dir + '/' + file;
        const stat = fs.statSync(file);
        if (stat && stat.isDirectory()) { 
            results = results.concat(walk(file));
        } else { 
            if(file.endsWith('.js')) {
                results.push(file);
            }
        }
    });
    return results;
}

const files = walk('./src');
files.forEach(file => {
    const content = fs.readFileSync(file, 'utf8');
    const lines = content.split('\n');
    lines.forEach((line, i) => {
        if(line.includes("from '../models/") && !line.includes("Model.js'")) {
            console.log(`File: ${file}:${i+1} -> ${line.trim()}`);
        }
        if(line.includes("require('../models/") && !line.includes("Model.js'")) {
            console.log(`File: ${file}:${i+1} -> ${line.trim()}`);
        }
    });
});

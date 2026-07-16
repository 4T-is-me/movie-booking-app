const fs = require('fs');
const path = require('path');

const layoutDir = path.join(__dirname, 'app', 'src', 'main', 'res', 'layout');
const stringsFile = path.join(__dirname, 'app', 'src', 'main', 'res', 'values', 'strings.xml');
const colorsFile = path.join(__dirname, 'app', 'src', 'main', 'res', 'values', 'colors.xml');

const stringPattern = /(android:text|app:title)="([^@\?][^"]*)"/g;
const colorPattern = /(android:textColor|android:background|app:backgroundTint|app:tint|app:strokeColor|app:cardBackgroundColor|android:buttonTint)="(#([0-9a-fA-F]{3,8}))"/g;

const existingStrings = new Set();
const existingColors = new Map();

function getStringName(val) {
    let name = val.trim().toLowerCase().replace(/[^a-z0-9]/g, '_').replace(/_+/g, '_').replace(/^_|_$/g, '');
    if (!name) name = "str_auto";
    if (name.length > 30) name = name.substring(0, 30).replace(/_$/, '');
    
    let baseName = "auto_str_" + name;
    let finalName = baseName;
    let count = 1;
    while (existingStrings.has(finalName)) {
        finalName = baseName + "_" + count;
        count++;
    }
    existingStrings.add(finalName);
    return finalName;
}

function getColorName(hex) {
    const hexUpper = hex.toUpperCase();
    if (existingColors.has(hexUpper)) return existingColors.get(hexUpper);
    const name = "auto_color_" + hexUpper.replace('#', '');
    existingColors.set(hexUpper, name);
    return name;
}

const extractedStrings = new Map();
const extractedColors = new Map();

let filesChanged = 0;

if (!fs.existsSync(layoutDir)) {
    console.error("Layout directory not found:", layoutDir);
    process.exit(1);
}

const files = fs.readdirSync(layoutDir);
for (const file of files) {
    if (!file.endsWith('.xml')) continue;
    const filePath = path.join(layoutDir, file);
    let content = fs.readFileSync(filePath, 'utf8');
    const original = content;

    content = content.replace(stringPattern, (match, attr, val) => {
        const name = getStringName(val);
        extractedStrings.set(name, val);
        return `${attr}="@string/${name}"`;
    });

    content = content.replace(colorPattern, (match, attr, hex) => {
        const name = getColorName(hex);
        extractedColors.set(name, hex);
        return `${attr}="@color/${name}"`;
    });

    if (content !== original) {
        fs.writeFileSync(filePath, content, 'utf8');
        filesChanged++;
    }
}

if (extractedStrings.size > 0) {
    let sContent = fs.readFileSync(stringsFile, 'utf8');
    const idx = sContent.lastIndexOf('</resources>');
    if (idx !== -1) {
        let newStrings = "\n    <!-- Auto-extracted strings (Node Script) -->\n";
        for (const [name, val] of extractedStrings.entries()) {
            const escaped = val.replace(/&/g, '&amp;').replace(/'/g, "\\'").replace(/"/g, '\\"').replace(/</g, '&lt;');
            newStrings += `    <string name="${name}">${escaped}</string>\n`;
        }
        sContent = sContent.slice(0, idx) + newStrings + sContent.slice(idx);
        fs.writeFileSync(stringsFile, sContent, 'utf8');
    }
}

if (extractedColors.size > 0) {
    let cContent = fs.readFileSync(colorsFile, 'utf8');
    const idx = cContent.lastIndexOf('</resources>');
    if (idx !== -1) {
        let newColors = "\n    <!-- Auto-extracted colors (Node Script) -->\n";
        for (const [name, hex] of extractedColors.entries()) {
            newColors += `    <color name="${name}">${hex}</color>\n`;
        }
        cContent = cContent.slice(0, idx) + newColors + cContent.slice(idx);
        fs.writeFileSync(colorsFile, cContent, 'utf8');
    }
}

console.log("=== Extraction Complete ===");
console.log(`Files modified: ${filesChanged}`);
console.log(`New strings extracted: ${extractedStrings.size}`);
console.log(`New colors extracted: ${extractedColors.size}`);

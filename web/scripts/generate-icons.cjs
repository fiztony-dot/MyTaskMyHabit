#!/usr/bin/env node
// Genera iconos PNG placeholder para el manifiesto PWA
// Sin dependencias externas — solo módulos built-in de Node.js
const zlib = require('zlib');
const fs = require('fs');
const path = require('path');

const crcTable = (() => {
  const t = [];
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = (c & 1) ? (0xEDB88320 ^ (c >>> 1)) : (c >>> 1);
    t[n] = c;
  }
  return t;
})();

function crc32(buf) {
  let crc = 0xFFFFFFFF;
  for (let i = 0; i < buf.length; i++) crc = (crc >>> 8) ^ crcTable[(crc ^ buf[i]) & 0xFF];
  return (crc ^ 0xFFFFFFFF) >>> 0;
}

function u32be(n) {
  const b = Buffer.alloc(4);
  b.writeUInt32BE(n, 0);
  return b;
}

function pngChunk(type, data) {
  const t = Buffer.from(type, 'ascii');
  const c = crc32(Buffer.concat([t, data]));
  return Buffer.concat([u32be(data.length), t, data, u32be(c)]);
}

function makePNG(size, r, g, b) {
  const sig = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);

  const ihdrData = Buffer.alloc(13);
  ihdrData.writeUInt32BE(size, 0);
  ihdrData.writeUInt32BE(size, 4);
  ihdrData[8] = 8; // bit depth
  ihdrData[9] = 2; // color type RGB
  // compression, filter, interlace = 0

  const rowSize = size * 3 + 1;
  const raw = Buffer.alloc(size * rowSize);
  for (let y = 0; y < size; y++) {
    const base = y * rowSize;
    raw[base] = 0; // filter None
    for (let x = 0; x < size; x++) {
      raw[base + 1 + x * 3] = r;
      raw[base + 2 + x * 3] = g;
      raw[base + 3 + x * 3] = b;
    }
  }

  const compressed = zlib.deflateSync(raw, { level: 9 });

  return Buffer.concat([
    sig,
    pngChunk('IHDR', ihdrData),
    pngChunk('IDAT', compressed),
    pngChunk('IEND', Buffer.alloc(0)),
  ]);
}

const outDir = path.join(__dirname, '../public/icons');
fs.mkdirSync(outDir, { recursive: true });

// Theme color: indigo #6366f1
const r = 0x63, g = 0x66, b = 0xf1;

fs.writeFileSync(path.join(outDir, 'icon-192.png'), makePNG(192, r, g, b));
fs.writeFileSync(path.join(outDir, 'icon-512.png'), makePNG(512, r, g, b));
console.log('✓ Iconos generados: public/icons/icon-192.png, icon-512.png');

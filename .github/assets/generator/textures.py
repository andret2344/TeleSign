"""Reads textures straight from the Minecraft client jar, so no Mojang file has to be in the repository."""
import os
import struct
import zipfile
import zlib

# The client jar of the version the textures come from; MINECRAFT_JAR points to another one
JAR = os.environ.get('MINECRAFT_JAR', os.path.join(os.environ.get('APPDATA', ''), '.minecraft', 'versions', '26.2',
                                                   '26.2.jar'))


def texture(name):
    """A texture from the jar by its path under assets/minecraft/textures, e.g. block/oak_sign."""
    with zipfile.ZipFile(JAR) as jar:
        return read_png(jar.read('assets/minecraft/textures/' + name + '.png'))


def read_png(data):
    """Decodes a PNG into rows of (r, g, b, a) tuples."""
    assert data[:8] == b'\x89PNG\r\n\x1a\n'
    pos, idat, palette, trns = 8, b'', None, None
    while pos < len(data):
        length, kind = struct.unpack('>I4s', data[pos:pos + 8])
        chunk = data[pos + 8:pos + 8 + length]
        pos += 12 + length
        if kind == b'IHDR':
            width, height, depth, ctype, _, _, interlace = struct.unpack('>IIBBBBB', chunk)
            assert depth <= 8 and interlace == 0, (depth, interlace)
        elif kind == b'PLTE':
            palette = [tuple(chunk[i:i + 3]) for i in range(0, len(chunk), 3)]
        elif kind == b'tRNS':
            trns = chunk
        elif kind == b'IDAT':
            idat += chunk
    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ctype]
    raw = zlib.decompress(idat)
    stride = (width * channels * depth + 7) // 8
    # Filters work on whole bytes, so with less than 8 bits per pixel they look one byte back
    bpp = max(1, channels * depth // 8)
    rows, prev, i = [], bytearray(stride), 0
    for _ in range(height):
        f = raw[i]
        line = bytearray(raw[i + 1:i + 1 + stride])
        i += 1 + stride
        for x in range(stride):
            a = line[x - bpp] if x >= bpp else 0
            b = prev[x]
            c = prev[x - bpp] if x >= bpp else 0
            if f == 1:
                line[x] = (line[x] + a) & 255
            elif f == 2:
                line[x] = (line[x] + b) & 255
            elif f == 3:
                line[x] = (line[x] + (a + b) // 2) & 255
            elif f == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[x] = (line[x] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
        rows.append(line)
        prev = line
    if depth < 8:
        # Unpacks the samples, so each one takes a byte like with 8 bits per pixel
        per_byte, mask = 8 // depth, (1 << depth) - 1
        unpacked = []
        for line in rows:
            samples = bytearray()
            for byte in line:
                for k in range(per_byte):
                    samples.append((byte >> (8 - depth * (k + 1))) & mask)
            if ctype == 0:
                samples = bytearray(s * 255 // mask for s in samples)
            unpacked.append(samples[:width * channels])
        rows = unpacked
    pixels = []
    for line in rows:
        row = []
        for x in range(width):
            px = line[x * channels:(x + 1) * channels]
            if ctype == 6:
                row.append(tuple(px))
            elif ctype == 2:
                row.append((*px, 255))
            elif ctype == 3:
                idx = px[0]
                alpha = trns[idx] if trns and idx < len(trns) else 255
                row.append((*palette[idx], alpha))
            elif ctype == 4:
                row.append((px[0], px[0], px[0], px[1]))
            else:
                row.append((px[0], px[0], px[0], 255))
        pixels.append(row)
    return pixels



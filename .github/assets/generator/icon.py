"""Draws the TeleSign icon as a pure vector SVG from the Minecraft oak sign texture and font."""
from textures import texture

SIGN = texture('block/oak_sign')
FONT = texture('font/ascii')

# Regions of the 26.2 sign texture in pixels (x, y, width, height), from models/block/template_sign_rot_0.json
BOARD_FRONT = (0, 2, 24, 12)
STICK_FRONT = (28, 0, 2, 14)

LINES = [('[TELESIGN]', '#55ffff'), ('-71.5, 63.5, 35.5', '#000000')]


def hex_color(rgba):
    r, g, b, _ = rgba
    return '#%02x%02x%02x' % (r, g, b)


def region(rect):
    """The texels of a texture region as squares in its own coordinates, one texel = 1."""
    x0, y0, width, height = rect
    out = []
    for v in range(height):
        for u in range(width):
            rgba = SIGN[y0 + v][x0 + u]
            if rgba[3] == 0:
                continue
            color = hex_color(rgba)
            # The stroke in the same colour hides the hairline seams between neighbouring texels
            out.append('<rect x="%d" y="%d" width="1" height="1" fill="%s" stroke="%s" stroke-width="0.04"/>'
                       % (u, v, color, color))
    return ''.join(out)


def glyph(char):
    """The pixels of a character in the 16x16 grid of 8x8 cells, and how far the next one starts."""
    if char == ' ':
        return [], 4
    code = ord(char)
    cx, cy = (code % 16) * 8, (code // 16) * 8
    pixels = [(x, y) for y in range(8) for x in range(8) if FONT[cy + y][cx + x][3] > 0]
    return pixels, max(x for x, _ in pixels) + 2


def text_width(content):
    return sum(glyph(char)[1] for char in content) - 1


def text(content, x, y, scale, color):
    """Minecraft text as one path of squares, in the coordinates of the face it is written on."""
    rects = []
    cursor = x
    for char in content:
        pixels, advance = glyph(char)
        rects += ['M%.3f %.3fh%.3fv%.3fh-%.3fz' % (cursor + px * scale, y + py * scale, scale, scale, scale)
                  for px, py in pixels]
        cursor += advance * scale
    return '<path d="%s" fill="%s"/>' % (''.join(rects), color)


def sign_text():
    """The lines on the board front, centred like on a sign, as large as the longest one allows."""
    width, height = BOARD_FRONT[2], BOARD_FRONT[3]
    scale = (width - 2.4) / max(text_width(line) for line, _ in LINES)
    # The first line is drawn bigger, it is what the icon is about
    scales = [min(scale * 1.6, (width - 2.4) / text_width(LINES[0][0])), scale]
    gap = 3 * scale
    total = sum(8 * s for s in scales) + gap
    y = (height - total) / 2
    out = []
    for (line, color), s in zip(LINES, scales):
        out.append(text(line, (width - text_width(line) * s) / 2, y, s, color))
        y += 8 * s + gap
    return ''.join(out)


def svg(body, size=512):
    return '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 %d %d" width="%d" height="%d">%s</svg>' % (
        size, size, size, size, body)


def icon():
    """The sign seen from the front: the board with the text, the stick under it, with a margin around."""
    texel = 16.5
    width, height = 24 * texel, 26 * texel
    x, y = (512 - width) / 2, (512 - height) / 2
    board = '<g transform="translate(%.1f %.1f) scale(%.2f)">%s%s</g>' % (x, y, texel, region(BOARD_FRONT), sign_text())
    stick = '<g transform="translate(%.1f %.1f) scale(%.2f)">%s</g>' % (x + 11 * texel, y + 12 * texel, texel,
                                                                         region(STICK_FRONT))
    return svg(stick + board)


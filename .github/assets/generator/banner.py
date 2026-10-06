"""Draws the TeleSign banner, 1200x400: the icon on the left, the name and the tagline on dark planks."""
import re

from icon import hex_color, icon, text
from textures import texture

WIDTH, HEIGHT = 1200, 400
AQUA, AQUA_SHADOW = '#55ffff', '#153f3f'
LIGHT, LIGHT_SHADOW = '#e6e6e6', '#393939'


def pattern(pattern_id, tex, size):
    cell = size / len(tex)
    rects = []
    for v, row in enumerate(tex):
        for u, rgba in enumerate(row):
            rects.append('<rect x="%.2f" y="%.2f" width="%.2f" height="%.2f" fill="%s"/>' % (
                u * cell, v * cell, cell + 0.3, cell + 0.3, hex_color(rgba)))
    return '<pattern id="%s" patternUnits="userSpaceOnUse" width="%.2f" height="%.2f">%s</pattern>' % (
        pattern_id, size, size, ''.join(rects))


def shadowed(content, x, y, scale, color, shadow):
    """Minecraft-style text with its shadow one pixel down and right."""
    return text(content, x + scale, y + scale, scale, shadow) + text(content, x, y, scale, color)


def icon_inner():
    return re.sub(r'^<svg[^>]*>', '', icon()).rsplit('</svg>', 1)[0]


def nested(inner, x, y, size):
    return '<svg x="%.1f" y="%.1f" width="%.1f" height="%.1f" viewBox="0 0 512 512">%s</svg>' % (x, y, size, size, inner)


def svg(defs, body):
    return ('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 %d %d" width="%d" height="%d"><defs>%s</defs>%s</svg>'
            % (WIDTH, HEIGHT, WIDTH, HEIGHT, defs, body))


def banner():
    """The icon on the left, the name and the tagline on the right, on darkened planks, like the BlockGens banner."""
    defs = (pattern('planks', texture('block/dark_oak_planks'), 64)
            + '<linearGradient id="fade" x1="0" x2="1"><stop offset="0" stop-color="#0e0f12" stop-opacity="0.45"/>'
              '<stop offset="1" stop-color="#0e0f12" stop-opacity="0.9"/></linearGradient>')
    body = ('<rect width="%d" height="%d" fill="url(#planks)"/><rect width="%d" height="%d" fill="url(#fade)"/>'
            % (WIDTH, HEIGHT, WIDTH, HEIGHT))
    body += nested(icon_inner(), 30, 10, 380)
    text_x, title_y, title_scale, line_scale = 430, 92, 11, 5
    body += shadowed('TeleSign', text_x, title_y, title_scale, AQUA, AQUA_SHADOW)
    line_y = title_y + 8 * title_scale + 42
    for line in ('Teleport signs for Paper', 'No warps. No commands.'):
        body += shadowed(line, text_x, line_y, line_scale, LIGHT, LIGHT_SHADOW)
        line_y += 10 * line_scale + 8
    return svg(defs, body)


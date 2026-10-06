"""Generates the icon and the banner into .github/assets, as SVG and PNG, and the icon of the IntelliJ project.

Run from anywhere with `python .github/assets/generator/generate.py`. Needs the Minecraft 26.2 client jar for the
textures (MINECRAFT_JAR points to another one) and Chrome to turn the SVGs into PNGs (CHROME points to another one).
"""
import os
import pathlib
import shutil
import subprocess
import sys
import tempfile

sys.path.insert(0, str(pathlib.Path(__file__).parent))

from banner import HEIGHT, WIDTH, banner  # noqa: E402
from icon import icon  # noqa: E402

ASSETS = pathlib.Path(__file__).resolve().parent.parent
ROOT = ASSETS.parent.parent
CHROME = os.environ.get('CHROME', r'C:\Program Files\Google\Chrome\Application\chrome.exe')


def render(svg_path, png_path, width, height):
    """Screenshots the SVG with headless Chrome, keeping the transparent background."""
    with tempfile.TemporaryDirectory() as profile:
        subprocess.run([CHROME, '--headless=new', '--disable-gpu', '--hide-scrollbars', '--user-data-dir=' + profile,
                        '--default-background-color=00000000', '--window-size=%d,%d' % (width, height),
                        '--screenshot=' + str(png_path), svg_path.as_uri()],
                       check=True, capture_output=True)


def main():
    for name, content, width, height in (('icon', icon(), 512, 512), ('banner', banner(), WIDTH, HEIGHT)):
        svg_path = ASSETS / (name + '.svg')
        svg_path.write_text(content, encoding='utf-8', newline='\n')
        render(svg_path, ASSETS / (name + '.png'), width, height)
    shutil.copyfile(ASSETS / 'icon.png', ROOT / '.idea' / 'icon.png')


if __name__ == '__main__':
    main()

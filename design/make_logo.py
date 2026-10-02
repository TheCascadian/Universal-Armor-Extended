"""Builds logo.png (square mod icon) and banner.png for Universal Armor Extended.

Reuses the Universal Armor title lettering and the Universal Pipes pixel font,
with the three armor shapes (light / standard / heavy) as the emblem.
Usage: python3 design/make_logo.py <outdir>
"""
import importlib.util, sys
from pathlib import Path
from PIL import Image
import gen_designs as gd

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent
spec = importlib.util.spec_from_file_location("pipes_logo", ROOT / "Universal-Pipes/tools/make_logo.py")
pl = importlib.util.module_from_spec(spec); spec.loader.exec_module(pl)
pl.G.update({
    'X': ["X...X", "X...X", ".X.X.", "..X..", ".X.X.", "X...X", "X...X"],
    'T': ["XXXXX", "..X..", "..X..", "..X..", "..X..", "..X..", "..X.."],
    'D': ["XXXX.", "X...X", "X...X", "X...X", "X...X", "X...X", "XXXX."],
})
BG = (30, 33, 40, 255)
# One muted stone/iron palette shared by all three sets, matching the Core lettering
MATS = {gd.F: (140, 143, 148), gd.P: (92, 82, 76), gd.L: (112, 118, 124), gd.A: (176, 154, 96)}
PART_COLOURS = [MATS[gd.F], MATS[gd.P], MATS[gd.L], MATS[gd.A]]


def figure(shape, yaw):
    im = gd.render(gd.build(shape), MATS, yaw, (92, 150), 3.1, shape=shape)
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            if px[x, y] == BG: px[x, y] = (0, 0, 0, 0)
    return pl.crop(im)


def emblem(height):
    figs = [pl.outline(figure(s, y), th=1) for s, y in (("light", -22), ("heavy", 0), ("standard", 22))]
    k = height // max(f.height for f in figs)
    figs = [f.resize((f.width * k, f.height * k), Image.NEAREST) for f in figs]
    gap = 2 * k
    W = sum(f.width for f in figs) + gap * 2
    out = Image.new("RGBA", (W, max(f.height for f in figs)), (0, 0, 0, 0))
    x = 0
    for f in figs:
        out.alpha_composite(f, (x, out.height - f.height)); x += f.width + gap
    return out


def core_lettering():
    src = Image.open(ROOT / "Universal-Armor/universalarmor3.png").convert("RGBA")
    a = src.crop((620, 0, src.width, src.height))
    top = pl.crop(a.crop((0, 0, a.width, 268)))
    bottom = pl.crop(a.crop((0, 268, a.width, a.height)))
    return top, bottom


def banner(H=600):
    top, bottom = core_lettering()
    ext = pl.outline_big(pl.word("EXTENDED", 20, colors=[(150, 152, 158)] * 8), 20)
    tw = max(top.width, bottom.width, ext.width)
    # scale the Core lettering so it matches the width of EXTENDED
    s = ext.width / top.width
    top = top.resize((ext.width, int(top.height * s)), Image.LANCZOS)
    bottom = bottom.resize((int(bottom.width * s), int(bottom.height * s)), Image.LANCZOS)
    gap = 18
    th = top.height + bottom.height + ext.height + gap * 2
    icon = emblem(max(th, H // 2))
    W = icon.width + 50 + ext.width
    Hh = max(icon.height, th)
    out = Image.new("RGBA", (W, Hh), (0, 0, 0, 0))
    out.alpha_composite(icon, (0, (Hh - icon.height) // 2))
    x, y = icon.width + 50, (Hh - th) // 2
    for im in (top, bottom, ext):
        out.alpha_composite(im, (x, y)); y += im.height + gap
    return out


if __name__ == "__main__":
    outdir = Path(sys.argv[1]); outdir.mkdir(parents=True, exist_ok=True)
    b = banner(); b.save(outdir / "banner.png")
    ic = emblem(300)
    sc = 448 // max(ic.size) or 1
    sq = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
    ic = ic.resize((448, int(ic.height * 448 / ic.width)), Image.NEAREST)
    sq.alpha_composite(ic, ((512 - ic.width) // 2, (512 - ic.height) // 2))
    sq.resize((1024, 1024), Image.NEAREST).save(outdir / "logo.png")
    print(b.size)

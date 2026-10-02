"""Armor shape designs: paints per-component masks and renders a preview sheet.

Usage: python3 design/gen_designs.py sheet          -> design/armor_sheet.png
       python3 design/gen_designs.py export <dir>   -> writes mask PNGs for the mod
"""
import math, sys, os
from PIL import Image, ImageDraw, ImageFont

NONE, F, P, L, A = 0, 1, 2, 3, 4
NAMES = {F: "frame", P: "padding", L: "plating", A: "attachment"}

# part -> (u, v, w, h, d)
PARTS1 = {"head": (0, 0, 8, 8, 8), "body": (16, 16, 8, 12, 4),
          "arm": (40, 16, 4, 12, 4), "leg": (0, 16, 4, 12, 4)}
PARTS2 = {"body": (16, 16, 8, 12, 4), "leg": (0, 16, 4, 12, 4)}

def face_rect(part, face):
    u, v, w, h, d = part
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
            "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
            "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}[face]

# Heavy uses real extra geometry on a 128x64 texture: extras live right of / below the vanilla layout.
EXTRA1 = {"crest": (64, 0, 1, 2, 10), "paul1": (64, 16, 7, 3, 7), "paul2": (64, 28, 8, 2, 8),
          "keel": (96, 16, 2, 6, 1), "toe": (96, 28, 5, 3, 2), "brow": (96, 36, 11, 1, 1)}
EXTRA2 = {"tasset": (64, 48, 9, 4, 1)}

class Layer:
    def __init__(self, parts, W=64, H=32):
        self.parts = parts
        self.W, self.H = W, H
        self.comp = [[NONE] * W for _ in range(H)]
        self.tone = [[1.0] * W for _ in range(H)]

    def R(self, part, faces, x0, y0, x1, y1, comp):
        if faces == "sides": faces = ["right", "front", "left", "back"]
        elif faces == "all": faces = ["top", "bottom", "right", "front", "left", "back"]
        elif isinstance(faces, str): faces = [faces]
        for f in faces:
            fx, fy, fw, fh = face_rect(self.parts[part], f)
            for y in range(y0, min(y1, fh)):
                for x in range(x0, min(x1, fw)):
                    self.comp[fy + y][fx + x] = comp

    def finish(self):
        """Bevel shading from region edges."""
        base = {F: 0.80, P: 0.70, L: 0.90, A: 0.95}
        out = [[0.0] * self.W for _ in range(self.H)]
        for y in range(self.H):
            for x in range(self.W):
                c = self.comp[y][x]
                if not c: continue
                s = base[c]
                up = self.comp[y - 1][x] if y else 0
                dn = self.comp[y + 1][x] if y < self.H - 1 else 0
                lf = self.comp[y][x - 1] if x else 0
                rt = self.comp[y][x + 1] if x < self.W - 1 else 0
                if up != c: s += 0.12
                if lf != c: s += 0.06
                if dn != c: s -= 0.14
                if rt != c: s -= 0.07
                out[y][x] = max(0.35, min(1.0, s))
        self.tone = out

def build(shape):
    if shape == "heavy":
        l1 = Layer({**PARTS1, **EXTRA1}, 128, 64); l2 = Layer({**PARTS2, **EXTRA2}, 128, 64)
    else:
        l1, l2 = Layer(PARTS1), Layer(PARTS2)
    if shape == "standard":
        h = "head"
        l1.R(h, "sides", 0, 0, 8, 6, F); l1.R(h, "front", 0, 6, 8, 8, NONE)
        l1.R(h, "back", 0, 0, 8, 8, F); l1.R(h, "top", 0, 0, 8, 8, F)
        l1.R(h, "front", 0, 0, 8, 3, F); l1.R(h, "front", 0, 2, 8, 3, L)
        l1.R(h, "front", 0, 3, 2, 8, L); l1.R(h, "front", 6, 3, 8, 8, L)
        l1.R(h, "front", 3, 3, 5, 6, L); l1.R(h, "front", 2, 3, 6, 4, P)
        l1.R(h, "front", 2, 4, 3, 8, NONE); l1.R(h, "front", 5, 4, 6, 8, NONE)
        l1.R(h, ["right", "left"], 2, 2, 6, 5, L)
        l1.R(h, "top", 3, 0, 5, 8, L)
        b = "body"
        l1.R(b, "all", 0, 0, 8, 12, F)
        for f in ("front", "back"):
            l1.R(b, f, 1, 0, 7, 8, L); l1.R(b, f, 1, 8, 7, 12, P)
        l1.R(b, "front", 3, 3, 5, 5, A)
        l1.R(b, "bottom", 0, 0, 8, 4, NONE)
        a = "arm"
        l1.R(a, "all", 0, 0, 4, 12, F); l1.R(a, "top", 0, 0, 4, 4, L)
        l1.R(a, "sides", 0, 0, 4, 3, L); l1.R(a, "sides", 0, 9, 4, 10, A)
        g = "leg"
        l1.R(g, "sides", 0, 6, 4, 12, F); l1.R(g, "bottom", 0, 0, 4, 4, L)
        l1.R(g, "front", 0, 9, 4, 12, L); l1.R(g, "sides", 0, 6, 4, 7, A)
        l1.R(g, ["right", "left"], 0, 7, 4, 9, P)
        l2.R("body", "sides", 0, 8, 8, 12, F); l2.R("body", "sides", 0, 8, 8, 9, A)
        l2.R("body", "front", 3, 8, 5, 9, L)
        l2.R("leg", "sides", 0, 0, 4, 12, F); l2.R("leg", "top", 0, 0, 4, 4, F)
        l2.R("leg", "front", 0, 2, 4, 5, L); l2.R("leg", "front", 0, 6, 4, 8, L)
        l2.R("leg", ["right", "left"], 1, 3, 3, 9, P)
    elif shape == "heavy":
        h = "head"
        l1.R(h, "all", 0, 0, 8, 8, L); l1.R(h, "bottom", 0, 0, 8, 8, NONE)
        l1.R(h, "front", 1, 3, 7, 5, NONE)
        l1.R(h, "front", 0, 2, 8, 3, A); l1.R(h, "front", 0, 5, 8, 6, A)
        l1.R(h, "top", 3, 0, 5, 8, A); l1.R(h, "back", 3, 0, 5, 6, A)
        l1.R(h, ["right", "left"], 1, 1, 2, 2, A); l1.R(h, ["right", "left"], 6, 1, 7, 2, A)
        l1.R(h, "sides", 0, 7, 8, 8, P); l1.R(h, ["right", "left", "front"], 0, 6, 8, 7, F)
        b = "body"
        l1.R(b, "all", 0, 0, 8, 12, L); l1.R(b, "bottom", 0, 0, 8, 4, NONE)
        for f in ("front", "back"):
            l1.R(b, f, 0, 0, 1, 12, F); l1.R(b, f, 7, 0, 8, 12, F)
            l1.R(b, f, 3, 0, 5, 1, A); l1.R(b, f, 3, 11, 5, 12, A)
            l1.R(b, f, 0, 9, 8, 10, A); l1.R(b, f, 0, 10, 8, 11, P)
        for (rx, ry) in ((1, 1), (6, 1), (1, 7), (6, 7)):
            l1.R(b, "front", rx, ry, rx + 1, ry + 1, A)
        l1.R(b, "front", 3, 5, 5, 7, A)
        a = "arm"
        l1.R(a, "all", 0, 0, 4, 12, L)
        l1.R(a, "sides", 0, 0, 4, 1, A); l1.R(a, "sides", 0, 3, 4, 4, A)
        l1.R(a, "sides", 0, 6, 4, 7, A); l1.R(a, "sides", 0, 7, 4, 9, P)
        l1.R(a, "sides", 0, 9, 4, 10, A)
        g = "leg"
        l1.R(g, "sides", 0, 5, 4, 12, L); l1.R(g, "bottom", 0, 0, 4, 4, F)
        l1.R(g, "sides", 0, 5, 4, 6, A); l1.R(g, "sides", 0, 6, 4, 7, P)
        l1.R(g, "sides", 0, 7, 4, 10, F); l1.R(g, "front", 0, 10, 4, 12, A)
        l2.R("body", "sides", 0, 7, 8, 12, L); l2.R("body", "sides", 0, 7, 8, 9, A)
        l2.R("body", "front", 3, 8, 5, 9, L)
        l2.R("leg", "all", 0, 0, 4, 12, L); l2.R("leg", "sides", 0, 0, 4, 1, A)
        l2.R("leg", "front", 0, 5, 4, 8, A)
        l2.R("leg", ["right", "left", "back"], 1, 2, 3, 4, F)
        l2.R("leg", ["right", "left"], 1, 8, 3, 11, P)
        # extra geometry
        l1.R("crest", "all", 0, 0, 1, 12, L); l1.R("crest", "top", 0, 0, 1, 10, A)
        l1.R("paul1", "all", 0, 0, 8, 7, L); l1.R("paul1", "top", 0, 0, 7, 7, L)
        l1.R("paul1", ["right", "front", "left", "back"], 0, 2, 7, 3, A)
        l1.R("paul2", "all", 0, 0, 8, 8, L); l1.R("paul2", ["right", "front", "left", "back"], 0, 0, 8, 1, A)
        l1.R("paul2", ["right", "front", "left", "back"], 0, 1, 8, 2, F)
        l1.R("keel", "all", 0, 0, 2, 6, L); l1.R("keel", "front", 0, 0, 2, 6, A)
        l1.R("toe", "all", 0, 0, 5, 3, L); l1.R("toe", "front", 0, 0, 5, 1, A)
        l1.R("brow", "all", 0, 0, 11, 1, L); l1.R("brow", "front", 0, 0, 11, 1, A)
        l2.R("tasset", "all", 0, 0, 9, 4, L)
        l2.R("tasset", ["front", "back"], 0, 0, 9, 1, A); l2.R("tasset", ["front", "back"], 0, 3, 9, 4, A)
    else:  # light
        h = "head"
        l1.R(h, "top", 0, 0, 8, 8, P); l1.R(h, "top", 3, 0, 5, 8, F)
        l1.R(h, "front", 0, 0, 8, 2, P); l1.R(h, "front", 0, 2, 8, 3, A)
        l1.R(h, "front", 2, 1, 6, 2, L)
        l1.R(h, ["right", "left"], 0, 0, 8, 4, P)
        l1.R(h, ["right", "left"], 0, 4, 3, 6, F)
        l1.R(h, "back", 0, 0, 8, 8, P); l1.R(h, "back", 3, 6, 5, 8, A)
        b = "body"
        l1.R(b, "sides", 0, 0, 8, 12, P)
        l1.R(b, "top", 0, 0, 8, 4, F)
        for f in ("front", "back"):
            l1.R(b, f, 0, 0, 1, 12, F); l1.R(b, f, 7, 0, 8, 12, F)
            for i in range(8):
                l1.R(b, f, i, i * 8 // 8, i + 1, i * 8 // 8 + 2, A)
                l1.R(b, f, 7 - i, i * 8 // 8, 8 - i, i * 8 // 8 + 2, A)
            l1.R(b, f, 0, 9, 8, 10, A); l1.R(b, f, 3, 9, 5, 10, L)
        l1.R(b, "front", 2, 1, 6, 5, L)
        for f in ("front", "back"):
            for i in range(8):
                pass
        l1.R(b, "front", 3, 5, 5, 6, NONE) if False else None
        a = "arm"
        l1.R(a, "sides", 0, 0, 4, 12, P); l1.R(a, "top", 0, 0, 4, 4, F)
        l1.R(a, "sides", 0, 0, 4, 2, F); l1.R(a, "sides", 0, 8, 4, 11, A)
        l1.R(a, "sides", 1, 9, 3, 10, L)
        g = "leg"
        l1.R(g, "sides", 0, 6, 4, 12, P); l1.R(g, "sides", 0, 6, 4, 8, F)
        l1.R(g, "sides", 0, 8, 4, 9, A); l1.R(g, "front", 1, 10, 3, 12, L)
        l1.R(g, "bottom", 0, 0, 4, 4, F)
        l2.R("body", "sides", 0, 8, 8, 12, P); l2.R("body", "sides", 0, 8, 8, 9, A)
        l2.R("body", "front", 5, 9, 7, 11, A)
        l2.R("leg", "sides", 0, 0, 4, 12, P); l2.R("leg", "top", 0, 0, 4, 4, F)
        l2.R("leg", "sides", 0, 3, 4, 4, A); l2.R("leg", "front", 1, 5, 3, 7, L)
        l2.R("leg", "sides", 0, 9, 4, 10, A)
    l1.finish(); l2.finish()
    return l1, l2

# ---------------------------------------------------------------- rendering
MATS = {F: (190, 192, 198), P: (150, 92, 52), L: (72, 205, 192), A: (232, 190, 62)}

def colour(layer, x, y, mats):
    c = layer.comp[y][x]
    if not c: return None
    m, t = mats[c], layer.tone[y][x]
    return tuple(min(255, int(v * t)) for v in m)

def sheet_image(layer, mats, scale=5):
    scale = scale * 64 // layer.W if layer.W > 64 else scale
    im = Image.new("RGBA", (layer.W * scale, layer.H * scale), (40, 44, 52, 255))
    d = ImageDraw.Draw(im)
    for y in range(layer.H):
        for x in range(layer.W):
            c = colour(layer, x, y, mats)
            if c: d.rectangle([x * scale, y * scale, (x + 1) * scale - 1, (y + 1) * scale - 1], fill=c + (255,))
    return im

# model boxes: name -> (x0,y0,z0, w,h,d, part key, mirror)
def boxes(inflate1, inflate2):
    out = []
    e = inflate1
    out.append(("head", 1, (-4, -8, -4, 8, 8, 8), "head", False, e))
    out.append(("body", 1, (-4, 0, -2, 8, 12, 4), "body", False, e))
    out.append(("arm", 1, (-8, 0, -2, 4, 12, 4), "arm", False, e))
    out.append(("arm", 1, (4, 0, -2, 4, 12, 4), "arm", True, e))
    out.append(("leg", 1, (-4, 12, -2, 4, 12, 4), "leg", False, e))
    out.append(("leg", 1, (0, 12, -2, 4, 12, 4), "leg", True, e))
    e = inflate2
    out.append(("body", 2, (-4, 0, -2, 8, 12, 4), "body", False, e))
    out.append(("leg", 2, (-4, 12, -2, 4, 12, 4), "leg", False, e))
    out.append(("leg", 2, (0, 12, -2, 4, 12, 4), "leg", True, e))
    return out

def extra_boxes(shape):
    if shape != "heavy": return []
    o = []
    # (layer, part, (x0,y0,z0,w,h,d) absolute, mirror)
    o.append((1, "crest", (-0.5, -11, -5, 1, 2, 10), False))
    o.append((1, "brow", (-5.5, -6, -6, 11, 1, 1), False))
    o.append((1, "keel", (-1, 1, -4, 2, 6, 1), False))
    o.append((1, "paul1", (-9.5, -1.5, -3.5, 7, 3, 7), False))
    o.append((1, "paul1", (2.5, -1.5, -3.5, 7, 3, 7), True))
    o.append((1, "paul2", (-10, 1.5, -4, 8, 2, 8), False))
    o.append((1, "paul2", (2, 1.5, -4, 8, 2, 8), True))
    o.append((1, "toe", (-4.5, 22, -5, 5, 3, 2), False))
    o.append((1, "toe", (-0.5, 22, -5, 5, 3, 2), True))
    o.append((2, "tasset", (-4.5, 10.5, -3.5, 9, 4, 1), False))
    o.append((2, "tasset", (-4.5, 10.5, 2.5, 9, 4, 1), False))
    return o

FACE_SHADE = {"front": 1.0, "back": 0.78, "left": 0.88, "right": 0.88, "top": 1.05, "bottom": 0.6}

def face_texels(bx, face, mirror, part, e):
    """Yield (3D corner points of texel quad, texture x, y) for a box face."""
    x0, y0, z0, w, h, d = bx
    x0 -= e; y0 -= e; z0 -= e; w += 2 * e; h += 2 * e; d += 2 * e
    x1, y1, z1 = x0 + w, y0 + h, z0 + d
    fx, fy, fw, fh = face_rect(part, face)
    nx, ny = (fw, fh)
    res = []
    for j in range(ny):
        for i in range(nx):
            ii = (nx - 1 - i) if mirror else i
            def lerp(a, b, t): return a + (b - a) * t
            u0, u1, v0, v1 = i / nx, (i + 1) / nx, j / ny, (j + 1) / ny
            if face == "front":   # z=z0, u->+x, v->+y
                pts = [(lerp(x0, x1, u), lerp(y0, y1, v), z0) for (u, v) in ((u0, v0), (u1, v0), (u1, v1), (u0, v1))]
            elif face == "back":  # z=z1, u->-x
                pts = [(lerp(x1, x0, u), lerp(y0, y1, v), z1) for (u, v) in ((u0, v0), (u1, v0), (u1, v1), (u0, v1))]
            elif face == "right": # x=x0, u->-z... toward front (-z)
                pts = [(x0, lerp(y0, y1, v), lerp(z1, z0, u)) for (u, v) in ((u0, v0), (u1, v0), (u1, v1), (u0, v1))]
            elif face == "left":  # x=x1, u from front (-z) to back
                pts = [(x1, lerp(y0, y1, v), lerp(z0, z1, u)) for (u, v) in ((u0, v0), (u1, v0), (u1, v1), (u0, v1))]
            elif face == "top":   # y=y0, u->+x, v->front(-z)
                pts = [(lerp(x0, x1, u), y0, lerp(z1, z0, v)) for (u, v) in ((u0, v0), (u1, v0), (u1, v1), (u0, v1))]
            else:                 # bottom
                pts = [(lerp(x0, x1, u), y1, lerp(z0, z1, v)) for (u, v) in ((u0, v0), (u1, v0), (u1, v1), (u0, v1))]
            if mirror:
                # mirror the whole box left/right by swapping texture column; geometry stays
                pass
            res.append((pts, fx + ii, fy + j))
    return res

def render(shape_layers, mats, yaw, size=(300, 400), scale=7.5, shape="standard"):
    l1, l2 = shape_layers
    layers = {1: l1, 2: l2}
    im = Image.new("RGBA", size, (30, 33, 40, 255))
    d = ImageDraw.Draw(im)
    th = math.radians(yaw)
    cx, cy = size[0] / 2, 56

    def proj(p):
        x, y, z = p
        xr = x * math.cos(th) + z * math.sin(th)
        zr = -x * math.sin(th) + z * math.cos(th)
        return (cx + xr * scale, cy + (y + 8) * scale + zr * scale * 0.18, zr)

    quads = []
    # underlying mannequin
    skin = {(-4, -8, -4, 8, 8, 8): (210, 170, 135), (-4, 0, -2, 8, 12, 4): (90, 100, 120),
            (-8, 0, -2, 4, 12, 4): (210, 170, 135), (4, 0, -2, 4, 12, 4): (210, 170, 135),
            (-4, 12, -2, 4, 12, 4): (70, 78, 100), (0, 12, -2, 4, 12, 4): (70, 78, 100)}
    for bx, col in skin.items():
        x0, y0, z0, w, h, dd = bx
        for face in FACE_SHADE:
            fake = (0, 0, 8, 8, 8)
            for pts, _, _ in face_texels(bx, face, False, (0, 0, w, h, dd), 0):
                pp = [proj(p) for p in pts]
                s = FACE_SHADE[face]
                quads.append((sum(p[2] for p in pp) / 4, [(p[0], p[1]) for p in pp], tuple(int(c * s) for c in col)))
    for name, ln, bx, part, mirror, e in boxes(1.0, 0.5):
        layer = layers[ln]
        for face in FACE_SHADE:
            for pts, tx, ty in face_texels(bx, face, mirror, layer.parts[part], e):
                c = colour(layer, tx, ty, mats)
                if not c: continue
                pp = [proj(p) for p in pts]
                s = FACE_SHADE[face]
                quads.append((sum(p[2] for p in pp) / 4, [(p[0], p[1]) for p in pp], tuple(min(255, int(v * s)) for v in c)))
    for ln, part, bx, mirror in extra_boxes(shape):
        layer = layers[ln]
        for face in FACE_SHADE:
            for pts, tx, ty in face_texels(bx, face, mirror, layer.parts[part], 0):
                c = colour(layer, tx, ty, mats)
                if not c: continue
                pp = [proj(p) for p in pts]
                sh = FACE_SHADE[face]
                quads.append((sum(p[2] for p in pp) / 4, [(p[0], p[1]) for p in pp], tuple(min(255, int(v * sh)) for v in c)))
    quads.sort(key=lambda q: -q[0])
    for _, poly, col in quads:
        d.polygon(poly, fill=col + (255,))
    return im

DESC = {
    "standard": ("STANDARD", "Balanced plate. Open-face helm with brow band\nand cheek guards, breastplate with centre clasp,\nshoulder caps, knee plates, toe caps."),
    "heavy": ("HEAVY", "Full plate. Closed helm with visor slit and crest,\nriveted cuirass, large layered pauldrons and\nvambraces, tassets, armoured sabatons."),
    "light": ("LIGHT", "Leather-style. Hood cap with open face, strap\nharness over a tunic, small chest plate, shoulder\ncaps, bracers, knee pads, cuffed boots."),
}

def make_sheet(path):
    shapes = ["standard", "heavy", "light"]
    font = ImageFont.truetype("/usr/share/fonts/TTF/DejaVuSansCondensed.ttf", 18)
    small = ImageFont.truetype("/usr/share/fonts/TTF/DejaVuSansCondensed.ttf", 13)
    rw, rh = 300, 400
    tex_w = 64 * 5
    W = 40 + rw * 2 + 20 + tex_w + 40
    H = 80 + (rh + 40) * 3 + 30
    sheet = Image.new("RGBA", (W, H), (22, 24, 29, 255))
    d = ImageDraw.Draw(sheet)
    d.text((20, 14), "Universal Armor Extended - shape designs (proposal)", font=font, fill=(240, 240, 240))
    lx = 20
    for i, (c, n) in enumerate(((F, "frame: iron"), (P, "padding: leather"), (L, "plating: diamond"), (A, "attachment: gold"))):
        d.rectangle([lx, 46, lx + 14, 60], fill=MATS[c]); d.text((lx + 20, 44), n, font=small, fill=(200, 200, 205)); lx += 170
    for r, s in enumerate(shapes):
        layers = build(s)
        y0 = 80 + r * (rh + 40)
        d.rectangle([20, y0 - 6, W - 20, y0 + rh + 30], outline=(70, 76, 90))
        d.text((30, y0), DESC[s][0], font=font, fill=(255, 220, 120))
        for k, yaw in enumerate((28, 208)):
            sheet.alpha_composite(render(layers, MATS, yaw, (rw, rh), shape=s), (30 + k * rw, y0 + 24))
        tx = 40 + rw * 2 + 10
        d.text((tx, y0), "outer layer (helmet / chest / boots)", font=small, fill=(180, 184, 192))
        sheet.alpha_composite(sheet_image(layers[0], MATS), (tx, y0 + 20))
        d.text((tx, y0 + 20 + 160 + 10), "inner layer (leggings)", font=small, fill=(180, 184, 192))
        sheet.alpha_composite(sheet_image(layers[1], MATS), (tx, y0 + 20 + 160 + 30))
        d.text((tx, y0 + 20 + 160 + 30 + 160 + 10), DESC[s][1], font=small, fill=(200, 204, 212))
    sheet.convert("RGB").save(path)

def export(outdir):
    os.makedirs(outdir, exist_ok=True)
    for s in ("standard", "heavy", "light"):
        for n, layer in zip(("1", "2"), build(s)):
            for c, name in NAMES.items():
                im = Image.new("RGBA", (layer.W, layer.H), (0, 0, 0, 0)); px = im.load()
                for y in range(layer.H):
                    for x in range(layer.W):
                        if layer.comp[y][x] == c:
                            v = int(layer.tone[y][x] * 255); px[x, y] = (v, v, v, 255)
                im.save(f"{outdir}/{s}_{name}_layer_{n}.png")
            # silhouette doubles as the fallback universal layer
            im = Image.new("RGBA", (layer.W, layer.H), (0, 0, 0, 0)); px = im.load()
            for y in range(layer.H):
                for x in range(layer.W):
                    if layer.comp[y][x]:
                        v = int(layer.tone[y][x] * 255); px[x, y] = (v, v, v, 255)
            im.save(f"{outdir}/{s}_universal_layer_{n}.png")

if __name__ == "__main__":
    here = os.path.dirname(os.path.abspath(__file__))
    if sys.argv[1] == "sheet": make_sheet(os.path.join(here, "armor_sheet.png"))
    else: export(sys.argv[2])

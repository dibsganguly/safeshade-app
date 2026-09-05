"""Generate ui/icons/SafeShadeIcons.kt from the SVGs in docs/Icons.

Throwaway: run again when the icon set changes. The SVGs stay the source of
truth; the generated Kotlin is checked in so the build needs no codegen step.
"""
import re, glob, os, sys

SRC = "docs/Icons"
OUT = "app/src/main/java/com/safeshade/ui/icons/SafeShadeIcons.kt"

CAP = {"butt": "Butt", "round": "Round", "square": "Square"}
JOIN = {"miter": "Miter", "round": "Round", "bevel": "Bevel"}


def attr(s, name, default=None):
    m = re.search(r'\b%s="([^"]*)"' % re.escape(name), s)
    return m.group(1) if m else default


def pascal(slug):
    return "".join(p.capitalize() for p in slug.split("-"))


def circle_to_path(cx, cy, r):
    """An SVG circle as two arcs - addPathNodes has no circle primitive."""
    cx, cy, r = float(cx), float(cy), float(r)
    return ("M%g %g a %g %g 0 1 0 %g 0 a %g %g 0 1 0 %g 0"
            % (cx - r, cy, r, r, 2 * r, r, r, -2 * r))


def shapes_of(svg):
    """Every drawable element, with its paint attributes resolved from the root."""
    head = svg[:svg.index(">") + 1]
    root = {
        "fill": attr(head, "fill", "none"),
        "stroke": attr(head, "stroke"),
        "stroke-width": attr(head, "stroke-width"),
        "stroke-linecap": attr(head, "stroke-linecap", "butt"),
        "stroke-linejoin": attr(head, "stroke-linejoin", "miter"),
    }
    out = []
    for m in re.finditer(r"<(path|circle)\b([^>]*)>", svg):
        tag, raw = m.group(1), m.group(2)
        if tag == "path":
            d = attr(raw, "d")
            if not d:
                continue
        else:
            d = circle_to_path(attr(raw, "cx", "0"), attr(raw, "cy", "0"), attr(raw, "r", "0"))
        shape = dict(root)
        for k in list(root):
            v = attr(raw, k)
            if v is not None:
                shape[k] = v
        shape["d"] = " ".join(d.split())
        out.append(shape)
    return out


def kotlin_for(slug, svg):
    head = svg[:svg.index(">") + 1]
    vb = (attr(head, "viewBox") or "0 0 24 24").split()
    vw, vh = float(vb[2]), float(vb[3])

    lines = []
    for sh in shapes_of(svg):
        data = '            pathData = addPathNodes(\n                "%s"\n            ),' % sh["d"]
        if sh["fill"] not in (None, "none"):
            lines.append("        addPath(\n%s\n            fill = SolidColor(Color.Black)\n        )" % data)
        else:
            lines.append(
                "        addPath(\n%s\n"
                "            stroke = SolidColor(Color.Black),\n"
                "            strokeLineWidth = %sf,\n"
                "            strokeLineCap = StrokeCap.%s,\n"
                "            strokeLineJoin = StrokeJoin.%s\n"
                "        )" % (data, float(sh["stroke-width"] or "1.5"),
                               CAP[sh["stroke-linecap"]], JOIN[sh["stroke-linejoin"]]))

    body = "\n".join("    " + ln for ln in "\n".join(lines).split("\n"))
    name = pascal(slug)
    return ('\n'
            '    /** `%s.svg` */\n'
            '    val %s: ImageVector by lazy {\n'
            '        ImageVector.Builder(\n'
            '            name = "%s",\n'
            '            defaultWidth = 24.dp,\n'
            '            defaultHeight = 24.dp,\n'
            '            viewportWidth = %gf,\n'
            '            viewportHeight = %gf\n'
            '        ).apply {\n'
            '%s\n'
            '        }.build()\n'
            '    }\n' % (slug, name, name, vw, vh, body))


HEADER = '''package com.safeshade.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The app's own icons.
 *
 * GENERATED from the SVGs in `docs/Icons` - do not hand-edit. Run the generator
 * again when the set changes; those SVGs are the source of truth and stay in
 * the repo.
 *
 * **Why Kotlin `ImageVector`s and not vector XML drawables.** The obvious route
 * - drop the SVGs in `res/drawable` and load them with
 * `ImageVector.vectorResource` - cannot work here: that function is
 * `@Composable`, and this app carries icons as plain data on `BoardWay`, built
 * outside composition. Generated `ImageVector`s are ordinary values, so every
 * existing `icon: ImageVector?` parameter accepts one with no change at any
 * call site.
 *
 * Strokes are declared `Color.Black`, which is never seen: `Icon()` applies a
 * tint `ColorFilter` over the whole painter, so a glyph takes `inkMuted` or its
 * row's accent exactly as a Material icon does.
 *
 * Members rather than extension properties. Material declares its icons as
 * extensions so that a very large set stays splittable; 35 do not need that,
 * and extensions would cost a separate import line per icon at every call site.
 * `by lazy` still means none is built until it is first drawn.
 *
 * These are **feature** icons. Utility chrome - back arrows, chevrons, close,
 * add, share - stays on Material, because the set contains no equivalent.
 */
object SafeShadeIcons {'''


def main():
    files = sorted(glob.glob(os.path.join(SRC, "*.svg")))
    if not files:
        sys.exit("no SVGs found in %s" % SRC)
    parts = [HEADER]
    for f in files:
        slug = os.path.splitext(os.path.basename(f))[0]
        parts.append(kotlin_for(slug, open(f, encoding="utf-8").read()))
    parts.append("}\n")
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8", newline="") as fh:
        fh.write("".join(parts))
    print("wrote %s from %d icons" % (OUT, len(files)))


main()

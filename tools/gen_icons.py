"""Generate ui/icons/SafeShadeIcons.kt from the SVGs in docs/Icons.

Run again when the icon set changes. The SVGs stay the source of truth; the
generated Kotlin is checked in so the build needs no codegen step.

    python tools/gen_icons.py        # from the repo root

Three things this has to get right, each of which it once got wrong:

* **Kotlin identifiers cannot start with a digit.** `3-g-signal` naively
  pascal-cased is `3GSignal`, which is a syntax error, not a warning. The
  leading numeric run moves to the end instead: `Signal3G`.
* **Every drawable element must be handled.** The element scan used to cover
  `<path>` and `<circle>` only, so the `<ellipse>` in `internet` was dropped
  silently and that glyph rendered as a circle with an equator and no
  meridian. Anything unrecognised now fails loudly rather than vanishing.
* **A glyph whose artwork fills more of its viewBox than its neighbours reads
  as oversized** even at an identical declared size. `OPTICAL` corrects those
  by wrapping the paths in a scaled group, so the SVGs stay untouched and a
  redrop of the source file does not silently undo the correction.
"""
import re, glob, os, sys

SRC = "docs/Icons"
OUT = "app/src/main/java/com/safeshade/ui/icons/SafeShadeIcons.kt"

# Hugeicons exports carry their style in the filename. It is the same style for
# every icon in this set, so it says nothing at a call site and only makes the
# name longer: `SafeShadeIcons.Tick02StrokeRounded` reads worse than `Tick02`.
STYLE_SUFFIXES = ("-stroke-rounded", "-stroke-sharp", "-stroke-standard")

# Optical size corrections, keyed by slug, as a scale factor about the centre.
#
# These are not arbitrary taste. A 24-unit viewBox says nothing about how much
# of itself the artwork occupies, and this set is not uniform: most glyphs span
# about 18 of the 24 units, `check-in` spans 20. Drawn at the same declared
# 20dp in a `Way` row it is visibly the largest thing in the column, which is
# exactly the complaint that produced this table.
OPTICAL = {
    # 18/20 - brings it level with navbar-board, gps and the rest of the rows.
    "check-in": 0.90,
}

CAP = {"butt": "Butt", "round": "Round", "square": "Square"}
JOIN = {"miter": "Miter", "round": "Round", "bevel": "Bevel"}

# Everything that can carry paint. Anything outside this set is a hard error:
# a silently skipped element is a glyph that renders wrong with no way to tell.
DRAWABLE = ("path", "circle", "ellipse", "rect", "line", "polyline", "polygon")


def attr(s, name, default=None):
    m = re.search(r'\b%s="([^"]*)"' % re.escape(name), s)
    return m.group(1) if m else default


def slug_of(path):
    slug = os.path.splitext(os.path.basename(path))[0]
    for suffix in STYLE_SUFFIXES:
        if slug.endswith(suffix):
            return slug[: -len(suffix)]
    return slug


def pascal(slug):
    name = "".join(p[:1].upper() + p[1:] for p in slug.split("-"))
    # A leading digit is not a legal Kotlin identifier. Move the numeric run
    # (plus a single trailing letter, so "3G" travels as a unit) to the end.
    m = re.match(r"^(\d+[A-Za-z]?)(.+)$", name)
    return m.group(2) + m.group(1) if m else name


def ellipse_to_path(cx, cy, rx, ry):
    """An SVG ellipse as two arcs - addPathNodes has no ellipse primitive."""
    cx, cy, rx, ry = float(cx), float(cy), float(rx), float(ry)
    return ("M%g %g a %g %g 0 1 0 %g 0 a %g %g 0 1 0 %g 0"
            % (cx - rx, cy, rx, ry, 2 * rx, rx, ry, -2 * rx))


def rect_to_path(x, y, w, h):
    x, y, w, h = float(x), float(y), float(w), float(h)
    return "M%g %g h%g v%g h%g Z" % (x, y, w, h, -w)


def points_to_path(points, close):
    nums = [float(n) for n in re.findall(r"-?\d*\.?\d+(?:e-?\d+)?", points)]
    pairs = list(zip(nums[0::2], nums[1::2]))
    if not pairs:
        return ""
    d = "M%g %g " % pairs[0] + " ".join("L%g %g" % p for p in pairs[1:])
    return d + " Z" if close else d


def path_data(tag, raw):
    """The `d` for one drawable element, whatever kind of element it is."""
    if tag == "path":
        return attr(raw, "d")
    if tag == "circle":
        r = attr(raw, "r", "0")
        return ellipse_to_path(attr(raw, "cx", "0"), attr(raw, "cy", "0"), r, r)
    if tag == "ellipse":
        return ellipse_to_path(attr(raw, "cx", "0"), attr(raw, "cy", "0"),
                               attr(raw, "rx", "0"), attr(raw, "ry", "0"))
    if tag == "rect":
        return rect_to_path(attr(raw, "x", "0"), attr(raw, "y", "0"),
                            attr(raw, "width", "0"), attr(raw, "height", "0"))
    if tag == "line":
        return "M%s %s L%s %s" % (attr(raw, "x1", "0"), attr(raw, "y1", "0"),
                                  attr(raw, "x2", "0"), attr(raw, "y2", "0"))
    if tag in ("polyline", "polygon"):
        return points_to_path(attr(raw, "points", ""), close=(tag == "polygon"))
    raise AssertionError("unhandled element <%s>" % tag)


def shapes_of(slug, svg):
    """Every drawable element, with its paint attributes resolved from the root."""
    head = svg[: svg.index(">") + 1]
    root = {
        "fill": attr(head, "fill", "none"),
        "stroke": attr(head, "stroke"),
        "stroke-width": attr(head, "stroke-width"),
        "stroke-linecap": attr(head, "stroke-linecap", "butt"),
        "stroke-linejoin": attr(head, "stroke-linejoin", "miter"),
    }
    # Loud, not silent: an element this generator does not know how to convert
    # would otherwise leave a partly-drawn glyph and no error anywhere.
    body = svg[svg.index(">") + 1:]
    for m in re.finditer(r"<([a-zA-Z][\w-]*)", body):
        tag = m.group(1)
        if tag in DRAWABLE or tag in ("svg", "desc", "title", "defs", "g", "style", "metadata"):
            continue
        sys.exit("%s: unhandled element <%s> - it would be dropped silently" % (slug, tag))
    if re.search(r"\btransform=", svg):
        sys.exit("%s: has a transform= this generator does not apply" % slug)

    out = []
    for m in re.finditer(r"<(%s)\b([^>]*)>" % "|".join(DRAWABLE), svg):
        tag, raw = m.group(1), m.group(2)
        d = path_data(tag, raw)
        if not d:
            continue
        shape = dict(root)
        for k in list(root):
            v = attr(raw, k)
            if v is not None:
                shape[k] = v
        shape["d"] = " ".join(d.split())
        out.append(shape)
    if not out:
        sys.exit("%s: produced no drawable shapes" % slug)
    return out


def kotlin_for(slug, svg):
    head = svg[: svg.index(">") + 1]
    vb = (attr(head, "viewBox") or "0 0 24 24").split()
    vw, vh = float(vb[2]), float(vb[3])

    lines = []
    for sh in shapes_of(slug, svg):
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

    body = "\n".join(lines)
    scale = OPTICAL.get(slug)
    if scale is not None:
        # Scale about the viewBox centre, so the glyph shrinks in place rather
        # than towards the origin. A group is the only way to express this -
        # ImageVector.Builder's viewport has a size but no origin offset, so a
        # negative-origin viewBox cannot be transcribed directly.
        inset_x, inset_y = vw * (1 - scale) / 2, vh * (1 - scale) / 2
        # addGroup/clearGroup rather than the `group {}` helper: those two are
        # members of the builder, so a corrected icon costs the generated file
        # no extra import that all 101 un-corrected ones would carry unused.
        body = ('        // Optically matched to its neighbours - see OPTICAL in tools/gen_icons.py.\n'
                '        addGroup(\n'
                '            scaleX = %gf,\n'
                '            scaleY = %gf,\n'
                '            translationX = %gf,\n'
                '            translationY = %gf\n'
                '        )\n%s\n        clearGroup()' % (scale, scale, inset_x, inset_y, body))

    body = "\n".join("    " + ln for ln in body.split("\n"))
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
 * GENERATED from the SVGs in `docs/Icons` - do not hand-edit. Run
 * `python tools/gen_icons.py` from the repo root when the set changes; those
 * SVGs are the source of truth and stay in the repo.
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
 * extensions so that a very large set stays splittable; this one does not need
 * that, and extensions would cost a separate import line per icon at every call
 * site. `by lazy` still means none is built until it is first drawn.
 *
 * **Chrome is in scope now.** An earlier revision of this file said utility
 * chrome - back arrows, chevrons, close, tick, info - stays on Material,
 * because the set then had no equivalent for any of them. It does now, and the
 * back chevron alone is the most repeated glyph in the app, so the exception is
 * retired. What is still on Material is what this set genuinely has no answer
 * for: the mode personas and device shapes in `ui/board/ModeVisuals.kt`, and a
 * handful of one-off actions. Reusing one glyph across eight distinct modes
 * would be worse than the mixture it replaced.
 *
 * [All] is the whole set in declaration order, which is what the kit gallery
 * renders. It is generated too, so a new SVG appears there without anybody
 * remembering to add it.
 */
object SafeShadeIcons {'''


def main():
    files = sorted(glob.glob(os.path.join(SRC, "*.svg")))
    if not files:
        sys.exit("no SVGs found in %s" % SRC)

    seen = {}
    for f in files:
        slug = slug_of(f)
        name = pascal(slug)
        if not re.match(r"^[A-Za-z][A-Za-z0-9_]*$", name):
            sys.exit("%s: %r is not a legal Kotlin identifier" % (slug, name))
        if name in seen:
            sys.exit("name collision: %s and %s both give %s" % (seen[name], slug, name))
        seen[name] = slug

    parts = [HEADER]
    entries = []
    for f in files:
        slug = slug_of(f)
        parts.append(kotlin_for(slug, open(f, encoding="utf-8").read()))
        entries.append((slug, pascal(slug)))

    parts.append('\n    /** Every icon in the set, for the kit gallery. */\n'
                 '    val All: List<Pair<String, ImageVector>> = listOf(\n'
                 + "".join('        "%s" to %s,\n' % (slug, name) for slug, name in entries)
                 + '    )\n')
    parts.append("}\n")

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8", newline="") as fh:
        fh.write("".join(parts))
    print("wrote %s from %d icons" % (OUT, len(files)))


main()

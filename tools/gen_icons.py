"""Generate ui/icons/SafeShadeIcons.kt from the SVGs in docs/Icons.

Run again when the icon set changes. The SVGs stay the source of truth; the
generated Kotlin is checked in so the build needs no codegen step.

    python tools/gen_icons.py        # from the repo root

Three things this has to get right, each of which it once got wrong, and two more
that the icon drop this file was last run against would have caught it on:

* **Kotlin identifiers cannot start with a digit.** `3-g-signal` naively
  pascal-cased is `3GSignal`, which is a syntax error, not a warning. The
  leading numeric run moves to the end instead: `Signal3G`.
* **Every drawable element must be handled.** The element scan used to cover
  `<path>` and `<circle>` only, so the `<ellipse>` in `internet` was dropped
  silently and that glyph rendered as a circle with an equator and no
  meridian. Anything unrecognised now fails loudly rather than vanishing.
* **A glyph whose artwork fills more of its viewBox than its neighbours reads
  as oversized** even at an identical declared size, and one that runs to the
  top of its box sits above the word it labels. `OPTICAL` corrects both by
  wrapping the paths in a scaled and translated group, so the SVGs stay
  untouched and a redrop of the source file does not silently undo it.
* **The root element is not always the first tag.** The root used to be read as
  everything up to the first `>`, which is the `<?xml ?>` prolog in any file
  that carries one. No icon in the set before this drop had a prolog; the two
  brand marks do, and both would have had their `viewBox` and their `fill` read
  as absent and been drawn into a 24-unit box they are not drawn in. The root
  is now located as the `<svg ...>` element itself.
* **Paint has to resolve the way SVG says it does.** The old rule was "fill if
  the element has one, otherwise stroke it at 1.5", with the root defaulting to
  `fill="none"`. Every file in the set happens to state enough paint for that
  to land on the right answer, so it never produced a wrong glyph - but a shape
  that states no paint at all would have been emitted as a hairline stroke in
  whatever units its viewBox uses, which in a 512-unit box is 1/340th of the
  glyph's width and invisible at every size the app draws it. Fill now defaults
  to black and stroke to none, as the specification says; a shape can carry
  both; and one that ends up with neither is a hard error rather than a blank.
"""
import re, glob, os, sys

SRC = "docs/Icons"
OUT = "app/src/main/java/com/safeshade/ui/icons/SafeShadeIcons.kt"

# Hugeicons exports carry their style in the filename. It is the same style for
# every icon in this set, so it says nothing at a call site and only makes the
# name longer: `SafeShadeIcons.Tick02StrokeRounded` reads worse than `Tick02`.
STYLE_SUFFIXES = ("-stroke-rounded", "-stroke-sharp", "-stroke-standard")

# Every icon is emitted into a 24-unit viewport whatever box it was drawn in,
# so a call site, and the OPTICAL table below, can speak one set of units.
BOX = 24.0

# A stroked path that states no width. SVG's own default is 1, but nothing in
# this set means it: the Hugeicons exports all declare 1.5 and the one file that
# omits the attribute (`google-logo-outline`) is drawn to sit beside them. The
# set's weight is the right fallback here, not the specification's.
DEFAULT_STROKE_WIDTH = 1.5

# Optical size corrections, keyed by slug, as a scale factor about the centre.
#
# These are not arbitrary taste. A 24-unit viewBox says nothing about how much
# of itself the artwork occupies, and this set is not uniform: most glyphs span
# about 18 of the 24 units, `check-in` spans 20. Drawn at the same declared
# 20dp in a `Way` row it is visibly the largest thing in the column, which is
# exactly the complaint that produced this table.
# A value is either a bare scale, or a dict of `scale`, `dx` and `dy` in
# viewport units (positive dy moves the glyph down).
#
# `dy` exists because a glyph can be the right size and still sit wrong. `Way`
# top-aligns its icon and lifts it 2dp so the glyph's cap lands level with the
# title's, which is correct for artwork inset from the top of its box and wrong
# for artwork that runs right up to it - those sit visibly above the word they
# label. Measured on device: `call-after-a-fall` rendered 13px higher than
# `fall-detection` and `text-as-well` in the same bank, at the same declared
# size, on the same screen.
OPTICAL = {
    # 18/20 - brings it level with navbar-board, gps and the rest of the rows.
    "check-in": 0.90,
    # Spans 21.6 x 21.1 of its 24-unit box against a set norm of about 18, so
    # it is the largest artwork in the whole drop, and its ink centre is low
    # and right of the box centre rather than on it.
    "adaptive-mode": {"scale": 0.86, "dx": -0.33, "dy": -0.57},
    # Runs to the top of its box, and sat 13px above its neighbours in the same
    # bank at the same declared size. `daily-reminder` was in here too and has
    # been taken back out: it looked identical on screen but the cause was
    # `Way` anchoring every icon against the row instead of against the title's
    # first line, which is fixed in `Way.kt` and was never this glyph's fault.
    "call-after-a-fall": {"dy": 2.5},
    # The three solid-silhouette imports. Normalisation fits their box to 24
    # units, but their artwork then runs to the very edge of it: measured with
    # `getBBox` in a browser, the set's own glyphs span 21.5 units at the median
    # (19.5 at the tenth percentile) including their stroke, while these three
    # span 23.5, 23.9 and 24.0. Each scale below brings the glyph a little
    # inside that median rather than exactly onto it, because a solid shape
    # carries more ink per unit than an outline of the same size and reads
    # larger for it.
    #
    # An earlier pass took them to 0.80 by eye and overshot: on the Safety
    # screen `fall-detection` then sat visibly smaller than the phone and speech
    # bubble either side of it, which is the same complaint as before with the
    # sign flipped.
    "fall-detection": 0.88,
    "apple-logo": 0.86,
    "google-logo": 0.86,
}


def optical(slug):
    """(scale, dx, dy) for a slug, or None when it needs no correction."""
    v = OPTICAL.get(slug)
    if v is None:
        return None
    if isinstance(v, dict):
        return v.get("scale", 1.0), v.get("dx", 0.0), v.get("dy", 0.0)
    return v, 0.0, 0.0

CAP = {"butt": "Butt", "round": "Round", "square": "Square"}
JOIN = {"miter": "Miter", "round": "Round", "bevel": "Bevel"}

# Everything that can carry paint. Anything outside this set is a hard error:
# a silently skipped element is a glyph that renders wrong with no way to tell.
DRAWABLE = ("path", "circle", "ellipse", "rect", "line", "polyline", "polygon")

# Structural or descriptive elements that carry no ink of their own.
IGNORABLE = ("svg", "g", "desc", "title", "defs", "metadata", "style")

# Painting attributes that inherit down the tree. `transform` inherits too but
# composes rather than overrides, so it is tracked separately.
INHERITED = ("fill", "stroke", "stroke-width", "stroke-linecap",
             "stroke-linejoin", "stroke-miterlimit", "fill-rule")

# SVG's initial values for those, which is what a file that states nothing gets.
INITIAL = {"fill": "black", "stroke": "none", "stroke-width": None,
           "stroke-linecap": "butt", "stroke-linejoin": "miter",
           "stroke-miterlimit": "4", "fill-rule": "nonzero"}

NUM = r"-?\d*\.?\d+(?:[eE]-?\d+)?"


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
    nums = [float(n) for n in re.findall(NUM, points)]
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


class Group(object):
    """One `addGroup` worth of transform, in Compose's parameter names."""

    def __init__(self, rotate=0.0, pivot_x=0.0, pivot_y=0.0,
                 scale_x=1.0, scale_y=1.0, tx=0.0, ty=0.0):
        self.rotate, self.pivot_x, self.pivot_y = rotate, pivot_x, pivot_y
        self.scale_x, self.scale_y, self.tx, self.ty = scale_x, scale_y, tx, ty

    def args(self):
        pairs = [("rotate", self.rotate, 0.0), ("pivotX", self.pivot_x, 0.0),
                 ("pivotY", self.pivot_y, 0.0), ("scaleX", self.scale_x, 1.0),
                 ("scaleY", self.scale_y, 1.0), ("translationX", self.tx, 0.0),
                 ("translationY", self.ty, 0.0)]
        return [(k, v) for k, v, default in pairs if v != default]


def parse_transform(slug, spec):
    """An SVG `transform` as the list of groups that reproduces it.

    Compose builds a group's matrix as `T(tx+px, ty+py) . R . S . T(-px, -py)`,
    which is exactly one translate, one rotation about a pivot and one scale -
    less than SVG allows in a single attribute. An SVG transform list applies
    left to right outermost first, so each function becomes its own group and
    they nest in source order. Anything a group cannot express - a matrix with
    shear or rotation in it, `skewX`, `skewY` - is a hard error: quietly
    dropping it would leave the glyph mirrored or displaced with nothing said.
    """
    groups = []
    pos = 0
    for m in re.finditer(r"([a-zA-Z]+)\s*\(([^)]*)\)\s*,?\s*", spec):
        if spec[pos:m.start()].strip():
            sys.exit("%s: cannot parse transform %r" % (slug, spec))
        pos = m.end()
        fn = m.group(1)
        a = [float(n) for n in re.findall(NUM, m.group(2))]
        if fn == "translate" and len(a) in (1, 2):
            groups.append(Group(tx=a[0], ty=a[1] if len(a) > 1 else 0.0))
        elif fn == "scale" and len(a) in (1, 2):
            groups.append(Group(scale_x=a[0], scale_y=a[1] if len(a) > 1 else a[0]))
        elif fn == "rotate" and len(a) in (1, 3):
            groups.append(Group(rotate=a[0],
                                pivot_x=a[1] if len(a) == 3 else 0.0,
                                pivot_y=a[2] if len(a) == 3 else 0.0))
        elif fn == "matrix" and len(a) == 6:
            av, b, c, d, e, f = a
            if b or c:
                sys.exit("%s: transform matrix(%s) has shear or rotation, which "
                         "an ImageVector group cannot express" % (slug, m.group(2)))
            groups.append(Group(scale_x=av, scale_y=d, tx=e, ty=f))
        else:
            sys.exit("%s: unsupported transform %s(%s)" % (slug, fn, m.group(2)))
    if spec[pos:].strip() or not groups:
        sys.exit("%s: cannot parse transform %r" % (slug, spec))
    return groups


def root_of(slug, svg):
    """The `<svg ...>` element - which is not necessarily the first tag.

    A file with an `<?xml ?>` prolog or a leading comment puts a `>` before the
    root's, so slicing to the first one reads the prolog's attributes instead
    of the root's and silently loses the viewBox and the paint.
    """
    m = re.search(r"<svg\b[^>]*>", svg, re.S)
    if not m:
        sys.exit("%s: no <svg> root element" % slug)
    return m.group(0)


def shapes_of(slug, svg):
    """Every drawable element, with paint and transform inherited down the tree.

    Returned in document order as (paint dict, `d`, [Group]) where the groups
    run outermost first and are the accumulated `transform`s of the element and
    of every `<g>` enclosing it.
    """
    root = root_of(slug, svg)
    body = svg[svg.index(root) + len(root):]

    # Loud, not silent: an element this generator does not know how to convert
    # would otherwise leave a partly-drawn glyph and no error anywhere.
    for m in re.finditer(r"<([a-zA-Z][\w-]*)", body):
        if m.group(1) not in DRAWABLE and m.group(1) not in IGNORABLE:
            sys.exit("%s: unhandled element <%s> - it would be dropped silently"
                     % (slug, m.group(1)))

    def inherit(base, raw):
        out = dict(base)
        for k in INHERITED:
            v = attr(raw, k)
            if v is not None:
                out[k] = v
        return out

    paint = [inherit(INITIAL, root)]
    trans = [parse_transform(slug, attr(root, "transform")) if attr(root, "transform") else []]
    out = []
    # `<g>` is the only element here that has children, so a stack of one kind
    # of frame is enough; a self-closing `<g/>` opens and closes in one tag.
    for m in re.finditer(r"<(/?)([a-zA-Z][\w-]*)\b([^>]*?)(/?)>", body):
        closing, tag, raw, selfclose = m.groups()
        if tag == "g":
            if closing:
                paint.pop()
                trans.pop()
            elif not selfclose:
                paint.append(inherit(paint[-1], raw))
                t = attr(raw, "transform")
                trans.append(trans[-1] + (parse_transform(slug, t) if t else []))
            continue
        if closing or tag not in DRAWABLE:
            continue
        d = path_data(tag, raw)
        if not d:
            continue
        shape = inherit(paint[-1], raw)
        if shape["fill"] == "none" and shape["stroke"] == "none":
            sys.exit("%s: <%s> paints neither a fill nor a stroke" % (slug, tag))
        shape["d"] = " ".join(d.split())
        t = attr(raw, "transform")
        out.append((shape, trans[-1] + (parse_transform(slug, t) if t else [])))
    if not out:
        sys.exit("%s: produced no drawable shapes" % slug)
    return out


def indent(text, depth):
    pad = "    " * depth
    return "\n".join(pad + ln if ln else ln for ln in text.split("\n"))


def wrap(body, group, note=None):
    """`body` inside one `addGroup`/`clearGroup` pair."""
    args = ",\n".join("    %s = %gf" % (k, v) for k, v in group.args())
    head = ("// %s\n" % note if note else "") + "addGroup(\n%s\n)" % args
    return "%s\n%s\nclearGroup()" % (head, body)


def kotlin_for(slug, svg):
    vb = (attr(root_of(slug, svg), "viewBox") or "0 0 24 24").split()
    if len(vb) != 4:
        sys.exit("%s: viewBox %r is not four numbers" % (slug, " ".join(vb)))
    min_x, min_y, vw, vh = [float(n) for n in vb]

    needs = set()
    blocks = []
    for sh, groups in shapes_of(slug, svg):
        args = ['pathData = addPathNodes(\n    "%s"\n)' % sh["d"]]
        if sh["fill-rule"] in ("evenodd", "evenOdd"):
            args.append("pathFillType = PathFillType.EvenOdd")
            needs.add("PathFillType")
        if sh["fill"] != "none":
            args.append("fill = SolidColor(Color.Black)")
        if sh["stroke"] != "none":
            width = float(sh["stroke-width"] or DEFAULT_STROKE_WIDTH)
            args.append("stroke = SolidColor(Color.Black)")
            args.append("strokeLineWidth = %sf" % width)
            args.append("strokeLineCap = StrokeCap.%s" % CAP[sh["stroke-linecap"]])
            args.append("strokeLineJoin = StrokeJoin.%s" % JOIN[sh["stroke-linejoin"]])
            if float(sh["stroke-miterlimit"]) != 4.0:
                args.append("strokeLineMiter = %sf" % float(sh["stroke-miterlimit"]))
        block = "addPath(\n%s\n)" % indent(",\n".join(args), 1)
        for g in reversed(groups):
            block = wrap(indent(block, 1), g)
        blocks.append(block)

    body = "\n".join(blocks)

    # Normalise whatever box the artwork was drawn in onto the shared 24-unit
    # viewport, fitting the longer side and centring the shorter one. Without
    # this a 512-unit import would need its own viewport, and then every
    # OPTICAL entry and every stroke width would be in different units per
    # glyph. Skipped when the file is already drawn in the target box, so the
    # 223 icons that are stay byte-for-byte what they were.
    if (min_x, min_y, vw, vh) != (0.0, 0.0, BOX, BOX):
        s = BOX / max(vw, vh)
        body = wrap(indent(body, 1),
                    Group(scale_x=s, scale_y=s,
                          tx=(BOX - s * vw) / 2 - s * min_x,
                          ty=(BOX - s * vh) / 2 - s * min_y),
                    "Drawn in a %g x %g box; fitted to the shared %g-unit viewport."
                    % (vw, vh, BOX))

    correction = optical(slug)
    if correction is not None:
        scale, dx, dy = correction
        # Scale about the viewport centre, so the glyph shrinks in place rather
        # than towards the origin. A group is the only way to express this -
        # ImageVector.Builder's viewport has a size but no origin offset, so a
        # negative-origin viewBox cannot be transcribed directly.
        #
        # The nudge is added to that inset and is pre-scale, so it is stated in
        # the same viewport units the artwork ends up in whether or not the
        # glyph is also being resized. Outside the normalisation group above,
        # so an imported glyph and a native one take the same numbers.
        #
        # addGroup/clearGroup rather than the `group {}` helper: those two are
        # members of the builder, so a corrected icon costs the generated file
        # no extra import that the un-corrected ones would carry unused.
        body = wrap(indent(body, 1),
                    Group(scale_x=scale, scale_y=scale,
                          tx=BOX * (1 - scale) / 2 + dx * scale,
                          ty=BOX * (1 - scale) / 2 + dy * scale),
                    "Optically matched to its neighbours - see OPTICAL in tools/gen_icons.py.")

    return needs, ('\n'
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
            '    }\n' % (slug, pascal(slug), pascal(slug), BOX, BOX,
                         indent(body, 3)))


HEADER = '''package com.safeshade.ui.icons

import androidx.compose.ui.graphics.Color
%simport androidx.compose.ui.graphics.SolidColor
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
 * row's accent exactly as a Material icon does. The one consequence worth
 * knowing is that a glyph drawn in brand colours - `GoogleLogo` - arrives
 * monochrome like every other, because the tint covers the whole painter.
 *
 * Members rather than extension properties. Material declares its icons as
 * extensions so that a very large set stays splittable; this one does not need
 * that, and extensions would cost a separate import line per icon at every call
 * site. `by lazy` still means none is built until it is first drawn.
 *
 * **The whole app is on this set.** Earlier revisions of this file carved out
 * exceptions - first utility chrome, then the mode personas and device shapes
 * in `ui/board/ModeVisuals.kt` and a handful of one-off actions - because the
 * set had no equivalent for them. It does now, down to a walking figure, a
 * bicycle, a paw and a pair of brand marks, so the exceptions are retired and
 * no screen draws a Material icon.
 *
 * Every icon is emitted into a 24-unit viewport whatever box its SVG was drawn
 * in, so a call site can size any two of them against each other and get what
 * it asked for.
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

    unknown = sorted(set(OPTICAL) - set(seen.values()))
    if unknown:
        sys.exit("OPTICAL names icons that are not in %s: %s" % (SRC, ", ".join(unknown)))

    needs = set()
    parts = [None]
    entries = []
    for f in files:
        slug = slug_of(f)
        n, kt = kotlin_for(slug, open(f, encoding="utf-8").read())
        needs |= n
        parts.append(kt)
        entries.append((slug, pascal(slug)))

    # Only the imports the set actually needs: an unused one is a build warning
    # on a file nobody is allowed to hand-edit away.
    extra = "import androidx.compose.ui.graphics.PathFillType\n" if "PathFillType" in needs else ""
    parts[0] = HEADER % extra

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

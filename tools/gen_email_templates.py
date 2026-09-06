#!/usr/bin/env python3
"""Generates the two things that must not be produced by hand.

1.  ``supabase/functions/_shared/email/templates.ts`` -- every ``.html`` file in
    that directory, verbatim, as a TypeScript string constant.

    ``composeEmail`` used to read those files at runtime with
    ``Deno.readTextFile`` and fall back to unbranded HTML if the read failed.
    Whether the ``.html`` files survive a deploy was never proven -- the MCP
    deploy path uploads only the files it is handed, and the CLI path needs a
    ``config.toml`` entry that is deliberately not committed.  A branded email
    is a product requirement, so the templates are compiled into the module
    graph instead: if they are missing, the function does not build, which is a
    failure somebody sees.  There is no fallback any more because there is
    nothing left to fall back from.

2.  ``supabase/auth-templates/*.html`` -- the sign-in emails Supabase Auth sends
    itself, which cannot go through ``resend.ts`` at all.  They are whole
    documents (layout with the body inlined), with every ``{{name}}``
    placeholder resolved: the emblem data URI, the accent, the literal copy,
    and Supabase's own Go-template variables where a value is Supabase's to
    supply.

    Two traps this exists to route around.  First, the placeholder syntaxes look
    alike: ``render.ts`` uses ``{{code}}`` and Go uses ``{{ .Token }}``, and a
    file pasted unchanged into the dashboard mails real people the literal text
    ``{{code}}`` -- or fails to parse at all, because Go reads ``{{code}}`` as a
    call to a function it does not have.  Second, the source files' own HTML
    comments *talk about* ``{{code}}`` and ``{{action_url}}`` in prose, so the
    comments are stripped before substitution and the output is then scanned for
    any surviving ``{{`` that is not a Supabase ``{{ .X }}`` form.  That scan is
    a hard gate: the generator exits non-zero rather than write a file that
    would send braces to a person.

Run from the repo root::

    python tools/gen_email_templates.py

It writes with LF endings on every platform and prints what it wrote.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
EMAIL_DIR = REPO / "supabase" / "functions" / "_shared" / "email"
EMBLEM_TS = EMAIL_DIR / "emblem.ts"
TEMPLATES_TS = EMAIL_DIR / "templates.ts"
AUTH_DIR = REPO / "supabase" / "auth-templates"

ACCENT_TEAL = "#6FD3CC"

# The layout is the frame; everything else is a body that sits in its content
# slot.  Kept explicit rather than inferred so that adding a body file cannot
# silently turn it into a second layout.
LAYOUT_NAME = "layout"

GENERATED_HEADER = """\
// ---------------------------------------------------------------------------
// GENERATED FILE -- DO NOT EDIT.
//
// The .html files in this directory are the source of truth.  Regenerate with:
//
//     python tools/gen_email_templates.py
//
// They live here as compiled-in string constants rather than being read from
// disk at runtime, because a deploy that drops a file must fail loudly at build
// time instead of quietly sending unbranded mail about somebody having fallen
// over.  See tools/gen_email_templates.py for the whole argument.
// ---------------------------------------------------------------------------
"""


def read_text(path: Path) -> str:
    """Reads a file and normalises to LF, whatever the working copy has."""
    return path.read_text(encoding="utf-8").replace("\r\n", "\n").replace("\r", "\n")


def write_text(path: Path, text: str) -> None:
    """Writes with LF endings on every platform."""
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as handle:
        handle.write(text)


def ts_template_literal(text: str) -> str:
    """Escapes [text] for a TypeScript backtick template literal.

    Order matters and is the whole of the correctness here: the backslash pass
    must run first, or it would double the backslashes the later passes insert.
    A single stray backslash in an HTML file would otherwise change the meaning
    of whatever character followed it.
    """
    escaped = text.replace("\\", "\\\\")
    escaped = escaped.replace("`", "\\`")
    escaped = escaped.replace("${", "\\${")
    return "`" + escaped + "`"


def emblem_data_uri() -> str:
    """Pulls EMBLEM_DATA_URI out of emblem.ts without importing TypeScript."""
    source = read_text(EMBLEM_TS)
    match = re.search(
        r'export\s+const\s+EMBLEM_DATA_URI\s*=\s*"([^"]+)"\s*;', source
    )
    if not match:
        raise SystemExit(
            "emblem.ts no longer holds EMBLEM_DATA_URI as one double-quoted "
            "string literal; gen_email_templates.py cannot read it."
        )
    return match.group(1)


def body_names() -> list[str]:
    return sorted(
        p.stem for p in EMAIL_DIR.glob("*.html") if p.stem != LAYOUT_NAME
    )


# ---------------------------------------------------------------------------
# 1. templates.ts
# ---------------------------------------------------------------------------


def generate_templates_ts() -> Path:
    bodies = body_names()
    lines = [GENERATED_HEADER, ""]
    lines.append("/** The shared frame. Bodies render into its `{{{content}}}` slot. */")
    lines.append(
        "export const LAYOUT_HTML: string = "
        + ts_template_literal(strip_comments(read_text(EMAIL_DIR / f"{LAYOUT_NAME}.html")))
        + ";"
    )
    lines.append("")
    lines.append("/** Every body template, keyed by its file name without the extension. */")
    lines.append("export const BODY_TEMPLATES = {")
    for name in bodies:
        key = name if re.fullmatch(r"[A-Za-z_$][\w$]*", name) else f'"{name}"'
        lines.append(
            f"  {key}: "
            + ts_template_literal(strip_comments(read_text(EMAIL_DIR / f"{name}.html")))
            + ","
        )
    lines.append("} as const;")
    lines.append("")
    lines.append("/** The names {@link BODY_TEMPLATES} accepts. */")
    lines.append("export type BodyTemplateName = keyof typeof BODY_TEMPLATES;")
    lines.append("")
    write_text(TEMPLATES_TS, "\n".join(lines))
    return TEMPLATES_TS


# ---------------------------------------------------------------------------
# 2. supabase/auth-templates/*.html
# ---------------------------------------------------------------------------

COMMENT_RE = re.compile(r"<!--.*?-->", re.DOTALL)

# A `{{` that is not Supabase's `{{ .Something }}`.  Anything this matches in an
# output would reach a real inbox as literal braces, or stop Go parsing the
# template at all.
STRAY_RE = re.compile(r"\{\{(?!\s*\.)")


class AuthTemplate:
    """One dashboard-ready file: which body, what copy, what Supabase supplies."""

    def __init__(
        self,
        out_name: str,
        body: str,
        title: str,
        preheader: str,
        footer: str,
        substitutions: dict[str, str],
        copy_edits: tuple[tuple[str, str], ...] = (),
    ) -> None:
        self.out_name = out_name
        self.body = body
        self.title = title
        self.preheader = preheader
        self.footer = footer
        self.substitutions = substitutions
        self.copy_edits = copy_edits


AUTH_TEMPLATES = [
    AuthTemplate(
        out_name="magic-link-otp.html",
        body="otp",
        title="Your SafeShade sign-in code",
        preheader="Your six-digit SafeShade sign-in code.",
        footer="Sent by SafeShade because somebody asked to sign in with this address.",
        # Supabase's six-digit one-time code.  This is the template the app asks
        # for first: the guardian is usually reading the email on the same phone
        # they are signing in on, and typing six digits has fewer ways to fail
        # than a link that opens a browser and bounces back through a deep link.
        substitutions={"{{code}}": "{{ .Token }}"},
    ),
    AuthTemplate(
        out_name="magic-link.html",
        body="magic-link",
        title="Sign in to SafeShade",
        preheader="Your SafeShade sign-in link.",
        footer="Sent by SafeShade because somebody asked to sign in with this address.",
        substitutions={"{{action_url}}": "{{ .ConfirmationURL }}"},
    ),
    # THIS ONE IS A CODE, NOT A LINK, AND THAT IS NOT A TYPO.
    #
    # "Confirm signup" reads like a template only a password sign-up would ever
    # use, so the obvious thing to put here is the magic-link body.  That would
    # break the app's primary sign-in for every new user.
    #
    # GoTrue's `SendMagicLink` sends the **Confirm signup** template, not the
    # Magic Link one, when the address has no account yet -- one call, two
    # templates, chosen by whether the user already exists.  The app calls
    # `signInWith(OTP) { createUser = true }` and then shows a six-digit field.
    # So the very first email a brand-new guardian ever receives is this one,
    # and if it contains a link instead of a code they are looking at a button
    # while the app waits for six digits, with the deep-link handler not yet
    # wired.  Stuck on the first screen, on the first try.
    #
    # `verifyOtp` accepts the signup confirmation token exactly as it accepts a
    # magic-link token, so the app's existing `verifyEmailOtp` needs no change.
    AuthTemplate(
        out_name="confirm-signup.html",
        body="otp",
        title="Confirm your email for SafeShade",
        preheader="Your code for confirming this address with SafeShade.",
        footer="Sent by SafeShade because this address was used to create an account.",
        substitutions={"{{code}}": "{{ .Token }}"},
        # Same body, different occasion: the words are about signing in and this
        # email is about proving an address.  Anchored on surrounding markup so a
        # future edit to otp.html fails the generator loudly instead of silently
        # not applying.
        copy_edits=(
            (
                "\n  Your sign-in code\n</h1>",
                "\n  Confirm your email address\n</h1>",
            ),
            (
                "Enter this in SafeShade to sign in. It expires in one hour.",
                "Enter this in SafeShade to confirm your address. It expires in one hour.",
            ),
            (
                "If you did not ask to sign in, you can ignore this email. Nobody can use this\n  code without also having your inbox.",
                "If you did not create a SafeShade account, you can ignore this email. Nobody\n  can use this code without also having your inbox.",
            ),
        ),
    ),
]

# invite.html is deliberately NOT generated as an auth template.  Its
# placeholders -- inviter_name, circle_name, wearer_name, role_label,
# role_description, expires_at_label -- have no Supabase Auth counterpart; they
# would only be available through `.Data` on an `inviteUserByEmail` call, and
# nothing in SafeShade makes one.  Invitations go out through the `send-invite`
# edge function and Resend, which renders invite.html from templates.ts with
# real values.  Emitting a dashboard file full of `{{ .Data.role_label }}` would
# advertise a contract nobody wires up.


def strip_comments(html: str) -> str:
    """Removes ``<!-- ... -->`` from a template. Used by BOTH output modes.

    For the auth templates it is a correctness gate: the source comments discuss
    ``{{code}}`` and ``{{action_url}}`` in prose, and Go's template parser reads
    ``{{code}}`` as a call to a function it does not have and refuses the whole
    template.

    For ``templates.ts`` it fixes a live defect.  ``render.ts``'s
    ``toPlainText`` strips tags with ``<[^>]+>``, which stops at the first
    ``>`` -- and these comments contain arrows like ``Dashboard -> Auth``.  So a
    comment was only half-removed and the remaining prose became visible body
    text in the ``text/plain`` alternative of every email, which is the part a
    locked phone shows as the notification preview.  The comments are authoring
    notes about the source file; they were never content.  They stay in the
    ``.html`` files, which remain the source of truth.
    """
    without = COMMENT_RE.sub("", html)
    # Tidy the debris a removed comment leaves: the indentation that preceded it
    # becomes a whitespace-only line, and consecutive removals leave blank runs.
    without = re.sub(r"[ \t]+\n", "\n", without)
    return re.sub(r"\n{3,}", "\n\n", without)


def render_auth_template(spec: AuthTemplate, emblem: str) -> str:
    layout = strip_comments(read_text(EMAIL_DIR / f"{LAYOUT_NAME}.html"))
    body = strip_comments(read_text(EMAIL_DIR / f"{spec.body}.html"))

    for old, new in spec.copy_edits:
        if old not in body:
            raise SystemExit(
                f"{spec.out_name}: copy edit no longer matches the source body "
                f"({spec.body}.html). The source changed; update "
                f"tools/gen_email_templates.py rather than the output."
            )
        body = body.replace(old, new)

    for old, new in spec.substitutions.items():
        if old not in body:
            raise SystemExit(
                f"{spec.out_name}: expected placeholder {old} is not in "
                f"{spec.body}.html any more."
            )
        body = body.replace(old, new)

    html = layout.replace("{{{content}}}", body.strip())
    html = html.replace("{{{footer}}}", spec.footer)
    html = html.replace("{{emblem}}", emblem)
    html = html.replace("{{accent}}", ACCENT_TEAL)
    html = html.replace("{{title}}", spec.title)
    html = html.replace("{{preheader}}", spec.preheader)
    return html.strip() + "\n"


def check_no_stray_placeholders(name: str, html: str) -> list[str]:
    problems = []
    for match in STRAY_RE.finditer(html):
        start = max(0, match.start() - 20)
        problems.append(f"{name}: stray placeholder near {html[start:match.start() + 40]!r}")
    if "}}}" in html:
        problems.append(f"{name}: a triple-brace {{{{{{ }}}}}} slot survived")
    return problems


def generate_auth_templates() -> tuple[list[Path], list[str]]:
    emblem = emblem_data_uri()
    written: list[Path] = []
    problems: list[str] = []
    for spec in AUTH_TEMPLATES:
        html = render_auth_template(spec, emblem)
        problems.extend(check_no_stray_placeholders(spec.out_name, html))
        out = AUTH_DIR / spec.out_name
        write_text(out, html)
        written.append(out)
    return written, problems


def main() -> int:
    templates = generate_templates_ts()
    print(f"wrote {templates.relative_to(REPO).as_posix()}")

    written, problems = generate_auth_templates()
    for path in written:
        print(f"wrote {path.relative_to(REPO).as_posix()}")

    if problems:
        print("\nUNRESOLVED PLACEHOLDERS -- these files must not be pasted:", file=sys.stderr)
        for problem in problems:
            print(f"  {problem}", file=sys.stderr)
        return 1

    print("\nplaceholder scan clean: every {{...}} left is a Supabase {{ .X }} form")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

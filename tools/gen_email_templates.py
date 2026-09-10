#!/usr/bin/env python3
"""Generates the two things that must not be produced by hand.

1.  ``supabase/functions/_shared/email/templates/`` -- one TypeScript module
    per ``.html`` file in that directory, each holding that template verbatim
    and its subject line, plus ``layout.ts`` for the shared frame.

    One module per template rather than one holding all of them, because an
    edge function ships the files it imports: ``send-account-deleted`` used to
    carry the fall alert and the whole weekly-report infographic in its bundle,
    and "which emails can this function send" was not answerable by reading it.

    Each body file carries its own subject line in a ``<title>`` element at the
    top.  The subject and the email are one design decision -- an alert whose
    heading says a fall was detected and whose subject line says "SafeShade
    notification" is two different emails -- so they live in one file, and the
    generator lifts the ``<title>`` out into ``SUBJECTS`` and strips it from the
    body constant (a ``<title>`` left in a fragment would render inside the
    layout's content slot as visible text).  The functions then read the subject
    from ``SUBJECTS`` rather than each writing its own, which is how the two
    stay in step.

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
    placeholder resolved: the two hosted brand image URLs, the accent, the literal copy,
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
BRAND_TS = EMAIL_DIR / "brand.ts"
TEMPLATES_DIR = EMAIL_DIR / "templates"
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


def brand_urls() -> tuple[str, str]:
    """Pulls EMBLEM_URL and LOGO_URL out of brand.ts without importing TypeScript.

    The URLs are read from the module the functions themselves import, so the
    dashboard templates and the edge functions cannot point at different
    images. The base is a plain string constant; the two exports are template
    literals over it.
    """
    source = read_text(BRAND_TS)
    base = re.search(r'const\s+BRAND_BASE\s*=\s*"([^"]+)"', source)
    emblem = re.search(r'EMBLEM_URL\s*=\s*`\$\{BRAND_BASE\}([^`]+)`', source)
    logo = re.search(r'LOGO_URL\s*=\s*`\$\{BRAND_BASE\}([^`]+)`', source)
    if not (base and emblem and logo):
        raise SystemExit(
            "brand.ts no longer holds BRAND_BASE, EMBLEM_URL and LOGO_URL in the "
            "form this generator reads; update both together."
        )
    return base.group(1) + emblem.group(1), base.group(1) + logo.group(1)


def check_brand_images(urls: tuple[str, str]) -> list[str]:
    """Every URL must answer 200 before a template that carries it is written.

    A hosted image that is missing shows a broken-image icon in Gmail, which
    is worse than the alt text the old data: URI produced there. So the
    generator asks the bucket first. `--skip-image-check` exists for editing
    offline; it prints that it skipped, so the omission is on the screen.
    """
    if "--skip-image-check" in sys.argv:
        print("SKIPPED the brand image check (--skip-image-check); do not deploy from this run")
        return []
    import urllib.error
    import urllib.request

    problems = []
    for url in urls:
        try:
            with urllib.request.urlopen(urllib.request.Request(url, method="HEAD"), timeout=15) as r:
                if r.status != 200:
                    problems.append(f"{url}: HTTP {r.status}")
        except urllib.error.HTTPError as e:
            problems.append(f"{url}: HTTP {e.code} (upload supabase/brand/*.png to the brand bucket)")
        except Exception as e:  # noqa: BLE001 - any failure to fetch is the same failure here
            problems.append(f"{url}: {e}")
    return problems


TITLE_RE = re.compile(r"<title>(.*?)</title>\s*", re.DOTALL | re.IGNORECASE)


def split_title(name: str, html: str) -> tuple[str, str]:
    """Lifts a body file's ``<title>`` out and returns ``(subject, body)``.

    A body template is a *fragment* that renders into the layout's content
    slot, so a ``<title>`` left in it would be shown as ordinary text in the
    middle of the email in every client that does not parse it as head markup.
    It is written there anyway because that is where the author is looking when
    they decide what the email says, and lifting it out here is a two-line job.

    A body without one is a hard failure rather than a default subject: a
    default subject is exactly the sort of thing that ships and is only noticed
    in somebody's inbox.
    """
    match = TITLE_RE.search(html)
    if not match:
        raise SystemExit(
            f"{name}.html has no <title>. Every body template must carry its "
            f"own subject line; see tools/gen_email_templates.py."
        )
    subject = " ".join(match.group(1).split())
    if not subject:
        raise SystemExit(f"{name}.html has an empty <title>.")
    return subject, TITLE_RE.sub("", html, count=1)


def body_names() -> list[str]:
    return sorted(
        p.stem for p in EMAIL_DIR.glob("*.html") if p.stem != LAYOUT_NAME
    )


# ---------------------------------------------------------------------------
# 1. templates/*.ts -- one generated module per template
# ---------------------------------------------------------------------------


def ts_identifier(name: str) -> str:
    """A file stem as a TypeScript identifier: ``weekly-report`` -> ``weeklyReport``."""
    head, *rest = name.split("-")
    return head + "".join(part[:1].upper() + part[1:] for part in rest)


def generate_templates_ts() -> tuple[list[Path], dict[str, str]]:
    """Writes ``templates/layout.ts`` and one module per body template.

    ### One file per template, not one file holding all of them

    An edge function is deployed as a bundle of the files it is handed, and
    every one of these functions imports the email module.  A single
    ``templates.ts`` holding all ten bodies meant that ``send-account-deleted``
    -- which can send exactly one email -- shipped the fall alert, the weekly
    report's whole infographic and three dashboard templates it can never
    reach.  That is 20 KB of dead weight in every deploy and, worse, it makes
    "which emails can this function send" a question nobody can answer by
    reading its imports.

    Now each function imports the templates it actually sends, by name, and the
    import list is the answer.
    """
    bodies = body_names()
    subjects: dict[str, str] = {}
    written: list[Path] = []

    layout = strip_comments(read_text(EMAIL_DIR / f"{LAYOUT_NAME}.html"))
    layout_ts = TEMPLATES_DIR / "layout.ts"
    write_text(layout_ts, "\n".join([
        GENERATED_HEADER,
        "",
        "/** The shared frame. Bodies render into its `{{{content}}}` slot. */",
        "export const LAYOUT_HTML: string = " + ts_template_literal(layout) + ";",
        "",
    ]))
    written.append(layout_ts)

    for name in bodies:
        subject, body = split_title(
            name, strip_comments(read_text(EMAIL_DIR / f"{name}.html"))
        )
        subjects[name] = subject
        out = TEMPLATES_DIR / f"{name}.ts"
        write_text(out, "\n".join([
            GENERATED_HEADER,
            "",
            f"import type {{ EmailTemplate }} from \"../resend.ts\";",
            "",
            f"/** `{name}.html`, with its subject line from that file's `<title>`. */",
            f"export const {ts_identifier(name)}Template: EmailTemplate = {{",
            "  subject: " + ts_template_literal(subject) + ",",
            "  html: " + ts_template_literal(body.strip() + "\n") + ",",
            "};",
            "",
        ]))
        written.append(out)

    # Anything left over from an earlier run -- a template file whose .html was
    # renamed or deleted -- is removed, so a stale module cannot go on being
    # imported by a function that should have failed to build.
    keep = {p.name for p in written}
    for stale in TEMPLATES_DIR.glob("*.ts"):
        if stale.name not in keep:
            stale.unlink()
            print(f"removed stale {stale.relative_to(REPO).as_posix()}")

    return written, subjects


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
        dashboard_name: str,
        preheader: str,
        footer: str,
        substitutions: dict[str, str],
        accent: str = ACCENT_TEAL,
        title: str | None = None,
        copy_edits: tuple[tuple[str, str], ...] = (),
    ) -> None:
        self.out_name = out_name
        self.body = body
        # The name of the slot in Authentication -> Email Templates. Printed by
        # the generator and listed in supabase/README.md, because pasting the
        # right file into the wrong slot is silent.
        self.dashboard_name = dashboard_name
        # None means "the subject in the body file's <title>". Only
        # confirm-signup overrides it, because it shares otp.html with a
        # different occasion.
        self.title = title
        self.preheader = preheader
        self.footer = footer
        self.substitutions = substitutions
        self.accent = accent
        self.copy_edits = copy_edits


ACCENT_AMBER = "#F5A623"

AUTH_TEMPLATES = [
    AuthTemplate(
        out_name="magic-link-otp.html",
        body="otp",
        dashboard_name="Magic Link",
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
        dashboard_name="Magic Link (link instead of a code)",
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
        dashboard_name="Confirm signup",
        # The one spec that overrides the body's own <title>: it shares otp.html
        # with the sign-in code and the occasion is different.
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
                "Enter this code in SafeShade to sign in.",
                "Enter this code in SafeShade to confirm your address.",
            ),
            (
                "If you did not ask for this, nothing happens unless the code is used.\n      Nobody can sign in without it, so there is nothing you need to do.",
                "If you did not create a SafeShade account, nothing happens unless the code\n      is used. Nobody can use it without also having your inbox, so there is\n      nothing you need to do.",
            ),
        ),
    ),
    # ---- Added in the same pass as notify-joined and the weekly report -----
    #
    # None of the three below can fire on SafeShade today, and that is written
    # here rather than discovered later:
    #
    #  * Reset password and Reauthentication need a password sign-in. The app
    #    offers a six-digit code and Google; `signInWithPassword` exists on
    #    CloudClient with no screen behind it.
    #  * Change email address needs `updateUser({ email })`, which nothing calls.
    #
    # They are generated anyway because the alternative is three dashboard slots
    # holding Supabase's unbranded defaults on the day one of those flows ships,
    # which is the day nobody is looking at email design.
    AuthTemplate(
        out_name="reset-password.html",
        body="reset-password",
        dashboard_name="Reset Password",
        preheader="Choose a new password for SafeShade.",
        footer="Sent by SafeShade because a password reset was asked for with this address.",
        accent=ACCENT_AMBER,
        # Both, because GoTrue populates both for a recovery: the link for a
        # laptop reader, the code for somebody holding the phone.
        substitutions={
            "{{action_url}}": "{{ .ConfirmationURL }}",
            "{{code}}": "{{ .Token }}",
        },
    ),
    AuthTemplate(
        out_name="change-email.html",
        body="change-email",
        dashboard_name="Change Email Address",
        preheader="Confirm the new address on your SafeShade account.",
        footer="Sent by SafeShade to both the old and the new address, because the address on an account was asked to change.",
        accent=ACCENT_AMBER,
        # `.NewEmail` is supported in THIS template and no other. `.Email` is
        # the address the account has now. Getting these two the wrong way round
        # produces a sentence that is grammatical, plausible and false.
        substitutions={
            "{{action_url}}": "{{ .ConfirmationURL }}",
            "{{old_email}}": "{{ .Email }}",
            "{{new_email}}": "{{ .NewEmail }}",
        },
    ),
    AuthTemplate(
        out_name="reauthentication.html",
        body="reauthentication",
        dashboard_name="Reauthentication",
        preheader="Your SafeShade confirmation code.",
        footer="Sent by SafeShade because a change to your account needs confirming.",
        accent=ACCENT_AMBER,
        # `.Token` only. A reauthentication has no confirmation URL, which is
        # why reauthentication.html has no button to point one at.
        substitutions={"{{code}}": "{{ .Token }}"},
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


def render_auth_template(spec: AuthTemplate, emblem: str, logo: str) -> tuple[str, str]:
    """Returns ``(html, subject)`` for one dashboard-ready file."""
    layout = strip_comments(read_text(EMAIL_DIR / f"{LAYOUT_NAME}.html"))
    body_subject, body = split_title(
        spec.body, strip_comments(read_text(EMAIL_DIR / f"{spec.body}.html"))
    )
    subject = spec.title or body_subject

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
    html = html.replace("{{logo}}", logo)
    html = html.replace("{{accent}}", spec.accent)
    html = html.replace("{{title}}", subject)
    html = html.replace("{{preheader}}", spec.preheader)
    return html.strip() + "\n", subject


def check_no_stray_placeholders(name: str, html: str) -> list[str]:
    problems = []
    for match in STRAY_RE.finditer(html):
        start = max(0, match.start() - 20)
        problems.append(f"{name}: stray placeholder near {html[start:match.start() + 40]!r}")
    if "}}}" in html:
        problems.append(f"{name}: a triple-brace {{{{{{ }}}}}} slot survived")
    return problems


def generate_auth_templates() -> tuple[list[tuple[Path, AuthTemplate, str]], list[str]]:
    emblem, logo = brand_urls()
    written: list[tuple[Path, AuthTemplate, str]] = []
    problems: list[str] = list(check_brand_images((emblem, logo)))
    for spec in AUTH_TEMPLATES:
        html, subject = render_auth_template(spec, emblem, logo)
        problems.extend(check_no_stray_placeholders(spec.out_name, html))
        out = AUTH_DIR / spec.out_name
        write_text(out, html)
        written.append((out, spec, subject))
    return written, problems


def main() -> int:
    templates, subjects = generate_templates_ts()
    for path in templates:
        print(f"wrote {path.relative_to(REPO).as_posix()}")

    written, problems = generate_auth_templates()
    for path, _spec, _subject in written:
        print(f"wrote {path.relative_to(REPO).as_posix()}")

    if problems:
        print("\nUNRESOLVED PLACEHOLDERS -- these files must not be pasted:", file=sys.stderr)
        for problem in problems:
            print(f"  {problem}", file=sys.stderr)
        return 1

    print("\nplaceholder scan clean: every {{...}} left is a Supabase {{ .X }} form")

    # Printed rather than only written, because the dashboard's Subject field is
    # a separate box that is NOT read from the pasted file. Somebody has to type
    # these in by hand, and the only way that goes right is if the exact strings
    # are in front of them. supabase/README.md carries the same table.
    print("\nSubjects (the dashboard's Subject box, typed by hand):")
    for _path, spec, subject in written:
        print(f"  {spec.dashboard_name:<38} {subject}")

    print("\nSubjects compiled into templates/*.ts for the edge functions:")
    for name, subject in subjects.items():
        print(f"  {name:<20} {subject}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

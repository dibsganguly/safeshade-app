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


import type { EmailTemplate } from "../resend.ts";

/** `joined.html`, with its subject line from that file's `<title>`. */
export const joinedTemplate: EmailTemplate = {
  subject: `{{joiner_name}} joined {{circle_name}} on SafeShade`,
  html: `<h1 class="ss-h1 ss-ink" style="margin:0 0 12px; font-family:Archivo, 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size:26px; line-height:32px; font-weight:800; color:#22282E;">
  {{joiner_name}} joined {{circle_name}}
</h1>

<p style="margin:0 0 22px; font-size:15px; line-height:23px; color:#4A5058;">
  The invitation you sent has been accepted. {{joiner_name}} is now
  <strong style="color:#22282E;">{{role_label}}</strong> in this Circle.
</p>

<table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="margin:0 0 22px; border:1px solid #DED8CE; border-radius:10px;">
  <tr>
    <td style="padding:16px 18px; font-size:14px; line-height:22px; color:#4A5058;">
      <div style="margin-bottom:4px;">
        <span style="color:#6A7078;">Who:</span>
        <strong style="color:#22282E; word-break:break-all;">{{joiner_email}}</strong>
      </div>
      <div style="margin-bottom:4px;">
        <span style="color:#6A7078;">Role:</span>
        <strong style="color:#22282E;">{{role_label}}</strong>
      </div>
      <div>
        <span style="color:#6A7078;">When:</span>
        <strong style="color:#22282E;">{{when_label}}</strong>
      </div>
    </td>
  </tr>
</table>

<table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="margin:0 0 22px; background:#F2EFE9; border-radius:10px;">
  <tr>
    <td style="padding:16px 18px; font-size:14px; line-height:21px; color:#4A5058;">
      <strong style="color:#22282E;">As {{role_label}} they can:</strong><br>
      {{role_description}}
    </td>
  </tr>
</table>

<p style="margin:0; font-size:13px; line-height:20px; color:#6A7078;">
  If this was not expected, open SafeShade and remove them from the Circle. You
  are the owner; nobody else can.
</p>
`,
};

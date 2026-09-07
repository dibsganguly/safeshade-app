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

/** `invite.html`, with its subject line from that file's `<title>`. */
export const inviteTemplate: EmailTemplate = {
  subject: `{{inviter_name}} added you to {{circle_name}} on SafeShade`,
  html: `<h1 class="ss-h1 ss-ink" style="margin:0 0 12px; font-family:Archivo, 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size:26px; line-height:32px; font-weight:800; color:#22282E;">
  {{inviter_name}} added you to {{circle_name}}
</h1>

<p style="margin:0 0 20px; font-size:15px; line-height:23px; color:#4A5058;">
  SafeShade is a safety wearable for {{wearer_name}}. Joining this Circle as
  <strong style="color:#22282E;">{{role_label}}</strong> means you will be told
  if there is a fall, an SOS, or a missed check-in.
</p>

<table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="margin:0 0 24px; background:#F2EFE9; border-radius:10px;">
  <tr>
    <td style="padding:16px 18px; font-size:14px; line-height:21px; color:#4A5058;">
      <strong style="color:#22282E;">As {{role_label}} you can:</strong><br>
      {{role_description}}
    </td>
  </tr>
</table>

<table role="presentation" cellpadding="0" cellspacing="0" border="0" style="margin:0 0 20px;">
  <tr>
    <td align="center" style="background:#22282E; border-radius:10px;">
      <a href="{{action_url}}"
         style="display:inline-block; padding:15px 32px; font-family:Archivo, 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size:15px; font-weight:700; color:#FBF9F5; text-decoration:none;">
        Join {{circle_name}}
      </a>
    </td>
  </tr>
</table>

<p style="margin:0 0 18px; font-size:13px; line-height:20px; color:#6A7078;">
  If the button does not open SafeShade, install the app and paste this link
  into your phone's browser:<br>
  <span style="word-break:break-all; color:#4A5058;">{{action_url}}</span>
</p>

<p style="margin:0 0 18px; font-size:14px; line-height:21px; color:#4A5058;">
  <strong style="color:#22282E;">This invitation expires on {{expires_at_label}}.</strong>
  After that {{inviter_name}} can send you a new one.
</p>

<table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="margin:0; background:#F2EFE9; border-radius:10px;">
  <tr>
    <td style="padding:14px 16px; font-size:13px; line-height:20px; color:#4A5058;">
      If you did not expect this, nothing happens unless the link is used. You do
      not join anything and nobody can add you without it.
    </td>
  </tr>
</table>
`,
};

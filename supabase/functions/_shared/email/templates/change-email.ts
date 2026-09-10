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

/** `change-email.html`, with its subject line from that file's `<title>`. */
export const changeEmailTemplate: EmailTemplate = {
  subject: `Confirm your new email address for SafeShade`,
  html: `<table role="presentation" cellpadding="0" cellspacing="0" border="0" align="center" style="margin:0 auto 14px;">
  <tr>
    <td align="center" style="background:{{accent}}; border-radius:999px; padding:5px 14px; font-family:Archivo, -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; font-size:11px; line-height:16px; font-weight:700; letter-spacing:1.4px; text-transform:uppercase; color:#22282E;">
      Email address
    </td>
  </tr>
</table>

<h1 class="ss-h1 ss-ink" style="margin:0 0 12px; font-family:Archivo, -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; font-size:28px; line-height:34px; font-weight:800; letter-spacing:-0.3px; color:#22282E; text-align:center;">
  Confirm your new email address
</h1>

<p style="margin:0 0 22px; font-size:15px; line-height:23px; color:#4A5058; text-align:center;">
  Somebody asked to change the address on this SafeShade account. Confirm it and
  alerts, invitations and sign-in codes will go to the new address instead.
</p>

<table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="margin:0 0 24px; border:1px solid #DED8CE; border-radius:14px;">
  <tr>
    <td align="left" style="padding:16px 18px; font-size:14px; line-height:22px; color:#4A5058; text-align:left;">
      <div style="margin-bottom:6px;">
        <span style="color:#6A7078;">Now:</span>
        <strong style="color:#22282E; word-break:break-all;">{{old_email}}</strong>
      </div>
      <div>
        <span style="color:#6A7078;">After you confirm:</span>
        <strong style="color:#22282E; word-break:break-all;">{{new_email}}</strong>
      </div>
    </td>
  </tr>
</table>

<table role="presentation" cellpadding="0" cellspacing="0" border="0" align="center" style="margin:0 auto 20px;">
  <tr>
    <td align="center" style="background:#6FD3CC; border-radius:999px;">
      <a href="{{action_url}}"
         style="display:inline-block; padding:15px 34px; font-family:Archivo, -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; font-size:15px; font-weight:800; color:#22282E; text-decoration:none;">
        Confirm the new address
      </a>
    </td>
  </tr>
</table>

<p style="margin:0 0 18px; font-size:13px; line-height:20px; color:#6A7078; text-align:center;">
  If the button does not work, copy this address into your browser:<br>
  <span style="word-break:break-all; color:#4A5058;">{{action_url}}</span>
</p>

<p style="margin:0 0 18px; font-size:14px; line-height:21px; color:#4A5058; text-align:center;">
  <strong style="color:#22282E;">This link expires one hour after it was sent.</strong>
</p>

<table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="margin:0; background:#F2EFE9; border-radius:14px;">
  <tr>
    <td align="center" style="padding:14px 18px; font-size:13px; line-height:20px; color:#4A5058; text-align:center;">
      If you did not ask for this, nothing happens unless the link is used. The
      address on the account stays as it is, and nobody can change it without
      this email.
    </td>
  </tr>
</table>
`,
};

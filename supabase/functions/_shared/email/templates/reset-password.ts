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

/** `reset-password.html`, with its subject line from that file's `<title>`. */
export const resetPasswordTemplate: EmailTemplate = {
  subject: `Reset your SafeShade password`,
  html: `<table role="presentation" cellpadding="0" cellspacing="0" border="0" align="center" style="margin:0 auto 14px;">
  <tr>
    <td align="center" style="background:{{accent}}; border-radius:999px; padding:5px 14px; font-family:Archivo, -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; font-size:11px; line-height:16px; font-weight:700; letter-spacing:1.4px; text-transform:uppercase; color:#22282E;">
      Password
    </td>
  </tr>
</table>

<h1 class="ss-h1 ss-ink" style="margin:0 0 12px; font-family:Archivo, -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; font-size:28px; line-height:34px; font-weight:800; letter-spacing:-0.3px; color:#22282E; text-align:center;">
  Reset your password
</h1>

<p style="margin:0 0 24px; font-size:15px; line-height:23px; color:#4A5058; text-align:center;">
  Somebody asked to reset the SafeShade password for this address. Use the
  button below to choose a new one.
</p>

<table role="presentation" cellpadding="0" cellspacing="0" border="0" align="center" style="margin:0 auto 20px;">
  <tr>
    <td align="center" style="background:#6FD3CC; border-radius:999px;">
      <a href="{{action_url}}"
         style="display:inline-block; padding:15px 34px; font-family:Archivo, -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; font-size:15px; font-weight:800; color:#22282E; text-decoration:none;">
        Choose a new password
      </a>
    </td>
  </tr>
</table>

<p style="margin:0 0 22px; font-size:13px; line-height:20px; color:#6A7078; text-align:center;">
  If the button does not work, copy this address into your browser:<br>
  <span style="word-break:break-all; color:#4A5058;">{{action_url}}</span>
</p>

<div style="font-family:Archivo, -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; font-size:12px; font-weight:700; letter-spacing:1.2px; text-transform:uppercase; color:#6A7078; margin:0 0 8px;">
  Or enter this code in the app
</div>
<table role="presentation" cellpadding="0" cellspacing="0" border="0" align="center" style="margin:0 auto 20px;">
  <tr>
    <td align="center" style="background:#22282E; border-radius:14px; padding:22px 34px;">
      <span class="ss-code" style="font-family:Archivo, -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; font-size:38px; line-height:44px; font-weight:800; letter-spacing:10px; color:#6FD3CC;">{{code}}</span>
    </td>
  </tr>
</table>

<p style="margin:0 0 18px; font-size:14px; line-height:21px; color:#4A5058; text-align:center;">
  <strong style="color:#22282E;">The link and the code both expire one hour after this email was sent.</strong>
</p>

<table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="margin:0; background:#F2EFE9; border-radius:14px;">
  <tr>
    <td align="center" style="padding:14px 18px; font-size:13px; line-height:20px; color:#4A5058; text-align:center;">
      If you did not ask for this, nothing happens unless the link or the code is
      used. Your password has not changed and nobody can change it without one of
      them.
    </td>
  </tr>
</table>
`,
};

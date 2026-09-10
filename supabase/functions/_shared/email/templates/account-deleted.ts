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

/** `account-deleted.html`, with its subject line from that file's `<title>`. */
export const accountDeletedTemplate: EmailTemplate = {
  subject: `A request to delete your SafeShade account`,
  html: `<table role="presentation" cellpadding="0" cellspacing="0" border="0" align="center" style="margin:0 auto 14px;">
  <tr>
    <td align="center" style="background:{{accent}}; border-radius:999px; padding:5px 14px; font-family:Archivo, -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; font-size:11px; line-height:16px; font-weight:700; letter-spacing:1.4px; text-transform:uppercase; color:#22282E;">
      Your account
    </td>
  </tr>
</table>

<h1 class="ss-h1 ss-ink" style="margin:0 0 12px; font-family:Archivo, -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; font-size:28px; line-height:34px; font-weight:800; letter-spacing:-0.3px; color:#22282E; text-align:center;">
  Your SafeShade account is being deleted
</h1>

<p style="margin:0 0 22px; font-size:15px; line-height:23px; color:#4A5058; text-align:center;">
  A request to delete this account was made from a signed-in SafeShade app on
  {{when_label}}. This email was sent at the moment the request was made, which
  is the last moment there is an address to send it to.
</p>

<table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="margin:0 0 22px; border:1px solid #DED8CE; border-radius:14px;">
  <tr>
    <td align="left" style="padding:16px 18px; font-size:14px; line-height:22px; color:#4A5058; text-align:left;">
      <div style="margin-bottom:4px;">
        <span style="color:#6A7078;">Account:</span>
        <strong style="color:#22282E; word-break:break-all;">{{account_email}}</strong>
      </div>
      <div>
        <span style="color:#6A7078;">Requested:</span>
        <strong style="color:#22282E;">{{when_label}}</strong>
      </div>
    </td>
  </tr>
</table>

<div style="font-family:Archivo, -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; font-size:12px; font-weight:700; letter-spacing:1.2px; text-transform:uppercase; color:#6A7078; margin:0 0 8px;">
  What this removes
</div>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="margin:0 0 22px; background:#F2EFE9; border-radius:14px;">
  <tr>
    <td align="left" style="padding:16px 18px; font-size:14px; line-height:22px; color:#4A5058; text-align:left;">
      Your Circle and everything in it: members and invitations, wearers, alerts
      and their trip log, messages and voice notes, safe zones, the Medical ID,
      emergency contacts and any evidence recordings. It cannot be restored and
      SafeShade keeps no copy.
      <div style="margin-top:10px;">
        The SafeShade app on your phone keeps working. Everything it holds
        locally stays on the phone - deleting the account removes what was
        synced, not the device.
      </div>
    </td>
  </tr>
</table>

<table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="margin:0; background:#F2EFE9; border-radius:14px;">
  <tr>
    <td align="center" style="padding:14px 18px; font-size:13px; line-height:20px; color:#4A5058; text-align:center;">
      If you did not ask for this, somebody has been signed in to SafeShade as
      you. Sign in again straight away - if the account still exists, the
      deletion did not complete, and you should change the address on it.
    </td>
  </tr>
</table>
`,
};

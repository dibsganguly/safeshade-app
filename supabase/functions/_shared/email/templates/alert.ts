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

/** `alert.html`, with its subject line from that file's `<title>`. */
export const alertTemplate: EmailTemplate = {
  subject: `{{headline}}`,
  html: `<table role="presentation" cellpadding="0" cellspacing="0" border="0" align="center" style="margin:0 auto 14px;">
  <tr>
    <td align="center" style="background:{{accent}}; border-radius:999px; padding:5px 14px; font-family:Archivo, -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; font-size:11px; line-height:16px; font-weight:700; letter-spacing:1.4px; text-transform:uppercase; color:#FFFFFF;">
      Alert
    </td>
  </tr>
</table>

<h1 class="ss-h1 ss-ink" style="margin:0 0 10px; font-family:Archivo, -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; font-size:28px; line-height:34px; font-weight:800; letter-spacing:-0.3px; color:#22282E; text-align:center;">
  {{headline}}
</h1>

<table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="margin:0 0 22px; background:#F2EFE9; border-radius:14px;">
  <tr>
    <td align="left" style="padding:16px 18px; font-size:15px; line-height:23px; color:#4A5058; text-align:left;">
      <div style="margin-bottom:4px;"><span style="color:#6A7078;">When:</span> <strong style="color:#22282E;">{{when_label}}</strong></div>
      <div><span style="color:#6A7078;">Where:</span> <strong style="color:#22282E;">{{where_label}}</strong></div>
    </td>
  </tr>
</table>

<table role="presentation" cellpadding="0" cellspacing="0" border="0" align="center" style="margin:0 auto 14px;">
  <tr>
    <td align="center" style="background:#22282E; border-radius:999px;">
      <a href="{{map_url}}"
         style="display:inline-block; padding:15px 32px; font-family:Archivo, -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; font-size:15px; font-weight:700; color:#FBF9F5; text-decoration:none;">
        Open the location on a map
      </a>
    </td>
  </tr>
</table>

<p style="margin:0 0 24px; font-size:13px; line-height:20px; color:#6A7078; text-align:center;">
  On the phone with SafeShade installed, this opens the app:
  <span style="word-break:break-all; color:#4A5058;">{{app_url}}</span>
</p>

<table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="margin:0 0 22px; border:1px solid #DED8CE; border-radius:14px;">
  <tr>
    <td align="left" style="padding:18px; font-size:14px; line-height:22px; color:#4A5058; text-align:left;">
      <div style="font-family:Archivo, -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; font-size:12px; font-weight:700; letter-spacing:1.2px; text-transform:uppercase; color:#6A7078; margin-bottom:10px;">
        Medical ID &mdash; {{wearer_name}}
      </div>
      {{{medical_rows}}}
    </td>
  </tr>
</table>

<div style="font-family:Archivo, -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; font-size:12px; font-weight:700; letter-spacing:1.2px; text-transform:uppercase; color:#6A7078; margin:0 0 8px;">
  Also notified
</div>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="margin:0 0 22px;">
  <tr>
    <td style="font-size:14px; line-height:22px; color:#4A5058;">
      {{{delivery_rows}}}
    </td>
  </tr>
</table>

<p style="margin:0; font-size:13px; line-height:20px; color:#6A7078; text-align:center;">
  Open SafeShade to acknowledge this alert so the rest of the Circle knows
  somebody has seen it.
</p>
`,
};

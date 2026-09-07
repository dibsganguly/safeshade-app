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

/** `reauthentication.html`, with its subject line from that file's `<title>`. */
export const reauthenticationTemplate: EmailTemplate = {
  subject: `Your SafeShade confirmation code`,
  html: `<h1 class="ss-h1 ss-ink" style="margin:0 0 12px; font-family:Archivo, 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size:26px; line-height:32px; font-weight:800; color:#22282E;">
  Confirm it is you
</h1>

<p style="margin:0 0 22px; font-size:15px; line-height:23px; color:#4A5058;">
  SafeShade asked for this code before making a change to your account. Enter it
  in the app to go on.
</p>

<table role="presentation" cellpadding="0" cellspacing="0" border="0" style="margin:0 0 20px;">
  <tr>
    <td align="center" style="background:#F2EFE9; border:1px solid #DED8CE; border-radius:10px; padding:20px 30px;">
      <span class="ss-code ss-ink" style="font-family:'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; font-size:34px; line-height:40px; font-weight:700; letter-spacing:8px; color:#22282E;">{{code}}</span>
    </td>
  </tr>
</table>

<p style="margin:0 0 18px; font-size:14px; line-height:21px; color:#4A5058;">
  <strong style="color:#22282E;">This code expires one hour after it was sent.</strong>
</p>

<table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="margin:0; background:#F2EFE9; border-radius:10px;">
  <tr>
    <td style="padding:14px 16px; font-size:13px; line-height:20px; color:#4A5058;">
      If you did not ask for this, nothing happens unless the code is used. The
      change cannot be made without it. If you are not signed in to SafeShade
      right now, sign in and change your sign-in address.
    </td>
  </tr>
</table>
`,
};

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

/** `weekly-report.html`, with its subject line from that file's `<title>`. */
export const weeklyReportTemplate: EmailTemplate = {
  subject: `{{circle_name}}: your SafeShade week`,
  html: `<h1 class="ss-h1 ss-ink" style="margin:0 0 8px; font-family:Archivo, 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size:26px; line-height:32px; font-weight:800; color:#22282E;">
  {{circle_name}}
</h1>

<p style="margin:0 0 24px; font-size:14px; line-height:21px; color:#6A7078;">
  {{period_label}}
</p>

{{^has_data}}
<p style="margin:0 0 8px; font-size:15px; line-height:23px; color:#4A5058;">
  Nothing was recorded in this Circle in the last seven days &mdash; no alerts,
  no messages, no zone events. {{quiet_caveat}}
</p>
{{/has_data}}

{{#has_data}}

<table role="presentation" width="536" cellpadding="0" cellspacing="0" border="0" style="width:536px; margin:0 0 8px; background:#F2EFE9; border-radius:10px;">
  <tr>
    <td align="center" width="134" style="width:134px; padding:16px 4px;">
      <div class="ss-stat ss-ink" style="font-family:Archivo, 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size:28px; line-height:34px; font-weight:800; color:#22282E;">{{alert_count}}</div>
      <div style="font-size:11px; line-height:16px; color:#6A7078;">alerts</div>
    </td>
    <td align="center" width="134" style="width:134px; padding:16px 4px;">
      <div class="ss-stat" style="font-family:Archivo, 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size:28px; line-height:34px; font-weight:800; color:{{answered_colour}};">{{answered_count}}</div>
      <div style="font-size:11px; line-height:16px; color:#6A7078;">answered</div>
    </td>
    <td align="center" width="134" style="width:134px; padding:16px 4px;">
      <div class="ss-stat ss-ink" style="font-family:Archivo, 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size:28px; line-height:34px; font-weight:800; color:#22282E;">{{message_count}}</div>
      <div style="font-size:11px; line-height:16px; color:#6A7078;">messages</div>
    </td>
    <td align="center" width="134" style="width:134px; padding:16px 4px;">
      <div class="ss-stat ss-ink" style="font-family:Archivo, 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size:28px; line-height:34px; font-weight:800; color:#22282E;">{{covered_days}}</div>
      <div style="font-size:11px; line-height:16px; color:#6A7078;">days with data</div>
    </td>
  </tr>
</table>

{{#comparison_rows}}
<table role="presentation" width="536" cellpadding="0" cellspacing="0" border="0" style="width:536px; margin:0 0 26px;">
  <tr>
    <td style="padding:2px 4px; font-size:13px; line-height:20px; color:#6A7078;">
      {{{comparison_rows}}}
    </td>
  </tr>
</table>
{{/comparison_rows}}

{{#kind_bars}}
<div style="font-family:Archivo, 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size:12px; font-weight:700; letter-spacing:1.2px; text-transform:uppercase; color:#6A7078; margin:0 0 10px;">
  What was raised
</div>
<table role="presentation" width="536" cellpadding="0" cellspacing="0" border="0" style="width:536px; margin:0 0 26px;">
  {{{kind_bars}}}
</table>
{{/kind_bars}}

<div style="font-family:Archivo, 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size:12px; font-weight:700; letter-spacing:1.2px; text-transform:uppercase; color:#6A7078; margin:0 0 10px;">
  Day by day
</div>
<table role="presentation" width="536" cellpadding="0" cellspacing="0" border="0" style="width:536px; margin:0 0 10px;">
  <tr>
    {{{day_cells}}}
  </tr>
</table>
<p style="margin:0 0 26px; font-size:12px; line-height:18px; color:#8A9099;">
  {{day_legend}}
</p>

{{#wearer_panels}}
<div style="font-family:Archivo, 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size:12px; font-weight:700; letter-spacing:1.2px; text-transform:uppercase; color:#6A7078; margin:0 0 10px;">
  Who this is about
</div>
{{{wearer_panels}}}
{{/wearer_panels}}

{{/has_data}}

<p style="margin:0; font-size:13px; line-height:20px; color:#6A7078;">
  This report counts only what reached SafeShade Cloud. A phone that was offline
  all week has nothing here and that is not the same as a quiet week.
</p>
`,
};

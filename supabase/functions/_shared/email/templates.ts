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


/** The shared frame. Bodies render into its `{{{content}}}` slot. */
export const LAYOUT_HTML: string = `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">

<meta name="color-scheme" content="light dark">
<meta name="supported-color-schemes" content="light dark">
<title>{{title}}</title>
<style>
  /*
    Almost nothing lives here. Gmail strips <style> in some clients and
    Outlook's Word rendering engine ignores most of it, so every rule that
    matters is inlined on the element. This block carries only the things that
    can be lost without breaking the message.
  */
  body { margin: 0; padding: 0; background: #F2EFE9; }
  a { color: #22282E; }
  @media (max-width: 620px) {
    .ss-pad { padding-left: 20px !important; padding-right: 20px !important; }
    .ss-h1 { font-size: 24px !important; line-height: 30px !important; }
  }
</style>
</head>

<body style="margin:0; padding:0; background:#F2EFE9;">

<div style="display:none; font-size:1px; color:#F2EFE9; line-height:1px; max-height:0; max-width:0; opacity:0; overflow:hidden;">
  {{preheader}}
</div>

<table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="background:#F2EFE9;">
  <tr>
    <td align="center" style="padding:24px 12px;">

      <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" style="width:600px; max-width:600px;">

        <tr>
          <td class="ss-pad" style="background:#22282E; border-radius:14px 14px 0 0; padding:22px 32px;">
            <table role="presentation" cellpadding="0" cellspacing="0" border="0">
              <tr>
                <td style="vertical-align:middle; padding-right:12px;">

                  <img src="{{emblem}}" width="26" height="40" alt="SafeShade"
                       style="display:block; border:0; outline:none; text-decoration:none; height:40px; width:26px;">
                </td>
                <td style="vertical-align:middle;">
                  <span style="font-family:Archivo, 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size:19px; font-weight:800; letter-spacing:0.2px; color:#FBF9F5;">SafeShade</span>
                </td>
              </tr>
            </table>
          </td>
        </tr>

        <tr>
          <td style="background:{{accent}}; height:4px; line-height:4px; font-size:0;">&nbsp;</td>
        </tr>

        <tr>
          <td class="ss-pad" style="background:#FBF9F5; padding:32px; font-family:Archivo, 'Helvetica Neue', Helvetica, Arial, sans-serif; color:#22282E;">
            {{{content}}}
          </td>
        </tr>

        <tr>
          <td class="ss-pad" style="background:#F2EFE9; border-radius:0 0 14px 14px; padding:20px 32px; font-family:Archivo, 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size:12px; line-height:18px; color:#6A7078;">
            {{{footer}}}
            <div style="margin-top:10px;">
              SafeShade &middot; Universal Safety Companion
            </div>
          </td>
        </tr>

      </table>

    </td>
  </tr>
</table>

</body>
</html>
`;

/** Every body template, keyed by its file name without the extension. */
export const BODY_TEMPLATES = {
  alert: `
<h1 class="ss-h1" style="margin:0 0 10px; font-family:Archivo, 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size:26px; line-height:32px; font-weight:800; color:#22282E;">
  {{headline}}
</h1>

<p style="margin:0 0 20px; font-size:15px; line-height:23px; color:#4A5058;">
  {{when_label}}{{where_clause}}
</p>

<table role="presentation" cellpadding="0" cellspacing="0" border="0" style="margin:0 0 26px;">
  <tr>
    <td align="center" style="background:#22282E; border-radius:10px;">
      <a href="{{map_url}}"
         style="display:inline-block; padding:14px 30px; font-family:Archivo, 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size:15px; font-weight:700; color:#FBF9F5; text-decoration:none;">
        Open the location on a map
      </a>
    </td>
  </tr>
</table>

<table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="margin:0 0 22px; border:1px solid #DED8CE; border-radius:10px;">
  <tr>
    <td style="padding:18px; font-size:14px; line-height:22px; color:#4A5058;">
      <div style="font-family:Archivo, 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size:12px; font-weight:700; letter-spacing:1.2px; text-transform:uppercase; color:#6A7078; margin-bottom:10px;">
        Medical ID &mdash; {{wearer_name}}
      </div>
      {{{medical_rows}}}
    </td>
  </tr>
</table>

<div style="font-family:Archivo, 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size:12px; font-weight:700; letter-spacing:1.2px; text-transform:uppercase; color:#6A7078; margin:0 0 8px;">
  Also notified
</div>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="margin:0 0 22px;">
  <tr>
    <td style="font-size:14px; line-height:22px; color:#4A5058;">
      {{{delivery_rows}}}
    </td>
  </tr>
</table>

<p style="margin:0; font-size:13px; line-height:20px; color:#6A7078;">
  You are receiving this because you are in {{wearer_name}}'s SafeShade Circle.
  Open the app to acknowledge this alert so the rest of the Circle knows somebody
  has seen it.
</p>
`,
  digest: `
<h1 class="ss-h1" style="margin:0 0 10px; font-family:Archivo, 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size:26px; line-height:32px; font-weight:800; color:#22282E;">
  {{wearer_name}}'s week
</h1>

<p style="margin:0 0 24px; font-size:15px; line-height:23px; color:#4A5058;">
  {{period_label}}
</p>

<table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="margin:0 0 26px; background:#F2EFE9; border-radius:10px;">
  <tr>
    <td align="center" style="padding:18px 8px; width:33%;">
      <div style="font-family:Archivo, 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size:28px; line-height:34px; font-weight:800; color:#22282E;">{{alert_count}}</div>
      <div style="font-size:12px; line-height:18px; color:#6A7078;">alerts</div>
    </td>
    <td align="center" style="padding:18px 8px; width:33%;">
      <div style="font-family:Archivo, 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size:28px; line-height:34px; font-weight:800; color:#22282E;">{{checkin_count}}</div>
      <div style="font-size:12px; line-height:18px; color:#6A7078;">check-ins</div>
    </td>
    <td align="center" style="padding:18px 8px; width:33%;">
      <div style="font-family:Archivo, 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size:28px; line-height:34px; font-weight:800; color:#22282E;">{{zone_event_count}}</div>
      <div style="font-size:12px; line-height:18px; color:#6A7078;">zone events</div>
    </td>
  </tr>
</table>

<div style="font-family:Archivo, 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size:12px; font-weight:700; letter-spacing:1.2px; text-transform:uppercase; color:#6A7078; margin:0 0 8px;">
  What happened
</div>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="margin:0 0 22px;">
  <tr>
    <td style="font-size:14px; line-height:22px; color:#4A5058;">
      {{{event_rows}}}
    </td>
  </tr>
</table>

<table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="margin:0 0 22px; border:1px solid #DED8CE; border-radius:10px;">
  <tr>
    <td style="padding:16px 18px; font-size:14px; line-height:22px; color:#4A5058;">
      <strong style="color:#22282E;">{{device_name}}</strong><br>
      {{device_status}}
    </td>
  </tr>
</table>

<p style="margin:0; font-size:13px; line-height:20px; color:#6A7078;">
  You can turn this summary off in SafeShade under Settings.
</p>
`,
  invite: `
<h1 class="ss-h1" style="margin:0 0 12px; font-family:Archivo, 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size:26px; line-height:32px; font-weight:800; color:#22282E;">
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

<table role="presentation" cellpadding="0" cellspacing="0" border="0" style="margin:0 0 22px;">
  <tr>
    <td align="center" style="background:#22282E; border-radius:10px;">
      <a href="{{action_url}}"
         style="display:inline-block; padding:14px 30px; font-family:Archivo, 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size:15px; font-weight:700; color:#FBF9F5; text-decoration:none;">
        Join {{circle_name}}
      </a>
    </td>
  </tr>
</table>

<p style="margin:0 0 18px; font-size:13px; line-height:20px; color:#6A7078;">
  If the button does not work, copy this address into your browser:<br>
  <span style="word-break:break-all; color:#4A5058;">{{action_url}}</span>
</p>

<p style="margin:0; font-size:14px; line-height:21px; color:#4A5058;">
  This invitation expires on {{expires_at_label}}. If you were not expecting it,
  ignore this email - nothing happens until you accept.
</p>
`,
  "magic-link": `
<h1 class="ss-h1" style="margin:0 0 12px; font-family:Archivo, 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size:26px; line-height:32px; font-weight:800; color:#22282E;">
  Sign in to SafeShade
</h1>

<p style="margin:0 0 24px; font-size:15px; line-height:23px; color:#4A5058;">
  Tap the button below to sign in. The link works once and expires in one hour.
</p>

<table role="presentation" cellpadding="0" cellspacing="0" border="0" style="margin:0 0 22px;">
  <tr>
    <td align="center" style="background:#22282E; border-radius:10px;">
      <a href="{{action_url}}"
         style="display:inline-block; padding:14px 30px; font-family:Archivo, 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size:15px; font-weight:700; color:#FBF9F5; text-decoration:none;">
        Sign in to SafeShade
      </a>
    </td>
  </tr>
</table>

<p style="margin:0 0 18px; font-size:13px; line-height:20px; color:#6A7078;">
  If the button does not work, copy this address into your browser:<br>
  <span style="word-break:break-all; color:#4A5058;">{{action_url}}</span>
</p>

<p style="margin:0; font-size:14px; line-height:21px; color:#4A5058;">
  If you did not ask to sign in, you can ignore this email.
</p>
`,
  otp: `
<h1 class="ss-h1" style="margin:0 0 12px; font-family:Archivo, 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size:26px; line-height:32px; font-weight:800; color:#22282E;">
  Your sign-in code
</h1>

<p style="margin:0 0 22px; font-size:15px; line-height:23px; color:#4A5058;">
  Enter this in SafeShade to sign in. It expires in one hour.
</p>

<table role="presentation" cellpadding="0" cellspacing="0" border="0" style="margin:0 0 24px;">
  <tr>
    <td align="center" style="background:#F2EFE9; border:1px solid #DED8CE; border-radius:10px; padding:18px 28px;">
      <span style="font-family:'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; font-size:34px; line-height:40px; font-weight:700; letter-spacing:8px; color:#22282E;">{{code}}</span>
    </td>
  </tr>
</table>

<p style="margin:0 0 6px; font-size:14px; line-height:21px; color:#4A5058;">
  If you did not ask to sign in, you can ignore this email. Nobody can use this
  code without also having your inbox.
</p>
`,
} as const;

/** The names {@link BODY_TEMPLATES} accepts. */
export type BodyTemplateName = keyof typeof BODY_TEMPLATES;

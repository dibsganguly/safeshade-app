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

<link href="https://fonts.googleapis.com/css2?family=Archivo:wght@400;600;700;800&display=swap" rel="stylesheet">
<style>
  @import url('https://fonts.googleapis.com/css2?family=Archivo:wght@400;600;700;800&display=swap');
  /*
    Almost nothing lives here. Gmail strips <style> in some clients and
    Outlook's Word rendering engine ignores most of it, so every rule that
    matters is inlined on the element. This block carries only the things that
    can be lost without breaking the message.
  */
  body { margin: 0; padding: 0; background: #EDF4F3; }
  a { color: #22282E; }
  /* Outlook.com and Apple Mail can still force a dark rendering even with
     color-scheme set. These rules make that case survivable rather than
     pretty: the panel keeps a light ground and the ink keeps its contrast, so
     no heading is ever charcoal on charcoal. */
  @media (prefers-color-scheme: dark) {
    .ss-panel { background: #FFFFFF !important; }
    .ss-ink { color: #22282E !important; }
  }
  @media (max-width: 620px) {
    .ss-pad { padding-left: 20px !important; padding-right: 20px !important; }
    .ss-h1 { font-size: 24px !important; line-height: 30px !important; }
    .ss-code { font-size: 32px !important; letter-spacing: 6px !important; }
    .ss-stat { font-size: 24px !important; }
  }
</style>
</head>

<body style="margin:0; padding:0; background:#EDF4F3;">

<div style="display:none; font-size:1px; color:#EDF4F3; line-height:1px; max-height:0; max-width:0; opacity:0; overflow:hidden;">
  {{preheader}}
</div>

<table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="background:#EDF4F3;">
  <tr>
    <td align="center" style="padding:28px 12px 36px;">

      <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" align="center" style="width:600px; max-width:600px;">

        <tr>
          <td align="center" bgcolor="#F2EDE3" style="background:#F2EDE3; padding:0; font-size:0; line-height:0; border-radius:18px 18px 0 0; text-align:center;">
            <img src="{{masthead}}" width="600" height="168" alt="SafeShade - Your Everything Safety Companion"
                 style="display:block; margin:0 auto; border:0; outline:none; text-decoration:none; width:600px; height:168px; max-width:100%;">
          </td>
        </tr>

        <tr>
          <td style="padding:0; font-size:0; line-height:0;">
            <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" style="width:600px;">
              <tr>
                <td width="480" style="width:480px; background:{{accent}}; height:6px; line-height:6px; font-size:0;">&nbsp;</td>
                <td width="120" style="width:120px; background:#F5A623; height:6px; line-height:6px; font-size:0;">&nbsp;</td>
              </tr>
            </table>
          </td>
        </tr>

        <tr>
          <td class="ss-pad ss-panel" align="center" style="background:#FFFFFF; padding:34px 32px 30px; text-align:center; font-family:Archivo, -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; color:#22282E;">
            {{{content}}}
          </td>
        </tr>

        <tr>
          <td class="ss-pad" align="center" style="background:#F6F4EF; border-top:1px solid #E4E0D8; border-radius:0 0 18px 18px; padding:18px 32px 22px; text-align:center; font-family:Archivo, -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; font-size:12px; line-height:17px; color:#6A7078;">
            <div style="margin:0; padding:0; line-height:17px;">{{{footer}}}</div>
            <div style="margin:0; padding:0; line-height:17px;">Replies to this address are not read.</div>
            <div style="margin:6px 0 0; padding:0; line-height:17px; color:#8A9099;">SafeShade, Patia, OD, India | CQ5D-OTPM</div>
            <div style="margin:0; padding:0; line-height:17px; color:#8A9099;">&copy; 2026 SafeShade. All rights reserved.</div>
          </td>
        </tr>

      </table>

    </td>
  </tr>
</table>

</body>
</html>
`;

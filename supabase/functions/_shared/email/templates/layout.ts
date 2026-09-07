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
  /* Outlook.com and Apple Mail can still force a dark rendering even with
     color-scheme set. These two rules make that case survivable rather than
     pretty: the panel keeps a light ground and the ink keeps its contrast, so
     no heading is ever charcoal on charcoal. */
  @media (prefers-color-scheme: dark) {
    .ss-panel { background: #FBF9F5 !important; }
    .ss-ink { color: #22282E !important; }
  }
  @media (max-width: 620px) {
    .ss-pad { padding-left: 20px !important; padding-right: 20px !important; }
    .ss-h1 { font-size: 24px !important; line-height: 30px !important; }
    .ss-code { font-size: 30px !important; letter-spacing: 6px !important; }
    .ss-stat { font-size: 24px !important; }
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

                  <div style="font-family:Archivo, 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size:19px; line-height:23px; font-weight:800; letter-spacing:0.2px; color:#FBF9F5;">SafeShade</div>
                  <div style="font-family:Archivo, 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size:11px; line-height:16px; letter-spacing:1.4px; text-transform:uppercase; color:#9AA1A9;">Universal Safety Companion</div>
                </td>
              </tr>
            </table>
          </td>
        </tr>

        <tr>
          <td style="background:{{accent}}; height:4px; line-height:4px; font-size:0;">&nbsp;</td>
        </tr>

        <tr>
          <td class="ss-pad ss-panel" style="background:#FBF9F5; padding:32px; font-family:Archivo, 'Helvetica Neue', Helvetica, Arial, sans-serif; color:#22282E;">
            {{{content}}}
          </td>
        </tr>

        <tr>
          <td class="ss-pad" style="background:#F2EFE9; border-radius:0 0 14px 14px; padding:20px 32px; font-family:Archivo, 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size:12px; line-height:18px; color:#6A7078;">
            {{{footer}}}
            <div style="margin-top:8px;">
              Replies to this address are not read.
            </div>
            <div style="margin-top:8px; color:#8A9099;">
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

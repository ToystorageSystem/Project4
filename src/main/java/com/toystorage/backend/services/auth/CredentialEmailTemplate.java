package com.toystorage.backend.services.auth;

import org.springframework.web.util.HtmlUtils;

/** Table layout and inline styles for email clients, with a plain-text alternative in the mailer. */
final class CredentialEmailTemplate {
    private CredentialEmailTemplate() {}

    static String render(String email, String password, boolean newAccount) {
        String title = newAccount ? "Welcome to ToyStorage" : "Your new password is ready";
        String intro = newAccount
                ? "Your account has been created. Use the details below to get started."
                : "Use the temporary password below to sign back in to your account.";
        return """
                <!doctype html>
                <html lang="en">
                <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>ToyStorage</title></head>
                <body style="margin:0;padding:0;background-color:#f5f3ef;color:#292524;font-family:Arial,Helvetica,sans-serif;">
                <div style="display:none;max-height:0;overflow:hidden;">Your ToyStorage sign-in details are ready.</div>
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f5f3ef;">
                  <tr><td align="center" style="padding:36px 16px;">
                    <table role="presentation" width="560" cellpadding="0" cellspacing="0" style="width:100%%;max-width:560px;">
                      <tr><td style="padding:0 4px 22px;font-size:24px;font-weight:bold;letter-spacing:-1px;color:#c4511b;">ToyStorage<span style="color:#78716c;font-size:14px;font-weight:normal;letter-spacing:0;"> &nbsp;/ Warehouse System</span></td></tr>
                      <tr><td style="background-color:#ffffff;border:1px solid #e7e2db;border-top:5px solid #e87633;border-radius:12px;padding:36px 28px;">
                        <p style="margin:0 0 14px;color:#b54918;font-size:12px;font-weight:bold;letter-spacing:2px;">YOUR ACCOUNT</p>
                        <h1 style="margin:0 0 16px;font-size:28px;line-height:1.3;letter-spacing:-0.6px;color:#292524;">%s</h1>
                        <p style="margin:0 0 26px;font-size:16px;line-height:1.7;color:#57534e;">Hello,<br>%s</p>
                        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#faf8f4;border:1px solid #eee7dd;border-radius:8px;">
                          <tr><td style="padding:22px 20px 16px;"><p style="margin:0 0 8px;font-size:13px;color:#78716c;">Sign-in email</p><p style="margin:0;font-size:16px;line-height:1.5;word-break:break-all;color:#292524;">%s</p></td></tr>
                          <tr><td style="padding:0 20px 22px;"><p style="margin:0 0 10px;font-size:13px;color:#78716c;">Temporary password</p><p dir="ltr" style="margin:0;font-family:Consolas,'Courier New',monospace;font-size:36px;line-height:1.3;letter-spacing:6px;font-weight:bold;color:#b54918;">%s</p></td></tr>
                        </table>
                        <p style="margin:26px 0 10px;font-size:16px;line-height:1.6;font-weight:bold;">Next steps</p>
                        <p style="margin:0;font-size:15px;line-height:1.8;color:#57534e;">1. Open the ToyStorage sign-in page.<br>2. Enter the email and password above.<br>3. When prompted, choose a new password with 6 to 72 digits.</p>
                        <p style="margin:22px 0 0;padding-top:20px;border-top:1px solid #eee7dd;font-size:13px;line-height:1.7;color:#78716c;">If Google Authenticator is enabled, you will still need your verification code to complete sign-in. Keep your sign-in details private.</p>
                      </td></tr>
                      <tr><td align="center" style="padding:22px 12px 0;font-size:12px;line-height:1.8;color:#8a8178;">ToyStorage · Warehouse Management System<br>This is an automated email. Please do not reply.</td></tr>
                    </table>
                  </td></tr>
                </table>
                </body></html>
                """.formatted(title, intro, HtmlUtils.htmlEscape(email), HtmlUtils.htmlEscape(password));
    }
}

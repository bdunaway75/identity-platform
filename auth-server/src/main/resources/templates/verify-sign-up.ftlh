<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="color-scheme" content="light dark">
    <meta name="supported-color-schemes" content="light dark">
    <title>Verify your email</title>
    <style>
        @media screen and (max-width: 520px) {
            .email-shell {
                padding: 18px 12px !important;
            }

            .email-card {
                padding: 30px 22px !important;
                border-radius: 14px !important;
            }

            .email-title {
                font-size: 28px !important;
                line-height: 1.15 !important;
            }

            .email-button-wrap,
            .email-button {
                width: 100% !important;
            }

            .email-button {
                box-sizing: border-box !important;
            }
        }
    </style>
</head>
<body bgcolor="#ffffff" style="margin:0;padding:0;background:#ffffff !important;background-color:#ffffff !important;font-family:Arial,Helvetica,sans-serif;color:#e5e7eb;">
<table role="presentation" width="100%" cellspacing="0" cellpadding="0" bgcolor="#ffffff" style="width:100%;border-collapse:collapse;background:#ffffff !important;background-color:#ffffff !important;">
    <tr>
        <td class="email-shell" align="center" bgcolor="#ffffff" style="padding:36px 16px;background:#ffffff !important;background-color:#ffffff !important;">
            <table role="presentation" width="100%" cellspacing="0" cellpadding="0" bgcolor="#ffffff" style="max-width:560px;border-collapse:collapse;background:#ffffff !important;background-color:#ffffff !important;">
                <tr>
                    <td class="email-card" bgcolor="#000000" style="padding:42px 40px;background:#000000 !important;background-color:#000000 !important;border:0;border-radius:16px;text-align:center;">
                        <img src="cid:${logoContentId}" width="96" alt="Identity Platform"
                             style="display:block;width:96px;max-width:96px;height:auto;margin:0 auto 24px;border:0;outline:none;text-decoration:none;">
                        <h1 class="email-title" style="margin:0;color:#ffffff;font-size:34px;line-height:1.18;font-weight:700;">
                            Confirm your email
                        </h1>
                        <p style="margin:16px auto 0;max-width:410px;color:#cbd5e1;font-size:16px;line-height:1.6;">
                            Verify <strong style="color:#ffffff;">${emailAddress}</strong> to finish creating your account.
                        </p>

                        <table class="email-button-wrap" role="presentation" align="center" cellspacing="0" cellpadding="0" style="margin:30px auto 26px;border-collapse:collapse;">
                            <tr>
                                <td align="center" bgcolor="#1d4ed8" style="border-radius:8px;">
                                    <a class="email-button" href="${verificationUrl}" target="_blank"
                                       style="display:inline-block;padding:14px 28px;min-width:168px;border-radius:8px;background:#1d4ed8;color:#ffffff;font-size:16px;font-weight:700;line-height:1.25;text-align:center;text-decoration:none;">
                                        Verify email
                                    </a>
                                </td>
                            </tr>
                        </table>

                        <p style="margin:0 auto;max-width:390px;color:#cbd5e1;font-size:14px;line-height:1.6;">
                            This link expires in 5 minutes. If this was not you, you can ignore this email.
                        </p>

                        <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="margin:28px 0 22px;border-collapse:collapse;">
                            <tr>
                                <td style="height:1px;background:#1f2937;font-size:1px;line-height:1px;">&nbsp;</td>
                            </tr>
                        </table>

                        <p style="margin:0 auto;max-width:430px;color:#94a3b8;font-size:12px;line-height:1.7;word-break:break-word;">
                            Or paste this link into your browser:<br>
                            <a href="${verificationUrl}" target="_blank" style="color:#93c5fd;text-decoration:underline;">${verificationUrl}</a>
                        </p>
                    </td>
                </tr>
            </table>
        </td>
    </tr>
</table>
</body>
</html>

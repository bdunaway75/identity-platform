<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Subscription active</title>
</head>
<body style="margin:0;padding:24px;background:#f8fafc;font-family:Arial,Helvetica,sans-serif;color:#0f172a;">
<table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="border-collapse:collapse;">
    <tr>
        <td align="center">
            <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="max-width:600px;border-collapse:collapse;background:#ffffff;border:1px solid #e2e8f0;border-radius:14px;">
                <tr>
                    <td style="padding:32px;">
                        <h1 style="margin:0 0 16px;font-size:28px;line-height:1.2;color:#0f172a;">Your subscription is active</h1>
                        <p style="margin:0 0 14px;font-size:16px;line-height:1.6;">Hi ${emailAddress},</p>
                        <p style="margin:0 0 14px;font-size:16px;line-height:1.6;">
                            Your subscription is now on the <strong>${currentTierName}</strong> tier.
                        </p>
                        <#if previousTierName != "None">
                            <p style="margin:0 0 22px;font-size:16px;line-height:1.6;">
                                This change replaced your previous <strong>${previousTierName}</strong> tier.
                            </p>
                        <#else>
                            <p style="margin:0 0 22px;font-size:16px;line-height:1.6;">
                                Your purchase has been processed successfully and your account has been updated.
                            </p>
                        </#if>
                        <p style="margin:0 0 22px;font-size:16px;line-height:1.6;">
                            You can review or manage your subscription here:
                            <a href="${manageSubscriptionUrl}" style="color:#2563eb;">${manageSubscriptionUrl}</a>
                        </p>
                        <p style="margin:0;font-size:14px;line-height:1.6;color:#475569;">
                            Need help? Contact <a href="mailto:${supportEmail}" style="color:#2563eb;">${supportEmail}</a>.
                        </p>
                    </td>
                </tr>
            </table>
        </td>
    </tr>
</table>
</body>
</html>

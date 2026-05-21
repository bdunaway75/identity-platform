<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Subscription downgraded</title>
</head>
<body style="margin:0;padding:24px;background:#f8fafc;font-family:Arial,Helvetica,sans-serif;color:#0f172a;">
<table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="border-collapse:collapse;">
    <tr>
        <td align="center">
            <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="max-width:600px;border-collapse:collapse;background:#ffffff;border:1px solid #e2e8f0;border-radius:14px;">
                <tr>
                    <td style="padding:32px;">
                        <h1 style="margin:0 0 16px;font-size:28px;line-height:1.2;color:#0f172a;">Your subscription has been downgraded</h1>
                        <p style="margin:0 0 14px;font-size:16px;line-height:1.6;">Hi ${emailAddress},</p>
                        <p style="margin:0 0 14px;font-size:16px;line-height:1.6;">
                            Your subscription moved from <strong>${previousTierName}</strong> to <strong>${currentTierName}</strong>.
                        </p>
                        <p style="margin:0 0 22px;font-size:16px;line-height:1.6;">
                            You can review or change your plan here:
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

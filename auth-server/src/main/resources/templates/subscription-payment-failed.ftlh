<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Subscription payment failed</title>
</head>
<body style="margin:0;padding:24px;background:#f8fafc;font-family:Arial,Helvetica,sans-serif;color:#0f172a;">
<table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="border-collapse:collapse;">
    <tr>
        <td align="center">
            <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="max-width:640px;border-collapse:collapse;background:#ffffff;border:1px solid #e2e8f0;border-radius:14px;">
                <tr>
                    <td style="padding:32px;">
                        <h1 style="margin:0 0 16px;font-size:28px;line-height:1.2;color:#7c2d12;">Your subscription payment failed</h1>
                        <p style="margin:0 0 14px;font-size:16px;line-height:1.6;">Hi ${emailAddress},</p>
                        <p style="margin:0 0 14px;font-size:16px;line-height:1.6;">
                            We could not process your recurring subscription payment for <strong>${previousTierName}</strong>.
                        </p>
                        <p style="margin:0 0 14px;font-size:16px;line-height:1.6;">
                            Your account has been moved to <strong>${currentTierName}</strong>. You have until <strong>${purgeDeadline}</strong> to repurchase a qualifying plan.
                        </p>
                        <p style="margin:0 0 14px;font-size:16px;line-height:1.6;">
                            If you do not repurchase within those 2 days, any data that exceeds your downgraded tier limits will be permanently purged.
                        </p>
                        <p style="margin:0 0 10px;font-size:16px;line-height:1.6;">
                            Your downgraded tier limits are:
                        </p>
                        <ul style="margin:0 0 22px;padding-left:22px;font-size:16px;line-height:1.7;color:#0f172a;">
                            <li>${allowedRegisteredClients} registered clients</li>
                            <li>${allowedGlobalUsers} global users</li>
                            <li>${allowedGlobalScopes} global scopes</li>
                            <li>${allowedGlobalAuthorities} global authorities</li>
                        </ul>
                        <p style="margin:0 0 22px;font-size:16px;line-height:1.6;">
                            Repurchase or manage your subscription here:
                            <a href="${manageSubscriptionUrl}" style="color:#2563eb;">${manageSubscriptionUrl}</a>
                        </p>
                        <p style="margin:0;font-size:14px;line-height:1.6;color:#475569;">
                            If you need help, contact <a href="mailto:${supportEmail}" style="color:#2563eb;">${supportEmail}</a>.
                        </p>
                    </td>
                </tr>
            </table>
        </td>
    </tr>
</table>
</body>
</html>

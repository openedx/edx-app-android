package org.edx.mobile.util;

import android.content.Intent;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import org.edx.mobile.R;
import org.edx.mobile.logger.Logger;

public class EmailUtil {

    private static final Logger logger = new Logger(EmailUtil.class.getName());

    public static void sendEmailIntent(FragmentActivity activityContext, String to, String subject,
                                       String email) {
        Intent email_intent = new Intent(Intent.ACTION_SEND);
        email_intent.putExtra(Intent.EXTRA_EMAIL, new String[]{to});
        email_intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        email_intent.putExtra(Intent.EXTRA_TEXT, email);
        email_intent.setType("plain/text");

        try {
            email_intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (activityContext != null) {
                // add flag to make sure this call works from non-activity context
                Intent targetIntent = Intent.createChooser(email_intent,
                        activityContext.getString(R.string.email_chooser_header));
                targetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activityContext.startActivity(targetIntent);
            }

        } catch (android.content.ActivityNotFoundException ex) {
            //There is no activity which can perform the intended share Intent
            Toast.makeText(activityContext, activityContext.getString(R.string.email_client_not_present),
                            Toast.LENGTH_SHORT)
                    .show();
            logger.error(ex);
        }
    }
}

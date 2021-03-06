package org.thanthoai.securesms.services;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;

import org.thanthoai.securesms.BuildConfig;
import org.thanthoai.securesms.model.SentMessageModel;
import org.thanthoai.securesms.utils.locale.Country;
import org.thanthoai.securesms.utils.locale.IPhoneNumberConverter.NotValidPersonalNumberException;
import org.thanthoai.securesms.utils.locale.PhoneNumberConverterFactory;

import java.util.Locale;


public class DeleteMessageService extends IntentService {

    public static final String DELETE_MESSAGE_DONE = "delete.message.done";

    public DeleteMessageService() {
        super("DeleteMessageService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (Intent.ACTION_DELETE.equals(action)) {
                final String address = intent.getStringExtra("address");
                if (BuildConfig.DEBUG && address == null)
                    throw new AssertionError();

                Intent i = new Intent(DELETE_MESSAGE_DONE);
                i.putExtra("result", handleActionDeleteMessage(address));
                LocalBroadcastManager.getInstance(this).sendBroadcast(i);
            }
        }
    }

    private boolean handleActionDeleteMessage(String address) {
        Uri uri = Uri.parse("content://sms/");
        String shortAddress = address;
        String where;

        try {
            shortAddress = String.valueOf(Long.parseLong(shortAddress));
            if (!PhoneNumberConverterFactory.getConverter(
                    new Locale("vn", Country.VIETNAM)).isValidPersonalNumber(shortAddress)) {
                throw new NotValidPersonalNumberException();
            }
            where = "address like '%" + shortAddress + "'";
        } catch (NumberFormatException | NotValidPersonalNumberException ex) {
            where = "address='" + shortAddress + "'";
        }
        return (getContentResolver().delete(uri, where, null) != -1)
                && SentMessageModel.deleteByAddress(this, address);
    }
}

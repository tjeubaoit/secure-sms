package org.thanthoai.securesms.loaders;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import org.thanthoai.securesms.model.Contact;
import org.thanthoai.securesms.model.SentMessageModel;
import org.thanthoai.securesms.model.SmsConversation;
import org.thanthoai.securesms.utils.cache.CacheHelper;
import org.thanthoai.securesms.utils.locale.Country;
import org.thanthoai.securesms.utils.locale.PhoneNumberConverterFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SmsConversationLoader extends SimpleBaseLoader<List<SmsConversation>> {

    public SmsConversationLoader(Context context) {
        super(context);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<SmsConversation> loadInBackground() {
        Uri inboxUri = Uri.parse("content://sms/");
        String[] reqCols = new String[] {"address", "body", "date"};
        List<SmsConversation> results = new ArrayList<>();

        Cursor c = getContext().getContentResolver().query(inboxUri, reqCols,
                null, null, "date DESC");
        Set<String> addressSet = new HashSet<>();
        while (c.moveToNext()) {
            String address = c.getString(c.getColumnIndex("address"));
            address = PhoneNumberConverterFactory.getConverter(
                    new Locale("vn", Country.VIETNAM)).toLocal(address);
            final boolean ok = addressSet.add(address);
            if (ok) {
                SmsConversation conversation = new SmsConversation();
                conversation.Address = address;
                conversation.Content = c.getString(c.getColumnIndex("body"));
                conversation.Date = c.getString(c.getColumnIndex("date"));
                try {
                    String number = String.valueOf(Long.parseLong(address));
                    conversation.AddressInContact = phoneLookupFromCache(number);
                    if (conversation.AddressInContact == null) {
                        conversation.AddressInContact = phoneLookup(number);
                    }
                } catch (NumberFormatException ignored) {
                }
                results.add(conversation);
            }
        }
        c.close();

        for (SmsConversation conversation : results) {
            List<SentMessageModel> models = SentMessageModel.findByAddress(getContext(),
                    conversation.Address, "date DESC", "1");
            if (models == null || models.isEmpty()) continue;
            final SentMessageModel model = models.get(0);
            if (Long.parseLong(conversation.Date) < Long.parseLong(model.Date)) {
                conversation.Date = model.Date;
                conversation.Content = model.Body;
            }
        }
        Collections.sort(results, new Comparator<SmsConversation>() {
            @Override
            public int compare(SmsConversation lhs, SmsConversation rhs) {
                long l1 = Long.parseLong(lhs.Date);
                long l2 = Long.parseLong(rhs.Date);
                if (l1 > l2) return -1;
                else if (l1 == l2) return 0;
                else return 1;
            }
        });

        return results;
    }

    private String phoneLookup(String number) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number));
        Cursor c = getContext().getContentResolver().query(uri,
                new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME},
                null, null, null);
        if (c.moveToFirst()) {
            Set<String> results = new HashSet<>();
            do {
                results.add(c.getString(c.getColumnIndex(
                        ContactsContract.PhoneLookup.DISPLAY_NAME)));
            } while (c.moveToNext());
            c.close();

            if (!results.isEmpty()) {
                StringBuilder builder = new StringBuilder();
                int i = 0;
                for (String s : results) {
                    if (i++ > 0) builder.append(", ");
                    builder.append(s);
                }
                return builder.toString();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String phoneLookupFromCache(String number) {
        if (!CacheHelper.getInstance().contains("contact")) return null;
        List<Contact> list = (List<Contact>)
                CacheHelper.getInstance().get("contact");
        for (Contact contact : list) {
            Set<String> results = new HashSet<>();
            for (String phoneNumber : contact.PhoneNumbers.keySet()) {
                if (phoneNumber.equals(number)) {
                    results.add(contact.DisplayName);
                    break;
                }
            }

            if (!results.isEmpty()) {
                StringBuilder builder = new StringBuilder();
                int i = 0;
                for (String s : results) {
                    if (i++ > 0) builder.append(", ");
                    builder.append(s);
                }
                return builder.toString();
            }
        }
        return null;
    }
}

package org.thanthoai.securesms.utils;

import android.util.Log;

import org.apache.log4j.Logger;
import org.thanthoai.securesms.SecureSmsApplication;

import java.util.List;

public class Global {

    public static final String DEFAULT_PASSPHRASE = "%1q2W3e4R5ta@_[}&#";
    public static final String AES_PREFIX = "$]";

    public static long sLastTimeLeave = 0;
    public static final long TIME_NEED_RE_AUTHENTICATE = 10000l; // 30 seconds
    public static final int AUTHENTICATE_REQUEST_CODE = 211;

    static final Logger sLogger = Logger.getLogger(SecureSmsApplication.class);

    public static boolean smartContains(String s1, String s2, List<Integer> matchedPos) {
        if (s2.length() > s1.length()) return false;
        else if (s2.length() == s1.length()) return s1.equals(s2);

        int i = -1;
        for (int j = 0; j < s2.length(); j++) {
            final String s = String.valueOf(s2.charAt(j));
            if ((i = s1.indexOf(s, i+ 1)) < 0) {
                return false;
            }
            if (matchedPos != null) {
                matchedPos.add(i);
            }
        }
        return true;
    }

    public static void log(Object obj) {
        log("SecureSMS-debug", obj);
    }

    public static void log(String tag, Object obj) {
        Log.i(tag, obj.toString());
        sLogger.info(obj);
    }

    public static void error(Object obj) {
        error("SecureSMS-error", obj);
    }

    public static void error(String tag, Object obj) {
        Log.e(tag, obj.toString());
        if (obj instanceof Throwable) {
            Throwable e = (Throwable) obj;
            sLogger.error(e.getMessage(), e);
        } else {
            sLogger.error(obj);
        }
    }
}

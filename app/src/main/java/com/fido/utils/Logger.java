package com.fido.utils;

import android.util.Log;
import java.util.Map;
import java.util.TreeMap;

public final class Logger {
	
	private static String apptag = "Finger_";
	
    public static final int LOG_IS_SWITCHED_OFF = 8;
    public static final int LOG_IS_SWITCHED_ON = 1;
    public static final int LOG_MAX_LEN = 4000;
    private static boolean logEnabled = true;
    private static Map<String, Integer> timersMap = new TreeMap();

    public Logger() {
    }
    
    public static void setTag(String mTag){
    	apptag = mTag;
    }

    public static boolean setLogEnabled(boolean aLogEnabled) {
        boolean temp = logEnabled;
        logEnabled = aLogEnabled;
        return temp;
    }

    public static void startTimer(String tag, String key) {
        if (logEnabled) {
            String mapKey = tag + key;
            if (!timersMap.containsKey(mapKey)) {
                timersMap.put(mapKey,
                        Integer.valueOf((int) System.currentTimeMillis()));
                Log.i(tag, "\"" + key + "\" timer started.");
            } else {
                Log.w(tag, "\"" + key + "\" timer is already running.");
            }
        }

    }

    public static void endTimer(String tag, String key) {
        if (logEnabled) {
            String mapKey = tag + key;
            if (timersMap.containsKey(mapKey)) {
                int diff = (int) System.currentTimeMillis()
                        - ((Integer) timersMap.get(mapKey)).intValue();
                timersMap.remove(mapKey);
                Log.i(tag, "End of \"" + key + "\" timer. Run in " + diff
                        + " millisecond(s)");
            } else {
                Log.w(tag, "There is no \"" + key + "\" timer running.");
            }
        }

    }


    public static int i(String tag, String msg) {
        byte level = 4;
        if(!logEnabled) {
            return 8;
        } else {
            int remainder = msg.length();
            if(remainder <= 4000) {
                Log.i(apptag+tag, msg);
            } else {
                int start = 0;
                int end = 0;

                do {
                    end += remainder > 4000?4000:remainder;
                    remainder -= end - start;
                    Log.i(tag, msg.substring(start, end));
                    start += 4000;
                } while(remainder > 0);
            }

            return level;
        }
    }

    public static int i(String tag, Throwable tr) {
        return i(apptag+tag, tr.toString());
    }

    public static int i(String tag, String msg, byte[] buffer) {
        return !logEnabled?8:i(apptag+tag, dumpBytes(msg, buffer));
    }

    public static int v(String tag, String msg) {
        byte level = 2;
        if(!logEnabled) {
            return 8;
        } else {
            int remainder = msg.length();
            if(remainder <= 4000) {
                Log.v(apptag+tag, msg);
            } else {
                int start = 0;
                int end = 0;

                do {
                    end += remainder > 4000?4000:remainder;
                    remainder -= end - start;
                    Log.v(apptag+tag, msg.substring(start, end));
                    start += 4000;
                } while(remainder > 0);
            }

            return level;
        }
    }

    public static int v(String tag, Throwable tr) {
        return v(apptag+tag, tr.toString());
    }

    public static int v(String tag, String msg, byte[] buffer) {
        return !logEnabled?8:v(apptag+tag, dumpBytes(msg, buffer));
    }

    public static int w(String tag, String msg) {
        byte level = 5;
        if(!logEnabled) {
            return 8;
        } else {
            int remainder = msg.length();
            if(remainder <= 4000) {
                Log.w(apptag+tag, msg);
            } else {
                int start = 0;
                int end = 0;

                do {
                    end += remainder > 4000?4000:remainder;
                    remainder -= end - start;
                    Log.w(apptag+tag, msg.substring(start, end));
                    start += 4000;
                } while(remainder > 0);
            }

            return level;
        }
    }

    public static int w(String tag, Throwable tr) {
        return w(tag, tr.toString());
    }

    public static int w(String tag, String msg, byte[] buffer) {
        return !logEnabled?8:w(apptag+tag, dumpBytes(msg, buffer));
    }

    public static int d(String tag, String msg) {
        byte level = 3;
        if(!logEnabled) {
            return 8;
        } else {
            int remainder = msg.length();
            if(remainder <= 4000) {
                Log.d(apptag+tag, msg);
            } else {
                int start = 0;
                int end = 0;

                do {
                    end += remainder > 4000?4000:remainder;
                    remainder -= end - start;
                    Log.d(apptag+tag, msg.substring(start, end));
                    start += 4000;
                } while(remainder > 0);
            }

            return level;
        }
    }

    public static int d(String tag, Throwable tr) {
        return d(tag, tr.toString());
    }

    public static int d(String tag, String msg, byte[] buffer) {
        return !logEnabled?8:Log.d(apptag+tag, dumpBytes(msg, buffer));
    }

    public static int e(String tag, String msg) {
        byte level = 6;
        if(!logEnabled) {
            return 8;
        } else {
            int remainder = msg.length();
            if(remainder <= 4000) {
                Log.e(apptag+tag, msg);
            } else {
                int start = 0;
                int end = 0;

                do {
                    end += remainder > 4000?4000:remainder;
                    remainder -= end - start;
                    Log.e(apptag+tag, msg.substring(start, end));
                    start += 4000;
                } while(remainder > 0);
            }

            return level;
        }
    }

    public static int e(String tag, Throwable tr) {
        return !logEnabled?8:Log.e(apptag+tag, "", tr);
    }

    public static int e(String tag, String message, Throwable tr) {
        return !logEnabled?8:Log.e(apptag+tag, message, tr);
    }

    public static int e(String tag, String msg, byte[] buffer) {
        return !logEnabled?8:e(apptag+tag, dumpBytes(msg, buffer));
    }

    private static String dumpBytes(String title, byte[] bytes) {
        String text = "";
        if(title != null) {
            text = text + title;
        }

        if(bytes == null) {
            text = text + ":null";
            return text;
        } else {
            boolean lineLength = true;
            int inLentgh = bytes.length;
            text = text + "(" + inLentgh + "):\n";

            for(int i = 0; i < inLentgh; i += 16) {
                text = text + String.format("%06x:", new Object[]{Integer.valueOf(i)});

                int j;
                for(j = 0; j < 16; ++j) {
                    if(i + j < inLentgh) {
                        text = text + String.format("%02x ", new Object[]{Integer.valueOf(bytes[i + j] & 255)});
                    } else {
                        text = text + "   ";
                    }
                }

                text = text + " ";

                for(j = 0; j < 16; ++j) {
                    if(i + j < inLentgh) {
                        char ch = (char)(bytes[i + j] & 255);
                        text = text + String.format("%c", new Object[]{Character.valueOf(ch >= 32 && ch <= 126?ch:'.')});
                    }
                }

                text = text + "\n";
            }

            return text;
        }
    }
}

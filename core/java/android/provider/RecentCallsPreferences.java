package android.provider;
 
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class RecentCallsPreferences {
    private SharedPreferences mSharedPreferences;
    private static RecentCallsPreferences preferences;
    private static SharedPreferences.Editor editor;

    private static final String PREFERENCES_NAME = "RecentCallsDuration";
    
    public static final String ALL_LAST_CALLS = "all_last_calls";
    public static final String ALL_INCOMING_CALLS = "all_incoming_calls";
    public static final String ALL_OUTGOING_CALLS = "all_outgoing_calls";
    public static final String ALL_TOTAL_CALLS = "all_total_calls";
/*
    public static final String CDMA_LAST_CALLS = "cdma_last_calls";
    public static final String CDMA_INCOMING_CALLS = "cdma_incoming_calls";
    public static final String CDMA_OUTGOING_CALLS = "cdma_outgoing_calls";
    public static final String CDMA_TOTAL_CALLS = "cdma_total_calls";

    public static final String GSM_LAST_CALLS = "gsm_last_calls";
    public static final String GSM_INCOMING_CALLS = "gsm_incoming_calls";
    public static final String GSM_OUTGOING_CALLS = "gsm_outgoing_calls";
    public static final String GSM_TOTAL_CALLS = "gsm_total_calls";
*/
    public static final String SUB1_LAST_CALLS = "sub1_last_calls";
    public static final String SUB1_INCOMING_CALLS = "sub1_incoming_calls";
    public static final String SUB1_OUTGOING_CALLS = "sub1_outgoing_calls";
    public static final String SUB1_TOTAL_CALLS = "sub1_total_calls";

    public static final String SUB2_LAST_CALLS = "sub2_last_calls";
    public static final String SUB2_INCOMING_CALLS = "sub2_incoming_calls";
    public static final String SUB2_OUTGOING_CALLS = "sub2_outgoing_calls";
    public static final String SUB2_TOTAL_CALLS = "sub2_total_calls";

    public RecentCallsPreferences(Context context) {
     
        mSharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
      
        editor = mSharedPreferences.edit();
       
        if(!(mSharedPreferences.contains(ALL_LAST_CALLS) || mSharedPreferences.contains(ALL_INCOMING_CALLS)
            || mSharedPreferences.contains(ALL_OUTGOING_CALLS) || mSharedPreferences.contains(ALL_TOTAL_CALLS)
            //|| mSharedPreferences.contains(CDMA_LAST_CALLS) || mSharedPreferences.contains(CDMA_INCOMING_CALLS)
            //|| mSharedPreferences.contains(CDMA_OUTGOING_CALLS) || mSharedPreferences.contains(CDMA_TOTAL_CALLS)
            //|| mSharedPreferences.contains(GSM_LAST_CALLS) || mSharedPreferences.contains(GSM_INCOMING_CALLS)
            //|| mSharedPreferences.contains(GSM_OUTGOING_CALLS) || mSharedPreferences.contains(GSM_TOTAL_CALLS)
            || mSharedPreferences.contains(SUB1_LAST_CALLS) || mSharedPreferences.contains(SUB1_INCOMING_CALLS)
            || mSharedPreferences.contains(SUB1_OUTGOING_CALLS) || mSharedPreferences.contains(SUB1_TOTAL_CALLS)
            || mSharedPreferences.contains(SUB2_LAST_CALLS) || mSharedPreferences.contains(SUB2_INCOMING_CALLS)
            || mSharedPreferences.contains(SUB2_OUTGOING_CALLS) || mSharedPreferences.contains(SUB2_TOTAL_CALLS)
            )) {

            editor.putLong(ALL_LAST_CALLS, 0);
            editor.putLong(ALL_INCOMING_CALLS, 0);
            editor.putLong(ALL_OUTGOING_CALLS, 0);
            editor.putLong(ALL_TOTAL_CALLS, 0);
            /*editor.putLong(CDMA_LAST_CALLS, 0);
            editor.putLong(CDMA_INCOMING_CALLS, 0);
            editor.putLong(CDMA_OUTGOING_CALLS, 0);
            editor.putLong(CDMA_TOTAL_CALLS, 0);
            editor.putLong(GSM_LAST_CALLS, 0);
            editor.putLong(GSM_INCOMING_CALLS, 0);
            editor.putLong(GSM_OUTGOING_CALLS, 0);
            editor.putLong(GSM_TOTAL_CALLS, 0);*/

            editor.putLong(SUB1_LAST_CALLS, 0);
            editor.putLong(SUB1_INCOMING_CALLS, 0);
            editor.putLong(SUB1_OUTGOING_CALLS, 0);
            editor.putLong(SUB1_TOTAL_CALLS, 0);
            editor.putLong(SUB2_LAST_CALLS, 0);
            editor.putLong(SUB2_INCOMING_CALLS, 0);
            editor.putLong(SUB2_OUTGOING_CALLS, 0);
            editor.putLong(SUB2_TOTAL_CALLS, 0);
            editor.commit();
        }
    }

    public static synchronized RecentCallsPreferences getPreferences(Context context) {
       
        if (preferences == null) {
            preferences = new RecentCallsPreferences(context);
        }
       
        return preferences;
    }
     
    public void setLong(String key, long value) {
       
        editor.putLong(key, value);
        editor.commit();
       
    }
   
    public long getLong(String key) {
      
        return mSharedPreferences.getLong(key, 0);
        
    }   
}

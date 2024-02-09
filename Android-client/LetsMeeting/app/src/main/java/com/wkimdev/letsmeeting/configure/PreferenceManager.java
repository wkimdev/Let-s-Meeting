package com.wkimdev.letsmeeting.configure;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * SharedPreferences 데이터 저장 및 로드 클래스
 */
public class PreferenceManager {

    public static final String PREFERENCES_NAME = "pref";

    private static final String DEFAULT_VALUE_STRING = " ";
    private static final boolean DEFAULT_VALUE_BOOLEAN = false;
    private static final int DEFAULT_VALUE_INT = -1;
    private static final long DEFAULT_VALUE_LONG = -1L;
    private static final float DEFAULT_VALUE_FLOAT = -1F;

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREFERENCES_NAME, context.MODE_PRIVATE);
    }

    /* String 값 저장
       param context
       param key
       param value
     */
    public static void setString(Context context, String key, String value){
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.apply();
    }

     /* boolean 값 저장
       param context
       param key
       param value
     */
    public static void setBoolean(Context context, String key, boolean value){
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    /* int 값 저장
       param context
       param key
       param value
     */
    public static void setInt(Context context, String key, int value){
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    /* long 값 저장
       param context
       param key
       param value
     */
    public static void setLong(Context context, String key, long value){
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(key, value);
        editor.apply();
    }

    /* float 값 저장
       param context
       param key
       param value
     */
    public static void setFloat(Context context, String key, float value){
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat(key, value);
        editor.apply();
    }

    /* arrayList 값 저장
       param context
       param key
       param ArrayList<String>
     */
    public static void setArrayList(Context context, String key, ArrayList<String> value) {
        SharedPreferences pref = getPreferences(context);
        SharedPreferences.Editor edit = pref.edit();

        Set<String> set = new HashSet<String>();
        set.addAll(value);

        edit.putStringSet(key, set);
        edit.commit();
    }


    /* String 값 로드
       param context
       param key
       return
     */
    public static String getString(Context context, String key){
        SharedPreferences prefs = getPreferences(context);
        String value = prefs.getString(key, DEFAULT_VALUE_STRING);
        return value;
    }

    /* boolean 값 로드
       param context
       param key
       return
     */
    public static boolean getBoolean(Context context, String key){
        SharedPreferences prefs = getPreferences(context);
        boolean value = prefs.getBoolean(key, DEFAULT_VALUE_BOOLEAN);
        return value;
    }

     /* int 값 로드
       param context
       param key
       return
     */
    public static int getInt(Context context, String key){
        SharedPreferences prefs = getPreferences(context);
        int value = prefs.getInt(key, DEFAULT_VALUE_INT);
        return value;
    }

    /* long 값 로드
      param context
      param key
      return
    */
    public static long getLong(Context context, String key){
        SharedPreferences prefs = getPreferences(context);
        long value = prefs.getLong(key, DEFAULT_VALUE_LONG);
        return value;
    }

    /* long 값 로드
      param context
      param key
      return
    */
    public static float getFloat(Context context, String key){
        SharedPreferences prefs = getPreferences(context);
        float value = prefs.getFloat(key, DEFAULT_VALUE_FLOAT);
        return value;
    }


    /* arrayList 값 로드
      param context
      param key
      return
    */
    public static ArrayList<String> getArrayList(Context context, String key) {
        SharedPreferences pref = getPreferences(context);
        Set<String> set = pref.getStringSet(key, null);
        return new ArrayList<String>(set);
    }


    /* 키 값 삭제
      param context
      param key
    */
    public static void removeKey(Context context, String key){
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(key);
        editor.commit();
    }

    /* 모든 저장 데이터 삭제
      param context
    */
    public static void clear(Context context){
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.commit();
    }

    public static void listData(Context context, String datalist, TextView tvlist) {
        SharedPreferences prefs = getPreferences(context);
        Map<String, ?> totalValue = prefs.getAll();
        for (Map.Entry<String, ?> entry : totalValue.entrySet()) {
            datalist += entry.getKey().toString() + ": " + entry.getValue().toString() + "\r\n";
        }
        tvlist.setText(datalist);
    }

}



package org.sedki.locationcheckin;

import android.content.Context;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.List;

public final class CenterStore {
    private static final String PREF="center_store_v24", KEY="centers";
    public static void save(Context c, List<HealthCenter> items) throws Exception {
        JSONArray a=new JSONArray(); for(HealthCenter h:items) a.put(h.toJson());
        c.getSharedPreferences(PREF,0).edit().putString(KEY,a.toString()).apply();
    }
    public static List<HealthCenter> load(Context c) {
        ArrayList<HealthCenter> out=new ArrayList<>();
        try {
            String s=c.getSharedPreferences(PREF,0).getString(KEY,"[]"); JSONArray a=new JSONArray(s);
            for(int i=0;i<a.length();i++) out.add(HealthCenter.fromJson(a.getJSONObject(i)));
        } catch(Exception ignored) {}
        return out;
    }
}

package org.sedki.locationcheckin;

import org.json.JSONException;
import org.json.JSONObject;

public final class HealthCenter {
    public final String name;
    public final double latitude;
    public final double longitude;
    public final double radiusMeters;

    public HealthCenter(String name, double latitude, double longitude, double radiusMeters) {
        this.name = name; this.latitude = latitude; this.longitude = longitude;
        this.radiusMeters = radiusMeters > 0 ? radiusMeters : 100.0;
    }
    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("name", name); o.put("lat", latitude); o.put("lon", longitude); o.put("radius", radiusMeters);
        return o;
    }
    public static HealthCenter fromJson(JSONObject o) throws JSONException {
        return new HealthCenter(o.getString("name"), o.getDouble("lat"), o.getDouble("lon"), o.optDouble("radius",100));
    }
    @Override public String toString() { return name; }
}

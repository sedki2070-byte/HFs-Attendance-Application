package org.sedki.locationcheckin;

import org.json.JSONException;
import org.json.JSONObject;

public final class HealthCenter {
    public static final double FIXED_ALLOWED_RADIUS_METERS = 20.0;

    public final String name;
    public final double latitude;
    public final double longitude;
    public final double radiusMeters;

    public HealthCenter(String name, double latitude, double longitude, double radiusMeters) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        // Attendance is always restricted to a fixed 20-meter radius,
        // regardless of any radius value supplied in the Excel file.
        this.radiusMeters = FIXED_ALLOWED_RADIUS_METERS;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("name", name);
        o.put("lat", latitude);
        o.put("lon", longitude);
        o.put("radius", FIXED_ALLOWED_RADIUS_METERS);
        return o;
    }

    public static HealthCenter fromJson(JSONObject o) throws JSONException {
        return new HealthCenter(
                o.getString("name"),
                o.getDouble("lat"),
                o.getDouble("lon"),
                FIXED_ALLOWED_RADIUS_METERS
        );
    }

    @Override public String toString() { return name; }
}

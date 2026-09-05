package org.sedki.locationcheckin;

import android.content.Intent;
import android.net.Uri;
import android.os.Parcelable;

public class MainActivityV37 extends MainActivity {
    private boolean transforming=false;

    @Override public void startActivity(Intent intent) {
        if (!transforming && Intent.ACTION_CHOOSER.equals(intent.getAction())) {
            try {
                Parcelable p=intent.getParcelableExtra(Intent.EXTRA_INTENT);
                if (p instanceof Intent) {
                    Intent target=(Intent)p;
                    if (Intent.ACTION_SENDTO.equals(target.getAction()) && target.getData()!=null && "mailto".equals(target.getData().getScheme())) {
                        String body=target.getStringExtra(Intent.EXTRA_TEXT);
                        String[] lines=body==null?new String[0]:body.split("\\n");
                        String employee=value(lines,"Employee Name: ");
                        String facility=value(lines,"Health Facility / Location: ");
                        String type=value(lines,"Attendance Type: ");
                        String date=value(lines,"Date & Time: ");
                        double lat=number(value(lines,"Latitude: "));
                        double lon=number(value(lines,"Longitude: "));
                        String distance=value(lines,"Distance from HF: ");
                        Uri xlsx=AttendanceExcelExporter.create(this,employee,facility,type,date,lat,lon,distance);

                        Intent send=new Intent(Intent.ACTION_SEND);
                        send.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                        send.putExtra(Intent.EXTRA_EMAIL,target.getStringArrayExtra(Intent.EXTRA_EMAIL));
                        send.putExtra(Intent.EXTRA_SUBJECT,target.getStringExtra(Intent.EXTRA_SUBJECT));
                        send.putExtra(Intent.EXTRA_TEXT,body);
                        send.putExtra(Intent.EXTRA_STREAM,xlsx);
                        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        Intent chooser=Intent.createChooser(send, getPreferences(0).getBoolean("arabic_ui",false)?"إرسال بيانات الحضور مع ملف Excel":"Send attendance data with Excel file");
                        transforming=true;
                        super.startActivity(chooser);
                        transforming=false;
                        return;
                    }
                }
            } catch (Exception ignored) { transforming=false; }
        }
        super.startActivity(intent);
    }

    private static String value(String[] lines,String prefix){ for(String s:lines)if(s.startsWith(prefix))return s.substring(prefix.length()).trim(); return ""; }
    private static double number(String s){ try{return Double.parseDouble(s);}catch(Exception e){return 0;} }
}

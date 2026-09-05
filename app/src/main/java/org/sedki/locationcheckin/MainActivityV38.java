package org.sedki.locationcheckin;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.InputType;
import android.widget.EditText;
import java.io.File;

public class MainActivityV38 extends MainActivity {
    private static final String FIXED_EMAIL="sedki2070@gmail.com";
    private boolean intercepting=false;

    @Override public void onCreate(Bundle b){
        // Fixed sender/recipient so no email address is requested in the app UI.
        getPreferences(0).edit()
                .putString("recipient_email",FIXED_EMAIL)
                .putString("smtp_sender",FIXED_EMAIL)
                .apply();
        super.onCreate(b);
        if(!hasSenderConfig()) showSenderSetup();
    }

    private boolean hasSenderConfig(){
        return !getPreferences(0).getString("smtp_app_password","").trim().isEmpty();
    }

    private void showSenderSetup(){
        final EditText pass=new EditText(this);
        pass.setHint(getPreferences(0).getBoolean("arabic_ui",false)?"كلمة مرور تطبيق Gmail":"Gmail App Password");
        pass.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        int pad=(int)(20*getResources().getDisplayMetrics().density);
        pass.setPadding(pad,pad/2,pad,pad/2);

        AlertDialog d=new AlertDialog.Builder(this)
                .setTitle(getPreferences(0).getBoolean("arabic_ui",false)?"إعداد الإرسال التلقائي":"Automatic Email Setup")
                .setMessage(getPreferences(0).getBoolean("arabic_ui",false)?
                        "سيتم إرسال ملف Excel تلقائياً إلى sedki2070@gmail.com. أدخل كلمة مرور التطبيق الخاصة بحساب Gmail هذا مرة واحدة. لا تستخدم كلمة مرور Gmail العادية.":
                        "Excel files will be sent automatically to sedki2070@gmail.com. Enter the Gmail App Password for this account once. Do not use the normal Gmail password.")
                .setView(pass)
                .setCancelable(false)
                .setPositiveButton(getPreferences(0).getBoolean("arabic_ui",false)?"حفظ":"Save",null)
                .create();
        d.setOnShowListener(x->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String p=pass.getText().toString().replace(" ","").trim();
            if(p.length()<12){pass.setError(getPreferences(0).getBoolean("arabic_ui",false)?"كلمة مرور التطبيق غير صحيحة":"Invalid App Password");return;}
            getPreferences(0).edit()
                    .putString("recipient_email",FIXED_EMAIL)
                    .putString("smtp_sender",FIXED_EMAIL)
                    .putString("smtp_app_password",p)
                    .apply();
            d.dismiss();
        }));
        d.show();
    }

    @Override public void startActivity(Intent intent){
        if(!intercepting && Intent.ACTION_CHOOSER.equals(intent.getAction())){
            try{
                Parcelable p=intent.getParcelableExtra(Intent.EXTRA_INTENT);
                if(p instanceof Intent){
                    Intent target=(Intent)p;
                    if(Intent.ACTION_SENDTO.equals(target.getAction()) && target.getData()!=null && "mailto".equals(target.getData().getScheme())){
                        handleAutomaticSend(target);
                        return;
                    }
                    if(Intent.ACTION_SEND.equals(target.getAction())){
                        handleAutomaticSend(target);
                        return;
                    }
                }
            }catch(Exception ignored){}
        }
        super.startActivity(intent);
    }

    private void handleAutomaticSend(Intent target){
        final String recipient=FIXED_EMAIL;
        final String subject=target.getStringExtra(Intent.EXTRA_SUBJECT);
        final String body=target.getStringExtra(Intent.EXTRA_TEXT);
        if(!hasSenderConfig()){showSenderSetup();return;}

        new Thread(()->{
            try{
                String[] lines=body==null?new String[0]:body.split("\\n");
                String employee=value(lines,"Employee Name: ");
                String facility=value(lines,"Health Facility / Location: ");
                String type=value(lines,"Attendance Type: ");
                String date=value(lines,"Date & Time: ");
                double lat=number(value(lines,"Latitude: "));
                double lon=number(value(lines,"Longitude: "));
                String distance=value(lines,"Distance from HF: ");
                Uri ignored=AttendanceExcelExporter.create(this,employee,facility,type,date,lat,lon,distance);
                File dir=new File(getCacheDir(),"attendance_exports");
                File[] files=dir.listFiles((d,n)->n.endsWith(".xlsx"));
                if(files==null||files.length==0) throw new Exception("Excel file was not created");
                File newest=files[0];
                for(File f:files) if(f.lastModified()>newest.lastModified()) newest=f;
                String appPassword=getPreferences(0).getString("smtp_app_password","").replace(" ","").trim();
                SmtpMailer.sendGmail(FIXED_EMAIL,appPassword,recipient,subject==null?"HFs Attendance":subject,body==null?"":body,newest);
                runOnUiThread(()->android.widget.Toast.makeText(this,getPreferences(0).getBoolean("arabic_ui",false)?"تم إرسال البريد وملف Excel تلقائياً إلى sedki2070@gmail.com":"Email and Excel file sent automatically to sedki2070@gmail.com",android.widget.Toast.LENGTH_LONG).show());
            }catch(Exception e){
                runOnUiThread(()->{
                    android.widget.Toast.makeText(this,(getPreferences(0).getBoolean("arabic_ui",false)?"فشل الإرسال التلقائي: ":"Automatic email failed: ")+e.getMessage(),android.widget.Toast.LENGTH_LONG).show();
                    getPreferences(0).edit().remove("smtp_app_password").apply();
                    showSenderSetup();
                });
            }
        }).start();
    }

    private static String value(String[] lines,String prefix){for(String s:lines)if(s.startsWith(prefix))return s.substring(prefix.length()).trim();return "";}
    private static double number(String s){try{return Double.parseDouble(s);}catch(Exception e){return 0;}}
}

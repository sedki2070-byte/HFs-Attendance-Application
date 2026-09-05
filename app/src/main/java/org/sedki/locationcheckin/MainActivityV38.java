package org.sedki.locationcheckin;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.InputType;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.LinearLayout;
import java.io.File;

public class MainActivityV38 extends MainActivity {
    private boolean intercepting=false;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        if(!hasSenderConfig()) showSenderSetup();
    }

    private boolean hasSenderConfig(){
        return !getPreferences(0).getString("smtp_sender","").trim().isEmpty()
                && !getPreferences(0).getString("smtp_app_password","").trim().isEmpty();
    }

    private void showSenderSetup(){
        final EditText sender=new EditText(this);
        sender.setHint("Sender Gmail (example@gmail.com)");
        sender.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        final EditText pass=new EditText(this);
        pass.setHint("Gmail App Password");
        pass.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad=(int)(16*getResources().getDisplayMetrics().density);
        box.setPadding(pad,pad/2,pad,pad/2);
        box.addView(sender);
        box.addView(pass);
        new AlertDialog.Builder(this)
                .setTitle(getPreferences(0).getBoolean("arabic_ui",false)?"إعداد الإرسال التلقائي":"Automatic Email Setup")
                .setMessage(getPreferences(0).getBoolean("arabic_ui",false)?"أدخل بريد Gmail المُرسِل وكلمة مرور التطبيق. سيتم حفظها على هذا الهاتف واستخدامها لإرسال ملف Excel تلقائياً.":"Enter the sender Gmail and its App Password. They will be saved on this phone and used for automatic Excel email delivery.")
                .setView(box)
                .setCancelable(false)
                .setPositiveButton(getPreferences(0).getBoolean("arabic_ui",false)?"حفظ":"Save",null)
                .create();
        AlertDialog d=new AlertDialog.Builder(this)
                .setTitle(getPreferences(0).getBoolean("arabic_ui",false)?"إعداد الإرسال التلقائي":"Automatic Email Setup")
                .setMessage(getPreferences(0).getBoolean("arabic_ui",false)?"أدخل بريد Gmail المُرسِل وكلمة مرور التطبيق. لا تستخدم كلمة مرور Gmail العادية.":"Enter the sender Gmail and its Gmail App Password. Do not use the normal Gmail password.")
                .setView(box)
                .setCancelable(false)
                .setPositiveButton(getPreferences(0).getBoolean("arabic_ui",false)?"حفظ":"Save",null)
                .create();
        d.setOnShowListener(x->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String s=sender.getText().toString().trim();
            String p=pass.getText().toString().replace(" ","").trim();
            if(!Patterns.EMAIL_ADDRESS.matcher(s).matches()){sender.setError("Invalid email");return;}
            if(p.length()<12){pass.setError("Invalid App Password");return;}
            getPreferences(0).edit().putString("smtp_sender",s).putString("smtp_app_password",p).apply();
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
        final String[] to=target.getStringArrayExtra(Intent.EXTRA_EMAIL);
        final String recipient=(to!=null&&to.length>0)?to[0]:getPreferences(0).getString("recipient_email","").trim();
        final String subject=target.getStringExtra(Intent.EXTRA_SUBJECT);
        final String body=target.getStringExtra(Intent.EXTRA_TEXT);
        if(recipient.isEmpty()){return;}
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
                String sender=getPreferences(0).getString("smtp_sender","").trim();
                String appPassword=getPreferences(0).getString("smtp_app_password","").replace(" ","").trim();
                SmtpMailer.sendGmail(sender,appPassword,recipient,subject==null?"HFs Attendance":subject,body==null?"":body,newest);
                runOnUiThread(()->android.widget.Toast.makeText(this,getPreferences(0).getBoolean("arabic_ui",false)?"تم إرسال البريد وملف Excel تلقائياً":"Email and Excel file sent automatically",android.widget.Toast.LENGTH_LONG).show());
            }catch(Exception e){
                runOnUiThread(()->{
                    android.widget.Toast.makeText(this,(getPreferences(0).getBoolean("arabic_ui",false)?"فشل الإرسال التلقائي: ":"Automatic email failed: ")+e.getMessage(),android.widget.Toast.LENGTH_LONG).show();
                    showSenderSetup();
                });
            }
        }).start();
    }

    private static String value(String[] lines,String prefix){for(String s:lines)if(s.startsWith(prefix))return s.substring(prefix.length()).trim();return "";}
    private static double number(String s){try{return Double.parseDouble(s);}catch(Exception e){return 0;}}
}

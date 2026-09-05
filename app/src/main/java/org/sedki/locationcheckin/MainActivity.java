package org.sedki.locationcheckin;

import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.*;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import android.util.Patterns;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private static final int PICK_XLSX=90, REQ_LOC=91;
    private static final long FIVE_HOURS=5L*60*60*1000;
    private EditText name, emailInput;
    private Spinner centers;
    private TextView status, imported;
    private Button action, saveEmail, importButton;
    private LinearLayout emailSetup, facilitySetup;
    private List<HealthCenter> list=new ArrayList<>();
    private final SimpleDateFormat dayFmt=new SimpleDateFormat("yyyy-MM-dd",Locale.US);

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        dayFmt.setTimeZone(TimeZone.getTimeZone("Asia/Aden"));
        buildUi();
        loadCenters();
        loadIdentity();
        loadEmailSetup();
    }

    private void buildUi(){
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20),dp(20),dp(20),dp(16));
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView title=t("HFs Attendance Application",26,true);
        root.addView(title,mp(-1,-2));

        TextView hint=t("اكتب الإسم واختر اسم المركز الصحي. الضغطة الأولى لتسجيل الحضور والضغط الثاني لتسجيل الإنصراف بعد مرور خمس ساعات على الأقل.",15,false);
        hint.setGravity(Gravity.CENTER);
        root.addView(hint,margin(-1,-2,0,12));

        emailSetup=new LinearLayout(this);
        emailSetup.setOrientation(LinearLayout.VERTICAL);
        TextView emailLabel=t("إعداد البريد الإلكتروني لأول مرة",15,true);
        emailLabel.setGravity(Gravity.CENTER);
        emailSetup.addView(emailLabel,margin(-1,-2,0,6));
        emailInput=new EditText(this);
        emailInput.setHint("أدخل البريد الإلكتروني لاستقبال البيانات");
        emailInput.setSingleLine(true);
        emailInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailSetup.addView(emailInput,margin(-1,-2,0,6));
        saveEmail=new Button(this);
        saveEmail.setText("حفظ البريد الإلكتروني");
        saveEmail.setOnClickListener(v->saveRecipientEmail());
        emailSetup.addView(saveEmail,margin(-1,-2,0,12));
        root.addView(emailSetup,mp(-1,-2));

        facilitySetup=new LinearLayout(this);
        facilitySetup.setOrientation(LinearLayout.VERTICAL);
        TextView facilityLabel=t("إعداد المراكز الصحية لأول مرة",15,true);
        facilityLabel.setGravity(Gravity.CENTER);
        facilitySetup.addView(facilityLabel,margin(-1,-2,0,6));
        importButton=new Button(this);
        importButton.setText("إضافة ملف المراكز الصحية");
        importButton.setOnClickListener(v->pickExcel());
        facilitySetup.addView(importButton,margin(-1,-2,0,6));
        imported=t("لا توجد مراكز مستوردة",13,false);
        imported.setGravity(Gravity.CENTER);
        facilitySetup.addView(imported,margin(-1,-2,0,12));
        root.addView(facilitySetup,mp(-1,-2));

        name=new EditText(this);
        name.setHint("اكتب الاسم الكامل");
        name.setSingleLine(true);
        root.addView(name,margin(-1,-2,0,8));

        centers=new Spinner(this);
        root.addView(centers,margin(-1,dp(52),0,8));

        action=new Button(this);
        action.setText("تسجيل الحضور");
        action.setOnClickListener(v->beginAttendance());
        root.addView(action,margin(-1,dp(56),0,10));

        status=t("جاهز",15,true);
        status.setGravity(Gravity.CENTER);
        root.addView(status,margin(-1,-2,0,22));

        TextView dev=t("Developed by Sedki Yassen",12,false);
        dev.setGravity(Gravity.CENTER);
        root.addView(dev,mp(-1,-2));

        ScrollView sc=new ScrollView(this);
        sc.addView(root);
        setContentView(sc);
    }

    private TextView t(String s,int sp,boolean bold){
        TextView v=new TextView(this);
        v.setText(s);
        v.setTextSize(sp);
        v.setTextColor(Color.rgb(35,55,65));
        if(bold)v.setTypeface(null,1);
        return v;
    }

    private LinearLayout.LayoutParams mp(int w,int h){return new LinearLayout.LayoutParams(w,h);}
    private LinearLayout.LayoutParams margin(int w,int h,int l,int top){LinearLayout.LayoutParams p=mp(w,h);p.topMargin=dp(top);return p;}
    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}

    private void pickExcel(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        startActivityForResult(i,PICK_XLSX);
    }

    @Override protected void onActivityResult(int r,int c,Intent d){
        super.onActivityResult(r,c,d);
        if(r==PICK_XLSX&&c==RESULT_OK&&d!=null){
            Uri u=d.getData();
            if(u!=null) importExcel(u);
        }
    }

    private void importExcel(Uri u){
        status.setText("جارٍ قراءة ملف Excel...");
        new Thread(()->{
            try{
                List<HealthCenter>x=XlsxCenterReader.read(getContentResolver(),u);
                if(x==null||x.isEmpty()) throw new Exception("لم يتم العثور على مراكز صحيحة في الملف");
                CenterStore.save(this,x);
                runOnUiThread(()->{
                    list=x;
                    refreshSpinner();
                    imported.setText("تم استيراد "+x.size()+" مركزًا صحيًا بنجاح");
                    facilitySetup.setVisibility(View.GONE);
                    status.setText("تم حفظ المراكز الصحية بنجاح");
                });
            }catch(Exception e){
                runOnUiThread(()->status.setText("تعذر الاستيراد: "+e.getMessage()));
            }
        }).start();
    }

    private void loadEmailSetup(){
        String email=getPreferences(0).getString("recipient_email","").trim();
        emailSetup.setVisibility(email.isEmpty()?View.VISIBLE:View.GONE);
        if(email.isEmpty()) status.setText("يرجى إدخال البريد الإلكتروني لاستقبال البيانات");
    }

    private void saveRecipientEmail(){
        String email=emailInput.getText().toString().trim();
        if(email.isEmpty()||!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            status.setText("يرجى إدخال بريد إلكتروني صحيح");
            return;
        }
        getPreferences(0).edit().putString("recipient_email",email).apply();
        emailInput.setText("");
        emailSetup.setVisibility(View.GONE);
        status.setText("تم حفظ البريد الإلكتروني بنجاح");
    }

    private boolean hasRecipientEmail(){
        return !getPreferences(0).getString("recipient_email","").trim().isEmpty();
    }

    private void sendAttendanceEmail(String n,HealthCenter hc,Location loc,String type,float distance){
        String recipient=getPreferences(0).getString("recipient_email","").trim();
        if(recipient.isEmpty())return;
        SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("Asia/Aden"));
        String subject="HFs Attendance - "+type+" - "+n;
        String body="Employee Name: "+n+"\nHealth Facility: "+hc.name+"\nAttendance Type: "+type+"\nDate & Time: "+f.format(new Date())+"\nLatitude: "+loc.getLatitude()+"\nLongitude: "+loc.getLongitude()+"\nDistance from HF: "+Math.round(distance)+" m";
        try{
            Intent i=new Intent(Intent.ACTION_SENDTO);
            i.setData(Uri.parse("mailto:"+Uri.encode(recipient)));
            i.putExtra(Intent.EXTRA_EMAIL,new String[]{recipient});
            i.putExtra(Intent.EXTRA_SUBJECT,subject);
            i.putExtra(Intent.EXTRA_TEXT,body);
            startActivity(Intent.createChooser(i,"إرسال بيانات الحضور"));
        }catch(Exception e){status.setText(status.getText()+" • تعذر فتح تطبيق البريد");}
    }

    private void loadCenters(){
        list=CenterStore.load(this);
        refreshSpinner();
        boolean hasCenters=!list.isEmpty();
        facilitySetup.setVisibility(hasCenters?View.GONE:View.VISIBLE);
        if(!hasCenters){
            imported.setText("لا توجد مراكز مستوردة");
            status.setText("يرجى إضافة ملف المراكز الصحية لأول مرة");
        }
    }

    private void refreshSpinner(){
        ArrayList<String>a=new ArrayList<>();
        if(list.isEmpty()) a.add("يرجى إدخال ملف المراكز الصحية أولًا");
        else for(HealthCenter h:list)a.add(h.name);
        centers.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,a));
    }

    private void loadIdentity(){
        String n=getPreferences(0).getString("identity_name","");
        if(!n.isEmpty()){
            name.setText(n);
            name.setEnabled(false);
        }
        updateAction();
    }

    private void updateAction(){
        SharedPreferences p=getPreferences(0);
        String today=dayFmt.format(new Date());
        long first=p.getLong("first_press_time",0);
        String d=p.getString("state_day","");
        action.setText(today.equals(d)&&first>0?"تسجيل الإنصراف":"تسجيل الحضور");
    }

    private void beginAttendance(){
        if(!hasRecipientEmail()){
            emailSetup.setVisibility(View.VISIBLE);
            status.setText("يرجى إدخال البريد الإلكتروني أولًا");
            return;
        }
        if(list.isEmpty()){
            facilitySetup.setVisibility(View.VISIBLE);
            status.setText("يرجى إضافة ملف المراكز الصحية أولًا");
            return;
        }
        String n=name.getText().toString().trim();
        if(n.isEmpty()){
            status.setText("يرجى كتابة الاسم");
            return;
        }
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},REQ_LOC);
            return;
        }
        captureLocation(n);
    }

    @Override public void onRequestPermissionsResult(int r,String[]p,int[]g){
        super.onRequestPermissionsResult(r,p,g);
        if(r==REQ_LOC&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)beginAttendance();
        else status.setText("صلاحية الموقع مطلوبة للتسجيل");
    }

    private void captureLocation(String n){
        HealthCenter hc=list.get(centers.getSelectedItemPosition());
        LocationManager lm=(LocationManager)getSystemService(LOCATION_SERVICE);
        Location best=null;
        try{
            for(String pr:lm.getProviders(true)){
                Location l=lm.getLastKnownLocation(pr);
                if(l!=null&&(best==null||l.getAccuracy()<best.getAccuracy()))best=l;
            }
        }catch(SecurityException ignored){}
        if(best!=null&&best.getAccuracy()<=100){finishAttendance(n,hc,best);return;}
        status.setText("جارٍ تحديد الموقع...");
        final LocationListener[] box=new LocationListener[1];
        box[0]=new LocationListener(){
            public void onLocationChanged(Location l){
                if(l.getAccuracy()<=100){
                    try{lm.removeUpdates(box[0]);}catch(Exception ignored){}
                    finishAttendance(n,hc,l);
                }
            }
            public void onStatusChanged(String p,int s,Bundle e){}
            public void onProviderEnabled(String p){}
            public void onProviderDisabled(String p){}
        };
        try{lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,0,box[0]);}
        catch(Exception e){status.setText("تعذر الوصول إلى GPS");}
    }

    private void finishAttendance(String n,HealthCenter hc,Location loc){
        float[]res=new float[1];
        Location.distanceBetween(loc.getLatitude(),loc.getLongitude(),hc.latitude,hc.longitude,res);
        if(res[0]>hc.radiusMeters){
            status.setText("أنت خارج نطاق المركز: "+Math.round(res[0])+" متر");
            return;
        }
        SharedPreferences p=getPreferences(0);
        String today=dayFmt.format(new Date());
        String sd=p.getString("state_day","");
        long first=p.getLong("first_press_time",0),now=System.currentTimeMillis();
        if(!today.equals(sd)){
            first=0;
            p.edit().putString("state_day",today).remove("first_press_time").apply();
        }
        if(first==0){
            p.edit().putLong("first_press_time",now).putString("identity_name",n).apply();
            name.setEnabled(false);
            status.setText("تم تسجيل الحضور بنجاح • "+hc.name);
            sendAttendanceEmail(n,hc,loc,"Check-in",res[0]);
        }else{
            long elapsed=now-first;
            if(elapsed<FIVE_HOURS){
                long mins=(FIVE_HOURS-elapsed+59999)/60000;
                status.setText("لا يمكن تسجيل الإنصراف الآن. المتبقي تقريبًا "+mins+" دقيقة");
                return;
            }
            p.edit().remove("first_press_time").putString("state_day",today).apply();
            status.setText("تم تسجيل الإنصراف بنجاح • "+hc.name);
            sendAttendanceEmail(n,hc,loc,"Check-out",res[0]);
        }
        updateAction();
    }
}

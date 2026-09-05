package org.sedki.locationcheckin;

import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.*;
import android.net.Uri;
import android.os.Bundle;
import android.util.Patterns;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private static final int PICK_XLSX=90, REQ_LOC=91;
    private static final long FIVE_HOURS=5L*60*60*1000;
    private static final int NAVY=Color.rgb(15,43,93), TEAL=Color.rgb(0,167,174), BLUE=Color.rgb(18,123,234);
    private EditText name, emailInput;
    private Spinner centers;
    private TextView status, imported, checkInLabel, checkOutLabel;
    private LinearLayout emailSetup, facilitySetup;
    private List<HealthCenter> list=new ArrayList<>();
    private final SimpleDateFormat dayFmt=new SimpleDateFormat("yyyy-MM-dd",Locale.US);
    private String requestedAction="";

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        dayFmt.setTimeZone(TimeZone.getTimeZone("Asia/Aden"));
        buildUi();
        loadCenters();
        loadIdentity();
        loadEmailSetup();
        updateActionCards();
    }

    private void buildUi(){
        ScrollView sc=new ScrollView(this);
        sc.setFillViewport(true);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16),dp(14),dp(16),dp(22));
        root.setBackgroundColor(Color.rgb(244,250,253));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        LinearLayout header=new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16),dp(14),dp(16),dp(14));
        header.setBackground(gradient(new int[]{Color.rgb(0,166,171),Color.rgb(0,142,180)},18));
        TextView menu=t("☰",25,true,Color.WHITE); header.addView(menu,mp(dp(36),-2));
        TextView appTitle=t("HFs Attendance",20,true,Color.WHITE); appTitle.setGravity(Gravity.CENTER); header.addView(appTitle,new LinearLayout.LayoutParams(0,-2,1));
        TextView pin=t("●",18,true,Color.WHITE); pin.setGravity(Gravity.CENTER); header.addView(pin,mp(dp(36),-2));
        root.addView(header,margin(-1,-2,0,0,0,16));

        LinearLayout welcome=card(Color.WHITE,18);
        welcome.setOrientation(LinearLayout.HORIZONTAL);
        welcome.setGravity(Gravity.CENTER_VERTICAL);
        TextView avatar=iconBubble("●",Color.rgb(29,159,217),Color.rgb(233,248,255));
        welcome.addView(avatar,margin(dp(52),dp(52),0,0,12,0));
        LinearLayout wtxt=new LinearLayout(this); wtxt.setOrientation(LinearLayout.VERTICAL);
        TextView wt=t("Welcome",18,true,NAVY); wtxt.addView(wt);
        TextView ws=t("Ready to check in",14,false,Color.rgb(93,111,139)); wtxt.addView(ws);
        welcome.addView(wtxt,new LinearLayout.LayoutParams(0,-2,1));
        root.addView(welcome,margin(-1,-2,0,0,0,14));

        emailSetup=card(Color.WHITE,18); emailSetup.setOrientation(LinearLayout.VERTICAL); emailSetup.setPadding(dp(16),dp(14),dp(16),dp(14));
        emailSetup.addView(sectionTitle("✉  Enter Email Address"));
        emailInput=new EditText(this); emailInput.setHint("example@domain.com"); emailInput.setSingleLine(true); emailInput.setTextColor(NAVY); emailInput.setHintTextColor(Color.rgb(145,158,177));
        emailInput.setBackground(inputBg()); emailInput.setPadding(dp(12),0,dp(12),0); emailInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailSetup.addView(emailInput,margin(-1,dp(50),8,0,0,10));
        Button saveEmail=primaryButton("Save & Continue"); saveEmail.setOnClickListener(v->saveRecipientEmail()); emailSetup.addView(saveEmail,mp(-1,dp(50)));
        root.addView(emailSetup,margin(-1,-2,0,0,0,14));

        facilitySetup=card(Color.WHITE,18); facilitySetup.setOrientation(LinearLayout.VERTICAL); facilitySetup.setPadding(dp(16),dp(14),dp(16),dp(14));
        facilitySetup.addView(sectionTitle("▣  Import Health Facilities (Excel File)"));
        Button importButton=primaryButton("Select File"); importButton.setOnClickListener(v->pickExcel()); facilitySetup.addView(importButton,margin(-1,dp(50),8,0,0,8));
        imported=t("No facilities imported",13,false,Color.rgb(98,113,136)); imported.setGravity(Gravity.CENTER); facilitySetup.addView(imported);
        root.addView(facilitySetup,margin(-1,-2,0,0,0,14));

        LinearLayout identity=card(Color.WHITE,18); identity.setOrientation(LinearLayout.VERTICAL); identity.setPadding(dp(16),dp(14),dp(16),dp(14));
        identity.addView(sectionTitle("Employee & Health Facility"));
        name=new EditText(this); name.setHint("Full employee name"); name.setSingleLine(true); name.setTextColor(NAVY); name.setHintTextColor(Color.rgb(145,158,177)); name.setBackground(inputBg()); name.setPadding(dp(12),0,dp(12),0);
        identity.addView(name,margin(-1,dp(50),8,0,0,10));
        centers=new Spinner(this); centers.setBackground(inputBg()); identity.addView(centers,mp(-1,dp(52)));
        root.addView(identity,margin(-1,-2,0,0,0,14));

        LinearLayout row1=new LinearLayout(this); row1.setOrientation(LinearLayout.HORIZONTAL); row1.setGravity(Gravity.CENTER); row1.setWeightSum(2);
        LinearLayout inCard=actionCard("↪","Check In",Color.rgb(18,151,70),Color.rgb(235,251,240));
        checkInLabel=(TextView)inCard.getChildAt(1); inCard.setOnClickListener(v->{requestedAction="in"; beginAttendance();});
        LinearLayout outCard=actionCard("↩","Check Out",Color.rgb(232,57,61),Color.rgb(255,239,239));
        checkOutLabel=(TextView)outCard.getChildAt(1); outCard.setOnClickListener(v->{requestedAction="out"; beginAttendance();});
        row1.addView(inCard,weightedCard(1,0,6)); row1.addView(outCard,weightedCard(1,6,0)); root.addView(row1,margin(-1,-2,0,0,0,12));

        LinearLayout row2=new LinearLayout(this); row2.setOrientation(LinearLayout.HORIZONTAL); row2.setWeightSum(2);
        LinearLayout records=actionCard("▣","My Records",Color.rgb(26,136,219),Color.rgb(236,247,255)); records.setOnClickListener(v->showRecordStatus());
        LinearLayout settings=actionCard("⚙","Settings",Color.rgb(102,116,135),Color.rgb(244,247,250)); settings.setOnClickListener(v->showSettings());
        row2.addView(records,weightedCard(1,0,6)); row2.addView(settings,weightedCard(1,6,0)); root.addView(row2,margin(-1,-2,0,0,0,14));

        status=t("Ready",14,true,NAVY); status.setGravity(Gravity.CENTER); status.setPadding(dp(12),dp(12),dp(12),dp(12)); status.setBackground(round(Color.rgb(233,247,252),16)); root.addView(status,margin(-1,-2,0,0,0,18));
        TextView dev=t("Developed by Sedki Yassen",12,false,Color.rgb(104,119,143)); dev.setGravity(Gravity.CENTER); root.addView(dev,mp(-1,-2));
        sc.addView(root); setContentView(sc);
    }

    private LinearLayout actionCard(String icon,String label,int accent,int bg){
        LinearLayout c=card(bg,18); c.setOrientation(LinearLayout.VERTICAL); c.setGravity(Gravity.CENTER); c.setPadding(dp(8),dp(18),dp(8),dp(16)); c.setClickable(true); c.setFocusable(true);
        TextView i=iconBubble(icon,accent,Color.WHITE); c.addView(i,mp(dp(58),dp(58)));
        TextView l=t(label,15,true,accent); l.setGravity(Gravity.CENTER); c.addView(l,margin(-1,-2,8,0,0,0));
        return c;
    }
    private LinearLayout.LayoutParams weightedCard(float w,int left,int right){ LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(142),w); p.leftMargin=dp(left); p.rightMargin=dp(right); return p; }
    private TextView iconBubble(String s,int color,int bg){ TextView v=t(s,28,true,color); v.setGravity(Gravity.CENTER); v.setBackground(round(bg,30)); return v; }
    private TextView sectionTitle(String s){ TextView v=t(s,15,true,NAVY); v.setPadding(0,0,0,dp(8)); return v; }
    private LinearLayout card(int color,int radius){ LinearLayout v=new LinearLayout(this); v.setBackground(round(color,radius)); v.setElevation(dp(3)); return v; }
    private GradientDrawable round(int color,int radius){ GradientDrawable g=new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(radius)); return g; }
    private GradientDrawable gradient(int[] colors,int radius){ GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,colors); g.setCornerRadius(dp(radius)); return g; }
    private GradientDrawable inputBg(){ GradientDrawable g=round(Color.WHITE,12); g.setStroke(dp(1),Color.rgb(220,230,239)); return g; }
    private Button primaryButton(String s){ Button b=new Button(this); b.setText(s); b.setTextColor(Color.WHITE); b.setTextSize(15); b.setAllCaps(false); b.setTypeface(null,Typeface.BOLD); b.setBackground(gradient(new int[]{Color.rgb(12,123,235),Color.rgb(8,92,218)},12)); return b; }
    private TextView t(String s,int sp,boolean bold,int color){ TextView v=new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(color); if(bold)v.setTypeface(null,Typeface.BOLD); return v; }
    private LinearLayout.LayoutParams mp(int w,int h){return new LinearLayout.LayoutParams(w,h);}
    private LinearLayout.LayoutParams margin(int w,int h,int top,int bottom,int left,int right){LinearLayout.LayoutParams p=mp(w,h);p.topMargin=dp(top);p.bottomMargin=dp(bottom);p.leftMargin=dp(left);p.rightMargin=dp(right);return p;}
    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}

    private void pickExcel(){ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"); startActivityForResult(i,PICK_XLSX); }
    @Override protected void onActivityResult(int r,int c,Intent d){ super.onActivityResult(r,c,d); if(r==PICK_XLSX&&c==RESULT_OK&&d!=null&&d.getData()!=null) importExcel(d.getData()); }
    private void importExcel(Uri u){ status.setText("Reading Excel file..."); new Thread(()->{ try{ List<HealthCenter>x=XlsxCenterReader.read(getContentResolver(),u); if(x==null||x.isEmpty())throw new Exception("No valid health facilities found"); CenterStore.save(this,x); runOnUiThread(()->{list=x;refreshSpinner();imported.setText(x.size()+" facilities imported successfully");facilitySetup.setVisibility(View.GONE);status.setText("Health facilities saved successfully");}); }catch(Exception e){runOnUiThread(()->status.setText("Import failed: "+e.getMessage()));} }).start(); }
    private void loadEmailSetup(){ String e=getPreferences(0).getString("recipient_email","").trim(); emailSetup.setVisibility(e.isEmpty()?View.VISIBLE:View.GONE); if(e.isEmpty())status.setText("Please enter recipient email"); }
    private void saveRecipientEmail(){ String e=emailInput.getText().toString().trim(); if(e.isEmpty()||!Patterns.EMAIL_ADDRESS.matcher(e).matches()){status.setText("Please enter a valid email address");return;} getPreferences(0).edit().putString("recipient_email",e).apply(); emailInput.setText(""); emailSetup.setVisibility(View.GONE); status.setText("Email saved successfully"); }
    private boolean hasRecipientEmail(){return !getPreferences(0).getString("recipient_email","").trim().isEmpty();}

    private void sendAttendanceEmail(String n,HealthCenter hc,Location loc,String type,float distance){ String recipient=getPreferences(0).getString("recipient_email","").trim(); if(recipient.isEmpty())return; SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US); f.setTimeZone(TimeZone.getTimeZone("Asia/Aden")); String subject="HFs Attendance - "+type+" - "+n; String body="Employee Name: "+n+"\nHealth Facility: "+hc.name+"\nAttendance Type: "+type+"\nDate & Time: "+f.format(new Date())+"\nLatitude: "+loc.getLatitude()+"\nLongitude: "+loc.getLongitude()+"\nDistance from HF: "+Math.round(distance)+" m"; try{Intent i=new Intent(Intent.ACTION_SENDTO);i.setData(Uri.parse("mailto:"+Uri.encode(recipient)));i.putExtra(Intent.EXTRA_EMAIL,new String[]{recipient});i.putExtra(Intent.EXTRA_SUBJECT,subject);i.putExtra(Intent.EXTRA_TEXT,body);startActivity(Intent.createChooser(i,"Send attendance data"));}catch(Exception e){status.setText("Unable to open email application");} }

    private void loadCenters(){ list=CenterStore.load(this); refreshSpinner(); boolean has=!list.isEmpty(); facilitySetup.setVisibility(has?View.GONE:View.VISIBLE); if(!has){imported.setText("No facilities imported");status.setText("Please import health facilities first");} }
    private void refreshSpinner(){ ArrayList<String>a=new ArrayList<>(); if(list.isEmpty())a.add("Import health facilities first"); else for(HealthCenter h:list)a.add(h.name); ArrayAdapter<String> ad=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,a); centers.setAdapter(ad); }
    private void loadIdentity(){ String n=getPreferences(0).getString("identity_name",""); if(!n.isEmpty()){name.setText(n);name.setEnabled(false);} }
    private void updateActionCards(){ SharedPreferences p=getPreferences(0); boolean active=dayFmt.format(new Date()).equals(p.getString("state_day",""))&&p.getLong("first_press_time",0)>0; if(checkInLabel!=null){checkInLabel.setText(active?"Checked In":"Check In"); checkOutLabel.setText(active?"Check Out":"Check Out");} }

    private void showRecordStatus(){ SharedPreferences p=getPreferences(0); long first=p.getLong("first_press_time",0); String d=p.getString("state_day",""); if(first>0&&dayFmt.format(new Date()).equals(d))status.setText("Today's record: Check-in completed • Check-out pending"); else status.setText("No active attendance record for today"); }
    private void showSettings(){ emailSetup.setVisibility(View.VISIBLE); facilitySetup.setVisibility(View.VISIBLE); status.setText("Settings: you can update email or re-import facilities"); }

    private void beginAttendance(){ if(!hasRecipientEmail()){emailSetup.setVisibility(View.VISIBLE);status.setText("Please save recipient email first");return;} if(list.isEmpty()){facilitySetup.setVisibility(View.VISIBLE);status.setText("Please import health facilities first");return;} String n=name.getText().toString().trim(); if(n.isEmpty()){status.setText("Please enter employee name");return;} SharedPreferences p=getPreferences(0); boolean active=dayFmt.format(new Date()).equals(p.getString("state_day",""))&&p.getLong("first_press_time",0)>0; if("in".equals(requestedAction)&&active){status.setText("Check-in is already recorded for today");return;} if("out".equals(requestedAction)&&!active){status.setText("Please complete Check In first");return;} if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},REQ_LOC);return;} captureLocation(n); }
    @Override public void onRequestPermissionsResult(int r,String[]p,int[]g){super.onRequestPermissionsResult(r,p,g);if(r==REQ_LOC&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)beginAttendance();else status.setText("Location permission is required");}
    private void captureLocation(String n){ HealthCenter hc=list.get(centers.getSelectedItemPosition()); LocationManager lm=(LocationManager)getSystemService(LOCATION_SERVICE); Location best=null; try{for(String pr:lm.getProviders(true)){Location l=lm.getLastKnownLocation(pr);if(l!=null&&(best==null||l.getAccuracy()<best.getAccuracy()))best=l;}}catch(SecurityException ignored){} if(best!=null&&best.getAccuracy()<=100){finishAttendance(n,hc,best);return;} status.setText("Detecting GPS location..."); final LocationListener[]box=new LocationListener[1]; box[0]=new LocationListener(){public void onLocationChanged(Location l){if(l.getAccuracy()<=100){try{lm.removeUpdates(box[0]);}catch(Exception ignored){}finishAttendance(n,hc,l);}}public void onStatusChanged(String p,int s,Bundle e){}public void onProviderEnabled(String p){}public void onProviderDisabled(String p){}}; try{lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,0,box[0]);}catch(Exception e){status.setText("Unable to access GPS");} }
    private void finishAttendance(String n,HealthCenter hc,Location loc){ float[]res=new float[1]; Location.distanceBetween(loc.getLatitude(),loc.getLongitude(),hc.latitude,hc.longitude,res); if(res[0]>hc.radiusMeters){status.setText("Outside allowed range • "+Math.round(res[0])+" m");return;} SharedPreferences p=getPreferences(0); String today=dayFmt.format(new Date()),sd=p.getString("state_day",""); long first=p.getLong("first_press_time",0),now=System.currentTimeMillis(); if(!today.equals(sd)){first=0;p.edit().putString("state_day",today).remove("first_press_time").apply();} if("in".equals(requestedAction)){p.edit().putLong("first_press_time",now).putString("state_day",today).putString("identity_name",n).apply();name.setEnabled(false);status.setText("✓ Check In successful • "+hc.name);sendAttendanceEmail(n,hc,loc,"Check-in",res[0]);}else{if(first==0){status.setText("Please complete Check In first");return;}long elapsed=now-first;if(elapsed<FIVE_HOURS){long mins=(FIVE_HOURS-elapsed+59999)/60000;status.setText("Minimum 5 hours required • "+mins+" min remaining");return;}p.edit().remove("first_press_time").putString("state_day",today).apply();status.setText("✓ Check Out successful • "+hc.name);sendAttendanceEmail(n,hc,loc,"Check-out",res[0]);}updateActionCards(); }
}

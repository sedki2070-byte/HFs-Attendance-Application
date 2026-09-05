package org.sedki.locationcheckin;

import android.content.ContentResolver;
import android.net.Uri;
import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class XlsxCenterReader {
    private static final Set<String> NAME = set("healthcentername","healthfacilityname","hfname","facilityname","healthcenter","healthfacility","hf","centername","اسم المركز الصحي","اسم المركز","اسم المرفق الصحي","المركز الصحي","المرفق الصحي");
    private static final Set<String> LAT = set("latitude","lat","gpslatitude","y","خط العرض","دائرة العرض");
    private static final Set<String> LON = set("longitude","long","lon","lng","gpslongitude","x","خط الطول");
    private static final Set<String> RAD = set("allowedradius","allowedradiusm","radius","radiusm","distancem","المسافة المسموحة","نطاق السماح","نصف القطر");
    private static Set<String> set(String...v){ return new HashSet<>(Arrays.asList(v)); }
    private static String norm(String s){ if(s==null)return""; return s.trim().toLowerCase(Locale.ROOT).replaceAll("[\\s_\\-()\\[\\].:/]+",""); }

    public static List<HealthCenter> read(ContentResolver cr, Uri uri) throws Exception {
        Map<String,byte[]> zip = unzip(cr.openInputStream(uri));
        List<String> shared = parseShared(zip.get("xl/sharedStrings.xml"));
        List<String> sheets = new ArrayList<>();
        for(String k:zip.keySet()) if(k.matches("xl/worksheets/sheet\\d+\\.xml")) sheets.add(k);
        Collections.sort(sheets);
        List<HealthCenter> best = new ArrayList<>();
        for(String sheet:sheets){ List<HealthCenter> got=parseSheet(zip.get(sheet),shared); if(got.size()>best.size()) best=got; }
        if(best.isEmpty()) throw new IllegalArgumentException("لم يتم العثور على أعمدة اسم المركز وLatitude وLongitude في ملف Excel");
        return best;
    }
    private static Map<String,byte[]> unzip(InputStream in) throws Exception {
        if(in==null) throw new FileNotFoundException(); Map<String,byte[]> out=new HashMap<>();
        ZipInputStream z=new ZipInputStream(new BufferedInputStream(in)); ZipEntry e; byte[] buf=new byte[8192];
        while((e=z.getNextEntry())!=null){ ByteArrayOutputStream b=new ByteArrayOutputStream(); int n; while((n=z.read(buf))>0)b.write(buf,0,n); out.put(e.getName(),b.toByteArray()); }
        z.close(); return out;
    }
    private static List<String> parseShared(byte[] data) throws Exception {
        ArrayList<String> out=new ArrayList<>(); if(data==null)return out;
        XmlPullParser p=Xml.newPullParser(); p.setInput(new ByteArrayInputStream(data),"UTF-8"); int ev; StringBuilder cur=null;
        while((ev=p.next())!=XmlPullParser.END_DOCUMENT){ if(ev==XmlPullParser.START_TAG&&"si".equals(p.getName()))cur=new StringBuilder(); else if(ev==XmlPullParser.START_TAG&&"t".equals(p.getName())&&cur!=null)cur.append(p.nextText()); else if(ev==XmlPullParser.END_TAG&&"si".equals(p.getName())&&cur!=null){out.add(cur.toString());cur=null;} }
        return out;
    }
    private static List<HealthCenter> parseSheet(byte[] data,List<String> shared) throws Exception {
        ArrayList<Map<Integer,String>> rows=new ArrayList<>(); if(data==null)return new ArrayList<>();
        XmlPullParser p=Xml.newPullParser(); p.setInput(new ByteArrayInputStream(data),"UTF-8"); int ev; Map<Integer,String> row=null; int col=-1; String type=null;
        while((ev=p.next())!=XmlPullParser.END_DOCUMENT){
            if(ev==XmlPullParser.START_TAG&&"row".equals(p.getName())) row=new HashMap<>();
            else if(ev==XmlPullParser.START_TAG&&"c".equals(p.getName())){ String ref=p.getAttributeValue(null,"r"); col=colIndex(ref); type=p.getAttributeValue(null,"t"); }
            else if(ev==XmlPullParser.START_TAG&&("v".equals(p.getName())||"t".equals(p.getName()))&&row!=null&&col>=0){ String v=p.nextText(); if("s".equals(type)){ try{v=shared.get(Integer.parseInt(v));}catch(Exception ignored){} } row.put(col,v); }
            else if(ev==XmlPullParser.END_TAG&&"row".equals(p.getName())&&row!=null){ rows.add(row); row=null; if(rows.size()>5000)break; }
        }
        int hr=-1,nc=-1,la=-1,lo=-1,ra=-1;
        for(int i=0;i<Math.min(30,rows.size());i++){
            Map<Integer,String> r=rows.get(i); int tn=-1,tl=-1,tlo=-1,tr=-1;
            for(Map.Entry<Integer,String> e:r.entrySet()){String x=norm(e.getValue()); if(NAME.contains(x))tn=e.getKey(); if(LAT.contains(x))tl=e.getKey(); if(LON.contains(x))tlo=e.getKey(); if(RAD.contains(x))tr=e.getKey();}
            if(tn>=0&&tl>=0&&tlo>=0){hr=i;nc=tn;la=tl;lo=tlo;ra=tr;break;}
        }
        ArrayList<HealthCenter> out=new ArrayList<>(); if(hr<0)return out; Set<String> dedupe=new HashSet<>();
        for(int i=hr+1;i<rows.size();i++){
            Map<Integer,String> r=rows.get(i); String name=clean(r.get(nc)); if(name.isEmpty())continue;
            try{double lat=num(r.get(la)),lon=num(r.get(lo)); if(lat<-90||lat>90||lon<-180||lon>180)continue; double radius=100; if(ra>=0){try{radius=num(r.get(ra));}catch(Exception ignored){}}
                String key=name.toLowerCase(Locale.ROOT)+"|"+lat+"|"+lon; if(dedupe.add(key)) out.add(new HealthCenter(name,lat,lon,radius));
            }catch(Exception ignored){}
        } return out;
    }
    private static String clean(String s){return s==null?"":s.trim();}
    private static double num(String s){ if(s==null)throw new NumberFormatException(); return Double.parseDouble(s.trim().replace(',','.')); }
    private static int colIndex(String ref){ if(ref==null)return-1; int n=0,i=0; while(i<ref.length()&&Character.isLetter(ref.charAt(i))){n=n*26+(Character.toUpperCase(ref.charAt(i))-'A'+1);i++;} return n-1; }
}

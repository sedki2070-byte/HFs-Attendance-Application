package org.sedki.locationcheckin;

import android.content.Context;
import android.net.Uri;
import androidx.core.content.FileProvider;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class AttendanceExcelExporter {
    private AttendanceExcelExporter() {}

    public static Uri create(Context context, String employee, String facility, String type,
                             String dateTime, double latitude, double longitude, String distance) throws IOException {
        File dir = new File(context.getCacheDir(), "attendance_exports");
        if (!dir.exists() && !dir.mkdirs()) throw new IOException("Cannot create export folder");
        File file = new File(dir, "Attendance_" + System.currentTimeMillis() + ".xlsx");
        try (ZipOutputStream z = new ZipOutputStream(new FileOutputStream(file))) {
            add(z,"[Content_Types].xml","<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>");
            add(z,"_rels/.rels","<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
            add(z,"xl/workbook.xml","<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Attendance\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
            add(z,"xl/_rels/workbook.xml.rels","<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>");
            String[] headers={"Employee Name","Health Facility / Location","Attendance Type","Date & Time","Latitude","Longitude","Distance from HF"};
            String[] vals={employee,facility,type,dateTime,String.valueOf(latitude),String.valueOf(longitude),distance};
            StringBuilder s=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
            row(s,1,headers); row(s,2,vals); s.append("</sheetData></worksheet>");
            add(z,"xl/worksheets/sheet1.xml",s.toString());
        }
        return FileProvider.getUriForFile(context, context.getPackageName()+".fileprovider", file);
    }
    private static void row(StringBuilder s,int n,String[] v){ s.append("<row r=\"").append(n).append("\">"); for(int i=0;i<v.length;i++){ char c=(char)('A'+i); s.append("<c r=\"").append(c).append(n).append("\" t=\"inlineStr\"><is><t>").append(esc(v[i])).append("</t></is></c>"); } s.append("</row>"); }
    private static String esc(String x){ if(x==null)return ""; return x.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;"); }
    private static void add(ZipOutputStream z,String name,String text)throws IOException{ z.putNextEntry(new ZipEntry(name)); z.write(text.getBytes("UTF-8")); z.closeEntry(); }
}

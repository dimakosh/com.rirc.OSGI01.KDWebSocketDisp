package com.rirc.OSGI01.RunSoftRep;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;

import com.rirc.OSGI01.DataViewStructure.Detail;
import com.rirc.OSGI01.DataViewStructure.DetailFlags;
import com.rirc.OSGI01.DataViewStructure.TextLabel;
import com.rirc.OSGI01.KDCompMethod;
import com.rirc.OSGI01.KDCompStepInfoPing;
import com.rirc.OSGI01.KDConnection;
import com.rirc.OSGI01.SoftRep;

public class RunSoftRep implements KDCompMethod {
	final private SoftRep[] softReps;
	
	private final static String fontName= "Courier New";

	public RunSoftRep(SoftRep softRep) {
		softReps= new SoftRep[] {softRep};
	}
	
	public RunSoftRep(SoftRep[] _softReps) {
		softReps= _softReps;
	}
	
	private int counterNRow;
	private SoftRep sn;
	private Workbook wb;
	private String sheetname;
	private Sheet sh;
	
	private int getNRow(int cnter) {
		int nRow= cnter%65530;
		
		if (nRow==0) {
			int nSH= cnter/65530;
			String shn= (nSH!=0)? String.valueOf(nSH)+'-'+sheetname : sheetname;
			sh= wb.createSheet(shn);
			sh.setFitToPage(true);

			for (int i= 0; i< sn.details.size(); i++) {
				Detail d= sn.details.get(i);
				sh.setColumnWidth(i, d.Width*15*25);
			}
		}
		
		return nRow;
	}

	@KDCompStepInfoPing
	public String stepInfo() {
		return "RunSoftRep: "+String.valueOf(counterNRow)+' '+String.valueOf(sheetname);
	}

	private void make() throws Exception {
		counterNRow= 0;

		if (sn.repSmallHead!=null) {
			Font f7= wb.createFont();
			f7.setFontName(fontName);
			f7.setFontHeightInPoints((short)7);
			
			CellStyle s01l= wb.createCellStyle();
			s01l.setFont(f7);
			s01l.setAlignment(HorizontalAlignment.LEFT);
			s01l.setVerticalAlignment(VerticalAlignment.BOTTOM);
			
			int nRow= getNRow(counterNRow++);
			
			Row row= sh.createRow(nRow);
			row.setHeightInPoints(10);

			Cell cell= row.createCell(0);
			cell.setCellStyle(s01l);
			cell.setCellValue(sn.repSmallHead);

			CellRangeAddress reg= new CellRangeAddress(nRow, nRow, 0, sn.details.size() -1); 
			sh.addMergedRegion(reg);
		}

        double[] repSums= null;
		
		for (int i= 0; i< sn.details.size(); i++) {
			Detail d= sn.details.get(i);
			//sh.setColumnWidth(i, d.Width*15*25);
			
			if (repSums==null && DetailFlags.RepSum.equals(d.Df)) repSums= new double[sn.details.size()];
		}

		{
			Font f12= wb.createFont();
			f12.setFontName(fontName);
			f12.setFontHeightInPoints((short)12);
			
			CellStyle s02= wb.createCellStyle();
			s02.setFont(f12);
			s02.setAlignment(HorizontalAlignment.CENTER);
			s02.setVerticalAlignment(VerticalAlignment.BOTTOM);

			for (TextLabel tl : sn.heads) {
				int nRow= getNRow(counterNRow++);
				
				Row row= sh.createRow(nRow);
				row.setHeightInPoints(18);

				Cell cell= row.createCell(0);
				cell.setCellStyle(s02);
				cell.setCellValue(tl.Text);
				
				if (1<sn.details.size()) {
					CellRangeAddress reg= new CellRangeAddress(nRow, nRow, 0, sn.details.size() -1); 
					sh.addMergedRegion(reg);
				}
			}
		}
		
		{
			Font f10= wb.createFont();
			f10.setFontName(fontName);
			f10.setFontHeightInPoints((short)10);
			
			CellStyle s04= wb.createCellStyle();
			s04.setFont(f10);
			s04.setAlignment(HorizontalAlignment.CENTER);
			s04.setVerticalAlignment(VerticalAlignment.CENTER);
			s04.setWrapText(true);
			s04.setBorderBottom(BorderStyle.THIN);
			s04.setBorderLeft(BorderStyle.THIN);
			s04.setBorderRight(BorderStyle.THIN);
			s04.setBorderTop(BorderStyle.THIN);
			
			int nRow= getNRow(counterNRow++);
			
			Row row= sh.createRow(nRow);
			row.setHeightInPoints(36*2);

			for (int i= 0; i< sn.details.size(); i++) {
				Detail d= sn.details.get(i);
				
				Cell cell= row.createCell(i);
				cell.setCellStyle(s04);
				cell.setCellValue(d.Title);
			}
		}

		{
			Font f7= wb.createFont();
			f7.setFontName(fontName);
			f7.setFontHeightInPoints((short)7);
			
			CellStyle s03= wb.createCellStyle();
			s03.setFont(f7);
			s03.setAlignment(HorizontalAlignment.CENTER);
			s03.setVerticalAlignment(VerticalAlignment.CENTER);
			s03.setWrapText(true);
			s03.setBorderBottom(BorderStyle.THIN);
			s03.setBorderLeft(BorderStyle.THIN);
			s03.setBorderRight(BorderStyle.THIN);
			s03.setBorderTop(BorderStyle.THIN);
			
			int nRow= getNRow(counterNRow++);
			
			Row row= sh.createRow(nRow);
			row.setHeightInPoints(9);

			for (int i= 0; i< sn.details.size(); i++) {
				Cell cell= row.createCell(i);
				cell.setCellStyle(s03);
				cell.setCellValue(i+1);
			}
		}
		
		try (Connection conn= (sn.connPrms!=null)? KDConnection.get(sn.connPrms):null;
			 ResultSet dataSrc= sn.dataCmd.getResultSet(conn)) {

			CellStyle dateStyle;
			{
				CreationHelper createHelper = wb.getCreationHelper();
				dateStyle= wb.createCellStyle();
				dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("m/d/yy"));
			}
			
			Map<Integer,CellStyle> mNumericStyle= new HashMap<Integer,CellStyle>();

			Font f10= wb.createFont();
			f10.setFontName(fontName);
			f10.setFontHeightInPoints((short)10);

			CellStyle sRow= wb.createCellStyle();
			sRow.setFont(f10);

            int[] colOrds = new int[sn.details.size()];
			
			for (int i= 0; i< sn.details.size(); i++) {
				Detail d= sn.details.get(i);
				
				if (d.Field!=null && !d.Field.isEmpty()) {
					try {
						colOrds[i]= dataSrc.findColumn(d.Field);
					} catch (SQLException ex) {
						colOrds[i]= -1;
					}
				} else {
                    if (0 <= d.ColOrd) colOrds[i] = d.ColOrd;
                    else colOrds[i] = i;
				}
			}
			
			while (dataSrc.next()) {
				userBeak();
				
				int nRow= getNRow(counterNRow++);

				Row row= sh.createRow(nRow);
				row.setHeightInPoints(14);

				for (int i= 0; i< sn.details.size(); i++) {
					Detail d= sn.details.get(i);

					Cell cell= row.createCell(i);
					cell.setCellStyle(sRow);

					switch (d.Tp) {
					case N: {
						int ind= colOrds[i];
						double val= (0<=ind)? dataSrc.getDouble(ind):0d;
						cell.setCellValue(val);

						cell.setCellStyle(mNumericStyle.computeIfAbsent(d.Dec, (k)-> {
							String format;
							if (k<=0) format= "#0";
							else {
								format= "#0.";
								for (int j= 0; j< k; j++) format+= '0';
							}

							CellStyle numericStyle;
							CreationHelper createHelper = wb.getCreationHelper();
							numericStyle= wb.createCellStyle();
							numericStyle.setDataFormat(createHelper.createDataFormat().getFormat(format));
							
							return numericStyle;
						}));

						if (repSums!=null && DetailFlags.RepSum.equals(d.Df)) repSums[i]+= val;
					}
					break;
					
                    case C: {
						int ind= colOrds[i];
                    	String val= (0<=ind)? dataSrc.getString(ind):null;
						cell.setCellValue(val);
                    }
                    break;

                    case D: {
						int ind= colOrds[i];
                    	Date val= (0<=ind)? dataSrc.getDate(ind):null;
						cell.setCellValue(val);
						cell.setCellStyle(dateStyle);
                    }
                    break;

                    case Mn: {
						int ind= colOrds[i];
                    	Object val= (0<=ind)? dataSrc.getObject(ind):null;
						cell.setCellValue(String.valueOf(val));
                    }
                    break;
                    
                    default:
                        throw new IllegalStateException("Detail type: "+d.Tp);
					}
				}				
			}
		}
		
		if (repSums!=null) {
			Font f10b= wb.createFont();
			f10b.setFontName(fontName);
			f10b.setFontHeightInPoints((short)10);
			f10b.setBold(true);

			CellStyle sSum= wb.createCellStyle();
			sSum.setFont(f10b);

			int nRow= getNRow(counterNRow++);

			Row row= sh.createRow(nRow);
			row.setHeightInPoints(14);

			for (int i= 0; i< sn.details.size(); i++) {
				Detail d= sn.details.get(i);

				if (DetailFlags.RepSum.equals(d.Df)) {
					Cell cell= row.createCell(i);
					cell.setCellStyle(sSum);
					
					cell.setCellValue(repSums[i]);
				}
			}			
		}
	
		{
			Font f12= wb.createFont();
			f12.setFontName(fontName);
			f12.setFontHeightInPoints((short)12);
			
			CellStyle s02= wb.createCellStyle();
			s02.setFont(f12);
			s02.setAlignment(HorizontalAlignment.CENTER);
			s02.setVerticalAlignment(VerticalAlignment.BOTTOM);

			for (TextLabel tl : sn.footers) {
				int nRow= getNRow(counterNRow++);
				
				Row row= sh.createRow(nRow);
				row.setHeightInPoints(18);

				Cell cell= row.createCell(0);
				cell.setCellStyle(s02);
				cell.setCellValue(tl.Text);
			}
		}
	}

	public File makes() throws Exception {
		wb= WorkbookFactory.create(false);

		for (SoftRep _sn : softReps) {
			sn= _sn;
			
			sheetname= sn.repSmallHead;
			if (sheetname==null) sheetname= sn.repFileName;

			make();
		}
		
		Path pathXLS= Files.createTempFile(null, null);
		File fileXLS= pathXLS.toFile();

		try (OutputStream f= new FileOutputStream(fileXLS)) {
			wb.write(f);
		} finally {
			fileXLS.deleteOnExit();
		}

		return fileXLS;
	}
}

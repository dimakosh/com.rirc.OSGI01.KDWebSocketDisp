package com.rirc.OSGI01.KDWebSocketDisp;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;

import com.rirc.OSGI01.CloseableIterable;
import com.rirc.OSGI01.ConnPrms;
import com.rirc.OSGI01.KDConnection;
import com.rirc.OSGI01.KDTime;

public class KDOSGISQL {
	
	public static class TypeIterable implements Closeable {
		int[] types;
		CloseableIterable<Object[]> rows;

		@Override
		public void close() throws IOException {
			rows.close();
		}
	}

	public static int execSQL(ConnPrms connPrms, String sql, Object... values) throws Exception {
		try (Connection conn= KDConnection.get(connPrms)) {
			try (PreparedStatement pstmt= conn.prepareStatement(sql)) {
				setPreparedStatement(values, pstmt);
				return pstmt.executeUpdate();
			}				
		}
	}

	public static TypeIterable execSelect(ConnPrms connPrms, String sql, Object... values) throws Exception {
		Connection conn= KDConnection.get(connPrms);
		try {
			PreparedStatement pstmt= conn.prepareStatement(sql);
			setPreparedStatement(values, pstmt);
			ResultSet rs= pstmt.executeQuery();
			ResultSetMetaData md= rs.getMetaData();
			int ccnt= md.getColumnCount();
		
			TypeIterable ti= new TypeIterable();
			ti.types= new int[ccnt];
			for (int i= 0; i< ccnt; i++) ti.types[i]= md.getColumnType(i+1);
			
			ti.rows=  new CloseableIterable<Object[]>() {
				@Override
				public Iterator<Object[]> iterator() {
					return new Iterator<Object[]>() {
						@Override
						public boolean hasNext() {
							try {
								return rs.next();
							} catch (SQLException ex) {
								throw new RuntimeException(ex);
							}
						}
	
						@Override
						public Object[] next() {
							try {
								Object[] r= new Object[ccnt];
								for (int i= 0; i< ccnt; i++) r[i]= rs.getObject(i+1);
								return r;
							} catch (SQLException ex) {
								throw new RuntimeException(ex);
							}
						}
					};
				}
	
				@Override
				public void close() throws IOException {
					try (Connection a1= conn; PreparedStatement a2= pstmt; ResultSet a3= rs) {
					} catch (SQLException ex) {
						throw new IOException(ex);
					}
				}
			};
			
			return ti;
		} catch (Exception ex1) {
			try (Connection a1= conn) {
			}
			throw ex1;
		}
	}
	
	private static void setPreparedStatement(Object[] values, PreparedStatement pstmt) throws SQLException {
		for (int i= 0; i< values.length; i++) {
			Object val= values[i];
			if (val instanceof Date) pstmt.setDate(i+1, KDTime.sqlDate((Date)val));
			else pstmt.setObject(i+1, val);
		}
	}
}

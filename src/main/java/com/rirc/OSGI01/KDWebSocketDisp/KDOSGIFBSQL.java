package com.rirc.OSGI01.KDWebSocketDisp;

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

public class KDOSGIFBSQL {

	public static int execSQL(ConnPrms connPrms, String sql, Object... values) throws Exception {
		try (Connection conn= KDConnection.get(connPrms)) {
			try (PreparedStatement pstmt= conn.prepareStatement(sql)) {
				setPreparedStatement(values, pstmt);
				return pstmt.executeUpdate();
			}				
		}
	}

	public static CloseableIterable<Object[]> execSelect(ConnPrms connPrms, String sql, Object... values) throws Exception {
		return new CloseableIterable<Object[]>() {
			Connection conn;
			PreparedStatement pstmt;
			ResultSet rs;
			int ccnt;
			
			{
				try {
					conn= KDConnection.get(connPrms);
					pstmt= conn.prepareStatement(sql);
					setPreparedStatement(values, pstmt);
					rs= pstmt.executeQuery();
					ResultSetMetaData md= rs.getMetaData();
					ccnt= md.getColumnCount();
				} catch (Exception ex) {
					try (Connection a1= conn; PreparedStatement a2= pstmt; ResultSet a3= rs) {
					}
					throw ex;
				}
			}
			
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
	}
	
	private static void setPreparedStatement(Object[] values, PreparedStatement pstmt) throws SQLException {
		for (int i= 0; i< values.length; i++) {
			Object val= values[i];
			if (val instanceof Date) pstmt.setDate(i+1, sqlDate((Date)val));
			else pstmt.setObject(i+1, val);
		}
	}
	
	@SuppressWarnings("deprecation")
	private static java.sql.Date sqlDate(Date d) {
		return (d==null)? null : new java.sql.Date(d.getYear(), d.getMonth(), d.getDate());
	}
}

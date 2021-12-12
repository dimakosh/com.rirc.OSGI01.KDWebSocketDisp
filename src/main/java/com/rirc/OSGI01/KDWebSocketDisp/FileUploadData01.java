package com.rirc.OSGI01.KDWebSocketDisp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpService;

import com.rirc.OSGI01.KDFile;
import com.rirc.OSGI01.KDHttpServlet;

@Component
public class FileUploadData01 extends KDHttpServlet {
	private static final long serialVersionUID = 1L;

	@Reference
    private HttpService httpService;

    @Activate
    public void activate() throws Exception {
        httpService.registerServlet("/FileUploadData01", this, null, null);
    }

    @Deactivate
    public void deactivate() throws Exception {
        httpService.unregister("/FileUploadData01");
    }

    /** The size threshold after which the file will be written to disk. */
    private static final int FILE_SIZE_THRESHOLD = 1024 * 1024 * 2;

    /** The maximum size allowed for uploaded files (-1L means unlimited). */
    private static final long MAX_FILE_SIZE = 1024 * 1024 * 1024;

    /** The maximum size allowed for "multipart/form-data" requests (-1L means unlimited). */
    private static final long MAX_REQUEST_SIZE = 1024 * 1024 * 1024;

    private static SortedMap<Long, File[]> mUplFiles= new ConcurrentSkipListMap<Long, File[]>();
    
    public static File[] getUplFiles(Long uploadId) {
    	return mUplFiles.get(uploadId);
    }

    @Override
    public void init() throws ServletException {
    	super.init();
    	
    	mUplFiles.clear();
    	
        MultipartConfigElement multipartConfigElement= new MultipartConfigElement(null, MAX_FILE_SIZE, MAX_REQUEST_SIZE, FILE_SIZE_THRESHOLD);

        for (Map.Entry<String, ? extends ServletRegistration> entry : getServletContext().getServletRegistrations().entrySet()) {
            ServletRegistration reg= entry.getValue();
            if (reg==null) continue;

            if (reg instanceof ServletRegistration.Dynamic) {
                ServletRegistration.Dynamic regDyn= (ServletRegistration.Dynamic)reg;
                regDyn.setMultipartConfig(multipartConfigElement);
            }
        }
    }
    
    @Override
    public void destroy() {
    	for (Entry<Long, File[]> e : mUplFiles.entrySet()) {
    		for (File f : e.getValue()) {
    			if (f.exists()) f.delete();
    		}
    	}
    	mUplFiles.clear();
    	
    	super.destroy();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");
		res.setCharacterEncoding("UTF-8");
		res.setContentType("text/plain; charset=UTF-8");
		res.addHeader("Access-Control-Allow-Origin", "*");

    	Long uploadId= Long.valueOf(req.getParameter("UploadId"));
    	File[] fs= mUplFiles.get(uploadId);
    	
    	res.getWriter().print((fs==null)? "-1":String.valueOf(fs.length));
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");
		res.setCharacterEncoding("UTF-8");
		res.setContentType("text/plain; charset=UTF-8");
		res.addHeader("Access-Control-Allow-Origin", "*");

    	Long uploadId= Long.valueOf(req.getParameter("UploadId"));
		
		{
			Map<Long, File[]> oldUF= new HashMap<Long, File[]>();
			oldUF.putAll(mUplFiles.headMap(System.currentTimeMillis()-1000*3600));

			{
				File[] f= mUplFiles.get(uploadId);
				if (f!=null) oldUF.put(uploadId, f);
			}
	    	for (Entry<Long, File[]> e : oldUF.entrySet()) {
	    		for (File f : e.getValue()) {
	    			if (f.exists()) f.delete();
	    		}
	    		mUplFiles.remove(e.getKey());
	    	}
		}
		{
	    	List<File> uplFiles= new ArrayList<File>();
	        for (Part p : req.getParts()) {
	            File tempFile= KDFile.createTemp();
	            Files.copy(p.getInputStream(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
	            uplFiles.add(tempFile);
	        }
	        mUplFiles.put(uploadId, uplFiles.toArray(new File[uplFiles.size()]));
		}
    }
}

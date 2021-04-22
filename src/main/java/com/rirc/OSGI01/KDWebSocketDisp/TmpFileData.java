package com.rirc.OSGI01.KDWebSocketDisp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpService;

import com.rirc.OSGI01.KDHttpServlet;

@Component
public class TmpFileData extends KDHttpServlet {
	private static final long serialVersionUID = 1L;

	@Reference
    private HttpService httpService;

    @Activate
    public void activate() throws Exception {
        httpService.registerServlet("/tmpfile/*", this, null, null);
    }

    @Deactivate
    public void deactivate() throws Exception {
        httpService.unregister("/tmpfile/*");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");
		res.setCharacterEncoding("UTF-8");
		res.addHeader("Access-Control-Allow-Origin", "*");
    	
    	String contentType= req.getParameter("ContentType");
		if (contentType!=null) res.setContentType(contentType);
		else res.setContentType("text/plain; charset=UTF-8");

		String sSysTime= req.getParameter("SysTime");
		if (sSysTime!=null) {
			Long sysTime= Long.valueOf(sSysTime);

			File[] fs= KDCompTypeShowAndRunServlet01.mSysTime2TmpFile.remove(sysTime);
			if (fs==null) fs= WebSocketDisp01.mSysTime2TmpFile.remove(sysTime);
			if (fs!=null) {
				File f= fs[0];
				Files.copy(f.toPath(), res.getOutputStream());
				f.delete();
			}
		}
    }
}

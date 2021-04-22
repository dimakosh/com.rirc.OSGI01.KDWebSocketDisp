package com.rirc.OSGI01.KDWebSocketDisp;

import java.io.IOException;
import java.util.Date;

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
public class Servlet01 extends KDHttpServlet {
	private static final long serialVersionUID = 1L;

	@Reference
    private HttpService httpService;

    @Activate
    public void activate() throws Exception {
        httpService.registerServlet("/Servlet01", this, null, null);
    }

    @Deactivate
    public void deactivate() throws Exception {
        httpService.unregister("/Servlet01");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");
		res.setCharacterEncoding("UTF-8");
		res.setContentType("text/plain; charset=UTF-8");
		res.addHeader("Access-Control-Allow-Origin", "*");

		res.getWriter().print("Servlet_01: "+new Date());
    }
}

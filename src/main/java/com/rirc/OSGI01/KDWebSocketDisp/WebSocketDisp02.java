package com.rirc.OSGI01.KDWebSocketDisp;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpService;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@WebSocket
public class WebSocketDisp02 {

	private static final ConcurrentMap<Session,Long> mSes2Time= new ConcurrentHashMap<Session,Long>();

    @Reference
    private HttpService httpService;

    @Activate
    public void activate() throws Exception {
        httpService.registerServlet("/KDOSGIWSDisp02", new WebSocketServlet02(), null, null);
    }

    @Deactivate
    public void deactivate() throws Exception {
        httpService.unregister("/KDOSGIWSDisp02");
    }

    @OnWebSocketConnect
    public void onOpen(Session session) {
        session.setIdleTimeout(-1);
		Long t= System.currentTimeMillis();
        mSes2Time.put(session, t);
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
    	mSes2Time.remove(session);
    }
    
    @OnWebSocketMessage
    public void OnMessage(Session session, String msg) {
    	System.out.println("WebSocketDisp02");
    	System.out.println(msg);
    }
}

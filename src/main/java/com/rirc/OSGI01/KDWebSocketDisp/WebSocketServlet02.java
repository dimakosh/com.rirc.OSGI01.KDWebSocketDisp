package com.rirc.OSGI01.KDWebSocketDisp;

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

// ssh -p 8101 karaf@localhost
// feature:install webconsole
// http://localhost:8181/system/console/bundles
// feature:install http
// feature:install jetty
// feature:install scr

public class WebSocketServlet02 extends WebSocketServlet {
	private static final long serialVersionUID = 1L;

	@Override
    public void configure(WebSocketServletFactory factory) {
        factory.register(WebSocketDisp02.class);
    }
}

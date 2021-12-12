package com.rirc.OSGI01.KDWebSocketDisp;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import com.rirc.OSGI01.KBCmpArr;
import com.rirc.OSGI01.KDCompMethod;
import com.rirc.OSGI01.KDCompType;

//tar -xvzf apache-karaf-4.2.8.tar.gz
//ssh -p 8101 karaf@localhost
//feature:install webconsole
//http://localhost:8181/system/console/bundles
//https://karafcgk.rirc.info/system/console/bundles
//feature:install http
//feature:install jetty
//feature:install scr
//bundle:install -s wrap:mvn:com.google.code.gson/gson/2.8.6
//bundle:install -s wrap:mvn:org.firebirdsql.jdbc/jaybird-jdk18/3.0.8 - нет!!! не хватает вызова Class.forName("org.firebirdsql.jdbc.FBDriver"); для загрузки ???
//bundle:install -s wrap:mvn:org.apache.poi/poi/4.0.1
//bundle:install -s wrap:mvn:org.apache.poi/poi-ooxml/4.0.1 - нет!!!

//<version>4.0.1</version>

// - не работает -
//bundle:install -s wrap:mvn:org.apache.poi/poi/4.1.2
//bundle:install -s wrap:mvn:org.apache.poi/poi-ooxml/4.1.2
//bundle:install -s wrap:mvn:org.jxls/jxls-poi/2.10.0

//		//bundle:install -s wrap:mvn:org.apache.poi/poi/5.0.0
//		//bundle:install -s wrap:mvn:org.apache.poi/poi-ooxml/5.0.0

//bundle:install -s wrap:mvn:org.apache.commons/commons-math3/3.6.1

//bundle:install -s wrap:mvn:org.xerial/sqlite-jdbc/3.25.2
//bundle:install -s wrap:mvn:org.apache.httpcomponents/httpclient/4.5.10 - нет!!!
//bundle:install -s wrap:mvn:org.apache.httpcomponents/httpclient-osgi/4.5.10 - нет!!!

//bundle:install -s wrap:mvn:com.linuxense/javadbf/0.4.0

//bundle:install -s wrap:mvn:org.apache.commons/commons-collections4/4.3 - нет!!!
//install mvn:org.apache.commons/commons-lang3/3.8.1 попробовать
//install wrap:mvn:org.apache.poi/poi/4.0.1
//install wrap:mvn:org.apache.poi/poi-ooxml/4.0.1

public class Activator implements BundleActivator {

	private ServiceTracker<KDCompMethod, KDCompMethod> KDCompMethodServiceTracker;

	public static final ConcurrentMap<KBCmpArr<String>,KDCompMethod> mKDComp2ServObj= new ConcurrentHashMap<KBCmpArr<String>,KDCompMethod>();
	
	//public static final ConcurrentNavigableMap<KBCmpArr<String>,KDCompMethod> mKDComp2ServObj= new ConcurrentSkipListMap<KBCmpArr<String>,KDCompMethod>();
	/*
	public static final ConcurrentNavigableMap<KBCmpArr<String>,KDCompMethod> mKDComp2ServObj= new ConcurrentSkipListMap<KBCmpArr<String>,KDCompMethod>((v1,v2)-> {
		try {
			String[] a1= v1.getArr();
			String[] a2= v2.getArr();
			
			int n= Math.min(a1.length, a2.length);
			
			for (int i= 0; i< n; i++) {
				int r= a1[i].compareTo(a2[i]);
				if (r!=0) {
					System.out.println(r);
					return r;
				}
			}
			
			System.out.println(a1.length-a2.length);
			return a1.length-a2.length;
		} catch (Exception ex) {
			ex.printStackTrace();
			return 0;
		}
	});
	*/

	@Override
	public void start(BundleContext context) throws Exception {
		KDCompMethodServiceTracker= new ServiceTracker<KDCompMethod, KDCompMethod>(context, KDCompMethod.class, null) {
			@Override
			public KDCompMethod addingService(ServiceReference<KDCompMethod> reference) {
				KDCompMethod serv= context.getService(reference);
				if (serv==null) return null;
				regServ(serv, true);
				return serv;
			}
			
			@Override
			public void removedService(ServiceReference<KDCompMethod> reference, KDCompMethod serv) {
				regServ(serv, false);
			}
		};
		KDCompMethodServiceTracker.open();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		KDCompMethodServiceTracker.close();
	}
	
	private void regServ(KDCompMethod serv, boolean isAdd) {
		Class<?> classServ= serv.getClass();

		KDCompType kdCompModul= classServ.getAnnotation(KDCompType.class);
		if (kdCompModul!=null) {
			KBCmpArr<String> key= new KBCmpArr<String>(new String[] {kdCompModul.project(), kdCompModul.type(), serv.getClass().getName()});
			if (isAdd) mKDComp2ServObj.put(key, serv);
			else mKDComp2ServObj.remove(key);
		}
	}
}

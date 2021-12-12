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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rirc.OSGI01.ConnPrms;
import com.rirc.OSGI01.KBCmpArr;
import com.rirc.OSGI01.KDCompField;
import com.rirc.OSGI01.KDCompMethod;
import com.rirc.OSGI01.KDCompRun;
import com.rirc.OSGI01.KDCompStepInfoPing;
import com.rirc.OSGI01.KDCompType;
import com.rirc.OSGI01.KDExecutors;
import com.rirc.OSGI01.KDStr;
import com.rirc.OSGI01.KDTime;
import com.rirc.OSGI01.LSEnumValue;
import com.rirc.OSGI01.SoftRep;
import com.rirc.OSGI01.RunSoftRep.RunSoftRep;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@WebSocket
public class WebSocketDisp01 {

	private static final ConcurrentMap<Session,Long> mSes2Time= new ConcurrentHashMap<Session,Long>();
	private static final AtomicInteger aiCompIdent= new AtomicInteger();
	private static final ConcurrentMap<Integer,KDCompMethod> mCompIdent2RunMeth= new ConcurrentHashMap<Integer,KDCompMethod>();
	private static final ConcurrentMap<CompletableFuture<Void>,String> mCF2Mess= new ConcurrentHashMap<CompletableFuture<Void>,String>();

	protected static final ConcurrentMap<Long,File[]> mSysTime2TmpFile= new ConcurrentHashMap<Long,File[]>();
	private static final ConcurrentMap<Session,Set<Long>> mSes2CITmpFiles= new ConcurrentHashMap<Session,Set<Long>>();
	
    @Reference
    private HttpService httpService;

    @Activate
    public void activate() throws Exception {
        httpService.registerServlet("/KDOSGIWSDisp01", new WebSocketServlet01(), null, null);
    }

    @Deactivate
    public void deactivate() throws Exception {
        httpService.unregister("/KDOSGIWSDisp01");
    }

    @OnWebSocketConnect
    public void onOpen(Session session) {
        session.setIdleTimeout(-1);
		Long t= System.currentTimeMillis();
        mSes2Time.put(session, t);
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
    	/*
		synchronized (System.out) {
	    	System.out.println("onClose");
	    	System.out.println(session);
	    	System.out.println(mSes2Time.remove(session));
		}
		*/

    	mSes2Time.remove(session);
    	
    	Set<Long> ciTmpFiles= mSes2CITmpFiles.remove(session);
    	if (ciTmpFiles!=null) {
	    	for (Long sysTime : ciTmpFiles) {
	    		File[] tmpFile= mSysTime2TmpFile.remove(sysTime);
	    		if (tmpFile!=null) {
	    			for (File f : tmpFile) f.delete();
	    		}
	    	}
    	}
    }

    @OnWebSocketMessage
    public void OnMessage(Session session, String msg) {
    	CompletableFuture<Void> cf= CompletableFuture.runAsync(()-> {
    		
    		//System.out.println(msg);
    		
    		try {
	    		if ("ping".equals(msg)) {
	    			Long t= System.currentTimeMillis();
	    			mSes2Time.put(session, t);
	    			session.getRemote().sendString("{\"ping\":"+t+",\"CFSize\":"+mCF2Mess.size()+"}");
	    		} else {
	    			JsonElement element= JsonParser.parseString(msg);
	    			JsonObject rootObject= element.getAsJsonObject();

    				int reqNo= rootObject.get("ReqNo").getAsInt();
	    			
	    			try {
		    			switch ( rootObject.get("ReqId").getAsString() ) {
		    			case "execSQL":
		    				case_execSQL(reqNo, rootObject, session);
		    				break;
		    			case "execSelect":
		    				case_execSelect(reqNo, rootObject, session);
		    				break;
		    			case "getKDCompTypes":
		    				case_getKDCompTypes(reqNo, rootObject, session);
		    				break;
		    			case "runKDComp":
	    					case_runKDComp(reqNo, rootObject, session);
		    				break;
		    			case "pingKDComp":
	    					case_pingKDComp(reqNo, rootObject, session);
		    				break;
		    			default:
		    				JsonObject joError= new JsonObject();
		    				joError.addProperty("ReqNo", reqNo);
		    				joError.addProperty("Exception", "nofunc");
		    				session.getRemote().sendString(joError.toString());
		    			}
	    			} catch (Exception ex) {
	    				JsonObject joError= new JsonObject();
	    				joError.addProperty("ReqNo", reqNo);
	    				joError.addProperty("Exception", ex.toString());
	    				session.getRemote().sendString(joError.toString());
	    			}
	    		}
    		} catch (Exception ex) {
    			throw new RuntimeException(ex);
    		}
    	}, KDExecutors.getStdExecutor());

    	mCF2Mess.put(cf, msg);
    	
    	cf.handleAsync((v,ex1)-> {
    		mCF2Mess.remove(cf);
    		
    		if (ex1!=null) {
				JsonObject joError= new JsonObject();
				joError.addProperty("ReqNo", "0");
				joError.addProperty("Exception", ex1.toString());
				try {
					session.getRemote().sendString(joError.toString());
				} catch (IOException ex2) {
	    			throw new RuntimeException(ex2);
				}
    		}

    		return null;
    	}, KDExecutors.getStdExecutor());
    }
    
    private void case_execSQL(int reqNo, JsonObject rootObject, Session session) throws IOException {
    	try {
			JsonArray rets= new JsonArray(); 
			for (int ret : execSQL(reqNo, rootObject)) rets.add(ret);
			JsonObject joRes= new JsonObject();
			joRes.addProperty("ReqNo", reqNo);
			joRes.add("Rets", rets);
			session.getRemote().sendString(joRes.toString());
		} catch (Exception ex) {
			JsonObject joError= new JsonObject();
			joError.addProperty("ReqNo", reqNo);
			joError.addProperty("Exception", KDStr.getExMessage(ex));
			session.getRemote().sendString(joError.toString());
		}
    }
    
	private int[] execSQL(int reqNo, JsonObject joReg) throws Exception {
		JsonObject joLogin= joReg.get("Login").getAsJsonObject();
        ConnPrms connPrms= new ConnPrms(joLogin.get("server").getAsString(), joLogin.get("source").getAsString(), joLogin.get("user").getAsString(), joLogin.get("pass").getAsString());

		JsonArray jaSQLPach= joReg.get("SQLPach").getAsJsonArray();
		
		int[] ret= new int[jaSQLPach.size()];

		for (int n= 0; n< jaSQLPach.size(); n++) {
			JsonObject joSQLVals= jaSQLPach.get(n).getAsJsonObject();
			String sql= joSQLVals.get("SQL").getAsString();

			JsonElement elTypes= joSQLVals.get("Types");
			if (elTypes==null) {
				ret[n]= KDOSGISQL.execSQL(connPrms, sql);
			} else {
				JsonArray jaTypes= elTypes.getAsJsonArray();
				JsonArray jaValues= joSQLVals.get("Values").getAsJsonArray();

				ret[n]= KDOSGISQL.execSQL(connPrms, sql, setPreparedValues(jaTypes, jaValues));
			}
		}
		
		return ret;
	}    

	private void case_execSelect(int reqNo, JsonObject rootObject, Session session) throws IOException {
		try {
			JsonObject joRes= new JsonObject();
			joRes.addProperty("ReqNo", reqNo);
			
			List<KDOSGISQL.TypeIterable> rets= execSelect(reqNo, rootObject);
			
			joRes.addProperty("data", rets.size());

			for (int n= 0; n< rets.size(); n++) {
				try (KDOSGISQL.TypeIterable ret= rets.get(n)) {
					
					JsonArray el_data= new JsonArray();
					
					JsonArray el_type= new JsonArray();
					for (int t : ret.types) el_type.add(t);
					
					for (Object[] r : ret.rows) {

						JsonArray jr= new JsonArray();
						for (int i= 0; i< r.length; i++) {
							Object o= r[i];
							jr.add((o==null)? null:o.toString());
						}
						el_data.add(jr);
					}

					joRes.add("data"+n, el_data);
					joRes.add("type"+n, el_type);
				}
			}
			session.getRemote().sendString(joRes.toString());
		} catch (Exception ex) {
			//ex.printStackTrace();
			JsonObject joError= new JsonObject();
			joError.addProperty("ReqNo", reqNo);
			joError.addProperty("Exception", KDStr.getExMessage(ex));
			session.getRemote().sendString(joError.toString());
		}
	}

	private List<KDOSGISQL.TypeIterable> execSelect(int reqNo, JsonObject joReg) throws Exception {
		JsonObject joLogin= joReg.get("Login").getAsJsonObject();
        ConnPrms connPrms= new ConnPrms(joLogin.get("server").getAsString(), joLogin.get("source").getAsString(), joLogin.get("user").getAsString(), joLogin.get("pass").getAsString());

		JsonArray jaSQLPach= joReg.get("SQLPach").getAsJsonArray();
		
		List<KDOSGISQL.TypeIterable> ret= new ArrayList<KDOSGISQL.TypeIterable>();

		for (int n= 0; n< jaSQLPach.size(); n++) {
			JsonObject joSQLVals= jaSQLPach.get(n).getAsJsonObject();
			String sql= joSQLVals.get("SQL").getAsString();

			JsonElement elTypes= joSQLVals.get("Types");
			if (elTypes==null) {
				ret.add(KDOSGISQL.execSelect(connPrms, sql));
			} else {
				JsonArray jaTypes= elTypes.getAsJsonArray();
				JsonArray jaValues= joSQLVals.get("Values").getAsJsonArray();

				ret.add(KDOSGISQL.execSelect(connPrms, sql, setPreparedValues(jaTypes, jaValues)));
			}
		}
		
		return ret;
	}
	
	private static Object[] setPreparedValues(JsonArray jaTypes, JsonArray jaValues) throws Exception {
		Object[] values= new Object[jaTypes.size()];

		for (int i1= 0; i1< jaTypes.size(); i1++) {
			
			JsonElement jeType= jaTypes.get(i1);
			if (jeType.isJsonNull()) values[i1]= null;
			else {
				String type= jeType.getAsString();
				switch (type) {
				case "java.lang.Integer":
					values[i1]= jaValues.get(i1).getAsInt();
					break;
				case "java.lang.Double":
					values[i1]= jaValues.get(i1).getAsDouble();
					break;
				case "java.lang.String":
					values[i1]= jaValues.get(i1).getAsString();
					break;
				//case "com.smartgwt.client.util.LogicalDate":
				case "java.util.Date":
					values[i1]= getDateFromString(jaValues.get(i1).getAsString());
					break;
				case "com.google.gwt.core.client.JavaScriptObject": {
					JsonArray ja= jaValues.get(i1).getAsJsonArray();
					int n= ja.size();
					byte[] b= new byte[n];
					for (int i2= 0; i2< n; i2++) {
						JsonElement je= ja.get(i2);
						b[i2]= je.getAsByte();
					}
					values[i1]= b;
					break;
				}
				default:
					throw new Exception("setPreparedValues error type: "+type+", value: "+String.valueOf(jaValues.get(i1)));
				}
			}
		}
		
		return values;
	}
	
	@SuppressWarnings("deprecation")
	public static Date getDateFromString(String s) {
		return new Date(Integer.parseInt(s.substring(0, 4))-1900, Integer.parseInt(s.substring(5, 7))-1, Integer.parseInt(s.substring(8, 10)));
	}
    
	private void case_getKDCompTypes(int reqNo, JsonObject rootObject, Session session) throws IOException {
		String project= rootObject.get("project").getAsString();
		String type= rootObject.get("type").getAsString();
		
		JsonObject joRes= new JsonObject();
		joRes.addProperty("ReqNo", reqNo);

		JsonArray el_comps= new JsonArray();
		JsonArray el_fields= new JsonArray();
		JsonArray el_steps= new JsonArray();
		JsonArray el_runs= new JsonArray();

		for (Entry<KBCmpArr<String>, KDCompMethod> tps : Activator.mKDComp2ServObj.entrySet()) {
			String[] key= tps.getKey().getArr();

			if (project.equals(key[0]) && type.equals(key[1])) {
				Class<?> classServ= tps.getValue().getClass();
				KDCompType kdCompModul= classServ.getAnnotation(KDCompType.class);
				if (kdCompModul!=null) {
					JsonArray el_comp= new JsonArray();
					JsonArray el_field= new JsonArray();
					JsonArray el_step= new JsonArray();
					JsonArray el_run= new JsonArray();
					
					for (int i= 2; i< key.length; i++) el_comp.add(key[i]);
					for (String s : kdCompModul.name()) el_comp.add(s);
					
					for (Field field : classServ.getDeclaredFields()) {
						{
							KDCompStepInfoPing kdCompStepInfoPing= field.getAnnotation(KDCompStepInfoPing.class);
							if (kdCompStepInfoPing!=null) {
								JsonArray el_var= new JsonArray();
								
								el_var.add(field.getName());
								el_var.add(kdCompStepInfoPing.name());
								el_var.add(kdCompStepInfoPing.width());
								el_var.add(kdCompStepInfoPing.height());
								el_var.add(isSubclass(field.getType(), Collection.class));
								
								el_step.add(el_var);
							}
						}
						{
							KDCompField kdCompField= field.getAnnotation(KDCompField.class);
							if (kdCompField!=null) {
								JsonArray el_var= new JsonArray();
	
								el_var.add(field.getName());
								el_var.add(field.getType().getName());
								el_var.add(kdCompField.name());
								el_var.add(kdCompField.clientType());
								el_var.add(kdCompField.width());
								el_var.add(kdCompField.decimal());
								el_var.add(kdCompField.maxlength());
								el_var.add((String)null); // резерв
								el_var.add(kdCompField.from());
								el_var.add(kdCompField.fKod());
								el_var.add(kdCompField.fName());
								el_var.add(kdCompField.isRequired());
								el_var.add(kdCompField.enblDsbl().name());
								el_var.add(field.getType().isArray());
								el_var.add(kdCompField.height());
								el_var.add(kdCompField.initValue());
								
								el_field.add(el_var);
							}
						}
					}
					
					for (Method method : classServ.getDeclaredMethods()) {
						{
							KDCompStepInfoPing kdCompStepInfoPing= method.getAnnotation(KDCompStepInfoPing.class);
							if (kdCompStepInfoPing!=null) {
								JsonArray el_var= new JsonArray();
								
								el_var.add(method.getName());
								el_var.add(kdCompStepInfoPing.name());
								el_var.add(kdCompStepInfoPing.width());
								el_var.add(kdCompStepInfoPing.height());
								el_var.add(isSubclass(method.getReturnType(), Collection.class));
								
								el_step.add(el_var);
							}
						}
						{
							KDCompRun kdCompRun= method.getAnnotation(KDCompRun.class);
							if (kdCompRun!=null) {
								JsonArray el_var= new JsonArray();
								
								el_var.add(method.getName());
								el_var.add(kdCompRun.name());
								
								Class<?> rt= method.getReturnType();
								if (rt.isArray()) {
									el_var.add("true");
									el_var.add(rt.getComponentType().getName());
								} else {
									el_var.add("false");
									el_var.add(rt.getName());
								}

								el_run.add(el_var);
							}
						}
					}
					
					el_comps.add(el_comp);
					el_fields.add(el_field);
					el_steps.add(el_step);
					el_runs.add(el_run);
				}
			}
		}
		
		joRes.add("comps", el_comps);
		joRes.add("fields", el_fields);
		joRes.add("steps", el_steps);
		joRes.add("runs", el_runs);

		session.getRemote().sendString(joRes.toString());
	}
	
	private void case_runKDComp(int reqNo, JsonObject rootObject, Session session) throws Exception {
		String project= rootObject.get("Project").getAsString();
		String type= rootObject.get("Type").getAsString();;
		String methodIdent= rootObject.get("MethodIdent").getAsString();
		String methodName= rootObject.get("Name").getAsString();
		JsonObject formData= rootObject.get("FormData").getAsJsonObject();

		KBCmpArr<String> key= new KBCmpArr<String>(new String[] {project, type, methodIdent});
		
		Class<?> classServ= Activator.mKDComp2ServObj.get(key).getClass();
		KDCompMethod serv= (KDCompMethod)classServ.getDeclaredConstructor().newInstance();

		for (Field field : classServ.getDeclaredFields()) {
			{
				KDCompStepInfoPing kdCompStepInfoPing= field.getAnnotation(KDCompStepInfoPing.class);
				if (kdCompStepInfoPing!=null) {
					String fieldType= field.getType().getName();
					if ("java.util.Collection".equals(fieldType)) {
						field.setAccessible(true);
						field.set(serv, new ConcurrentLinkedQueue<String>());
					}
				}
			}

			{
				KDCompField kdCompField= field.getAnnotation(KDCompField.class);
				if (kdCompField!=null) {
					String clientType= kdCompField.clientType();
					field.setAccessible(true);
					
					if ("ConnPrms".equals(clientType)) {
						JsonObject login= rootObject.get("Login").getAsJsonObject();
						ConnPrms connPrms= new ConnPrms(login.get("server").getAsString(), login.get("source").getAsString(), login.get("user").getAsString(), login.get("pass").getAsString());
						field.set(serv, connPrms);
					} else {
						String fieldName= field.getName();
						JsonElement je_Val= formData.get(fieldName);
						if (je_Val==null) continue;
	
						switch (clientType) {
						case "LSEnum": {
							JsonArray jsAr= je_Val.getAsJsonArray();
							
							LSEnumValue le= new LSEnumValue();
		
							JsonElement je_grp= jsAr.get(0);
							if (!je_grp.isJsonNull()) le.Grp= je_grp.getAsInt();
		
							JsonElement je_numBeg= jsAr.get(1);
							if (!je_numBeg.isJsonNull()) le.NumBeg= je_numBeg.getAsString();
		
							JsonElement je_numEnd= jsAr.get(2);
							if (!je_numEnd.isJsonNull()) le.NumEnd= je_numEnd.getAsString();
		
							JsonElement je_HsOrStrByLSBeg= jsAr.get(3);
							if (!je_HsOrStrByLSBeg.isJsonNull()) le.HsOrStrByLSBeg= je_HsOrStrByLSBeg.getAsInt();
		
							JsonElement je_HsByLSEnd= jsAr.get(4);
							if (!je_HsByLSEnd.isJsonNull()) le.HsByLSEnd= je_HsByLSEnd.getAsInt();
		
							JsonElement je_PlatOnly= jsAr.get(5);
							if (!je_PlatOnly.isJsonNull()) le.PlatOnly= je_PlatOnly.getAsInt();
							
							field.set(serv, le);
							break;
						}
						case "Month": {
							if (!je_Val.isJsonNull()) field.set(serv, KDTime.Date2Month(getDateFromString(je_Val.getAsString())));
							break;
						}
						case "Money": {
							if (!je_Val.isJsonNull()) field.set(serv, je_Val.getAsDouble());
							break;
						}
						case "TextArea": {
							if (!je_Val.isJsonNull()) field.set(serv, je_Val.getAsString());
							break;
						}
						case "FileUpload": {
							if (!je_Val.isJsonNull()) {
								File[] uplFiles= FileUploadData01.getUplFiles(Long.valueOf(je_Val.getAsString()));
								if (uplFiles!=null && 0<uplFiles.length) {
									if (field.getType().isArray()) field.set(serv, uplFiles);
									else field.set(serv, uplFiles[0]);
								}
							}
							break;
						}
						case "NameSprav":
						case "KodNameSprav": {
							if (!je_Val.isJsonNull()) field.set(serv, je_Val.getAsInt());
							break;
						}
						case "": {
							String fieldType= field.getType().getName();
							
							switch (fieldType) {
							case "int": {
								if (!je_Val.isJsonNull()) field.set(serv, je_Val.getAsInt());
								break;
							}
							case "boolean": {
								if (!je_Val.isJsonNull()) field.set(serv, je_Val.getAsBoolean());
								break;
							}
							case "double": {
								if (!je_Val.isJsonNull()) field.set(serv, je_Val.getAsDouble());
								break;
							}
							case "java.lang.String": {
								if (!je_Val.isJsonNull()) field.set(serv, je_Val.getAsString());
								break;
							}
							case "java.io.File": {
								if (!je_Val.isJsonNull()) {
									File[] uplFiles= FileUploadData01.getUplFiles(Long.valueOf(je_Val.getAsString()));
									if (uplFiles!=null && 0<uplFiles.length) {
										if (field.getType().isArray()) field.set(serv, uplFiles);
										else field.set(serv, uplFiles[0]);
									}
								}
								break;
							}
							case "java.util.Date": {
								if (!je_Val.isJsonNull()) {
									String cDate= je_Val.getAsString();
									if (!KDStr.isNullOrEmpty(cDate)) {
										Date dt= KDTime.fromXML(cDate.substring(0, 10));
										field.set(serv, dt);
									}
								}
								break;
							}
							default:
								throw new IllegalStateException("Invalid valType: "+fieldType);
							}
							
							break;
						}				
						default:
							throw new IllegalStateException("Invalid clientType: "+clientType);
						}
					}
				}
			}
		}

		final int compIdent= aiCompIdent.incrementAndGet();

		mCompIdent2RunMeth.put(compIdent, serv);
		try {
			{
				JsonObject joRes= new JsonObject();
				joRes.addProperty("ReqNo", reqNo);
				joRes.addProperty("CompIdent", compIdent);
				session.getRemote().sendString(joRes.toString());
			}
			
			serv.ping(/*mCF2Mess*/);
			
			String rets= null;
			File[] fileXLS= null;
			File[] fileBin= null;
			Throwable ex= null;
			//boolean isThrowable= false;
			try {
				Method method= classServ.getDeclaredMethod(methodName);
				method.setAccessible(true);

				String retName;
				boolean isArray;
				{
					Class<?> rt= method.getReturnType();
					isArray= rt.isArray();
					if (isArray) {
						retName= rt.getComponentType().getName();
					} else {
						retName= rt.getName();
					}
				}
				

				Object r= method.invoke(serv);
				/*
				Object r;
				try {
					r= method.invoke(serv);
				} catch (Throwable oex) {
					System.out.println("method.invoke(serv)");
					oex.printStackTrace(System.out);
					throw oex;
				} finally {
					System.out.println("finally method.invoke(serv)");
				}
				*/
				
				/*
				System.out.println("Object r= method.invoke(serv)");
				System.out.println(compIdent);
				System.out.println(r);
				*/
				
				switch (retName) {
				case "com.rirc.OSGI01.SoftRep": {
					SoftRep[] softReps= (isArray)? (SoftRep[])r: new SoftRep[] {(SoftRep)r};
					
					RunSoftRep runSoftRep= new RunSoftRep(softReps);
					mCompIdent2RunMeth.put(compIdent, runSoftRep);
					runSoftRep.ping(/*mCF2Mess*/);
					fileXLS= new File[] {runSoftRep.makes()};
				}
				break;
				case "java.io.File":
					fileBin= (isArray)? (File[])r: new File[] {(File)r};
				break;
				default:
					if (r!=null) rets= String.valueOf(r);
				}
			} catch (InvocationTargetException itex) {
				ex= itex.getTargetException();
			} catch (Exception oex) {
				ex= oex;
			} catch (Throwable oex) {
				ex= oex;
				//isThrowable= true;
			}
			
			/*
			if (ex!=null) {
				System.out.println("case_runKDComp");
				ex.printStackTrace();
			}
			*/

			{
				JsonObject joRes= new JsonObject();
				joRes.addProperty("CompIdent", compIdent);
				if (ex!=null) {
					joRes.addProperty("Exception", KDStr.getExAllInfo(ex));
				}
				if (fileXLS!=null) {
					Long sysTime= System.currentTimeMillis();
					mSysTime2TmpFile.put(sysTime, fileXLS);
					mSes2CITmpFiles.computeIfAbsent(session, (k)-> new HashSet<Long>()).add(sysTime);
					joRes.addProperty("Rets", sysTime);
					joRes.addProperty("FCnt", fileXLS.length);
				} else {
					if (fileBin!=null) {
						Long sysTime= System.currentTimeMillis();
						mSysTime2TmpFile.put(sysTime, fileBin);
						mSes2CITmpFiles.computeIfAbsent(session, (k)-> new HashSet<Long>()).add(sysTime);
						joRes.addProperty("Rets", sysTime);
						joRes.addProperty("FCnt", fileBin.length);
						joRes.addProperty("FName", fileBin[0].getName());
					} else {
						if (rets!=null) joRes.addProperty("Rets", rets);
					}
				}
				session.getRemote().sendString(joRes.toString());
			}
		} finally {
			mCompIdent2RunMeth.remove(compIdent);
		}
	}
	
	private void case_pingKDComp(int reqNo, JsonObject rootObject, Session session) throws Exception {
		int compIdent= rootObject.get("CompIdent").getAsInt();

		JsonArray jaStepNames= rootObject.get("StepNames").getAsJsonArray();
		
		JsonObject joRes= new JsonObject();
		joRes.addProperty("ReqNo", reqNo);
		

		KDCompMethod serv= mCompIdent2RunMeth.get(compIdent);
		if (serv!=null) {
			serv.ping(/*mCF2Mess*/);
			
			joRes.addProperty("IsRun", true);

			JsonArray jaRets= new JsonArray();
			
			jaStepNames.forEach((el)-> {
				String stepName= el.getAsString();
				
				try {
					Field field= serv.getClass().getDeclaredField(stepName);
					field.setAccessible(true);
					
					Object oval= field.get(serv);
					if (oval instanceof Queue<?>) {
						Queue<?> qval= (Queue<?>)oval;
						Object o;
						while ((o= qval.poll())!=null) jaRets.add(o.toString());
					} else {
						String si= String.valueOf(oval);
						jaRets.add(si);
					}
				} catch (NoSuchFieldException ex) {
				} catch (SecurityException | IllegalAccessException ex) {
					throw new RuntimeException(ex);
				}

				try {
					Method method= serv.getClass().getDeclaredMethod(stepName);
					method.setAccessible(true);
					
					String si= String.valueOf(method.invoke(serv));
					jaRets.add(si);
				} catch (NoSuchMethodException ex) {
				} catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
					throw new RuntimeException(ex);
				}
			});
			
			joRes.add("Rets", jaRets);
		} else {
			joRes.addProperty("IsRun", false);
		}
		
		String res= joRes.toString();
		//System.out.println(res);
		try {
			session.getRemote().sendString(res);
		} catch (IOException ex) { //IllegalStateException  RuntimeException
			//System.out.println("session.getRemote().sendString(res)");
			ex.printStackTrace(System.out); // !!!!
			//System.out.println(res);
		}
		//session.getRemote().sendString(res, this);
	}

	static boolean isSubclass(Class<?> c1, Class<?> c2) {
		try {
			c1.asSubclass(c2);
			return true;
		} catch (ClassCastException ex) {
			return false;
		}
	}
}

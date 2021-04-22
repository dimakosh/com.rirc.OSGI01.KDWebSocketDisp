package com.rirc.OSGI01.KDWebSocketDisp;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
import com.rirc.OSGI01.KDCompStepInfoPing;
import com.rirc.OSGI01.KDExecutors;
import com.rirc.OSGI01.KDHttpServlet;
import com.rirc.OSGI01.KDStr;
import com.rirc.OSGI01.KDTime;
import com.rirc.OSGI01.LSEnumValue;
import com.rirc.OSGI01.SoftRep;
import com.rirc.OSGI01.RunSoftRep.RunSoftRep;

@Component
public class KDCompTypeShowAndRunServlet01 extends KDHttpServlet {
	private static final long serialVersionUID = 1L;

	@Reference
    private HttpService httpService;

    @Activate
    public void activate() throws Exception {
        httpService.registerServlet("/KDCompTypeShowAndRunServlet01", this, null, null);
    }

    @Deactivate
    public void deactivate() throws Exception {
        httpService.unregister("/KDCompTypeShowAndRunServlet01");
    }
    
    private static class PrData {
		final private CompletableFuture<Object> cf;
		final private KDCompMethod serv;
		final private Method method;

    	public PrData(CompletableFuture<Object> _cf, KDCompMethod _serv, Method _method) {
			cf = _cf;
			serv = _serv;
			method= _method;
		}
    	
		public CompletableFuture<Object> getCf() {
			return cf;
		}
		public KDCompMethod getServ() {
			return serv;
		}
		public Method _getMethod() {
			return method;
		}
    }

	private final ConcurrentMap<UUID,PrData> mUUID2CF= new ConcurrentHashMap<UUID,PrData>();
	protected static final ConcurrentMap<Long,File[]> mSysTime2TmpFile= new ConcurrentHashMap<Long,File[]>();
	
	private static UUID runKDComp(ConcurrentMap<UUID,PrData> mUUID2CF, String runKDComp_json) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		JsonElement element= JsonParser.parseString(runKDComp_json);
		JsonObject rootObject= element.getAsJsonObject();
		
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
		
		Method method= classServ.getDeclaredMethod(methodName);
		method.setAccessible(true);

		UUID compIdent= UUID.randomUUID(); 
		CompletableFuture<Object> cf= CompletableFuture.supplyAsync(()-> {
			serv.ping();
			try {
				return method.invoke(serv);
			} catch (Exception ex) { // InvocationTargetException
				throw new RuntimeException(ex);
			}
    	}, KDExecutors.getStdExecutor());
		mUUID2CF.put(compIdent, new PrData(cf, serv, method));
		
		return compIdent;
	}
	
	private static JsonObject pingKDComp(ConcurrentMap<UUID,PrData> mUUID2CF, String pingKDComp_json) {
		JsonElement element= JsonParser.parseString(pingKDComp_json);
		JsonObject rootObject= element.getAsJsonObject();

		UUID compIdent= UUID.fromString(rootObject.get("CompIdent").getAsString());
		
		PrData prData= mUUID2CF.remove(compIdent);
		KDCompMethod serv= prData.getServ();
		
		serv.ping();

		JsonObject joRes= new JsonObject();

		try {
			Object r= prData.getCf().get(1, TimeUnit.SECONDS);

			String retName;
			boolean isArray;
			{
				Method method= prData._getMethod();
				if (method!=null) {
					Class<?> rt= method.getReturnType();
					isArray= rt.isArray();
					if (isArray) {
						retName= rt.getComponentType().getName();
					} else {
						retName= rt.getName();
					}
				} else {
					isArray= false;
					retName= "java.io.File";
				}
			}
			
			switch (retName) {
			case "com.rirc.OSGI01.SoftRep": {
				SoftRep[] softReps= (isArray)? (SoftRep[])r: new SoftRep[] {(SoftRep)r};
				
				RunSoftRep runSoftRep= new RunSoftRep(softReps);

				CompletableFuture<Object> cf= CompletableFuture.supplyAsync(()-> {
					runSoftRep.ping();
					try {
						return runSoftRep.makes();
					} catch (Exception ex) {
						throw new RuntimeException(ex);
					}
				});				
				mUUID2CF.put(compIdent, new PrData(cf, runSoftRep, null));

				JsonArray jaRets= new JsonArray();
				joRes.add("StepInfo", jaRets);
			}
			break;
			case "java.io.File": {
				File[] fileBin= (isArray)? (File[])r: new File[] {(File)r};
				
				Long sysTime= System.currentTimeMillis();
				mSysTime2TmpFile.put(sysTime, fileBin);
				joRes.addProperty("Rets", sysTime);
				joRes.addProperty("FCnt", fileBin.length);
				joRes.addProperty("FName", fileBin[0].getName());
			}
			break;
			default:
				if (r!=null) joRes.addProperty("Rets", String.valueOf(r));
			}
		} catch (InterruptedException | CancellationException ex) {
			joRes.addProperty("Exception", KDStr.getExMessage(ex));
		} catch (ExecutionException ex1) {
			Throwable ex2= ex1.getCause().getCause();
			if (ex2 instanceof InvocationTargetException) ex2= ex2.getCause(); 
			joRes.addProperty("Exception", KDStr.getExAllInfo(ex2));
		} catch (TimeoutException exTimeoutException) {
			JsonArray jaStepNames= rootObject.get("StepNames").getAsJsonArray();
			
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

			joRes.add("StepInfo", jaRets);

			mUUID2CF.put(compIdent, prData);
		}

		return joRes;
	}

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");
		res.setCharacterEncoding("UTF-8");
		res.setContentType("text/plain; charset=UTF-8");
		res.addHeader("Access-Control-Allow-Origin", "*");
		
		try {
			String runKDComp_json= req.getParameter("runKDComp");
			if (runKDComp_json!=null) {
				res.getWriter().print(runKDComp(mUUID2CF, runKDComp_json));
			}
			
			String pingKDComp_json= req.getParameter("pingKDComp");
			if (pingKDComp_json!=null) {
				res.getWriter().print(pingKDComp(mUUID2CF, pingKDComp_json));
			}			
		} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException ex) {
			throw new ServletException(KDStr.getExMessage(ex));
		} catch (InvocationTargetException ex) {
			throw new ServletException(ex);
		}
    }
	
	@SuppressWarnings("deprecation")
	public static Date getDateFromString(String s) {
		return new Date(Integer.parseInt(s.substring(0, 4))-1900, Integer.parseInt(s.substring(5, 7))-1, Integer.parseInt(s.substring(8, 10)));
	}
}

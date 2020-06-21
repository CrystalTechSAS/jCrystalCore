package jcrystal.server.web.servlet;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import jcrystal.main.data.ClientContext;
import jcrystal.model.web.JCrystalMultipartWebService;
import jcrystal.model.web.JCrystalWebService;
import jcrystal.model.web.JCrystalWebServiceManager;
import jcrystal.reflection.StoredProcedure;
import jcrystal.reflection.annotations.ws.SingleCallWS;
import jcrystal.server.GeneradorEntidadPush;
import jcrystal.server.async.Async;
import jcrystal.types.IJType;
import jcrystal.types.JClass;
import jcrystal.types.utils.GlobalTypes;
import jcrystal.utils.StringUtils;
import jcrystal.utils.langAndPlats.AbsCodeBlock;
import jcrystal.utils.langAndPlats.AbsICodeBlock;
import jcrystal.utils.langAndPlats.JavaCode;
import jcrystal.utils.langAndPlats.AbsCodeBlock.B;

public class GeneradorServlet {
	private final ClientContext context;
	public GeneradorServlet(ClientContext context) {
		this.context = context;
	}
	public void generarServlets() throws Exception{
		if(context.input.CHECKED_CLASSES.contains(HttpServlet.class.getName())) {
			generarOnLoadServlet();
			generarAbsSubServlet();
			if(!context.data.clases_push_entities.isEmpty())
				generarServletPush();
		}
		registerClients();
		if(context.input.CHECKED_CLASSES.contains(HttpServlet.class.getName())) {
			createSubServlets(false);
			createSubServlets(true);
		}
	}
	
	private void generarOnLoadServlet() throws Exception {
		new JavaCode() {{
			$("package " + context.input.SERVER.WEB.getBasePackage() + ".servlets;");
			$("import javax.servlet.ServletException;");
			$("import javax.servlet.http.*;");
			$("@javax.servlet.annotation.WebServlet(name = \"_ah_warmup\", urlPatterns = {\"/_ah/warmup\"}, loadOnStartup = 1)");
			$("public class OnLoadServlet extends HttpServlet", () -> {
				$("private static boolean init = true;");
				$("@Override public void doGet(HttpServletRequest req, HttpServletResponse resp)", () -> {
					$("try",()->{
						$("this.init();");
					});
					$SingleCatch("Exception ex",  "ex.printStackTrace();");
				});
				$("@Override public void init() throws ServletException",()->{
					$if("init",()->{
						$("super.init();");
						$("init = false;");
						$("jcrystal.context.CrystalContext.initialize();");
						context.data.globalServerLoadMethods.forEach(m->{
							$("try",()->{
								if(m.isAnnotationPresent(SingleCallWS.class)) {
									$("jcrystal.utils.SingleCallWSEntity $version = jcrystal.utils.SingleCallWSEntity.get(\"onload:" + m.declaringClass.name + "." + m.name + "\");");
									$if("$version.version() < " + m.getAnnotation(SingleCallWS.class).value(),()->{
										$("$version.version("+m.getAnnotation(SingleCallWS.class).value()+").put();");
										$($(m.declaringClass)+"."+m.name()+"();");
									});
								}else
									$($(m.declaringClass)+"."+m.name()+"();");
							});
							$SingleCatch("Exception ex",  "ex.printStackTrace();");
						});
					});
				});
			});
			context.output.exportFile(this, context.input.SERVER.WEB.getBasePackage().replace(".", "/") + "/servlets/OnLoadServlet.java");
		}};
	}
	public static void putCatchs(ClientContext context, AbsICodeBlock code) {
		String exceptionLog = "";
		if(context.input.SERVER.DEBUG.LOG_EXCEPTIONS)
			exceptionLog = "log.log(java.util.logging.Level.INFO,\"error\", ex);";
		code.$SingleCatch("NumberFormatException ex", exceptionLog+"resp.getWriter().print(\"{\\\"success\\\":2, \\\"mensaje\\\":\\\"Invalid request\\\"}\");");
		code.$SingleCatch("org.json.JSONException ex", exceptionLog+"resp.getWriter().print(\"{\\\"success\\\":2,\\\"code\\\": 500, \\\"mensaje\\\":\\\"Invalid JSON object\\\"}\");");
		code.$SingleCatch("ValidationException ex", exceptionLog+"resp.getWriter().print(\"{\\\"success\\\":2, \\\"mensaje\\\":\\\"\" + ex.getMessage() + \"\\\"}\");");
		code.$SingleCatch("InternalException ex",
				exceptionLog+"resp.getWriter().print(\"{\\\"success\\\":2,\\\"code\\\":\" + ex.code + \", \\\"mensaje\\\":\\\"\" + ex.getMessage() + \"\\\"}\");");
		code.$("catch(Throwable ex)", () -> {
			code.$("resp.setStatus(500);");
			code.$("log.log(java.util.logging.Level.SEVERE,\"error\", ex);");
		});
	}
	
	private void generarAbsSubServlet() throws Exception {
		new JavaCode() {
			{
				$("package " + context.input.SERVER.WEB.getBasePackage() + ".servlets;");
				$("import org.json.JSONObject;");
				$("import org.json.JSONTokener;");
				$("import jcrystal.utils.InternalException;");
				$("import jcrystal.utils.ValidationException;");
				$("import jcrystal.datetime.*;");
				$("import javax.servlet.http.*;");
				$("import java.io.IOException;");
				$("import java.sql.SQLException;");
				$("import java.util.logging.Level;");
				$("import java.util.logging.Logger;");
				$("import static jcrystal.utils.ServletUtils.*;");
				$("@SuppressWarnings(\"unused\")");
				$("public abstract class AbsSubServlet extends HttpServlet", () -> {
					$("private static final long serialVersionUID = 1L;");
					$("private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(AbsSubServlet.class.getName());");
					$("public final void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException", () -> {
						if(context.input.SERVER.DEBUG.CORS)
							$if("req.getHeader(\"Origin\") != null",()->{
								$("resp.setHeader(\"Access-Control-Allow-Origin\", req.getHeader(\"Origin\"));");											
							});
						$("final String path = req.getServletPath();");
						$("resp.setCharacterEncoding(\"UTF-8\");");
						$("try", () -> {
							$("resp.setContentType(\"application/json\");");
							$("doGet(path, req, resp);");
						});
						$("catch(jcrystal.http.responses.HttpResponseException ex)", () -> {
							$("resp.setContentType(\"text/plain\");");
							$("resp.setStatus(ex.getCode());");
							$if("ex.getContent() != null",()->{
								$("resp.getWriter().print(ex.getContent());");
							});
						});
						putCatchs(context, this);
					});
					$("public final void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException",() -> {
						if(context.input.SERVER.DEBUG.CORS)
							$if("req.getHeader(\"Origin\") != null",()->{
								$("resp.setHeader(\"Access-Control-Allow-Origin\", req.getHeader(\"Origin\"));");											
							});
						$("final String path = req.getServletPath();");
						$("resp.setCharacterEncoding(\"UTF-8\");");
						$("try", () -> {
							$("resp.setContentType(\"application/json\");");
							$("doPost(path, req, resp);");
						});
						$("catch(jcrystal.http.responses.HttpResponseException ex)", () -> {
							$("resp.setContentType(\"text/plain\");");
							$("resp.setStatus(ex.getCode());");
							$if("ex.getContent() != null",()->{
								$("resp.getWriter().print(ex.getContent());");
							});
						});
						putCatchs(context, this);
					});
					if(context.input.SERVER.DEBUG.CORS) {
						$("public void doOptions(HttpServletRequest req, HttpServletResponse resp) throws IOException", () -> {
							$("resp.setHeader(\"Access-Control-Allow-Origin\", req.getHeader(\"Origin\"));");
							$("resp.setHeader(\"Access-Control-Allow-Credentials\", \"true\");");
							$("resp.setHeader(\"Access-Control-Allow-Methods\", \"POST, GET, OPTIONS, DELETE\");");
							$("resp.setHeader(\"Access-Control-Max-Age\", \"3600\");");
							$("resp.setHeader(\"Access-Control-Allow-Headers\", \"Authorization, Content-Type, Accept, X-Requested-With, remember-me\");");
						});
					}
					$("public abstract void doGet(String path, HttpServletRequest req, HttpServletResponse resp)throws Exception;");
					$("public abstract void doPost(String path, HttpServletRequest req, HttpServletResponse resp)throws Exception;");
					for(IJType type : GlobalTypes.primitivesAndObjects) {
						String methodName = StringUtils.capitalize(type.getSimpleName());
						if(type.isPrimitiveObjectType())
							methodName = methodName + "Obj";
						IJType primitive = type.getPrimitiveType();
						IJType objectPrimitive = type.getObjectType();
						String parseExp; 
						if(primitive.is(char.class))
							parseExp = "val.charAt(0)";
						else
							parseExp = objectPrimitive.getSimpleName()+".parse"+StringUtils.capitalize(primitive.getSimpleName())+"(val)";
						$("public static " + type.getSimpleName() + " opt" + methodName + "(HttpServletRequest req, String name, " + type.getSimpleName() + " defaultValue)",()->{
							$("String val = req.getParameter(name);");
							$if("val == null || val.isEmpty()",()->{
								$("return defaultValue;");
							});
							$("return "+parseExp+";");
													
						});
						$("public static " + type.getSimpleName() + " opt" + methodName + "(HttpServletRequest req, String name)",()->{
							$("String val = req.getParameter(name);");
							$if("val == null || val.isEmpty()",()->{
								if(type.isPrimitive())
									$("return "+GlobalTypes.defaultValuesStr.get(type)+";");
								else
									$("return null;");
							});
							$("return "+parseExp+";");
						});
						$("public static " + type.getSimpleName() + " get" + methodName + "(HttpServletRequest req, String name)",()->{
							$("String val = req.getParameter(name);");
							$if("val == null || val.isEmpty()",()->{
								$("throw new jcrystal.errors.ErrorException(\"No given value on request for value: \" + name);");
							});
							$("return "+parseExp+";");
						});
					}
					$("public static String optString(HttpServletRequest req, String name, String defaultValue)",()->{
						$("String val = req.getParameter(name);");
						$if("val == null || val.isEmpty()",()->{
							$("return defaultValue;");
						});
						$("return val;");
												
					});
					$("public static String optString(HttpServletRequest req, String name)",()->{
						$("String val = req.getParameter(name);");
						$if("val == null || val.isEmpty()",()->{
							$("return null;");
						});
						$("return val;");
					});
					$("public static String getString(HttpServletRequest req, String name)",()->{
						$("String val = req.getParameter(name);");
						$if("val == null || val.isEmpty()",()->{
							$("throw new jcrystal.errors.ErrorException(\"No given value on request for value: \" + name);");
						});
						$("return val;");
					});
				});
				context.output.exportFile(this, context.input.SERVER.WEB.getBasePackage().replace(".", "/") + "/servlets/AbsSubServlet.java");
			}
			void createGetMethods(IJType type) {
				
			}
		};
	}
	private void generarServletPush() throws Exception {
		final JavaCode GETS = new JavaCode();
		final JavaCode POSTS = new JavaCode();
		GETS.new B() {{
			final AbsCodeBlock GET_BLOCK = this.P;
			$("@Override");
			$("public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException", () -> {
				if(context.input.SERVER.DEBUG.CORS)
					$("resp.setHeader(\"Access-Control-Allow-Origin\", req.getHeader(\"Origin\"));");
				$("final String path = req.getPathInfo();");
				$("resp.setContentType(\"application/json\");");
				$("resp.setCharacterEncoding(\"UTF-8\");");
				$("try", () -> {
					$("switch(path)", () -> {
						POSTS.new B() {{
								final AbsCodeBlock POST_BLOCK = this.P;
								$("@Override");
								$("public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException",() -> {
									if(context.input.SERVER.DEBUG.CORS)
										$("resp.setHeader(\"Access-Control-Allow-Origin\", req.getHeader(\"Origin\"));");
									$("final String path = req.getPathInfo();");
									$("resp.setContentType(\"application/json\");");
									$("resp.setCharacterEncoding(\"UTF-8\");");
									$("try", () -> {
										$("switch(path)", () -> {
											for (JClass pushClass : context.data.clases_push_entities)
												GeneradorEntidadPush.generarServletCode(context, pushClass, POST_BLOCK);
											$("default: send404(resp);break;");
										});
									});
									putCatchs(context, this);
								});
						}};
						$("default: send404(resp);break;");
					});
				});
				putCatchs(context, this);
			});
		}};
		ArrayList<String> INTERFACES = new ArrayList<>();
		TreeSet<String> creadas = new TreeSet<>();
		for (JCrystalWebServiceManager clase : context.data.services)
			for (StoredProcedure sp : clase.storedProcedures)
				sp.crearInterfaces(creadas, INTERFACES);
		StringWriter sw = new StringWriter();
		try (PrintWriter pw = new PrintWriter(sw)) {
			pw.println("package " + context.input.SERVER.WEB.getBasePackage() + ".interfaces;");
			pw.println(
			"public class JCrystalInterfaces{");
			for (String h : INTERFACES)
			pw.println(h);
			pw.println("}");
		}
		context.output.send(sw.toString(), context.input.SERVER.WEB.getBasePackage().replace(".", "/") + "/interfaces/JCrystalInterfaces.java");
		new JavaCode() {{
			$("package " + context.input.SERVER.WEB.getBasePackage() + ".servlets;");
			$("import jcrystal.utils.InternalException;");
			$("import jcrystal.utils.ValidationException;");
			$("import javax.servlet.http.*;");
			$("import java.io.IOException;");
			$("import static jcrystal.utils.ServletUtils.*;");
			$("@javax.servlet.annotation.WebServlet(name = \"ServletPush\",urlPatterns = {\""+context.input.SERVER.WEB.servlet_root_path +"/pushs/*\"})");
			$("public class ServletPush extends HttpServlet", () -> {
				$("private static final long serialVersionUID = " + context.back.random.nextLong() + "L;");
				$("private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(ServletPush.class.getName());");
				$append(GETS);
				$append(POSTS);
			});
			context.output.exportFile(this, context.input.SERVER.WEB.getBasePackage().replace(".", "/") + "/servlets/ServletPush.java");
		}};
		
	}
	private void registerClients() {
		context.data.services.stream().filter(clase->!clase._metodos.isEmpty()).forEach(clase->{
			if(clase.isMultipart)
				context.utils.clientGenerator.registrar(clase);
			else
				for (JCrystalWebService metodo : clase._metodos) {
					if(metodo.isAnnotationPresent(Async.class));
					else if (Modifier.isPublic(metodo.unwrappedMethod.getModifiers())) {
						context.utils.clientGenerator.registrar(metodo.padre, metodo);			
					}
				}
		});
	}
	private static void getPath(List<String> rutas, JCrystalWebService metodo) {
		if(metodo.name().equals("index"))
			rutas.add("\""+metodo.getPath(null)+"/\"");
		rutas.add("\""+metodo.getPath(null)+"\"");
	}
	private boolean needsJpa = false;
	private boolean shouldBeOnServlet(JCrystalWebServiceManager manager, boolean multipart) {
		if(multipart)
			return manager.isMultipart || manager._metodos.stream().anyMatch(f->shouldBeOnServlet(f, multipart));
		else
			return manager._metodos.stream().anyMatch(f->shouldBeOnServlet(f, multipart));
		
	}
	private boolean shouldBeOnServlet(JCrystalWebService ws, boolean multipart) {
		if(ws.external)
			return false;
		if(multipart)
			return ws.isMultipart();
		else
			return ws.isNotMultipart();
	}
	private void createSubServlets(boolean multipart) {
		needsJpa = false;
		context.data.services.stream()
			.filter(clase->!clase._metodos.isEmpty())
			.filter(f->shouldBeOnServlet(f, multipart))
			.collect(Collectors.groupingBy(f->f.getSubservletClassName())).forEach((ruta, lista)->{
			lista.sort((m1,m2)->m1.clase.name.compareTo(m2.clase.name));
			String nombre = multipart ? ruta.replace("SubServlet", "SubServletMP") : ruta;
			List<String> rutas = new ArrayList<>();
			for(JCrystalWebServiceManager manager : lista) {
				if(manager.isMultipart) {
					if(multipart)
						rutas.add("\""+manager.getPath(null)+"\"");
				}else
					manager._metodos.stream().filter(f->shouldBeOnServlet(f, multipart)).sorted().forEach(metodo->getPath(rutas, metodo));
			}
			String rutasManejadas = rutas.stream().collect(Collectors.joining(", "));
			new JavaCode() {{
				$("package " + context.input.SERVER.WEB.getBasePackage() + ".servlets;");
				$("import org.json.JSONObject;");
				$("import org.json.JSONTokener;");
				$("import jcrystal.utils.InternalException;");
				$("import jcrystal.utils.ValidationException;");
				$("import jcrystal.datetime.*;");
				$("import javax.servlet.http.*;");
				$("import java.io.IOException;");
				$("import java.sql.SQLException;");
				$("import java.util.logging.Level;");
				$("import java.util.logging.Logger;");
				$("import static jcrystal.utils.ServletUtils.*;");
				$("@SuppressWarnings(\"unused\")");
				if(multipart)
					$("@javax.servlet.annotation.MultipartConfig(location=\"/tmp\", fileSizeThreshold=1024*1024, maxFileSize=1024*1024*150, maxRequestSize=1024*1024*150)");
				$("@javax.servlet.annotation.WebServlet(name = \"" + nombre + "\",urlPatterns = {"+rutasManejadas+"})");
				$("public class " + nombre + " extends AbsSubServlet", () -> {
					$("private static final long serialVersionUID = 1L;");
					$("private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(" + nombre + ".class.getName());");
					
					putServletMethods(this, lista, multipart);
					
					for (JCrystalWebServiceManager clase : lista) {
						if(clase.isMultipart) {
							long cuentaSalidas = clase._metodos.stream().filter(f->f.isWritatableResponse()).count();
							$("public static class SubServlet"+clase.clase.simpleName,()->{
								String instanceName = "$instance"+clase.clase.name().replace(".", "_");
								$("private "+clase.clase.name().replace("$", ".")+" " + instanceName + " = new "+clase.clase.name().replace("$", ".")+"();");
								for (JCrystalWebService metodo : clase._metodos)
									if(metodo.isWritatableResponse())
										$($(metodo.getReturnType())+" $salida"+(cuentaSalidas>1?StringUtils.capitalize(metodo.name()):"")+";");
								$("public static void doMultipart(HttpServletRequest req, HttpServletResponse resp)throws Exception",()->{
									GeneradorMetodoServlet.putAuthCondition(context, this, new JCrystalMultipartWebService(context, clase), ()->{
										if(clase.clase.hasEmptyConstructor())
											$("SubServlet"+clase.clase.simpleName+" subServlet = new SubServlet"+clase.clase.simpleName+"();");
										else {
											String params = "SubServlet"+clase.clase.simpleName+"("+clase.clase.constructors.get(0).params.stream().map(f->f.name).collect(Collectors.joining(", "));
											$("final "+clase.clase.name()+" subServlet = new "+params+");");
										}
										clase.managedAttributes.forEach(m->$("subServlet."+instanceName+"."+m.name+" = " + m.name+";"));
										for (JCrystalWebService metodo : clase._metodos)
											if(metodo.isWritatableResponse())
												$("subServlet.$salida"+(cuentaSalidas>1?StringUtils.capitalize(metodo.name()):"") + " = subServlet."+metodo.getSubservletMethodName()+"(req, resp);");
											else
												$("subServlet."+metodo.getSubservletMethodName()+"(req, resp);");
										$("subServlet.doOutput(resp);");										
									});
								});
								for (JCrystalWebService metodo : clase._metodos) {
									if (!metodo.external && Modifier.isPublic(metodo.unwrappedMethod.getModifiers())) {
										GeneradorMetodoServlet gen = new GeneradorMetodoServlet(context, metodo);
										gen.crearMetodoServlet(this);
										needsJpa |= gen.needsJpaEntityManager;
									}
								}
								$("void doOutput(HttpServletResponse resp)throws Exception",()->{
									if(cuentaSalidas==0) {
										$("resp.getWriter().print(\"{\\\"success\\\":1}\");");										
									}else if(cuentaSalidas==1){
										for (JCrystalWebService metodo : clase._metodos) {
											if (metodo.isWritatableResponse()) {
												GeneradorMetodoServlet gen = new GeneradorMetodoServlet(context, metodo);
												gen.writeResponse(this);
											}
										}			
									}else {
										throw new NullPointerException("Unssuported count " + cuentaSalidas);
									}
								});
							});
						}else
							clase._metodos.stream().sorted((m1,m2)->m1.getPath(null).compareTo(m2.getPath(null))).forEach(metodo->{
								if (Modifier.isPublic(metodo.unwrappedMethod.getModifiers()) && shouldBeOnServlet(metodo, multipart)) {//TODO: revisar si en necesario revisar que sea publico
									GeneradorMetodoServlet gen = new GeneradorMetodoServlet(context, metodo);
									gen.crearMetodoServlet(this);
									needsJpa |= gen.needsJpaEntityManager;
								}
							});
					}
				});
				context.output.exportFile(this, context.input.SERVER.WEB.getBasePackage().replace(".", "/") + "/servlets/" + nombre + ".java");
			}};
		});
	}
	private void putServletMethods(JavaCode code, List<JCrystalWebServiceManager> lista, boolean multipart) {
		final JavaCode GETS = new JavaCode();
		final JavaCode POSTS = new JavaCode();
		int[] cuentas = {0, 0};
		GETS.new B() {{
			$("@Override");
			$("public void doGet(String path, HttpServletRequest req, HttpServletResponse resp) throws Exception", () -> {
				$("switch(path)", () -> {
					POSTS.new B() {{
						$("@Override");
							$("public void doPost(String path, HttpServletRequest req, HttpServletResponse resp) throws Exception",() -> {
								$("switch(path)", () -> {
									for (JCrystalWebServiceManager clase : lista) {
										clase.generateClass();
										if(clase.isMultipart) {
											if(multipart) {
												cuentas[1]++;
												clase.crearSwitchServletManager(POSTS);
											}
										}else
											clase._metodos.stream().filter(metodo->shouldBeOnServlet(metodo, multipart)).sorted().forEach(metodo->{
												GeneradorMetodoServlet gen = new GeneradorMetodoServlet(context, metodo);
												if (metodo.tipoRuta.isGetLike()) {
													cuentas[0]++;
													gen.crearSwitchServlet(clase, GETS);
												}else{
													cuentas[1]++;
													gen.crearSwitchServlet(clase, POSTS);
												}
											});
									}
									$("default: send404(resp);break;");
								});
							});
					}};
					$("default: send404(resp);break;");
				});
			});
		}};
		code.new B() {{
			if(cuentas[0] > 0)
				$append(GETS);
			else {
				$("@Override");
				$("public void doGet(String path, HttpServletRequest req, HttpServletResponse resp) throws Exception", () -> {
					$("send404(resp);");
				});
			}
			if(cuentas[1] > 0)
				$append(POSTS);
			else {
				$("@Override");
				$("public void doPost(String path, HttpServletRequest req, HttpServletResponse resp) throws Exception", () -> {
					$("send404(resp);");
				});
			}
		}};
	}
}

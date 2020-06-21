
package jcrystal.clients.android;

import static jcrystal.utils.StringUtils.camelizar;
import static jcrystal.utils.StringUtils.capitalize;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import jcrystal.clients.AbsClientGenerator;
import jcrystal.clients.ClientGeneratorDescriptor;
import jcrystal.clients.ClientId;
import jcrystal.configs.clients.IInternalConfig;
import jcrystal.configs.clients.JClientAndroid;
import jcrystal.entity.types.LongText;
import jcrystal.entity.types.security.Password;
import jcrystal.json.JsonLevel;
import jcrystal.lang.Language;
import jcrystal.main.data.ClientContext;
import jcrystal.model.server.db.EntityField;
import jcrystal.model.server.db.EntityClass;
import jcrystal.model.web.HttpType;
import jcrystal.model.web.IWServiceEndpoint;
import jcrystal.model.web.JCrystalWebService;
import jcrystal.model.web.JCrystalWebServiceParam;
import jcrystal.reflection.annotations.CrystalDate;
import jcrystal.reflection.annotations.com.jSerializable;
import jcrystal.reflection.annotations.ws.ContentType;
import jcrystal.results.Tupla2;
import jcrystal.reflection.annotations.LoginResultClass;
import jcrystal.reflection.annotations.Post;
import jcrystal.reflection.annotations.jEntity;
import jcrystal.server.FileDownloadDescriptor;
import jcrystal.service.types.Authorization;
import jcrystal.types.IJType;
import jcrystal.types.JClass;
import jcrystal.types.JEnum.EnumValue;
import jcrystal.types.utils.GlobalTypes;
import jcrystal.types.vars.IAccessor;
import jcrystal.types.JVariable;
import jcrystal.utils.InternalException;
import jcrystal.utils.StringSeparator;
import jcrystal.utils.StringUtils;
import jcrystal.utils.context.CodeGeneratorContext;
import jcrystal.utils.context.CodeGeneratorContext.ContextType;
import jcrystal.utils.langAndPlats.AbsCodeBlock;
import jcrystal.utils.langAndPlats.JavaCode;

public class AndroidClient extends AbsClientGenerator<JClientAndroid>{

	public static final String paqueteEntidades = "jcrystal.mobile.entities";
	public static final String paqueteValidadores = "jcrystal.mobile.validators";
	public static final String paqueteResultados = "jcrystal.mobile.results";
	public static final String paquetePushs = "jcrystal.mobile.pushs";
	public static final String paqueteUtils = "jcrystal.mobile.net.utils";
	public static final String paquetePush = "jcrystal.mobile.push";
	public static final String netPackage = "jcrystal.mobile.net"; 
	public static final String paqueteControllers =netPackage + ".controllers";
	public static final String paqueteMobile = "jcrystal.mobile";
	public static final String paqueteDates = "jcrystal.datetime";
	public static final String paquetePadre = "jcrystal";
	
	public AndroidClient(ClientGeneratorDescriptor<JClientAndroid> descriptor){
		super(descriptor);
		List<String> imports = Arrays.asList(paqueteMobile+".*", paqueteEntidades+".enums.*");
		entityGenerator = new GeneradorEntidad(this, descriptor, imports);
	}
	@Override
	protected void setupEnviroment() throws Exception {
		ContextType.MOBILE.init();
		CodeGeneratorContext.set(Language.JAVA, new AndroidTypeConverter(context));
	}
	@Override
	public void generarCliente() throws Exception {
		createAbstractNetTasks();
		generarCodigoMobile();
		generarEntidades();
		generarResultados();
		generarEnums();
		GeneradorPushs.generarPushClasses(this);
	}

	private void addResource(Map<String, Object> enviroment, String paquete, String name) throws Exception {
		addResource(AndroidClient.class.getResourceAsStream("net/" + name), enviroment, paquete.replace(".", File.separator) + File.separator + name + ".java");
	}

	private void addResource(String paquete, String name) throws Exception {
		addResource(AndroidClient.class.getResourceAsStream("net/" + name), paquete, paquete.replace(".", File.separator) + File.separator + name + ".java");
	}

	private void generarEnums() throws Exception {
		final String paquete = paqueteEntidades + ".enums";
		requiredClasses.stream().filter(f->f.isEnum()).forEach(claseEnum->{
			JClass clase = (JClass)claseEnum;
			final JavaCode $ = new JavaCode() {{
				$("package " + paquete + ";");
				$("public enum " + claseEnum.getSimpleName(), () -> {
					final IJType idType = clase.enumData.propiedades.get("id");
					for (EnumValue o : clase.enumData.valores)
						if(idType.is(String.class))
							$(o.name + "(\"" + o.propiedades.get("id") + "\"),");
						else
							$(o.name + "(" + o.propiedades.get("id") + "),");
					
					$(";");
					$("public final "+idType.getSimpleName()+" id;");
					$(claseEnum.getSimpleName() + "("+idType.getSimpleName()+" id)", () -> {
						$("this.id = id;");
					});
					clase.enumData.propiedades.forEach((key, type)->{
						if(!key.equals("id"))
							$("public " + $($convert(type))+ " "+(key.equals("name")?"getName":key)+"()", ()->{
								$("switch(this)",()->{
									for (EnumValue o : clase.enumData.valores)
										if(type.is(String.class))
											$("case "+ o.name + " : return \"" + o.propiedades.get(key)+"\";");
										else
											$("case "+ o.name + " : return " + o.propiedades.get(key)+";");										
								});
								if(type.is(String.class))
									$("return null;");
								else
									$("return 0;");
							});
					});
					$("public static " + claseEnum.getSimpleName() + " fromId("+idType.getSimpleName()+" id)", () -> {
						if(idType.is(long.class))//TODO: REsolver bien (< O(n))
							for (EnumValue o : clase.enumData.valores)
								$if("id ==" + o.propiedades.get("id"), "return " + o.name + ";");
						else
							$("switch(id)", () -> {
								for (EnumValue o : clase.enumData.valores)
									if(idType.is(String.class))
										$("case \"" + o.propiedades.get("id") + "\": return " + o.name + ";");
									else
										$("case " + o.propiedades.get("id") + ": return " + o.name + ";");
							});
						$("return null;");
					});
					$("public static <T> T[] mapped(Class<T> clase, "+paqueteUtils+".Function<"+claseEnum.getSimpleName()+", T> mapper)", () -> {
						$("@SuppressWarnings(\"unchecked\")");
						$(claseEnum.getSimpleName()+"[] vals = values();");
						$("final T[] a = (T[]) java.lang.reflect.Array.newInstance(clase, vals.length);");
						$("for(int e = 0; e < a.length; e++)",()->{
							$("a[e] = mapper.eval(vals[e]);");
						});
						$("return a;");
					});
					$("public static <T> T[] mapped(Class<T> clase, "+paqueteUtils+".Predicate<"+claseEnum.getSimpleName()+"> p, "+paqueteUtils+".Function<"+claseEnum.getSimpleName()+", T> mapper)", () -> {
						$("@SuppressWarnings(\"unchecked\")");
						$(claseEnum.getSimpleName()+"[] vals = values();");
						$("int size = 0;");
						$("for(int e = 0; e < vals.length; e++)",()->{
							$("if(p.eval(vals[e]))size++;");
						});
						$("final T[] a = (T[]) java.lang.reflect.Array.newInstance(clase, size);");
						$("for(int e = 0, i = 0; e < vals.length; e++)",()->{
							$("if(p.eval(vals[e]))",()->{
								$("a[i++] = mapper.eval(vals[e]);");								
							});
						});
						$("return a;");
					});
				});
			}};
			exportFile($, paquete.replace(".", File.separator) + File.separator + claseEnum.getSimpleName() + ".java");
		});
		/*TODO: Move to library final JavaCode $ = new JavaCode() {{
			$("package " + paquete + ";");
			$("public enum JsonLevel", () -> {
				StringSeparator vals = new StringSeparator(", ");
				for (JsonLevel level : JsonLevel.managedValues)
					vals.add(level.name());
				$(vals + ";");
			});
		}};
		exportFile($, paquete.replace(".", File.separator) + File.separator + "JsonLevel.java");*/
	}
	private void generarClaseResultadoOPush(String paquete, final JClass clase){
		new JavaCode() {{
			$("package " + paquete + ";");
			$("import " + paqueteMobile + ".*;");
			$("import java.io.*;");
			$("import static " + paquetePadre + ".JSONUtils.*;");
			$("import " + paqueteEntidades + ".enums.*;");
			
			$("public class " + clase.simpleName + " implements Serializable, "+netPackage+".ISerializable", () -> {
				clase.attributes.forEach(f->{
					$("private " + $($convert(f.type())) + " " + f.name() + ";");
					$("public " + $($convert(f.type())) + " " + f.name() + "(){return this." + f.name() + ";}");
					$("public void " + f.name() + "(" + $($convert(f.type())) + " val){" + f.name() + " = val;}");
				});
				$("public " + clase.simpleName + "()", () -> {
				});
				$("protected " + clase.simpleName + "(org.json.JSONObject json)throws org.json.JSONException", () -> {
					clase.attributes.forEach(f->{
						if (f.type().isEnum())
							requiredClasses.add(f.type());
						context.utils.extrators.jsonObject.procesarCampo(this, clase, f.accessor().$this());
					});
				});
				generateFromSimpleJson(this, clase, null, clase.attributes);
				generateFromListJson(this, clase, null);
				
				$("@Override public void toJson(java.io.PrintStream _pw)",()->{
					$("Serializer"+clase.simpleName+".toJson(_pw, this);");
				});
				
				if (clase.isAnnotationPresent(LoginResultClass.class))
					crearcodigoTokenSeguridad(context, this, clase);

				$("//Serializable things");
				$("private void writeObject(ObjectOutputStream aOutputStream) throws IOException", () -> {
					for (final JVariable f : clase.attributes) {
						if (f.type().isPrimitive())
							$("aOutputStream.write" + capitalize(f.type().getSimpleName()) + "(" + f.name() + ");");
						else if (f.type().isEnum()) {
							IJType idType =  ((JClass)f.type()).enumData.propiedades.get("id");
							if(idType == null)
								$("aOutputStream.writeString(" + f.name() + "==null?null:" + f.name() + ".name());");
							else if(idType.is(String.class)) 
								$("aOutputStream.writeString(" + f.name() + "==null?null:" + f.name() + ".id);");
							else if(idType.is(int.class)) 
								$("aOutputStream.writeInt(" + f.name() + "==null?0:" + f.name() + ".id);");
							else if(idType.is(long.class)) 
								$("aOutputStream.writeLong(" + f.name() + "==null?0:" + f.name() + ".id);");
							else 
								throw new InternalException(500, "Unssuported id type for enum " + f.type());
						}else if (f.type().is(String.class))
							$("aOutputStream.writeUTF(" + f.name() + ");");
						else
							$("aOutputStream.writeObject(" + f.name() + ");");
					}
				});
				$("private void readObject(ObjectInputStream aInputStream) throws ClassNotFoundException, IOException", () -> {
					for (final JVariable f : clase.attributes) {
						if (f.type().isPrimitive())
							$("this." + f.name() + " = aInputStream.read" + capitalize(f.type().getSimpleName()) + "();");
						else if (f.type().isEnum()) {
							IJType idType =  ((JClass)f.type()).enumData.propiedades.get("id");
							if(idType == null)
								$("this." + f.name() + " = " + $($convert(f.type())) + ".fromId(aInputStream.readString());");
							else if(idType.is(String.class)) 
								$("this." + f.name() + " = " + $($convert(f.type())) + ".fromId(aInputStream.readString());");
							else if(idType.is(int.class)) 
								$("this." + f.name() + " = " + $($convert(f.type())) + ".fromId(aInputStream.readInt());");
							else if(idType.is(long.class)) 
								$("this." + f.name() + " = " + $($convert(f.type())) + ".fromId(aInputStream.readLong());");
							else 
								throw new InternalException(500, "Unssuported id type for enum " + f.type());
						}else if (f.type().is(String.class))
							$("this." + f.name() + " = aInputStream.readUTF();");
						else if (f.type().isAnnotationPresent(jSerializable.class))
							$("this." + f.name() + " = ("+$($convert(f.type()))+")aInputStream.readObject();");
						else
							$("this." + f.name() + " = ("+$($convert(f.type()))+")aInputStream.readObject();");
					}
				});
			});
			exportFile(this, paquete.replace(".", File.separator) + File.separator + clase.getSimpleName() + ".java");
		}};
		new JavaCode() {{
			$("package " + paquete + ";");
			$("import " + paqueteMobile + ".*;");
			$("import java.io.*;");
			$("import static " + paquetePadre + ".JSONUtils.*;");
			$("import " + paqueteEntidades + ".enums.*;");
			$("import jcrystal.PrintWriterUtils;");
			$("public class Serializer" + clase.simpleName + " ", () -> {
				context.utils.generadorToJson.generateJsonify(clase, this, null, clase.attributes.stream().map(f -> f.accessor().asProperty().prefix("objeto.")).collect(Collectors.toList()));
			});
			exportFile(this, paquete.replace(".", File.separator) + File.separator + "Serializer" + clase.getSimpleName() + ".java");
		}};
		// CREAR LA CLASE

		new GeneradorStorage().crearCodigoAlmacenamiento(this, paquete, clase, null);
	}
	public static void crearcodigoTokenSeguridad(ClientContext context, AbsCodeBlock code, JClass clase) {
		code.new B() {
			{
				$("private static " + clase.simpleName + " cachedToken = null;");
				$("public " + clase.simpleName + " storeToken()throws org.json.JSONException", () -> {
					$("if(DB" + clase.simpleName + ".store(\"Token\", this))cachedToken = this;");
					$("return this;");
				});
				$("public static String getTokenId()", () -> {
					$("if(cachedToken != null)return cachedToken.token;");
					$("else getToken();");
					$("if(cachedToken == null)return null;");
					$("return cachedToken.token;");
				});
				$("public static void deleteToken()", () -> {
					$("cachedToken = null;");
					$("DB" + clase.simpleName + ".delete(\"Token\");");
				});
				$("public static " + clase.simpleName + " getToken()", () -> {
					$("if(cachedToken == null)", () -> {
						$("cachedToken = DB" + clase.simpleName + ".retrieve(\"Token\");");
					});
					$("return cachedToken;");
				});
				$("public static boolean isAuthenticated()", () -> {
					$("return getToken() != null;");
				});
				if (context.data.rolClass != null) {
					for (final EnumValue value : context.data.rolClass.enumData.valores) {
						String name = "Rol" + StringUtils.camelizar(value.name);
						$("public static boolean is" + name + "()", () -> {
							if(!value.propiedades.containsKey("id"))
								throw new NullPointerException("No hay id para el enum de roles: " + context.data.rolClass.name);
							$("return getToken() != null && (cachedToken.rol & " + value.propiedades.get("id") + ")!=0;");
						});
					}
				}
			}
		};
	}
	private void generarResultados() throws Exception {
		requiredClasses.stream().filter(f->f.isAnnotationPresent(jSerializable.class)).forEach(clase->{
			generarClaseResultadoOPush(paqueteResultados, (JClass)clase);
		});
	}
	private void createAbstractNetTasks() throws Exception{
		final String netPackage = paqueteControllers.substring(0, paqueteControllers.lastIndexOf('.'));
		for(IInternalConfig config : descriptor.configs.values()) {
			final String id = config.id()==null?"Default":config.id();
			final JavaCode code = new JavaCode() {{
				$("package " + netPackage + ";");
				$("import android.app.Activity;");
				$("import androidx.fragment.app.Fragment;");
				$("import jcrystal.mobile.net.utils.*;");
				if(descriptor.client.getFirebaseCrashReportingEnabled())
					$("import com.google.firebase.crash.FirebaseCrash;");
				$("public abstract class Abs"+id+"Manager<T> extends NetTask<T>", () -> {
					$("public static final String BASE_URL = \"" + config.BASE_URL(null)+"\";");
					$("private boolean formData;");
					$("protected String boundary;");
					$("public Abs"+id+"Manager(Activity activity, Fragment fragment, OnErrorListener onError)", () -> {
						$("super(activity, fragment, onError);");
					});
					$("@Override protected final T doRequest()throws Exception", () -> {										
						$("java.net.HttpURLConnection connection = (java.net.HttpURLConnection) new java.net.URL(BASE_URL + getUrl()).openConnection();");
						$("if(" + paquetePadre + ".JCrystalApp.DEBUG)System.out.println($type + \" \" + BASE_URL + getUrl());");
						$("connection.setConnectTimeout(NetConfig.TIMEOUT);");
						$("connection.setRequestMethod($type.name());");
						$("connection.setRequestProperty(\"Accept\", \"application/json\");");
						$("if($authorization != null)",()->{
							$("connection.setRequestProperty(\"Authorization\", $authorization);");							
						});
						$("if($headers != null)",()->{
							$("for (java.util.Map.Entry<String, String> entry : $headers.entrySet())",()->{
								$("connection.setRequestProperty(entry.getKey(), entry.getValue());");							
							});
						});
						
						$if("$type.isPost",()->{
							$if("formData",()->{
								$("connection.setRequestProperty(\"Content-Type\", \"multipart/form-data; boundary=---------------------------\"+boundary);");
							});
							$else(()->{
								$("connection.setRequestProperty(\"Content-Type\", \"application/json\");");
							});
						});
						$("connection.connect();");
						$if("$type.isPost",()->{
							$if(paquetePadre + ".JCrystalApp.DEBUG",()->{
								$("java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();"); 
								$("java.io.PrintStream _pw = new java.io.PrintStream(baos, false, \"UTF-8\");");
								$("makeBody(_pw);");
								$("_pw.flush();");
								$("_pw.close();");
								$("System.out.println(new String(baos.toByteArray()));");
								$("connection.getOutputStream().write(baos.toByteArray());");
								$("connection.getOutputStream().close();");
							});
							$else(()->{
								$("java.io.PrintStream _pw = new java.io.PrintStream(connection.getOutputStream(), false, \"UTF-8\");");
								$("makeBody(_pw);");
								$("_pw.flush();");
								$("_pw.close();");
							});
						});

						$("final int responseCode = connection.getResponseCode();");
						$if("responseCode >= 200 && responseCode <= 299", () -> {
							$("final StringBuilder resp = HTTPUtils.readResponse(connection.getInputStream());");
							$("connection.disconnect();");
							$("if(" + paquetePadre + ".JCrystalApp.DEBUG)System.out.println(resp);");
							$("return getResponse(resp);");
						});
						$else(() -> {
							$if("onError != null", () -> {
								
								$("final java.io.InputStream errorStream = connection.getErrorStream();");
								$("final StringBuilder resp;");
								$if("errorStream != null",()->{
									$("resp = HTTPUtils.readResponse(errorStream);");
									$("if(" + paquetePadre + ".JCrystalApp.DEBUG)System.out.println(resp);");
								}).$else(()->{
									$("resp = new StringBuilder(\"\");");
									
								});
								$("connection.disconnect();");
								$("$error = new RequestError(responseCode, resp.toString());");
							});
							$else(() -> {
								$("connection.disconnect();");
							});
							$("return null;");
						});
					});
					$("public Abs"+id+"Manager doFormData()",()->{
						$("formData = true;");
						$("boundary = Long.toString(System.currentTimeMillis());");
						$("return this;");
					});
					$("@Override");
					$("protected void onPostExecute(T result)", () -> {
						$if("isContextActive()", () -> {
							$if("result != null", () -> {
								$("try", () -> {
									$("onResponse(result);");
								});
								$("catch(Exception ex)", () -> {
									if(descriptor.client.getFirebaseCrashReportingEnabled())
										$("FirebaseCrash.report(ex);");
									$("if(" + paquetePadre + ".JCrystalApp.DEBUG)ex.printStackTrace();");
									$("if(onError!=null)onError.onError(new RequestError(TipoError.SERVER_ERROR, \"Ocurrió un error con el servidor\"));");
								});
							});
							$else_if("onError != null", () -> {
								$("onError.onError($error);");
							});
							$("if($chain != null)$chain.endTask();");
						});
					});
					$("protected void makeBody(java.io.PrintStream _pw) throws java.io.UnsupportedEncodingException, java.io.IOException", () -> {});
					$("protected abstract void onResponse(T result) throws Exception;");
					$("abstract T getResponse(StringBuilder resp) throws Exception;");
					for(Class<?> tipo : new Class<?>[] {String.class, JSONObject.class, JSONArray.class}) {
						$("public abstract static class "+tipo.getSimpleName()+"Resp extends Abs"+id+"Manager<"+tipo.getName()+">",()->{
							$("public "+tipo.getSimpleName()+"Resp(Activity activity, Fragment fragment, OnErrorListener onError)",()->{
								$("super(activity, fragment, onError);");
							});
							$("@Override protected "+tipo.getName()+" getResponse(StringBuilder resp)throws Exception",()->{
								if (tipo == String.class) {
									$("return resp.toString();");
									return;
								} else if (tipo == JSONArray.class)
									$("org.json.JSONArray json = new org.json.JSONArray(resp.toString());");
								else
									$("org.json.JSONObject json = new org.json.JSONObject(resp.toString());");
	
								if (config.SUCCESS_TYPE() != null) {
									IJType successType = getSuccessType(config.SUCCESS_TYPE());
									if (tipo == JSONArray.class)
										$("final " + $($convert(successType)) + " success = 1;");
									else
										$("final " + $($convert(successType)) + " success = json.opt" + capitalize($convert(successType).getSimpleName()) + "(\"success\", " + config.SUCCESS_DAFAULT_VALUE() + ");");
								}
								$if(config.SUCCESS_CONDITION(), "return json;");
								if (config.ERROR_CONDITION() != null)
									$if(config.ERROR_CONDITION(), () -> {
										if (tipo == JSONArray.class)
											$("$error = new RequestError(0, \"SERVER ERROR\");");
										else
											$("$error = new RequestError(json.optInt(\"code\",0), json.getString(\"" + config.ERROR_MESSAGE_NAME() + "\"));");
										$("return null;");
									});
								if (config.UNATHORIZED_CONDITION() != null)
									$if(config.UNATHORIZED_CONDITION(), () -> {
										if (tipo == JSONArray.class)
											$("$error = new RequestError(0, \"SERVER ERROR\");");
										else
											$("$error = new RequestError(TipoError.UNAUTHORIZED, json.getString(\"" + config.ERROR_MESSAGE_NAME() + "\"));");
										$("return null;");
									});
								if (config.SUCCESS_TYPE() != null) {
									if (tipo == JSONArray.class)
										$("$error = new RequestError(0, \"SERVER ERROR\");");
									else
										$("$error = new RequestError(TipoError.SERVER_ERROR, json.getString(\"" + config.ERROR_MESSAGE_NAME() + "\"));");
									$("return null;");
								}
							});
						});
					}
				});
			}};
			exportFile(code, netPackage.replace(".", File.separator) + File.separator + "Abs"+id+"Manager.java");
		}
	}

	private void generarCodigoMobile() throws Exception {
		boolean hasEntities = requiredClasses.stream().anyMatch(f->f.isAnnotationPresent(jEntity.class));
		boolean hasResults = requiredClasses.stream().anyMatch(f->f.isAnnotationPresent(jSerializable.class));
		boolean hasEnums = requiredClasses.stream().anyMatch(f->f.isEnum());
		for (final Map.Entry<String, Set<IWServiceEndpoint>> entry : endpoints.entrySet()) {
			String name = "Manager" + StringUtils.capitalize(StringUtils.camelizarSoft(entry.getKey().contains("/") ? entry.getKey().substring(entry.getKey().lastIndexOf('/') + 1) : entry.getKey()));
			final String paquete;
			if (entry.getKey().contains("/"))
				paquete = paqueteControllers + "." + entry.getKey().substring(0, entry.getKey().lastIndexOf('/')).replace("/", ".");
			else
				paquete = paqueteControllers;
			new JavaCode() {
				{
					$("package " + paquete + ";");
					$("import " + paqueteUtils + ".*;");
					if(hasResults)
						$("import " + paqueteResultados + ".*;");
					if(hasEntities)
						$("import " + paqueteEntidades + ".*;");
					if(hasEnums) {
						$("import " + paqueteEntidades + ".enums.*;");
					}
					$("import jcrystal.PrintWriterUtils;");
					$("import jcrystal.mobile.entities.enums.JsonLevel;");
					$("import " + paqueteMobile + ".*;");
					$("import " + AndroidClient.paqueteDates + ".*;");
					$("import " + paqueteControllers.substring(0, paqueteControllers.lastIndexOf('.')) + ".*;");
					if(descriptor.client.getFirebaseCrashReportingEnabled())
						$("import com.google.firebase.crash.FirebaseCrash;");
					$("import android.os.AsyncTask;");
					$("import static jcrystal.JSONUtils.*;");
					$("public class " + name, () -> {
						for (final IWServiceEndpoint endpoint : entry.getValue()) {
							JCrystalWebService m = (JCrystalWebService)endpoint;
							$("/**");
							$("* " + m.getPath(descriptor));
							$("**/");
							final Tupla2<List<JVariable>, Map<HttpType, List<JVariable>>> clientParams = getParamsWebService(m);
							final List<JVariable> parametros = clientParams.v0;
							if (m.isClientQueueable()) {
								/*$("public static void " + m.m.name() + "(" + parametros.stream().map(f -> "final " + f.tipo + " " + f.nombre).collect(Collectors.joining(", ")) + ")", () -> {
									$("try", () -> {
										String url = m.internalConfig.BASE_URL("android");
										if(url==null)url=descriptor.client.serverUrl;
										$("final String $ruta = \"" + url + m.rutaMetodo + "\";");
										if (m.m.getAnnotation(FormData.class) == null) {
											$("String params = null;");
											for (String h : interno)
												$(h);
										}
										$("final String $method = \"" + m.tipoRuta.name() + "\";");
										if (m.tokenParam != null) {
											$("new jcrystal.mobile.net.AsyncNetTask($method, $ruta, params, null, " + m.tokenParam.type().getSimpleName() + ".getTokenId()).save();");
										} else
											$("new jcrystal.mobile.net.AsyncNetTask($method, $ruta, params, null).save();");
									});
									$("catch(Exception ex)", () -> {

									});
								});*/
								//throw new NullPointerException();
							} else {
								boolean addOnError = true;
								if (m.unwrappedMethod.isVoid)
									parametros.add(P(GlobalTypes.jCrystal.VoidSuccessListener, "onSuccess"));
								else if (m.getReturnType().is(String.class))
									parametros.add(P(GlobalTypes.jCrystal.OnSuccessListener(GlobalTypes.STRING), "onSuccess"));
								else if (SUPPORTED_NATIVE_TYPES.contains(m.getReturnType()))
									parametros.add(P(GlobalTypes.jCrystal.NativeSuccessListener(m.getReturnType()), "onSuccess"));
								else if (m.getReturnType().isAnyAnnotationPresent(jEntity.class, jSerializable.class) 
										|| m.getReturnType().isIterable() || m.getReturnType().isArray())
									parametros.add(P(GlobalTypes.jCrystal.OnSuccessListener($convert(m.getReturnType())), "onSuccess"));
								else if (m.getReturnType().isTupla()) {
									final List<IJType> tipos = new ArrayList<>(m.getReturnType().getInnerTypes().size());
									for (IJType p : m.getReturnType().getInnerTypes()) {
										if (p.getInnerTypes().isEmpty()) {
											tipos.add($convert(p));
										} else{
											if (p.isSubclassOf(List.class)) {
												IJType subClaseTipo = p.getInnerTypes().get(0);
												tipos.add($convert(subClaseTipo).createListType());
											} else
												throw new NullPointerException("Error, tipos incompatibles. No se puede tener un metodo con retorno " + p);
										}
									}
									parametros.add(P(GlobalTypes.jCrystal.OnSuccessListener(tipos), "onSuccess"));
								}else if(m.getReturnType().is(FileDownloadDescriptor.class)) {
									addOnError = false;
								}else
									throw new NullPointerException(m.getReturnType().toString());
								if(addOnError)
									parametros.add(P(GlobalTypes.jCrystal.ErrorListener, "onError"));

								IInternalConfig methodConfig = m.exportClientConfig(descriptor);
								
								String methodName = StringUtils.lowercalize(StringUtils.camelizarSoft(m.name()));
								final String visibility = m.isClientQueueable() ? "private" : "public";
								final boolean nativeEmbeddedResponse = methodConfig.embeddedResponse() && SUPPORTED_NATIVE_TYPES.contains(m.getReturnType());
								if(m.getReturnType().is(jcrystal.server.FileDownloadDescriptor.class)) {
									$(visibility + " static String " + methodName + "(" + parametros.stream().map(this::$V).collect(Collectors.joining(", ")) + ")", () -> {
										String ruta = m.getPath(descriptor);
										for (final JCrystalWebServiceParam param : m.parametros)
											if (param.tipoRuta == HttpType.PATH)
												ruta = ruta.replace("$" + param.nombre, "\"+" + param.nombre + "+\"");
										$("String ruta = \"" + ruta + "\";");
										processGetParams(this, clientParams.v1.get(HttpType.GET));
										$("return Abs"+methodConfig.getDefId()+"Manager.BASE_URL + ruta;");
									});
								}else {
									$(visibility + " static AsyncTask " + methodName + "(android.app.Activity $activity, " + parametros.stream().map(this::$V).collect(Collectors.joining(", ")) + ")", () -> {
										String pars = parametros.stream().map(f -> f.name).collect(Collectors.joining(", "));
										$("return " + methodName + "($activity, (androidx.fragment.app.Fragment)null, " + pars + ").exec();");
									});
									$(visibility + " static AsyncTask " + methodName + "(androidx.fragment.app.Fragment $fragment, " + parametros.stream().map(this::$V).collect(Collectors.joining(", ")) + ")", () -> {
										String pars = parametros.stream().map(f -> f.name).collect(Collectors.joining(", "));
										$("return " + methodName + "(null, $fragment, " + pars + ").exec();");
									});
									final List<JVariable> parametrosSinError = new ArrayList<>(parametros);
									parametrosSinError.remove(parametrosSinError.size() - 1);
									$(visibility + " static <T extends androidx.fragment.app.Fragment & OnErrorListener> AsyncTask " + methodName + "(T $fragment, " + parametrosSinError.stream().map(this::$V).collect(Collectors.joining(", ")) + ")", () -> {
										String pars = parametrosSinError.stream().map(f -> f.name).collect(Collectors.joining(", "));
										$("return " + methodName + "(null, $fragment, " + pars + ", $fragment).exec();");
									});
									$(visibility + " static <T extends android.app.Activity & OnErrorListener> AsyncTask " + methodName + "(T $activity, " + parametrosSinError.stream().map(this::$V).collect(Collectors.joining(", ")) + ")", () -> {
										String pars = parametrosSinError.stream().map(f -> f.name).collect(Collectors.joining(", "));
										$("return " + methodName + "($activity, (androidx.fragment.app.Fragment)null, " + pars + ", $activity).exec();");
									});
									$(visibility + " static void " + methodName + "(NetChain $chain, " + parametrosSinError.stream().map(this::$V).collect(Collectors.joining(", ")) + ")", () -> {
										String pars = parametrosSinError.stream().map(f -> f.name).collect(Collectors.joining(", "));
										$if("$chain.fragment != null", () -> {
											$("$chain.add(" + methodName + "(null, $chain.fragment, " + pars + ", $chain));");
										}).$else(() -> {
											$("$chain.add(" + methodName + "($chain.activity, (androidx.fragment.app.Fragment)null, " + pars + ", $chain));");
										});
									});
									String helperResponseType = (nativeEmbeddedResponse ? "String" : m.isJsonArrayResponse(methodConfig) ? "org.json.JSONArray" : "org.json.JSONObject");
									String helperResponseSimpleType = (nativeEmbeddedResponse ? "String" : m.isJsonArrayResponse(methodConfig) ? "JSONArray" : "JSONObject");
									$("private static NetTask<"+helperResponseType+"> " + methodName + "(android.app.Activity $activity, androidx.fragment.app.Fragment $fragment, " + parametros.stream().map(this::$V).collect(Collectors.joining(", ")) + ")", () -> {
										parametros.remove(parametros.size()-1);
										String token = m.parametros.stream().filter(f->f.tipoRuta == HttpType.HEADER).map(f->{
											if(f.type().is(Authorization.class))
												return ".authorization("+f.nombre+")";
											else if(f.type().is(ClientId.class))
												return ".header(\""+f.nombre+"\", \""+descriptor.client.id+"\")";
											else
												throw new NullPointerException("Unssuported type  for header : " + f.type());
										}).collect(Collectors.joining());

										if (m.isMultipart())
											token += ".doFormData()";
										if (m.tokenParam != null)
											token += ".authorization("+m.tokenParam.type().getSimpleName() + ".getTokenId())";
										token += ".do"+StringUtils.capitalize(m.tipoRuta.name().toLowerCase())+"()";
										
										$("return new Abs"+methodConfig.getDefId()+"Manager."+helperResponseSimpleType+"Resp($activity, $fragment, onError)", () -> {
											$("@Override protected String getUrl()throws java.io.UnsupportedEncodingException",()->{
												String ruta = m.getPath(descriptor);
												for (final JCrystalWebServiceParam param : m.parametros)
													if (param.tipoRuta == HttpType.PATH)
														ruta = ruta.replace("$" + param.nombre, "\"+" + param.nombre + "+\"");
												$("String ruta = \"" + ruta + "\";");
												processGetParams(this, clientParams.v1.get(HttpType.GET));
												$("return ruta;");
											});
											$("@Override");
											$("public void onResponse("+helperResponseType+" result)throws Exception",()->{
													if (m.unwrappedMethod.isVoid)
														$("onSuccess.onSuccess();");
													else if (m.getReturnType().is(String.class))
														$("onSuccess.onSuccess(result.getString(\"r\"));");
													else if (SUPPORTED_NATIVE_TYPES.contains(m.getReturnType())) {
														if (nativeEmbeddedResponse)
															$("onSuccess.onSuccess(" + m.getReturnType().getObjectType().getSimpleName() + ".parse" + StringUtils.capitalize(m.getReturnType().getSimpleName()) + "(result));");
														else
															$("onSuccess.onSuccess(result.get" + StringUtils.capitalize(m.getReturnType().getSimpleName()) + "(\"r\"));");
													} else if (m.getReturnType().isAnnotationPresent(jSerializable.class)) {
														requiredClasses.add(m.getReturnType());
														if (methodConfig.embeddedResponse())
															$("onSuccess.onSuccess(" + $($convert(m.getReturnType())) + ".fromJson(result));");
														else if (m.getReturnType().isAnnotationPresent(LoginResultClass.class))
															$("onSuccess.onSuccess(result.isNull(\"r\")?null:" + $($convert(m.getReturnType())) + ".fromJson(result.getJSONObject(\"r\")).storeToken());");
														else
															$("onSuccess.onSuccess(result.isNull(\"r\")?null:" + $($convert(m.getReturnType())) + ".fromJson(result.getJSONObject(\"r\")));");
													} else if (m.getReturnType().isIterable()) {
														final IJType tipoParamero = m.getReturnType().getInnerTypes().get(0);
														if (tipoParamero.isAnnotationPresent(jSerializable.class)) {
															requiredClasses.add(tipoParamero);
															if (methodConfig.embeddedResponse()) {
																$("org.json.JSONArray $array = result;");
															} else {
																$("org.json.JSONArray $array = result.getJSONArray(\"r\");");
															}
															$("java.util.List<" + $($convert(tipoParamero)) + "> $lista = new java.util.ArrayList<" + paqueteResultados + "." + tipoParamero.getSimpleName() + ">($array.length());");
															$("for(int pos = 0, l = $array.length(); pos < l; pos++)", () -> {
																$("$lista.add(" + $($convert(tipoParamero)) + ".fromJson($array.getJSONObject(pos)));");
															});
															$("onSuccess.onSuccess($lista);");
														}else if(tipoParamero.isAnnotationPresent(jEntity.class)) {
															$("onSuccess.onSuccess("+$($convert(context.data.entidades.targetEntity(tipoParamero))) + ".ListUtils.listFromJson" + m.getJsonLevel(tipoParamero).baseName() + "(result.getJSONArray(\"r\")));");
														} else if (tipoParamero.is(String.class)) {
															if (methodConfig.embeddedResponse()) {
																$("org.json.JSONArray $array = result;");
															} else {
																$("org.json.JSONArray $array = result.getJSONArray(\"r\");");
															}
															$("java.util.List<String> $lista = new java.util.ArrayList<String>($array.length());");
															$("for(int pos = 0, l = $array.length(); pos < l; pos++)", () -> {
																$("$lista.add($array.optString(pos));");
															});
															$("onSuccess.onSuccess($lista);");
														} else
															throw new NullPointerException("Unsupported type " + m.getReturnType());
													} else if (m.getReturnType().isAnnotationPresent(jEntity.class)) {
														if (methodConfig.embeddedResponse()) {
															if (m.getReturnType().isAnnotationPresent(LoginResultClass.class))
																$("onSuccess.onSuccess(new " + $($convert(context.data.entidades.targetEntity(m.getReturnType()))) + "(result, JsonLevel." + m.getJsonLevel(m.getReturnType()).name() + ").storeToken());");
															else
																$("onSuccess.onSuccess(new " +  $($convert(context.data.entidades.targetEntity(m.getReturnType()))) + "(result, JsonLevel." + m.getJsonLevel(m.getReturnType()).name() + "));");
														}else if (m.getReturnType().isAnnotationPresent(LoginResultClass.class))
															$("onSuccess.onSuccess(result.isNull(\"r\")?null:new " + $($convert(m.unwrappedMethod.getReturnType())) + "(result.getJSONObject(\"r\"), JsonLevel." + m.getJsonLevel(m.getReturnType()).name() + ").storeToken());");
														else
															$("onSuccess.onSuccess(result.isNull(\"r\")?null:new " + $($convert(m.unwrappedMethod.getReturnType())) + "(result.getJSONObject(\"r\"), JsonLevel." + m.getJsonLevel(m.getReturnType()).name() + "));");
													} else if (m.getReturnType().isTupla()) {
														final List<IJType> tipos = m.getReturnType().getInnerTypes();
														if (methodConfig.embeddedResponse())
															$("org.json.JSONArray $array = result;");
														else
															$("org.json.JSONArray $array = result.getJSONArray(\"r\");");
														StringSeparator sp = new StringSeparator(',');
														int e = 0;
														for (IJType p : tipos) {
															if (p.getInnerTypes().isEmpty()) {
																if (p.isAnnotationPresent(jEntity.class)) {
																	if(p.isAnnotationPresent(LoginResultClass.class)){
																		sp.add("$array.isNull("+e+")?null:(new " + $($convert(context.data.entidades.targetEntity(p))) + "($array.getJSONObject(" + e + "), JsonLevel." + m.getJsonLevel(p).name() + ").storeToken())");
																	}else 
																		sp.add("$array.isNull("+e+")?null:new " + $($convert(context.data.entidades.targetEntity(p))) + "($array.getJSONObject(" + e + "), JsonLevel." + m.getJsonLevel(p).name() + ")");
																} else if (p.isAnnotationPresent(jSerializable.class)) {
																	if(p.isAnnotationPresent(LoginResultClass.class)){
																		sp.add("$array.isNull("+e+")?null:(" + $($convert(p)) + ".fromJson($array.getJSONObject(" + e + ")).storeToken())");
																	}else 
																		sp.add("$array.isNull("+e+")?null:" + $($convert(p)) + ".fromJson($array.getJSONObject(" + e + "))");
																} else if (p.is(Integer.class))
																	sp.add("$array.isNull("+e+")?null:$array.getInt(" + e + ")");
																else if (p.isPrimitiveObjectType())
																	sp.add("$array.isNull("+e+")?null:$array.get"+p.getSimpleName()+"(" + e + ")");
																else if (p.is(String.class))
																	sp.add("$array.isNull("+e+")?null:$array.getString(" + e + ")");
																else
																	throw new NullPointerException("Unssuported type " + p);
															} else{
																if (p.isIterable()) {
																	final IJType pClass = p.getInnerTypes().get(0);
																	if (pClass.isAnnotationPresent(jEntity.class)) {
																		sp.add($($convert(context.data.entidades.targetEntity(pClass))) + ".ListUtils.listFromJson" + m.getJsonLevel(pClass).baseName() + "($array.getJSONArray(" + e + "))");
																	} else if (pClass.isAnnotationPresent(jSerializable.class)) {
																		sp.add($convert(pClass) + ".listFromJson($array.getJSONArray(" + e + "))");
																	} else
																		throw new NullPointerException("Unssuported type " + p);
																} else
																	throw new NullPointerException("Error, tipos incompatibles. No se puede tener un metodo con retorno " + p);
															}
															e++;
														}
														$("onSuccess.onSuccess(" + sp + ");");
													} else
														$("onSuccess.onSuccess(result);");
											});
											if (m.tipoRuta.isPostLike()) {
												if (!m.isMultipart()) {
													String name = m.name();
													List<JCrystalWebServiceParam> bodyParams = m.parametros.stream().filter(param -> param.tipoRuta.isPostLike() && param.nombre.equals("body")).collect(Collectors.toList());
													if (bodyParams.size() > 1)
														throw new NullPointerException("Too many bodys");
													else if (bodyParams.isEmpty())
														context.utils.generadorToJson.generateSimplePlainToJson(Modifier.PROTECTED, "makeBody", $(), this, clientParams.v1.get(HttpType.POST).stream().map(f->f.accessor()).collect(Collectors.toList()));
													else {
														$("@Override protected void makeBody(java.io.PrintStream _pw)",()->{
															IAccessor param = entityUtils.changeToKeys(bodyParams.get(0));
															
															if (param.type().is(String.class, org.json.JSONObject.class)) {
																$("_pw.print(" + param.name() + ".toString());");
															} else if (param.type().isPrimitive()) {
																throw new NullPointerException("A " + param.type() + " cant be on body");
															} else if (param.type().isAnnotationPresent(Post.class)) {
																JsonLevel level = param.type().getAnnotation(Post.class).level();
																$(param.name() + ".toJson" + camelizar(level.name()) + "(_pw);");
															} else if (param.type().isAnnotationPresent(jSerializable.class)) {
																$("Serializer"+param.type().getSimpleName() + ".toJson(_pw, "+param.name()+");");
															} else if (param.type().isSubclassOf(List.class)) {
																final IJType tipo = param.type().getInnerTypes().get(0);
																if (tipo.isAnnotationPresent(jSerializable.class)) {
																	$("Serializer"+tipo.getSimpleName() + ".toJson" + tipo.getSimpleName() + "(_pw, " + param.name() + ");");
																} else if (tipo.isAnnotationPresent(Post.class)) {
																	JClass superClase = context.data.entidades.get(tipo).clase;
																	JsonLevel level = tipo.getAnnotation(Post.class).level();
																	if (param.name().equals("body"))
																		$if(param.name() + " != null", () -> {
																			$(paqueteEntidades + "." + superClase.simpleName + ".toJson" + superClase.simpleName + "(_pw, " + param.name() + ");");
																		});
																} else
																	throw new NullPointerException("Unssuported post type " + param.type().name()); // TODO: Si se postea una lista de Posts o Jsonifies
															} else
																throw new NullPointerException("Unssuported post type " + param.type().name());
														});
													}
												}else {
													$("@Override protected void makeBody(java.io.PrintStream writer) throws java.io.IOException",()->{
														if(m.parametros.stream().anyMatch(f->f.type().is("jcrystal.server.FileUploadDescriptor")))
															$("byte[] buffer = new byte[14*1024];");
														for (JCrystalWebServiceParam param : m.parametros) 
															if(param.p.type().is("jcrystal.server.FileUploadDescriptor")) {
																$("writer.println(\"-----------------------------\" + boundary);");
																$("writer.append(\"Content-Disposition: form-data; name="+param.name()+"\\r\\n\");");
																$if(param.name()+".mimeType != null",()->{
																	$("writer.append(\"Content-Type: \"+"+param.name()+".mimeType+\"\\r\\n\");");
																});
																$("writer.append(\"\\r\\n\");");
																$("try",()->{
																	$("java.io.InputStream fis = "+param.name()+".stream != null ? "+param.name()+".stream : new java.io.FileInputStream("+param.name()+".file);");
																	$("for(int n; (n = fis.read(buffer)) != -1; )",()->{
																		$("writer.write(buffer, 0, n);");
																	});
																	$("fis.close();");
																});
																$("catch(java.io.IOException ex)",()->{
																	$("throw ex;");
																});
																$("writer.append(\"\\r\\n\");");
															}else if(param.tipoRuta.isPostLike()){
																$if(param.p.type().isPrimitive() ? null : (param.nombre + "!= null"), () -> {
																	onMultipartBoundary(this, param.nombre, ()->{
																		if (param.p.type().isPrimitive()) {
																			$("writer.print(" + param.p.type().getObjectType().name() + ".toString(" + param.nombre + "));");
																		} else if (param.p.type().is(String.class)) {
																			$("writer.print(java.net.URLEncoder.encode(" + param.nombre + ", \"UTF-8\"));");
																		} else if (param.p.type().isPrimitiveObjectType()) {
																			$("writer.print(" + param.p.type().getObjectType().name() + ".toString(" + param.nombre + "));");
																		} else if (param.p.type().isJAnnotationPresent(CrystalDate.class)) {
																			$("writer.print(" + param.nombre + ".format());");
																		} else
																			throw new NullPointerException("Unssuported form data post type " + param.p.type().name());
																	});
																});
															}
															
														$("writer.println(\"-----------------------------\" + boundary + \"--\");");
													});
												}
											}
										},token+";");
									});
								}
							}
						}
					});
					exportFile(this, paquete.replace(".", File.separator) + File.separator + name + ".java");					
				}
			};
		}
	}

	private void createUtilFuntions() throws Exception {
		new JavaCode() {{
			$("package " + paqueteUtils + ";");
			$("public interface Predicate<T>", () -> {
				$("public boolean eval(T t);");
			});
			exportFile(this, paqueteUtils.replace(".", File.separator) + File.separator + "Predicate.java");
		}};
		new JavaCode() {{
			$("package " + paqueteUtils + ";");
			$("public interface Function<In, Out>", () -> {
				$("public Out eval(In t);");
			});
			exportFile(this, paqueteUtils.replace(".", File.separator) + File.separator + "Function.java");
		}};
	}
	
	private void createListener(int cantidad) throws Exception {
		final String name = "On" + cantidad + "SuccessListener";

		StringSeparator classParams = new StringSeparator(',');
		final StringSeparator methodParams = new StringSeparator(',');
		for (int e = 0; e < cantidad; e++) {
			String letra = Character.toString((char) ('K' + e));
			classParams.add(letra);
			methodParams.add(letra + " " + letra.toLowerCase());
		}
		final JavaCode cliente = new JavaCode() {
			{
				$("package " + paqueteUtils + ";");
				$("public interface " + name + "<" + classParams + ">", () -> {
					$("public void onSuccess(" + methodParams + ");");
				});
			}
		};
		exportFile(cliente, paqueteUtils.replace(".", File.separator) + File.separator + name + ".java");
	}

	private void createListener(Class<?> nativetype) throws Exception {
		final String name = "On" + StringUtils.capitalize(nativetype.getSimpleName()) + "SuccessListener";
		final JavaCode cliente = new JavaCode() {
			{
				$("package " + paqueteUtils + ";");
				$("public interface " + name, () -> {
					$("public void onSuccess(" + nativetype.getSimpleName() + " result);");
				});
			}
		};
		exportFile(cliente, paqueteUtils.replace(".", File.separator) + File.separator + name + ".java");
	}
	private void onMultipartBoundary(AbsCodeBlock code, String name, Runnable r) {
		code.$("writer.println(\"-----------------------------\" + boundary);");
		code.$("writer.append(\"Content-Disposition: form-data; name=" + name + "\\r\\n\");");
		code.$("writer.append(\"\\r\\n\");");
		r.run();
		code.$("writer.append(\"\\r\\n\");");
	}
	@Override
	protected <X extends AbsCodeBlock> void processPostParams(X METODO, ContentType contentType, List<JVariable> params) {
		throw new NullPointerException("processPostParams");
	}
	@Override
	protected <T extends AbsCodeBlock> void processGetParams(T METODO, List<JVariable> params) {
		if(params.isEmpty())
			return;
		METODO.new B() {{
			$("String params = null;");
			for (JVariable param : params) {
				if(param.type().nullable()) {
					$("if(" + param.name() + " != null){");
					METODO.incLevel();
				}

				if (param.type().isPrimitive()) {
					$("params = (params==null?\"?\":(params + \"&\")) + \"" + param.name() + "=\" + " + param.type().getObjectType().getSimpleName() + ".toString(" + param.name() + ");");
				} else if (param.type().is(String.class)) {
					$("params = (params==null?\"?\":(params + \"&\")) + \"" + param.name() + "=\" + java.net.URLEncoder.encode(" + param.name() + ", \"UTF-8\");");
				} else if (param.type().isPrimitiveObjectType()) {
					$("params = (params==null?\"?\":(params + \"&\")) + \"" + param.name() + "=\" + " + param.type().getObjectType().getSimpleName() + ".toString(" + param.name() + ");");
				}
				else if (param.type().isEnum()) {
					$("params = (params==null?\"?\":(params + \"&\")) + \"" + param.name() + "=\" + " + param.name() + ".id;");
				} else if (param.type().getSimpleName().equals("JSONObject")) {
					// TODO.
					/*
					 * if(param.nombre.equals("body")) retorno.add("body"); else
					 * retorno.add("body.getJSONObject(\"" + param.nombre + "\")");
					 */
					throw new NullPointerException();
				} else if (param.type().getSimpleName().equals("JSONArray")) {
					// TODO: retorno.add("body.getJSONArray(\"" + param.nombre + "\")");
					throw new NullPointerException();
				}
				else if (param.type().isJAnnotationPresent(CrystalDate.class))
					$("params = (params==null?\"?\":(params + \"&\")) + \"" + param.name() + "=\" + " + param.name() + ".format();");
				else
					throw new NullPointerException("Parametro no reconocido " + param.type().getSimpleName());
				if(param.type().nullable()) {
					METODO.decLevel();
					$("}");
				}
			}
			$("if(params != null)ruta+=params;");
		}};
	}

	public static void generateFromListJson(AbsCodeBlock bloque, final JClass clase, final JsonLevel level) {
		bloque.new B() {{
			$("public static java.util.ArrayList<" + clase.getSimpleName() + (level == null ? "" : level.baseName()) + "> listFromJson" + (level == null ? "" : level.baseName()) + "(org.json.JSONArray json)throws org.json.JSONException", () -> {
				$("java.util.ArrayList<" + clase.getSimpleName() + (level == null ? "" : level.baseName()) + "> ret = new java.util.ArrayList<" + clase.getSimpleName() + (level == null ? "" : level.baseName()) + ">(json.length());");
				$("for(int e = 0, i = json.length(); e < i; e++)", () -> {
					$("ret.add(new " + clase.getSimpleName() + "(json.getJSONObject(e)" + (level != null ? ", JsonLevel." + level.name() : "") + "));");
				});
				$("return ret;");
			});
		}};
	}

	public void generateFromSimpleJson(AbsCodeBlock bloque, final JClass clase, final JsonLevel level, final List<JVariable> campos) {
		bloque.new B() {{
			$("public static " + clase.getSimpleName() + " fromJson" + (level == null ? "" : level.baseName()) + "(org.json.JSONObject json)throws org.json.JSONException", () -> {
				if (level == null)
					$("return new " + clase.getSimpleName() + "(json);");
				else {
					$(clase.getSimpleName() + " ret = new " + clase.getSimpleName() + "();");
					for (final JVariable f : campos)
						context.utils.extrators.jsonObject.procesarCampo(this.P, clase, f.accessor().prefix("ret."));
					$("return ret;");
				}
			});
		}};
	}

}

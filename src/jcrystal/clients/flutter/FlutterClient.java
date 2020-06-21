package jcrystal.clients.flutter;

import static jcrystal.utils.StringUtils.camelizar;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.json.JSONArray;
import org.json.JSONObject;

import jcrystal.clients.AbsClientGenerator;
import jcrystal.clients.ClientGeneratorDescriptor;
import jcrystal.clients.ClientId;
import jcrystal.configs.clients.Client;
import jcrystal.configs.clients.IInternalConfig;
import jcrystal.entity.types.LongText;
import jcrystal.json.JsonLevel;
import jcrystal.lang.Language;
import jcrystal.main.data.ClientContext;
import jcrystal.model.web.HttpType;
import jcrystal.model.web.IWServiceEndpoint;
import jcrystal.model.web.JCrystalWebService;
import jcrystal.model.web.JCrystalWebServiceParam;
import jcrystal.reflection.annotations.CrystalDate;
import jcrystal.reflection.annotations.LoginResultClass;
import jcrystal.reflection.annotations.Post;
import jcrystal.reflection.annotations.jEntity;
import jcrystal.reflection.annotations.com.jSerializable;
import jcrystal.reflection.annotations.ws.ContentType;
import jcrystal.reflection.annotations.ws.Method;
import jcrystal.results.Tupla2;
import jcrystal.server.FileDownloadDescriptor;
import jcrystal.service.types.Authorization;
import jcrystal.types.IJType;
import jcrystal.types.JClass;
import jcrystal.types.JEnum.EnumValue;
import jcrystal.types.JType;
import jcrystal.types.JVariable;
import jcrystal.types.utils.GlobalTypes;
import jcrystal.types.vars.IAccessor;
import jcrystal.utils.StringSeparator;
import jcrystal.utils.StringUtils;
import jcrystal.utils.context.CodeGeneratorContext;
import jcrystal.utils.context.CodeGeneratorContext.ContextType;
import jcrystal.utils.langAndPlats.AbsCodeBlock;
import jcrystal.utils.langAndPlats.DartCode;

public class FlutterClient extends AbsClientGenerator<Client>{

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
	
	public FlutterClient(ClientGeneratorDescriptor<Client> descriptor){
		super(descriptor);
		entityGenerator = new GeneradorEntidad(this, descriptor);
	}
	@Override
	protected void setupEnviroment() throws Exception {
		ContextType.MOBILE.init();
		CodeGeneratorContext.set(Language.DART, new FlutterTypeConverter(context));
	}
	@Override
	public void generarCliente() throws Exception {
		addResource(paquetePadre, "PrintWriterUtils");
		addResource(paquetePadre, "DBUtils");
		addResource(paquetePadre, "JCFile");
		
		createAbstractNetTasks();
		generarCodigoMobile();
		generarEntidades();
		generarResultados();
		generarEnums();
		GeneradorPushs.generarPushClasses(this);
	}

	private void addResource(Map<String, Object> enviroment, String paquete, String name) throws Exception {
		addResource(FlutterClient.class.getResourceAsStream("net/" + name), enviroment, paquete.replace(".", File.separator) + File.separator + name + ".dart");
	}

	private void addResource(String paquete, String name) throws Exception {
		addResource(FlutterClient.class.getResourceAsStream("net/" + name), paquete.replace(".", File.separator) + File.separator + name + ".dart");
	}

	private void generarEnums() throws Exception {
		final String paquete = paqueteEntidades + ".enums";
		requiredClasses.stream().filter(f->f.isEnum()).forEach(claseEnum->{
			JClass clase = (JClass)claseEnum;
			final DartCode $ = new DartCode() {{
				$("enum " + claseEnum.getSimpleName(), () -> {
					for (EnumValue o : clase.enumData.valores)
						$(o.name+",");
				});
				$("class " + claseEnum.getSimpleName()+"Helper", () -> {
					clase.enumData.propiedades.forEach((key, type)->{
						$("static "+$($convert(type))+ " "+(key.equals("name")?"getName":key)+"("+claseEnum.getSimpleName()+" val)", ()->{
							$("switch(val)",()->{
								for (EnumValue o : clase.enumData.valores)
									if(type.is(String.class))
										$("case "+claseEnum.getSimpleName()+"."+ o.name + " : return \"" + o.propiedades.get(key)+"\";");
									else
										$("case "+claseEnum.getSimpleName()+"."+  o.name + " : return " + o.propiedades.get(key)+";");										
							});
							if(type.is(String.class))
								$("return null;");
							else
								$("return 0;");
						});
					});
					$("static " + claseEnum.getSimpleName() + " fromId(int id)", () -> {
						$("switch(id)", () -> {
							for (EnumValue o : clase.enumData.valores)
								$("case " + o.propiedades.get("id") + ": return " +claseEnum.getSimpleName()+"."+ o.name + ";");
						});
						$("return null;");
					});
				});
			}};
			exportFile($, paquete.replace(".", File.separator) + File.separator + claseEnum.getSimpleName() + ".dart");
		});
		/*TODO: Move to library final DartCode $ = new DartCode() {{
			$("package " + paquete + ";");
			$("public enum JsonLevel", () -> {
				StringSeparator vals = new StringSeparator(", ");
				for (JsonLevel level : JsonLevel.managedValues)
					vals.add(level.name());
				$(vals + ";");
			});
		}};
		exportFile($, paquete.replace(".", File.separator) + File.separator + "JsonLevel.dart");*/
	}
	private void generarClaseResultadoOPush(String paquete, final JClass clase){
		new DartCode() {{
			clase.attributes.stream().forEach(p->{
				p.type.iterate(f->{
					if(f.isAnnotationPresent(jSerializable.class))
						$("import '"+f.getSimpleName()+".dart';");
					else if(f.isEnum())
						$("import '../entities/enums/"+f.getSimpleName()+".dart';");	
				});
			});
			if (clase.isAnnotationPresent(LoginResultClass.class)) {
				$("import 'package:shared_preferences/shared_preferences.dart';");
				$("import 'dart:convert';");
			}
			$("import '../../DBUtils.dart';");
			$("class " + clase.simpleName+" implements ISerializable",()->{// + " , "+netPackage+".ISerializable", () -> {
				clase.attributes.forEach(f->{
					$($($convert(f.type())) + " " + f.name() + ";");
					$($($convert(f.type())) + " get" + StringUtils.capitalize(f.name()) + "(){return this." + f.name() + ";}");
					$("void set" + StringUtils.capitalize(f.name()) + "(" + $($convert(f.type())) + " val){" + f.name() + " = val;}");
				});
				$(clase.simpleName + "()", () -> {
				});
				$("factory "+clase.simpleName+".fromJson(Map<String, dynamic> json)", () -> {
					$("var $ret = "+clase.simpleName+"();");
					clase.attributes.forEach(f->{
						if (f.type().isEnum())
							requiredClasses.add(f.type());
						GeneradorEntidad.procesarCampo(this, clase, f.accessor().prefix("$ret."));
					});
					$("return $ret;");
				});
				$("static List<"+clase.simpleName+"> listFromJson(List<dynamic> json)",()->{
					$("return json.map((f) => "+clase.simpleName+".fromJson(f)).toList();");
				});
				
				
				$("Map<String, dynamic> toJson()",()->{
					$("return <String, dynamic>",()->{
						clase.attributes.forEach(f->{
							$("'"+f.name()+"': "+f.name()+",");
						});	
					},";");
										
				});
				
				if (clase.isAnnotationPresent(LoginResultClass.class))
					crearcodigoTokenSeguridad(context, this, clase);
			});
			exportFile(this, paquete.replace(".", File.separator) + File.separator + clase.getSimpleName() + ".dart");
		}};

		new GeneradorStorage().crearCodigoAlmacenamiento(this, paquete, clase, null);
	}

	public static void crearcodigoTokenSeguridad(ClientContext context, AbsCodeBlock code, JClass clase) {
		code.new B() {
			{
				$("static " + clase.simpleName + " cachedToken = null;");
				$(clase.simpleName + " storeToken()", () -> {
					//$("if(DB" + clase.simpleName + ".store(\"Token\", this))cachedToken = this;");
					$("cachedToken = this;");
					$("SharedPreferences.getInstance().then((prefs)",()->{
						$("prefs.setString('token', json.encode(this.toJson()));");							
					},");");
					$("return this;");
				});
				$("static String getTokenId()", () -> {
					$("if(cachedToken != null)return cachedToken.token;");
					$("else loadToken();");
					$("if(cachedToken == null)return null;");
					$("return cachedToken.token;");
				});
				$("static void deleteToken()async", () -> {
					$("cachedToken = null;");
					$("var prefs = await SharedPreferences.getInstance();");
					$("prefs.remove('token');");
				});
				$("static " + clase.simpleName + " loadToken()", () -> {
					$("if(cachedToken == null)", () -> {
						$("SharedPreferences.getInstance().then((prefs)",()->{
							$("var tokenStr = prefs.getString('token');");
							$if("tokenStr != null && !tokenStr.isEmpty",()->{
								$("cachedToken = "+clase.simpleName+".fromJson(json.decode(tokenStr));");								
							});
							
						},");");
						//$("cachedToken = DB" + clase.simpleName + ".retrieve(\"Token\");");
					});
					$("return cachedToken;");
				});
				$("static Future<" + clase.simpleName + "> loadTokenSync() async", () -> {
					$("if(cachedToken == null)", () -> {
						$("var prefs = await SharedPreferences.getInstance();");
						$("var tokenStr = prefs.getString('token');");
						$if("tokenStr != null && !tokenStr.isEmpty",()->{
							$("cachedToken = "+clase.simpleName+".fromJson(json.decode(tokenStr));");								
						});
					});
					$("return cachedToken;");
				});
				$("static Future<bool> isAuthenticated() async", () -> {
					$("return await loadTokenSync() != null;");
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
		new DartCode() {{
			$("import 'dart:async';");
			$("import '../../../jcrystal/JCrystalApp.dart';");
			$("enum RequestType", () -> {
				$(Arrays.stream(Method.values()).filter(tipo->tipo != Method.DEFAULT).map(f->f.name()).collect(Collectors.joining(", ")));
			});
			$("class RequestTypeHelper", () -> {
				$("static String name(RequestType requestType)",()->{
					$("switch (requestType)",()->{
						Arrays.stream(Method.values()).filter(tipo->tipo != Method.DEFAULT).forEach(t->{
							$("case RequestType."+t.name()+":");
							$("return '"+t.name().toLowerCase()+"';");
						});
					});
					$("return null;");
				});
				$("static bool isPost(RequestType requestType)",()->{
					$("switch (requestType)",()->{
						Arrays.stream(Method.values()).filter(tipo->tipo != Method.DEFAULT).forEach(t->{
							$("case RequestType."+t.name()+":");
							$("return "+t.isPostLike()+";");
						});
					});
					$("return false;");
				});
			});
			$("typedef OnErrorListener = void Function(RequestError error);");
			$("typedef OnVoidSuccessListener = void Function();");
			for(int e = 1; e < 10; e++) {
				String types = IntStream.range(0, e).mapToObj(f->""+(char)('A'+f)).collect(Collectors.joining(", "));
				String params = IntStream.range(0, e).mapToObj(f->""+(char)('A'+f) + " " + (char)('a'+f)).collect(Collectors.joining(", "));
				$("typedef On"+e+"SuccessListener<"+types+"> = void Function("+params+");");					
			}
			SUPPORTED_NATIVE_TYPES.forEach(nativetype->{
				final String name = "On" + StringUtils.capitalize(nativetype.getSimpleName()) + "SuccessListener";
				$("typedef "+name+" = void Function("+$($convert(nativetype))+"  result);");
			});
			$("enum TipoError",()->{
				$("ERROR,SERVER_ERROR,UNAUTHORIZED,NO_INTERNET");
			});
			$("class RequestError implements Exception",()->{
				$("TipoError tipoError;"); 
				$("String mensaje;"); 
				$("int codigo;");
				$("RequestError(String mensaje, {TipoError tipoError = TipoError.ERROR, int codigo = -1})",()->{
					$("this.codigo = codigo;");
					$("this.tipoError = tipoError;");
					$("this.mensaje = mensaje;");
				});
			});
			$("class PrintWriter",()->{
				$("EventSink<List<int>> sink;");
				$("String body = \"\";");
				$("int size = 0;");
				$("PrintWriter({this.sink});");
				$("print(String val)",()->{
					$("sink.add(val.codeUnits);");
					$("if(JCrystalApp.DEBUG)body += val;");
					$("size += val.length;");
				});
				$("append(String val)",()->{
					$("sink.add(val.codeUnits);");
					$("if(JCrystalApp.DEBUG)body += val;");
					$("size += val.length;");
				});
				$("close()",()->{
					$("sink.close();");
				});
			});
			exportFile(this, netPackage.replace(".", File.separator) + File.separator + "NetUtils.dart");
		}};
		new DartCode() {{
			$("class JCrystalApp", () -> {
				$("static bool DEBUG = false;"); 
				$("static String APP_VERSION = \"None\";"); 
				$("static int APP_CODE = 0;");
			});
			exportFile(this, paquetePadre.replace(".", File.separator) + File.separator + "JCrystalApp.dart");
		}};
		
		
		for(IInternalConfig config : descriptor.configs.values()) {
			final String id = config.id()==null?"Default":config.id();
			new DartCode() {{
				$("import 'dart:async';");
				$("import 'dart:convert';");
				$("import 'package:http/http.dart' as http;");
				$("import 'NetUtils.dart';");
				$("import '../../JCrystalApp.dart';");

				$("typedef StringProducer = String Function();");
				$("typedef BodyMaker = Future<void> Function(PrintWriter, [http.MultipartRequest]);");
				$("typedef OnResponse<T> = void Function(T);");

				$("abstract class Abs"+id+"Manager<T>", () -> {
					$("static final String BASE_URL = \"" + config.BASE_URL(null)+"\";");
					$("bool formData = false;");
					$("OnErrorListener onError;");
					$("RequestType type;");
					$("var headers = {\"Accept\":\"application/json\"};");
					$("Abs"+id+"Manager({this.onError});");
					$("StringProducer getUrl;");
					$("BodyMaker makeBody;");
					$("OnResponse onResponse;");
					$("void doRequest() async", () -> {										
						$("if(JCrystalApp.DEBUG)print(type.toString() + \" \" + BASE_URL + getUrl());");
						
						$if("RequestTypeHelper.isPost(type)",()->{
							$if("!formData",()->{
								$("headers[\"Content-Type\"] = \"application/json\";");
							});
						});
						$("http.BaseRequest request;");
						$if("RequestTypeHelper.isPost(type)",()->{
							$if("formData",()->{
								$("var multipartRequest = new http.MultipartRequest(RequestTypeHelper.name(type), Uri.parse(BASE_URL + getUrl()));");
								$("await makeBody(null, multipartRequest);");									
								$("request = multipartRequest;");
							});
							$else(()->{
								$("var streamRequest = new http.StreamedRequest(RequestTypeHelper.name(type), Uri.parse(BASE_URL + getUrl()));");
								$("var _pw = new PrintWriter(sink:streamRequest.sink);");
								$("await makeBody(_pw);");
								$("_pw.close();");
								$("streamRequest.contentLength = _pw.size;");
								$("if(JCrystalApp.DEBUG)",()->{
									$("print(\"Body: \" + _pw.body);");
									$("_pw.body = null;");
								});
								$("request = streamRequest;");
							});
							
						});
						$else(()->{
							$("request = new http.Request(RequestTypeHelper.name(type), Uri.parse(BASE_URL + getUrl()));");
						});
						$("request.headers.addAll(headers);");
						$("final http.BaseResponse response = await request.send().then(http.Response.fromStream);");
						$("if(JCrystalApp.DEBUG)print(response.statusCode);");
						$if("response.statusCode >= 200 && response.statusCode <= 299", () -> {
							$("var resp = (response as http.Response).body;");
							$("if(JCrystalApp.DEBUG)print(resp);");
							$("try", () -> {
								$("getResponse(resp);");
							});
							$("catch(ex)", () -> {
								$("if(JCrystalApp.DEBUG)print(ex);");
								$("if(onError!=null)onError(new RequestError(\"Ocurrió un error con el servidor\", tipoError:TipoError.SERVER_ERROR));");
							});
							
						});
						$else(() -> {
							$if("onError != null", () -> {
								$("var resp = (response as http.Response).body;");
								$("if(JCrystalApp.DEBUG)print(resp);");
								$("onError(new RequestError(resp, codigo:response.statusCode));");
							});
						});
					});
					$("Abs"+id+"Manager doFormData()",()->{
						$("formData = true;");
						$("return this;");
					});
					$("T getResponse(String resp);");
					
					$("Abs"+id+"Manager<T> authorization(String val)",()->{
						$("headers[\""+config.AUTHORIZATION_NAME()+"\"] = val;");
						$("return this;");
					});
					$("Abs"+id+"Manager<T> header(String key, String val)",()->{
						$("headers[key] = val;");
						$("return this;");
					});
					for(Method t : Method.values()) {
						if(t != Method.DEFAULT) {
							$("Abs"+id+"Manager<T> do"+StringUtils.capitalize(t.name().toLowerCase())+"()",()->{
								$("this.type = RequestType."+t.name().toUpperCase()+";");;
								$("doRequest();");
								$("return this;");
							});
						}
					}
				});
				for(Class<?> clase : new Class<?>[] {String.class, JSONObject.class, JSONArray.class}) {
					IJType tipo = new JType(null, clase);
					$("class "+tipo.getSimpleName()+"Resp extends Abs"+id+"Manager<"+$($convert(tipo))+">",()->{
						$(tipo.getSimpleName()+"Resp(OnErrorListener onError) : super(onError : onError)",()->{
						});
						$("url(StringProducer getUrl)",()->{
							$("this.getUrl = getUrl;");
							$("return this;");
						});
						$("body(BodyMaker makeBody)",()->{
							$("this.makeBody = makeBody;");
							$("return this;");
						});
						$("response(OnResponse onResponse)",()->{
							$("this.onResponse = onResponse;");
							$("return this;");
						});						
						$($($convert(tipo))+" getResponse(String resp)",()->{
							if (clase == String.class) {
								$("onResponse(resp);");
								$("return resp;");
								return;
							}
							else
								$("var data = json.decode(resp);");

							if (config.SUCCESS_TYPE() != null) {
								IJType successType = getSuccessType(config.SUCCESS_TYPE());
								if (clase == JSONArray.class)
									$("final " + $($convert(successType)) + " success = 1;");
								else
									$("final " + $($convert(successType)) + " success = data[\"success\"] ?? " + config.SUCCESS_DAFAULT_VALUE() + ";");
							}
							$if(config.SUCCESS_CONDITION(), "onResponse(data);return data;");
							if (config.ERROR_CONDITION() != null)
								$if(config.ERROR_CONDITION(), () -> {
									if (clase == JSONArray.class)
										$("onError(new RequestError(\"SERVER ERROR\", codigo:0));");
									else
										$("onError(new RequestError(data[\"" + config.ERROR_MESSAGE_NAME() + "\"], codigo:data[\"code\"] ?? 0));");
									$("return null;");
								});
							if (config.UNATHORIZED_CONDITION() != null)
								$if(config.UNATHORIZED_CONDITION(), () -> {
									if (clase == JSONArray.class)
										$("onError(new RequestError(\"SERVER ERROR\", codigo:0));");
									else
										$("onError(new RequestError(data[\"" + config.ERROR_MESSAGE_NAME() + "\"], tipoError : TipoError.UNAUTHORIZED));");
									$("return null;");
								});
							if (config.SUCCESS_TYPE() != null) {
								if (clase == JSONArray.class)
									$("onError(new RequestError(\"SERVER ERROR\", codigo:0));");
								else
									$("onError(new RequestError(data[\"" + config.ERROR_MESSAGE_NAME() + "\"], tipoError : TipoError.SERVER_ERROR));");
								$("return null;");
							}
						});
					});
				}
				exportFile(this, netPackage.replace(".", File.separator) + File.separator + "Abs"+id+"Manager.dart");
			}};
			
		}
	}

	private void generarCodigoMobile() throws Exception {
		for (final Map.Entry<String, Set<IWServiceEndpoint>> entry : endpoints.entrySet()) {
			String name = "Manager" + StringUtils.capitalize(StringUtils.camelizarSoft(entry.getKey().contains("/") ? entry.getKey().substring(entry.getKey().lastIndexOf('/') + 1) : entry.getKey()));
			final String paquete;
			if (entry.getKey().contains("/"))
				paquete = paqueteControllers + "." + entry.getKey().substring(0, entry.getKey().lastIndexOf('/')).replace("/", ".");
			else
				paquete = paqueteControllers;
			new DartCode() {{
				$("import 'dart:io';");
				$("import '../../../JCFile.dart';");
				$("import '../../../PrintWriterUtils.dart';");
				$("import '../AbsDefaultManager.dart';");
				$("import '../NetUtils.dart';");
				$("import 'dart:convert';");
				$("import 'package:http/http.dart' as http;");
				imports:{
					for(final IWServiceEndpoint endPoint : entry.getValue())
						endPoint.gatherRequiredTypes(imports);
					int numSlash =  entry.getKey().length() - entry.getKey().replace("/", "").length();
					$imports(numSlash);
				}
					/*$("import " + paqueteUtils + ".*;");
					if(!GeneradorRutas.clientData.clases_resultados.isEmpty())
						$("import " + paqueteResultados + ".*;");
					if(!GeneradorRutas.clientData.clases_entidades.isEmpty()) {
						$("import " + paqueteEntidades + ".*;");
						$("import " + paqueteEntidades + ".enums.*;");
					}
					$("import " + paqueteMobile + ".*;");
					$("import " + FlutterClient.paqueteDates + ".*;");
					$("import " + paqueteControllers.substring(0, paqueteControllers.lastIndexOf('.')) + ".*;");
					$("import android.os.AsyncTask;");
					$("import static jcrystal.JSONUtils.*;");*/
					$("var jsonQuote = json.encode;");
					$("class " + name, () -> {
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
								else if (m.getReturnType().getSimpleName().startsWith("Tupla")) {
									final List<IJType> tipos = new ArrayList<>();
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
								final boolean nativeEmbeddedResponse = methodConfig.embeddedResponse() && SUPPORTED_NATIVE_TYPES.contains(m.getReturnType());
								if(m.getReturnType().is(jcrystal.server.FileDownloadDescriptor.class)) {
									$("static String " + methodName + "(" + parametros.stream().map(this::$V).collect(Collectors.joining(", ")) + ")", () -> {
										String ruta = m.getPath(descriptor);
										for (final JCrystalWebServiceParam param : m.parametros)
											if (param.tipoRuta == HttpType.PATH)
												ruta = ruta.replace("$" + param.nombre, "\"+" + param.nombre + "+\"");
										$("String ruta = \"" + ruta + "\";");
										processGetParams(this, clientParams.v1.get(HttpType.GET));
										$("return Abs"+methodConfig.getDefId()+"Manager.BASE_URL + ruta;");
									});
								}else {
									String helperResponseSimpleType = (nativeEmbeddedResponse ? "String" : m.isJsonArrayResponse(methodConfig) ? "JSONArray" : "JSONObject");
									$("static void " + methodName + "(" + parametros.stream().map(this::$V).collect(Collectors.joining(", ")) + ")", () -> {
										parametros.remove(parametros.size()-1);
										String token = m.parametros.stream().filter(f->f.tipoRuta == HttpType.HEADER).map(f->{
											if(f.type().is(Authorization.class))
												return ".authorization("+f.nombre+")";
											else if(f.type().is(ClientId.class))
												return ".header(\""+f.nombre+"\", \""+descriptor.client.id+"\")";
											else
												throw new NullPointerException("Unssuported type  for header : " + f.type());
										}).collect(Collectors.joining());

										if (m.tokenParam != null)
											token += ".authorization("+m.tokenParam.type().getSimpleName() + ".getTokenId())";
										if (m.isMultipart())
											token += ".doFormData()";
										token += ".do"+StringUtils.capitalize(m.tipoRuta.name().toLowerCase())+"()";
										
										$("new "+helperResponseSimpleType+"Resp(onError)");
										$(".url(()",()->{
											String ruta = m.getPath(descriptor);
											for (final JCrystalWebServiceParam param : m.parametros)
												if (param.tipoRuta == HttpType.PATH)
													ruta = ruta.replace("$" + param.nombre, "\"+" + param.nombre + "+\"");
											$("String ruta = \"" + ruta + "\";");
											processGetParams(this, clientParams.v1.get(HttpType.GET));
											$("return ruta;");
										},")");
										$(".response((result)",()->{
											if (m.unwrappedMethod.isVoid)
												$("onSuccess();");
											else if (m.getReturnType().is(String.class))
												$("onSuccess(result.getString(\"r\"));");
											else if (SUPPORTED_NATIVE_TYPES.contains(m.getReturnType())) {
												if (nativeEmbeddedResponse)
													$("onSuccess(" + m.getReturnType().getObjectType().getSimpleName() + ".parse" + StringUtils.capitalize(m.getReturnType().getSimpleName()) + "(result));");
												else
													$("onSuccess(result.get" + StringUtils.capitalize(m.getReturnType().getSimpleName()) + "(\"r\"));");
											} else if (m.getReturnType().isIterable()) {
												final IJType tipoParamero = m.getReturnType().getInnerTypes().get(0);
												if(tipoParamero.isAnnotationPresent(jEntity.class)) {
													$("var $array = result[\"r\"];");
													$("List<" + $($convert(tipoParamero)) + "> $lista = List<" + $($convert(tipoParamero)) + ">($array.length);");
													$("for(int pos = 0, l = $array.length; pos < l; pos++)", () -> {
														$("$lista[pos] = new " + $($convert(tipoParamero)) + "($array[pos], JsonLevel." + m.getJsonLevel(tipoParamero).name() + ");");
													});
													$("onSuccess($lista);");
												} else if (tipoParamero.isAnnotationPresent(jSerializable.class)) {
													requiredClasses.add(tipoParamero);
													if (methodConfig.embeddedResponse()) {
														$("var $array = result;");
													} else {
														$("var $array = result[\"r\"];");
													}
													$("List<" + $($convert(tipoParamero)) + "> $lista = new List($array.length);");
													$("for(int pos = 0, l = $array.length; pos < l; pos++)", () -> {
														$("$lista[pos] = " + $($convert(tipoParamero)) + ".fromJson($array[pos]);");
													});
													$("onSuccess($lista);");
												}
											}else if (m.getReturnType().isAnnotationPresent(jEntity.class)) {
												if (methodConfig.embeddedResponse()) {
													if (m.getReturnType().isAnnotationPresent(LoginResultClass.class))
														$("onSuccess(new " + $($convert(m.getReturnType())) + "(result, JsonLevel." + m.getJsonLevel(m.getReturnType()).name() + ").storeToken());");
													else
														$("onSuccess(new " + $($convert(m.getReturnType())) + "(result, JsonLevel." + m.getJsonLevel(m.getReturnType()).name() + "));");
												}else if (m.getReturnType().isAnnotationPresent(LoginResultClass.class))
													$("onSuccess(result[\"r\"]==null?null:new " + $($convert(m.getReturnType())) + "(result[\"r\"], JsonLevel." + m.getJsonLevel(m.getReturnType()).name() + ").storeToken());");
												else
													$("onSuccess(result[\"r\"]==null?null:new " + $($convert(m.getReturnType())) + "(result[\"r\"], JsonLevel." + m.getJsonLevel(m.getReturnType()).name() + "));");
											} else if (m.getReturnType().isAnnotationPresent(jSerializable.class)) {
												 if (methodConfig.embeddedResponse())
													 $("onSuccess(" + $($convert(m.getReturnType())) + ".fromJson(result));");
												 else if (m.getReturnType().isAnnotationPresent(LoginResultClass.class))
													 $("onSuccess(result[\"r\"]==null?null:" + $($convert(m.getReturnType())) + ".fromJson(result[\"r\"]).storeToken());");
												else
													$("onSuccess(result[\"r\"]==null?null:" + $($convert(m.getReturnType())) + ".fromJson(result[\"r\"]));");
											} else if (m.getReturnType().getSimpleName().startsWith("Tupla")) {
												final List<IJType> tipos = m.getReturnType().getInnerTypes();
												if (methodConfig.embeddedResponse())
													$("var $array = result;");
												else
													$("var $array = result[\"r\"];");
												StringSeparator sp = new StringSeparator(',');
												int e = 0;
												for (IJType p : tipos) {
													if (p.getInnerTypes().isEmpty()) {
														if (p.isAnnotationPresent(jEntity.class)) {
															if(p.isAnnotationPresent(LoginResultClass.class)){
																sp.add("$array["+e+"]==null?null:(new " + $($convert(p)) + "($array[" + e + "], JsonLevel." + m.getJsonLevel(p).name() + ").storeToken())");
															}else 
																sp.add("$array["+e+"]==null?null:new " + $($convert(p)) + "($array[" + e + "], JsonLevel." + m.getJsonLevel(p).name() + ")");
														} else if (p.isAnnotationPresent(jSerializable.class)) {
															if(p.isAnnotationPresent(LoginResultClass.class)){
																sp.add("$array["+e+"]==null?null:(" + $($convert(p)) + ".fromJson($array[" + e + "]).storeToken())");
															}else 
																sp.add("$array["+e+"]==null?null:" + $($convert(p)) + ".fromJson($array[" + e + "])");
														} else if (p.is(Integer.class))
															sp.add("$array["+e+"]==null?null:$array.getInt(" + e + ")");
														else if (p.isPrimitiveObjectType())
															sp.add("$array["+e+"]==null?null:$array.get"+p.getSimpleName()+"(" + e + ")");
														else if (p.is(String.class))
															sp.add("$array["+e+"]==null?null:$array.getString(" + e + ")");
														else
															throw new NullPointerException("Unssuported type " + p);
													} else{
														if (p.isSubclassOf(List.class)) {
															final IJType pClass = p.getInnerTypes().get(0);
															if (pClass.isAnnotationPresent(jEntity.class)) {
																sp.add($($convert(pClass)) + ".ListUtils.listFromJson" + m.getJsonLevel(pClass).baseName() + "($array[" + e + "])");
															} else if (pClass.isAnnotationPresent(jSerializable.class)) {
																sp.add($($convert(pClass)) + ".listFromJson($array[" + e + "])");
															} else
																throw new NullPointerException("Unssuported type " + p);
														} else
															throw new NullPointerException("Error, tipos incompatibles. No se puede tener un metodo con retorno " + p);
													}
													e++;
												}
												$("onSuccess(" + sp + ");");
											} else
												$("onSuccess(result);");
										},")");
										if (m.tipoRuta.isPostLike()) {
											if (!m.isMultipart()) {
												List<JCrystalWebServiceParam> bodyParams = m.parametros.stream().filter(param -> param.tipoRuta.isPostLike() && param.nombre.equals("body")).collect(Collectors.toList());
												if (bodyParams.size() > 1)
													throw new NullPointerException("Too many bodys");
												else if (bodyParams.isEmpty())
													$(".body((_pw, [multipart])async",()->{
														context.utils.generadorToJson.fillBody(this, m.parametros.stream().filter(param -> param.tipoRuta.isPostLike()).map(entityUtils::changeToKeys).collect(Collectors.toList()));														
													},")");
												else {
													$(".body((_pw, [multipart])async",()->{
														IAccessor param = entityUtils.changeToKeys(bodyParams.get(0));
														
														if (param.type().is(String.class, org.json.JSONObject.class)) {
															$("_pw.add(" + param.name() + ".toString().codeUnits);");
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
																JClass superClase = context.data.entidades.get(context.data.entidades.targetEntity(tipo)).clase;
																JsonLevel level = tipo.getAnnotation(Post.class).level();
																if (param.name().equals("body"))
																	$if(param.name() + " != null", () -> {
																		$(paqueteEntidades + "." + superClase.simpleName + ".toJson" + superClase.simpleName + "(_pw, " + param.name() + ");");
																	});
															} else
																throw new NullPointerException("Unssuported post type " + param.type().name()); // TODO: Si se postea una lista de Posts o Jsonifies
														} else
															throw new NullPointerException("Unssuported post type " + param.type().name());
													},")");
												}
											}else {
												$(".body((writer, [multipart])async",()->{
													for (JCrystalWebServiceParam param : m.parametros) 
														if(param.tipoRuta.isPostLike()){
															if(param.p.type().is("jcrystal.server.FileUploadDescriptor")) {
																$if(param.name()+" != null",()->{
																	$("multipart.files.add(await http.MultipartFile.fromPath('"+param.name()+"', "+param.name()+".file));");																	
																});
															}else
																$if(param.p.type().isPrimitive() ? null : (param.nombre + "!= null"), () -> {
																	onMultipartBoundary(this, param.nombre, ()->{
																		if (param.p.type().isPrimitive()) {
																			$("writer.print(" + param.p.type().getObjectType().name() + ".toString(" + param.nombre + "));");
																		} else if (param.p.type().is(String.class, LongText.class)) {
																			$("writer.print(Uri.encodeFull(" + param.nombre + "));");
																		} else if (param.p.type().isPrimitiveObjectType()) {
																			$("writer.print(" + param.p.type().getObjectType().name() + ".toString(" + param.nombre + "));");
																		} else if (param.p.type().isJAnnotationPresent(CrystalDate.class)) {
																			$("writer.print(" + param.nombre + ".format());");
																		} else
																			throw new NullPointerException("Unssuported form data post type " + param.p.type().name());
																	});
																});
														}
												},")");
											}
										}
										$(token+";");
									});
								}
							}
						}
					});
					exportFile(this, paquete.replace(".", File.separator) + File.separator + name + ".dart");					
				}
			};
		}
	}

	private void createUtilFuntions() throws Exception {
		new DartCode() {{
			$("package " + paqueteUtils + ";");
			$("public interface Predicate<T>", () -> {
				$("public boolean eval(T t);");
			});
			exportFile(this, paqueteUtils.replace(".", File.separator) + File.separator + "Predicate.dart");
		}};
		new DartCode() {{
			$("package " + paqueteUtils + ";");
			$("public interface Function<In, Out>", () -> {
				$("public Out eval(In t);");
			});
			exportFile(this, paqueteUtils.replace(".", File.separator) + File.separator + "Function.dart");
		}};
	}
	
	private void onMultipartBoundary(AbsCodeBlock code, String name, Runnable r) {
		code.$("writer.println(\"-----------------------------\" + boundary);");
		code.$("writer.append(\"Content-Disposition: form-data; name=" + name + "\\r\\n\");");
		code.$("writer.append(\"\\r\\n\");");
		r.run();
		code.$("writer.append(\"\\r\\n\");");
	}
	@Override
	protected <X extends AbsCodeBlock> void processGetParams(X METODO, List<JVariable> params) {
		if(params.isEmpty())
			return;
		METODO.new B() {{
			$("String params = null;");
			for (JVariable param : params) {
				if(param.type().nullable()) {
					$("if(" + param.name() + " != null){");
					METODO.incLevel();
				}
				if (param.type().isPrimitive())
					$("params = (params==null?\"?\":(params + \"&\")) + \"" + param.name() + "=\" + " + param.name()  + ".toString();");
				else if (param.type().is(String.class))
					$("params = (params==null?\"?\":(params + \"&\")) + \"" + param.name() + "=\" + Uri.encodeFull(" + param.name() + ");");
				else if (param.type().isPrimitiveObjectType())
					$("params = (params==null?\"?\":(params + \"&\")) + \"" + param.name() + "=\" + " + param.name() + ".toStr	ing();");
				else if (param.type().isEnum())
					$("params = (params==null?\"?\":(params + \"&\")) + \"" + param.name() + "=\" + " + param.name() + ".id;");
				else if (param.type().getSimpleName().equals("JSONObject"))
					throw new NullPointerException();
				else if (param.type().getSimpleName().equals("JSONArray"))
					throw new NullPointerException();
				else if (param.type().isJAnnotationPresent(CrystalDate.class))
					$("params = (params==null?\"?\":(params + \"&\")) + \"" + param.name() + "=\" + " + param.name() + ".format();");
				else
					throw new NullPointerException("Parametro no reconocido " + param.type().getSimpleName());
				if(param.type().nullable()) {
					$("}");
					METODO.decLevel();
				}
			}
			$("if(params != null)ruta+=params;");
		}};
	}
	@Override
	protected <X extends AbsCodeBlock> void processPostParams(X METODO, ContentType contentType, List<JVariable> params) {
		throw new NullPointerException("processPostParams");
	}

	public static void generateFromListJson(AbsCodeBlock bloque, final JClass clase, final JsonLevel level) {
		bloque.new B() {{
			$("public static java.util.ArrayList<" + clase.getSimpleName() + (level == null ? "" : level.baseName()) + "> listFromJson" + (level == null ? "" : level.baseName()) + "(List<dynamic> json)throws org.json.JSONException", () -> {
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
						GeneradorEntidad.procesarCampo(this.P, clase, f.accessor().prefix("ret."));
					$("return ret;");
				}
			});
		}};
	}

}

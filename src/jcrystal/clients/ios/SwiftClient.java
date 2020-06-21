package jcrystal.clients.ios;

import static jcrystal.utils.StringUtils.camelizar;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.json.JSONArray;
import org.json.JSONObject;

import jcrystal.clients.AbsClientGenerator;
import jcrystal.clients.ClientGeneratorDescriptor;
import jcrystal.clients.ClientId;
import jcrystal.configs.clients.IInternalConfig;
import jcrystal.configs.clients.JClientIOS;
import jcrystal.datetime.DateType;
import jcrystal.json.JsonLevel;
import jcrystal.lang.Language;
import jcrystal.main.data.ClientContext;
import jcrystal.model.web.HttpType;
import jcrystal.model.web.IWServiceEndpoint;
import jcrystal.model.web.JCrystalWebService;
import jcrystal.model.web.JCrystalWebServiceParam;
import jcrystal.reflection.annotations.CrystalDate;
import jcrystal.reflection.annotations.EntityProperty;
import jcrystal.reflection.annotations.LoginResultClass;
import jcrystal.reflection.annotations.Post;
import jcrystal.reflection.annotations.jEntity;
import jcrystal.reflection.annotations.com.jSerializable;
import jcrystal.reflection.annotations.ws.ContentType;
import jcrystal.reflection.annotations.ws.Method;
import jcrystal.results.Tupla2;
import jcrystal.service.types.Authorization;
import jcrystal.types.IJType;
import jcrystal.types.JClass;
import jcrystal.types.JEnum.EnumValue;
import jcrystal.types.JType;
import jcrystal.types.JVariable;
import jcrystal.types.utils.GlobalTypes;
import jcrystal.utils.StringUtils;
import jcrystal.utils.context.CodeGeneratorContext;
import jcrystal.utils.context.CodeGeneratorContext.ContextType;
import jcrystal.utils.langAndPlats.AbsCodeBlock;
import jcrystal.utils.langAndPlats.SwiftCode;
public class SwiftClient extends AbsClientGenerator<JClientIOS>{
	
	public static String jCrystalPackage = "jCrystaliOSPackage";
	
	public static final String paqueteEntidades = "jCrystal.Sources.jCrystal.mobile.entities";
	public static final String paqueteEnums = "jCrystal.Sources.jCrystal.mobile.enums";
	private static final String paqueteResultados = "jCrystal.Sources.jCrystal.mobile.results";
	private static final String paqueteUtils = "jCrystal.Sources.jCrystal.mobile.net.utils";
	public static final String paqueteValidadores = "jCrystal.Sources.jCrystal.mobile.validators";
	public static final String paqueteSerializer = "jCrystal.Sources.jCrystal.mobile.serializer";
	private static final String paqueteControllers = "jCrystal.Sources.jCrystal.mobile.net.controllers";
	private static final String paquetePadre = "jCrystal.Sources.jCrystal";
	
	final GeneradorSerializer serializerGenerator;
	
	public SwiftClient(ClientGeneratorDescriptor<JClientIOS> descriptor){
		super(descriptor);
		entityGenerator = new GeneradorEntidad(this, descriptor);
		serializerGenerator = new GeneradorSerializer(this);
	}
	
	@Override
	protected void setupEnviroment() throws Exception {
		ContextType.MOBILE.init();
		CodeGeneratorContext.set(Language.SWIFT, new IOSTypeConverter(context));
	}
	@Override
	public void generarCliente() throws Exception {
		addResource("jCrystal", "Package");
		
		createAbstractNetTasks();
		generarCodigoMobile();
		generarEntidades();
		generarResultados();
		generateEnums();
	}
	private void addResource(Map<String, Object> enviroment, String paquete, String name) throws Exception{
		addResource(SwiftClient.class.getResourceAsStream("netswift/" + name), enviroment, paquete.replace(".", File.separator) + File.separator + name + ".swift");
	}
	void addResource(String paquete, String name) throws Exception{
		addResource(SwiftClient.class.getResourceAsStream("netswift/" + name), paquete.replace(".", File.separator) + File.separator + name + ".swift");
	}
	private void generateEnums()throws Exception{
		requiredClasses.stream().filter(f->f.isEnum()).forEach(claseEnum->{
			JClass clase = (JClass)claseEnum;
			final SwiftCode $ = new SwiftCode(){{
				$("public enum " + claseEnum.getSimpleName()+" : Int", ()->{
					try {
						for (EnumValue o : clase.enumData.valores)
							$("case " + o.name + " = " + o.propiedades.get("id"));
						
						clase.enumData.propiedades.forEach((key, type)->{
							if(!key.equals("id"))
								$("var "+key+": "+$($convert(type)), ()->{
									$("switch self",()->{
										for (EnumValue o : clase.enumData.valores)
											if(type.is(String.class))
												$("case ."+ o.name + " : return \"" + o.propiedades.get(key)+"\"");
											else
												$("case ."+ o.name + " : return " + o.propiedades.get(key)+"");										
									});
								});
						});
						$("var id: Int", ()->{
							$("return self.rawValue");
						});
						$("public static func fromId(_ id : Int) -> " + claseEnum.getSimpleName(), ()->{
							$("return " + claseEnum.getSimpleName() + "(rawValue: id)!");
						});
					} catch (Exception ex) {
						throw new NullPointerException("Id is absent for enum "+claseEnum.getSimpleName());
					}
					$("static let values = ["+clase.enumData.valores.stream().map(d->d.name).collect(Collectors.joining(", "))+"]");
				});
			}};
			exportFile($, paqueteEnums.replace(".", File.separator) + File.separator + claseEnum.getSimpleName() + ".swift");
		});
	}
	private void generarResultados()throws Exception{
		requiredClasses.stream().filter(f->f.isAnnotationPresent(jSerializable.class)).forEach(clase->{
			JClass jclase = (JClass)clase;
			entityGenerator.entityValidatorGenerator.validateResult(jclase);
			new SwiftCode(){{
				$("import Foundation");
				$("import " + jCrystalPackage);
				$("public class " + clase.getSimpleName(), ()->{
					for(final JVariable f : jclase.attributes) {
						if(f.type().isPrimitive()) {
							if(f.type().is(boolean.class))
								$("fileprivate var _" + f.name() + " : " + $($convert(f.type()))+ " = false");
							else
								$("fileprivate var _" + f.name() + " : " + $($convert(f.type()))+ " = 0");
						}else
							$("fileprivate var _" + f.name() + " : " + $($convert(f.type())));
					}
					for(final JVariable f : jclase.attributes) {
						$("public func " + (f.name()) + "()-> " + $($convert(f.type())) + "{return self._"+f.name()+"}");
						$("public func " + (f.name()) + "(set val: "+$($convert(f.type()))+"){self._"+f.name()+" = val}");
						if (f.type().is(Date.class) && f.isAnnotationPresent(EntityProperty.class)) {
							$("private static let _SDF_" + f.name() + " = new java.text.SimpleDateFormat(\"" + DateType.DATE_MILIS.format + "\");");
							$("static", ()->{
								$("SDF_" + f.name() + ".setTimeZone(java.util.TimeZone.getTimeZone(\"UTC\"));");
							});
						}
					}
					$("init()", ()->{});
					
					if(clase.isAnnotationPresent(LoginResultClass.class))
						crearCodigoTokenSeguridad(context, this, jclase);
				});
				serializerGenerator.generateResultClass(jclase, this);
				exportFile(this, paqueteResultados.replace(".", File.separator) + File.separator + clase.getSimpleName() + ".swift");
			}};
			crearCodigoAlmacenamiento(paqueteResultados, jclase, null);
		});
	}
	
	public static void crearCodigoTokenSeguridad(ClientContext context, SwiftCode code, JClass clase) {
		code.new B() {{
				$("private static var cachedToken : "+clase.getSimpleName()+"! = nil");
				$("public func storeToken()-> " + clase.getSimpleName(), ()->{
					$if("DB" + clase.getSimpleName() + ".store(\"Token\", self)", clase.getSimpleName()+".cachedToken = self");
					$("return self");
				});
				$("public static func getTokenId() -> String!", ()->{
					$("if let ct = getToken() {return ct.token()}");
					$("return nil");
				});
				$("public static func deleteToken()", ()->{
					$("cachedToken = nil");
					$("DB"+clase.getSimpleName()+".delete(\"Token\")");
				});
				$("public static func getToken()-> " + clase.getSimpleName()+"!", ()->{
					$if("cachedToken == nil", ()->{
						$("cachedToken = DB"+clase.getSimpleName()+".retrieve(\"Token\")");
					});
					$("return cachedToken");
				});
				$("public static func isAuthenticated()-> Bool", ()->{
					$("return getToken() != nil");
				});
				if(context.data.rolClass != null){
					for(final EnumValue value : context.data.rolClass.enumData.valores) {
						String name = "Rol" + StringUtils.camelizar(value.name);
						$("public static func is"+name+"()-> Bool", ()->{
							try {
								$("return getToken() != nil && (cachedToken.rol() & " + value.propiedades.get("id") + ") != 0");
							} catch (Exception e) {
								$("return false");
							}
						});
					}
				}
		}};
	}
	private void createAbstractNetTasks() throws Exception{
		final String netPackage = paqueteControllers.substring(0, paqueteControllers.lastIndexOf('.'));
		final SwiftCode codeEnum = new SwiftCode() {{
			$("enum RequestType : String", () -> {
				Arrays.stream(Method.values()).filter(tipo->tipo != Method.DEFAULT).forEach(f->{
					$("case " + f.name()+" = \""+f.name()+"\"");
				});
				$("var isPost : Bool",()->{
					$("switch self",()->{
						Arrays.stream(Method.values()).filter(tipo->tipo != Method.DEFAULT).forEach(f->{
							$("case ." + f.name()+": return " + f.isPostLike());						
						});
					});
				});
			});
		}};
		exportFile(codeEnum, netPackage.replace(".", File.separator) + File.separator + "RequestType.swift");
		for(IInternalConfig config : descriptor.configs.values()) {
			final String id = config.id()==null?"Default":config.id();
			final SwiftCode code = new SwiftCode() {{
				$("import Foundation");
				$("import " + jCrystalPackage);
				$("public class Abs"+id+"Manager", () -> {
					$("static let BASE_URL = \"" + config.BASE_URL(null)+"\"");
					$("var formData = false");
					$("var boundary = \"\"");
					$("var type = RequestType.GET");
					$("var headers : [String:String] = [:]");
					$("var onError : (RequestError)->()");
					$("var url : String");
					$("var contentURL : URL!");
					$("var makeBody : (("+$($convert(GlobalTypes.Java.PrintWriter))+", Abs"+id+"Manager) -> ())?");
					$("init(url : String, onError : @escaping (RequestError)->())", () -> {
						$("self.onError = onError");
						$("self.url = url");
					});
					$("func doFormData() -> Abs"+id+"Manager", () -> {
						$("formData = true");
						$("self.boundary = \"Boundary-\\(UUID().uuidString)\"");
						$("return self");
					});
					$("func doRequest()", () -> {	
						$("let ruta = Abs"+id+"Manager.BASE_URL + url");
						$("if DEBUG {print( type.rawValue + ruta) }");
						$("let requestURL: URL = URL(string: ruta)!");
						$("let urlRequest: NSMutableURLRequest = NSMutableURLRequest(url: requestURL)");
						$("urlRequest.setValue(\"application/json\", forHTTPHeaderField: \"Accept\")");
						$("urlRequest.httpMethod = type.rawValue");
						$("for (key, value) in headers",()->{
							$("urlRequest.setValue(value, forHTTPHeaderField: key)");
						});
						$("let session = URLSession.shared");
						$if("type.isPost",()->{
							$if("formData",()->{
								$("urlRequest.setValue(\"multipart/form-data; boundary=---------------------------\"+boundary, forHTTPHeaderField: \"Content-Type\")");
							});
							$else(()->{
								$("urlRequest.setValue(\"application/json\", forHTTPHeaderField: \"Content-Type\")");	
							});
							
							$("contentURL = URL(fileURLWithPath: NSTemporaryDirectory()).appendingPathComponent(UUID().uuidString)");
							$("let _pw = OutputStream(url: contentURL!, append: false)!");
							$("_pw.open()");
							$if("let bodyMaker = makeBody",()->{
								$("bodyMaker(_pw, self)");									
							});
							
							$("_pw.close()");
							$("if DEBUG{try? print(\"CONTENT: \\(String(contentsOf: contentURL))\")}");
							$L("let task = session.uploadTask(with: urlRequest as URLRequest, fromFile: contentURL, completionHandler:", new AbsCodeBlock.Lambda("data, response, error") {
								@Override public void run() {
									$("self.completationHandler(data : data, response : response, error : error)");
								}
							}, ")");
							$("task.resume()");
						});
						$else(()->{
							$L("let task = session.dataTask(with: urlRequest as URLRequest, completionHandler:", new AbsCodeBlock.Lambda("data, response, error") {
								@Override public void run() {
									$("self.completationHandler(data : data, response : response, error : error)");
								}
							}, ")");
							$("task.resume()");
						});
					});
					$("func completationHandler(data : Data?, response : URLResponse?, error : Error?)",()->{
						$if("let httpResponse = response as? HTTPURLResponse", ()->{
							$("let statusCode = httpResponse.statusCode");
							$("DispatchQueue.main.async(execute: ", ()->{
								$if("statusCode >= 200 && statusCode <= 299", ()->{
									$if("let data = data", ()->{
										$("if DEBUG{print(NSString(data: data, encoding: String.Encoding.utf8.rawValue) ?? \"Unparsable Error\")}");
											$("self.getResponse(data)");
									});
								}).$else(()->{
									$if("let data = data", ()->{
										
										$("if DEBUG{print(NSString(data: data, encoding: String.Encoding.utf8.rawValue) ?? \"Unparsable Error\")}");
										$("self.onError(RequestError(tipoError: TipoError.SERVER_ERROR, mensaje: String(data: data, encoding: String.Encoding.utf8) ?? \"Unparsable Error\"))");
									}).$else("self.onError(RequestError(tipoError: TipoError.SERVER_ERROR, mensaje: \"Error en conexión al servidor\"))");
								});
							},")");
						}).$else(()->{
							$("DispatchQueue.main.async(execute: ", ()->{
								$("self.onError(RequestError(tipoError: TipoError.NO_INTERNET, mensaje: nil))");
							},")");
						});
					});
					$("deinit",()->{
						$if("let contUrl = contentURL",()->{
							$("DispatchQueue.global(qos: .utility).async { [contentURL = self.contentURL] in");
							$("	try? FileManager.default.removeItem(at: contUrl)");
							$("}");
						});
					});
					for(Class<?> clase : new Class<?>[] {String.class, JSONObject.class, JSONArray.class}) {
						IJType tipo = new JType(null, clase);
						$("public class "+tipo.getSimpleName()+"Resp : Abs"+id+"Manager",()->{
							String onResponseType = clase == String.class ? "(String)->()" : "("+$($convert(tipo))+")->()"; 
							$("var onResponse : "+onResponseType+"");
							$("init(url : String, onResponse : @escaping "+onResponseType+", onError : @escaping (RequestError)->())", () -> {
								$("self.onResponse = onResponse");
								$("super.init(url : url, onError : onError)");
							});
							$("override func getResponse(_ data : Data)",()->{
								if (clase == String.class) {
									$("self.onResponse(String(data: data, encoding: String.Encoding.utf8)  ?? \"\")");
								} else{
									$("do",()->{
										if (clase == JSONArray.class)
											$("let result = try JSONSerialization.jsonObject(with: data, options: []) as! [[String: AnyObject]]");
										else
											$("let result = try JSONSerialization.jsonObject(with: data, options: []) as! [String: AnyObject]");
										
										if (config.SUCCESS_TYPE() != null) {
											IJType successType = getSuccessType(config.SUCCESS_TYPE());
											if (clase == JSONArray.class)
												$("let success = "+config.SUCCESS_DAFAULT_VALUE());
											else
												$("let success = result[\""+config.SUCCESS_NAME()+"\"] as? " + $($convert(successType)) + " ?? "+config.SUCCESS_DAFAULT_VALUE());
										}
										$if(config.SUCCESS_CONDITION(), ()->{
											$("self.onResponse(result)");
											$("return;");
										});
										if (clase != JSONArray.class) {
											if(config.ERROR_CONDITION() != null)
												$if(config.ERROR_CONDITION(), ()->{
													if (clase == JSONArray.class)
														$("self.onError(RequestError(tipoError: TipoError.SERVER_ERROR, mensaje: \"Server error\"))");
													else
														$("self.onError(RequestError(codigo: (result[\"code\"] as? Int) ?? 0, mensaje: result[\""+config.ERROR_MESSAGE_NAME()+"\"] as? String))");
													$("return;");
												});
											if(config.UNATHORIZED_CONDITION() != null)
												$if(config.UNATHORIZED_CONDITION(), ()->{
													if (clase == JSONArray.class)
														$("self.onError(RequestError(tipoError: TipoError.UNAUTHORIZED, mensaje: \"Non authorized\"))");
													else
														$("self.onError(RequestError(tipoError: TipoError.UNAUTHORIZED, mensaje: result[\""+config.ERROR_MESSAGE_NAME()+"\"] as? String))");
													$("return;");
												});
											if(config.SUCCESS_TYPE() != null) {
												if (clase == JSONArray.class)
													$("self.onError(RequestError(tipoError: TipoError.SERVER_ERROR, mensaje: \"Server error\"))");
												else
													$("self.onError(RequestError(tipoError: TipoError.SERVER_ERROR, mensaje: result[\""+config.ERROR_MESSAGE_NAME()+"\"] as? String))");;
												$("return;");
											}
										}
									});
									$("catch",()->{
										$("self.onError(RequestError(tipoError: TipoError.FAIL, mensaje: \"\\(error)\"))");
									});
																
								}
							});
						});
					}
					$("func getResponse(_ resp : Data)",()->{
					});
					$("func authorization(_ authorization : String) -> Abs"+id+"Manager",()->{
						$("self.headers[\"Authorization\"] = authorization");
						$("return self");
					});
					$("func header(_ key : String, _ value : String) -> Abs"+id+"Manager",()->{
						$("self.headers[key] = value");
						$("return self");
					});
					Arrays.stream(Method.values()).filter(tipo->tipo != Method.DEFAULT).forEach(f->{
						if(f.isPostLike())
							$("func do"+StringUtils.capitalize(f.name().toLowerCase())+"(makeBody : @escaping ("+$($convert(GlobalTypes.Java.PrintWriter))+", Abs"+id+"Manager) -> ())",()->{
								$("self.makeBody = makeBody");
								$("self.type = ."+f.name());
								$("DispatchQueue.main.async",()->{
									$("self.doRequest()");									
								});
							});
						else {
							$("func do"+StringUtils.capitalize(f.name().toLowerCase())+"()",()->{
								$("self.type = ."+f.name());
								$("self.doRequest()");
							});
						}
					});
				});
			}};
			exportFile(code, netPackage.replace(".", File.separator) + File.separator + "Abs"+id+"Manager.swift");
		}
	}
	private void generarCodigoMobile() throws Exception {
		for(final Map.Entry<String, Set<IWServiceEndpoint>> entry : endpoints.entrySet()){
			String name = "Manager" + StringUtils.capitalize(StringUtils.camelizarSoft(entry.getKey().contains("/") ? entry.getKey().substring(entry.getKey().lastIndexOf('/') + 1) : entry.getKey()));
			String paquete = paqueteControllers;
			if(entry.getKey().contains("/"))
				paquete+= "."+entry.getKey().substring(0, entry.getKey().lastIndexOf('/')).replace("/", ".");
			final SwiftCode cliente = new SwiftCode(){{
					$("import Foundation");
					$("import " + jCrystalPackage);
					
					$("public class " + name, ()->{
						for (final IWServiceEndpoint endpoint : entry.getValue()) {
							JCrystalWebService m = (JCrystalWebService)endpoint;
							IInternalConfig internalConfig = m.exportClientConfig(descriptor);
							$("/**");
							$("* " + m.getPath(descriptor));
							$("**/");
							final Tupla2<List<JVariable>, Map<HttpType, List<JVariable>>> clientParams = getParamsWebService(m);
							final List<JVariable> parametros = clientParams.v0;
							boolean addOnError = true;
							if(m.unwrappedMethod.isVoid)
								parametros.add(P(GlobalTypes.jCrystal.VoidSuccessListener, "onSuccess"));
							else if(m.getReturnType().is(String.class))
								parametros.add(P(GlobalTypes.jCrystal.OnSuccessListener(GlobalTypes.STRING), "onSuccess"));
							else if (SUPPORTED_NATIVE_TYPES.contains(m.getReturnType()) 
									|| m.getReturnType().isAnyAnnotationPresent(jEntity.class, jSerializable.class) || m.getReturnType().isIterable()
									|| m.getReturnType().isArray()){
								parametros.add(P(GlobalTypes.jCrystal.OnSuccessListener($convert(m.getReturnType())), "onSuccess"));
							}else if(m.getReturnType().isTupla()){
								final List<IJType> tipos = new ArrayList<>();
								for(IJType p : m.getReturnType().getInnerTypes()) {
									if(p.getInnerTypes().isEmpty()) {
										tipos.add($convert(p));
									}else{
										if(p.isIterable()){
											IJType subClaseTipo = p.getInnerTypes().get(0);
											tipos.add($convert(subClaseTipo).createListType());
										}else
											throw new NullPointerException("Error, tipos incompatibles. No se puede tener un metodo con retorno " + p);
									}
								}
								parametros.add(P(GlobalTypes.jCrystal.OnSuccessListener(tipos), "onSuccess"));
							}else if(m.getReturnType().is(jcrystal.server.FileDownloadDescriptor.class)) {
								addOnError = false;
							}else
								throw new NullPointerException(m.getReturnType().toString());
							
							if(addOnError)
								parametros.add(P(GlobalTypes.jCrystal.ErrorListener, "onError"));
							String methodName = m.name().replace("$", "");
							if(m.getReturnType().is(jcrystal.server.FileDownloadDescriptor.class)) {
								$("public static func " + methodName + "(" + parametros.stream().map(this::$V).collect(Collectors.joining(", ")) + ") -> String", ()->{
									String ruta = m.getPath(descriptor);
									for(final JCrystalWebServiceParam param : m.parametros)
										if(param.tipoRuta == HttpType.PATH) {
											if(param.p.type().is(String.class))
												ruta = ruta.replace("$"+param.nombre, "\\("+param.nombre+".addingPercentEncoding(withAllowedCharacters: .urlHostAllowed)!)");
											else
												ruta = ruta.replace("$"+param.nombre, "\\("+param.nombre+")");
										}
									
									processGetParams(this, clientParams.v1.get(HttpType.GET));
									String path = "\""+ruta+"\"";
									if(!clientParams.v1.get(HttpType.GET).isEmpty())
										path += " + (params ?? \"\")";
									$("return Abs"+internalConfig.getDefId()+"Manager.BASE_URL + "+path);
								});
							}else {
								$("public static func " + methodName + "(" + parametros.stream().map(this::$V).collect(Collectors.joining(", ")) + ")", ()->{
									String ruta = m.getPath(descriptor);
									for(final JCrystalWebServiceParam param : m.parametros)
										if(param.tipoRuta == HttpType.PATH) {
											if(param.p.type().is(String.class))
												ruta = ruta.replace("$"+param.nombre, "\\("+param.nombre+".addingPercentEncoding(withAllowedCharacters: .urlHostAllowed)!)");
											else
												ruta = ruta.replace("$"+param.nombre, "\\("+param.nombre+")");
										}
									
									IInternalConfig methodConfig = m.exportClientConfig(descriptor);
									final boolean nativeEmbeddedResponse = internalConfig.embeddedResponse() && SUPPORTED_NATIVE_TYPES.contains(m.getReturnType());
									String helperResponseSimpleType = (nativeEmbeddedResponse ? "String" : m.isJsonArrayResponse(methodConfig) ? "JSONArray" : "JSONObject");
									
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
									if(m.isMultipart())
										token += ".doFormData()";
									if(m.tipoRuta.isGetLike())
										token += ".do"+StringUtils.capitalize(m.tipoRuta.name().toLowerCase())+"()";
									
									processGetParams(this, clientParams.v1.get(HttpType.GET));
									String path = "\""+ruta+"\"";
									if(!clientParams.v1.get(HttpType.GET).isEmpty())
										path += " + (params ?? \"\")";
									$L("Abs"+internalConfig.getDefId()+"Manager."+helperResponseSimpleType+"Resp(url: "+path+", onResponse: ", new AbsCodeBlock.Lambda("result") {
										@Override public void run() {
											if (m.unwrappedMethod.isVoid)
												$("onSuccess()");
											else if(m.getReturnType().is(String.class))
												$("onSuccess(result[\"r\"] as! String)");
											else {
												if (m.getReturnType().isIterable()) { 
													final IJType tipoParamero = m.getReturnType().getInnerTypes().get(0);
													if(tipoParamero.is(String.class))
														$("onSuccess(result[\"r\"] as! [String])");
													else if(tipoParamero.isPrimitiveObjectType()){
														$("onSuccess(result[\"r\"] as! ["+$(tipoParamero)+"])");
													}else if(tipoParamero.isAnnotationPresent(jEntity.class)){
														$("let _array = result[\"r\"] as! [[String : AnyObject]]");
														String tipo = $($convert(tipoParamero).createListType(), false);
														$("var _lista = " + tipo + "()");
														$("for pos in 0 ..< _array.count", ()->{
															$("_lista.append(Serializer" + context.data.entidades.targetEntity(tipoParamero).getSimpleName() + ".fromJson" + m.getJsonLevel(tipoParamero).baseName() + "(_array[pos]))");
														});
														$("onSuccess(_lista)");
													}else if (tipoParamero.isAnnotationPresent(jSerializable.class)){
														requiredClasses.add(tipoParamero);
														if(methodConfig.embeddedResponse()){
															$("let _array = result");
														}else
															$("let _array = result[\"r\"] as! [[String : AnyObject]]");
														String tipo = $($convert(tipoParamero).createListType(), false);
														$("var _lista = " + tipo + "()");
														$("for pos in 0 ..< _array.count", ()->{
															$("_lista.append(Serializer" + tipoParamero.getSimpleName() + ".fromJson(_array[pos]))");
														});
														$("onSuccess(_lista)");
													}else throw new NullPointerException("Unsupported Type : "+m.getReturnType());
												}
												else if (m.getReturnType().isAnnotationPresent(jEntity.class)) {
													if(m.getReturnType().isAnnotationPresent(LoginResultClass.class)){
														$("let token = Serializer"+ context.data.entidades.targetEntity(m.getReturnType()).getSimpleName() + ".fromJson" + m.getJsonLevel(m.getReturnType()).baseName() + "(result[\"r\"] as! [String : AnyObject])");
														$("token.storeToken()");
														$("onSuccess(token)");
													}else
														$("onSuccess(Serializer" + context.data.entidades.targetEntity(m.getReturnType()).getSimpleName() + ".fromJson" + m.getJsonLevel(m.getReturnType()).baseName() + "(result[\"r\"] as! [String : AnyObject]))");
												}else if (m.getReturnType().isAnnotationPresent(jSerializable.class)) {
													requiredClasses.add(m.getReturnType());
													if(methodConfig.embeddedResponse())
														$("onSuccess(Serializer" + m.getReturnType().getSimpleName() + ".fromJson(result))");
													else if(m.getReturnType().isAnnotationPresent(LoginResultClass.class)){
														$("onSuccess(Serializer" + m.getReturnType().getSimpleName() + ".fromJson(result[\"r\"] as! [String : AnyObject]).storeToken())");
													}else
														$("onSuccess(Serializer" + m.getReturnType().getSimpleName() + ".fromJson(result[\"r\"] as! [String : AnyObject]))");
												}else if(m.getReturnType().isTupla()){
													final List<IJType> tipos = m.getReturnType().getInnerTypes();
													if (methodConfig.embeddedResponse())
														$("let _array = result");
													else
														$("let _array = result[\"r\"] as! [AnyObject]");
													int e = 0;
													for(IJType p : tipos){
														if(p.getInnerTypes().isEmpty()) {
															if (p.isAnnotationPresent(jEntity.class)) {
																if(p.isAnnotationPresent(LoginResultClass.class)){
																	$("let val_"+e+" = _array[" + e + "] is NSNull ? nil : Serializer"+context.data.entidades.targetEntity(p).getSimpleName() + ".fromJson" + m.getJsonLevel(p).baseName() + "(_array[" + e + "] as! [String : AnyObject]).storeToken()");
																}else
																	$("let val_"+e+" = _array[" + e + "] is NSNull ? nil : Serializer"+context.data.entidades.targetEntity(p).getSimpleName() + ".fromJson" + m.getJsonLevel(p).baseName() + "(_array[" + e + "] as! [String : AnyObject])");
															}else if (p.isAnnotationPresent(jSerializable.class)) {
																requiredClasses.add(p);
																if(p.isAnnotationPresent(LoginResultClass.class)){
																	$("let val_"+e+" = _array[" + e + "] is NSNull ? nil : Serializer"+p.getSimpleName() + ".fromJson(_array["+e+"] as! [String : AnyObject]).storeToken()");
																}else
																	$("let val_"+e+" = _array[" + e + "] is NSNull ? nil : Serializer"+p.getSimpleName() + ".fromJson(_array["+e+"] as! [String : AnyObject])");
															}else if(p.is(String.class) || p.isPrimitiveObjectType())
																$("let val_"+e+" = _array[" + e + "] is NSNull ? nil : _array[" + e + "] as? "+$($convert(p), false));
															else throw new NullPointerException("Unssuported type "+p);
														}else{
															if(p.isIterable()){
																final IJType pClass = p.getInnerTypes().get(0);
																if (pClass.isAnnotationPresent(jEntity.class)) {
																	$("let val_"+e+" = _array[" + e + "] is NSNull ? nil : Serializer"+context.data.entidades.targetEntity(pClass).getSimpleName() + ".listFromJson" + m.getJsonLevel(pClass).baseName() + "(_array["+e+"] as! [[String : AnyObject]])");
																}else if (pClass.isAnnotationPresent(jSerializable.class)) {
																	requiredClasses.add(pClass);
																	$("let val_"+e+" = _array[" + e + "] is NSNull ? nil : Serializer"+pClass.getSimpleName() + ".listFromJson(_array["+e+"] as! [[String : AnyObject]])");
																}else throw new NullPointerException("Unssuported type "+p);
															}else
																throw new NullPointerException("Error, tipos incompatibles. No se puede tener un metodo con retorno " + p);
														}
														e++;
													}
													$("onSuccess("+IntStream.range(0, tipos.size()).mapToObj(f->"val_"+f).collect(Collectors.joining(", "))+")");
												}else if(m.getReturnType().isPrimitive()) {
													if(methodConfig.embeddedResponse()) {
														$("onSuccess("+$($convert(m.getReturnType()))+"(result)!)");
													}else
														$("onSuccess(result[\"r\"] as! "+$($convert(m.getReturnType()))+")");
												}
											}
										}
									}, ", onError: onError)"+token);
									if(m.tipoRuta.isPostLike()) {
										final SwiftCode thisCode = this;
										if (!m.isMultipart()) {
											$L(".do"+StringUtils.capitalize(m.tipoRuta.name().toLowerCase())+"(makeBody : ", new Lambda("_pw, request") {
												@Override
												public void run() {
													if (m.tipoRuta.isPostLike()) {
														List<JCrystalWebServiceParam> bodyParams = m.parametros.stream().filter(param -> param.tipoRuta.isPostLike() && param.nombre.equals("body")).collect(Collectors.toList());
														if (bodyParams.size() > 1)
															throw new NullPointerException("Too many bodys");
														else if (bodyParams.isEmpty())
															context.utils.generadorToJson.fillBody(thisCode, m.parametros.stream().filter(param -> param.tipoRuta.isPostLike()).collect(Collectors.toList()));
														else {
															JCrystalWebServiceParam param = bodyParams.get(0);
															if (param.p.type().is(String.class)) {
																$("_pw.print(" + param.nombre + ")");
															} else if (param.p.type().isPrimitive()) {
																throw new NullPointerException("A " + param.p.type() + " cant be on body");
															} else if (param.p.type().isAnnotationPresent(Post.class)) {
																JsonLevel level = param.p.type().getAnnotation(Post.class).level();
																$(param.nombre + ".toJson" + camelizar(level.name()) + "(_pw);");
															} else if (param.p.type().isAnnotationPresent(jSerializable.class)) {
																$("Serializer"+param.p.type().getSimpleName()+ ".toJson(_pw, objeto : " + param.nombre +");");
															} else if (param.p.type().isSubclassOf(List.class)) {
																final IJType tipo = param.p.type().getInnerTypes().get(0);
																if (tipo.isAnnotationPresent(jSerializable.class)) {
																	$("Serializer"+tipo.getSimpleName() + ".toJson"+tipo.getSimpleName()+"(_pw, lista: " + param.nombre + ");");
																} else if (tipo.isAnnotationPresent(Post.class)) {
																	JClass superClase = context.data.entidades.get(tipo).clase;
																	JsonLevel level = tipo.getAnnotation(Post.class).level();
																	if (param.nombre.equals("body"))
																		$if(param.nombre + " != null", () -> {
																		$(paqueteEntidades + "." + superClase.getSimpleName() + ".toJsonArray" + superClase.getSimpleName() + "(_pw, " + param.nombre + ");");
																	});
																} else
																	throw new NullPointerException("Unssuported post type " + param.p.type().name()); // TODO: Si se postea una lista de Posts o Jsonifies
															} else
																throw new NullPointerException("Unssuported post type " + param.p.type().name());
														}
													}
												}
											}, ")");											
										}else {
											$L(".do"+StringUtils.capitalize(m.tipoRuta.name().toLowerCase())+"(makeBody : ", new Lambda("_pw, request") {
												@Override
												public void run() {
													for (JCrystalWebServiceParam param : m.parametros)if(param.tipoRuta.isPostLike()){
														if (param.p.type().isPrimitive()) {
															onMultipartBoundary(thisCode, param.nombre, ()->{
																$("_pw.print(\"\\("+param.nombre+")\\r\\n\");");
															});
														}else if(param.p.type().is("jcrystal.server.FileUploadDescriptor")) {
															$("_pw.print(\"-----------------------------\" + request.boundary+\"\\r\\n\")");
															$("_pw.print(\"Content-Disposition: form-data; name="+param.name()+"\\r\\n\")");
															$if("let mimeType = "+param.name()+".mimeType",()->{
																$("_pw.print(\"Content-Type: \\(mimeType)\\r\\n\");");
															});
															$("_pw.print(\"\\r\\n\");");
															$("let data_"+param.nombre+" = [UInt8]("+param.name()+".data)");
															$("_pw.write(data_" + param.nombre + ", maxLength: data_" + param.nombre + ".count)");
															$("_pw.print(\"\\r\\n\");");
														}else {
															$if("let _" + param.nombre + " = " + param.nombre, ()->{
																onMultipartBoundary(thisCode, param.nombre, ()->{
																	if (param.p.type().is(String.class)) {
																		$("_pw.print(\"\\(_" + param.nombre+".addingPercentEncoding(withAllowedCharacters: .urlHostAllowed)!)\\r\\n\")");
																	} else if (param.p.type().isPrimitiveObjectType()) {
																		$("_pw.print(\"\\(_" + param.nombre+")\\r\\n\");");
																	} else if (param.p.type().isJAnnotationPresent(CrystalDate.class)) {
																		$("_pw.print(\"\\(" + param.nombre+".format())\\r\\n\");");
																	} else
																		throw new NullPointerException("Unssuported form data post type " + param.p.type().name());
																});
															});
														}
													}
													$("_pw.print(\"-----------------------------\" + request.boundary + \"--\\r\\n\");");
												}
											}, ")");
										}
										
									}
								});
							}
						}
					});
			}};
			exportFile(cliente, paquete.replace(".", File.separator) + File.separator + name + ".swift");
		}
	}
	private void onMultipartBoundary(AbsCodeBlock code, String name, Runnable r) {
		code.$("_pw.appendString(\"-----------------------------\" + request.boundary+\"\\r\\n\")");
		code.$("_pw.appendString(\"Content-Disposition: form-data; name="+ name+"\\r\\n\")");
		code.$("_pw.appendString(\"\\r\\n\");");
		r.run();
		code.$("_pw.appendString(\"\\r\\n\");");
		
	}
	@Override
	protected <X extends AbsCodeBlock> void processGetParams(X METODO, List<JVariable> params) {
		if(params.isEmpty())
			return;
		METODO.new B() {{
			$("var params : String! = nil");
			for(final JVariable param : params){
				String var;
				if(param.type().nullable()) {
					var = "_"+param.name();
					$("if let _" + param.name() + " = " + param.name());
					METODO.incLevel();
				}else
					var = param.name();
				String expression;
				if(param.type().isPrimitive())
					expression = param.name()+"=\\("+param.name()+")\"";
				else if(param.type().is(String.class))
					expression = param.name()+".addingPercentEncoding(withAllowedCharacters: .urlHostAllowed)!";
				else if(param.type().isPrimitiveObjectType()){
					expression = param.name()+"=\\(_"+param.name()+")\"";
				}else if (param.type().isEnum()) {
					expression = param.name()+"=\\(_"+param.name()+".id)\"";
				}else if(param.type().getSimpleName().equals("JSONObject")){
					throw new NullPointerException();
				}else if(param.type().getSimpleName().equals("JSONArray")){
					throw new NullPointerException();
				}else if(param.type().isJAnnotationPresent(CrystalDate.class)){
					expression = param.name()+"=\\("+param.name()+".format())\"";
				}
				else throw new NullPointerException("Parametro no reconocido "+param.type().getSimpleName());
				$if("let _params = params", ()->{
					$("params = _params + \"&" + expression);
				}).$else(()->{
					$("params = \"?" + expression);
				});
			}
		}};
	}
	@Override
	protected <X extends AbsCodeBlock> void processPostParams(X METODO, ContentType contentType, List<JVariable> params) {
		throw new NullPointerException("processPostParams");
	}
	
	public void crearCodigoAlmacenamiento(String paquete, final JClass clase, final TreeSet<JsonLevel> levels){
		new SwiftCode(){{
			$("import Foundation");
			$("import " + jCrystalPackage);
			$("public class DB" + clase.getSimpleName(), ()->{
				$("public static func retrieve(_ key: String)-> " + clase.getSimpleName() + "!", ()->{
					$("return retrieve(nil, key)");
				});
				crearCodigoStore(this, clase, null);
				if(levels != null)
					for(JsonLevel level : levels)
				crearCodigoStore(this, clase, level);
				$("@discardableResult public static func appendToList(_ partKey: String!, _ key: String, _ value: "+clase.getSimpleName()+")-> Bool", ()->{
					$if("let outputstream = DBUtils.getOutputStream(partKey, \"L\" + key, append: true)", ()->{
						$("outputstream.open()");
						$("let coma = [UInt8](\",\".utf8)");
						$("outputstream.write(coma, maxLength: coma.count)");
						$("Serializer"+clase.getSimpleName()+".toJson(outputstream, objeto : value)");
						$("outputstream.close()");
						$("return true");
					});
					$("return false");
				});
				$("public static func retrieve(_ partKey: String!, _ key: String)-> " + clase.getSimpleName() + "!", ()->{
					$("return DBUtils.retrieve(partKey: partKey, key: key, converter: Serializer"+clase.getSimpleName()+".fromJson)");
				});
				$("public static func retrieveList<T>(_ key: String, _ creator: (_ json : [String: AnyObject]) -> T)-> [T]!{return DBUtils.retrieveList(nil, key, creator)}");
				$("public static func delete(_ partKey: String!, _ key: String)", ()->{
					$("DBUtils.delete(partKey : partKey, key : key)");
				});
				$("public static func deleteList(_ partKey: String!, _ key: String)", ()->{
					$("DBUtils.deleteList(partKey: partKey, key: key)");
				});
				$("public static func delete(_ key: String)", ()->{
					$("DBUtils.delete(partKey : nil, key : key)");
				});
			});
			exportFile(this, paquete.replace(".", File.separator) + File.separator + "DB" + clase.getSimpleName() + ".swift");
		}};
		
	}
	private static void crearCodigoStore(SwiftCode code, final JClass clase, final JsonLevel level){
		code.new B() {{
				String classname = clase.getSimpleName()+(level==null?"":level.baseName());
				String suffix = (level==null?"":level.baseName());
				$("@discardableResult public static func store"+suffix+"(_ key: String, _ value: "+classname+")-> Bool", ()->{
					if(level != null)
						$("return store(nil, key, value as! " + clase.getSimpleName() + ")");
					else
						$("return store(nil, key, value)");
				});
				$("@discardableResult public static func store"+suffix+"(_ key: String, _ values: ["+classname+"])-> Bool", ()->{
					$("return store"+suffix+"(nil, key, values)");
				});
				
				$("@discardableResult public static func retrieveList"+suffix+"(_ key: String)-> ["+classname+"]! {return DBUtils.retrieveList(nil, key, Serializer"+clase.getSimpleName()+".CREATOR)}");
				$("@discardableResult public static func retrieveList"+suffix+"(_ partKey: String!, _ key: String)-> ["+classname+"]! {return DBUtils.retrieveList(partKey, key, Serializer"+clase.getSimpleName()+".CREATOR)}");
				
				$("@discardableResult public static func deleteFromList"+suffix+"(_ partKey: String!, _ key: String, _ filter : ("+classname+") -> Bool) -> ["+classname+"]",()->{
					$("if let _list = DB"+clase.getSimpleName()+".retrieveList(partKey, key)",()->{
						$("let newList = _list.filter({!filter($0)})");
						$("DBUtils.store(partKey: partKey, key: key, values: newList, storer: {Serializer"+clase.getSimpleName()+".toJson($0, objeto: $1 as! " + clase.getSimpleName() + ")})");
						$("return newList");
					});
					$("return ["+classname+"]()");
				});
				$("@discardableResult public static func deleteFromList"+suffix+"(_ key: String, _ filter : ("+classname+") -> Bool)-> ["+classname+"]! {return DB" + clase.getSimpleName() + ".deleteFromList"+suffix+"(nil, key, filter)}");
				
				if(level == null)
					$("@discardableResult public static func store(_ partKey: String!, _ key: String, _ value: " + clase.getSimpleName() + ")->Bool", ()->{
						$("return DBUtils.store(partKey: partKey, key : key) { (outputstream) in");
						$("\tSerializer"+clase.getSimpleName()+".toJson(outputstream, objeto : value)");
						$("}");
					});
				else
					$("@discardableResult public static func store(_ partKey: String!, _ key: String , _ value: " + classname + ") -> Bool", ()->{
						$("return store(partKey, key, value as! "+clase.getSimpleName()+")");
					});
				$("@discardableResult public static func store"+suffix+"(_ partKey : String!, _ key: String, _ values: ["+classname+"]) -> Bool", ()->{
					if(level==null)
						$("return DBUtils.store(partKey : partKey, key : key, values : values, storer : {Serializer"+clase.getSimpleName()+".toJson($0, objeto: $1 as! " + clase.getSimpleName() + ")})");
					else
						$("return DBUtils.store(partKey : partKey, key : key, values : values, storer : {Serializer"+clase.getSimpleName()+".toJson($0, objeto: $1 as! " + clase.getSimpleName() + ")})");
						//$("Serializer"+clase.getSimpleName()+".toJson(outputstream, objeto : value as! "+clase.getSimpleName()+")");
				});
		}};
	}
}


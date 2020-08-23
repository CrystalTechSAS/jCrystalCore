package jcrystal.clients.typescript;

import static java.lang.reflect.Modifier.PUBLIC;
import static java.lang.reflect.Modifier.STATIC;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.appengine.api.datastore.GeoPt;

import jcrystal.annotations.internal.model.db.InternalEntityKey;
import jcrystal.clients.AbsClientGenerator;
import jcrystal.clients.ClientGeneratorDescriptor;
import jcrystal.clients.ClientId;
import jcrystal.configs.clients.Client;
import jcrystal.configs.clients.IInternalConfig;
import jcrystal.datetime.DateType;
import jcrystal.db.query.Page;
import jcrystal.entity.types.LongText;
import jcrystal.entity.types.security.Password;
import jcrystal.json.JsonLevel;
import jcrystal.lang.Language;
import jcrystal.manager.utils.FileWrapperResponse;
import jcrystal.model.server.db.EntityField;
import jcrystal.model.server.db.EntityClass;
import jcrystal.model.web.HttpType;
import jcrystal.model.web.IWServiceEndpoint;
import jcrystal.model.web.JCrystalMultipartWebService;
import jcrystal.model.web.JCrystalWebService;
import jcrystal.model.web.JCrystalWebServiceParam;
import jcrystal.reflection.MaskedEnum;
import jcrystal.reflection.annotations.CrystalDate;
import jcrystal.reflection.annotations.EntityKey;
import jcrystal.reflection.annotations.com.jSerializable;
import jcrystal.reflection.annotations.LoginResultClass;
import jcrystal.reflection.annotations.Post;
import jcrystal.reflection.annotations.jEntity;
import jcrystal.reflection.annotations.entities.Rel1to1;
import jcrystal.reflection.annotations.entities.RelMto1;
import jcrystal.reflection.annotations.ws.ContentType;
import jcrystal.reflection.annotations.ws.Method;
import jcrystal.results.Tupla2;
import jcrystal.server.FileDownloadDescriptor;
import jcrystal.server.FileUploadDescriptor;
import jcrystal.types.IJType;
import jcrystal.types.JClass;
import jcrystal.types.JEnum;
import jcrystal.types.JEnum.EnumValue;
import jcrystal.types.JType;
import jcrystal.types.JVariable;
import jcrystal.types.utils.GlobalTypes;
import jcrystal.types.vars.IAccessor;
import jcrystal.utils.StreamUtils;
import jcrystal.utils.StringSeparator;
import jcrystal.utils.StringUtils;
import jcrystal.utils.context.CodeGeneratorContext;
import jcrystal.utils.context.CodeGeneratorContext.ContextType;
import jcrystal.utils.langAndPlats.AbsCodeBlock;
import jcrystal.utils.langAndPlats.TypescriptCode;
import jcrystal.utils.langAndPlats.AbsCodeBlock.B;
import jcrystal.utils.langAndPlats.delegates.TypescriptCodeDelegator;

/**
* Created by AndreaC on 13/12/2016.
*/
public class WebClientTypescript extends AbsClientGenerator<Client>{
	
	
	protected static final String paquetePadre = "jcrystal";
	public static final String paqueteServicios = "jcrystal/services";
	protected static final String paqueteEntidades = "jcrystal/entities";
	protected static final String paqueteDates = "jcrystal/dates";
	protected static final String paqueteEnums = "jcrystal/enums";
	protected static final String paqueteResultados = "jcrystal/results";
	
	protected List<JClass> securityTokens = new ArrayList<>();
	
	public WebClientTypescript(ClientGeneratorDescriptor<Client> descriptor){
		super(descriptor);
		entityGenerator = new GeneradorEntidad(this);
		
	}
	@Override
	protected void setupEnviroment() throws Exception {
		TypeScriptTypeConverter converter = new TypeScriptTypeConverter(context);
		ContextType.WEB.init();
		CodeGeneratorContext.set(Language.TYPESCRIPT, converter, converter);
	}
	
	@Override public void generarCliente() throws Exception{
		generarCodigo();
		generarEntidades();
		generarResultados();
		generarEnums();
		generarDateTypes();
		generarNetworkUtils();
		
		addResource(WebClientTypescript.class.getResourceAsStream("JSONUtils.ts"), paquetePadre.replace(".", File.separator) + File.separator + "JSONUtils.ts");
		addResource(WebClientTypescript.class.getResourceAsStream("error.services.ts"), paqueteServicios.replace(".", File.separator) + File.separator + "error.services.ts");
	}
	private void generarNetworkUtils() {
		final String paquete = paqueteServicios;
		final TypescriptCode $ = new TypescriptCode(){{
			$("import { HttpClient,HttpHeaders } from '@angular/common/http';");
			$("import { Router } from '@angular/router';");
			$("import { ErrorService } from './error.services';");
			$("import { Injectable } from '@angular/core';");
			$("import Swal from 'sweetalert2';");
			securityTokens.forEach(tokenClass->{
				if(tokenClass.isAnnotationPresent(jEntity.class)) {
					$("import {"+tokenClass.getSimpleName()+"} from \"../"+paqueteEntidades.replaceAll(paquetePadre+"/", "")+"/"+tokenClass.getSimpleName()+"\";");
				}else $("import {"+tokenClass.getSimpleName()+"} from \"../"+paqueteResultados.replaceAll(paquetePadre+"/", "")+"/"+tokenClass.getSimpleName()+"\";");	
			});
			

			$("export class AbsBaseNetwork",()->{
				descriptor.configs.values().forEach(internalConfig -> {
					String ruta = internalConfig.BASE_URL(null);
					ruta = ruta.replace("\"\"", "");
					while(ruta.trim().startsWith("+"))
						ruta = ruta.substring(1);
					while(ruta.trim().endsWith("+"))
						ruta = ruta.substring(0, ruta.length() - 1);
					$("static BASE_SERVER_URL : string = \""+ ruta +"\";");
				});
			});
			$("@Injectable()");
			$("export class DefaultOnError",()->{
				$("constructor(private _r: Router, private _e:ErrorService)",()->{});
				$("onError = (error : RequestError):void =>",()->{
					$if("error.tipoError == TipoError.UNAUTHORIZED",()->{
						securityTokens.forEach(tokenClass->{
							$(tokenClass.getSimpleName()+".deleteSession();");
						});
						$("this._r.navigate(['/login']);");
					}).$else(()->{
						$("console.log(error);");
						$("this._e.announceError(error.mensaje);");
					});
				});
			});
			$("export interface BaseNetwork", ()->{
				$("http:HttpClient;");
				$("onError? : ((error : RequestError)=>void);");
			});
			$("export enum TipoError", ()->{
			    $("/**");
			    $(" * Application error, eg: InternalError");
			    $(" */");
			    $("ERROR = 1,");
			    $("/**");
			    $("* Server connection error, eg: wrong json formats, HTTP errors, etc");
			    $("*/");
			    $("SERVER_ERROR = 2,");
			    $("UNAUTHORIZED = 3,");
			    $("NO_INTERNET = 4,");
			    $("CORS = 5");
			});
			$("export class RequestError",()->{
				$("public tipoError : TipoError;");
				$("public mensaje : string;");
				$("public codigo : number;");

				$("constructor(codigo : number = -1, mensaje : string, tipoError : TipoError = TipoError.ERROR)",()->{
					$("this.tipoError = tipoError;");
					$("this.mensaje = mensaje;");
					$("this.codigo = -1;");
				});
			});
			$("export const defaultOnError = (error : RequestError)=>",()->{ 
				$if("error.tipoError == TipoError.UNAUTHORIZED",()->{
					securityTokens.forEach(tokenClass->{
						$(tokenClass.getSimpleName()+".deleteSession();");
					});
					$("window.location.href = '/login';");
				}).$else("error.tipoError == TipoError.SERVER_ERROR",()->{
					$("Swal.fire(\"Internal server error\");");
				}).$else(()->{
					$("Swal.fire(error.mensaje);");
				});
			});
			$("export const alertInfo = (error : string)=>{ Swal.fire(error); }");
		}};
		exportFile($, paquete.replace(".", File.separator) + File.separator + "BaseNetwork.ts");
	}
	private void generarDateTypes() {
		for (DateType t : DateType.values()) {
			String name = null;
			for(String h : t.name().toLowerCase().split("_"))
			name = name == null ? h.substring(0,1).toUpperCase() + h.substring(1) : (name + h.substring(0,1).toUpperCase() + h.substring(1));
			StringWriter sw = new StringWriter();
			try (PrintWriter pw = new PrintWriter(sw)) {
				pw.println("import moment from 'moment';");
				pw.println("export class Crystal" + name + "{");
				pw.println("    date: Date;");
				pw.println("    constructor(d: Date | string){");
				pw.println("        if(typeof d === \"string\"){");
				pw.println("            let resp = moment.utc(d,'"+t.format.replace("y", "Y").replace("d","D")+"', true)");
				pw.println("            if(!resp.isValid()){ throw new Error('Invalid Format'); }");
				pw.println("            else{ this.date = resp.toDate();}");
				pw.println("        }else{");
				pw.println("            this.date = d;");
				pw.println("        }");	
				pw.println("    }");
				pw.println("    format(format : string = null) : string {");
				pw.println("        return moment.utc(this.date).format(format || '" +t.format.replace("y", "Y").replace("d","D") + "');");
				pw.println("    }");
				pw.println("    formatClient(format : string = null) : string {");
				pw.println("        return moment(this.date).format(format || '" +t.userFormat.replace("y", "Y").replace("d","D") + "');");
				pw.println("    }");
				pw.println("}");
			}
			exportFile(sw.toString(), paqueteDates.replace(".", File.separator) + File.separator +"Crystal"+name + ".ts");
		}
	}
	
	private void generarEnums() throws Exception{
		for(final IJType claseEnum : requiredClasses) 
			if(claseEnum.isEnum()){
				JClass clase = claseEnum.tryResolve();
				if(clase == null)
					continue;
				new TypescriptCode(){{
					JEnum enm = clase.enumData;
					$("export class "+enm.name, () -> {
						enm.valores.forEach(o->{
							StringSeparator sp = new StringSeparator(", ");
							enm.propiedades.forEach((key, type)->{
								if(type.is(String.class))
									sp.add("'" + o.propiedades.get(key)+"'");
								else
									sp.add("" + o.propiedades.get(key)+"");
							});
							$("public static "+o.name+" = new "+enm.name+"("+sp+");");
						});
						
						StringSeparator sp = new StringSeparator(", ");
						enm.propiedades.forEach((key, type)->sp.add("public "+key+" : "+$($convert(type))));
						$("public constructor("+sp+"){}");
						
						IJType tipoId = enm.propiedades.get("id");
						$("static getFromId(id : " + $(tipoId == null ? GlobalTypes.STRING : tipoId) + ") : " + enm.name,()->{
							enm.valores.forEach(o->{
								Object id = o.propiedades.get("id");
								if(tipoId.is(String.class))
									$if("'"+id+"' == id", "return "+enm.name+"."+o.name);
								else
									$if(id+" == id", "return "+enm.name+"."+o.name);
							});
						});
						if(clase.isSubclassOf(MaskedEnum.class)) {
							$("static getFromMask(num : number) : number[]",()->{
								$("let arr : number[] = [];");
								enm.valores.forEach(o->{
									$if("(num & "+o.propiedades.get("id")+") !== 0" , ()->{
										$("arr.push("+claseEnum.getSimpleName()+"."+o.name+".id);");
									});
								});
								$("return arr;");
							});
							$("static maskArray(array:number[]): number", ()->{
								$("let mask = 0;");
								$("for(let modo of array)", ()->{
									$("mask |= modo;");
								});
								$("return mask;");
							});
						}
						$("static values = ["+enm.valores.stream().map(f->enm.name+"."+f.name).collect(Collectors.joining(", "))+"];");
						
					});
					exportFile(this, paqueteEnums.replace(".", File.separator) + File.separator + claseEnum.getSimpleName() + ".ts");
				}};
			}
	}
	private void generarCodigo()throws Exception{
		for(final Map.Entry<String, Set<IWServiceEndpoint>> entry : endpoints.entrySet()){
			String name = Utils.getManagerName(entry.getKey());
			final TypescriptCode cliente = new TypescriptCode(){{
				int numSlash =  entry.getKey().length() - entry.getKey().replace("/", "").length();
				String path = String.join("", Collections.nCopies(numSlash+1, "../"));
				
				$("import 'rxjs';");
				$("import {AppConfiguration} from \""+path+"../utils/app-configuration\";");
				$("import {AbsBaseNetwork, RequestError, defaultOnError, TipoError} from \""+path+"../"+paqueteServicios+"/BaseNetwork\";");
				for(final IWServiceEndpoint endPoint : entry.getValue())
					endPoint.gatherRequiredTypes(imports);
				$imports(numSlash + 1);
				$("export class " + name, ()->{
					for(final IWServiceEndpoint endPoint : entry.getValue()) {
						JCrystalWebService m = (JCrystalWebService)endPoint;
						IInternalConfig internalConfig = m.exportClientConfig(descriptor);
						final boolean nativeEmbeddedResponse = internalConfig.embeddedResponse() && SUPPORTED_NATIVE_TYPES.contains(m.getReturnType());
						final Tupla2<List<JVariable>, Map<HttpType, List<JVariable>>> clientParams = getParamsWebService(endPoint);
						$("/**");
						$("* " + endPoint.getPath(descriptor));
						$("**/");
						if(endPoint instanceof JCrystalMultipartWebService) {
							$("static "+endPoint.name()+" = new class {");
							incLevel();
							$("fd = new FormData();");
							for(JCrystalWebService service : ((JCrystalMultipartWebService)endPoint).getServices()) {
								final TypescriptCode interno = new TypescriptCode();
								final List<JVariable> parametros = clientParams.v0;
								IJType successType = getReturnCallbackType(this, service);
								parametros.add(new JVariable(successType, "onSuccess"));
								
								$("onSuccess"+StringUtils.capitalize(service.name())+" : "+successType+";");
								$(service.name() + "(" + parametros + ")", ()->{
									$append(interno);
									$("this.fd.append(\""+service.name()+"\", body);");
								});
							}
						}
						
						final List<JVariable> parametros = clientParams.v0;
						
						IJType successType = getReturnCallbackType(this, endPoint);
						if(successType!=null)
							parametros.add(new JVariable(successType, "onSuccess"));
						
						String methodName = endPoint instanceof JCrystalMultipartWebService ? "doRequest" : endPoint.name();
						parametros.add(new JVariable(GlobalTypes.jCrystal.ErrorListener, "onError", "defaultOnError"));
						$((endPoint.isMultipart()?"":"static ") + methodName + "(" + parametros.stream().map(this::$V).collect(Collectors.joining(", ")) + ")", ()->{
							$("let params:string = null;");
							if(endPoint.isMultipart())
								$("var fd = new FormData();");
							
							$("var ruta : string = AbsBaseNetwork.BASE_SERVER_URL + \""+ endPoint.getPath(descriptor) + "\";");
							$("if(AppConfiguration.DEBUG)console.log(\"" + endPoint.getPathType().name() + " \"+ruta);");
							processGetParams(this, clientParams.v1.get(HttpType.GET));
							if(m.isMultipart())
								$("var fd = new FormData();");
							processPostParams(this, m.contentTypes[0], clientParams.v1.get(HttpType.POST));
							if(m.isMultipart() && m.hasPostParam())
								$("fd.append(\"$body\", JSON.stringify(body));");
							if(m.tipoRuta.isPostLike() && m.getReturnType().is(FileDownloadDescriptor.class)) {
								$ifNull("onSuccess",()->{
									$("onSuccess = function(blob, filename)",()->{
										//var blob = new Blob([request.response], { type: 'application/pdf' });
								        $("var link = document.createElement('a');");
								        $("link.href = window.URL.createObjectURL(blob);");
								        $("link.download = filename;");
								        $("document.body.appendChild(link);");
		        						$("link.click();");
        								$("document.body.removeChild(link);");	
									},";");
								});
							}
							$ifNotNull("onSuccess",()->{
								$("let $xhr = new XMLHttpRequest();");
								$("$xhr.open('" + m.tipoRuta.name() + "', ruta);");
								processHeaderParams(this, clientParams.v1.get(HttpType.HEADER), m.tokenParam);
								if(m.getReturnType().is(FileDownloadDescriptor.class))
									$("$xhr.responseType= 'blob';");
								else
									$("$xhr.responseType= 'json';");
								if(m.isMultipart()) {
									$("$xhr.send(fd);");
									//$("processData: false,");
									//$("contentType: false,");
								}else if(m.tipoRuta.isPostLike()) {
									$("$xhr.setRequestHeader(\"Content-Type\", \"application/json;charset=UTF-8\");");
									$("$xhr.send(JSON.stringify(body));");
								}else {
									$("$xhr.send();");
								}
								$("$xhr.onerror = function()",()->{
									$("onError(new RequestError($xhr.status, $xhr.response, TipoError.SERVER_ERROR));");
								},";");
								$("$xhr.onload = function()",()->{
									$if("$xhr.status >= 200 && $xhr.status < 300",()->{
										$("var response = $xhr.response;");
										if(m.getReturnType().is(FileDownloadDescriptor.class)) {
											//TODO: Agregar el nombre de archivo al callback
											//$("onSuccess(response, $xhr.getResponseHeader('Content-Disposition').match(/filename[^;=\\n]*=((['\"]).*?\\2|[^;\\n]*)/)[1]);");
											$("onSuccess(response);");
										}else {
											if (internalConfig.SUCCESS_TYPE() != null)
												$("var success = response.success || " + internalConfig.SUCCESS_DAFAULT_VALUE() + ";");
											$if(internalConfig.SUCCESS_CONDITION(), () -> {
												if(nativeEmbeddedResponse) {
													$("onSuccess(response);");
													return;
												}
												if (m.unwrappedMethod.isVoid) {
													$("onSuccess();");
													$("return;");
												}else {
													putResponseProcessing(this, m);
												}
											});
											if (internalConfig.ERROR_CONDITION() != null)
												$if(internalConfig.ERROR_CONDITION(), () -> {
													$("onError(new RequestError(response.code, response."+internalConfig.ERROR_MESSAGE_NAME() + ", TipoError.ERROR));");
													$("return;");
												});
											if (internalConfig.UNATHORIZED_CONDITION() != null)
												$if(internalConfig.UNATHORIZED_CONDITION(), () -> {
													$("onError(new RequestError(response.code, response."+internalConfig.ERROR_MESSAGE_NAME() + ", TipoError.UNAUTHORIZED));");
													$("return;");
												});
											if (internalConfig.SUCCESS_TYPE() != null) {
												$("onError(new RequestError(response.code, response."+internalConfig.ERROR_MESSAGE_NAME() + ", TipoError.SERVER_ERROR));");
											}
										}
									});
									$else(()->{
										$("onError(new RequestError($xhr.status, \"Server error : \" + $xhr.status, TipoError.ERROR));");
									});
								},";");
								
							});
						});
						
						if(endPoint instanceof JCrystalMultipartWebService) {
							$("}");
							decLevel();
						}
					}
				});
				if(!imports.isEmpty())
					throw new NullPointerException("Hay un import que no se puso en el header");
			}};
			
			String paquete = paqueteServicios;
			if(entry.getKey().contains("/"))
				paquete+= "."+entry.getKey().substring(0, entry.getKey().lastIndexOf('/')).replace("/", ".");
			exportFile(cliente, paquete.replace(".", File.separator) + File.separator + name + ".ts");
		}
	}
	//TODO: Es una copia del de jquery!!! abstraer
	protected <X extends AbsCodeBlock> void processHeaderParams(X METODO, List<JVariable> params, JCrystalWebServiceParam token) {
		StringSeparator headerParams = new StringSeparator(", ");
		for(JVariable param : params) {
			if(param.type().is(String.class)) {
				METODO.$ifNotNull(param.value, ()->{
					METODO.$("$xhr.setRequestHeader(\"" + param.name() + "\", " + param.value + ");");
				});
			}
		}
		if(token != null)
			METODO.$if(token.type().getSimpleName()+".getTokenId() != null",()->{
				METODO.$("$xhr.setRequestHeader(\"Authorization\", "+token.type().getSimpleName()+".getTokenId());");							
			});
		if(!headerParams.isEmpty())
			METODO.$("headers: {"+headerParams+"},");
		METODO.new B() {{
			
		}};
	}
	private void putResponseProcessing(AbsCodeBlock code, IWServiceEndpoint m) {
		code.new B() {{
			if(m.getReturnType().is(Void.TYPE))
				$("onSuccess();");
			else if(m.getReturnType().is(String.class))
				$("onSuccess(response.r as string)");
			else if (m.getReturnType().isIterable()) { 
				final IJType tipoParametro = m.getReturnType().getInnerTypes().get(0);
				$("let _array : any[] = response.r;");
				if(tipoParametro.isAnnotationPresent(jEntity.class)){
					$("var _lista : "+$($convert(tipoParametro))+"[]=[];");
					$("for( let e of _array)", ()->{
						$("_lista.push(" + context.data.entidades.targetEntity(tipoParametro).getSimpleName() + ".fromJson" + m.getJsonLevel(tipoParametro).baseName() + "(e));");
					});
				}else{
					$("var _lista : "+$($convert(tipoParametro))+"[]=[];");
					if (tipoParametro.isAnnotationPresent(jSerializable.class)){
						requiredClasses.add(tipoParametro);
						$("for( let e of _array)", ()->{
							$("_lista.push(" + tipoParametro.getSimpleName() + ".fromJson(e));");
						});
					}else if(tipoParametro.is(String.class) || tipoParametro.isPrimitiveObjectType()) {
						$("for( let e of _array)", ()->{
							$("_lista.push(e as "+$($convert(tipoParametro))+");");
						});
					}else
						 throw new NullPointerException("Unssuported type " + $(m.getReturnType()));
				}
				$("onSuccess(_lista)");
			}else if (m.getReturnType().isAnnotationPresent(jEntity.class)) {
				if (m.getReturnType().isAnnotationPresent(LoginResultClass.class))
					$("onSuccess("+ context.data.entidades.targetEntity(m.getReturnType()).getSimpleName()+".setSession("+ context.data.entidades.targetEntity(m.getReturnType()).getSimpleName()+".fromJson"+ m.getJsonLevel(m.getReturnType()).baseName() +"(response.r as any)));");
				else
					$("onSuccess("+ context.data.entidades.targetEntity(m.getReturnType()).getSimpleName()+".fromJson"+ m.getJsonLevel(m.getReturnType()).baseName() +"(response.r as any));");
			} else if (m.getReturnType().isAnnotationPresent(jSerializable.class)) {
				requiredClasses.add(m.getReturnType());
				if (m.getReturnType().isAnnotationPresent(LoginResultClass.class))
					$("onSuccess("+ m.getReturnType().getSimpleName()+".setSession("+m.getReturnType().getSimpleName()+".fromJson(response.r as any)));");
				else
					$("onSuccess("+ m.getReturnType().getSimpleName() + ".fromJson(response.r as any));");
			} else if (m.getReturnType().getSimpleName().startsWith("Tupla")) {
				final List<IJType> tipos = m.getReturnType().getInnerTypes();
				StringSeparator sp = new StringSeparator(',');
				int e = 0;
				for (IJType p : tipos) {
					if (p.getInnerTypes().isEmpty()) {
						if (p.isAnnotationPresent(jEntity.class)) {
							if (p.isAnnotationPresent(LoginResultClass.class))
								sp.add(context.data.entidades.targetEntity(p).getSimpleName()+".setSession("+ context.data.entidades.targetEntity(p).getSimpleName() + ".fromJson" + m.getJsonLevel(p).baseName() + "(response.r[" + e + "] as any))");
							else
								sp.add(context.data.entidades.targetEntity(p).getSimpleName() + ".fromJson" + m.getJsonLevel(p).baseName() + "(response.r[" + e + "] as any)");
						} else if (p.isAnnotationPresent(jSerializable.class)) {
							requiredClasses.add(p);
							if (p.isAnnotationPresent(LoginResultClass.class))
								sp.add(p.getSimpleName()+".setSession("+ $($convert(p))+".fromJson(response.r[" + e + "] as any))");
							else
								sp.add($($convert(p))+".fromJson(response.r[" + e + "] as any)");
						} else if(p.isPrimitiveObjectType() || p.is(String.class))
							sp.add("response.r[" + e + "] as "+$($convert(p)));
						else if(p.is(GeoPt.class))
							sp.add("[response.r[" + e + "][0] as number, response.r[" + e + "][1] as number]");
						else throw new NullPointerException("Unssuported type " + p);
					} else{
						if(p.isSubclassOf(FileWrapperResponse.class))
							p = p.getInnerTypes().get(0);
						if (p.isSubclassOf(List.class)) {
							IJType pClass = p.getInnerTypes().get(0);
							if (pClass.isAnnotationPresent(jEntity.class)) {
								sp.add(context.data.entidades.targetEntity(pClass).getSimpleName()+".listFromJson"+ m.getJsonLevel(pClass).baseName()+"(response.r[" + e + "] as any[])");
							} else if (pClass.isAnnotationPresent(jSerializable.class)) {
								requiredClasses.add(pClass);
								sp.add($($convert(pClass)) + ".listFromJson(response.r[" + e + "] as any[])");
							} else throw new NullPointerException("Unssuported type " + p);
						} else
							throw new NullPointerException("Error, tipos incompatibles. No se puede tener un metodo con retorno " + p);
					}
					e++;
				}
				$("onSuccess(" + sp + ");");
			}else if(m.getReturnType().isPrimitive())
				$("onSuccess(response.r as "+$($convert(m.getReturnType()))+")");
			else
				$("onSuccess(response.r);");
		}};
	}
	
	protected void crearcodigoTokenSeguridad(TypescriptCode code, JClass clase, List<JVariable> campos) {
		securityTokens.add(clase);
		code.new B() {{
			$("public static session:"+clase.getSimpleName()+";");
			
			$("public static setSession(session:"+clase.getSimpleName()+") : "+clase.getSimpleName(), ()->{
				$("window.sessionStorage.setItem('"+clase.getSimpleName()+"', JSON.stringify(",()->{
					for(final JVariable f : campos)
						$(f.name()+": session."+f.name()+",");
				},"));");
				$(clase.getSimpleName()+".session=session;");
				$("return session;");
			});
			$("public static getTokenId():string ", ()->{
				$if(clase.getSimpleName()+".session", "return "+clase.getSimpleName()+".session.token;");
				$("else{ "+clase.getSimpleName()+".getSession(); if("+clase.getSimpleName()+".session){return "+clase.getSimpleName()+".session.token;}else{return null;}}");
			});
			$("public static deleteSession()", ()->{
				$(clase.getSimpleName()+".session=null;");
				$("window.sessionStorage.removeItem('"+clase.getSimpleName()+"');");
			});
			
			$("public static getSession():"+clase.getSimpleName(), ()->{
				$if("window.sessionStorage.getItem('"+clase.getSimpleName()+"') != null", ()->{
					$(clase.getSimpleName()+".session = "+clase.getSimpleName()+".fromJsonNormal(JSON.parse(window.sessionStorage.getItem('"+clase.getSimpleName()+"')));");
					$("return "+clase.getSimpleName()+".session;");
				});
				$(clase.getSimpleName()+".session = null;");
				$("return null;");
			});
			
			$("public static isAuthenticated():boolean", ()->{
				$if(clase.getSimpleName()+".session", "return true;");
				$("return false;");
			});
			
			if(context.data.rolClass != null){
				for(final EnumValue value : context.data.rolClass.enumData.valores) {
					String name = "Rol" + StringUtils.camelizar(value.name);
					$("public static is"+name+"():boolean", ()->{
						try {
							$("return "+clase.getSimpleName()+".isAuthenticated() && ("+clase.getSimpleName()+".session.rol & " + value.propiedades.get("id") + ")!==0;");
						} catch (Exception e) {
							$("return false;");
						}
					});
				}
			}
			$("private static getCookie(name: string)", ()->{
				$("let ca: Array<string> = document.cookie.split(';');");
				$("let caLen: number = ca.length;");
				$("let cookieName = name + \"=\";");
				$("let c: string;");
				$("for (let i: number = 0; i < caLen; i += 1)",()->{
					$("c = ca[i].replace(/^\\s\\+/g, \"\");");
					$("c = c.trim();");
					$if("c.indexOf(cookieName) === 0",()->{
						$("return c.substring(cookieName.length, c.length);");
					});
				});
				
			});
			$("private static deleteCookie(name:string)", ()->{
				$(clase.getSimpleName()+".setCookie(name,'',-1);");
			});
			$("private static setCookie(name: string, value: string, expireDays: number, path: string = \"\")", ()->{
				$(" let d:Date = new Date();");
				$("d.setTime(d.getTime() + expireDays * 24 * 60 * 60 * 1000);");
				$("let expires:string = \"expires=\" + d.toUTCString();");
				$("document.cookie = name + \"=\" + value + \"; \" + expires + (path.length > 0 ? \"; path=\" + path : \"\");\n");
			});
		}};
	}
	
	
	public void generarResultados()throws Exception{
		requiredClasses.stream().filter(f->f.isAnnotationPresent(jSerializable.class)).forEach(clase->{
			ResultGenerator.generate((JClass)clase);
		});
	}
	@Override
	protected <X extends AbsCodeBlock> void processGetParams(X METODO, List<JVariable> params) {
		METODO.new B() {{
			for(JVariable param : params){
				if(param.type().isPrimitive()){
					$("params = (params===null?\"?\":(params + \"&\")) + \"" + param.name()+"=\" + String("+ param.name()+");");
				}else if(param.type().isEnum()){
					$ifNotNull(param.name(), ()->{
						$("params = (params===null?\"?\":(params + \"&\")) + \"" + param.name()+"=\" + String("+ param.name()+".id);");
					});
				}else if(param.type().is(String.class)){
					$ifNotNull(param.name(), ()->{
						$("params = (params===null?\"?\":(params + \"&\")) + \""+param.name()+"=\" + encodeURIComponent("+param.name()+");");
					});
				}else if(param.type().isPrimitiveObjectType()){
					$ifNotNull(param.name(), ()->{
						$("params = (params===null?\"?\":(params + \"&\")) + \""+param.name()+"=\" + String("+param.name()+");");
					});
				}else if(param.type().isJAnnotationPresent(CrystalDate.class)){
					$if(param.name(),()->{
						$("params = (params===null?\"?\":(params + \"&\")) + \""+param.name()+"=\" + "+param.name()+".format();");
					});
				}else 
					throw new NullPointerException("Parametro no reconocido "+param.type().getSimpleName());					
			}
		}};
	}
	@Override
	protected <X extends AbsCodeBlock> void processPostParams(X METODO, ContentType contentType, List<JVariable> params) {
		if(params.isEmpty())
			return;
		METODO.new B() {{
			if(contentType == ContentType.JSON)
				$("let body: any = {};");
			for(JVariable param : params){
				if(param.type().is(FileUploadDescriptor.class)) {
					$("fd.append(\""+param.name()+"\", "+param.name()+");");
				}else if (param.type().is(String.class, LongText.class)) {
					$if(param.name(), ()->{
						$("body."+param.name()+"= "+param.name()+";");
					});
				} else if (param.type().isPrimitive()) {
					$("body."+param.name()+"= "+param.name()+";");
				}else if(param.type().is(Integer.class, Long.class, Double.class)){
					$ifNotNull(param.name(), ()->{
						$("body."+param.name()+"= "+param.name()+";");
					});
				}else if(param.type().isAnnotationPresent(Post.class)){
					JClass superClase  = context.data.entidades.get(param.type()).clase;
					JsonLevel level = param.type().getAnnotation(Post.class).level();
					$("body."+param.name()+"= ("+param.name()+" as "+superClase.getSimpleName()+").toJson"+level.baseName()+"();");
				}else if(param.type().isJAnnotationPresent(InternalEntityKey.class)) {
					$("body." + param.name() + " = " + param.name()+";");
				}else if(param.type().isAnnotationPresent(jEntity.class)) {
					EntityClass entidad = context.data.entidades.get(param.type());
					String nombre = "id"+StringUtils.capitalize(param.name());
					if(entidad.hasAccountFields) {
						entidad.iterateKeysAndProperties().filter(f->f.isAccountField()||f.type().is(Password.class)).forEach(campo->{
							$if(campo.fieldName(), ()->{
								$("body."+campo.fieldName()+"= "+campo.fieldName()+";");
							});
						});
					}else {
						$("body."+nombre+" = "+nombre+";");
					}
				}else if (param.type().isAnnotationPresent(jSerializable.class)) {
					$("body."+param.name()+"= "+param.name()+";");
				}else if (param.type().isIterable() || param.type().isArray()) {
					final IJType tipo = param.type().getInnerTypes().get(0);
					if(tipo.isPrimitiveObjectType() || tipo.isPrimitive()) {
						$("body."+param.name()+"= "+param.name()+";");
					}else if(tipo.isEnum()){
						$ifNotNull(param.name(), ()->{
							$("body."+param.name()+"= "+param.name()+".map(item => String(item.id));");
						});
					}
					else if (tipo.isAnnotationPresent(jSerializable.class))
						throw new NullPointerException();
					else if (tipo.isAnnotationPresent(Post.class)) {
						JClass superClase  = context.data.entidades.get(tipo).clase;
						JsonLevel level = tipo.getAnnotation(Post.class).level();
						$("body."+param.name()+"= "+superClase.getSimpleName()+".toJson"+level.baseName()+superClase.getSimpleName()+"("+param.name()+");");
					}else if(tipo.isAnnotationPresent(jEntity.class)) {
						$("body."+param.name()+"= [];");
						EntityClass entidad = context.data.entidades.get(tipo);
						if(entidad.key.isSimple()) {
							$("body."+param.name()+"= "+param.name()+";");
						}else {
							throw new NullPointerException();
						}
					} else
						throw new NullPointerException("Unssuported post type " + tipo.name()); // TODO: Si se postea una lista de Posts o Jsonifies
				} else if (param.type().is(org.json.JSONObject.class)){
					//TODO.
					/*if(param.name().equals("body"))
					retorno.add("body");
					else
						retorno.add("body.getJSONObject(\"" + param.name() + "\")");*/
					
					throw new NullPointerException();
				} else if (param.type().is(org.json.JSONArray.class)){
					//TODO: check
					$("body."+param.name()+"="+param.name()+";");
					//throw new NullPointerException();
				}else
					throw new NullPointerException("Unssuported post type " + param.type().name());
			}
		}};
	}
	
	private void exportFile(List<String> codigo, String out){
		addResource(codigo, out);
	}
	
	public final ResultGenerator ResultGenerator = new ResultGenerator(); 
	protected class ResultGenerator implements TypescriptCodeDelegator{
		TypescriptCode code;
		@Override
		public TypescriptCode getDelegator() {
			return code;
		}
		public void generate(final JClass clase) {
			new TypescriptCode(){{
				ResultGenerator.this.code = this;
				//CREAR LA CLASE
				$("export class " + clase.getSimpleName(), ()->{
					for(final JVariable f : clase.attributes){
						$(f.name()+":"+$($convert(f.type()))+";");
						if(f.type().isEnum() || f.type().isAnnotationPresent(jSerializable.class))
							$import(f.type());	
					}
					
					$("constructor()", ()->{});
					
					generateFromJson(clase, null, clase.attributes.stream().map(f->f.accessor()).collect(Collectors.toList()), false);
					
					if (clase.isAnnotationPresent(LoginResultClass.class)) {
						crearcodigoTokenSeguridad(this, clase, clase.attributes);
					}
					$("store(key : string)",()-> {
						$("localStorage.setItem( key, JSON.stringify(this));");
					});
					$("public static retrieve(key : string)",()-> {
						$("var ret = localStorage.getItem(key);");
						$if("ret != null",()->{
							$("return this.fromJson(JSON.parse(ret));");//TODO @acbuitrago: Se cambió fromJsonLocal por fromJson, un Result puede tener campos específicos del lado del cliente? 
						});
						$("return null;");
					});
				});
				$imports();
				requiredClasses.addAll(imports);
				exportFile(this, paqueteResultados.replace(".", File.separator) + File.separator + clase.getSimpleName() + ".ts");
			}};
		}
		void generateFromJson(final JClass clase, final JsonLevel level, final List<IAccessor> campos, boolean isLocal){
			$M(PUBLIC | STATIC, clase.getSimpleName(), "fromJson" + (level == null ? "" : level.baseName()) + (isLocal ? "Local" : "" ), $(P(GlobalTypes.Object, "json")), ()->{
				$if("!json", "return null;");
				$("let ret = new "+clase.getSimpleName()+"();");
				for(final IAccessor f : campos)
					procesarCampo(f);
				$("return ret;");
			});
			
			$M(PUBLIC | STATIC, clase.getSimpleName()+(level==null?"":level.baseName())+"[]", "listFromJson"+(level==null?"":level.baseName()) + (isLocal ? "Local" : "" ), $(P(GlobalTypes.ARRAY.Object, "json")), ()->{
				$("return json.map(this.fromJson"+(level==null?"":level.baseName()) + (isLocal ? "Local" : "" )+");");
			});
		}
		void procesarCampo(final IAccessor f){
			if(f.type().name().equals("com.google.appengine.api.datastore.GeoPt")) {
				$ifNotNull("json." + f.name(), ()->{
					$("ret." + f.name() + " = [json." + f.name() + "[0] as number, json."+ f.name() +"[1] as number]");
				});
			}else if(f.type().isEnum()) {
				$("ret." + f.name() +" = json."+ f.name()+" as "+$($convert(f.type()))+";");
			}else if(f.type().is(Date.class)) {
				$("let fecha =json."+ f.name()+" as string");
				$("ret." + f.name() +"= moment.utc(fecha,SDF_"+ f.name()+");");
			}else if(f.type().isJAnnotationPresent(CrystalDate.class)) {
				$("ret." + f.name() +"= new " + $(f.type()) + "(json."+f.name()+" as string);");
			}else if(f.type().is(int.class, Integer.class, long.class, Long.class, double.class, float.class))
				$("ret." + f.name() + " = (json." + f.name() + " as number);");
			else if(f.type().is(boolean.class))
				$("ret." + f.name() + " = json." + f.name() + " as boolean; ");
			else if(f.type().is(String.class, com.google.appengine.api.datastore.Text.class)) {
				$("ret." + f.name() + " = json." + f.name() + " as string;");
			}else if(f.type().isArray()){
				final IJType arrayType = f.type().getInnerTypes().get(0);
				if(arrayType.isSubclassOf(MaskedEnum.class)){
					$("ret." + f.name() + " = " + arrayType.getSimpleName() + ".getFromMask(json." + f.name() + " as number);");
				}else{
					$("let $Array" + f.name() + " = json." + f.name() + " as any[];");
					$if("$Array"+  f.name(),()->{
						$( "ret." + f.name() +" = [];");
						$("for(let i = 0; i < $Array" +  f.name() + ".length; i++)", ()->{
							if(arrayType.is(int.class, long.class))
								$("ret." + f.name() + "[i] = Number($Array" + f.name() + "[i]);");
							else if(arrayType.isEnum()) {
								$("ret." + f.name() + "[i] = Number($Array" + f.name() + "[i]);");
								requiredClasses.add(arrayType);
							}else
								throw new NullPointerException(arrayType.name());
						});
					});
				}
			}else if(f.type().isAnnotationPresent(jSerializable.class)) {
				$ifNotNull("json." + f.name(), ()->{
					$("ret." + f.name() + " = "+f.type().getSimpleName()+".fromJson(json."+ f.name()+");");
				});
			}
			else if(f.type().is(Map.class)) {
				$("let $Array" + f.name() + " = json." + f.name() + " as any;");
				$if("$Array"+  f.name(), ()->{
					final IJType tipoParamero = f.type().getInnerTypes().get(0);
					$("ret." + f.name() + " = $Array" + f.name()+");");
				});
			}
			else if(f.type().isIterable()) {
				$("let $Array" + f.name() + " = json." + f.name() + " as any[];");
				$if("$Array"+  f.name(), ()->{
					final IJType tipoParamero = f.type().getInnerTypes().get(0);
					if(f.isJAnnotationPresent(descriptor.clientAnnotationClass.name()) && tipoParamero.name().equals("java.lang.Object")) {
						$("ret." + f.name() + " = $Array" + f.name() + ";");
					}else {
						$("ret." + f.name() + " = [];");
						$("for(let $temp of $Array" + f.name() + ")", ()->{
							if(tipoParamero.is(Long.class)) {
								$("ret." + f.name() + ".push($temp as number);");
							}else if (tipoParamero.name().equals("com.google.appengine.api.datastore.GeoPt")) {
								$("ret." + f.name() + ".push([$temp[0] as number, $temp[1] as number]);");
							}else if (tipoParamero.isEnum()) {
								IJType tipoId = ((JClass)tipoParamero).enumData.propiedades.get("id");
								$("ret." + f.name() + ".push($temp as "+$(tipoId)+");");
							}else if(tipoParamero.isAnnotationPresent(jEntity.class)) {
								EntityClass entidad = context.data.entidades.get(tipoParamero);
								$("ret." + f.name() + ".push($temp as " + entidad.name() + ");");
							}else
								throw new NullPointerException($(f.type()));
						});
					}
				});
			}else if(f.type().isJAnnotationPresent(InternalEntityKey.class)) {
				$ifNotNull("json." + f.name(), ()->{
					$("ret." + f.name() + " = "+f.type().getJAnnotation(InternalEntityKey.class).simpleKeyName()+".fromJson(json."+ f.name()+");");
				});
			}else if(f.isAnnotationPresent(RelMto1.class) || f.isAnnotationPresent(Rel1to1.class) || f.isAnnotationPresent(EntityKey.class)) {
				EntityClass entidad = context.data.entidades.get(f.type());
				if(entidad == null)
					throw new NullPointerException("Referencia " + f.name()+" de tipo " + f.type().name()+" no existe la entidad target");
				$("ret." + f.name() + " = (json." + f.name() + " as " + $($convert(entidad.key.getSingleKeyType())) +");");
			}else if(f.isJAnnotationPresent(descriptor.clientAnnotationClass)) {
				
				EntityClass entidad = context.data.entidades.get(f.type());
				if(entidad == null) {
					if(f.type().is(Object.class));
					else
						throw new NullPointerException("Referencia " + f.name()+" de tipo " + f.type().name()+" no existe la entidad target");
				}
				if(!f.type().is(Object.class))
					$("ret." + f.name() + " = "+$($convert(f.type()))+".fromJsonLocal(json." + f.name() + ");");//TODO @acbuitrago: No entiendo como funciona esta linea
			}else{
				throw new NullPointerException("Error procesando " + $(f.type())+" : "+f.name());
			}
		}
		
	}
}


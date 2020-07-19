package jcrystal.clients.angular;

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
import jcrystal.clients.typescript.GeneradorDatasources;
import jcrystal.clients.typescript.WebClientTypescript;
import jcrystal.configs.clients.Client;
import jcrystal.configs.clients.IInternalConfig;
import jcrystal.datetime.DateType;
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
import jcrystal.utils.langAndPlats.delegates.TypescriptCodeDelegator;

/**
* Created by AndreaC on 13/12/2016.
*/
public class WebClientAngular extends WebClientTypescript{
	
	private static final IJType BaseNetwork = new JType(null, "BaseNetwork");
	
	protected GeneradorDatasources generadorDatasources;
	
	public WebClientAngular(ClientGeneratorDescriptor<Client> descriptor){
		super(descriptor);
	}
	@Override
	protected void setupEnviroment() throws Exception {
		super.setupEnviroment();
		generadorDatasources = new GeneradorDatasources(this);
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
					String ruta = "\"" + internalConfig.BASE_URL(null) + "\"";
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
				pw.println("import * as moment from 'moment';");
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
			generadorDatasources.generar(entry.getKey(), entry.getValue());
			
			String name = Utils.getManagerName(entry.getKey());
			final TypescriptCode cliente = new TypescriptCode(){{
				int numSlash =  entry.getKey().length() - entry.getKey().replace("/", "").length();
				String path = String.join("", Collections.nCopies(numSlash+1, "../"));
				
				$("import { Injectable } from '@angular/core';");
				$("import { HttpClient,HttpHeaders } from '@angular/common/http';");
				$("import 'rxjs';");
				$("import {AppConfiguration} from \""+path+"../utils/app-configuration\";"); //TODO
				$("import { BaseNetwork, AbsBaseNetwork, defaultOnError, TipoError, RequestError } from '"+path+"services/BaseNetwork';");
				$("import { environment } from 'src/environments/environment';");
				$("export class " + name, ()->{
					for(final IWServiceEndpoint endPoint : entry.getValue()) {
						endPoint.gatherRequiredTypes(imports);
						$("/**");
						$("* " + endPoint.getPath(descriptor));
						$("**/");
						if(endPoint.isMultipart()) {
							$("static "+endPoint.name()+" = new class {");
							incLevel();
							$("fd = new FormData();");
							for(JCrystalWebService service : ((JCrystalMultipartWebService)endPoint).getServices()) {
								final TypescriptCode interno = new TypescriptCode();
								final Tupla2<List<JVariable>, Map<HttpType, List<JVariable>>> clientParams = getParamsWebService(endPoint);
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
						
						final Tupla2<List<JVariable>, Map<HttpType, List<JVariable>>> clientParams = getParamsWebService(endPoint);
						final List<JVariable> parametros = clientParams.v0;
						parametros.add(0, new JVariable(BaseNetwork, "base"));
						
						IJType successType = getReturnCallbackType(this, endPoint);
						if(successType!=null)
							parametros.add(new JVariable(successType, "onSuccess"));
						
						String methodName = endPoint.isMultipart() ? "doRequest" : endPoint.name();
						parametros.add(new JVariable(GlobalTypes.jCrystal.ErrorListener, "onError", "defaultOnError"));
						$((endPoint.isMultipart()?"":"static ") + methodName + "(" + parametros.stream().map(this::$V).collect(Collectors.joining(", ")) + ")", ()->{
							$("let params:string = null;");
							if(endPoint.isMultipart())
								$("var fd = new FormData();");
							
							if(endPoint.isMultipart())
								$("let headers =  new HttpHeaders();");
							else
								$("let headers =  new HttpHeaders({'Content-Type': 'application/json"+(endPoint.getPathType().isGetLike()?"'":";charset='+document.characterSet")+"});");
							
							processHeaderParams(this, clientParams.v1.get(HttpType.HEADER), endPoint.getTokenParam());
							$("var ruta : string = AbsBaseNetwork.BASE_SERVER_URL + \""+ endPoint.getPath(descriptor) + "\";");
							$if("base.onError",()->{//TODO: Revisar posibilidad de bug: && !onError
								$("onError = base.onError;");
							});
							$("if(AppConfiguration.DEBUG)console.log(\"" + endPoint.getPathType().name() + " \"+ruta);");
							
							
							processGetParams(this, clientParams.v1.get(HttpType.GET));
							processPostParams(this, endPoint.contentType()[0], clientParams.v1.get(HttpType.POST));
							$if("params",()->{	
								$("ruta+=params;");
							});
							if(endPoint.getPathType() == Method.GET) {
								$if("!onSuccess", ()->{
									$("return {open : function()",()->{
										$("window.open(ruta);");
									},"};");
								});
							}
								String body;
								if(endPoint.isMultipart())
									body="this.fd, ";
								else if(endPoint.isMultipart())
									body="fd, ";
								else if(endPoint.getPathType().isGetLike())
									body="";
								else
									body = "JSON.stringify(body), ";
								
								String responseType = "json";
								if(endPoint.getReturnType().is(jcrystal.server.FileDownloadDescriptor.class, File.class))
									responseType = "blob";
								
								$("base.http." +(endPoint.getPathType().isGetLike()?"get":"post")+"(ruta, "+body+"{responseType : '"+responseType+"', headers : headers}).subscribe((js:any) =>", ()->{
									if(endPoint.getReturnType().is(jcrystal.server.FileDownloadDescriptor.class, File.class)) {
										$("onSuccess(js);");
									}else {
										$if("js.success === 1", ()->{
											putResponseProcessing(this, endPoint);
										});
										$else_if("js.success === 2", ()->{
											$("onError(new RequestError(js.code, js.mensaje, TipoError.ERROR));");
										});
										$else_if("js.success === 3", ()->{
											$("onError(new RequestError(-1, js.mensaje, TipoError.UNAUTHORIZED));");
										});
										$else(()->{
											$("onError(new RequestError(-1, js.mensaje, TipoError.SERVER_ERROR));");
										});
									}
								},", error =>", ()->{
									//TODO: Better error management
									$("console.log(error);");
									$("onError(new RequestError(-1, error.statusText, TipoError.SERVER_ERROR));");
								},");");
						});
						
						if(endPoint.isMultipart()) {
							$("}");
							decLevel();
						}
					}
				});
				
				$imports(numSlash + 1);
			}};
			
			String paquete = paqueteServicios;
			if(entry.getKey().contains("/"))
				paquete+= "."+entry.getKey().substring(0, entry.getKey().lastIndexOf('/')).replace("/", ".");
			exportFile(cliente, paquete.replace(".", File.separator) + File.separator + name + ".ts");
		}
	}
	protected <X extends AbsCodeBlock> void processHeaderParams(X METODO, List<JVariable> params, JCrystalWebServiceParam token) {
		METODO.new B() {{
			if(token != null)
				$if(token.type().getSimpleName()+".getTokenId() != null",()->{
					$("headers = headers.append('Authorization', "+token.type().getSimpleName()+".getTokenId());");							
				});
		}};
	}
	private void putResponseProcessing(AbsCodeBlock code, IWServiceEndpoint m) {
		code.new B() {{
			if(m.getReturnType().is(Void.TYPE))
				$("onSuccess();");
			else if(m.getReturnType().is(String.class))
				$("onSuccess(js.r as string)");
			else if (m.getReturnType().isIterable()) { 
				final IJType tipoParametro = m.getReturnType().getInnerTypes().get(0);
				$("let _array : any[] = js.r;");
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
					$("onSuccess("+ context.data.entidades.targetEntity(m.getReturnType()).getSimpleName()+".setSession("+ m.getReturnType().getSimpleName()+".fromJson"+ m.getJsonLevel(m.getReturnType()).baseName() +"(js.r as any)));");
				else
					$("onSuccess("+ context.data.entidades.targetEntity(m.getReturnType()).getSimpleName()+".fromJson"+ m.getJsonLevel(m.getReturnType()).baseName() +"(js.r as any));");
			} else if (m.getReturnType().isAnnotationPresent(jSerializable.class)) {
				requiredClasses.add(m.getReturnType());
				if (m.getReturnType().isAnnotationPresent(LoginResultClass.class))
					$("onSuccess("+ m.getReturnType().getSimpleName()+".setSession("+m.getReturnType().getSimpleName()+".fromJson(js.r as any)));");
				else
					$("onSuccess("+ m.getReturnType().getSimpleName() + ".fromJson(js.r as any));");
			} else if (m.getReturnType().getSimpleName().startsWith("Tupla")) {
				final List<IJType> tipos = m.getReturnType().getInnerTypes();
				StringSeparator sp = new StringSeparator(',');
				int e = 0;
				for (IJType p : tipos) {
					if (p.getInnerTypes().isEmpty()) {
						if (p.isAnnotationPresent(jEntity.class)) {
							if (p.isAnnotationPresent(LoginResultClass.class))
								sp.add(context.data.entidades.targetEntity(p).getSimpleName()+".setSession("+ context.data.entidades.targetEntity(p).getSimpleName() + ".fromJson" + m.getJsonLevel(p).baseName() + "(js.r[" + e + "] as any))");
							else
								sp.add(context.data.entidades.targetEntity(p).getSimpleName() + ".fromJson" + m.getJsonLevel(p).baseName() + "(js.r[" + e + "] as any)");
						} else if (p.isAnnotationPresent(jSerializable.class)) {
							requiredClasses.add(p);
							if (p.isAnnotationPresent(LoginResultClass.class))
								sp.add(p.getSimpleName()+".setSession("+ $($convert(p))+".fromJson(js.r[" + e + "] as any))");
							else
								sp.add($($convert(p))+".fromJson(js.r[" + e + "] as any)");
						} else if(p.isPrimitiveObjectType() || p.is(String.class))
							sp.add("js.r[" + e + "] as "+$($convert(p)));
						else if(p.is(GeoPt.class))
							sp.add("[js.r[" + e + "][0] as number, js.r[" + e + "][1] as number]");
						else throw new NullPointerException("Unssuported type " + p);
					} else{
						if(p.isSubclassOf(FileWrapperResponse.class))
							p = p.getInnerTypes().get(0);
						if (p.isSubclassOf(List.class)) {
							IJType pClass = p.getInnerTypes().get(0);
							if (pClass.isAnnotationPresent(jEntity.class)) {
								sp.add(context.data.entidades.targetEntity(pClass).getSimpleName()+".listFromJson"+ m.getJsonLevel(pClass).baseName()+"(js.r[" + e + "] as any[])");
							} else if (pClass.isAnnotationPresent(jSerializable.class)) {
								requiredClasses.add(pClass);
								sp.add($($convert(pClass)) + ".listFromJson(js.r[" + e + "] as any[])");
							} else throw new NullPointerException("Unssuported type " + p);
						} else
							throw new NullPointerException("Error, tipos incompatibles. No se puede tener un metodo con retorno " + p);
					}
					e++;
				}
				$("onSuccess(" + sp + ");");
			}else if(m.getReturnType().isPrimitive())
				$("onSuccess(js.r as "+$($convert(m.getReturnType()))+")");
			else
				$("onSuccess(js.r);");
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
}


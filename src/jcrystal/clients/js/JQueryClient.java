package jcrystal.clients.js;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import jcrystal.clients.AbsClientGenerator;
import jcrystal.clients.ClientGeneratorDescriptor;
import jcrystal.configs.clients.Client;
import jcrystal.configs.clients.IInternalConfig;
import jcrystal.datetime.DateType;
import jcrystal.db.query.Page;
import jcrystal.json.JsonLevel;
import jcrystal.lang.Language;
import jcrystal.model.server.db.EntityField;
import jcrystal.model.server.db.EntityClass;
import jcrystal.model.web.HttpType;
import jcrystal.model.web.IWServiceEndpoint;
import jcrystal.model.web.JCrystalWebService;
import jcrystal.model.web.JCrystalWebServiceParam;
import jcrystal.reflection.MaskedEnum;
import jcrystal.reflection.annotations.CrystalDate;
import jcrystal.reflection.annotations.LoginResultClass;
import jcrystal.reflection.annotations.com.jSerializable;
import jcrystal.reflection.annotations.ws.ContentType;
import jcrystal.results.Tupla2;
import jcrystal.reflection.annotations.jEntity;
import jcrystal.server.FileDownloadDescriptor;
import jcrystal.server.FileUploadDescriptor;
import jcrystal.types.IJType;
import jcrystal.types.JClass;
import jcrystal.types.JVariable;
import jcrystal.types.utils.GlobalTypes;
import jcrystal.utils.HashUtils;
import jcrystal.utils.StringSeparator;
import jcrystal.utils.StringUtils;
import jcrystal.utils.context.CodeGeneratorContext;
import jcrystal.utils.context.CodeGeneratorContext.ContextType;
import jcrystal.utils.langAndPlats.AbsCodeBlock;
import jcrystal.utils.langAndPlats.JavaCode;
import jcrystal.utils.langAndPlats.JavascriptCode;

public class JQueryClient extends AbsClientGenerator<Client> {
	GeneradorEntidad internalGeneradorEntidad;
	public JQueryClient(ClientGeneratorDescriptor<Client> descriptor){
		super(descriptor);
		entityGenerator = internalGeneradorEntidad = new GeneradorEntidad(this);
	}
	@Override
	protected void setupEnviroment() throws Exception {
		if(context.data.rolClass != null)
			requiredClasses.add(context.data.rolClass);
		ContextType.CLIENT.init();
		CodeGeneratorContext.set(Language.JAVASCRIPT, type->{
			if(type.isJAnnotationPresent(CrystalDate.class))
				return GlobalTypes.DATE;
			else if(type.isArray() && type.getInnerTypes().get(0).isSubclassOf(MaskedEnum.class))
				return GlobalTypes.LONG;
			return type;
		});
	}
	@Override
	public void generarCliente() throws Exception {
		generarCodigoMobile();
		generateUtils();
	}
	
	private void generarEnums(JavascriptCode code) throws Exception {
		code.new B() {{
			$("let Enum = ",()->{
				requiredClasses.stream().filter(f->f.isEnum()).map(f->(JClass)f).forEach(claseEnum->{
					$(claseEnum.simpleName+":", () -> {
						IJType idType = claseEnum.enumData.propiedades.get("id");
						claseEnum.enumData.valores.forEach(o->{
							$(o.name + " : ",()->{
								claseEnum.enumData.propiedades.forEach((key, type)->{
									if(type.is(String.class))
										$(key + ": '" + o.propiedades.get(key)+"',");
									else
										$(key + ": " + o.propiedades.get(key)+",");
								});
							}, ",");
						});
						$("values : function()",()->{
							$("return ["+claseEnum.enumData.valores.stream().sorted((o1,o2)->o1.name.compareTo(o2.name)).map(o->"Enum."+claseEnum.simpleName+"."+o.name).collect(Collectors.joining(", "))+"];");
						},",");
						$("toJSON : function()",()->{
							$("return this.id;");
						},",");
						try {
							$("getFromId: function(id)",()->{
								claseEnum.enumData.valores.forEach(o->{
									if(idType == null) {
										$if("'"+o.name+"' == id", "return Enum."+claseEnum.simpleName+"."+o.name);
									}else {
										Object id = o.propiedades.get("id");
										if(idType.is(String.class))
											$if("'"+id+"' == id", "return Enum."+claseEnum.simpleName+"."+o.name);
										else
											$if(id+" == id", "return Enum."+claseEnum.simpleName+"."+o.name);	
									}
									
								});
							},",");
							if(claseEnum.isSubclassOf(MaskedEnum.class)) {
								$("getFromMask: function(mask)",()->{
									$("var ret = [];");
									claseEnum.enumData.valores.forEach(o->{
										Object id = o.propiedades.get("id");
										$if("(mask & "+id+") != 0", "ret.push(Enum."+claseEnum.name+"."+o.name+");");
									});
									$("return ret;");
								});
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					},",");
				});
			},";");
		}};
	}
	
	private void generarCodigoMobile() throws Exception {
		endpoints.entrySet().stream().collect(Collectors.toMap(entry->Utils.getManagerName(entry.getKey()), entry->{
			return new ArrayList<>(entry.getValue());
		}, (l1,l2)->{
			l1.addAll(l2);
			return l1;
		})).forEach((name, managerList)->{
			new JavascriptCode() {{
				$("var " + name +" = ", () -> {
					for (final IWServiceEndpoint endpoint : managerList) {
						JCrystalWebService m = (JCrystalWebService)endpoint;
						final Tupla2<List<JVariable>, Map<HttpType, List<JVariable>>> clientParams = getParamsWebService(m);
						final List<JVariable> parametros = clientParams.v0;
						if (m.isClientQueueable()) {
							throw new NullPointerException();
						} else {
							parametros.add(P(GlobalTypes.jCrystal.VoidSuccessListener, "onSuccess"));
							parametros.add(P(GlobalTypes.jCrystal.ErrorListener, "onError"));
							
							IInternalConfig internalConfig = m.exportClientConfig(descriptor);
							String methodName = m.name();
							final boolean nativeEmbeddedResponse = internalConfig.embeddedResponse() && SUPPORTED_NATIVE_TYPES.contains(m.getReturnType());
							generateDocs(name, methodName, endpoint);
							if(m.getReturnType().is(Page.class))
								parametros.add(new JVariable(GlobalTypes.STRING, "nextPageToken"));
							$(methodName + " : function("+parametros.stream().map(f -> f.name()).collect(Collectors.joining(", ")) + ")", () -> {
								String ruta = m.getPath(descriptor);
								for (final JCrystalWebServiceParam param : m.parametros)
									if (param.tipoRuta == HttpType.PATH)
										ruta = ruta.replace("$" + param.nombre, "\"+" + param.nombre + "+\"");
								String url = internalConfig.BASE_URL("web");
								$("var _ruta = \"" + url + ruta + "\";");
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
									$("$('[data-" + name + "-" + methodName + "]').addClass('loading');");
									$("let $xhr = new XMLHttpRequest();");
									$("$xhr.open('" + m.tipoRuta.name() + "', _ruta);");
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
										$("$('[data-" + name + "-" + methodName + "]').removeClass('loading');");
										$("onError({type: \"SERVER_ERROR\", code: $xhr.status, msg: $xhr.response});");
									},";");
									$("$xhr.onload = function()",()->{
										$("$('[data-" + name + "-" + methodName + "]').removeClass('loading');");
										$if("$xhr.status >= 200 && $xhr.status < 300",()->{
											$("var response = $xhr.response;");
											if(m.tipoRuta.isPostLike() && m.getReturnType().is(FileDownloadDescriptor.class)) {
												$("onSuccess(response, $xhr.getResponseHeader('Content-Disposition').match(/filename[^;=\\n]*=((['\"]).*?\\2|[^;\\n]*)/)[1]);");
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
														if (SUPPORTED_NATIVE_TYPES.contains(m.getReturnType()) || m.getReturnType().is(String.class)) {
															if(nativeEmbeddedResponse)
																$("onSuccess(response);");
															else
																$("onSuccess(response.r);");
														} else if (m.getReturnType().isSubclassOf(Map.class)) {
															if(nativeEmbeddedResponse)
																$("onSuccess(response);");
															else
																$("onSuccess(response.r);");
														}else if (m.getReturnType().is(Page.class)) {
															IJType inner = m.getReturnType().getInnerTypes().get(0); 
															String response = "response.r.list";
															if(nativeEmbeddedResponse)
																throw new NullPointerException("Unsupported");
															if (inner.isAnnotationPresent(jEntity.class))
																response = "Entity."+inner.resolve().getSimpleName() + ".prototype.$convertArray(" + response + ")";
															$("onSuccess({'list':" + response + ", 'next': response.r.nextToken ? function()",()->{
																$(name+"."+methodName+"(" + parametros.stream().limit(parametros.size() - 1).map(f -> f.name()).collect(Collectors.joining(", ")) + ", response.r.nextToken);");
															}," : null});");
														}else if (m.getReturnType().isIterable()) {
															IJType inner = m.getReturnType().getInnerTypes().get(0); 
															String response = "response.r";
															if(nativeEmbeddedResponse)
																response = "response";
															if (inner.isAnnotationPresent(jEntity.class))
																response = "Entity."+context.data.entidades.targetEntity(inner).getSimpleName() + ".prototype.$convertArray(" + response + ")";
															$("onSuccess(" + response + ");");
														}else if (m.getReturnType().isAnnotationPresent(jEntity.class)) {
															if(context.input.SERVER.DEBUG.CORS && m.getReturnType().isAnnotationPresent(LoginResultClass.class))
																$("onSuccess(response.r ? new Entity."+context.data.entidades.targetEntity(m.getReturnType()).getSimpleName() + "(response.r).storeToken() : null);");
															else
																$("onSuccess(response.r ? new Entity."+context.data.entidades.targetEntity(m.getReturnType()).getSimpleName() + "(response.r) : null);");
														} else if (m.getReturnType().isAnnotationPresent(jSerializable.class)) {
															if(nativeEmbeddedResponse)
																$("onSuccess(response);");
															else
																$("onSuccess(response.r);");
														} else if (m.getReturnType().getSimpleName().startsWith("Tupla")) {
															final List<IJType> tipos =m.getReturnType().getInnerTypes();
															StringSeparator sp = new StringSeparator(", ");
															int e = 0;
															for (IJType p : tipos) {
																if (p.isAnnotationPresent(jEntity.class))
																	sp.add("response.r[" + e + "] ? new Entity." + context.data.entidades.targetEntity(p).getSimpleName() + "(response.r[" + e + "]) : null");
																else if(p.isIterable() && p.getInnerTypes().get(0).isAnnotationPresent(jEntity.class))
																	sp.add("Entity." + context.data.entidades.targetEntity(p.getInnerTypes().get(0)).getSimpleName() + ".prototype.$convertArray(response.r[" + e + "])");
																else
																	sp.add("response.r[" + e + "]");
																e++;
															}
															$("onSuccess(" + sp + ");");
														} else
															$("onSuccess(result);");
														$("return;");
													}
												});
												if (internalConfig.ERROR_CONDITION() != null)
													$if(internalConfig.ERROR_CONDITION(), () -> {
													$("onError({type: \"ERROR\", code: response.code, msg: response."+internalConfig.ERROR_MESSAGE_NAME() + "});");
													$("return;");
												});
												if (internalConfig.UNATHORIZED_CONDITION() != null)
													$if(internalConfig.UNATHORIZED_CONDITION(), () -> {
													$("onError({type: \"UNAUTHORIZED\", code: response.code, msg: response."+internalConfig.ERROR_MESSAGE_NAME() + "});");
													$("return;");
												});
												if (internalConfig.SUCCESS_TYPE() != null) {
													$("onError({type: \"SERVER_ERROR\", code: response.code, msg: response."+internalConfig.ERROR_MESSAGE_NAME() + "});");
												}
											}
										});
										$else(()->{
											$("onError({type: \"ERROR\", code: $xhr.status, msg: \"Server error : \" + $xhr.status});");
										});
									},";");
									
								});
								if(m.tipoRuta.isGetLike())
									$else(()->{
										$("return ",()->{
											$("OPEN : function()",()->{
												$("var win = window.open(_ruta, '_blank');");
												$if("win",()->{
													$("win.focus();");
												});
												$else(()->{
													$("alert('Please allow popups for this website');");
												});
											},",");
											$("URL : function()",()->{
												$("return _ruta;");
											});
										},";");
									});
							},",");
						}
					}
				});
				exportFile(this, name + ".js");
			}};
		});
	}
	@Override
	protected <X extends AbsCodeBlock> void processPostParams(X METODO, ContentType contentType, List<JVariable> params) {
		if(params.isEmpty())
			return;
		METODO.new B() {{
			StringSeparator postParams = new StringSeparator(", ");
			for(JVariable param : params) {
				if(param.type().is(FileUploadDescriptor.class)) {
					$ifNotNull(param.name(), ()->{
						$("fd.append(\""+param.name()+"\", "+param.name()+");");
					});
				}else
					postParams.add(param.name()+": " + param.name());
			}
			if(!postParams.isEmpty())
				$("var body = {"+postParams+"};");
		}};
	}
	protected <X extends AbsCodeBlock> void processHeaderParams(X METODO, List<JVariable> params, JCrystalWebServiceParam token) {
		StringSeparator headerParams = new StringSeparator(", ");
		for(JVariable param : params) {
			if(param.type().is(String.class)) {
				METODO.$ifNotNull(param.value, ()->{
					METODO.$("$xhr.setRequestHeader(\"" + param.name() + "\", " + param.value + ");");
				});
			}
		}
		if(token != null && context.input.SERVER.DEBUG.CORS)
			METODO.$ifNotNull("localStorage.getItem('"+token.type().getSimpleName()+"')",()->{
				METODO.$("$xhr.setRequestHeader(\"Authorization\", localStorage.getItem('"+token.type().getSimpleName()+"'));");
			});
		if(!headerParams.isEmpty())
			METODO.$("headers: {"+headerParams+"},");
	}
	@Override
	protected <X extends AbsCodeBlock> void processGetParams(X METODO, List<JVariable> params) {
		if(params.isEmpty())
			return;
		StringSeparator getParams = new StringSeparator(", ");
		METODO.new B() {{
			for (JVariable param : params) {
				if (param.type().isPrimitive()) {
					getParams.add(param.name()+": " + param.name());
				} else if (param.type().is(String.class)) {
					getParams.add(param.name()+": " + param.name());
				} else if (param.type().isPrimitiveObjectType()) {
					getParams.add(param.name()+": " + param.name());
				} else if (param.type().isEnum()) {
					getParams.add(param.name()+": " + param.name());
				}
				else if (param.type().getSimpleName().equals("JSONObject")) {
					throw new NullPointerException();
				} else if (param.type().getSimpleName().equals("JSONArray")) {
					throw new NullPointerException();
				} else if (param.type().isJAnnotationPresent(CrystalDate.class)) {
					getParams.add(param.name()+": " + param.name());
				} else
					throw new NullPointerException("Parametro no reconocido " + param.type().getSimpleName());
			}
			$("_ruta += \"?\" + $.param({"+getParams+"});");
		}};
	}
	protected void generateDocs(String managerName, String methodName, IWServiceEndpoint endpoint) {
		new JavascriptCode() {{
			$(managerName+"."+methodName+"(function()",()->{
				
			},");");
			endpoint.registerClientExample(descriptor.client.id, this);
		}};
	}
	private void generateUtils() throws Exception {
		new JavascriptCode() {{
			$("let CrystalDates = ",()->{
				Arrays.stream(DateType.values()).forEach(type->{
					String nombre = StringUtils.camelizar(type.name());
					$(nombre + " : function(val)",()->{
						$if("typeof val === 'string' || val instanceof String",()->{
							$("this.strValue = val;");
							$("this.dateValue = null;");
						});
						$if("typeof val === 'Date' || val instanceof Date",()->{
							if(type == DateType.DATE || type == DateType.MONTH) {
								$("val.setMinutes(0);");
								$("val.setSeconds(0);");
								$("val.setHours(0);");
								if(type == DateType.MONTH)
									$("val.setDate(1);");
							}
							$("this.dateValue = val;");
							$("this.strValue = this.format();");
						});
					},",");
				});
			});
			Arrays.stream(DateType.values()).forEach(type->{
				String nombre = StringUtils.camelizar(type.name());
				final Function<String, String> tokenizer = (letter)->{
					int a = type.format.indexOf(letter);
					int e = type.format.lastIndexOf(letter)+1;
					if(a>=0) {
						if("M".equals(letter))
							return "parseInt(this.strValue.substring("+a+","+e+"))-1";
						else
							return "this.strValue.substring("+a+","+e+")";
					}
					else if("d".equals(letter))
						return "1";
					return "0";
				};
				final BiFunction<Boolean, Character, String> getPart = (utc, letter)->{
					String pre = utc?"UTC":"";
					switch (letter.charValue()) {
						case 'y':
							return "(this.dateValue.get"+pre+"FullYear()).toString()";
						case 'M':
							return "((this.dateValue.get"+pre+"Month() < 9 ? '0' : '')+(this.dateValue.get"+pre+"Month()+1))";
						case 'd':
							return "(this.dateValue.get"+pre+"Date() < 10 ? '0' : '')+this.dateValue.get"+pre+"Date().toString()";
						case 'H':
							return "(this.dateValue.get"+pre+"Hours() < 10 ? \"0\"+this.dateValue.get"+pre+"Hours() : this.dateValue.get"+pre+"Hours())";
						case 'm':
							return "(this.dateValue.get"+pre+"Minutes() < 10 ? \"0\"+this.dateValue.get"+pre+"Minutes() : this.dateValue.get"+pre+"Minutes())";
						case 's':
							return "(this.dateValue.get"+pre+"Seconds() < 10 ? \"0\"+this.dateValue.get"+pre+"Seconds() : this.dateValue.get"+pre+"Seconds())";
					}
					return "'"+letter+"'";
				};
				
				$("CrystalDates." + nombre + ".prototype = ",()->{
					$("CLIENT_FORMAT : ",()->{
						$("value : \""+type.userFormat.replace("M", "x").replace("m", "M").replace("x", "m")+"\",");
						$("enumerable: false, configurable: false, writable: false,");
					},",");
					$("toJSON : function()",()->{
						$("return this.strValue;");
					},",");
					$("parse : function()",()->{
						$ifNull("this.dateValue",()->{
							$("this.dateValue = new Date(Date.UTC("+tokenizer.apply("y")+", "+tokenizer.apply("M")+", "+tokenizer.apply("d")+", "+tokenizer.apply("H")+", "+tokenizer.apply("m")+", "+tokenizer.apply("s")+"));");
						});
						$("return this.dateValue;");
					},",");
					$("next : function()",()->{
						$("var copiedDate = new Date(this.parse());");
						switch (type) {
							case DATE_MILIS:
							case TIME_MILIS:
								$("copiedDate = new Date(copiedDate + 1);");
								break;
							case DATE_SECONDS:
							case TIME_SECONDS:
								$("copiedDate.setUTCSeconds(copiedDate.getUTCSeconds() + 1);");
								break;
							case DATE_TIME:
							case TIME:
								$("copiedDate.setUTCMinutes(copiedDate.getUTCMinutes() + 1);");
								break;
							case DATE:
								$("copiedDate.setUTCDate(copiedDate.getUTCDate() + 1);");
								break;
							case MONTH:
								$("copiedDate.setMonth(copiedDate.getMonth() + 1);");
								break;
							case YEAR:
								$("copiedDate.setUTCFullYear(copiedDate.getUTCFullYear() + 1);");
								break;
							default:
								break;
						}
						$("return new CrystalDates."+nombre+"(copiedDate);");
					},",");
					$("prev : function()",()->{
						$("var copiedDate = new Date(this.parse());");
						switch (type) {
							case DATE_MILIS:
							case TIME_MILIS:
								$("copiedDate = new Date(copiedDate - 1);");
								break;
							case DATE_SECONDS:
							case TIME_SECONDS:
								$("copiedDate.setUTCSeconds(copiedDate.getUTCSeconds() - 1);");
								break;
							case DATE_TIME:
							case TIME:
								$("copiedDate.setUTCMinutes(copiedDate.getUTCMinutes() - 1);");
								break;
							case DATE:
								$("copiedDate.setUTCDate(copiedDate.getUTCDate() - 1);");
								break;
							case MONTH:
								$("copiedDate.setMonth(copiedDate.getMonth() - 1);");
								break;
							case YEAR:
								$("copiedDate.setUTCFullYear(copiedDate.getUTCFullYear() - 1);");
								break;
							default:
								break;
						}
						$("return new CrystalDates."+nombre+"(copiedDate);");
					},",");
					for(boolean b : new boolean[] {false,true}) {
						$("format"+(b?"Client":"")+" : function("+(b?"utc":"format")+")",()->{
							char ant = 0;
							String val = "";
							for(char c : (b ? type.userFormat : type.format).toCharArray()) {
								if(c != ant)
									val += " + " + getPart.apply(!b, c);
								ant = c;
							}
							String finalVal = val;
							$ifNull("this.dateValue", ()->{
								$("this.parse();");
							});
							if(b) {
								$ifNotNull("this.CLIENT_FORMAT.value", ()->{
									$("return this.dateValue.format(this.CLIENT_FORMAT.value, utc);");
								});
							}
							if(!b)
								$ifNotNull("format",()->{
									$("return this.dateValue.format(format);");
								});
							$("return " + finalVal.substring(3)+";");
						},",");
					}
					$("toString : function()",()->{
						$("return this.formatClient();");
					},",");
				},";");
			});
			internalGeneradorEntidad.setCodeDelegator(this);
			$("let Entity = ",()->{
				RequieredEntities entities = getRequiredEntities();
				for(final Map.Entry<IJType, TreeSet<JsonLevel>> classEntity : entities.complete.entrySet()){
					final EntityClass entidad = context.data.entidades.get(classEntity.getKey());
					internalGeneradorEntidad.generateClass(entidad, classEntity.getValue());
				}	
			});
			$("let Results = ",()->{
				requiredClasses.stream().filter(f->f.isAnnotationPresent(jSerializable.class)).collect(Collectors.toList()).forEach(r->{
					$(r.getSimpleName()+" : function(rawObject)",()->{
						for(final JVariable f : ((JClass)r).attributes){
							if(f.type().isEnum()) {
								requiredClasses.add(f.type());
								$ifNotNull("rawObject." + f.name(),()->{
									$("this." + f.name() + " = new Enum."+f.type().getSimpleName()+".getFromId(rawObject." + f.name() + ");");
								});
							}else if(f.type().isJAnnotationPresent(CrystalDate.class)) {
								String nombre = StringUtils.camelizar(f.type().getJAnnotation(CrystalDate.class).value().name());
								$ifNotNull("rawObject." + f.name(),()->{
									$("this." + f.name() + " = new CrystalDates."+nombre+"(rawObject." + f.name()+");");
								});
							}
							else
								$("this." + f.name() + " = rawObject." + f.name()+";");
						}
					},",");					
				});
			});
			
			generarEntidades();
			
			generarEnums(this);
			$("var escapeHtml = function(unsafe)",()->{
			    $("return unsafe.replace(/&/g, \"&amp;\").replace(/</g, \"&lt;\").replace(/>/g, \"&gt;\").replace(/\"/g, \"&quot;\").replace(/'/g, \"&#039;\");");
			},";");
			$("function getUrlParam(name)",()->{
			    $("name = name.replace(/[\\[]/, '\\\\[').replace(/[\\]]/, '\\\\]');");
	    		$("var regex = new RegExp('[\\\\?&]' + name + '=([^&#]*)');");
				$("var results = regex.exec(location.search);");
				$("return results === null ? '' : decodeURIComponent(results[1].replace(/\\+/g, ' '));");
			});
			exportFile(this, "model.js");
		}};
	}
}

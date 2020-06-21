package jcrystal.server.web.servlet;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.appengine.api.datastore.GeoPt;

import jcrystal.annotations.internal.model.db.InternalEntityKey;
import jcrystal.clients.AbsClientGenerator;
import jcrystal.clients.ClientId;
import jcrystal.db.query.Page;
import jcrystal.entity.types.LongText;
import jcrystal.main.data.ClientContext;
import jcrystal.manager.utils.FileWrapperResponse;
import jcrystal.model.security.SecurityTokenClass;
import jcrystal.model.server.db.EntityClass;
import jcrystal.model.server.db.EntityField;
import jcrystal.model.web.HttpType;
import jcrystal.model.web.IWServiceEndpoint;
import jcrystal.model.web.JCrystalWebService;
import jcrystal.model.web.JCrystalWebServiceManager;
import jcrystal.model.web.JCrystalWebServiceParam;
import jcrystal.model.web.WSStateType;
import jcrystal.reflection.RolGenerator;
import jcrystal.reflection.annotations.CrystalDate;
import jcrystal.reflection.annotations.LoginResultClass;
import jcrystal.reflection.annotations.Post;
import jcrystal.reflection.annotations.jEntity;
import jcrystal.reflection.annotations.async.Cron;
import jcrystal.reflection.annotations.com.jSerializable;
import jcrystal.reflection.annotations.ws.ContentType;
import jcrystal.reflection.annotations.ws.HeaderParam;
import jcrystal.reflection.annotations.ws.SingleCallWS;
import jcrystal.reflection.docs.Doc;
import jcrystal.security.SignInInfo;
import jcrystal.server.FileDownloadDescriptor;
import jcrystal.server.FileUploadDescriptor;
import jcrystal.server.async.Async;
import jcrystal.service.types.Authorization;
import jcrystal.types.IJType;
import jcrystal.types.JClass;
import jcrystal.types.JMethod;
import jcrystal.types.JVariable;
import jcrystal.types.utils.GlobalTypes;
import jcrystal.utils.EnumUtils;
import jcrystal.utils.HashUtils;
import jcrystal.utils.StringSeparator;
import jcrystal.utils.StringUtils;
import jcrystal.utils.langAndPlats.AbsCodeBlock;

public class GeneradorMetodoServlet {
	JClass padre;
	JCrystalWebService metodo;
	JMethod m;
	private JVariable token;
	public final boolean needsJpaEntityManager;
	ClientContext context;

	public GeneradorMetodoServlet(ClientContext context, JCrystalWebService metodo) {
		this.metodo = metodo;
		this.context = context;
		m = metodo.unwrappedMethod;
		padre = metodo.padre.clase;
		needsJpaEntityManager = metodo.parametros.stream()
				.filter(param -> param.p.type().name().equals("javax.persistence.EntityManager")).findFirst()
				.isPresent();
	}

	public void crearSwitchServlet(JCrystalWebServiceManager clase, final AbsCodeBlock GENERADO) {
		final String methodName = metodo.getSubservletMethodName();
		GENERADO.new B() {
			{
				if (m.name().equals("index"))
					$("case \"" + metodo.getPath(null) + "/\":");
				$("case \"" + metodo.getPath(null) + "\":", () -> {
					$(methodName + "(req, resp);");
					$("break;");
				});
			}
		};
	}

	public static void putAuthCondition(final ClientContext context, final AbsCodeBlock code,
			IWServiceEndpoint endpoint, Runnable r) {
		if (endpoint.getTokenParam() == null)
			r.run();
		else
			code.new B() {
				{
					String tokenName = endpoint.getTokenParam().name();
					$(endpoint.getTokenParam().p.type().name() + " " + tokenName + " = "
							+ endpoint.getTokenParam().p.type().name() + ".get(getToken(req));");

					StringSeparator cond = new StringSeparator(" && ");
					cond.add(tokenName + " != null");
					StringSeparator roles = new StringSeparator(" || ");

					RolGenerator.ROL_MAPPER.forEach((key, rol) -> {
						if (endpoint.getTokenParam().p.type().isJAnnotationPresent(key)) {
							cond.add(rol.valueAccessor + ".is(" + tokenName + ".rol())");
						}
						if (endpoint.isJAnnotationPresent(key))
							roles.add(rol.valueAccessor + ".is(" + tokenName + ".rol())");
					});
					if (!roles.isEmpty())
						cond.add("(" + roles + ")");
					if (endpoint.isAdminClient() && endpoint != context.data.adminData.login)
						cond.add(tokenName + ".rol() == -1");
					$if(cond.toString(), () -> {
						r.run();
						$("if(" + tokenName + ".needsUpdate())" + tokenName + ".put();");
					});
					$("else sendNonAuthorizedUser(resp);");
				}
			};
	}

	public void putAuthCondition(final AbsCodeBlock code, boolean isBackgroundJob, IWServiceEndpoint endpoint,
			Runnable r) {
		code.new B() {
			{
				if (metodo.tokenParam != null) {
					token = metodo.tokenParam.p;
					putAuthCondition(context, code, endpoint, r);
				} else if (!isBackgroundJob && metodo.isAdminClient && metodo != context.data.adminData.login) {
					EntityClass entidadToken = context.data.entidades.get(context.data.adminData.login.getReturnType());
					token = new JVariable(0, entidadToken.clase, "token");
					$(token.type().name() + " token = " + token.type().name() + ".get(getToken(req));");
					$("if(token != null && token.rol() == -1){");
					P.incLevel();
					r.run();
					$("if(token.needsUpdate())token.put();");
					P.decLevel();
					$("}else sendNonAuthorizedUser(resp);");
				} else
					r.run();
			}
		};
	}

	private void putSpecialConditions(final AbsCodeBlock METODOS, Runnable r) {
		if (m.isAnnotationPresent(SingleCallWS.class)) {
			METODOS.new B() {
				{
					$("jcrystal.utils.SingleCallWSEntity $version = jcrystal.utils.SingleCallWSEntity.get(\""
							+ metodo.getPath(null) + "\");");
					$if("$version.version() >= " + m.getAnnotation(SingleCallWS.class).value(), () -> {
						$("resp.setStatus(403);");
						$("return;");
					});
					$("$version.version(" + m.getAnnotation(SingleCallWS.class).value() + ").put();");
				}
			};
		}
		r.run();
	}

	public void crearMetodoServlet(final AbsCodeBlock METODOS) {
		final String cacheMethodName = (padre.name() + "." + m.name()).replace(".", "_")
				+ (m.isAnnotationPresent(Async.class) ? "_Async" : "");
		METODOS.new B() {
			{
				if (m.getAnnotation(Deprecated.class) != null)
					$("@Deprecated");
				if (m.getAnnotation(Doc.class) != null)
					$("/** " + m.getAnnotation(Doc.class).value() + " **/");
				String returno = "void";
				if (metodo.padre.isMultipart && metodo.isWritatableResponse())
					returno = $(m.returnType);
				$((metodo.padre.isMultipart ? "" : "static ") + returno + " " + cacheMethodName
						+ "(HttpServletRequest req, HttpServletResponse resp)throws Exception", () -> {
							putSpecialConditions(METODOS, () -> {
								boolean isBackgroundJob = false;
								if (m.getAnnotation(Cron.class) != null) {
									if (!m.params.isEmpty())
										throw new NullPointerException("Invalid cron method");
									$if("req.getHeader(\"X-Appengine-Cron\") == null", () -> {
										$("resp.setStatus(401);");
										$("return;");
									});
									isBackgroundJob = true;
								} else if (m.getAnnotation(Async.class) != null) {
									$if("(!\"0.1.0.2\".equals(req.getRemoteAddr()) && com.google.appengine.api.utils.SystemProperty.environment.value() == com.google.appengine.api.utils.SystemProperty.Environment.Value.Production)",
											() -> {
												$("resp.setStatus(401);");
												$("return;");
											});
									isBackgroundJob = true;
								}
								putAuthCondition(METODOS, isBackgroundJob, metodo, () -> {
									if (metodo.transaccion != null) {
										$("for(int $retry = 5; $retry > 0; $retry--, Thread.sleep(1000))", () -> {
											if (needsJpaEntityManager) {
												$("try", () -> {
													$("jpaUtx.begin();");
													for (ContentType content : metodo.contentTypes) {
														$if(metodo.tipoRuta.isPostLike(), ContentType.class.getName()
																+ "." + content.name() + ".is(req.getContentType())",
																() -> {
																	String methodParas = getParams(METODOS, content);
																	fillMethodContent(this.P, methodParas, () -> {
																		putResponseHeaders(METODOS);
																		writeResponse(this.P);
																	});
																});
													}
													$("jpaUtx.commit();");
												});
												$("catch (javax.transaction.NotSupportedException|javax.transaction.HeuristicMixedException ex)",
														() -> {
															$("break;");
														});
												$("catch (javax.transaction.RollbackException|javax.transaction.SystemException|javax.transaction.HeuristicRollbackException ex)",
														() -> {
															$("try", () -> {
																$("jpaUtx.rollback();");
																$("continue;");
															});
															$("catch (javax.transaction.SystemException ex1)", () -> {
															});
														});
												$("break;");
											} else {
												$("jcrystal.context.CrystalContext.clear();");
												$("try", () -> {
													for (ContentType content : metodo.contentTypes)
														$if(metodo.tipoRuta.isPostLike(), ContentType.class.getName()
																+ "." + content.name() + ".is(req.getContentType())",
																() -> {
																	String methodParas = getParams(METODOS, content);
																	fillMethodContent(this.P, methodParas, () -> {
																		$("jcrystal.context.CrystalContext.get().DefaultDB().endTx();");
																		$if("jcrystal.context.CrystalContext.get().DefaultDB().rollbackTx()",
																				() -> {
																					putResponseHeaders(METODOS);
																					writeResponse(this.P);
																					$("break;");
																				});
																	});
																});
												});
												$("catch(java.util.ConcurrentModificationException ex)", () -> {
													$if("!ex.getMessage().startsWith(\"too much contention on these datastore entities\")",
															() -> {
																$("throw ex;");
															});
												});
												$("catch(Exception ex)", () -> {
													$("jcrystal.context.CrystalContext.get().DefaultDB().rollbackTx();");
													$("$retry = Integer.MIN_VALUE;");
													$("throw ex;");
												});

											}
										});
									} else {
										for (ContentType content : metodo.contentTypes)
											$if(metodo.tipoRuta.isPostLike(), ContentType.class.getName() + "." + content.name() + ".is(req.getContentType())", () -> {
												String methodParas = getParams(METODOS, content);
												fillMethodContent(this.P, methodParas, () -> {
													putResponseHeaders(METODOS);
													if (!metodo.padre.isMultipart) {
														writeResponse(this.P);
													} else if (metodo.isWritatableResponse())
														$("return $salida;");
												});
											});
									}
								});
							});
							$("jcrystal.context.CrystalContext.clear();");
						});
			}
		};
	}

	private void fillMethodContent(AbsCodeBlock b, String methodParas, Runnable beforeWrite) {
		final String instanceName = "$instance" + padre.name().replace(".", "_");
		final String controller = metodo.stateType == WSStateType.STATIC ? padre.name : instanceName;
		b.new B() {
			{
				// Token related validations
				if (token != null) {
					SecurityTokenClass tokenClass = context.data.tokens.get(token.type);
					if (tokenClass != null)
						for (JCrystalWebServiceParam p : metodo.parametros) {
							JMethod validator = tokenClass.validators.get(p.type());
							if (validator != null)
								$(token.name + "." + validator.name + "(" + p.nombre + ");");
						}
				}
				// Method call
				if (!metodo.padre.isMultipart)
					if (metodo.stateType == WSStateType.STATELESS) {
						if (metodo.padre.clase.hasEmptyConstructor())
							$("final " + metodo.padre.clase.name() + " " + instanceName + " = new "
									+ metodo.padre.clase.name() + "();");
						else {
							String params = metodo.padre.clase.name() + "("
									+ metodo.padre.clase.constructors.get(0).params.stream().map(f -> f.name)
											.collect(Collectors.joining(", "));
							$("final " + metodo.padre.clase.name() + " " + instanceName + " = new " + params + ");");
						}
						metodo.padre.managedAttributes
								.forEach(m -> $(instanceName + "." + m.name + " = " + m.name + ";"));
					}

				if (metodo.customResponse) {
					if (!m.isVoid)
						throw new NullPointerException(
								"No se puede tener un servicio con custom response (HttpResponse) y un retorno no void");
					$(controller + "." + m.name() + "(" + methodParas + ");");
				} else if (m.isVoid) {
					$(controller + "." + m.name() + "(" + methodParas + ");");
					if (metodo.hasStringBuider)
						beforeWrite.run();
					else
						beforeWrite.run();
					if (m.name().equals("logout"))
						$("resp.addCookie(setAge(new Cookie(\"token\", \"\"), 0));");
				} else if (metodo.hasStringBuider) {
					throw new NullPointerException(
							"Error, tipos incompatibles. No se puede tener un metodo con retorno y con un parametro StringBuilder");
				} else if (m.getReturnType().is(String.class, Long.class, Integer.class)) {
					$(m.getReturnType().getSimpleName() + " $salida = " + controller + "." + m.name() + "("
							+ methodParas + ");");
					beforeWrite.run();
				} else if (AbsClientGenerator.SUPPORTED_NATIVE_TYPES.stream().filter(p -> m.getReturnType().is(p))
						.findFirst().isPresent()) {
					$(m.getReturnType().getSimpleName() + " $salida = " + controller + "." + m.name() + "("
							+ methodParas + ");");
					beforeWrite.run();
				} else if (m.getReturnType().isAnnotationPresent(jEntity.class)
						|| m.getReturnType().isAnnotationPresent(jSerializable.class)) {
					$(m.getReturnType().name() + " $salida = " + controller + "." + m.name() + "(" + methodParas
							+ ");");
					beforeWrite.run();
				} else if (m.getReturnType().is(JSONArray.class, JSONObject.class)) {
					$(m.getReturnType().name() + " $salida = " + controller + "." + m.name() + "(" + methodParas
							+ ");");
					beforeWrite.run();
				} else if (m.getReturnType().isSubclassOf(Map.class)) {
					IJType primerTipo = m.getReturnType().getInnerTypes().get(0);
					IJType segundoTipo = m.getReturnType().getInnerTypes().get(1);
					$(m.getReturnType().name() + "<" + primerTipo.name() + "," + segundoTipo.name() + "> $salida = "
							+ controller + "." + m.name() + "(" + methodParas + ");");
					beforeWrite.run();
				} else if (m.getReturnType().isArray()) {
					IJType primerTipo = m.getReturnType().getInnerTypes().get(0);
					if (primerTipo.isAnyAnnotationPresent(jEntity.class, jSerializable.class)
							|| primerTipo.isPrimitiveObjectType() || primerTipo.isPrimitive()) {
						$($(m.getReturnType()) + " $salida = " + controller + "." + m.name() + "(" + methodParas
								+ ");");
						beforeWrite.run();
					} else if (primerTipo.is(String.class)) {
						$("String $salida = jcrystal.JSONUtils.jsonQuote(" + controller + "." + m.name() + "("
								+ methodParas + "));");
						beforeWrite.run();
					} else
						throw new NullPointerException(
								"Error, tipos incompatibles. No se puede tener un metodo con retorno "
										+ m.getReturnType());
				} else if (m.getReturnType().isIterable() || m.getReturnType().is(Page.class)) {
					IJType primerTipo = m.getReturnType().getInnerTypes().get(0);
					if (primerTipo.isAnyAnnotationPresent(jEntity.class, jSerializable.class)
							|| primerTipo.is(String.class) || primerTipo.isPrimitiveObjectType()) {
						$(m.getReturnType().name() + "<" + primerTipo.name() + "> $salida = " + controller + "."
								+ m.name() + "(" + methodParas + ");");
						beforeWrite.run();
					} else
						throw new NullPointerException(
								"Error, tipos incompatibles. No se puede tener un metodo con retorno "
										+ m.getReturnType());
				} else if (m.getReturnType().isTupla()) {
					$($(m.getReturnType()) + " $salida = " + controller + "." + m.name() + "(" + methodParas + ");");
					beforeWrite.run();
				} else if (m.getReturnType().is(File.class)) {
					$("java.io.File $salida = " + controller + "." + m.name() + "(" + methodParas + ");");
					$if("$salida != null && $salida.exists()", () -> {
						beforeWrite.run();
					}).$else(() -> {
						$("send404(resp);");
					});
				} else if (m.getReturnType().is(FileDownloadDescriptor.class)) {
					$("jcrystal.server.FileDownloadDescriptor $salida = " + controller + "." + m.name() + "("
							+ methodParas + ");");
					$if("$salida != null", () -> {
						beforeWrite.run();
					}).$else(() -> {
						$("send404(resp);");
					});
				} else
					throw new NullPointerException(
							"Error, tipos incompatibles. No se puede tener un metodo con retorno " + padre.name()
									+ " : " + m.getReturnType());
			}
		};
	}

	public void writeResponse(AbsCodeBlock b) {
		b.new B() {
			{
				if (metodo.getReturnType().is(FileDownloadDescriptor.class)) {
					$("jcrystal.db.storage.StorageUtils.serve($salida, resp);");
				} else if (metodo.getReturnType().is(File.class)) {
					$("resp.setStatus(200);");
					$("try(java.io.FileInputStream fis = new java.io.FileInputStream($salida))", () -> {
						$("jcrystal.utils.ServletUtils.copy(8*1024, fis, resp.getOutputStream());");
					});
				} else {
					$("java.io.PrintWriter _pw = resp.getWriter();");
					if (m.isVoid) {
						$("_pw.print(\"{\\\"success\\\":1}\");");
					} else if (metodo.getReturnType().is(String.class, Long.class, Integer.class)) {
						$("_pw.print(\"{\\\"success\\\":1,\\\"r\\\":\"+jcrystal.JSONUtils.jsonQuote($salida)+\"}\");");
					} else if (AbsClientGenerator.SUPPORTED_NATIVE_TYPES.stream()
							.filter(p -> metodo.getReturnType().is(p)).findFirst().isPresent()) {
						$("_pw.print(\"{\\\"success\\\":1, \\\"r\\\":\"+$salida+\"}\");");
					} else if (metodo.getReturnType().isAnnotationPresent(jEntity.class)
							|| metodo.getReturnType().isAnnotationPresent(jSerializable.class)) {
						$ifNotNull("$salida", () -> {
							if (metodo.getReturnType().isAnnotationPresent(jSerializable.class)) {
								/*
								 * if(metodo.getReturnType().isAnnotationPresent(EmbeddedResponse.class))
								 * beforeWrite.accept(()->{
								 * $("_pw.print(\"{\\\"success\\\":1,\"+salida.toJsonEmbedded()+\"}\");"); });
								 * else {
								 */
								$("_pw.print(\"{\\\"success\\\":1, \\\"r\\\":\");");
								$(metodo.getReturnType().name() + ".Serializer.toJson(_pw, $salida);");
								$("_pw.print(\"}\");");
								// }
							} else {
								$("_pw.print(\"{\\\"success\\\":1, \\\"r\\\":\");");
								$(context.data.entidades.get(metodo.getReturnType()).serializerClassName() + ".toJson"
										+ metodo.getJsonLevel(metodo.getReturnType()).baseName() + "(_pw, $salida, "
										+ (token != null ? token.name + ".rol()" : "0") + ");");
								$("_pw.print(\"}\");");
							}
							if (context.data.isSecurityToken(metodo.getReturnType()))
								$("resp.addCookie(setPath(setAge(new Cookie(\"token\", $salida.token()), 3600*24),\"/\"));");
							else if (metodo.getReturnType().isAnnotationPresent(LoginResultClass.class))
								$("resp.addCookie(setPath(setAge(new Cookie(\"token\", $salida.token), 3600*24),\"/\"));");

						});
						$else(() -> {
							$("_pw.print(\"{\\\"success\\\":1, \\\"r\\\":null}\");");
						});
					} else if (metodo.getReturnType().is(JSONArray.class, JSONObject.class)) {
						$("_pw.print(\"{\\\"success\\\":1, \\\"r\\\":\"+$salida.toString()+\"}\");");
					} else if (metodo.getReturnType().isSubclassOf(Map.class)) {
						IJType primerTipo = metodo.getReturnType().getInnerTypes().get(0);
						IJType segundoTipo = metodo.getReturnType().getInnerTypes().get(1);
						$("_pw.print(\"{\\\"success\\\":1, \\\"r\\\":\");");
						if (segundoTipo.is(Double.class))
							$("jcrystal.JSONUtils.Map.jsonQuote" + primerTipo.getSimpleName() + ""
									+ segundoTipo.getSimpleName() + "(_pw, $salida);");
						else
							throw new NullPointerException("Map not supported " + metodo.getReturnType());
						$("_pw.print(\"}\");");
					} else if (metodo.getReturnType().isArray()) {
						IJType primerTipo = metodo.getReturnType().getInnerTypes().get(0);
						$("_pw.print(\"{\\\"success\\\":1, \\\"r\\\":\");");
						if (primerTipo.isAnnotationPresent(jEntity.class))
							$(context.data.entidades.get(primerTipo).serializerClassName() + ".toJson"
									+ metodo.getJsonLevel(primerTipo).baseName() + primerTipo.getSimpleName() + "(_pw, "
									+ (token != null ? token.name + ".rol(), " : "0, ") + " $salida);");
						else if (primerTipo.isAnyAnnotationPresent(jSerializable.class)) {
							$(primerTipo.name() + ".Serializer.toJson" + primerTipo.getSimpleName()
									+ "(_pw, $salida);");
						} else if (primerTipo.is(String.class) || primerTipo.isPrimitive()
								|| primerTipo.isPrimitiveObjectType()) {
							$("_pw.print(jcrystal.JSONUtils.jsonQuote($salida));");
						} else
							throw new NullPointerException(
									"Error, tipos incompatibles. No se puede tener un metodo con retorno "
											+ metodo.getReturnType());
						$("_pw.print(\"}\");");
					} else if (metodo.getReturnType().isIterable() || metodo.getReturnType().is(Page.class)) {
						$ifNotNull("$salida", () -> {
							IJType primerTipo = metodo.getReturnType().getInnerTypes().get(0);
							String salida;
							if (metodo.getReturnType().is(Page.class)) {
								salida = "$salida.getResult()";
								$("_pw.print(\"{\\\"success\\\":1, \\\"r\\\":{\\\"list\\\":\");");
								$("$salida.setPrevCursor(req.getHeader(\"nextToken" + HashUtils.shortMD5(metodo.getPath(null)) + "\"));");
							} else {
								salida = "$salida";
								$("_pw.print(\"{\\\"success\\\":1, \\\"r\\\":\");");
							}
							if (primerTipo.isAnnotationPresent(jEntity.class))
								$(context.data.entidades.get(primerTipo).serializerClassName() + ".toJson"
										+ metodo.getJsonLevel(primerTipo).baseName() + "List(_pw, "
										+ (token != null ? token.name + ".rol()," : "0,") + " " + salida + ");");
							else if (primerTipo.isAnnotationPresent(jSerializable.class)) {
								$(primerTipo.name() + ".Serializer.toJsonList(_pw, " + salida + ");");
							} else if (primerTipo.is(String.class) || primerTipo.isPrimitiveObjectType()) {
								$("jcrystal.JSONUtils.jsonQuote" + primerTipo.getSimpleName() + "(_pw, " + salida
										+ ");");
							} else
								throw new NullPointerException(
										"Error, tipos incompatibles. No se puede tener un metodo con retorno "
												+ metodo.getReturnType());
							if (metodo.getReturnType().is(Page.class)) {
								$ifNotNull("$salida.getNewCursor()", () -> {
									$("_pw.print(\",\\\"nextToken\\\":\");");
									$("_pw.print(jcrystal.JSONUtils.jsonQuote($salida.getNewCursor()));");
								});
								$("_pw.print(\"}}\");");
							} else
								$("_pw.print(\"}\");");
						});
						$else(() -> {
							$("_pw.print(\"{\\\"success\\\":1, \\\"r\\\":null}\");");
						});
					} else if (metodo.getReturnType().isTupla()) {
						final List<IJType> tipos = metodo.getReturnType().getInnerTypes();
						$("_pw.print(\"{\\\"success\\\":1, \\\"r\\\":[\");");
						IntStream.range(0, tipos.size()).forEach(e -> {
							if (e != 0)
								$("_pw.print(\",\");");
							if (tipos.get(e).getInnerTypes().isEmpty()) {
								IJType claseTipo = tipos.get(e);
								$if("$salida.v" + e + " == null", () -> {
									$("_pw.print(\"null\");");
								}).$else(() -> {
									if (claseTipo.is(GeoPt.class))
										$("_pw.print(jcrystal.JSONUtils.jsonQuote($salida.v" + e + "));");
									else if (claseTipo.isAnnotationPresent(jEntity.class))
										$(context.data.entidades.get(claseTipo).serializerClassName() + ".toJson"
												+ metodo.getJsonLevel(claseTipo).baseName() + "(_pw, $salida.v" + e
												+ ", " + (token != null ? token.name + ".rol()" : "0") + ");");
									else if (claseTipo.isAnnotationPresent(jSerializable.class))
										$(claseTipo.name() + ".Serializer.toJson(_pw, $salida.v" + e + ");");
									else if (claseTipo.is(String.class))
										$("_pw.print(jcrystal.JSONUtils.jsonQuote($salida.v" + e + "));");
									else if (claseTipo.isPrimitiveObjectType())
										$("_pw.print($salida.v" + e + ");");
									else
										throw new NullPointerException(
												"Error, tipos incompatibles. No se puede tener un metodo con retorno "
														+ metodo.getReturnType());
									if (context.data.isSecurityToken(claseTipo))
										$("resp.addCookie(setPath(setAge(new Cookie(\"token\", $salida.v" + e
												+ ".token()), 3600*24),\"/\"));");
									else if (claseTipo.isAnnotationPresent(LoginResultClass.class))
										$("resp.addCookie(setPath(setAge(new Cookie(\"token\", $salida.v" + e
												+ ".token), 3600*24),\"/\"));");
								});

							} else {
								IJType claseTipo = tipos.get(e);
								String valueAccessor = "$salida.v" + e;
								if (claseTipo.isSubclassOf(FileWrapperResponse.class)) {
									if (claseTipo.getInnerTypes().get(0).isIterable()) {
										IJType subsubClaseTipo = claseTipo.getInnerTypes().get(0).getInnerTypes()
												.get(0);
										if (subsubClaseTipo.isAnnotationPresent(jEntity.class)) {
											EntityClass entity = context.data.entidades.get(subsubClaseTipo);
											$(entity.serializerClassName() + ".toJson"
													+ metodo.getJsonLevel(subsubClaseTipo).baseName()
													+ entity.clase.getSimpleName() + "(_pw, "
													+ (token != null ? token.name + ".rol()" : "0") + ", "
													+ valueAccessor + ");");
										} else
											throw new NullPointerException(
													"Error, tipos incompatibles. No se puede tener un metodo con retorno "
															+ metodo.getReturnType());
									} else
										throw new NullPointerException(
												"Error, tipos incompatibles. No se puede tener un metodo con retorno "
														+ metodo.getReturnType());
								} else if (claseTipo.isSubclassOf(Map.class)) {
									IJType primerTipo = claseTipo.getInnerTypes().get(0);
									IJType segundoTipo = claseTipo.getInnerTypes().get(1);
									if (segundoTipo.is(Double.class))
										$("jcrystal.JSONUtils.Map.jsonQuote" + primerTipo.getSimpleName() + "Map(_pw, "
												+ valueAccessor + ");");
									else
										throw new NullPointerException("Map not supported " + metodo.getReturnType());
								} else if (claseTipo.isSubclassOf(List.class)) {
									IJType subClaseTipo = claseTipo.getInnerTypes().get(0);
									if (subClaseTipo.isAnnotationPresent(jEntity.class))
										$(context.data.entidades.get(subClaseTipo).serializerClassName() + ".toJson"
												+ metodo.getJsonLevel(subClaseTipo).baseName() + "List(_pw, "
												+ (token != null ? token.name + ".rol()" : "0") + ", " + valueAccessor
												+ ");");
									else if (subClaseTipo.isAnnotationPresent(jSerializable.class))
										$(subClaseTipo.name() + ".Serializer.toJson" + subClaseTipo.getSimpleName()
												+ "(_pw, " + valueAccessor + ");");
									else
										throw new NullPointerException(
												"Error, tipos incompatibles. No se puede tener un metodo con retorno "
														+ metodo.getReturnType());
								} else
									throw new NullPointerException(
											"Error, tipos incompatibles. No se puede tener un metodo con retorno "
													+ claseTipo);
							}
						});
						$("_pw.print(\"]}\");");
					} else
						throw new NullPointerException(
								"Error, tipos incompatibles. No se puede tener un metodo con retorno "
										+ metodo.getReturnType());
				}
			}
		};
	}

	private void putResponseHeaders(AbsCodeBlock b) {
		if (!metodo.responseHeaders.isEmpty())
			b.new B() {
				{
					metodo.responseHeaders.forEach(f -> {
						$("resp.setHeader(\"" + f.name() + "\", \"" + f.value() + "\");");
					});
				}
			};
	}

	public String getParamFromHttpRequest(IJType type, String var, String name, boolean required, String defaultValue) {
		String methodName = StringUtils.capitalize(type.getSimpleName());
		if (type.isPrimitiveObjectType())
			methodName = methodName + "Obj";
		if (required)
			return type.getSimpleName() + " " + var + " = get" + methodName + "(req, \"" + name + "\");";
		else if (defaultValue != null)
			return type.getSimpleName() + " " + var + " = opt" + methodName + "(req, \"" + name + "\", " + defaultValue
					+ ");";
		else
			return type.getSimpleName() + " " + var + " = opt" + methodName + "(req, \"" + name + "\");";
	}

	public String getParams(AbsCodeBlock METODOS, ContentType content) {
		StringSeparator retorno = new StringSeparator(", ");
		if (metodo.getPathType().isPostLike() && metodo.hasPostParam()) {
			if (content == ContentType.MultipartForm) {
				METODOS.add("Part $bodyPart = req.getPart(\"$body\");");
				METODOS.add(
						"JSONObject $body = new JSONObject(new JSONTokener(req.getCharacterEncoding()==null?new java.io.InputStreamReader("
								+ ("$bodyPart.getInputStream()") + "):new java.io.InputStreamReader("
								+ ("$bodyPart.getInputStream()") + ", req.getCharacterEncoding())));");
			} else if (content == ContentType.JSON)
				METODOS.add(
						"JSONObject $body = new JSONObject(new JSONTokener(req.getCharacterEncoding()==null?new java.io.InputStreamReader("
								+ ("req.getInputStream()") + "):new java.io.InputStreamReader("
								+ ("req.getInputStream()") + ", req.getCharacterEncoding())));");

		}

		// parametrosClase
		metodo.parametros.stream().filter(f -> f.tipoRuta == HttpType.SERVER).forEach(param -> {// Para parametros que
																								// se tienen que
																								// declarar antes que
																								// cualquier otro
			if (param.type().is(SignInInfo.class))
				METODOS.add("jcrystal.security.SignInInfo " + param.nombre + " = new jcrystal.security.SignInInfo();");
		});
		for (JCrystalWebServiceParam param : metodo.parametros) {
			final IJType type = param.type();
			Consumer<String> addParam = name -> {
				if (!param.classParam)
					retorno.add(name);
			};
			if (param.tipoRuta == HttpType.SERVER) {
				if (type.name().equals("javax.persistence.EntityManager"))
					addParam.accept("jpaEmf.createEntityManager()");
				else if (type.is(HttpServletRequest.class))
					addParam.accept("req");
				else if (type.is(HttpServletResponse.class))
					addParam.accept("resp");
				else if (type.is(SignInInfo.class))
					addParam.accept(param.nombre);
				else if (type.is(PrintWriter.class))
					addParam.accept("resp.getWriter()");
				else if (type.is(StringBuilder.class)) {
					METODOS.add("StringBuilder outBuilder = new StringBuilder();");
					addParam.accept("outBuilder");
				} else
					throw new NullPointerException(
							metodo.getPath(null) + ": Parametro no reconocido " + param.tipoRuta + " " + type);
			} else if (param.tipoRuta == HttpType.HEADER) {
				if (param.isAnnotationPresent(HeaderParam.class)) {
					if (type.is(String.class)) {
						METODOS.add("String " + param.nombre + " = req.getHeader(\"" + param.nombre + "\");");
						addParam.accept(param.nombre);
					} else
						throw new NullPointerException("Ussuported HeaderParam type : " + param.p);
				} else if (param.type().is(ClientId.class))
					addParam.accept(ClientId.class.getName() + ".valueOf(req.getHeader(\"" + param.nombre + "\"))");
				else if (type.is(Authorization.class)) {
					METODOS.add(Authorization.class.getName() + " " + param.nombre + " = new"
							+ Authorization.class.getName() + "(getToken(req));");
					addParam.accept(param.nombre);
				} else
					throw new NullPointerException(
							metodo.getPath(null) + ": Parametro no reconocido " + param.tipoRuta + " " + type);
			} else if (param.tipoRuta == HttpType.SESSION) {
				if (param.securityToken)
					addParam.accept(param.nombre);
				else
					throw new NullPointerException(
							metodo.getPath(null) + ": Parametro no reconocido " + param.tipoRuta + " " + type);
			} else if (type.isAnnotationPresent(jEntity.class)) {
				EntityClass entidad = metodo.context.data.entidades.get(type);
				String tipo = entidad.clase.name();
				if (entidad.hasAccountFields && metodo.hasAuthTokenResponse()) {
					UtilsGeneradorAuth.generateLoginUser(METODOS, metodo, param);
				} else {
					if (entidad.key.isSimple()) {
						EntityField key = entidad.key.getLlaves().get(0);
						String nombre = param.getWsParamName();
						if (param.tipoRuta == HttpType.POST) {
							if (key.type().is(Long.class, long.class)) {
								if (param.required)
									METODOS.add("long " + nombre + " = $body.getLong(\"" + nombre + "\");");
								else
									METODOS.add("Long " + nombre + " = $body.optLong(\"" + nombre + "\");");
							} else if (key.type().is(String.class)) {
								METODOS.add("String " + nombre + " = $body.optString(\"" + nombre + "\");");
							} else
								throw new NullPointerException("Entidad no reconocido 2" + type.getSimpleName());
						} else {
							if (key.type().is(Long.class, long.class)) {
								if (param.required)
									METODOS.add("long " + nombre + " = Long.parseLong(req.getParameter(\"" + nombre
											+ "\"));");
								else
									METODOS.add("Long " + nombre + " = optLong(req, \"" + nombre + "\");");
							} else if (key.type().is(String.class)) {
								METODOS.add("String " + nombre + " = req.getParameter(\"" + nombre + "\");");
							} else
								throw new NullPointerException("Entidad no reconocido 2" + type.getSimpleName());
						}
						String get = param.required ? "tryGet" : "get";
						METODOS.add(tipo + " " + param.nombre + " = " + tipo + "." + get + "(" + nombre + ");");
					} else {
						String methodName = param.required ? "getJSONObject" : "optJSONObject";
						if (param.tipoRuta == HttpType.POST) {
							METODOS.add(tipo + " " + param.nombre + " = " + entidad.clase.name() + ".get("
									+ entidad.clase.name() + ".Post.getKey($body." + methodName + "(\"" + param.nombre
									+ "\")));");
						} else
							throw new NullPointerException(
									"A compound key entity can only be recieved by post wss " + tipo);
					}
				}
				addParam.accept(param.nombre);
			} else if (param.tipoRuta == HttpType.GET) {
				if (type.isPrimitive() || type.isPrimitiveObjectType() || type.is(String.class)) {
					METODOS.add(getParamFromHttpRequest(type, param.nombre, param.nombre, param.required,
							param.valorDefecto));
					addParam.accept(param.nombre);
				} else if (type.isEnum()) {
					IJType enumIdType = EnumUtils.getIdType(type);
					METODOS.add(getParamFromHttpRequest(enumIdType, "$id_" + param.nombre, param.nombre, param.required,
							param.valorDefecto));
					if (enumIdType.nullable())
						METODOS.add(type.name() + " " + param.nombre + " = $id_" + param.nombre + "==null?null:"
								+ type.prefixName("Utils") + ".fromId($id_" + param.nombre + ");");
					else
						METODOS.add(type.name() + " " + param.nombre + " = " + type.prefixName("Utils") + ".fromId($id_"
								+ param.nombre + ");");
					addParam.accept(param.nombre);
				} else if (type.isJAnnotationPresent(CrystalDate.class)) {
					METODOS.add("String $" + param.nombre + " = req.getParameter(\"" + param.nombre + "\");");
					METODOS.add(type.getSimpleName() + " " + param.nombre + " = $" + param.nombre + " == null || ($"
							+ param.nombre + "=$" + param.nombre + ".trim()).isEmpty() ? null : new "
							+ type.getSimpleName() + "($" + param.nombre + ");");
					addParam.accept(param.nombre);
				} else
					throw new NullPointerException(
							metodo.getPath(null) + ": Parametro no reconocido " + param.tipoRuta + " " + type);
			} else if (param.tipoRuta == HttpType.POST) {
				if (content == ContentType.UrlForm) {
					if (type.isAnnotationPresent(jSerializable.class)) {
						String tipo = type.name();
						METODOS.add(tipo + " " + param.nombre + " = new " + tipo + "(req);");
						addParam.accept(param.nombre);
					} else if (type.isAnnotationPresent(Post.class)) {
						String tipo = METODOS.$(param.type());
						if (param.required)
							METODOS.add(tipo + " " + param.nombre + " = " + tipo + ".getFrom"
									+ param.type().getSimpleName() + "(req);");
						else
							METODOS.add(tipo + " " + param.nombre + " = " + tipo + ".getFrom"
									+ param.type().getSimpleName() + "(req);");
						addParam.accept(param.nombre);
					} else
						throw new NullPointerException("Not supported type : " + type.getSimpleName());
				} else if (content == ContentType.JSON || content == ContentType.MultipartForm) {
					if (type.is(FileUploadDescriptor.class)) {
						addParam.accept(param.nombre);
						METODOS.add("javax.servlet.http.Part $part" + param.name() + " = req.getPart(\"" + param.name()
								+ "\");");
						if (param.required) {
							METODOS.$ifNull("$part" + param.name(), () -> {
								METODOS.add("throw new InternalException(2, \"Invalid request: " + param.nombre
										+ " is required.\");");
							});
						}
						METODOS.add("jcrystal.server.FileUploadDescriptor " + param.nombre + " = $part" + param.name()
								+ " == null ? null : new jcrystal.server.FileUploadDescriptor($part" + param.name()
								+ ");");
					} else if (type.is(JSONObject.class)) {
						if (param.nombre.equals("body"))
							addParam.accept("$body");
						else
							addParam.accept("$body.getJSONObject(\"" + param.nombre + "\")");
					} else if (type.is(JSONArray.class)) {
						addParam.accept("$body.getJSONArray(\"" + param.nombre + "\")");
					} else if (type.is(String.class)) {
						METODOS.add(
								"String " + param.nombre + " = $body.has(\"" + param.nombre + "\") && !$body.isNull(\""
										+ param.nombre + "\") ? $body.optString(\"" + param.nombre + "\") : null;");
						if (param.required)
							METODOS.add("if(" + param.nombre + " == null || " + param.nombre
									+ ".isEmpty())throw new InternalException(2, \"Invalid request: " + param.nombre
									+ " is required.\");");
						addParam.accept(param.nombre);
					} else if (type.isPrimitiveObjectType()) {
						if (param.valorDefecto != null)
							METODOS.add(METODOS.$(type) + " " + param.nombre + " = $body.has(\"" + param.nombre
									+ "\") && !$body.isNull(\"" + param.nombre + "\") ? $body.opt"
									+ StringUtils.capitalize(type.getPrimitiveType().getSimpleName()) + "(\""
									+ param.nombre + "\", " + param.valorDefecto + ") : " + param.valorDefecto + ";");
						else
							METODOS.add(METODOS.$(type) + " " + param.nombre + " = $body.has(\"" + param.nombre
									+ "\") && !$body.isNull(\"" + param.nombre + "\") ? $body.opt"
									+ StringUtils.capitalize(type.getPrimitiveType().getSimpleName()) + "(\""
									+ param.nombre + "\", 0):null;");
						addParam.accept(param.nombre);
					} else if (type.isPrimitive()) {
						if (param.valorDefecto != null)
							METODOS.add(METODOS.$(type) + " " + param.nombre + " = $body.has(\"" + param.nombre
									+ "\") && !$body.isNull(\"" + param.nombre + "\") ? $body.opt"
									+ StringUtils.capitalize(type.getPrimitiveType().getSimpleName()) + "(\""
									+ param.nombre + "\", " + param.valorDefecto + ") : " + param.valorDefecto + ";");
						else
							METODOS.add(METODOS.$(type) + " " + param.nombre + " = $body.has(\"" + param.nombre
									+ "\") && !$body.isNull(\"" + param.nombre + "\") ? $body.opt"
									+ StringUtils.capitalize(type.getPrimitiveType().getSimpleName()) + "(\""
									+ param.nombre + "\", " + GlobalTypes.defaultValues.get(type) + ") : "
									+ GlobalTypes.defaultValues.get(type) + ";");
						addParam.accept(param.nombre);
					} else if (type.is(LongText.class)) {
						METODOS.add("jcrystal.entity.types.LongText " + param.nombre
								+ " = jcrystal.entity.types.LongText.create($body.optString(\"" + param.nombre
								+ "\"));");
						if (param.required)
							METODOS.add("if(" + param.nombre + " == null || " + param.nombre
									+ ".isEmpty())throw new InternalException(2, \"Invalid request: " + param.nombre
									+ " is required.\");");
						addParam.accept(param.nombre);
					} else if (type.isAnnotationPresent(Post.class)) {
						String tipo = METODOS.$(param.type());
						if (param.required)
							METODOS.add(
									tipo + " " + param.nombre + " = " + tipo + ".getFrom" + param.type().getSimpleName()
											+ "($body.getJSONObject(\"" + param.nombre + "\"));");
						else
							METODOS.add(
									tipo + " " + param.nombre + " = " + tipo + ".getFrom" + param.type().getSimpleName()
											+ "($body.optJSONObject(\"" + param.nombre + "\"));");
						addParam.accept(param.nombre);
					} else if (type.isJAnnotationPresent(InternalEntityKey.class)) {
						String tipo = type.getJAnnotation(InternalEntityKey.class).parentEntity().name();
						if (param.required)
							METODOS.add(METODOS.$(param.type()) + " " + param.nombre + " = " + tipo
									+ ".Post.getKey($body.getJSONObject(\"" + param.nombre + "\"));");
						else
							METODOS.add(METODOS.$(param.type()) + " " + param.nombre + " = " + tipo
									+ ".Post.getKey($body.getJSONObject(\"" + param.nombre + "\"));");
						addParam.accept(param.nombre);
					} else if (type.isAnnotationPresent(jSerializable.class)) {
						String tipo = type.name();
						if (param.nombre.equals("body"))
							METODOS.add(tipo + " " + param.nombre + " = new " + tipo + "($body);");
						else
							METODOS.add(tipo + " " + param.nombre + " = new " + tipo + "($body.getJSONObject(\""
									+ param.nombre + "\"));");
						addParam.accept(param.nombre);
					} else if (type.isArray()) {
						IJType innerType = type.getInnerTypes().get(0);
						if (innerType.is(byte.class)) {
							METODOS.add("String " + param.nombre + "_str = $body.getString(\"" + param.nombre + "\");");
							METODOS.add(innerType.name() + " " + param.nombre + "[] = null;");
							METODOS.add("if(" + param.nombre + "_str != null){");
							METODOS.add("\t" + param.nombre + " = java.util.Base64.getDecoder().decode(" + param.nombre
									+ "_str);");
							METODOS.add("}");
						} else {
							METODOS.add("org.json.JSONArray " + param.nombre + "__array = $body.getJSONArray(\""
									+ param.nombre + "\");");
							METODOS.add(innerType.name() + " " + param.nombre + "[] = new " + innerType.name() + "["
									+ param.nombre + "__array.length()];");
							METODOS.add("for(int $e = 0; $e < " + param.nombre + ".length; $e++){");
							if (innerType.isAnnotationPresent(jSerializable.class)) {
								METODOS.add("	" + param.nombre + "[$e] = new " + innerType.name() + "(" + param.nombre
										+ "__array.getJSONObject($e));");
							} else if (innerType.isPrimitiveObjectType() || innerType.isPrimitive()) {
								METODOS.add("	" + param.nombre + "[$e] = " + param.nombre + "__array.get"
										+ StringUtils.capitalize(innerType.getSimpleName()) + "($e);");
							} else
								throw new NullPointerException();
							METODOS.add("}");
						}
						addParam.accept(param.nombre);
					} else if (type.isIterable()) {
						final IJType tipo = type.getInnerTypes().get(0);

						if (tipo.isEnum())
							METODOS.add("java.util.List<" + METODOS.$(tipo) + "> " + param.nombre + " = "
									+ tipo.prefixName("Utils") + ".from($body.optJSONArray(\"" + param.nombre
									+ "\"));");
						else if (tipo.isAnnotationPresent(Post.class)) {
							METODOS.add("java.util.List<" + METODOS.$(tipo) + "> " + param.nombre + " = "
									+ METODOS.$(tipo) + ".getFrom" + tipo.getSimpleName() + "($body.optJSONArray(\""
									+ param.nombre + "\"));");
						} else {
							METODOS.add("org.json.JSONArray " + param.nombre + "__array = $body.optJSONArray(\""
									+ param.nombre + "\");");
							METODOS.add("java.util.ArrayList<" + tipo.name().replace("$", ".") + "> " + param.nombre
									+ " = new java.util.ArrayList<>(" + param.nombre + "__array==null?0:" + param.nombre
									+ "__array.length());");
							METODOS.add("if(" + param.nombre + "__array != null){");
							METODOS.add(
									"	for(int $e = 0, $l = " + param.nombre + "__array.length(); $e < $l; $e++){");
							if (tipo.isPrimitiveObjectType()) {
								METODOS.add("		" + param.nombre + ".add(new " + tipo.name() + "(" + param.nombre
										+ "__array.get" + tipo.getSimpleName() + "($e)));");
							} else if (tipo.isAnnotationPresent(jSerializable.class)) {
								METODOS.add("		" + param.nombre + ".add(new " + tipo.name() + "(" + param.nombre
										+ "__array.getJSONObject($e)));");
							} else if (tipo.isAnnotationPresent(jEntity.class)) {
								EntityClass entidad = metodo.context.data.entidades.get(tipo);
								StringSeparator args = new StringSeparator(", ");
								if (!entidad.key.isSimple())
									METODOS.add("	org.json.JSONArray __entKey = __array.getJSONArray($e);");
								int pos = 0;
								for (EntityField key : entidad.key.getLlaves()) {
									if (entidad.key.isSimple()) {
										if (key.type().is(Long.class, long.class)) {
											args.add(param.nombre + "__array.getLong($e)");
										} else if (entidad.key.getLlaves().get(0).type().is(String.class)) {
											args.add(param.nombre + "__array.getString($e)");
										} else
											throw new NullPointerException(
													"Entidad no reconocido 2" + type.getSimpleName());
									} else {
										if (key.type().is(Long.class, long.class)) {
											args.add("__entKey.getLong(" + pos + ")");
										} else if (entidad.key.getLlaves().get(0).type().is(String.class)) {
											args.add("__entKey.getString(" + pos + ")");
										} else
											throw new NullPointerException(
													"Entidad no reconocido 2" + type.getSimpleName());
									}
								}
								METODOS.add(
										param.nombre + ".add(" + METODOS.$(entidad.clase) + ".tryGet(" + args + "));");
							} else
								throw new NullPointerException("Entidad no reconocida " + type.getSimpleName());
							METODOS.add("}");
							METODOS.add("}");
						}
						addParam.accept(param.nombre);
					} else
						throw new NullPointerException("Not supported type : " + type.getSimpleName());
				} else
					throw new NullPointerException("Not supported content type : " + content);
			}
		}
		return retorno.toString();
	}
}

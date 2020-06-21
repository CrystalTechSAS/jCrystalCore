package jcrystal.reflection;

import static java.lang.reflect.Modifier.PUBLIC;
import static java.lang.reflect.Modifier.STATIC;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.json.JSONArray;
import org.json.JSONObject;

import jcrystal.annotations.internal.model.db.InternalEntityKey;
import jcrystal.clients.android.AndroidClient;
import jcrystal.datetime.CrystalDateMilis;
import jcrystal.datetime.DateType;
import jcrystal.entity.types.CreationTimestamp;
import jcrystal.entity.types.LongText;
import jcrystal.entity.types.ModificationTimestamp;
import jcrystal.json.JsonLevel;
import jcrystal.lang.Language;
import jcrystal.lang.elements.CustomAccesor;
import jcrystal.main.data.ClientContext;
import jcrystal.model.server.SerializableClass;
import jcrystal.model.server.db.EntityClass;
import jcrystal.reflection.annotations.CrystalDate;
import jcrystal.reflection.annotations.Post;
import jcrystal.reflection.annotations.jEntity;
import jcrystal.reflection.annotations.com.jSerializable;
import jcrystal.types.IJType;
import jcrystal.types.JClass;
import jcrystal.types.JType;
import jcrystal.types.JVariable;
import jcrystal.types.utils.GlobalTypes;
import jcrystal.types.vars.IAccessor;
import jcrystal.utils.SerializeLevelUtils;
import jcrystal.utils.StringUtils;
import jcrystal.utils.context.CodeGeneratorContext;
import jcrystal.utils.context.CodeGeneratorContext.ContextType;
import jcrystal.utils.langAndPlats.AbsCodeBlock;
import jcrystal.utils.langAndPlats.AbsCodeBlock.PL;
import jcrystal.utils.langAndPlats.JavaCode;

/**
* Created by G on 11/14/2016.
*/
public class GeneradorToJson {
	private final ClientContext context;
	public GeneradorToJson(ClientContext context) {
		this.context = context;
	}
	public void jsonificarClase(SerializableClass clase) throws Exception {
		generarSerializerClass(clase.jclass);
		new JavaCode(1) {{
			$("public " + clase.jclass.simpleName + "()", ()->{});
			$("public " + clase.jclass.simpleName + "(org.json.JSONObject json)throws org.json.JSONException", ()->{
				clase.fields().forEach(f->{
					context.utils.extrators.jsonObject.procesarCampo(this, clase.jclass, f.accessor().$this());
				});
			});
			$("public " + clase.jclass.simpleName + "(javax.servlet.http.HttpServletRequest req)", ()->{
				clase.fields().forEach(f->{
					context.utils.extrators.formData.procesarCampo(this, clase.jclass, f.accessor().$this());
				});
			});
			$("public static class Serializer extends Serializer"+clase.jclass.simpleName+"{};");
			AndroidClient.generateFromListJson(this, clase.jclass, null);
			context.output.addSection(clase.jclass, "FROMJSON", this);
		}};
	}
	private void generarSerializerClass(final JClass clase) throws IOException{
		new JavaCode(){{
			$("package "+clase.getPackageName()+";");
			$("import jcrystal.datetime.DateType;");
			$("import jcrystal.PrintWriterUtils;");
			$("@SuppressWarnings(\"unused\")");
			$("public class Serializer" + clase.getSimpleName(), ()->{
				generateJsonify(clase, this, null, clase.attributes.stream().map(f ->{
					if(f.type().isJAnnotationPresent(CrystalDate.class))
						return f.accessor().prefix("objeto.").subProperty(GlobalTypes.DATE, "toDate()");
					return f.accessor().prefix("objeto.");
				}).collect(Collectors.toList()));
			});
			context.output.exportFile(this, clase.getPackageName().replace(".", "/")+"/Serializer"+clase.getSimpleName()+".java");
		}};
	}
	
	public <T extends AbsCodeBlock> void generateJsonify(JClass clase, final T code, TreeMap<JsonLevel, List<IAccessor>> descriptions) {
		JsonLevel lastLevel = null;
		List<IAccessor> lastList = null;
		
		for (Map.Entry<JsonLevel, List<IAccessor>> ent : descriptions.entrySet())
			if (ent.getKey().level != Integer.MAX_VALUE && !ent.getValue().isEmpty()) {
				if (lastLevel == null || !isEqual(ent.getValue(), lastList))
					generateJsonify(clase, code, ent.getKey(), ent.getValue());
				else {
					code.add("public String toJson" + ent.getKey().baseName() + "(long rol){return toJson" + lastLevel.baseName() + "(rol);}");
					code.add("public static String toJson" + ent.getKey().baseName() + "List(long rol, java.lang.Iterable<" + clase.getSimpleName() + "> lista){return toJson" + lastLevel.baseName() + clase.getSimpleName() + "(rol, lista);}");
				}
				lastLevel = ent.getKey();
				lastList = ent.getValue();
			}
		
		if(ContextType.SERVER.is() && descriptions.containsKey(JsonLevel.ID)) {
			List<IAccessor> temp = new ArrayList<>(descriptions.get(JsonLevel.ID));
			temp.add(new CustomAccesor("tostring", "objeto.toString()", GlobalTypes.STRING, null));
			generateJsonify(clase, code, JsonLevel.TOSTRING, temp);
		}
	}
	
	private static boolean isEqual(List<IAccessor> l1, List<IAccessor> l2) {
		if (l1.size() != l2.size())
			return false;
		for (int e = 0; e < l1.size(); e++)
		if (!l1.get(e).equals(l2.get(e)))
			return false;
		return true;
	}
	public <T extends AbsCodeBlock> void generateSimplePlainToJson(int modifiers, String methodName, PL params, final T code, final List<IAccessor> campos) {
		code.new B(){{
			if(ContextType.isiOS())
				params.list.add(0, P(code.$convert(GlobalTypes.Java.PrintWriter), "_ _pw"));
			else if(ContextType.isAndroid())
				params.list.add(0, P(code.$convert(GlobalTypes.Java.PrintStream), "_pw"));
			else
				params.list.add(0, P(code.$convert(GlobalTypes.Java.PrintWriter), "_pw"));
			$M(modifiers, "void", methodName, params, ()->{
				fillBody(code, campos);
			});
		}};
	}
	public <T extends AbsCodeBlock> void fillBody(final T code, final List<IAccessor> campos) {
		code.new B(){{
			ArrayList<IAccessor> copiaCampos = new ArrayList<>(campos);
			Collections.sort(copiaCampos, (c1,c2)-> Boolean.compare(isPrimitive(c2), isPrimitive(c1)));
			if(campos.size() > 1 && !isPrimitive(copiaCampos.get(0)))
				$V(code.$(code.$convert(GlobalTypes.BOOLEAN)), "__first", "true");
			R(code, copiaCampos);
		}};
	}
	private JVariable getStreamParameter(final AbsCodeBlock code) {
		IJType type = ContextType.isAndroid()?GlobalTypes.Java.PrintStream:GlobalTypes.Java.PrintWriter;
		return code.P(code.$convert(type),CodeGeneratorContext.is(Language.SWIFT) ?"_ _pw" : "_pw");
	}
	public <T extends AbsCodeBlock> void generateJsonify(final JClass clase, final T code, final JsonLevel level, final List<IAccessor> campos) {
		if (campos.isEmpty())
			return;
		code.new B(){{
				//TODO: preguntar por el level == null para decidir sobre el rol es buggy. Tal vez hay una brecha de seguridad.
				final JVariable rol = level == null || !ContextType.SERVER.is() ? null : P(GlobalTypes.LONG, "rol");
				if(CodeGeneratorContext.is(Language.JAVA))
					generateSimplePlainToJson(PUBLIC | STATIC, "toJson" + (level == null ? "" : level.baseName()), $( P(clase/*TODO: Revisar porque se usa getSimpleName y no clase*/, "objeto"), rol), code, campos);
				else if(ContextType.isiOS())
					generateSimplePlainToJson(PUBLIC | STATIC, "toJson" + (level == null ? "" : level.baseName()), $( P(clase, "objeto"), rol), code, campos);
				else
					throw new NullPointerException("invalido, revisar este punto");
					//TODO: generateSimplePlainToJson(PUBLIC, "toJson" + (level == null ? "" : level.baseName()), $(rol), code, campos);
				/*TODO: DELETE if(clase.isAnnotationPresent(EmbeddedResponse.class)) {
					$M(PUBLIC, code.is_iOS() ? "String" : code.$convert(String.class), "toJsonEmbedded" + (level == null ? "" : level.baseName()), $(rol), ()->{
						$("return toJson" + (level == null ? "" : level.baseName())+ "("+(rol!=null?"rol":"")+");");
					});
				}*/
				IJType listType = CodeGeneratorContext.is(Language.JAVA) ? clase.createListType() : clase.createArrayType();
				crearToJsonWrapper(clase, code, level, rol);
				String methodName = "toJson" + (level == null ? "" : level.baseName()) + "List";
				$M(PUBLIC | STATIC, "void", methodName, $(getStreamParameter(code), rol, P(listType, "lista")), ()-> {
					if(CodeGeneratorContext.is(Language.JAVA))
						$("PrintWriterUtils.toJson(_pw, lista, valor->toJson" + (level == null ? "" : level.baseName()) + "(_pw, valor" + (rol == null ? "" : ", rol") + "));");
					else{
						$("_pw.print(\"[\");");
						$V(code.$(code.$convert(GlobalTypes.BOOLEAN)), "p", "false");
						$FE(clase.getSimpleName(), "valor", "lista", ()->{
							$if("p", "_pw.print(\",\");");
							$("Serializer"+clase.getSimpleName()+".toJson" + (level == null ? "" : level.baseName()) + "(_pw" + (level == null ? "" : ", rol") + ", objeto: valor);");
							$("p = true;");
						});
						$("_pw.print(\"]\");");
					}
				});
				if(CodeGeneratorContext.is(Language.JAVA))
					$M(PUBLIC | STATIC, "void", "toJson" + (level == null ? "" : level.baseName()) + "Map", $(getStreamParameter(code), rol, P(new JType(null, "java.util.Map<Long, "+clase.getSimpleName()+">"), "mapa")), ()-> {
						$("PrintWriterUtils.toJson(_pw, mapa, valor-> toJson" + (level == null ? "" : level.baseName()) + "(_pw, valor" + (rol == null ? "" : ", rol") + "));");	
					});
				else if(ContextType.isiOS()){
					Runnable body = ()->{
						$V("[String:Any]", "__ret", ("[String:Any]")+"()");
						for(int i = 0; i < campos.size(); i++){
							IAccessor ac = campos.get(i);
							String accessor = ac.type().isPrimitive() ? campos.get(i).read() : ("val" + i);
							if (campos.get(i).type().isPrimitive()) {
								$("__ret[\""+ac.name()+"\"] = "+accessor+";");
							} else{
								String acc = campos.get(i).read();
								$if_let($convert(campos.get(i).type()), "val" + i, acc, null, ()->{
									if(ac.type().isArray() && ac.type().getInnerTypes().get(0).isAnnotationPresent(jSerializable.class)){
										final String Cname = ac.type().getInnerTypes().get(0).getSimpleName();
										$("__ret[\""+ac.name()+"\"] = Serializer" + Cname + ".toJsonArray" + (level == null ? "" : level.baseName())+"("+accessor+")");
									}else if(ac.type().isIterable()){
										IJType primerTipo = ac.type().getInnerTypes().get(0);
										if(primerTipo.isAnnotationPresent(jSerializable.class) || primerTipo.isAnnotationPresent(jEntity.class)){
											final String Cname = primerTipo.getSimpleName();
											$("__ret[\""+ac.name()+"\"] = Serializer"+Cname+".toJsonArray" + (level == null ? "" : level.baseName())+"("+accessor+")");
										}else if(primerTipo.isPrimitive() || primerTipo.is(String.class, Long.class) || primerTipo.name().equals("com.google.appengine.api.datastore.GeoPt")){
											$("__ret[\""+ac.name()+"\"] = "+accessor);
										}else throw new NullPointerException("Unssuported post type " + ac.type() + " " + primerTipo);
									}else if(ac.type().isEnum()) {
										$("__ret[\""+ac.name()+"\"] = "+accessor+".id;");
									}else if(ac.type().isJAnnotationPresent(CrystalDate.class)) {
										$("__ret[\""+ac.name()+"\"] = "+accessor+".format();");
									}
									else
										$("__ret[\""+ac.name()+"\"] = "+accessor+";");
								});
							}
						}
						$("return __ret;");
					};
					$("public static func toJsonObject" + (level == null ? "" : level.baseName())+"(objeto : "+clase.getSimpleName()+") -> [String:Any]",body);
					
					body = ()->{
						$V("[[String:Any]]", "__ret", ("[[String:Any]]")+"()");
						$FE(clase.getSimpleName(), "valor", "lista", ()->{
							$("__ret.append(Serializer"+clase.getSimpleName()+".toJsonObject" + (level == null ? "" : level.baseName()) + "(objeto: valor))");
						});
						$("return __ret;");
					};
					$("public static func toJsonArray" + (level == null ? "" : level.baseName())+"(_ lista:"+listType+")->[[String:Any]]",body);
				}
		}};
	}
	private static boolean isPrimitive(IAccessor a){
		return a.type().isArray() && a.type().getInnerTypes().get(0).isSubclassOf(MaskedEnum.class) || a.type().isPrimitive();
	}
	private <T extends AbsCodeBlock> void R(final T code, final List<IAccessor> campos) {
		final String quote;
		if(ContextType.SERVER.is()) {
			quote = "jcrystal.JSONUtils.jsonQuote";
		}else if(ContextType.WEB.is()) {
			quote = "JSONUtils.jsonQuote";
		}else
			quote = "jsonQuote";
		final String quoteMap = ContextType.SERVER.is() ? "jcrystal.JSONUtils.Map.jsonQuote" : quote;
		final String quoteDatastore = ContextType.SERVER.is() ? "jcrystal.db.datastore.JSONUtils.jsonQuote" : quote;
		boolean[] primeroPuesto = {false};
		code.new B(){{
				$("_pw.print(\"{\");");
				IntStream.range(0, campos.size()).forEach(i->{
					final boolean primitive = isPrimitive(campos.get(i));
					Runnable toString = ()->{
						String prefix = "";
						if(campos.get(i).type().isPrimitive() && ContextType.WEB.is())
							prefix = "this.";
						BiConsumer<Runnable, String> wrapPrint = (r,string)->{
							Consumer<Boolean> put = coma->{
								if(r==null) {
									if(ContextType.isiOS())
										$("_pw.print(\""+(coma?",":"")+"\\\"" + campos.get(i).name() + "\\\":\", "+string+");");
									else
										$("PrintWriterUtils.print(_pw, \""+(coma?",":"")+"\\\"" + campos.get(i).name() + "\\\":\", "+string+");");
								}else {
									$("_pw.print(\""+(coma?",":"")+"\\\"" + campos.get(i).name() + "\\\":\");");
									r.run();
								}
							};
							if(primeroPuesto[0]) {
								put.accept(true);
							}else if(i==0){
								put.accept(false);
								if(!primitive && campos.size() > 1)
									$("__first = false;");
							}else{
								$if("__first", ()->{
									put.accept(false);
									$("__first = false;");
								}).$else(()->{
									put.accept(true);
								});
							}
						};
						
						String accessor = prefix + (primitive ? campos.get(i).read() : ("val" + i));
						if (campos.get(i).type().isEnum()) {
							if(campos.get(i).type().resolve(c->c.enumData.propiedades.get("id")) == null)
								wrapPrint.accept(null, quote+"("+accessor+(ContextType.WEB.is() ? "" : ".name())"));
							else
								wrapPrint.accept(null, accessor+(ContextType.WEB.is() ? "" : ".id"));
						}else if (campos.get(i).type().isJAnnotationPresent(CrystalDate.class)){
							if (ContextType.MOBILE.is() || (!ContextType.SERVER.is() && CodeGeneratorContext.is(Language.JAVA)))
								wrapPrint.accept(null, quote+"("+accessor + ".format())");
							else if(campos.get(i).type().is(CreationTimestamp.class, ModificationTimestamp.class))
								wrapPrint.accept(null, quote+"("+CrystalDateMilis.class.getName()+".format("+accessor + "))");
							else
								wrapPrint.accept(null, quote+"(jcrystal.datetime."+campos.get(i).type().getSimpleName()+".format("+accessor + "))");
						} else if (campos.get(i).type().is(Date.class)) {
							DateType tipo = campos.get(i).isJAnnotationPresent(CrystalDate.class) ? campos.get(i).getJAnnotation(CrystalDate.class).value():DateType.DATE_MILIS;
							if (CodeGeneratorContext.is(Language.SWIFT))
								wrapPrint.accept(null, quote+"(Crystal"+StringUtils.camelizar(tipo.name())+".SDF.string(from: " + accessor + "))");
							else
								wrapPrint.accept(null, quote+"(jcrystal.datetime.DateType." + tipo + ".FORMAT.format(" + accessor + "))");
						} 
						else if (campos.get(i).type().isJAnnotationPresent(InternalEntityKey.class)) {
							EntityClass entity = context.data.entidades.get(campos.get(i).type());
							if (CodeGeneratorContext.is(Language.JAVA))
								wrapPrint.accept(() -> $($(entity.clase)+".Serializer.Key.toJson(_pw, "+accessor+");"), null);
							else {
								String name = campos.get(i).type().name();
								String simpleName = name.endsWith(".Key") ? name.substring(0, name.length() - 4) : name;  
								wrapPrint.accept(() -> $(simpleName+".Serializer.Key.toJson(_pw, objeto: "+accessor+")"), null);
							}
						}else if (campos.get(i).type().isAnnotationPresent(jSerializable.class)) {
							if (CodeGeneratorContext.is(Language.JAVA))
								wrapPrint.accept(() -> $("Serializer"+campos.get(i).type().getSimpleName()+".toJson(_pw, "+accessor+");"), null);
							else
								wrapPrint.accept(() -> $("Serializer"+campos.get(i).type().getSimpleName()+".toJson(_pw, objeto: "+accessor+")"), null);
						}else if(campos.get(i).type().isAnnotationPresent(Post.class)) {
							JClass superClase = context.data.entidades.get(campos.get(i).type()).clase;
							JsonLevel level = campos.get(i).type().getAnnotation(Post.class).level();
							if (CodeGeneratorContext.is(Language.JAVA)) {
								wrapPrint.accept(() -> $("Serializer"+superClase.getSimpleName()+".toJson"+level.baseName()+"(_pw, "+accessor +");"), null);
							}else if (ContextType.isiOS()) {
								wrapPrint.accept(() -> $("Serializer"+superClase.getSimpleName()+".toJson(_pw, objeto: "+accessor+")"), null);
							}
							else throw new NullPointerException("Invalid type " + campos.get(i).type());
						}else if (campos.get(i).type().isAnnotationPresent(jEntity.class)) {
							JsonLevel level = SerializeLevelUtils.getAnnotedJsonLevel(campos.get(i));
							if(level == JsonLevel.NONE);
							else if (CodeGeneratorContext.is(Language.SWIFT))
								wrapPrint.accept(() -> $("Serializer"+campos.get(i).type().getSimpleName()+".toJson"+level.baseName()+"(_pw, "+accessor+" ,0);"), null);
							else
								wrapPrint.accept(() -> $(campos.get(i).type().name()+".Serializer.toJson"+level.baseName()+"(_pw, "+accessor+" ,0);"), null);
						}
						else if (campos.get(i).type().is(LongText.class, String.class) || campos.get(i).type().name().equals("com.google.appengine.api.datastore.Text"))
							wrapPrint.accept(null, quote+"(" + accessor + ")");
						else if (campos.get(i).type().name().equals("com.google.appengine.api.datastore.GeoPt"))
							wrapPrint.accept(null, quoteDatastore+"(" + accessor + ")");
						else if (campos.get(i).type().is(JSONObject.class) || campos.get(i).type().is(JSONArray.class))
							wrapPrint.accept(null, accessor + ".toString(0)");
						else if (campos.get(i).type().isArray()){
							if(ContextType.isiOS() && campos.get(i).type().getInnerTypes().get(0).is(byte.class)) {
								wrapPrint.accept(null, quote+"(" + accessor + ".base64EncodedString(options: []))");
							}else
								wrapPrint.accept(null, quote+"(" + accessor + ")");
						}else if (campos.get(i).type().isSubclassOf(Map.class)) {
							IJType primerTipo = campos.get(i).type().getInnerTypes().get(0);
							IJType segundoTipo = campos.get(i).type().getInnerTypes().get(1);
							if (segundoTipo.isAnnotationPresent(jEntity.class)) {
								JsonLevel level = SerializeLevelUtils.getAnnotedJsonLevel(campos.get(i));
								if(level != JsonLevel.NONE)
									wrapPrint.accept(() -> $(segundoTipo.getPackageName()+".Serializer"+segundoTipo.getSimpleName()+".toJson"+level.baseName()+segundoTipo.getSimpleName()+"(_pw, 0, "+ accessor + ");"), null);
								else {
									if(ContextType.SERVER.is())
										throw new NullPointerException("No json level for " + accessor);
									else
										wrapPrint.accept(null, quote+"(" + accessor + ")");
								}
							}else
								wrapPrint.accept(() -> $(quoteMap+primerTipo.getSimpleName()+segundoTipo.getSimpleName()+"(_pw, " + accessor + ");"), null);
						}else if (campos.get(i).type().isIterable()) {
							IJType primerTipo = campos.get(i).type().getInnerTypes().get(0);
							if (primerTipo.isPrimitive() || primerTipo.isPrimitiveObjectType() || primerTipo.is(String.class)){
								if(ContextType.isiOS())
									wrapPrint.accept(null, quote+"(" + accessor + ")");
								else
									wrapPrint.accept(() -> $(quote+primerTipo.getSimpleName()+"(_pw, " + accessor + ");"), null);
							}else if(primerTipo.isEnum()) {
								if(ContextType.SERVER.is())
									wrapPrint.accept(null, primerTipo.prefixName("Utils")+".jsonQuote(" + accessor + ")");
								else if(ContextType.isiOS())
									wrapPrint.accept(null, quote+"(" + accessor + ")");
								else
									wrapPrint.accept(() -> $(quote+"(_pw, " + accessor + ");"), null);
							}
							else if (primerTipo.is("com.google.appengine.api.datastore.GeoPt"))
								wrapPrint.accept(null, quoteDatastore+"(" + accessor + ")");
							else if(primerTipo.isAnnotationPresent(Post.class)) {
								JClass superClase = context.data.entidades.get(primerTipo).clase;
								JsonLevel level = primerTipo.getAnnotation(Post.class).level();
								if(CodeGeneratorContext.is(Language.JAVA))
									wrapPrint.accept(() -> $("Serializer"+superClase.getSimpleName() + ".toJson"+level.baseName()+"List(_pw, "+accessor+");"), null);
								else if(ContextType.isiOS())
									wrapPrint.accept(() -> $("Serializer"+$($convert(superClase)) + ".toJson"+superClase.getSimpleName()+"(_pw, lista: "+accessor+")"), null);
								else throw new NullPointerException("Invalid type " + primerTipo);
							}else if (primerTipo.isAnnotationPresent(jEntity.class)) {
								IAccessor campo = campos.get(i);
								JsonLevel level = SerializeLevelUtils.getAnnotedJsonLevel(campo);
								if(level != JsonLevel.NONE)
									wrapPrint.accept(() -> $(primerTipo.getPackageName()+".Serializer"+primerTipo.getSimpleName()+".toJson"+level.baseName()+primerTipo.getSimpleName()+"(_pw, 0, "+ accessor + ");"), null);
								else if(ContextType.SERVER.is())
									throw new NullPointerException("No json level for " + accessor + " : " + primerTipo.name());
								else if(ContextType.MOBILE.is())
									wrapPrint.accept(null, quote+"(" + accessor + ")");
								else
									wrapPrint.accept(() -> $(quote+primerTipo.getSimpleName()+"(_pw, " + accessor + ");"), null);
							}else if(primerTipo.isAnnotationPresent(jSerializable.class)){
								if(ContextType.isiOS())
									wrapPrint.accept(() -> $("Serializer"+primerTipo.getSimpleName()+".toJson"+primerTipo.getSimpleName()+"(_pw, lista: "+accessor + ")"), null);
								else if(CodeGeneratorContext.is(Language.JAVA))
									wrapPrint.accept(() -> $("Serializer"+primerTipo.getSimpleName()+".toJsonList(_pw, "+accessor + ");"), null);	
								else
									wrapPrint.accept(() -> $(primerTipo.getSimpleName()+".toJson"+primerTipo.getSimpleName()+"(_pw, "+accessor + ");"), null);
							}else throw new NullPointerException("Invalid type " + primerTipo);
						}else
							wrapPrint.accept(null, accessor);
						
						
					};
					String cond = !ContextType.SERVER.is()?null:RolGenerator.ROL_MAPPER.values().stream().filter(rol->campos.get(i).isJAnnotationPresent(rol.annotationClass)).map(rol->"(rol & " + rol.valueAccessor + ".id) != 0").collect(Collectors.joining(" || "));
					if (primitive) {
						if(CodeGeneratorContext.is(Language.JAVA) && campos.get(i).type().is(double.class)) {
							String condFinite = "(!Double.isInfinite("+campos.get(i).read()+") && !Double.isNaN("+campos.get(i).read()+"))";
							cond = cond == null || cond.isEmpty() ?  condFinite : (cond + " && " + condFinite);
						}
						$if(cond, toString);
						primeroPuesto[0] = true;
					}else if(campos.get(i).type().is(String.class, Long.class, Integer.class, Double.class, Boolean.class) && ContextType.SERVER.is()) {
						if(CodeGeneratorContext.is(Language.JAVA) && campos.get(i).type().is(Double.class)) {
							String condFinite = "(!Double.isInfinite("+campos.get(i).read()+") && !Double.isNaN("+campos.get(i).read()+"))";
							cond = cond == null || cond.isEmpty() ?  condFinite : (cond + " && " + condFinite);
						}
						$if(cond, ()->{
							if(primeroPuesto[0])
								$("PrintWriterUtils.printJsonProp(_pw, false, \"\\\"" + campos.get(i).name() + "\\\":\", "+campos.get(i).read()+");");
							else if(campos.size() == 1)
								$("PrintWriterUtils.printJsonProp(_pw, true, \"\\\"" + campos.get(i).name() + "\\\":\", "+campos.get(i).read()+");");
							else
								$("__first = PrintWriterUtils.printJsonProp(_pw, __first, \"\\\"" + campos.get(i).name() + "\\\":\", "+campos.get(i).read()+");");							
						});
						
					}else{
						String accessor = campos.get(i).read();
						$if_let($convert(campos.get(i).type()), "val" + i, accessor, cond, toString);
					}
				});
				$("_pw.print(\"}\");");
		}};
	}
	private static <T extends AbsCodeBlock> void crearToJsonWrapper(final JClass clase, final T code, final JsonLevel level, final JVariable rol) {
		code.new B() {{
			if(ContextType.SERVER.is()) {
				IJType listType = new JType(null, "jcrystal.manager.utils.FileWrapperResponse<java.util.List<" + clase.getSimpleName() + ">>");
				$M(PUBLIC | STATIC, "void", "toJson" + (level == null ? "" : level.baseName()) + clase.getSimpleName(), $(P(code.$convert(GlobalTypes.Java.PrintWriter),"_pw"), rol, P(listType, "lista")),"throws java.io.IOException", ()-> {
					$("PrintWriterUtils.toJson(_pw, lista.getItem(), valor->toJson" + (level == null ? "" : level.baseName()) + "(_pw, valor" + (rol == null ? "" : ", rol") + "));");
				});
			}
		}};
	}
}

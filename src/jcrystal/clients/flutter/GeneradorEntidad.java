package jcrystal.clients.flutter;

import static jcrystal.clients.android.AndroidClient.netPackage;
import static jcrystal.clients.android.AndroidClient.paqueteEntidades;
import static jcrystal.clients.android.AndroidClient.paqueteMobile;
import static jcrystal.clients.android.AndroidClient.paquetePadre;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.appengine.api.datastore.Text;

import jcrystal.clients.AbsEntityGenerator;
import jcrystal.clients.ClientGeneratorDescriptor;
import jcrystal.datetime.DateType;
import jcrystal.entity.types.LongText;
import jcrystal.json.JsonLevel;
import jcrystal.lang.Language;
import jcrystal.model.server.db.EntityClass;
import jcrystal.model.server.db.EntityField;
import jcrystal.reflection.MaskedEnum;
import jcrystal.reflection.annotations.CrystalDate;
import jcrystal.reflection.annotations.LoginResultClass;
import jcrystal.reflection.annotations.Selector;
import jcrystal.reflection.annotations.jEntity;
import jcrystal.reflection.annotations.com.jSerializable;
import jcrystal.reflection.annotations.entities.RelMto1;
import jcrystal.types.IJType;
import jcrystal.types.JClass;
import jcrystal.types.vars.IAccessor;
import jcrystal.utils.ListUtils;
import jcrystal.utils.SerializeLevelUtils;
import jcrystal.utils.StringSeparator;
import jcrystal.utils.StringUtils;
import jcrystal.utils.context.CodeGeneratorContext;
import jcrystal.utils.context.CodeGeneratorContext.ContextType;
import jcrystal.utils.langAndPlats.AbsCodeBlock;
import jcrystal.utils.langAndPlats.JavaCode;

public class GeneradorEntidad extends AbsEntityGenerator<FlutterClient>{
	
	private ClientGeneratorDescriptor descriptor;
	
	public GeneradorEntidad(FlutterClient client, ClientGeneratorDescriptor descriptor) {
		super(client);
		this.descriptor = descriptor;
		this.entityValidatorGenerator = new FlutterValidators(client);
	}
	
	private void crearInterfaz(final EntityClass entidad, final JsonLevel level){
		final JavaCode cliente = new JavaCode(){{
				$("package "+FlutterClient.paqueteEntidades+";");
				$("import "+FlutterClient.paqueteMobile+".*;");
				$("import "+FlutterClient.paqueteDates+".*;");
				$("public interface " + entidad.clase.simpleName+level.baseName()+" extends " + netPackage+".ISerializable", ()->{
					for(EntityField f : descriptor.getFields(entidad).get(level))
						$($($convert(client.entityUtils.convertToRawType(f.f))) + " " + f.name() + "();");
				});
		}};
		
		client.exportFile(cliente, paqueteEntidades.replace(".", File.separator) + File.separator + entidad.clase.getSimpleName()+level.baseName() + ".java");
	}
	@Override
	public void generateEntity(final EntityClass entidad, final TreeSet<JsonLevel> levels){
		
		StringSeparator interfaces = new StringSeparator(',');
		for(JsonLevel level : levels) {
			crearInterfaz(entidad, level);
			interfaces.add(entidad.clase.simpleName+level.baseName());
		}
		interfaces.add(netPackage+".ISerializable");
		//CREAR LA CLASE
		final List<EntityField> definitiveFields = ListUtils.join(descriptor.getFields(entidad).get(null), new ArrayList<EntityField>(descriptor.getFields(entidad).get(levels.last())));
		
		new JavaCode(){{
			$("package "+paqueteEntidades+";");
			$("import "+paqueteMobile+".*;");
			$("import "+paqueteEntidades+".enums.*;");
			$("import "+FlutterClient.paqueteDates+".*;");
			$("import static "+paquetePadre+".JSONUtils.*;");
			String extras = interfaces.isEmpty()?"":(" implements " + interfaces);
			if(entidad.padre != null && entidad.padre.campoSelector != null)
				extras = " extends " + entidad.padre.name() + extras;
			
			$("public class " + entidad.clase.simpleName + extras, ()->{
				for(final EntityField f : definitiveFields){
					$("private "+$($convert(client.entityUtils.convertToRawType(f.f)))+" " + f.name() + ";");
					$("public "+$($convert(client.entityUtils.convertToRawType(f.f)))+" " + f.name() + "(){return this."+f.name()+";}");
					$("public void " + f.name() + "("+$($convert(client.entityUtils.convertToRawType(f.f)))+" val){"+f.name()+" = val;}");
				}
				$("public " + entidad.clase.simpleName + "()", ()->{});
				$("protected " + entidad.clase.simpleName + "(org.json.JSONObject json)throws org.json.JSONException", ()->{
					if(entidad.padre != null && entidad.padre.campoSelector != null)
						$("super(json);");
					for(final EntityField f : definitiveFields){
						if(f.type().isEnum())
							client.requiredClasses.add(f.type());
						
						procesarCampo(this, entidad.clase, f.fieldKeyAccessor().prefix("this."));
					}
				});
				$("public " + entidad.clase.simpleName + "(org.json.JSONObject json, JsonLevel level)throws org.json.JSONException", ()->{
					if(entidad.padre != null && entidad.padre.campoSelector != null)
						$("super(json, level);");
					$if("level != null", ()->{
						$("switch(level)", ()->{
							for(final JsonLevel level : levels){
								$("case " + level.name() + ": ", ()->{
									for (final EntityField f : descriptor.getFields(entidad).get(level))
										procesarCampo(this, entidad.clase, f.fieldKeyAccessor().prefix("this."));
								});
								$("break;");
							}
						});
					}).$else(()->{
						for(final EntityField f : definitiveFields)
							procesarCampo(this, entidad.clase, f.fieldKeyAccessor().prefix("this."));
					});
				});
				$("@Override public void toJson(java.io.PrintStream _pw)",()->{
					$("Serializer"+entidad.clase.simpleName+".toJson(_pw, this);");
				});
				$("public static class ListUtils", ()->{
					FlutterClient.generateFromListJson(this, entidad.clase, null);
					for(final JsonLevel level : levels)
						FlutterClient.generateFromListJson(this, entidad.clase, level);
				});
				if(entidad.campoSelector != null){
					$("private static " + entidad.getTipo() + " getBySelector(org.json.JSONObject json)throws org.json.JSONException", ()->{
						String nombre =  entidad.campoSelector.dbName;
						
						$($convert(entidad.campoSelector.type()) + " selector = " + $convert(entidad.campoSelector.type())+".fromId(json.getInt(\"" + nombre + "\"));");
						for(EntityClass h : entidad.hijos)if(h.clase.isAnnotationPresent(Selector.class)){
							$("if(selector == " + h.clase.getAnnotation(Selector.class).valor()+")return new " + h.clase.simpleName + "(json);");
						}
						$("return new " + entidad.clase.simpleName+"(json);");
					});
				}
				if (entidad.clase.isAnnotationPresent(LoginResultClass.class) || entidad.isSecurityToken())
					FlutterClient.crearcodigoTokenSeguridad(client.context, this, entidad.clase);
				
				$("public static class MapList",()->{
					TreeSet<String> procesado = new TreeSet<>();
					descriptor.getFields(entidad).fields.forEach((level, list)->{
						if(level != null && levels.contains(level))
							list.stream().filter(f->!procesado.contains(f.name())).forEach(f->{
								procesado.add(f.name());
								String className =entidad.clase.getSimpleName()+level.baseName();
								if(f.type().is(Long.class, long.class) || f.isAnnotationPresent(RelMto1.class)) {
									$("public static<T extends "+className+"> java.util.Map<Long, T> By"+StringUtils.capitalize(f.name())+"(java.util.List<T> lista)",()->{
										$("java.util.Map<Long, T> ret = new java.util.TreeMap<>();");
										$("for(T val : lista)",()->{
											$("ret.put(val."+f.name()+"(), val);");
										});
										$("return ret;");
									});
								}	
								if(f.type().isEnum()) {
									$("public static<T extends "+className+"> java.util.Map<Integer, T> By"+StringUtils.capitalize(f.name())+"(java.util.List<T> lista)",()->{
										$("java.util.Map<Integer, T> ret = new java.util.TreeMap<>();");
										$("for(T val : lista)",()->{
											$if("val."+f.name()+"() != null",()->{
												$("ret.put(val."+f.name()+"().id, val);");
											});
										});
										$("return ret;");
									});
								}
							});
					});
				});
				$("public static class Group",()->{
					TreeSet<String> procesado = new TreeSet<>();
					descriptor.getFields(entidad).fields.forEach((level, list)->{
						if(level != null && levels.contains(level))
							list.stream().filter(f->!procesado.contains(f.name())).forEach(f->{
								procesado.add(f.name());
								String className =entidad.clase.getSimpleName()+level.baseName();
								if(f.type().is(Long.class, long.class)) {
									$("public static<T extends "+className+"> java.util.Map<Long, java.util.List<T>> By"+StringUtils.capitalize(f.name())+"(java.util.List<T> lista)",()->{
										$("java.util.Map<Long, java.util.List<T>> ret = new java.util.TreeMap<>();");
										$("for(T val : lista)",()->{
											$("java.util.List<T> list = ret.get(val."+f.name()+"());");
											$if("list == null",()->{
												$("ret.put(val."+f.name()+"(), list = new java.util.ArrayList<T>());");
											});
											$("list.add(val);");
										});
										$("return ret;");
									});
								}	
								if(f.type().isEnum()) {
									$("public static<T extends "+className+"> java.util.Map<Integer, java.util.List<T>> By"+StringUtils.capitalize(f.name())+"(java.util.List<T> lista)",()->{
										$("java.util.Map<Integer, java.util.List<T>> ret = new java.util.TreeMap<>();");
										$("for(T val : lista)",()->{
											$if("val."+f.name()+"() != null",()->{
												$("java.util.List<T> list = ret.get(val."+f.name()+"().id);");
												$if("list == null",()->{
													$("ret.put(val."+f.name()+"().id, list = new java.util.ArrayList<T>());");
												});
												$("list.add(val);");												
											});
										});
										$("return ret;");
									});
								}
							});
					});
				});
			});
			client.exportFile(this, paqueteEntidades.replace(".", File.separator) + File.separator + entidad.clase.simpleName + ".java");
		}};
		//CREAR SERIALIZER
		new JavaCode(){{
			$("package "+paqueteEntidades+";");
			$("import "+paqueteMobile+".*;");
			$("import "+paqueteEntidades+".enums.*;");
			$("import "+FlutterClient.paqueteDates+".*;");
			$("import static "+paquetePadre+".JSONUtils.*;");
			
			$("public class Serializer" + entidad.clase.simpleName, ()->{
				
				client.context.utils.generadorToJson.generateJsonify(entidad.clase, this, null, definitiveFields.stream().map(f -> f.propertyKeyAccessor().prefix("objeto.")).collect(Collectors.toList()));
				for(final JsonLevel level : levels)
					client.context.utils.generadorToJson.generateJsonify(entidad.clase, this, level, descriptor.getFields(entidad).get(level).stream().map(f -> f.propertyKeyAccessor().prefix("objeto.")).collect(Collectors.toList()));
			});
			client.exportFile(this, paqueteEntidades.replace(".", File.separator) + File.separator + "Serializer" + entidad.clase.simpleName + ".java");
		}};
		new GeneradorStorage().crearCodigoAlmacenamiento(client, paqueteEntidades, entidad.clase, levels);
		
	}
	public static void procesarCampo(AbsCodeBlock code, final JClass definingClass, final IAccessor f){
		code.new B(){{
				if(f.type().is(Text.class, LongText.class)){
					$(f.read()+ " = json.containsKey(\"" + f.name() + "\")?json[" + f.name()+"] : null;");
				}else if(f.type().name().equals("com.google.appengine.api.datastore.GeoPt")){
					throw new NullPointerException();
				}else if(f.type().isEnum()) {
					$(f.read()+ " = json[\"" + f.name() + "\"] != null ? " + f.type().getSimpleName() + "Helper.fromId(json[\"" + f.name() + "\"]) : null;");
				}else if(f.type().is(Date.class)) {
					final DateType tipo = f.isJAnnotationPresent(CrystalDate.class)?f.getJAnnotation(CrystalDate.class).value():DateType.DATE_MILIS;
					$("try", ()->{
						$(f.read()+ " = json.containsKey(\"" + f.name() + "\")&&!json.isNull(\""+f.name()+"\")?DateType." + tipo + ".FORMAT.parse(json.getString(\"" + f.name() + "\")):null;");
					});
					$("catch(java.text.ParseException ex)", ()->{
						$("throw new org.json.JSONException(ex.getMessage());");
					});
					throw new NullPointerException();
				}else if(f.type().isJAnnotationPresent(CrystalDate.class)) {
					$("try", ()->{
						if(!ContextType.MOBILE.is() && definingClass.isAnnotationPresent(jEntity.class))
							$(f.read()+ " = json.containsKey(\"" + f.name() + "\")&&!json.isNull(\""+f.name()+"\")?new " + f.type().getSimpleName() + "(json.getString(\"" + f.name() + "\")).toDate():null;");
						else
							$(f.read()+ " = json.containsKey(\"" + f.name() + "\")&&!json.isNull(\""+f.name()+"\")?new jcrystal.datetime." + f.type().getSimpleName() + "(json.getString(\"" + f.name() + "\")):null;");
					});
					$("catch(java.text.ParseException ex)", ()->{
						$("throw new org.json.JSONException(ex.getMessage());");
					});
					throw new NullPointerException();
				}else if(f.type().isAnnotationPresent(jSerializable.class)) {
					$(f.read()+ " = " + $($convert(f.type())) + ".fromJson(json[\"" + f.name() + "\"]);");
				}
				//Tipos Json
				else if(f.type().is(JSONArray.class))
					$(f.read()+ " = json[\"" + f.name() + "\"];");
				else if(f.type().is(JSONObject.class))
					$(f.read()+ " = json[\"" + f.name() + "\"];");
				//Tipos básicos
				else if(f.type().is(char.class))
					$(f.read()+ " = json[\"" + f.name() + "\"];");
				else if(f.type().is(int.class))
					$(f.read()+ " = json[\"" + f.name() + "\"];");
				else if(f.type().is(Integer.class))
					$(f.read()+ " = json[\"" + f.name() + "\"];");
				else if(f.type().is(long.class))
					$(f.read()+ " = json[\"" + f.name() + "\"];");
				else if(f.type().is(Long.class))
					$(f.read()+ " = json[\"" + f.name() + "\"];");
				else if(f.type().is(boolean.class))
					$(f.read()+ " = json[\"" + f.name() + "\"];");
				else if(f.type().is(double.class))
					$(f.read()+ " = json[\"" + f.name() + "\"];");
				else if (f.type().is(Double.class))
					$(f.read()+ " = json[\"" + f.name() + "\"];");
				else if(f.type().is(float.class))
					$(f.read()+ " = json[\"" + f.name() + "\"];");
				else if(f.type().is(String.class))
					$(f.read()+ " = json[\"" + f.name() + "\"];");
				//Entidades
				else if(f.type().isAnnotationPresent(jEntity.class)) {
					JsonLevel level = SerializeLevelUtils.getAnnotedJsonLevel(f);
					$(f.read()+ " = json.containsKey(\"" + f.name() + "\") && json[\""+f.name()+"\"] != null ? new "+f.type().name()+".Post."+level.baseName()+"(json[\"" + f.name() + "\"]).create():null;");
					//$(f.accessor()+ " = json.containsKey(\"" + f.name() + "\")&&!json.isNull(\""+f.name()+"\")?json.getLong(\"" + f.name() + "\"):null;");
				}else if(f.type().isArray()){
					$("", ()->{
						IJType arrayType = f.type().getInnerTypes().get(0);
						if(arrayType.isSubclassOf(MaskedEnum.class)){
							$(f.read()+ " = " + (ContextType.SERVER.is()?arrayType.name():arrayType.getSimpleName()) + ".getFromMask(json.optLong(\"" + f.name() + "\",0));");
						}else{
							$("List<dynamic> $Array" + f.name() + " = json[\"" + f.name() + "\"];");
							$if("$Array" + f.name()+" != null",()->{
								$(f.read()+ " = new " + arrayType.name() + "[$Array" + f.name() + ".length()];");
								$("for(int i = 0; i < " + f.read()+ ".length; i++)", ()->{
									if(arrayType.is(int.class))
										$(f.read()+ "[i] = $Array" + f.name() + ".getInt(i);");
									else if(arrayType.is(long.class))
										$(f.read()+ "[i] = $Array" + f.name() + ".getLong(i);");
									else if(arrayType.is(double.class))
										$(f.read()+ "[i] = $Array" + f.name() + ".getDouble(i);");
									else if(arrayType.is(boolean.class))
										$(f.read()+ "[i] = $Array" + f.name() + ".getBoolean(i);");
									else
										throw new NullPointerException(arrayType.name());
								});
							});
							
						}
					});
				}
				//Maps
				else if(f.type().isSubclassOf(Map.class)) {
					final IJType tipoKey = f.type().getInnerTypes().get(0);
					final IJType tipoVal = f.type().getInnerTypes().get(1);
					System.out.println("Map omitido en clase " + definingClass.name +" : " + f.name() + " : Map<"+tipoKey.getSimpleName()+", "+tipoVal.name()+">");
					System.err.println("Map omitido en clase " + definingClass.name +" : " + f.name() + " : Map<"+tipoKey.getSimpleName()+", "+tipoVal.name()+">");
				}
				//Listas
				else if(f.type().isSubclassOf(List.class)) {
					$("", ()->{
						final IJType tipoParamero = f.type().getInnerTypes().get(0);
						$("List<dynamic> $Array" + f.name() + " = json[\"" + f.name() + "\"];");
						$if("$Array" + f.name()+" != null",()->{
							if (tipoParamero.name().equals("com.google.appengine.api.datastore.GeoPt")) {
								if(ContextType.SERVER.is())
									$(f.read()+ " = new java.util.ArrayList<>();");
								else
									$(f.read()+ " = new double[$Array" + f.name() + ".length()][2];");
								$("for(int i = 0; i < $Array" + f.name() + ".length(); i++)", ()->{
									$("org.json.JSONArray $temp = $Array" + f.name() + ".getJSONArray(i);");
									if(ContextType.SERVER.is()) {
										$(f.read()+ ".add(new com.google.appengine.api.datastore.GeoPt((float)$temp.getDouble(0), (float)$temp.getDouble(1)));");
									}else{
										$(f.read()+ "[i][0] = $temp.getDouble(0);");
										$(f.read()+ "[i][1] = $temp.getDouble(1);");
									}
								});
							}else if(tipoParamero.is(Long.class, Integer.class, String.class)){
								String jsonName = tipoParamero.is(Integer.class) ? "Int" : StringUtils.capitalize(tipoParamero.getSimpleName());
								if(CodeGeneratorContext.is(Language.JAVA)) {
									$(f.read()+ " = new java.util.ArrayList<>();");
									$("for(int i = 0; i < $Array" + f.name() + ".length(); i++)", ()->{
										$(f.read()+ ".add($Array" + f.name() + ".get" + jsonName + "(i));");
									});
								}else {
									$(f.read()+ " = new "+tipoParamero.name()+"[$Array" + f.name() + ".length()];");
									$("for(int i = 0; i < $Array" + f.name() + ".length(); i++)", ()->{
										$(f.read()+ "[i] = $Array" + f.name() + ".get" + jsonName + "(i);");
									});
								}
								
							}else if(tipoParamero.isEnum()){
								if(ContextType.SERVER.is() || ContextType.isAndroid())
									$(f.read()+ " = new java.util.ArrayList<>();");
								else
									$(f.read()+ " = new "+tipoParamero.name()+"[$Array" + f.name() + ".length()];");
								$("for(int i = 0; i < $Array" + f.name() + ".length(); i++)", ()->{
									if(ContextType.SERVER.is() || ContextType.isAndroid()){
										$(f.read()+ ".add("+tipoParamero.getSimpleName()+".fromId($Array" + f.name() + ".getLong(i)));");
									}else{
										$(f.read()+ "[i] = "+tipoParamero.getSimpleName()+".fromId($Array" + f.name() + ".getLong(i));");
									}
								});
							}else if(tipoParamero.isAnnotationPresent(jSerializable.class)){
								if(ContextType.SERVER.is())
									$(f.read()+ " = " + tipoParamero.name() + ".listFromJson($Array" + f.name() + ");");
								else
									$(f.read()+ " = " + $($convert(tipoParamero)) + ".listFromJson($Array" + f.name() + ");");
							}else if(tipoParamero.isAnnotationPresent(jEntity.class)){
								JsonLevel level = SerializeLevelUtils.getAnnotedJsonLevel(f);
								if(ContextType.SERVER.is() || ContextType.isAndroid()) {
									$(f.read()+ " = new java.util.ArrayList<>();");
									$("for(int i = 0; i < $Array" + f.name() + ".length(); i++)", ()->{
										$(f.read()+ ".add(new "+tipoParamero.name()+".Post."+level.baseName()+"($Array"+f.name()+".getJSONObject(i)).create());");
										//$(f.accessor()+ ".add($Array" + f.name() + ".getLong(i));");
									});
								}else {
									$(f.read()+ " = new "+tipoParamero.name()+"[$Array" + f.name() + ".length()];");
									$("for(int i = 0; i < $Array" + f.name() + ".length(); i++)", ()->{
										$(f.read()+ "[i] = $Array" + f.name() + ".getLong(i);");
									});
								}
							}else
								throw new NullPointerException(f.type().name());
						});
					});
				}else{
					throw new NullPointerException(definingClass.name + " "+f.type().name());
				}
		}};
	}
	@Override
	public void generateEntityKey(EntityClass entidad) {
		// TODO Generate entity key
	}
}

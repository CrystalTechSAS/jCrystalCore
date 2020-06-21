package jcrystal.clients.android;

import static java.lang.reflect.Modifier.PUBLIC;
import static java.lang.reflect.Modifier.STATIC;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jcrystal.clients.AbsClientGenerator;
import jcrystal.clients.AbsEntityGenerator;
import jcrystal.clients.ClientGeneratorDescriptor;
import jcrystal.json.JsonLevel;
import jcrystal.model.server.db.EntityClass;
import jcrystal.model.server.db.EntityField;
import jcrystal.reflection.annotations.LoginResultClass;
import jcrystal.reflection.annotations.Selector;
import jcrystal.reflection.annotations.entities.RelMto1;
import jcrystal.types.IJType;
import jcrystal.types.JClass;
import jcrystal.utils.ListUtils;
import jcrystal.utils.StringSeparator;
import jcrystal.utils.StringUtils;
import jcrystal.utils.context.CodeGeneratorContext.ContextType;
import jcrystal.utils.langAndPlats.JavaCode;
import jcrystal.utils.langAndPlats.TypescriptCode;
import jcrystal.utils.langAndPlats.delegates.JavaCodeDelegator;
import jcrystal.utils.langAndPlats.delegates.TypescriptCodeDelegator;

public class GeneradorEntidad extends AbsEntityGenerator<AbsClientGenerator<?>>{

	private boolean crearDB = true;
	private ArrayList<String> imports = new ArrayList<String>(Arrays.asList("jcrystal.datetime.*", "jcrystal.PrintWriterUtils", "static jcrystal.JSONUtils.*"));
	private String entityPackage = AndroidClient.paqueteEntidades;
	private String netPackage = AndroidClient.netPackage;
	
	private ClientGeneratorDescriptor descriptor;
	public GeneradorEntidad(AbsClientGenerator<?> client, ClientGeneratorDescriptor descriptor, List<String> imports) {
		super(client);
		this.descriptor = descriptor;
		entityValidatorGenerator = new AndroidValidators(client);
		this.imports.addAll(imports);
	}
	
	public GeneradorEntidad(AbsClientGenerator<?> client, ClientGeneratorDescriptor descriptor, String entityPackage, String netPackage, boolean crearDB, List<String> imports) {
		this(client, descriptor, imports);
		this.netPackage = netPackage;
		this.entityPackage = entityPackage;
		this.crearDB = crearDB;
	}
	
	private void crearInterfaz(final EntityClass entidad, final JsonLevel level){
		final JavaCode cliente = new JavaCode(){{
			$("package "+entityPackage+";");
			$import(imports.toArray(new String[0]));
			$("public interface " + entidad.clase.simpleName+level.baseName()+" extends " + netPackage+".ISerializable", ()->{
				for(EntityField field : descriptor.getFields(entidad).get(level))
					$($($convert(client.entityUtils.convertToRawType(field.f))) + " " + field.name() + "();");
				if(entidad.key != null)
					$("public " + $($convert(entidad.key.getSingleKeyType()))+" $Key();");
			});
		}};
		
		client.exportFile(cliente, entityPackage.replace(".", File.separator) + File.separator + entidad.clase.getSimpleName()+level.baseName() + ".java");
	}
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
			$("package "+entityPackage+";");
			$import(imports.toArray(new String[0]));
			String extras = interfaces.isEmpty()?"":(" implements " + interfaces);
			if(entidad.padre != null && entidad.padre.campoSelector != null)
				extras = " extends " + entidad.padre.name() + extras;
			
			$("public class " + entidad.clase.simpleName + extras, ()->{
				$("/* EXTENSIONS */");
				$("/* END */");
				for(final EntityField f : definitiveFields){
					$("public "+$($convert(client.entityUtils.convertToRawType(f.f)))+" " + f.name() + ";");
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
						context.utils.extrators.jsonObject.procesarCampo(this, entidad.clase, f.fieldKeyAccessor().prefix("this."));
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
										context.utils.extrators.jsonObject.procesarCampo(this, entidad.clase, f.fieldKeyAccessor().prefix("this."));
								});
								$("break;");
							}
						});
					}).$else(()->{
						for(final EntityField f : definitiveFields)
							context.utils.extrators.jsonObject.procesarCampo(this, entidad.clase, f.fieldKeyAccessor().prefix("this."));
					});
				});
				$("@Override public void toJson(java.io.Print"+ (ContextType.isAndroid() ? "Stream":"Writer")+ " _pw)",()->{
					$("Serializer"+entidad.clase.simpleName+".toJson(_pw, this);");
				});
				if(entidad.key != null) {
					$("public " + $($convert(entidad.key.getSingleKeyType()))+" $Key()", ()->{
						if(entidad.key.isSimple())
							$("return this." + entidad.key.getLast().fieldName() + ";");
						else {
							$("Key $keyVal = new Key();");
							for(final EntityField f : entidad.key.getLlaves())
								$("$keyVal." + f.fieldName() + " = this." + f.fieldName() + ";");
							$("return $keyVal;");
						}
					});
					if(!entidad.key.isSimple())
						KeyGenerator.generate(this, entidad);
				}
				$("public static class ListUtils", ()->{
					AndroidClient.generateFromListJson(this, entidad.clase, null);
					for(final JsonLevel level : levels)
						AndroidClient.generateFromListJson(this, entidad.clase, level);
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
					AndroidClient.crearcodigoTokenSeguridad(client.context, this, entidad.clase);
				$("public static class Serializer extends Serializer" + entidad.clase.simpleName + "{}");
				$("public static class MapList",()->{
					TreeSet<String> procesado = new TreeSet<>();
					descriptor.getFields(entidad).fields.forEach((level, list)->{
						if(level != null && levels.contains(level))
							list.stream().filter(f->!procesado.contains(f.name())).forEach(f->{
								procesado.add(f.name());
								String className =entidad.clase.getSimpleName()+level.baseName();
								if(f.type().is(Long.class, long.class) || f.isAnnotationPresent(RelMto1.class)) {
									IJType type = f.getTargetEntity() != null ? f.getTargetEntity().key.getSingleKeyType() : f.type().getObjectType();
									$("public static<T extends "+className+"> java.util.Map<" + $(type) + ", T> By"+StringUtils.capitalize(f.name())+"(java.util.List<T> lista)",()->{
										$("java.util.Map<" + $(type) + ", T> ret = new java.util.TreeMap<>();");
										$("for(T val : lista)",()->{
											$("ret.put(val."+f.name()+"(), val);");
										});
										$("return ret;");
									});
								}	
								if(f.type().isEnum()) {
									IJType idType = ((JClass)f.type()).enumData.propiedades.get("id");
									$("public static<T extends "+className+"> java.util.Map<"+idType.getObjectType().getSimpleName()+", T> By"+StringUtils.capitalize(f.name())+"(java.util.List<T> lista)",()->{
										$("java.util.Map<"+idType.getObjectType().getSimpleName()+", T> ret = new java.util.TreeMap<>();");
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
									IJType idType = ((JClass)f.type()).enumData.propiedades.get("id");
									$("public static<T extends "+className+"> java.util.Map<"+idType.getObjectType().getSimpleName()+", java.util.List<T>> By"+StringUtils.capitalize(f.name())+"(java.util.List<T> lista)",()->{
										$("java.util.Map<"+idType.getObjectType().getSimpleName()+", java.util.List<T>> ret = new java.util.TreeMap<>();");
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
			client.exportFile(this, entityPackage.replace(".", File.separator) + File.separator + entidad.clase.simpleName + ".java");
		}};
		//CREAR SERIALIZER
		new JavaCode(){{
			$("package "+entityPackage+";");
			$import(imports.toArray(new String[0]));
			
			$("public class Serializer" + entidad.clase.simpleName, ()->{
				client.context.utils.generadorToJson.generateJsonify(entidad.clase, this, null, definitiveFields.stream().map(f -> f.propertyKeyAccessor().prefix("objeto.")).collect(Collectors.toList()));
				for(final JsonLevel level : levels)
					client.context.utils.generadorToJson.generateJsonify(entidad.clase, this, level, descriptor.getFields(entidad).get(level).stream().map(f -> f.propertyKeyAccessor().prefix("objeto.")).collect(Collectors.toList()));
				if(entidad.key != null && !entidad.key.isSimple())
					$("public static class Key",()->{
						context.utils.generadorToJson.generateSimplePlainToJson(PUBLIC | STATIC, "toJson", $( P(entidad.key.getSingleKeyType(), "objeto")), this, entidad.key.getLlaves().stream().filter(f->!f.isConstant).map(f ->{
							return f.propertyKeyAccessor("").prefix("objeto.");
						}).collect(Collectors.toList()));
					});
			});
			client.exportFile(this, entityPackage.replace(".", File.separator) + File.separator + "Serializer" + entidad.clase.simpleName + ".java");
		}};
		if (crearDB) {
			new GeneradorStorage().crearCodigoAlmacenamiento(client, entityPackage, entidad.clase, levels);
		}
		
	}

	@Override
	public void generateEntityKey(EntityClass entidad) {
		new JavaCode(){{
			$("package "+entityPackage+";");
			$("import jcrystal.PrintWriterUtils;");
			$("public class " + entidad.clase.simpleName, ()->{
				KeyGenerator.generate(this, entidad);
				$("public static class Serializer", ()->{
					$("public static class Key",()->{
						context.utils.generadorToJson.generateSimplePlainToJson(PUBLIC | STATIC, "toJson", $( P(entidad.key.getSingleKeyType(), "objeto")), this, entidad.key.getLlaves().stream().filter(f->!f.isConstant).map(f ->{
							return f.propertyKeyAccessor("").prefix("objeto.");
						}).collect(Collectors.toList()));
					});
				});
			});
			client.exportFile(this, entityPackage.replace(".", File.separator) + File.separator + entidad.clase.simpleName + ".java");
		}};
	}
	KeyGenerator KeyGenerator = new KeyGenerator();
	class KeyGenerator implements JavaCodeDelegator{
		JavaCode delegator;
		@Override
		public JavaCode getDelegator() {
			return delegator;
		}
		public void generate(JavaCode delegator, EntityClass entidad) {
			this.delegator = delegator; 
			$("public static class Key",()->{
				entidad.key.getLlaves().stream().forEach(k->{
					if(k.type().isPrimitive() || k.type().isPrimitiveObjectType() || k.type().is(String.class)) {
						$("public " + $($convert(k.type())) + " " + k.name() + ";");
						$("public " + $($convert(k.type())) + " " + k.name()+"()",()->{
							$("return " + k.name()+";");
						});
					}else if(k.getTargetEntity() != null) {
						EntityClass target = k.getTargetEntity();
						$("public " + $($convert(target.key.getSingleKeyType())) + " " + k.name() + ";");;
						$("public " + $($convert(target.key.getSingleKeyType())) + " " + k.name()+"()",()->{
							$("return " + k.name()+";");
						});
					}
					else 
						throw new NullPointerException("Unsupported key type " + k.type());
				});
				$("public Key(){}");
				$("public static Key fromJson(org.json.JSONObject json) throws org.json.JSONException",()->{
					$("if (json == null) {return null;}");
					$("Key ret = new Key();");
					for(final EntityField f : entidad.key.getLlaves())
						context.utils.extrators.jsonObject.procesarCampo(delegator, entidad.clase, f.fieldKeyAccessor().prefix("ret."));
					$("return ret;");
				});
			});
		}
	}
}

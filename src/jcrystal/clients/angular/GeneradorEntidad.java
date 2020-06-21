package jcrystal.clients.angular;

import static java.lang.reflect.Modifier.PUBLIC;
import static java.lang.reflect.Modifier.STATIC;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import jcrystal.clients.AbsEntityGenerator;
import jcrystal.clients.utils.EntityUtils;
import jcrystal.datetime.DateType;
import jcrystal.json.JsonLevel;
import jcrystal.model.server.db.EntityField;
import jcrystal.model.server.db.EntityClass;
import jcrystal.types.IJType;
import jcrystal.types.JClass;
import jcrystal.types.JType;
import jcrystal.types.vars.IAccessor;
import jcrystal.reflection.MaskedEnum;
import jcrystal.reflection.annotations.CrystalDate;
import jcrystal.reflection.annotations.LoginResultClass;
import jcrystal.reflection.annotations.jEntity;
import jcrystal.reflection.annotations.entities.RelMto1;
import jcrystal.utils.ListUtils;
import jcrystal.utils.StringSeparator;
import jcrystal.utils.StringUtils;
import jcrystal.utils.context.CodeGeneratorContext.ContextType;
import jcrystal.utils.context.CodeGeneratorContext;
import jcrystal.utils.context.ITypeConverter;
import jcrystal.utils.langAndPlats.TypescriptCode;
import jcrystal.utils.langAndPlats.delegates.TypescriptCodeDelegator;

public class GeneradorEntidad extends AbsEntityGenerator<WebClientTypescript>{
	public GeneradorEntidad(WebClientTypescript client) {
		super(client);
		entityValidatorGenerator = new GenerateValidators(client);
	}
	public void generateEntity(final EntityClass entidad, TreeSet<JsonLevel> levels){
		new TypescriptCode(){{
			StringSeparator interfaces = new StringSeparator(',');
			if(!levels.isEmpty()) {
				$("/* IMPORTS_EXTENSIONS */");
				$("/* END */");
				
				JsonLevel lastLevel[] = {null};
				levels.stream().forEach(level->{
					crearInterfaz(entidad, level, lastLevel[0]);
					String className =entidad.clase.getSimpleName()+level.baseName();
					interfaces.add(className);
				
					lastLevel[0] = level;
					$("import {"+className+"} from \"./"+className+"\"");
				});
				
				final List<EntityField> definitiveFields = ListUtils.join(client.descriptor.getFields(entidad).get(null), new ArrayList<EntityField>(client.descriptor.getFields(entidad).get(levels.last())));
				
				$("declare var moment: any;");
				$("export class " + entidad.clase.getSimpleName()+" implements "+interfaces, ()->{
					HashSet<String> fields = new HashSet<String>();
					for(final EntityField f : definitiveFields){
						if(fields.add(f.name())) {
							f.type().iterate(type->{
								if(type.isEnum() || type.isJAnnotationPresent(CrystalDate.class))
									$import(type);
								else if (type.isAnnotationPresent(jEntity.class)) {
									EntityClass target = client.context.data.entidades.get(type); 
									if(target.key == null || !target.key.isSimple())
										$import(type);
								}
							});
							
							if(f.type().isPrimitive()) {
								if(f.type().is(boolean.class))
									$("public "+f.name() + " : " + $($convert(f.type()))+ " = false;");
								else
									$("public "+f.name() + " : " + $($convert(f.type()))+ " = 0;");
							}else 
								$("public " + f.name() + ":" + $($convert(client.entityUtils.convertToRawType(f.f))) + ";");
						}
					}
					$("/* EXTENSIONS */");
					$("/* END */");
					$("constructor()", ()->{});
					
					for(final EntityField f : definitiveFields){
						if(f.type().isEnum()) {
							$("set"+StringUtils.capitalize(f.name()) + "("+f.name()+": "+f.type().getSimpleName()+")",()->{
								$("this."+f.name()+" = "+f.name()+"==null ? null : "+f.name()+".id;");
							});
							$("get"+StringUtils.capitalize(f.name()) + "() : "+f.type().getSimpleName(),()->{
								$("return "+f.type().getSimpleName()+".getFromId(this."+f.name()+");");
							});
						}
						else {
							$("set"+StringUtils.capitalize(f.name()) + "("+f.name()+": "+$($convert(client.entityUtils.convertToRawType(f.f)))+")",()->{
								$("this."+f.name()+" = "+f.name()+";");
							});
							$("get"+StringUtils.capitalize(f.name()) + "() : "+$($convert(client.entityUtils.convertToRawType(f.f))),()->{
								$("return this."+f.name()+";");
							});
						}
					}
					generateJsonify(entidad.clase, this, null, definitiveFields.stream().map(f -> f.fieldKeyAccessor()).collect(Collectors.toList()));
					for(final JsonLevel level : levels){
						generateJsonify(entidad.clase, this, level, definitiveFields.stream().map(f -> f.fieldKeyAccessor()).collect(Collectors.toList()));
						client.ResultGenerator.code = this;
						client.ResultGenerator.generateFromJson(entidad.clase, level, client.descriptor.getFields(entidad).get(level).stream().map(f->f.fieldKeyAccessor()).collect(Collectors.toList()), false);
					}
					client.ResultGenerator.generateFromJson(entidad.clase, null, definitiveFields.stream().map(f->f.fieldKeyAccessor()).collect(Collectors.toList()), true);
					
					if (entidad.clase.isAnnotationPresent(LoginResultClass.class)) {
						client.crearcodigoTokenSeguridad(this, entidad.clase, client.descriptor.getFields(entidad).last().stream().map(f->f.f).collect(Collectors.toList()));
					}
					$("store(key : string)",()-> {
						$("localStorage.setItem( key, JSON.stringify(this));");
					});
					$("public static retrieve(key : string)",()-> {
						$("var ret = localStorage.getItem(key);");
						$if("ret != null",()->{
							$("return this.fromJsonLocal(JSON.parse(ret));");
						});
						$("return null;");
					});
					if(entidad.key != null)
						$("$Key() : " + $($convert(entidad.key.getSingleKeyType())), ()->{
							if(entidad.key.isSimple())
								$("return this." + entidad.key.getLast().fieldName() + ";");
							else {
								$("let $keyVal = new "+entidad.clase.getSimpleName()+".Key();");
								for(final EntityField f : entidad.key.getLlaves())
									$("$keyVal." + f.fieldName() + " = this." + f.fieldName() + ";");
								$("return $keyVal;");
							}
						});
				});
			}
			$("export module " + entidad.clase.getSimpleName(), ()->{
				if(client.descriptor.getFields(entidad).fields.keySet().stream().anyMatch(level->level != null && levels.contains(level))) {
					$("export class Sort",()->{
						TreeSet<String> procesado = new TreeSet<>();
						client.descriptor.getFields(entidad).fields.forEach((level, list)->{
							if(level != null && levels.contains(level))
								list.stream().filter(f->!procesado.contains(f.name())).forEach(f->{
									procesado.add(f.name());
									String className =entidad.clase.getSimpleName()+level.baseName();
									if(f.type().is(String.class)){
										$("public static By"+StringUtils.capitalize(f.name())+"<T extends "+className+">(array:T[]):T[]",()->{
											$("array.sort(function(a,b) {return (a[\""+f.name()+"\"] > b[\""+f.name()+"\"]) ? 1 : ((b[\""+f.name()+"\"] > a[\""+f.name()+"\"]) ? -1 : 0)});");
											$("return array;");
										});
										$("public static By"+StringUtils.capitalize(f.name())+"_IgnCase<T extends "+className+">(array:T[]):T[]",()->{
											$("array.sort(function(a,b) {return (a[\""+f.name()+"\"].toLowerCase() > b[\""+f.name()+"\"].toLowerCase()) ? 1 : ((b[\""+f.name()+"\"].toLowerCase() > a[\""+f.name()+"\"].toLowerCase()) ? -1 : 0)});");
											$("return array;");
										});
									}
								});
						});
					});
					$("export class MapList",()->{
						TreeSet<String> procesado = new TreeSet<>();
						client.descriptor.getFields(entidad).fields.forEach((level, list)->{
							if(level != null && levels.contains(level))
								list.stream().filter(f->!procesado.contains(f.name())).forEach(f->{
									procesado.add(f.name());
									String className =entidad.clase.getSimpleName()+level.baseName();
									if(f.type().is(Long.class, long.class) || f.type().isEnum()) {
										$("public static By"+StringUtils.capitalize(f.name())+"<T extends "+className+">(a:T[]):Map<"+$($convert(f.type()))+", T>",()->{
											$("return new Map(a.map((i):["+$($convert(f.type()))+",T] => {return [i."+f.name()+", i]}));");
										});
									}	
								});
						});
						$("public static By<K, T>(a:T[], mapper : (val:T)=>K):Map<K, T>",()->{
							$("return new Map(a.map((i):[K,T] => {return [mapper(i), i]}));");
						});
					});
					$("export class Group",()->{
						TreeSet<String> procesado = new TreeSet<>();
						client.descriptor.getFields(entidad).fields.forEach((level, list)->{
							if(level != null && levels.contains(level))
								list.stream().filter(f->!procesado.contains(f.name())).forEach(f->{
									procesado.add(f.name());
									String className =entidad.clase.getSimpleName()+level.baseName();
									if(f.type().is(Long.class, long.class) || f.type().isEnum() || f.isAnnotationPresent(RelMto1.class)) {
										String mapType = f.isAnnotationPresent(RelMto1.class) ? $($convert(f.getTargetEntity().key.getSingleKeyType())) : $($convert(f.type()));
										$("public static By"+StringUtils.capitalize(f.name())+"<T extends "+className+">(a:T[]):Map<"+mapType+", T[]>",()->{
											$("return Group.By(a, val=>val."+f.name()+");");
										});
									}
								});
						});
						String className =entidad.clase.getSimpleName();
						$("public static By<K, T>(a:T[], mapper : (val:T)=>K):Map<K, T[]>",()->{
							$("const map = new Map();");
							$("a.forEach(e=>",()->{
								$("var list = map.get(mapper(e));");
								$if("!list",()->{
									$("map.set(mapper(e), [e]);");
								});
								$else(()->{
									$("list.push(e);");
								});	
							},");");
							$("return map;");
						});
					});
				}
				if(entidad.key != null && !entidad.key.isSimple())
					KeyGenerator.generate(this, entidad);
			});
			$imports();
			$("import {JSONUtils} from \"../JSONUtils\";");
			$("import {Injectable} from \"@angular/core\";");
			client.requiredClasses.addAll(imports);
			client.exportFile(this, client.paqueteEntidades.replace(".", File.separator) + File.separator + entidad.clase.getSimpleName() + ".ts");
		}};
	}
	@Override
	public void generateEntityKey(final EntityClass entidad){
		new TypescriptCode(){{
			StringSeparator interfaces = new StringSeparator(',');
			$("export module " + entidad.clase.getSimpleName(), ()->{
				KeyGenerator.generate(this, entidad);
			});
			client.exportFile(this, client.paqueteEntidades.replace(".", File.separator) + File.separator + entidad.clase.getSimpleName() + ".ts");
		}};
	}
	KeyGenerator KeyGenerator = new KeyGenerator(); 
	class KeyGenerator implements TypescriptCodeDelegator{
		TypescriptCode delegator;
		@Override
		public TypescriptCode getDelegator() {
			return delegator;
		}
		public void generate(TypescriptCode delegator, EntityClass entidad) {
			this.delegator = delegator; 
			$("export class Key",()->{
				entidad.key.getLlaves().stream().forEach(k->{
					if(k.type().isPrimitive() || k.type().isPrimitiveObjectType() || k.type().is(String.class) || k.type().isJAnnotationPresent(CrystalDate.class))
						$("public "+k.name() + " : " + $($convert(k.type()))+";");
					else if(k.getTargetEntity() != null) {
						EntityClass target = k.getTargetEntity();
						if(!target.key.isSimple())
							$import(k.type());
						$("public "+k.name() + " : " + $($convert(target.key.getSingleKeyType()))+";");;
					}
					else 
						throw new NullPointerException("Unsupported key type " + k.type());
				});
				$("constructor(){ }");
				$("public static fromJson(json : any):Key",()->{
					$("if (!json) {return null;}");
					$("let ret = new Key();");
					for(final EntityField f : entidad.key.getLlaves())
						client.ResultGenerator.procesarCampo(f.fieldKeyAccessor());
					$("return ret;");
				});
			});
		}
	}
	private ITypeConverter dbFieldConverter = new ITypeConverter() {
		@Override
		public IJType convert(IJType type) {
			if(type.isEnum())
				return type;
			CodeGeneratorContext cnt = CodeGeneratorContext.get(); 
			return cnt.typeConverter == null ? type : cnt.typeConverter.convert(type);
		}
	};
	private void crearInterfaz(final EntityClass entidad, final JsonLevel level, JsonLevel lastLevel){
		new TypescriptCode(){{
			if(lastLevel != null)
				$("import { " + entidad.clase.getSimpleName() + lastLevel.baseName()+" } from \"./"+entidad.clase.getSimpleName()+lastLevel.baseName()+"\";");
			else if(entidad.key != null && !entidad.key.isSimple())
				$("import { " + entidad.clase.getSimpleName() + " } from \"./"+entidad.clase.getSimpleName()+"\";");
			$("export interface " + entidad.clase.getSimpleName()+level.baseName() + (lastLevel!=null?" extends "+entidad.clase.getSimpleName()+lastLevel.baseName():""), ()->{
				HashSet<String> fields = new HashSet<String>();
				Consumer<EntityField> procesador = f -> {
					if(fields.add(f.name())) {
						$(f.name()+" : "+$($convert(client.entityUtils.convertToRawType(f.f)))+";");
						$("set"+StringUtils.capitalize(f.name()) + "("+f.name()+" : "+$(dbFieldConverter.convert(client.entityUtils.convertToRawType(f.f)))+");");
						$("get"+StringUtils.capitalize(f.name()) + "() : "+$(dbFieldConverter.convert(client.entityUtils.convertToRawType(f.f)))+";");
						
						f.type().iterate(type->{
							if(type.isEnum() || type.isJAnnotationPresent(CrystalDate.class))
								$import(type);
							else if (type.isAnnotationPresent(jEntity.class)) {
								EntityClass target = client.context.data.entidades.get(type); 
								if(target.key == null || !target.key.isSimple())
									$import(type);
							}
						});
						
							
					}
				};
				client.descriptor.getFields(entidad).get(level).forEach(procesador);
				client.descriptor.getFields(entidad).get(null).forEach(procesador);
				if(lastLevel == null && entidad.key != null)
					$("$Key() : " + $($convert(entidad.key.getSingleKeyType()))+";");
			});
			$imports();
			client.requiredClasses.addAll(imports);
			client.exportFile(this, client.paqueteEntidades.replace(".", File.separator) + File.separator + entidad.clase.getSimpleName()+level.baseName() + ".ts");
		}};
	}
	//TODO:Arreglar al nuevo manejo de tipos
	public static void generateJsonify(final JClass clase, final TypescriptCode code, final JsonLevel level, final List<IAccessor> campos) {
		if (campos.isEmpty())
			return;
		code.new B() {{
			$M(PUBLIC,"any", "toJson" + (level == null ? "" : level.baseName()), $(), ()->{
				$V("any", "__ret", "{}");
				R(code, 0, campos);
			});
			String listType = clase.getSimpleName() +(level == null ? "" : level.baseName())+ "[]";
			$M(PUBLIC | STATIC, "any", "toJson" + (level == null ? "" : level.baseName()) + clase.getSimpleName(), $(P(new JType(null, listType), "lista")), ()->{
				if(level==null)
					$("return lista.map(valor=>valor.toJson());");
				else
					$("return lista.map(valor=>(valor as "+clase.getSimpleName()+").toJson());");
			});
		}};
	}
	private static void R(final TypescriptCode code, final int i, final List<IAccessor> campos) {
		if (i == campos.size()) {
			code.add("return __ret;");
		} else {
			code.new B() {{
				String typo = $($convert(campos.get(i).type()));
				
				Runnable toString = ()->{
					String accessor = campos.get(i).type().isPrimitive() ? campos.get(i).read() : ("val" + i);
					if(campos.get(i).type().isPrimitive() && ContextType.WEB.is()){
						accessor = "this."+accessor;
					}
					String json = accessor;
					
					if (campos.get(i).type().isJAnnotationPresent(CrystalDate.class)) {
						json = accessor + ".format()";
					} else if (campos.get(i).type().is(Date.class)){
						final DateType tipo = campos.get(i).isJAnnotationPresent(CrystalDate.class)?campos.get(i).getJAnnotation(CrystalDate.class).value():DateType.DATE_MILIS;
						json = "jsonQuote(DateType." + tipo + ".FORMAT.format(" + accessor + "))";
					}else if( campos.get(i).type().isArray() && campos.get(i).type().getInnerTypes().get(0).isSubclassOf(MaskedEnum.class) ) {
						json = campos.get(i).type().getInnerTypes().get(0).getSimpleName()+".maskArray("+accessor+")";
					}

					$("__ret." + campos.get(i).name() + " = " + json + ";");
				};
				if (campos.get(i).type().isPrimitive()) {
					toString.run();
				} else{
					String accessor = campos.get(i).read();
					$if_let((String)null, "val" + i, accessor, null, toString);
				}
				R(code, i + 1, campos);
			}};
		}
	}
}

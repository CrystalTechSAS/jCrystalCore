package jcrystal.server.databases;

import static java.lang.reflect.Modifier.PUBLIC;
import static java.lang.reflect.Modifier.STATIC;
import static jcrystal.utils.StringUtils.capitalize;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.appengine.api.datastore.Text;

import jcrystal.entity.types.security.Password;
import jcrystal.json.JsonLevel;
import jcrystal.main.data.ClientContext;
import jcrystal.main.data.DataBackends.BackendWrapper;
import jcrystal.model.server.db.EntityField;
import jcrystal.model.server.db.EntityRelation;
import jcrystal.model.server.db.EntityClass;
import jcrystal.reflection.MaskedEnum;
import jcrystal.reflection.annotations.CrystalDate;
import jcrystal.reflection.annotations.IndexType;
import jcrystal.reflection.annotations.jEntity;
import jcrystal.reflection.annotations.validation.EmailValidation;
import jcrystal.reflection.annotations.validation.EmptyValidation;
import jcrystal.reflection.annotations.validation.MaxValidation;
import jcrystal.reflection.annotations.validation.MinValidation;
import jcrystal.reflection.annotations.validation.Validate;
import jcrystal.reflection.annotations.validation.date.GreaterThanValidation;
import jcrystal.reflection.annotations.validation.date.LessThanValidation;
import jcrystal.types.JVariable;
import jcrystal.types.vars.IAccessor;
import jcrystal.utils.StringSeparator;
import jcrystal.utils.ValidationException;
import jcrystal.utils.langAndPlats.JavaCode;

public abstract class AbsUtilsEntityGenerator extends JavaCode{
	protected final ClientContext context;
	protected EntityClass entidad;
	protected BackendWrapper back;
	public AbsUtilsEntityGenerator(AbsEntityGenerator parent) {
		this.entidad = parent.entidad;
		this.context = parent.context;
		this.back = parent.back;
	}
	public final JavaCode generate(boolean putHeaders) {
		if(putHeaders) {
			$("package "+entidad.clase.getPackageName()+";");
			$import(getImports());
			$import(getAdditionalImports());
			
		}
		putMeta();
		putPost();
		putSerializer();
		putUtils();
		return this;
	}
	
	public static String[] getImports() {
		return new String[] {"jcrystal.datetime.*", "jcrystal.PrintWriterUtils"};
	}
	protected abstract void putUtils();
	protected abstract String[] getAdditionalImports();
	private void putMeta() {
		$("class Meta" + entidad.getTipo(), ()->{
			$("public static enum M",()->{
				for(final EntityField f : entidad.properties){
					if(f.type().is(Password.class)) {
					}else {
						$(f.name()+",");
					}
				}
			});
			$("public enum F",()->{
				for(final EntityField f : entidad.properties){
					if(f.indexType != IndexType.NONE)
						$(f.name()+",");
				}
				for(EntityRelation f : entidad.manyToOneRelations)
					$(f.name()+",");
			});
			$("@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)");
			$("public @interface Order",()->{
				$("public M[] value() default {};");
			});
			$("@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)");
			$("public @interface Index",()->{
				$("public String name();");
				$("public F[] fields();");
			});
			$("@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)");
			$("public @interface Indexes",()->{
				$("public Index[] value();");
			});
		});
	}
	private void putPost() {
		if(!entidad.isSecurityToken()) {
			$("@SuppressWarnings(\"unused\")");
			$("class Post" + entidad.clase.getSimpleName(), ()->{
				//TODO:postClass.addAll(generarCodigo(entidad));
				JsonLevel[] last = {null};
				context.utils.SERVER_DESCRIPTOR.getFields(entidad).fields.forEach((level, fields)->{
					final Predicate<EntityField> filter = f-> f.isPostable(level);
					long cuentaPropiedades = fields.stream().filter(filter).count();
					if(cuentaPropiedades>0 && level.level > JsonLevel.ID.level){
						$("@jcrystal.reflection.annotations.Post(level = jcrystal.json.JsonLevel."+level.name()+")");
						$("public static class "+level.baseName() +(last[0]==null?"":" extends "+last[0].baseName()) + " implements java.io.Serializable", ()->{
							$("public "+level.baseName()+"(){}");
							fields.stream().filter(filter).forEach(f->{
								String visibility = f.keyData == null ? "public" : "private"; 
								if(f.getTargetEntity() != null) {
									if(f.getTargetEntity().entidad.internal()) {
										if(f.type().isIterable())
											$(visibility + " java.util.List<"+$($convert(f.getTargetEntity().clase))+".Post." + level.baseName() + "> " + f.fieldName()+";");
										else
											$(visibility + " "+$($convert(f.type()))+".Post." + level.baseName() + " " + f.fieldName()+";");
									}else
										$(visibility + " "+$($convert(f.getTargetEntity().key.getSingleKeyType()))+" " + f.fieldName()+";");
								}else if(f.isText())
									$(visibility + " String " + f.fieldName()+";");
								else
									$(visibility + " "+$($convert(f.type()))+" " + f.fieldName()+";");
							});
							if(last[0] == null && entidad.key != null && entidad.key.stream().anyMatch(f->f.editable)) {
								$("public " + entidad.clase.name() + " tryGet()", ()->{
									$("return " + entidad.clase.name() + ".tryGet(" + entidad.key.getKeyValues() + ");");
								});
								$("public " + entidad.clase.name() + " tryGet(" + entidad.clase.name() + " defValue)", ()->{
									$("return " + entidad.clase.name() + ".tryGet(" + entidad.key.getKeyValues() + ", defValue);");
								});
								$("public " + entidad.clase.name() + " get()", ()->{
									$("return " + entidad.clase.name() + ".get(" + entidad.key.getKeyValues() + ");");
								});
							}
							entidad.manyToOneRelations.stream().filter(f-> f.level.level <= level.level && f.editable && f.getTarget().key.isSimple()).forEach(r->
								$("public "+$($convert(r.getTarget().key.getKeyTypes().get(0)))+" " + r.fieldName+";")
							);
							$("public "+level.baseName()+"(org.json.JSONObject json)", ()->{
								if(last[0]!=null)
									$("super(json);");
								fields.stream().filter(filter).forEach((f)->{
									if(f.getTargetEntity() != null) {
										if(f.getTargetEntity().entidad.internal()) {
											$("this."+f.fieldName() + " = " + $(f.getTargetEntity().clase)+".Post."+level.baseName()+".getFrom"+level.baseName()+"(json.optJSONArray(\"" + f.name() + "\"));");
										}else if(f.getTargetEntity().key.isSimple())
											context.utils.extrators.jsonObject.procesarCampo(this, entidad.clase, f.fieldKeyAccessor().prefix("this."));
										else
											$("this."+f.fieldName() + " = " + $(f.type())+".Post.getKey(json.optJSONObject(\"" + f.name() + "\"));");
									}
									else
										context.utils.extrators.jsonObject.procesarCampo(this, entidad.clase, f.fieldAccessor().prefix("this."));
								});
								entidad.manyToOneRelations.stream().filter(f-> f.level.level <= level.level && f.editable && f.getTarget().key.isSimple()).forEach(f->
									context.utils.extrators.jsonObject.procesarCampo(this, entidad.clase, f.field.fieldKeyAccessor().prefix("this."))
								);
							});
							$("public static " + level.baseName() + " getFrom" + level.baseName() + "(org.json.JSONObject json)", ()->{
								$ifNull("json",()->{
									$("return null;");
								});
								$("return new " + level.baseName() + "(json);");
							});
							$("public " + level.baseName() + "(javax.servlet.http.HttpServletRequest req)", ()->{
								fields.stream().filter(filter).forEach(f->{
									context.utils.extrators.formData.procesarCampo(this, entidad.clase, f.fieldKeyAccessor().$this());
								});
								entidad.manyToOneRelations.stream().filter(f-> f.level.level <= level.level && f.editable && f.getTarget().key.isSimple()).forEach(f->
									context.utils.extrators.formData.procesarCampo(this, entidad.clase, f.field.fieldKeyAccessor().$this())
								);
							});
							$("public static " + level.baseName() + " getFrom" + level.baseName() + "(javax.servlet.http.HttpServletRequest req)", ()->{
								$ifNull("req",()->{
									$("return null;");
								});
								$("return new " + level.baseName() + "(req);");
							});
							$("public static java.util.List<" + level.baseName() + "> getFrom"+level.baseName()+"(org.json.JSONArray json)",()->{
								$if("json == null","return null;");
								$("java.util.ArrayList<" + level.baseName() + "> ret = new java.util.ArrayList<>(json.length());");
								$("for(int pos = 0; pos < json.length(); pos++)",()->{
									$("ret.add(new "+level.baseName()+"(json.getJSONObject(pos)));");
								});
								$("return ret;");
							});
							$("public static java.util.List<" + level.baseName() + "> getFrom"+level.baseName()+"(java.util.List<"+entidad.getTipo() + "> data)",()->{
								$if("data == null","return null;");
								$("java.util.ArrayList<" + level.baseName() + "> ret = new java.util.ArrayList<>(data.size());");
								$("for(int pos = 0; pos < data.size(); pos++)",()->{
									$("ret.add(new "+level.baseName()+"(data.get(pos)));");
								});
								$("return ret;");
							});
							$("public "+level.baseName()+"("+entidad.clase.name()+" entidad)", ()->{
								if(last[0]!=null)
									$("super(entidad);");
								fields.stream().filter(filter).filter(f->f.keyData == null && !f.isFinal).forEach((f)->{
									if(f.getTargetEntity() != null) {
										if(f.getTargetEntity().entidad.internal()) {
											if(f.type().isIterable())
												$("this."+f.fieldName() + " = " + $(f.getTargetEntity().clase)+".Post."+level.baseName()+".getFrom"+level.baseName()+"(entidad." + f.fieldName()+"());");
											else
												$("this."+f.fieldName() + " = new " + $(f.getTargetEntity().clase)+".Post."+level.baseName()+"(entidad." + f.fieldName()+"());");
										}else
											$("this."+f.fieldName()+" = entidad." + f.fieldName()+"();");
									}else if(f.isArray() && f.type().firstInnerType().isSubclassOf(MaskedEnum.class))
										$("this."+f.fieldName()+" = entidad." + f.fieldName()+"Array();");
									else if(f.isText())
										$("this."+f.fieldName()+" = entidad." + f.fieldName()+"();");
									else if(f.f.type().isJAnnotationPresent(CrystalDate.class))
										$("this."+f.fieldName()+" = entidad." + f.fieldName()+"();");
									else
										$("this."+f.fieldName()+" = entidad." + f.fieldName()+"();");
								});
							});
							$("public "+entidad.clase.name()+" merge("+entidad.clase.name()+" entidad)", ()->{
								if(last[0]!=null)
									$("super.merge(entidad);");
								fields.stream().filter(filter).filter(f->f.keyData == null && !f.isFinal).forEach((f)->{
									if(f.getTargetEntity() != null && f.getTargetEntity().entidad.internal()) {
										if(f.type().isIterable())
											$("entidad." + f.fieldName()+"("+$(f.getTargetEntity().clase)+".Post."+level.baseName()+".create(" + f.fieldName()+"));");
										else
											$("entidad." + f.fieldName()+"(" + f.fieldName()+".create());");
									}
									else if(f.f.type().isJAnnotationPresent(CrystalDate.class))
										$("entidad." + f.fieldName()+"(this."+f.fieldName()+"==null?null:this."+f.fieldName()+");");
									else
										$("entidad." + f.fieldName()+"(this."+f.fieldName()+");");
								});
								entidad.manyToOneRelations.stream().filter(f-> f.level.level <= level.level && f.editable && f.getTarget().key.isSimple()).forEach(f->{
									$("entidad." + f.fieldName()+"(this."+f.fieldName()+");");
								});
								$("return entidad;");
							});
							Stream<String> params = entidad.streamFinalFields().filter(f->!f.isConstant && !filter.test(f)).map(p->{
								if(p.getTargetEntity() != null)
									return $($convert(p.getTargetEntity().key.getSingleKeyType())) + " " + p.fieldName();
								return $($convert(p.type())) + " " + p.fieldName();
							});
							params = Stream.concat(params, entidad.onConstructMethods.stream().flatMap(f->f.params.stream()).map(f->$(f.type)+" " + f.name));
							$("public "+entidad.clase.name()+" create(" + params.collect(Collectors.joining(", ")) + ")", ()->{
								StringSeparator paramsValues = entidad.getFinalFields();
								entidad.onConstructMethods.stream().flatMap(f->f.params.stream()).forEach(f->paramsValues.add(f.name));;
								$("return merge(new " + entidad.clase.name()+"(" + paramsValues + "));");
							});
							if(entidad.key != null && entidad.streamFinalFields().filter(f->!f.isConstant && !filter.test(f)).anyMatch(f->f.getTargetEntity() != null && back.checkExposed(f.getTargetEntity()))) {
								params = entidad.streamFinalFields().filter(f->!f.isConstant && !filter.test(f)).map(p->
									$($convert(p.type())) + " " + p.fieldName()
								);
								params = Stream.concat(params, entidad.onConstructMethods.stream().flatMap(f->f.params.stream()).map(f->$(f.type)+" " + f.name));
								
								$("public "+entidad.clase.name()+" create(" + params.collect(Collectors.joining(", ")) + ")", ()->{
									StringSeparator paramsValues = entidad.getFinalFields();
									entidad.onConstructMethods.stream().flatMap(f->f.params.stream()).forEach(f->paramsValues.add(f.name));;
									$("return merge(new " + entidad.clase.name()+"(" + paramsValues + "));");
								});
							}
							if(entidad.entidad.internal())
								$("public static java.util.List<"+entidad.clase.name()+"> create(java.util.List<"+level.baseName()+"> data)", ()->{
									$if("data == null","return null;");
									$("java.util.ArrayList<" + entidad.clase.name() + "> ret = new java.util.ArrayList<>(data.size());");
									$("for(int pos = 0; pos < data.size(); pos++)",()->{
										$("ret.add(data.get(pos).create());");
									});
									$("return ret;");
								});
							fields.stream().filter(filter).forEach(f->{
								Consumer<String> getterSetter = (type)->{
									$("public void set" + capitalize(f.fieldName()) + "(" + type + " " + f.fieldName()+")", ()->{
										$("this." + f.fieldName() + " = " + f.fieldName()+";");
									});
									$("public "+type+" get" + capitalize(f.fieldName()) + "()", ()->{
										$("return this." + f.fieldName()+";");
									});
								};
								if(f.isAccountField())
									getterSetter.accept("String");
								else if(f.getTargetEntity()!=null) {
									if(f.getTargetEntity().entidad.internal()) {
										if(f.type().isIterable())
											getterSetter.accept("java.util.List<"+$($convert(f.getTargetEntity().clase))+".Post." + level.baseName()+">");
										else
											getterSetter.accept($($convert(f.type()))+".Post." + level.baseName());
									}else
										getterSetter.accept($($convert(f.getTargetEntity().key.getSingleKeyType())));
								}else
									getterSetter.accept($($convert(f.type())));
							});
							$("public "+level.baseName()+" validate()",()->{
								if(last[0] != null)
									$("super.validate();");
								fields.stream().filter(filter).forEach(f->{
									doValidation(this, f);
								});
								$("return this;");
							});
						});
						last[0] = level;
					}
				});
				if(entidad.key != null && !entidad.key.isSimple()) {
					$("public static "+entidad.clase.name()+".Key getKey(org.json.JSONObject json)",()->{
						$ifNull("json",()->{
							$("return null;");
						});
						List<JVariable> fields = entidad.key.getLlaves().stream().filter(f->!f.isConstant).map(f->{
							if(f.type().isAnnotationPresent(jEntity.class)) {
								EntityClass target = context.data.entidades.get(f.type());
								return P($convert(target.key.getSingleKeyType()), f.fieldName());	
							}else
								return P($convert(f.type()), f.fieldName());
						}).collect(Collectors.toList());
						fields.forEach(f->{
							$($(f.type())+" " + f.name()+";");
						});
						entidad.key.getLlaves().stream().forEach(f->{
							if(f.type().isAnnotationPresent(jEntity.class)) {
								EntityClass target = context.data.entidades.get(f.type());
								if(target.key.isSimple())
									context.utils.extrators.jsonObject.procesarCampo(this, entidad.clase, f.fieldKeyAccessor());
								else
									$(f.fieldName() + " = " + $(target.clase) + ".Post.getKey(json.getJSONObject(\""+f.fieldName()+"\"));");
							}else
								context.utils.extrators.jsonObject.procesarCampo(this, entidad.clase, f.fieldAccessor());
						});
						$("return new "+entidad.clase.name()+".Key("+fields.stream().map(f->f.name()).collect(Collectors.joining(", "))+");");
					});
				}

			});
		}
	}
	private void putSerializer() {
		$("class Serializer" + entidad.clase.getSimpleName(), ()->{
			TreeMap<JsonLevel, List<IAccessor>> map = new TreeMap<>(JsonLevel.nullComparator);
			
			context.utils.SERVER_DESCRIPTOR.getFields(entidad).fields.forEach((k,v) -> map.put(k, v.stream().map(f -> f.propertyKeyAccessor().prefix("objeto.")).collect(Collectors.toList())));
			
			map.remove(null);
			context.utils.generadorToJson.generateJsonify(entidad.clase, this, map);
			if(entidad.key != null && !entidad.key.isSimple())
				$("public static class Key",()->{
					context.utils.generadorToJson.generateSimplePlainToJson(PUBLIC | STATIC, "toJson", $( P(entidad.key.getSingleKeyType(), "objeto")), this, entidad.key.getLlaves().stream().filter(f->!f.isConstant).map(f ->{
						return f.propertyKeyAccessor("").prefix("objeto.");
					}).collect(Collectors.toList()));
				});
		});
	}
	static void doValidation(JavaCode code, EntityField f) {
		code.new B() {{
			if(f.isPrimitive()){
				f.f.ifAnnotation(Validate.class, val->{
					if(val.min() != Integer.MIN_VALUE){
						if(f.type().is(int.class, double.class, long.class, float.class)){
							$if(val.min() + " > " + f.fieldName(),()->{
								$("throw new "+ValidationException.class.getName()+"(\"Invalid " + f.fieldName() + " value.\");");
							});
						}
					}
					if(val.max() != Integer.MAX_VALUE){
						if(f.type().is(int.class, double.class, long.class, float.class)){
							$if(val.max() + " < " + f.fieldName(),()->{
								$("throw new "+ValidationException.class.getName()+"(\"Invalid " + f.fieldName() + " value.\");");
							});
						}
					}
				});
				f.f.ifAnnotation(EmptyValidation.class, empty->{
					$if("0 == " + f.fieldName(),()->{
						$("throw new "+ValidationException.class.getName()+"(\"" + empty.value() + "\");");
					});
				});
				f.f.ifAnnotation(MinValidation.class, min->{
					$if(min.min() + " > " + f.fieldName(),()->{
						$("throw new "+ValidationException.class.getName()+"(\"" + min.value() + "\");");
					});
				});
				f.f.ifAnnotation(MaxValidation.class, max->{
					$if(max.max() + " < " + f.fieldName(),()->{
						$("throw new "+ValidationException.class.getName()+"(\"" + max.value() + "\");");
					});
				});
			}else if(f.type().is(String.class, Text.class)){
				f.f.ifAnnotation(Validate.class, val->{
					if(val.notEmpty())
						$if(f.fieldName() + " == null || " + f.fieldName() + ".trim().isEmpty()",()->{
							$("throw new "+ValidationException.class.getName()+"(\"Invalid " + f.fieldName() + " value.\");");
						});
					if(val.min() > 0)
						$if(f.fieldName() + " == null || " + val.min() + " > " + f.fieldName()+".length()",()->{
							$("throw new "+ValidationException.class.getName()+"(\"Invalid " + f.fieldName() + " value.\");");
						});
					if(val.max() != Integer.MAX_VALUE)
						$if(f.fieldName() + " != null && " + val.max() + " < " + f.fieldName()+".length()",()->{
							$("throw new "+ValidationException.class.getName()+"(\"Invalid " + f.fieldName() + " value.\");");
						});
				});
				f.f.ifAnnotation(EmptyValidation.class, empty->{
					$if(f.fieldName() + " == null || " + f.fieldName() + ".trim().isEmpty()",()->{
						$("throw new "+ValidationException.class.getName()+"(\"" + empty.value() + "\");");
					});
				});
				f.f.ifAnnotation(MinValidation.class, min->{
					$if(f.fieldName() + " == null || " + min.min() + " > " + f.fieldName()+".length()",()->{
						$("throw new "+ValidationException.class.getName()+"(\"" + min.value() + "\");");
					});
				});
				f.f.ifAnnotation(MaxValidation.class, max->{
					$if(f.fieldName() + " != null && " + max.max() + " < " + f.fieldName()+".length()",()->{
						$("throw new "+ValidationException.class.getName()+"(\"" + max.value() + "\");");
					});
				});
				f.f.ifAnnotation(EmailValidation.class, email->{
					$if("!org.apache.commons.validator.EmailValidator.getInstance().isValid("+f.fieldName()+")",()->{
						$("throw new "+ValidationException.class.getName()+"(\"" + email.value() + "\");");
					});
				});
			}else {
				f.f.ifAnnotation(EmptyValidation.class, empty->{
					$ifNull(f.fieldName(),()->{
						$("throw new "+ValidationException.class.getName()+"(\"" + empty.value() + "\");");
					});
				});
				
				f.f.ifAnnotation(Validate.class, val->{
					if(val.notEmpty())
						$ifNull(f.fieldName(),()->{
						$("throw new "+ValidationException.class.getName()+"(\"Invalid " + f.fieldName() + " value.\");");
					});
				});
				if(f.f.type().isJAnnotationPresent(CrystalDate.class)) {//TODO: hacer estas validaciones dependiendo del tipo de fecha (por ejemplo, CrystalDate debe ser de ma�ana en adelante)
					f.f.ifAnnotation(GreaterThanValidation.class, gt->{
						if(gt.now())
							$if(f.fieldName() + " == null || " + f.fieldName()+".before(new java.util.Date())",()->{
								$("throw new "+ValidationException.class.getName()+"(\"" + gt.value() + "\");");
							});
						else if(!gt.field().isEmpty())
							$if(f.fieldName() + " == null || " + f.fieldName()+".before("+gt.field()+")",()->{
								$("throw new "+ValidationException.class.getName()+"(\"" + gt.value() + "\");");
							});
					});
					f.f.ifAnnotation(LessThanValidation.class, lt->{
						if(lt.now())
							$if(f.fieldName() + " == null || " + f.fieldName()+".after(new java.util.Date())",()->{
								$("throw new "+ValidationException.class.getName()+"(\"" + lt.value() + "\");");
							});
						else if(!lt.field().isEmpty())
							$if(f.fieldName() + " == null || " + f.fieldName()+".after("+lt.field()+")",()->{
								$("throw new "+ValidationException.class.getName()+"(\"" + lt.value() + "\");");
							});
					});
				}
			}			
		}};
	}
}

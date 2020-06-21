package jcrystal.server.databases.google.bigtable;

import static jcrystal.utils.StringUtils.capitalize;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.appengine.api.datastore.Text;

import jcrystal.entity.types.LongText;
import jcrystal.entity.types.security.Password;
import jcrystal.main.data.ClientContext;
import jcrystal.model.server.db.EntityField;
import jcrystal.model.server.db.EntityClass;
import jcrystal.model.server.db.EntityRelation;
import jcrystal.reflection.MaskedEnum;
import jcrystal.reflection.annotations.CrystalDate;
import jcrystal.reflection.annotations.EntityKey;
import jcrystal.reflection.annotations.Selector;
import jcrystal.server.databases.AbsEntityGenerator;
import jcrystal.types.IJType;
import jcrystal.types.JMethod;
import jcrystal.types.JVariable;
import jcrystal.types.utils.GlobalTypes;
import jcrystal.utils.GlobalTypeConverter;
import jcrystal.utils.InternalException;
import jcrystal.utils.StringUtils;

public class GeneradorGoogleBigtable extends AbsEntityGenerator{
	protected final KeyConvertersClass KeyConverters;
	public GeneradorGoogleBigtable(ClientContext context) {
		super(context);
		KeyConverters = new KeyConvertersClass(this);
	}
	@Override
	public final void generateEntity(boolean keysOnly){
		context.utils.converters.dates.extending(()->{
			context.output.exportFile(back, new UtilsEntityGenerator(this).generate(true), entidad.clase.getPackageName().replace(".", "/")+"/"+entidad.getTipo()+".java");
		});
		context.output.addSection(entidad.clase, "GEN", new EntityGenerator(keysOnly));
	}
	private static String getProperty(IJType tipo, String nombrePropiedad, Object defaultValue){
		tipo = GlobalTypeConverter.INSTANCE.convert(tipo);
		if (tipo.is(String.class, Text.class, LongText.class))
			return "(String)(rawEntity.get(" + nombrePropiedad + "))";
		else if (tipo.is(JSONObject.class))
			return "jcrystal.utils.EntityUtils.getJsonObject(rawEntity, " + nombrePropiedad + ")";
		else if (tipo.is(JSONArray.class))
			return "jcrystal.utils.EntityUtils.getJsonArray(rawEntity, " + nombrePropiedad + ")";
		else if (tipo.isPrimitive()){
			if (defaultValue != null)
				return "jcrystal.db.PrimitiveUtils.get"+StringUtils.capitalize(tipo.getSimpleName())+"(rawEntity.get(" + nombrePropiedad + "), " + defaultValue + ")";
			else
				return "jcrystal.db.PrimitiveUtils.get"+StringUtils.capitalize(tipo.getSimpleName())+"(rawEntity.get(" + nombrePropiedad + "), " + GlobalTypes.defaultValuesStr.get(tipo) + ")";
		}else if (tipo.isPrimitiveObjectType()){
			if (defaultValue != null)
				return "jcrystal.db.ObjectUtils.get"+StringUtils.capitalize(tipo.getSimpleName())+"(rawEntity.get(" + nombrePropiedad + "), " + defaultValue + ")";
			else
				return "jcrystal.db.ObjectUtils.get"+StringUtils.capitalize(tipo.getSimpleName())+"(rawEntity.get(" + nombrePropiedad + "), null)";
		}else if (tipo.is(Date.class) || tipo.isJAnnotationPresent(CrystalDate.class))
			return "(java.util.Date)rawEntity.get(" + nombrePropiedad + ")";
		else if (tipo.name().startsWith("com.google.appengine.api.datastore."))
			return "(" + tipo.getSimpleName() + ")rawEntity.getProperty(" + nombrePropiedad + ")";
		else if (tipo.is(Password.class))
			return "(com.google.appengine.api.datastore.ShortBlob)rawEntity.getProperty(" + nombrePropiedad + ")";
		else if (tipo.isEnum())
			return tipo.getSimpleName()+".fromId(jcrystal.utils.EntityUtils.getInt(rawEntity, " + nombrePropiedad + ")"+(defaultValue!=null?", "+defaultValue:"")+")";
		else if (tipo.isSubclassOf(List.class)) {
			if (tipo.getInnerTypes().get(0).is(Long.class))
				return "(java.util.List<Long>)rawEntity.getProperty(" + nombrePropiedad + ")";
			else throw new NullPointerException(tipo.name() + ": " + tipo);
		}
		else throw new NullPointerException(tipo.name());
	}
	private class EntityGenerator extends InternalEntityGenerator{
		public EntityGenerator(boolean keysOnly) {
			super(keysOnly);
		}
		private static final long serialVersionUID = 3808140930995364617L;
		@Override
		protected void initialize() {
		}
		@Override
		protected void generateClassHeader() {
			if(entidad.isSecurityToken()){
				$("private boolean needsUpdate = true;");
				$("public void cancelAutomaticUpdate(){needsUpdate = false;}");
				$("public boolean needsUpdate(){return needsUpdate;}");
				$("@Override public void delete(){cancelAutomaticUpdate();Entity.super.delete();}");
			}
			constantes:{
				$("public static final String ENTITY_NAME = \""+entidad.name()+"\";");
			}
		}
		
		@Override
		protected void generateConstructors() {
			//Constructors 1:
			if(entidad.padre == null){
				$("protected final java.util.Map<String, Object> rawEntity;");
				$("public final java.util.Map<String, Object> getRawEntity(){return rawEntity;}");
			}
			$("public " + entidad.getTipo() + "(java.util.Map<String, Object> rawEntity)", ()->{
				if(entidad.padre != null)
					$("super(rawEntity);");
				else
					$("this.rawEntity = rawEntity;");
			});
			
			//Constructors 2:
			for(final boolean pro : new boolean[]{false, true}){
				Stream<JVariable> params = entidad.streamFinalFields().map(f->{
					if(f.keyData == null || f.getTargetEntity() == null)
						return P(f.type(), f.fieldName());
					else if(f.getTargetEntity().key.isSimple())
						return P(f.getTargetEntity().key.getLlaves().get(0).type(), f.fieldName());
					else
						return P(f.getTargetEntity().key.getLlaves().get(0).type(), f.fieldName());
				});
				
				params = Stream.concat(params, entidad.onConstructMethods.stream().flatMap(f->f.params.stream()).map(f->P(f.type, f.name)));
				if(pro) 
					params = Stream.concat(Stream.of(P(GlobalTypes.STRING, "entityName")), params);
					
				$M(pro ? Modifier.PROTECTED : Modifier.PUBLIC, "", entidad.getTipo(), params, ()->{
					if(entidad.padre != null) {
						if(pro) {
							$("super(" + entidad.padre.getFinalFields().addFirst("entityName") + ");");
						}else {
							if(entidad.entidad.useParentName())
								$("super(" + entidad.padre.getFinalFields() + ");");
							else
								$("super(" + entidad.padre.getFinalFields().addFirst("ENTITY_NAME") + ");");
						}
						if(entidad.padre.campoSelector != null && entidad.clase.isAnnotationPresent(Selector.class)){
							$("this." + entidad.padre.campoSelector.fieldName() + "(" + entidad.clase.getAnnotation(Selector.class).valor()+");");
						}
					}else{
						$("rawEntity = new java.util.TreeMap<>();");
						if(entidad.key != null) {
							entidad.key.getLlaves().forEach(k->{
								$("rawEntity.put(\""+k.dbName+"\", "+k.fieldName()+");");
							});
						}
					}
					
					TreeSet<String> procesados = new TreeSet<>();
					entidad.properties.stream().forEach(f->{
						if (!procesados.contains(f.fieldName()) && f.isEntityProperty) {
							procesados.add(f.fieldName());
							if (f.isAutoNow) {
								$(f.fieldName() + "(new java.util.Date());");
							} else if(f.isFinal){
								$("this." + f.fieldName() + "(" + f.fieldName() + ");");
							}
						}else if(f.defaultValue != null){
							$("this." + f.fieldName() + "("+entidad.getTipo()+"." + f.fieldName() + ");");
						}
					});
					entidad.onConstructMethods.forEach(m->{
						$("this."+m.name()+"("+m.params.stream().map(f->f.name).collect(Collectors.joining(", "))+");");
					});
				});
			}
			if(entidad.streamFinalFields().anyMatch(f-> f.keyData != null && f.getTargetEntity() != null)) {
				Stream<JVariable> params = entidad.streamFinalFields().map(f->P(f.type(), f.fieldName()));
				params = Stream.concat(params, entidad.onConstructMethods.stream().flatMap(f->f.params.stream()).map(f->P(f.type, f.name)));
				$M(Modifier.PUBLIC, "", entidad.getTipo(), params, ()->{
					Stream<String> ps = entidad.streamFinalFields().map(f->{
					if(f.keyData == null || f.getTargetEntity() == null)
						return f.fieldName();
					else if(f.getTargetEntity().key.isSimple())
						return f.fieldName()+"."+f.getTargetEntity().key.getLlaves().get(0).fieldName()+"()";
					else
						throw new NullPointerException();
					});
					ps = Stream.concat(ps, entidad.onConstructMethods.stream().flatMap(f->f.params.stream()).map(f->f.name));
					$("this("+ps.collect(Collectors.joining(", "))+");");
				});
			}
			//TODO: Poner en otro lado
			$("public " + entidad.getTipo() + " cloneFrom(" + entidad.getTipo() + " from)",()->{
				$("this.rawEntity.putAll(from.rawEntity);");
				$("return this;");
			});
		}
		@Override
		protected void generateKeyMethods() {
			if(entidad.key == null);
			else if(entidad.key.isSimple()) {
				if(!entidad.key.isAutogeneratedKey()) {
					EntityField f = entidad.key.getLlaves().get(0);
					$("public " + $($convert(f.type())) + " "+f.fieldName()+"()",()->{
						if (f.type().is(String.class))
							$("return (String)rawEntity.get("+f.dbName+");");
						else if (f.type().isPrimitive())
							$("return jcrystal.db.PrimitiveUtils.get"+StringUtils.capitalize(f.type().getSimpleName())+"(rawEntity.get(\"" + f.dbName + "\"), 0);");
						else if (f.type().isPrimitiveObjectType())
							$("return jcrystal.db.ObjectUtils.get"+StringUtils.capitalize(f.type().getSimpleName())+"(rawEntity.get(\"" + f.dbName + "\"), null);");
						else throw new NullPointerException("Unsupported key type " + entidad.name()+":"+f.type());
					});
					if(context.input.SERVER.WEB.isEnableJSF()){
						$("public " + $($convert(f.type())) + " get"+capitalize(f.fieldName())+"()",()->{
							$("return this." + f.fieldName()+"();");
						});
					}
				}
			}else
				throw new InternalException(1, "BigQuery DBs doesnt support compound keys");
		}
		@Override
		protected void generateSaveMethods() {
			$("public " + entidad.getTipo() + " put()", ()->{
				for(EntityClass c = entidad; c != null; c = c.padre)
				for (EntityField f : c.properties) {
					if (f.isAutoNow && f.type().isJAnnotationPresent(CrystalDate.class) && !f.isFinal) {
						$(f.fieldName()+"(new java.util.Date());");
					}
					else if (f.isAutoNow && f.type().is(Date.class)) {
						if (!f.isFinal)
							$(f.fieldName()+"(new java.util.Date());");
					}
				}
				for(JMethod m : entidad.preWriteMethods)
					$("this."+m.name()+"();");
				$("Batch.put(java.util.Arrays.asList(this));");
				if(context.input.SERVER.DEBUG.ENTITY_CHECKS)
					$("DEBUG_CHANGES = false;");
				$("return this;");
			});
		}
		@Override
		protected void generateRetriveMethods() {
		}
		@Override
		protected void generatePropertyGetters() {
			for(final EntityField f : entidad.properties){
				if(f.type().is(Password.class)) {
					if(f.hashSalt == null)
						throw new NullPointerException("Todo tipo Password debe tener HashSalt");
					$("private com.google.appengine.api.datastore.ShortBlob " + f.fieldName() + "()", ()->{
						$("return " + getProperty(f.type(), "\"" + f.dbName + "\"", f.defaultValue)+";");
					});
				}
				else if (f.type().is(Date.class) || f.type().isJAnnotationPresent(CrystalDate.class)){
					$("public java.util.Date " + f.fieldName() + "()", ()->{
						$("return " + getProperty(f.type(), "\"" + f.dbName + "\"", f.defaultValue)+";");
					});
				}else if(f.type().is(ArrayList.class)){
					System.out.println("El uso de ArrayList!!! está @Deprecated. Debe usar lists, pero no olvide hacer la migración." + entidad.name());
					System.err.println("El uso de ArrayList!!! está @Deprecated. Debe usar lists, pero no olvide hacer la migración." + entidad.name());
					$("public ArrayList<" + f.type().getInnerTypes().get(0).name() + "> " + f.fieldName() + "()", ()->{
						$("ArrayList<" + f.type().getInnerTypes().get(0).name() + "> ret = new ArrayList<>();");
						$("for(int indx = 0; ;indx++)",()->{
							$(f.type().getInnerTypes().get(0).name() + " toAdd = " + getProperty(f.type().getInnerTypes().get(0), "\"" + f.dbName + ".\"+indx", f.defaultValue)+";");
							$("if(toAdd == null)break;");
							$("ret.add(toAdd);");
						});
						$("return ret;");
					});
				}else if(f.isArray()){
					if(f.type().firstInnerType().isSubclassOf(MaskedEnum.class)){
						$("public long " + f.fieldName() + "()", ()->{
							$("return (long)rawEntity.get(\"" + f.dbName + "\");");
						});
						$("public " + $(f.type().firstInnerType()) + "[] " + f.fieldName() + "Array()", ()->{
							$("final long mask = jcrystal.utils.EntityUtils.getLong(rawEntity, \"" + f.dbName + "\");");
							$("if(mask > 0)",()->{
								$("return " + f.type().firstInnerType().getSimpleName() + ".getFromMask(mask);");
							});
							$("return new " + f.type().firstInnerType().getSimpleName() + "[0];");
						});
					}else{
						$("public " + f.type().firstInnerType().getSimpleName() + "[] " + f.fieldName() + "()", ()->{
							$("final int size = jcrystal.utils.EntityUtils.getInt(rawEntity, \"" + f.dbName + ".len\", -1);");
							$("if(size > 0)",()->{
								$($(f.type().firstInnerType()) + "[] ret = new " + $(f.type().firstInnerType()) + "[size];");
								$("for(int indx = 0; indx < size;indx++)",()->{
									$("ret[indx] = " + getProperty(f.type().firstInnerType(), "\"" + f.dbName + ".\"+indx", f.defaultValue)+";");
								});
								$("return ret;");
							});
							$("return null;");
						});
					}
				}else if(GlobalTypes.is.geoPoint(f.type())) {
					$("public " + $(f.type()) + " " + f.fieldName() + "()", ()->{
						$("return null;");
					});
				}
				else {
					String returnType = $($convert(f.type()));
					if(f.isText())
						returnType = "String";
					else if(f.type().isSubclassOf(List.class))
						returnType = "java.util.List<"+f.type().getInnerTypes().get(0).getSimpleName()+">";
						
					$("public " + returnType + " " + f.fieldName() + "()", ()->{
						$("return " + getProperty(f.type(), "\"" + f.dbName + "\"", f.defaultValue)+";");
					});
					if(context.input.SERVER.WEB.isEnableJSF()){
						$("public " + $($convert(f.type())) + " get" + capitalize(f.fieldName()) + "()",()->{
							$("return this." + f.fieldName()+"();");
						});
					}
				}
			}
		}
		@Override
		protected void generatePropertySetters() {
			for (EntityField f : entidad.properties) {
				String VISIBILITY = f.isFinal ? "private " : "public ";
				int VISIBILITY_mod = f.isFinal ? Modifier.PRIVATE : Modifier.PUBLIC;
				Runnable doReturn = ()->{
					if(context.input.SERVER.DEBUG.ENTITY_CHECKS)
						$("DEBUG_CHANGES = true;");
					$("return this;");
				};
				if (f.isAutoNow && (f.type().is(Date.class) || f.type().isJAnnotationPresent(CrystalDate.class))) {
					$(VISIBILITY + entidad.getTipo() + " " + f.fieldName() + "(java.util.Date " + f.fieldName() + ")",()->{
						if (f.isFinal)
							$("rawEntity.put(\"" + f.dbName + "\", " + f.fieldName() + ");");
						else
							$("rawEntity.put(\"" + f.dbName + "\", " + f.fieldName() + " = new java.util.Date());");
						doReturn.run();
					});
				} else {
					if(f.type().is(Password.class)) {
						throw new NullPointerException("Should not store passwords on Google BigTable");
					}else if (f.type().is(ArrayList.class)) {
						$(VISIBILITY + entidad.getTipo() + " " + f.fieldName() + "(ArrayList<" + f.type().getInnerTypes().get(0).name() + "> " + f.fieldName() + ")", ()->{
							$("int indx = 0;");
							$("for(" + f.type().getInnerTypes().get(0).name() + " ele : " + f.fieldName() + ")");
							$("\trawEntity.put(\"" + f.dbName + ".\"+indx++, ele);");
							doReturn.run();
						});
					}else if (f.type().is(Date.class) || f.type().isJAnnotationPresent(CrystalDate.class)){
						$(VISIBILITY + entidad.getTipo() + " " + f.fieldName() + "(java.util.Date " + f.fieldName() + ")",()->{
							$("rawEntity.put(\"" + f.dbName + "\", jcrystal.context.GoogleBigQuery.TIMESTAMP_FORMAT.format(" + f.fieldName() + "));");
							doReturn.run();
						});
					}
					else if (f.isArray()) {
						$(VISIBILITY + entidad.getTipo() + " " + f.fieldName() + "(" + f.type().firstInnerType().getSimpleName() + "[] " + f.fieldName() + ")", ()->{
							if(f.type().firstInnerType().isSubclassOf(MaskedEnum.class)){
								$("long mask = 0;");
								$("if("+f.fieldName() + " != null)for("+f.type().firstInnerType().getSimpleName()+" val : " + f.fieldName() + ")",()->{
									$("mask |= val.id();");
								});
								$("rawEntity.put(\"" + f.dbName + "\", mask);");
							}else{
								$if(f.fieldName() + " != null",()->{
									$("rawEntity.setUnindexedProperty(\"" + f.dbName + ".len\", " + f.fieldName() + ".length);");
									$("for(int indx = 0; indx < " + f.fieldName() + ".length; indx++)");
									$("\trawEntity.put(\"" + f.dbName + ".\"+indx, " + f.fieldName() + "[indx]);");
								}).$else(()->{
									$("final int size = jcrystal.utils.EntityUtils.getInt(rawEntity, \"" + f.dbName + ".len\", -1);");
									$("for(int indx = 0; indx < size; indx++)");
									$("rawEntity.put(\"" + f.dbName + ".\"+indx, null);");
									$("rawEntity.put(\"" + f.dbName + ".len\", 0);");
								});
							}
							doReturn.run();
						});
					} else if(f.type().is(JSONObject.class, JSONArray.class)) {
						$M(VISIBILITY_mod, entidad.getTipo(), f.fieldName(), $(P(f.type(), f.fieldName())), ()->{
							$if(f.fieldName()+" == null",()-> $("rawEntity.put(\"" + f.dbName + "\", null);"))
							.$else(()->$("rawEntity.put(\"" + f.dbName + "\", new com.google.appengine.api.datastore.Text(" + f.fieldName() + ".toString(0)));"));
							doReturn.run();
						});
					}else if(GlobalTypes.is.geoPoint(f.type())) {
						$M(VISIBILITY_mod, entidad.getTipo(), f.fieldName(), $(P(f.type(), f.fieldName())), ()->{
							$("this." + f.fieldName() + "(" + f.fieldName() + ".getLatitude(), " + f.fieldName() + ".getLongitude());");
							doReturn.run();
						});
						$M(VISIBILITY_mod, entidad.getTipo(), f.fieldName(), $(P(GlobalTypes.DOUBLE, f.fieldName()+"_lat"), P(GlobalTypes.DOUBLE, f.fieldName()+"_lng")), ()->{
							$("rawEntity.put(\"" + f.dbName + "\", jcrystal.context.GoogleBigQuery.DECIMAL_6.format(" + f.fieldName() + "_lat) + \",\" + jcrystal.context.GoogleBigQuery.DECIMAL_6.format(" + f.fieldName() + "_lng));");
							doReturn.run();
						});
						$M(VISIBILITY_mod, entidad.getTipo(), f.fieldName(), $(P(GlobalTypes.FLOAT, f.fieldName()+"_lat"), P(GlobalTypes.FLOAT, f.fieldName()+"_lng")), ()->{
							$("rawEntity.put(\"" + f.dbName + "\", jcrystal.context.GoogleBigQuery.DECIMAL_6.format(" + f.fieldName() + "_lat) + \",\" + jcrystal.context.GoogleBigQuery.DECIMAL_6.format(" + f.fieldName() + "_lng));");
							doReturn.run();
						});
					}else if(f.isText()) {
						$M(VISIBILITY_mod, entidad.getTipo(), f.fieldName(), $(P(GlobalTypes.STRING, f.fieldName())), ()->{
							$if(f.fieldName()+" == null",()-> $("rawEntity.put(\"" + f.dbName + "\", null);"))
							.$else(()->$("rawEntity.put(\"" + f.dbName + "\", new com.google.appengine.api.datastore.Text(" + f.fieldName() + "));"));
							doReturn.run();
						});
						$M(VISIBILITY_mod, entidad.getTipo(), f.fieldName(), $(P(GlobalTypes.Google.DataStore.Text, f.fieldName())), ()->{
							$if(f.fieldName()+" == null",()-> $("rawEntity.put(\"" + f.dbName + "\", null);"))
							.$else(()->$("rawEntity.put(\"" + f.dbName + "\", " + f.fieldName() + ");"));
							doReturn.run();
						});
					}else {
						$M(VISIBILITY_mod, entidad.getTipo(), f.fieldName(), $(P($convert(f.type()), f.fieldName())), ()->{
							if(f.type().is(char.class))
								$("rawEntity.put(\"" + f.dbName + "\", (int)" + f.fieldName() + ");");
							else if (f.isEnum())
								$if(f.fieldName()+" == null",()-> $("rawEntity.put(\"" + f.dbName + "\", null);"))
								.$else(()->$("rawEntity.put(\"" + f.dbName + "\", " + f.fieldName() + ".id);"));
							else
								$("rawEntity.put(\"" + f.dbName + "\", " + f.fieldName() + ");");
							doReturn.run();
						});
						if(context.input.SERVER.WEB.isEnableJSF()){
							$(VISIBILITY + " void set" + capitalize(f.fieldName()) + "(" + $(f.type()) + " " + f.fieldName() + ")",()->{
								$("this." + f.fieldName()+"(" + f.fieldName() + ");");
							});
						}
					}
				}
			}
		}
		@Override
		protected void generateRelations() {
			Stream.concat(entidad.manyToOneRelations.stream(), entidad.ownedOneToOneRelations.stream()).filter(f->!f.field.isAnnotationPresent(EntityKey.class)).forEach(f->{
				String keyType = f.getTarget().key.isSimple()?f.getTarget().key.getKeyTypes(this):"com.google.appengine.api.datastore.Key";
				String keyName = f.getTarget().key.isSimple()?f.getTarget().key.getKeyValues()+"()":"getKey()";
				$("public "+keyType+" " + f.fieldName() + "$Key()", ()->{
					$("return ("+keyType+")rawEntity.getProperty(\"" + f.dbName  + "\");");
				});
				$("public "+f.getOwner().getTipo()+" " + f.fieldName() + "("+$(f.getTarget().clase)+" val)", ()->{
					$if("val != null",()->{
						$("rawEntity.setIndexedProperty(\""+f.dbName+"\", val."+keyName+");");
					}).$else(()->{
						$("rawEntity.setIndexedProperty(\""+f.dbName+"\", null);");
					});
					$("return this;");
				});
				$("public "+f.getOwner().getTipo()+" " + f.fieldName() + "(" + KeyConverters.keyType(f.getTarget())+")", ()->{
					if(f.getTarget().key.isSimple())
						$("rawEntity.setIndexedProperty(\""+f.dbName+"\", "+f.getTarget().key.getKeyValues()+");");
					else
						$("rawEntity.setIndexedProperty(\""+f.dbName+"\", "+$(f.getTarget().clase)+".createKey("+f.getTarget().key.getKeyValues()+"));");
					$("return this;");
				});
				$("public " + $(f.getTarget().clase) + " " + f.fieldName() + "()", ()->{
					$("return "+$(f.getTarget().clase)+".get(" +f.fieldName() + "$Key());");
				});
				$("public " + $(f.getTarget().clase) + " " + f.fieldName() + "Txn()", ()->{
					$("return "+$(f.getTarget().clase)+".getTxn(" +f.fieldName() + "$Key());");
				});
			});
			for(EntityRelation f : entidad.oneToManyRelations)if(f.targetName != null){
				$("public java.util.List<"+$(f.getOwner().clase)+"> " + f.targetName + "()", ()->{
					if(f.getTarget().key.isSimple())
						$("return "+$(f.getOwner().clase)+".Query."+capitalize(f.fieldName())+".get("+f.getTarget().key.getKeyValues()+"());");
					else
						$("return "+$(f.getOwner().clase)+".Query."+capitalize(f.fieldName())+".get(getKey());");
				});
			}
			for(EntityRelation f : entidad.targetedOneToOneRelations)if(f.targetName != null){
				$("public "+$(f.getOwner().clase)+" " + f.targetName + "()", ()->{
					if(f.getTarget().key.isSimple())
						$("return "+$(f.getOwner().clase)+".Query."+capitalize(f.fieldName())+".get("+f.getTarget().key.getKeyValues()+"());");
					else
						$("return "+$(f.getOwner().clase)+".Query."+capitalize(f.fieldName())+".get(getKey());");
				});
			}for(EntityRelation f : entidad.manyToManyRelations) {
				if(f.smallCardinality) {
					String keyType = f.getTarget().key.isSimple()?f.getTarget().key.getKeyTypes(this):"com.google.appengine.api.datastore.Key";
					String keyName = f.getTarget().key.isSimple()?f.getTarget().key.getKeyValues()+"()":"getKey()";
					if(f.getOwner() == entidad) {
						$("@SuppressWarnings(\"unchecked\")");
						$("public java.util.List<"+keyType+"> " + f.fieldName() + "$Key()", ()->{
							$("return (java.util.List<"+keyType+">)rawEntity.getProperty(\"" + f.dbName  + "\");");
						});
						$("public  "+ f.getOwner().getTipo()+" " + f.fieldName() + "$Key(java.util.List<"+keyType+"> $vals)", ()->{
							$("rawEntity.setIndexedProperty(\""+f.dbName+"\", $vals);");
							$("return this;");
						});
						$("public "+f.getOwner().getTipo()+" " + f.fieldName() + "(java.util.function.Consumer<java.util.List<"+keyType+">> editor)", ()->{
							$("java.util.List<"+keyType+"> $temp = "+f.fieldName()+"$Key();");
							$("if($temp==null)$temp = new java.util.ArrayList<>();");
							$("editor.accept($temp);");
							$("rawEntity.setIndexedProperty(\""+f.dbName+"\", $temp);");
							$("return this;");
						});
						$("public "+f.getOwner().getTipo()+" " + f.fieldName() + "("+$(f.getTarget().clase)+" val)", ()->{
							$("return this."+f.fieldName()+"(list->list.add(val."+keyName+"));");
						});
						$("public "+f.getOwner().getTipo()+" " + f.fieldName() + "(java.util.List<"+$(f.getTarget().clase)+"> $val)", ()->{
							$("rawEntity.setIndexedProperty(\""+f.dbName+"\", $val==null ? null : $val.stream().map($v -> $v."+keyName+").collect(java.util.stream.Collectors.toList()));");
							$("return this;");
						});
						$("public "+f.getOwner().getTipo()+" " + f.fieldName() + "(" + KeyConverters.keyType(f.getTarget()) + ")", ()->{
							if(f.getTarget().key.isSimple())
								$("return this."+f.fieldName()+"(list->list.add("+f.getTarget().key.getKeyValues()+"));");
							else
								$("return this."+f.fieldName()+"(list->list.add("+$(f.getTarget().clase)+".createKey("+f.getTarget().key.getKeyValues()+"));");
						});
						
						$("public java.util.List<" + $(f.getTarget().clase) + "> " + f.fieldName() + "()", ()->{
							$("return "+$(f.getTarget().clase)+".Batch.get(" +f.fieldName() + "$Key());");
						});
						$("public java.util.List<" + $(f.getTarget().clase) + "> " + f.fieldName() + "Txn()", ()->{
							$("return "+$(f.getTarget().clase)+".Batch.getTxn(" +f.fieldName() + "$Key());");
						});
					}else if(f.getTarget() == entidad && f.targetName != null) {
						$("public java.util.List<"+$(f.getOwner().clase)+"> " + f.targetName + "()", ()->{
							if(f.getTarget().key.isSimple())
								$("return "+$(f.getOwner().clase)+".Query."+capitalize(f.fieldName())+".get("+f.getTarget().key.getKeyValues()+"());");
							else
								$("return "+$(f.getOwner().clase)+".Query."+capitalize(f.fieldName())+".get(getKey());");
						});
					}
				}
			}
		}
		@Override
		protected void generateQueries() {
			if(!entidad.isSecurityToken())
				$("public static class Post extends Post"+entidad.clase.getSimpleName()+"{}");
			$("public static class Serializer extends Serializer"+entidad.clase.getSimpleName()+"{}");
			$("public static class Batch extends Batch"+entidad.clase.getSimpleName()+"{}");
			
			//TODO:if(context.data.mapEntidadesSimpleName.containsKey("Meta"))else
				$("public static class Meta extends Meta"+entidad.clase.getSimpleName()+"{}");
			
			if(context.input.SERVER.DEBUG.ENTITY_CHECKS) {
				$("//DEBUG");
				$("public boolean DEBUG_CHANGES = false;");
				$("@Override public boolean DEBUG_CHANGES(){return DEBUG_CHANGES;}");
			}
		}
		@Override
		protected void generateExtras() {
			// TODO Auto-generated method stub
			
		}
		@Override
		protected void generateKeyClass() {
			// TODO Auto-generated method stub
			
		}
	}
}

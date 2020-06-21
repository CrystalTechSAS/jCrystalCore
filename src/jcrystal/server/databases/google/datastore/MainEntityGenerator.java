package jcrystal.server.databases.google.datastore;

import static jcrystal.utils.StringUtils.capitalize;

import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.appengine.api.datastore.GeoPt;
import com.google.appengine.api.datastore.Text;

import jcrystal.annotations.internal.model.db.InternalEntityKey;
import jcrystal.datetime.DateType;
import jcrystal.entity.types.ConstantId;
import jcrystal.entity.types.Email;
import jcrystal.entity.types.LongText;
import jcrystal.entity.types.security.Password;
import jcrystal.main.data.ClientContext;
import jcrystal.model.server.db.EntityField;
import jcrystal.model.server.db.EntityClass;
import jcrystal.reflection.MaskedEnum;
import jcrystal.reflection.annotations.CrystalDate;
import jcrystal.reflection.annotations.EntityKey;
import jcrystal.reflection.annotations.IndexType;
import jcrystal.reflection.annotations.Selector;
import jcrystal.reflection.annotations.jEntity;
import jcrystal.reflection.annotations.security.Base;
import jcrystal.server.Entity;
import jcrystal.server.databases.AbsEntityGenerator;
import jcrystal.server.databases.datastore.FileData;
import jcrystal.types.IJType;
import jcrystal.types.JClass;
import jcrystal.types.JMethod;
import jcrystal.types.JVariable;
import jcrystal.types.utils.GlobalTypes;
import jcrystal.utils.GlobalTypeConverter;
import jcrystal.utils.StreamUtils;
import jcrystal.utils.StringUtils;
import jcrystal.utils.langAndPlats.JavaCode;

public class MainEntityGenerator extends AbsEntityGenerator{
	
	
	protected final KeyConvertersClass KeyConverters;
	
	public MainEntityGenerator(ClientContext context) {
		super(context);
		KeyConverters = new KeyConvertersClass(this);
	}
	@Override
	protected final void generateEntity(boolean keysOnly){
		context.utils.converters.dates.extending(()->{
			if(back.id == null) {
				context.output.addSection(entidad.clase, "GEN", new EntityGenerator(false));
				if(!keysOnly) {
					context.output.exportFile(back, new UtilsEntityGenerator(this).generate(true), entidad.clase.getPackageName().replace(".", "/")+"/"+entidad.getTipo()+".java");
				}
			}else {
				new JavaCode(){{
					$("package "+entidad.clase.getPackageName()+";");
					$import(UtilsEntityGenerator.getImports());
					$("public class " + entidad.clase.getSimpleName() + (keysOnly || entidad.entidad.internal() ? "" : (" implements " + Entity.class.getName()+"."+entidad.mainDBType.getDBName())), ()->{
						$append(new EntityGenerator(keysOnly));
					});
					if(!keysOnly)
						$append(new UtilsEntityGenerator(MainEntityGenerator.this).generate(false));
					context.output.exportFile(back, this, entidad.clase.getPackageName().replace(".", "/")+"/"+entidad.clase.getSimpleName()+".java");
				}};
			}
			if(!keysOnly)
				new QueryClassGenerator(this).generate();
		});
	}
	private class EntityGenerator extends InternalEntityGenerator{
		public EntityGenerator(boolean keysOnly) {
			super(keysOnly);
		}
		@Override
		protected void initialize() {
		}
		@Override
		protected void generateClassHeader() {
			if(entidad.isSecurityToken()){
				$("private boolean needsUpdate = true;");
				$("public void cancelAutomaticUpdate(){needsUpdate = false;}");
				$("public boolean needsUpdate(){return needsUpdate;}");
				if(entidad.padre == null)
					$("@Override public void delete(){cancelAutomaticUpdate();Entity."+entidad.mainDBType.getDBName()+".super.delete();}");
				else
					$("@Override public void delete(){cancelAutomaticUpdate();super.delete();}");
			}
		}
		
		@Override
		protected void generateConstructors() {
			//Constructors 1:
			if(entidad.entidad.internal()) {
				$("protected final com.google.appengine.api.datastore.EmbeddedEntity rawEntity;");
				$("public final com.google.appengine.api.datastore.EmbeddedEntity getRawEntity(){return rawEntity;}");	
			} else if(entidad.padre == null){
				$("protected final com.google.appengine.api.datastore.Entity rawEntity;");
				$("public final com.google.appengine.api.datastore.Entity getRawEntity(){return rawEntity;}");
			}
			$("public " + entidad.getTipo() + "(com.google.appengine.api.datastore." + (entidad.entidad.internal() ? "EmbeddedEntity" : "Entity") + " rawEntity)", ()->{
				if(entidad.padre != null)
					$("super(rawEntity);");
				else
					$("this.rawEntity = rawEntity;");
			});
			
			//Constructors 2:
			for(final boolean pro : new boolean[]{false, true}){
				Stream<JVariable> params = entidad.streamFinalFields().filter(f->!f.isConstant).map(f->{
					if(f.keyData == null || f.getTargetEntity() == null)
						return P($convert(f.type()), f.fieldName());
					else if(f.getTargetEntity().key.isSimple())
						return P(f.getTargetEntity().key.getLlaves().get(0).type(), f.fieldName());
					else
						return P(GlobalTypes.Google.DataStore.KEY, f.fieldName());
				});
				
				params = Stream.concat(params, entidad.onConstructMethods.stream().flatMap(f->f.params.stream()).map(f->P(f.type, f.name)));
				if(pro) 
					params = Stream.concat(Stream.of(P(GlobalTypes.STRING, "entityName")), params);
				
				List<JVariable> ps = params.collect(Collectors.toList());
					
				$M(pro ? Modifier.PROTECTED : Modifier.PUBLIC, "", entidad.getTipo(), ps.stream(), ()->{
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
						String entityName = pro?"entityName":"ENTITY_NAME";
						if(entidad.entidad.internal())
							$("rawEntity = new com.google.appengine.api.datastore.EmbeddedEntity();");
						else if(entidad.key.isAutogeneratedKey() && entidad.key.isSimple())
							$("rawEntity = new com.google.appengine.api.datastore.Entity(" + entityName + ");");
						else if(entidad.key.isAutogeneratedKey())
							$("rawEntity = new com.google.appengine.api.datastore.Entity(" + entityName + ", Key.createRawParentKey("+entidad.key.stream().filter(f->!f.isAutogenerated() && !f.isConstant).map(f->f.name()).collect(Collectors.joining(", "))+"));");
						else
							$("rawEntity = new com.google.appengine.api.datastore.Entity(Key.createRawKey("+entidad.key.stream().filter(f->!f.isAutogenerated() && !f.isConstant).map(f->f.name()).collect(Collectors.joining(", "))+"));");
					}
					
					TreeSet<String> procesados = new TreeSet<>();
					entidad.iterateKeysAndProperties().forEach(f->{
						if (!procesados.contains(f.fieldName())) {
							procesados.add(f.fieldName());
							if(f.keyData != null) {
								if(f.keyData.indexAsProperty())
									$("this." + f.fieldName() + "(" + f.fieldName() + ");");
							}else if (f.isAutoNow) {
								$(f.fieldName() + "(new java.util.Date());");
							} else if(f.isFinal){
								$("this." + f.fieldName() + "(" + f.fieldName() + ");");
							} else if(f.defaultValue != null){
								$("this." + f.fieldName() + "("+entidad.getTipo()+"." + f.fieldName() + ");");
							}
						}
					});
					entidad.onConstructMethods.forEach(m->{
						$("this."+m.name()+"("+m.params.stream().map(f->f.name).collect(Collectors.joining(", "))+");");
					});
				});
			}
			if(entidad.streamFinalFields().anyMatch(f-> f.keyData != null && f.getTargetEntity() != null && isExposed(f.getTargetEntity()))){
				Stream<JVariable> params = entidad.streamFinalFields().filter(f->!f.isConstant).map(f->{
					if(f.keyData != null && f.getTargetEntity() != null && !isExposed(f.getTargetEntity()))
						return P(f.getTargetEntity().key.getLlaves().get(0).type(), f.fieldName());					
					return P($convert(f.type()), f.fieldName());					
				});
				params = Stream.concat(params, entidad.onConstructMethods.stream().flatMap(f->f.params.stream()).map(f->P(f.type, f.name)));
				$M(Modifier.PUBLIC, "", entidad.getTipo(), params, ()->{
					Stream<String> ps = entidad.streamFinalFields().filter(f->!f.isConstant).map(f->{
						if(f.keyData == null || f.getTargetEntity() == null || !isExposed(f.getTargetEntity()))
							return f.fieldName();
						else if(f.getTargetEntity().key.isSimple())
							return f.fieldName()+"."+f.getTargetEntity().key.getLlaves().get(0).fieldName()+"()";
						else
							return f.fieldName()+".getKey()";
					});
					ps = Stream.concat(ps, entidad.onConstructMethods.stream().flatMap(f->f.params.stream()).map(f->f.name));
					$("this("+ps.collect(Collectors.joining(", "))+");");
				});
			}
			if(entidad.streamFinalFields().anyMatch(f-> f.keyData != null && f.getTargetEntity() != null && !f.getTargetEntity().key.isSimple())){
				Stream<JVariable> params = entidad.streamFinalFields().filter(f->!f.isConstant).map(f->{
					if(f.keyData != null && f.getTargetEntity() != null)
						return P(f.getTargetEntity().key.getSingleKeyType(), f.fieldName());					
					return P(f.type(), f.fieldName());					
				});
				params = Stream.concat(params, entidad.onConstructMethods.stream().flatMap(f->f.params.stream()).map(f->P(f.type, f.name)));
				$M(Modifier.PUBLIC, "", entidad.getTipo(), params, ()->{
					Stream<String> ps = entidad.streamFinalFields().filter(f->!f.isConstant).map(f->{
						if(f.keyData == null || f.getTargetEntity() == null || !isExposed(f.getTargetEntity()))
							return f.fieldName();
						else if(f.getTargetEntity().key.isSimple())
							return f.fieldName();
						else
							return f.fieldName()+".getRawKey()";
					});
					ps = Stream.concat(ps, entidad.onConstructMethods.stream().flatMap(f->f.params.stream()).map(f->f.name));
					$("this("+ps.collect(Collectors.joining(", "))+");");
				});
			}
			//TODO: Poner en otro lado
			$("public " + entidad.getTipo() + " cloneFrom(" + entidad.getTipo() + " from)",()->{
				$("this.rawEntity.setPropertiesFrom(from.rawEntity);");
				$("return this;");
			});
		}
		@Override
		protected void generateKeyMethods() {
			if(entidad.key == null)
				return;
			if(entidad.key.getLlaves().size() == 1) {
				EntityField f = entidad.key.getLlaves().get(0);
				$("public " + $($convert(f.type())) + " "+f.fieldName()+"()",()->{
					if (f.type().is(String.class))
						$("return rawEntity.getKey().getName();");
					else if (f.type().is(long.class, Long.class))
						$("return rawEntity.getKey().getId();");
					else throw new NullPointerException(entidad.name());
				});
				if(context.input.SERVER.WEB.isEnableJSF()){
					$("public " + $($convert(f.type())) + " get"+capitalize(f.fieldName())+"()",()->{
						$("return this." + f.fieldName()+"();");
					});
				}
			}else{
				StreamUtils.forEachWithIndex(entidad.key.getLlaves(), (i,key)->{
					if(!key.isEntityProperty && ! key.isConstant){
						if(key.getTargetEntity() != null) {
							String keyType = key.getTargetEntity().key.isSimple()?key.getTargetEntity().key.getKeyTypes(this):"com.google.appengine.api.datastore.Key";
							$("public "+keyType+" " + key.fieldName() + "$RawKey()", ()->{
								if(key.getTargetEntity().key.isSimple())
									$("return Key." + key.fieldName() + "(rawEntity.getKey());");
								else
									$("return Key.raw" + StringUtils.capitalize(key.fieldName()) + "(rawEntity.getKey());");
							});
							if(!key.getTargetEntity().key.isSimple())
								$("public "+$(key.getTargetEntity().key.getSingleKeyType())+" " + key.fieldName() + "$Key()", ()->{
									$(keyType+" rawKey = " + key.fieldName() + "$RawKey();");
									$ifNotNull("rawKey", ()->{
										$("return new "+$(key.getTargetEntity().key.getSingleKeyType())+"(rawKey);");
									});
									$("return null;");
								});
						}else
							$("public final " + $($convert(key.type())) + " " + key.fieldName() + "()", ()->{
								$("return Key." + key.fieldName() + "(rawEntity.getKey());");
							});
					}
				});
			}
		}
		@Override
		protected void generateSaveMethods() {
			if(entidad.entidad.internal())
				return;
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
				$("jcrystal.context.CrystalContext $ctx = jcrystal.context.CrystalContext.get();");
				$("$ctx."+entidad.mainDBType.getDBName()+"().service.put($ctx."+entidad.mainDBType.getDBName()+"().getTxn(), rawEntity);");
				if(context.input.SERVER.DEBUG.ENTITY_CHECKS)
					$("DEBUG_CHANGES = false;");
				$("return this;");
			});
		}
		@Override
		protected void generateRetriveMethods() {
			if(entidad.key == null)
				return;
			//Raw retrievers
			$("public static com.google.appengine.api.datastore.Entity rawGet(com.google.appengine.api.datastore.Key $key)", ()->{
				$if("$key == null", "return null;");
				$("try", ()->{
					$("return jcrystal.context.CrystalContext.get()."+entidad.mainDBType.getDBName()+"().get($key);");
				});
				$("catch(com.google.appengine.api.datastore.EntityNotFoundException | java.lang.IllegalArgumentException e)", ()->{
					$("return null;");
				});
			});
			get:{
				$("public static " + entidad.getTipo() + " get(com.google.appengine.api.datastore.Key $key)", ()->{
					$("com.google.appengine.api.datastore.Entity ent = rawGet($key);");
					$("if(ent == null)return null;");
					$("return new " + entidad.getTipo() + "(ent);");
				});
				$("public static " + entidad.getTipo() + " get(" + KeyConverters.keyType() + ")", ()->{
					$("com.google.appengine.api.datastore.Entity ent = rawGet(Key.createRawKey(" + entidad.key.getKeyValues() + "));");
					$("if(ent == null)return null;");
					$("return new " + entidad.getTipo() + "(ent);");
				});
				if(!entidad.key.isSimple()) {
					if(entidad.key.stream().anyMatch(f->f.getTargetEntity() != null && !f.getTargetEntity().key.isSimple()) && entidad.key.stream().filter(f->!f.isConstant).count() > 1)
						$("public static " + entidad.getTipo() + " get(" + KeyConverters.rawKeyType() + ")", ()->{
							$("return get(Key.createRawKey(" + entidad.key.getKeyValues() + "));");
						});
					$("public static " + entidad.getTipo() + " get(" + entidad.getTipo() + ".Key $key)", ()->{
						$("return $key==null ? null : get($key.getRawKey());");
					});
				}
			}
			$("public static boolean exist(" + KeyConverters.keyType() + ")", ()->{
				$("return rawGet(Key.createRawKey(" + entidad.key.getKeyValues() + ")) != null;");
			});
			tryGet:{
				$("public static " + entidad.getTipo() + " tryGet(com.google.appengine.api.datastore.Key $key)", ()->{
					$("com.google.appengine.api.datastore.Entity ent = rawGet($key);");
					$("if(ent == null)throw new jcrystal.utils.InternalException(17, \"Invalid identifier\");");
					$("return new " + entidad.getTipo() + "(ent);");
				});
				$("public static " + entidad.getTipo() + " tryGet(" + KeyConverters.keyType() + ")", ()->{
					$("return tryGet(Key.createRawKey(" + entidad.key.getKeyValues() + "));");
				});
				$("public static " + entidad.getTipo() + " tryGet(" + KeyConverters.keyType() + ", "+entidad.getTipo()+" $defValue)", ()->{
					$(entidad.getTipo() + " ent = get(" + entidad.key.getKeyValues() + ");");
					$("if(ent == null)return $defValue;");
					$("return ent;");
				});
				if(!entidad.key.isSimple()) {
					if(entidad.key.stream().anyMatch(f->f.getTargetEntity() != null && !f.getTargetEntity().key.isSimple()) && entidad.key.stream().filter(f->!f.isConstant).count() > 1)
						$("public static " + entidad.getTipo() + " tryGet(" + KeyConverters.rawKeyType() + ")", ()->{
							$("return tryGet(Key.createRawKey(" + entidad.key.getKeyValues() + "));");
						});
					$("public static " + entidad.getTipo() + " tryGet(" + entidad.getTipo() + ".Key $key)", ()->{
						$("return tryGet($key.getRawKey());");
					});
				}
			}
		}
		@Override
		protected void generatePropertyGetters() {
			for(final EntityField f : entidad.properties){
				if((f.keyData != null && !f.keyData.indexAsProperty()) || f.isConstant)
					continue;
				else if(f.type().is(Password.class)) {
					if(f.hashSalt == null)
						throw new NullPointerException("Todo tipo Password debe tener HashSalt");
					$("private com.google.appengine.api.datastore.ShortBlob " + f.fieldName() + "()", ()->{
						$("return " + getProperty(f.type(), "\"" + f.dbName + "\"")+";");
					});
				}
				else if (f.type().is(Date.class) || f.type().isJAnnotationPresent(CrystalDate.class)){
					$("public java.util.Date " + f.fieldName() + "()", ()->{
						$("return " + getProperty(f.type(), "\"" + f.dbName + "\"")+";");
					});
				}else if(f.isArray()){
					if(f.type().firstInnerType().isSubclassOf(MaskedEnum.class)){
						$("public long " + f.fieldName() + "()", ()->{
							$("return jcrystal.db.datastore.EntityUtils.getLong(rawEntity, \"" + f.dbName + "\");");
						});
						$("public " + $(f.type().firstInnerType()) + "[] " + f.fieldName() + "Array()", ()->{
							$("final long mask = jcrystal.db.datastore.EntityUtils.getLong(rawEntity, \"" + f.dbName + "\");");
							$("if(mask > 0)",()->{
								$("return " + f.type().firstInnerType().getSimpleName() + ".getFromMask(mask);");
							});
							$("return new " + f.type().firstInnerType().getSimpleName() + "[0];");
						});
					}else{
						$("public " + f.type().firstInnerType().getSimpleName() + "[] " + f.fieldName() + "()", ()->{
							$("final int size = jcrystal.db.datastore.EntityUtils.getInt(rawEntity, \"" + f.dbName + ".len\", -1);");
							$("if(size > 0)",()->{
								$($(f.type().firstInnerType()) + "[] ret = new " + $(f.type().firstInnerType()) + "[size];");
								$("for(int indx = 0; indx < size;indx++)",()->{
									$("ret[indx] = " + getProperty(f.type().firstInnerType(), "\"" + f.dbName + ".\"+indx")+";");
								});
								$("return ret;");
							});
							$("return null;");
						});
					}
				}else if(f.f.isJAnnotationPresent(Email.class))
					$("public " + $($convert(f.type())) + " " + f.fieldName() + "()", ()->{
						$("return jcrystal.db.datastore.EntityUtils.getEmail(rawEntity, \"" + f.dbName + "\");");
					});
				else {
					String returnType = $($convert(f.type()));
					if(f.isText())
						returnType = "String";
					else if(f.type().isSubclassOf(List.class))
						returnType = "java.util.List<"+f.type().getInnerTypes().get(0).getSimpleName()+">";
						
					$("public " + returnType + " " + f.fieldName() + "()", ()->{
						if(f.type().is(Map.class))
							$if("!rawEntity.hasProperty(\""+f.dbName+"\")",()->{
								$("rawEntity.setUnindexedProperty(\""+f.dbName+"\", new com.google.appengine.api.datastore.EmbeddedEntity());");
							});
						if(f.getTargetEntity() != null && f.getTargetEntity().key == null) {
							$if("!rawEntity.hasProperty(\""+f.dbName+"\") || rawEntity.getProperty(\""+f.dbName+"\") == null",()->{
								$("return null;");
							});
							$("return " + getProperty(f.type(), "\"" + f.dbName + "\"")+";");							
						}else
							$("return " + getProperty(f.type(), "\"" + f.dbName + "\"")+";");
					});
					if(context.input.SERVER.WEB.isEnableJSF()){
						$("public " + $($convert(f.type())) + " get" + capitalize(f.fieldName()) + "()",()->{
							$("return this." + f.fieldName()+"();");
						});
					}
				}
				if(f.type().is(FileData.class)) {
					$("public jcrystal.server.FileDownloadDescriptor download" + capitalize(f.fieldName())+"()",()->{
						$if(f.fieldName()+"() == null",()->{
							$("return null;");
						});
						$("return new jcrystal.server.FileDownloadDescriptor(Key.createRawPath(getKey()) + \"/"+f.fieldName()+"\");");
					});
					if(!entidad.key.isSimple())
						$("public static jcrystal.server.FileDownloadDescriptor download" + capitalize(f.fieldName())+"(Key key)",()->{
							$if("key == null",()->{
								$("return null;");
							});
							$("return new jcrystal.server.FileDownloadDescriptor(Key.createRawPath(key.getRawKey()) + \"/"+f.fieldName()+"\");");
						});
				}
			}
			Stream.concat(entidad.manyToOneRelations.stream(), entidad.ownedOneToOneRelations.stream())
				.filter(f->!f.field.isAnnotationPresent(EntityKey.class))
				.filter(f->isExposed(f.getTarget()))
				.forEach(f->{
				String keyType = f.getTarget().key.isSimple()?f.getTarget().key.getKeyTypes(this):"com.google.appengine.api.datastore.Key";
				$("public "+keyType+" " + f.fieldName() + "$RawKey()", ()->{
					$("return ("+keyType+")rawEntity.getProperty(\"" + f.dbName  + "\");");
				});
				if(!f.getTarget().key.isSimple())
					$("public "+$(f.getTarget().key.getSingleKeyType())+" " + f.fieldName() + "$Key()", ()->{
						$(keyType+" rawKey = " + f.fieldName() + "$RawKey();");
						$ifNotNull("rawKey", ()->{
							$("return new "+$(f.getTarget().key.getSingleKeyType())+"(rawKey);");
						});
						$("return null;");
					});
				$("public " + $(f.getTarget().clase) + " " + f.fieldName() + "()", ()->{
					$("return "+$(f.getTarget().clase)+".get(" +f.fieldName() + "$RawKey());");
				});
			});
		}
		@Override
		protected void generatePropertySetters() {
			entidad.iterateKeysAndProperties().filter(f->f.keyData == null || f.keyData.indexAsProperty()).forEach(f->{
				String methodName = f.indexType() != IndexType.NONE ? "setIndexedProperty" : "setUnindexedProperty";
				String VISIBILITY = f.isFinal || f.keyData != null ? "private " : "public ";
				int VISIBILITY_mod = f.isFinal || f.keyData != null ? Modifier.PRIVATE : Modifier.PUBLIC;
				Runnable doReturn = ()->{
					if(context.input.SERVER.DEBUG.ENTITY_CHECKS)
						$("DEBUG_CHANGES = true;");
					$("return this;");
				};
				if(f.f.isJAnnotationPresent(Email.class))
					$M(VISIBILITY_mod, entidad.getTipo(), f.fieldName(), $(P($convert(f.type()), f.fieldName())), ()->{
						$("rawEntity." + methodName + "(\"" + f.dbName + "\", new com.google.appengine.api.datastore.Email(" + f.fieldName() + "));");
						doReturn.run();
					});
				else if(f.type().is(String.class)) {
					$M(VISIBILITY_mod, entidad.getTipo(), f.fieldName(), $(P($convert(f.type()), f.fieldName())), ()->{
						if(f.hashSalt == null)
							$("rawEntity." + methodName + "(\"" + f.dbName + "\", " + f.fieldName() + ");");
						else {
							$ifNull(f.fieldName(), ()->{
								$("rawEntity." + methodName + "(\"" + f.dbName + "\", null);");
							});
							$else(()->{
								$("rawEntity." + methodName + "(\"" + f.dbName + "\", hash"+capitalize(f.fieldName())+"Encoded(" + f.fieldName() + "));");
							});
						}
						doReturn.run();
					});
				}else if (f.isAutoNow && (f.type().is(Date.class) || f.type().isJAnnotationPresent(CrystalDate.class))) {
					$(VISIBILITY + entidad.getTipo() + " " + f.fieldName() + "(java.util.Date " + f.fieldName() + ")",()->{
						if (f.isFinal)
							$("rawEntity." + methodName + "(\"" + f.dbName + "\", " + f.fieldName() + ");");
						else
							$("rawEntity." + methodName + "(\"" + f.dbName + "\", " + f.fieldName() + " = new java.util.Date());");
						doReturn.run();
					});
				} else if(f.type().is(Password.class)) {
					if(f.hashSalt == null)
						throw new NullPointerException("Todo tipo Password debe tener HashSalt");
					$("public "+entidad.getTipo()+" " + f.fieldName() + "(String __password)", ()->{
						$("rawEntity.setUnindexedProperty(\"" + f.dbName + "\", new com.google.appengine.api.datastore.ShortBlob(hash" + capitalize(f.fieldName())+"(__password)));");
						doReturn.run();
					});
				} else if(f.type().is(JSONObject.class, JSONArray.class)) { //Este debe ir antes de isIterable porque el JSONArray es iterable  
					$M(VISIBILITY_mod, entidad.getTipo(), f.fieldName(), $(P(f.type(), f.fieldName())), ()->{
						$if(f.fieldName()+" == null",()-> $("rawEntity." + methodName + "(\"" + f.dbName + "\", null);"))
						.$else(()->$("rawEntity." + methodName + "(\"" + f.dbName + "\", new com.google.appengine.api.datastore.Text(" + f.fieldName() + ".toString(0)));"));
						doReturn.run();
					});
				} else if (f.type().isIterable()) {
					IJType innerType = f.type().getInnerTypes().get(0);
					$(VISIBILITY + entidad.getTipo() + " " + f.fieldName() + "("+$(f.type()) + " " + f.fieldName() + ")", ()->{
						if(innerType.isPrimitiveObjectType() || innerType.is(String.class, GeoPt.class))
							$("rawEntity." + methodName + "(\"" + f.dbName + "\", " + f.fieldName() + ");");
						else if(innerType.isEnum()) {
							$("rawEntity." + methodName + "(\"" + f.dbName + "\", " + innerType.prefixName("Utils")+".toIds("+f.fieldName() + "));");
						}else if(f.getTargetEntity() != null) {
							$if(f.fieldName()+" == null", ()->{
								$("rawEntity." + methodName + "(\"" + f.dbName + "\", null);");
							}).$else(()->{
								$("rawEntity." + methodName + "(\"" + f.dbName + "\", " + f.fieldName() + ".stream().map(f->f.getRawEntity()).collect(java.util.stream.Collectors.toList()));");	
							});
						}
						else throw new NullPointerException("Unsuported List type : " + f.type());
						doReturn.run();
					});
					$(VISIBILITY + entidad.getTipo() + " " + f.fieldName() + "("+$(innerType) + "..." + f.fieldName() + ")", ()->{
						$("return "+f.fieldName()+"(java.util.Arrays.asList("+f.fieldName()+"));");
					});
				}else if(f.type().is(Map.class)) {
					$(VISIBILITY + entidad.getTipo() + " " + f.fieldName() + "("+$(f.type()) + " " + f.fieldName() + ")", ()->{
						$("new jcrystal.db.datastore.EntityMap(rawEntity, \"" + f.dbName + "\", " + f.fieldName() + ");");
						doReturn.run();
					});
				}
				else if (f.type().is(Date.class) || f.type().isJAnnotationPresent(CrystalDate.class)){
					$(VISIBILITY + entidad.getTipo() + " " + f.fieldName() + "(java.util.Date " + f.fieldName() + ")",()->{
						$("rawEntity." + methodName + "(\"" + f.dbName + "\", " + f.fieldName() + ");");
						doReturn.run();
					});
				}else if (f.isArray()) {
					$(VISIBILITY + entidad.getTipo() + " " + f.fieldName() + "(" + f.type().firstInnerType().getSimpleName() + "[] " + f.fieldName() + ")", ()->{
						if(f.type().firstInnerType().isSubclassOf(MaskedEnum.class)){
							$("long mask = 0;");
							$("if("+f.fieldName() + " != null)for("+f.type().firstInnerType().getSimpleName()+" val : " + f.fieldName() + ")",()->{
								$("mask |= val.id();");
							});
							$("rawEntity.setUnindexedProperty(\"" + f.dbName + "\", mask);");
						}else{
							$if(f.fieldName() + " != null",()->{
								$("rawEntity.setUnindexedProperty(\"" + f.dbName + ".len\", " + f.fieldName() + ".length);");
								$("for(int indx = 0; indx < " + f.fieldName() + ".length; indx++)");
								$("\trawEntity.setUnindexedProperty(\"" + f.dbName + ".\"+indx, " + f.fieldName() + "[indx]);");
							}).$else(()->{
								$("final int size = jcrystal.db.datastore.EntityUtils.getInt(rawEntity, \"" + f.dbName + ".len\", -1);");
								$("for(int indx = 0; indx < size; indx++)");
								$("	rawEntity.setUnindexedProperty(\"" + f.dbName + ".\"+indx, null);");
								$("rawEntity.setUnindexedProperty(\"" + f.dbName + ".len\", 0);");
							});
						}
						doReturn.run();
					});
				} else if(f.isText()) {
					$M(VISIBILITY_mod, entidad.getTipo(), f.fieldName(), $(P(GlobalTypes.STRING, f.fieldName())), ()->{
						$if(f.fieldName()+" == null",()-> $("rawEntity." + methodName + "(\"" + f.dbName + "\", null);"))
						.$else(()->$("rawEntity." + methodName + "(\"" + f.dbName + "\", new com.google.appengine.api.datastore.Text(" + f.fieldName() + "));"));
						doReturn.run();
					});
					$M(VISIBILITY_mod, entidad.getTipo(), f.fieldName(), $(P(GlobalTypes.Google.DataStore.Text, f.fieldName())), ()->{
						$if(f.fieldName()+" == null",()-> $("rawEntity." + methodName + "(\"" + f.dbName + "\", null);"))
						.$else(()->$("rawEntity." + methodName + "(\"" + f.dbName + "\", " + f.fieldName() + ");"));
						doReturn.run();
					});
				}else if(f.getTargetEntity() != null) {
					if(f.getTargetEntity().key != null)
						$M(VISIBILITY_mod, entidad.getTipo(), f.fieldName(), $(P(f.getTargetEntity().key.getSingleKeyType(), f.fieldName())), ()->{
							if(f.getTargetEntity().key.isSimple())
								$("rawEntity." + methodName + "(\"" + f.dbName + "\", " + f.fieldName() + ");");
							else
								$if(f.fieldName()+" == null",()-> $("rawEntity." + methodName + "(\"" + f.dbName + "\", null);"))
								.$else(()->$("rawEntity." + methodName + "(\"" + f.dbName + "\", "+f.fieldName()+".getRawKey());"));
							doReturn.run();
						});
					else
						$M(VISIBILITY_mod, entidad.getTipo(), f.fieldName(), $(P($convert(f.type()), f.fieldName())), ()->{
							$if(f.fieldName()+" == null",()-> 
								$("rawEntity." + methodName + "(\"" + f.dbName + "\", null);")
							).$else(
								()->$("rawEntity." + methodName + "(\"" + f.dbName + "\", " + f.fieldName() + ".getRawEntity());")
							);
							doReturn.run();
						});
				}else {
					$M(VISIBILITY_mod, entidad.getTipo(), f.fieldName(), $(P($convert(f.type()), f.fieldName())), ()->{
						if(f.type().is(char.class))
							$("rawEntity." + methodName + "(\"" + f.dbName + "\", (int)" + f.fieldName() + ");");
						else if (f.isEnum()) {
							String idName = f.type().resolve(c->c.enumData.propiedades.get("id")) == null ? ".name()" : ".id";
							$if(f.fieldName()+" == null",()-> 
								$("rawEntity." + methodName + "(\"" + f.dbName + "\", null);")
							).$else(
								()->$("rawEntity." + methodName + "(\"" + f.dbName + "\", " + f.fieldName() + idName + ");")
							);
						}else
							$("rawEntity." + methodName + "(\"" + f.dbName + "\", " + f.fieldName() + ");");
						doReturn.run();
					});
					if(context.input.SERVER.WEB.isEnableJSF()){
						$(VISIBILITY + " void set" + capitalize(f.fieldName()) + "(" + $(f.type()) + " " + f.fieldName() + ")",()->{
							$("this." + f.fieldName()+"(" + f.fieldName() + ");");
						});
					}
				}
				if(f.type().is(FileData.class)) {
					$("public boolean upload" + capitalize(f.fieldName())+"(jcrystal.server.FileUploadDescriptor data)",()->{
						$("return upload" + capitalize(f.fieldName())+"(data, true);");
					});
					$("public boolean upload" + capitalize(f.fieldName())+"(jcrystal.server.FileUploadDescriptor data, boolean put)",()->{
						$ifNull("data", ()->{
							$if("put && "+f.fieldName()+"() != null",()->{
								$(f.fieldName()+"(null);");
								$("if(put)put();");
							});
							$("return true;");
						});
						$if("!getKey().isComplete()",()->{
							$("throw new NullPointerException(\"Incomplete key for entity " + entidad.name() + ", you must put your entity on datastore before uploading files.\");");
						});
						$("try",()->{
							$("data.put(Key.createRawPath(getKey()) + \"/"+f.fieldName()+"\");");
							$(f.fieldName()+"(new "+FileData.class.getName()+"().name(data.getUserFileName()).mimetype(data.getContentType()).length(data.getSize()));");
							$("if(put)put();");
							$("return true;");
						});
						$("catch (java.io.IOException e)", ()->{
							$("return false;");
						});
					});
				}
			});
			Stream.concat(entidad.manyToOneRelations.stream(), entidad.ownedOneToOneRelations.stream())
				.filter(f->!f.field.isAnnotationPresent(EntityKey.class))
				.filter(f->isExposed(f.getTarget()))
				.forEach(f->{
				String keyName = f.getTarget().key.isSimple()?f.getTarget().key.getKeyValues()+"()":"getKey()";
				$("public " + entidad.getTipo() + " " + f.fieldName() + "("+$(f.getTarget().clase)+" val)", ()->{
					$if("val != null",()->{
						$("rawEntity.setIndexedProperty(\""+f.dbName+"\", val."+keyName+");");
					}).$else(()->{
						$("rawEntity.setIndexedProperty(\""+f.dbName+"\", null);");
					});
					$("return this;");
				});
				$("public " + entidad.getTipo() + " " + f.fieldName() + "("+KeyConverters.keyType(f.getTarget())+")", ()->{
					if(f.getTarget().key.isSimple())
						$("rawEntity.setIndexedProperty(\""+f.dbName+"\", "+f.getTarget().key.getKeyValues()+");");
					else
						$("rawEntity.setIndexedProperty(\""+f.dbName+"\", "+$(f.getTarget().clase)+".Key.createRawKey("+f.getTarget().key.getKeyValues()+"));");
					$("return this;");
				});
				if(!f.getTarget().key.isSimple()){
					$("public " + entidad.getTipo() + " " + f.fieldName() + "(" + $(f.getTarget().clase) + ".Key val)", ()->{
						$("rawEntity.setIndexedProperty(\"" + f.dbName + "\", val.getRawKey());");
						$("return this;");
					});
					$("public " + entidad.getTipo() + " " + f.fieldName() + "(com.google.appengine.api.datastore.Key val)", ()->{
						$("rawEntity.setIndexedProperty(\"" + f.dbName + "\", val);");
						$("return this;");
					});
				}
			});
		}
		@Override
		protected void generateRelations() {
			entidad.oneToManyRelations.stream()
				.filter(f->f.targetName != null)
				.filter(f->isExposed(f.getTarget()))
				.forEach(f->{
				$("public java.util.List<"+$(f.getOwner().clase)+"> " + f.targetName + "()", ()->{
					if(f.getTarget().key.isSimple())
						$("return " + f.getOwner().queryClassName() + "."+f.fieldName() + "("+f.getTarget().key.getKeyValues()+"());");
					else
						$("return " + f.getOwner().queryClassName() + "."+f.fieldName() + "(getKey());");
				});
			});
			entidad.targetedOneToOneRelations.stream()
				.filter(f->f.targetName != null)
				.filter(f->isExposed(f.getTarget()))
				.forEach(f->{
					$("public "+$(f.getOwner().clase)+" " + f.targetName + "()", ()->{
					if(f.getTarget().key.isSimple())
						$("return " + f.getOwner().queryClassName() + "." + f.fieldName()+"("+f.getTarget().key.getKeyValues()+"());");
					else
						$("return " + f.getOwner().queryClassName() + "." + f.fieldName()+"(getKey());");
				});
			});
			entidad.manyToManyRelations.stream()
				.filter(f->isExposed(f.getTarget()))
				.forEach(f->{
				if(f.smallCardinality) {
					String keyType = f.getTarget().key.isSimple()?f.getTarget().key.getKeyTypes(this):"com.google.appengine.api.datastore.Key";
					String keyName = f.getTarget().key.isSimple()?f.getTarget().key.getKeyValues()+"()":"getKey()";
					if(f.getOwner() == entidad) {
						$("@SuppressWarnings(\"unchecked\")");
						$("public java.util.List<"+keyType+"> " + f.fieldName() + "$RawKey()", ()->{
							$("return (java.util.List<"+keyType+">)rawEntity.getProperty(\"" + f.dbName  + "\");");
						});
						$("public  "+ f.getOwner().getTipo()+" " + f.fieldName() + "$Key(java.util.List<"+keyType+"> $vals)", ()->{
							$("rawEntity.setIndexedProperty(\""+f.dbName+"\", $vals);");
							$("return this;");
						});
						$("public "+f.getOwner().getTipo()+" " + f.fieldName() + "(java.util.function.Consumer<java.util.List<"+keyType+">> editor)", ()->{
							$("java.util.List<"+keyType+"> $temp = "+f.fieldName()+"$RawKey();");
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
						$("public "+f.getOwner().getTipo()+" " + f.fieldName() + "("+KeyConverters.keyType(f.getTarget())+")", ()->{
							if(f.getTarget().key.isSimple())
								$("return this."+f.fieldName()+"(list->list.add("+f.getTarget().key.getKeyValues()+"));");
							else
								$("return this."+f.fieldName()+"(list->list.add("+$(f.getTarget().clase)+".Key.createRawKey("+f.getTarget().key.getKeyValues()+")));");
						});
						if(f.getTarget().key != null && !f.getTarget().key.isSimple())
							$("public "+f.getOwner().getTipo()+" " + f.fieldName() + "(" + keyType + " val)", ()->{
								$("return this."+f.fieldName()+"(list->list.add(val));");
							});
						$("public java.util.List<" + $(f.getTarget().clase) + "> " + f.fieldName() + "()", ()->{
							$("return "+$(f.getTarget().clase)+".Batch.get(" +f.fieldName() + "$RawKey());");
						});
					}else if(f.getTarget() == entidad && f.targetName != null) {
						$("public java.util.List<"+$(f.getOwner().clase)+"> " + f.targetName + "()", ()->{
							if(f.getTarget().key.isSimple())
								$("return " + f.getOwner().queryClassName() + "." + f.fieldName() + "("+f.getTarget().key.getKeyValues()+"());");
							else
								$("return " + f.getOwner().queryClassName() + "." + f.fieldName() + "(getKey());");
						});
					}
				}
			});
		}
		@Override
		protected void generateQueries() {
			getAll:{
				if(entidad.campoSelector != null){
					$("private static " + entidad.getTipo() + " getBySelector(com.google.appengine.api.datastore.Entity rawEntity)", ()->{
						$($(entidad.campoSelector.type()) + " selector = " + getProperty(entidad.campoSelector.type(), "\"" + entidad.campoSelector.dbName + "\"")+";");
						for(EntityClass h : entidad.hijos)if(h.clase.isAnnotationPresent(Selector.class)){
							$("if(selector == " + h.clase.getAnnotation(Selector.class).valor()+")return new " + h.clase.name+"(rawEntity);");
						}
						$("return new " + entidad.clase.name()+"(rawEntity);");
					});
				}
				/*$("static class " + clase.getTipo() + "Iterable implements Iterable<" + clase.getTipo() + ">", ()->{
					$("private Iterable<com.google.appengine.api.datastore.Entity> iterable;");
					$("public " + clase.getTipo() + "Iterable(Iterable<com.google.appengine.api.datastore.Entity> iterable)",()->{
						$("this.iterable = iterable;");
					});
					$("static class " + clase.getTipo() + "Iterator implements java.util.Iterator<" + clase.getTipo() + ">",()->{
						$("private java.util.Iterator<com.google.appengine.api.datastore.Entity> iterator;");
						$("public " + clase.getTipo() + "Iterator(java.util.Iterator<com.google.appengine.api.datastore.Entity> iterator)",()->{
							$("this.iterator = iterator;");
						});
						$("@Override public boolean hasNext()",()->{
							$("return iterator.hasNext();");
						});
						$("@Override public " + clase.getTipo() + " next()",()->{$("return new " + clase.getTipo() + "(iterator.next());");});
						$("public void remove() {}");
					});
					$("@Override public java.util.Iterator<" + clase.getTipo() + "> iterator()",()->{
						$("return new " + clase.getTipo() + "Iterator(iterable.iterator());");
					});
				});
				$("public static Iterable<" + clase.getTipo() + "> getAll(Iterable<com.google.appengine.api.datastore.Key> llaves)", ()->{
					$("return new " + clase.getTipo() + "Iterable(jcrystal.context.CrystalContext.get()."+entidad.mainDBType.getDBName()+"().service.get(null, llaves).values());");
				});*/
			}
			cachedGetter:if(entidad.key != null){
				if(entidad.key.getLlaves().size() == 1) {
					$("public static class CachedGetter", ()->{
						final String keyTypes = entidad.key.getKeyTypes(this);
						$("private java.util.TreeMap<"+keyTypes+", "+entidad.getTipo()+"> cache = new java.util.TreeMap<>();");
						$("public " + entidad.getTipo() + " get(" + KeyConverters.keyType(entidad)+")", ()->{
							if(keyTypes.equals("com.google.appengine.api.datastore.Key"))
								$("com.google.appengine.api.datastore.Key $key = "+entidad.getTipo()+".Key.createRawKey("+entidad.key.getKeyValues()+");");
							if(keyTypes.equals("com.google.appengine.api.datastore.Key"))
								$(entidad.getTipo()+" ret = cache.get($key);");
							else
								$(entidad.getTipo()+" ret = cache.get("+entidad.key.getKeyValues()+");");
							$("if(ret == null)", ()->{
								if(keyTypes.equals("com.google.appengine.api.datastore.Key"))
									$("cache.put($key, ret = "+entidad.getTipo()+".get("+entidad.key.getKeyValues()+"));");
								else
									$("cache.put("+entidad.key.getKeyValues()+", ret = "+entidad.getTipo()+".get("+entidad.key.getKeyValues()+"));");
							});
							$("return ret;");
						});
					});
				}
			}
			hashes:{
				for(EntityClass c = entidad; c != null; c = c.padre)
					c.fields.stream().filter(f->f.hashSalt != null).forEach(f->{
						String basename = StringUtils.capitalize(f.hashSalt.base().name().split("_")[0].toLowerCase());
						$("public static byte[] hash" + capitalize(f.fieldName()) + "(String value)", ()->{
							$("try",()->{
								switch (f.hashSalt.alg()) {
								case SHA512:
								case SHA256:
								case SHA2:
									$("java.security.MessageDigest md = java.security.MessageDigest.getInstance(\"SHA-"+f.hashSalt.alg().name().replace("SHA","")+"\");");
									break;
								case MD5:
									$("java.security.MessageDigest md = java.security.MessageDigest.getInstance(\"MD5\");");
									break;
								}
								$("return md.digest((value + \"KFsWV4g" + f.hashSalt.value() + "\").getBytes(\"UTF-8\"));");
							});
							$("catch(java.io.UnsupportedEncodingException | java.security.NoSuchAlgorithmException ex)",()->{
								$("return null;");
							});
						});
						$("public boolean validate" + capitalize(f.fieldName()) + "(String password)", ()->{
							if (f.type().is(Password.class))
								$("com.google.appengine.api.datastore.ShortBlob _password = "+f.fieldName()+"();");
							else
								$("String _password = "+f.fieldName()+"();");
							$ifNull("_password", ()->$("return false;"));
							$("byte[] digest = hash" + capitalize(f.fieldName()) + "(password);");
							$("return java.util.Arrays.equals(_password.getBytes(), digest);");
						});
						$("public static String hash" + capitalize(f.fieldName()) + "Encoded(String value)", ()->{
							if(f.hashSalt.base() == Base.BASE64)
								$("return java.util.Base64.getEncoder().encodeToString(hash" + capitalize(f.fieldName()) + "(value));");
							else if(f.hashSalt.base() == Base.BASE64_URLSAFE)
								$("return java.util.Base64.getUrlEncoder().encodeToString(hash" + capitalize(f.fieldName()) + "(value));");
							else
								throw new NullPointerException("Unssuported base " + basename);
						});								
					});
				if(entidad.isSecurityToken()){
					String params = Stream.concat(Stream.of("String value"), entidad.streamFinalFields().skip(1).map(f->$(f.type()) + " " + f.fieldName())).collect(Collectors.joining(", "));
					$("public static "+entidad.getTipo()+" generateTokenBase64("+params+")", ()->{
						$("try",()->{
							$("java.security.MessageDigest md = java.security.MessageDigest.getInstance(\"SHA-256\");");
							Random r = new Random();
							$("byte[] hash = md.digest((value + \"KFsWV4g" + Long.toString(r.nextLong(), 32) + Long.toString(r.nextLong(), 32) + Long.toString(r.nextLong(), 32) + "\" + System.currentTimeMillis()).getBytes(\"UTF-8\"));");
							String values = Stream.concat(Stream.of("java.util.Base64.getEncoder().encodeToString(hash)"), entidad.streamFinalFields().skip(1).map(f->f.fieldName())).collect(Collectors.joining(", "));
							$("return new "+entidad.getTipo()+"("+values+");");
						});
						$("catch(java.io.UnsupportedEncodingException | java.security.NoSuchAlgorithmException ex)",()->{
							$("throw new jcrystal.http.responses.HttpInternalServerError500(\"Internal server error generating authentication token\");");
						});
					});
				}
			}
		}
		
		@Override public void generateExtras() {
			if(entidad.padre != null || !entidad.hijos.isEmpty()) {
				$("public static class Q{");
				incLevel();
			}
			if(!entidad.isSecurityToken())
				$("public static class Post extends Post"+entidad.clase.getSimpleName()+"{}");
			$("public static class Serializer extends Serializer"+entidad.clase.getSimpleName()+"{}");			
			if(!entidad.entidad.internal()) {
				$("public static Query"+entidad.clase.getSimpleName()+" Query = new Query"+entidad.clase.getSimpleName()+"();");
				$("public static Query"+entidad.clase.getSimpleName()+" Query(com.google.appengine.api.datastore.Key ancestor){ return new Query"+entidad.clase.getSimpleName()+"(ancestor); }");
				entidad.iterateAncestorKeys(keys->{
					$("public static Query"+entidad.clase.getSimpleName()+" Query(" + KeyConverters.keyType(keys) + "){ return new Query"+entidad.clase.getSimpleName()+"(" + KeyConverters.keyNames(keys) + "); }");
				});
				$("public static class Batch extends Batch"+entidad.clase.getSimpleName()+"{}");
			}
			$("public static class " + context.data.entidades.MetaClassName + " extends Meta"+entidad.clase.getSimpleName()+"{}");
			$("public static java.util.List<" + entidad.getTipo()+"> convertRawList(java.util.List<com.google.appengine.api.datastore."+(entidad.entidad.internal()?"EmbeddedEntity":"Entity")+"> rawData)", ()->{
				$if("rawData == null", "return null;");
				$else(()->{
					$("java.util.ArrayList<"+entidad.getTipo()+"> ret = new java.util.ArrayList<>();");
					$("for(com.google.appengine.api.datastore."+(entidad.entidad.internal()?"EmbeddedEntity":"Entity")+" data : rawData)",()->{
						$("ret.add(new "+entidad.getTipo()+"(data));");
					});
					$("return ret;");
				});
			});
			if(entidad.padre != null || !entidad.hijos.isEmpty()) {
				$("}");
				decLevel();
			}
			if(context.input.SERVER.DEBUG.ENTITY_CHECKS) {
				$("//DEBUG");
				$("public boolean DEBUG_CHANGES = false;");
				$("@Override public boolean DEBUG_CHANGES(){return DEBUG_CHANGES;}");
			}
		}
		
		private String getProperty(IJType tipo, String nombrePropiedad){
			tipo = GlobalTypeConverter.INSTANCE.convert(tipo);
			if (tipo.is(String.class, Text.class, LongText.class)) 
				return "jcrystal.db.datastore.EntityUtils.getString(rawEntity, " + nombrePropiedad + ")";
			else if (tipo.is(JSONObject.class))
				return "jcrystal.db.datastore.EntityUtils.getJsonObject(rawEntity, " + nombrePropiedad + ")";
			else if (tipo.is(JSONArray.class))
				return "jcrystal.db.datastore.EntityUtils.getJsonArray(rawEntity, " + nombrePropiedad + ")";
			else if (tipo.is(int.class, double.class, boolean.class, long.class)) {
				String name = StringUtils.capitalize(tipo.getSimpleName());
				return "jcrystal.db.datastore.EntityUtils.get"+name+"(rawEntity, " + nombrePropiedad + ", " + GlobalTypes.defaultValues.get(tipo) + ")";
			}else if (tipo.is(char.class)){
				return "(char)jcrystal.db.datastore.EntityUtils.getInt(rawEntity, " + nombrePropiedad + ")";
			}
			else if (tipo.is(Integer.class))
				return "jcrystal.db.datastore.EntityUtils.getInteger(rawEntity, " + nombrePropiedad + ")";
			else if (tipo.is(Long.class))
				return "jcrystal.db.datastore.EntityUtils.getLongObj(rawEntity, " + nombrePropiedad + ")";
			else if (tipo.is(Date.class) || tipo.isJAnnotationPresent(CrystalDate.class))
				return "(java.util.Date)rawEntity.getProperty(" + nombrePropiedad + ")";
			else if (tipo.name().startsWith("com.google.appengine.api.datastore."))
				return "(" + tipo.getSimpleName() + ")rawEntity.getProperty(" + nombrePropiedad + ")";
			else if (tipo.is(Password.class))
				return "(com.google.appengine.api.datastore.ShortBlob)rawEntity.getProperty(" + nombrePropiedad + ")";
			else if (tipo.isEnum()) {
				if(!(tipo instanceof JClass)) 
					return  tipo.name() + ".valueOf(jcrystal.db.datastore.EntityUtils.getString(rawEntity, " + nombrePropiedad + "))";
				else if(tipo.resolve(c->c.enumData.propiedades.get("id")) == null)
					return tipo.prefixName("Utils") + ".fromId(jcrystal.db.datastore.EntityUtils.getString(rawEntity, " + nombrePropiedad + "))";
				else
					return tipo.prefixName("Utils") + ".fromId(jcrystal.db.datastore.EntityUtils.getInt(rawEntity, " + nombrePropiedad + "))";
			}else if (tipo.is(Map.class)) {
				IJType keyType = tipo.getInnerTypes().get(0);
				IJType valueType = tipo.getInnerTypes().get(1);
				return "new jcrystal.db.datastore.EntityMap<"+valueType.name()+">(rawEntity, " + nombrePropiedad + ")";
			}else if(tipo.isAnnotationPresent(jEntity.class) && tipo.getAnnotation(jEntity.class).internal()) {
				EntityClass targetEntity = context.data.entidades.get(tipo);
				if(tipo.is(FileData.class) || targetEntity != null) {
					return "new "+tipo.name() + "((com.google.appengine.api.datastore.EmbeddedEntity)rawEntity.getProperty(" + nombrePropiedad + "))";
				}
				else throw new NullPointerException(tipo.name() + ": " + tipo);
			}
			else if (tipo.isIterable()) {
				if (tipo.firstInnerType().isPrimitiveObjectType() || tipo.firstInnerType().is(GeoPt.class))
					return "(java.util.List<"+tipo.getInnerTypes().get(0).getSimpleName()+">)rawEntity.getProperty(" + nombrePropiedad + ")";
				else if (tipo.getInnerTypes().get(0).isEnum()) {
					IJType typeId = ((JClass)tipo.getInnerTypes().get(0)).enumData.propiedades.get("id");
					if(typeId == null || typeId.is(String.class))
						return tipo.getInnerTypes().get(0).prefixName("Utils") + ".fromString((java.util.List<String>)rawEntity.getProperty(" + nombrePropiedad + "))";
					else
						throw new NullPointerException(tipo.name() + ": " + tipo);
				}
				else{
					EntityClass targetEntity = context.data.entidades.get(tipo.getInnerTypes().get(0));
					if(targetEntity != null) {
						return targetEntity.getTipo() + ".convertRawList((java.util.List<com.google.appengine.api.datastore.EmbeddedEntity>)rawEntity.getProperty(" + nombrePropiedad + "))";
					}
					else throw new NullPointerException(tipo.name() + ": " + tipo);
				}
			}
			else throw new NullPointerException(tipo.name());
		}
		@Override
		protected void generateKeyClass() {
			$("public static final String ENTITY_NAME = \""+entidad.name()+"\";");
			if(!entidad.entidad.internal()) {
				$("public static class Key",()->{
					if(!entidad.key.isSimple()) {
						entidad.key.getLlaves().stream().filter(f->!f.isConstant).forEach(f->{
							if(f.keyData == null || f.getTargetEntity() == null)
								$("public " + $($convert(f.type())) + " " + f.fieldName() + ";");
							else
								$("public " + $($convert(f.getTargetEntity().key.getSingleKeyType())) + " " + f.fieldName() + ";");
						});
						$("public Key(" + KeyConverters.keyType() + ")",()->{
							entidad.key.getLlaves().stream().filter(f->!f.isConstant).forEach(f->{
								$("this." + f.fieldName() + " = "+f.fieldName()+";");
							});
						});
						$("public Key(com.google.appengine.api.datastore.Key rawKey)",()->{
							entidad.key.getLlaves().stream().filter(f->!f.isConstant).forEach(f->{
								$("this." + f.fieldName() + " = " + f.fieldName()+"(rawKey);");
							});
						});
						$("public com.google.appengine.api.datastore.Key getRawKey()",()->{
							$("return "+entidad.getTipo()+".Key.createRawKey(" + entidad.key.getKeyValues() + ");");
						});
						$("public static com.google.appengine.api.datastore.Key cloneKey(com.google.appengine.api.datastore.Key rawKey)",()->{
							$("return new Key(rawKey).getRawKey();");
						});
					}else {
						$("private Key(){}");
						$("public static com.google.appengine.api.datastore.Key cloneKey(com.google.appengine.api.datastore.Key rawKey)",()->{
							if(entidad.key.getSingleKeyType().is(String.class))
								$("return "+entidad.getTipo()+".Key.createRawKey(rawKey.getName());");
							else
								$("return "+entidad.getTipo()+".Key.createRawKey(rawKey.getId());");
						});
					}
					for(String entityName : new String[] {"entityName", "ENTITY_NAME"}) {
						String param = entityName.equals("entityName") ? "String entityName, " : "";
						String value = entityName.equals("entityName") ? "entityName, " : "";
						if(!entidad.key.isSimple()) {
							if(entidad.key.stream().anyMatch(f->f.getTargetEntity() != null && isExposed(f.getTargetEntity())))
								$("public static com.google.appengine.api.datastore.Key createRawKey(" + param + KeyConverters.entityType() + ")", ()->{
									$("return createRawKey(" + value + entidad.key.stream().filter(f->!f.isConstant).map(f->{
										if(f.getTargetEntity() != null) {
											if(f.getTargetEntity().key.isSimple())
												return f.fieldName() + "." + f.getTargetEntity().key.getLast().fieldName()+"()";
											else
												return f.fieldName()+".getRawEntity().getKey()";
										}
										return f.fieldName();
									}).collect(Collectors.joining(", "))+");");
								});
							if(entidad.key.stream().anyMatch(f->f.getTargetEntity() != null && !f.getTargetEntity().key.isSimple()))
								$("public static com.google.appengine.api.datastore.Key createRawKey(" + param + KeyConverters.keyType() + ")", ()->{
									$("return createRawKey(" + value + entidad.key.stream().filter(f->!f.isConstant).map(f->{
										if(f.getTargetEntity() != null) {
											if(f.getTargetEntity().key.isSimple())
												return f.fieldName();
											else
												return f.fieldName()+".getRawKey()";
										}
										return f.fieldName();
									}).collect(Collectors.joining(", "))+");");
								});
						}
						$("public static com.google.appengine.api.datastore.Key createRawKey(" + param + KeyConverters.rawKeyType() + ")", ()->{
							String cond = entidad.key.getLlaves().stream().filter(f->!f.isConstant).filter(f->{
								if(f.keyData == null || f.getTargetEntity() == null)
									return $convert(f.type()).nullable();
								else
									return true;
							}).map(f->f.fieldName()+" == null").collect(Collectors.joining(" || "));
							if(!cond.isEmpty())
								$if(cond,()->{
									$("return null;");
								});
							$("return "+KeyConverters.getRawKeyExpresion(entityName)+";");
						});
					}
					entidad.iterateAncestorKeys(keys->{
						$("public static com.google.appengine.api.datastore.Key createRawParentKey(" + KeyConverters.keyType(keys) + ")",()->{
							String args = keys.stream().filter(f->!f.isConstant).map(f->{
								if(f.getTargetEntity() != null) {
									if(!f.getTargetEntity().key.isSimple())
										return f.fieldName()+".getRawKey()";
									else
										return $(f.getTargetEntity().clase)+".Key.createRawKey("+f.fieldName()+")";
								}
								return f.fieldName();
							}).collect(Collectors.joining(", "));
							if(args.equals("solicitud.getRawKey(), foodtrack.entidades.Usuario.Key.createRawKey(domiciliario)"))
								$("return null;");
							else if(keys.size() == 1)
								$("return " + args +";");
							else
								$("return createRawParentKey(" + args +");");
						});
					});
					for(int e = 1; e < entidad.key.size(); e++) {
						final int keySize = e;
						if(entidad.key.stream().limit(e).anyMatch(f->f.getTargetEntity() != null && !f.getTargetEntity().key.isSimple()))
							$("public static com.google.appengine.api.datastore.Key createRawParentKey(" + KeyConverters.rawKeyType(keySize) + ")", ()->{
								$("return "+KeyConverters.getRawKeyExpresion("ENTITY_NAME", keySize)+";");
							});
					}
					
					StreamUtils.forEachWithIndex(entidad.key.getLlaves(), (i,key)->{
							BiConsumer<IJType,String> simpleKeyGetter = (type,name)->{
								if(!entidad.key.isSimple() && type != GlobalTypes.Google.DataStore.KEY)
									$("public final " + $($convert(type)) + " " + name + "()", ()->{
										$("return " + name+";");
									});
								$("public static final " + $($convert(type)) + " " + name + "(com.google.appengine.api.datastore.Key rawKey)", ()->{
									String keyExp = "rawKey" + IntStream.range(0, entidad.key.getLlaves().size() - i - 1).mapToObj(f->".getParent()").collect(Collectors.joining());
									if (type.is(String.class))
										$("return "+keyExp+".getName();");
									else if (type.is(long.class,Long.class))
										$("return " + keyExp + ".getId();");
									else if (type.isJAnnotationPresent(CrystalDate.class)) {
										$("try", ()->{
											$("return "+type.getSimpleName() + ".SDF.parse(rawKey.getName().split(\"_\")[" + i + "]);");
										});
										$("catch(ParseException e)", ()->{});
										$("return null;");
									} else if (type.is(Date.class)) {
										DateType tipo = key.f.isJAnnotationPresent(CrystalDate.class)?key.f.getJAnnotation(CrystalDate.class).value():DateType.DATE_MILIS;
										$("try", ()->{
											$("return DateType." + tipo + ".FORMAT.parse(rawEntity.getKey().getName().split(\"_\")[" + i + "]);");
										});
										$("catch(ParseException e)", ()->{});
										$("return null;");
									} else if(type.isJAnnotationPresent(InternalEntityKey.class))
										$("return new " + type.name()+"("+keyExp+");");
									else
										$("return " + keyExp+";");
								});
							};
							if(key.getTargetEntity() != null) {
								simpleKeyGetter.accept(key.getTargetEntity().key.getSingleKeyType(), key.fieldName());
								simpleKeyGetter.accept(GlobalTypes.Google.DataStore.KEY, "raw" + StringUtils.capitalize(key.fieldName()));
							}else if(!key.isConstant)
								simpleKeyGetter.accept(key.type(), key.fieldName());
					});
					$("public static String createRawPath(com.google.appengine.api.datastore.Key rawKey)",()->{
						String ret = "";
						for(EntityField key : entidad.key.getLlaves()){
							IJType type = key.getTargetEntity() != null ? key.getTargetEntity().key.getSingleKeyType() : key.type();
							if(key == entidad.key.getLast())
								ret += " + \"/\" + "+$($convert(entidad.clase)) + ".ENTITY_NAME";
							else if(key.getTargetEntity() == null)
								ret += " + \"/" + key.fieldName() + "\"";

							if (type.is(String.class,long.class,Long.class))
								ret += " + \"/\" + " + key.fieldName()+"(rawKey)";
							else if (type.is(ConstantId.class))
								ret += " + \"/" + key.fieldName() + "\"";
							else if (type.isJAnnotationPresent(CrystalDate.class))
								ret += " + \"/\" + " + type.getSimpleName() + ".format("+key.fieldName()+"(rawKey))";
							else if (type.is(Date.class)) {
								DateType tipo = key.f.isJAnnotationPresent(CrystalDate.class)?key.f.getJAnnotation(CrystalDate.class).value():DateType.DATE_MILIS;
								ret += " + \"/\" + " + tipo + ".FORMAT.format("+key.fieldName()+"(rawKey))";
							} 
							else if(key.getTargetEntity().key.isSimple())
								ret += " + \"/\" + " + key.fieldName()+"(rawKey)";
							else
								ret += " + \"/\" + " + $($convert(key.getTargetEntity().clase))+".Key.createRawPath(raw" + StringUtils.capitalize(key.fieldName())+"(rawKey))";
						};
						
						$("return " + ret.substring(3).replaceFirst("/", "").replaceFirst("\"\" \\+ ", "")+";");
					});
				});
			}
		}
	}
	
	
}

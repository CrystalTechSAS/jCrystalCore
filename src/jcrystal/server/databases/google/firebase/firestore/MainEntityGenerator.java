package jcrystal.server.databases.google.firebase.firestore;

import static jcrystal.utils.StringUtils.capitalize;

import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.appengine.api.datastore.Text;

import jcrystal.datetime.DateType;
import jcrystal.entity.types.LongText;
import jcrystal.entity.types.security.Password;
import jcrystal.main.data.ClientContext;
import jcrystal.model.server.db.EntityField;
import jcrystal.model.server.db.EntityClass;
import jcrystal.reflection.MaskedEnum;
import jcrystal.reflection.annotations.CrystalDate;
import jcrystal.reflection.annotations.EntityKey;
import jcrystal.reflection.annotations.Selector;
import jcrystal.server.Entity;
import jcrystal.server.databases.AbsEntityGenerator;
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
					$("public class " + entidad.clase.getSimpleName() + (keysOnly || entidad.entidad.internal() ? "" : (" implements " + Entity.class.getName()+"."+entidad.mainDBType.getDBName())), ()->{
						$append(new EntityGenerator(keysOnly));
					});
					$append(new UtilsEntityGenerator(MainEntityGenerator.this).generate(false));
					context.output.exportFile(back, this, entidad.clase.getPackageName().replace(".", "/")+"/"+entidad.clase.getSimpleName()+".java");
				}};
			}
			new QueryClassGenerator(this).generate();
			QueryClassGenerator.generateUtils(this);
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
				$("@Override public void delete(){cancelAutomaticUpdate();Entity."+entidad.mainDBType.getDBName()+".super.delete();}");
			}
		}
		
		@Override
		protected void generateConstructors() {
			//Constructors 1:
			if(!entidad.entidad.internal()) {
				$("protected String[] rawKey;");
				$("public final String[] getRawKey(){return rawKey;}");
				$("public final String getRawKeyPath(){return Key.keyToPath(rawKey, 0);}");
			}
			if(entidad.padre == null) {
				$("protected final java.util.Map<String, Object> rawEntity;");
				$("public final java.util.Map<String, Object> getRawEntity(){return rawEntity;}");
			}
			$("public " + entidad.getTipo() + "(com.google.cloud.firestore.DocumentSnapshot rawEntity)", ()->{
				if(entidad.padre != null)
					$("super(rawEntity);");
				else {
					$("this.rawEntity = rawEntity.getData();");
					$("this.rawKey = Key.pathToKey(rawEntity.getReference().getPath());");
				}
			});
			
			Constructors2:{
				Stream<JVariable> params = entidad.streamFinalFields().map(f->{
					if(f.keyData == null || f.getTargetEntity() == null)
						return P(f.type(), f.fieldName());
					else if(f.getTargetEntity().key.isSimple())
						return P(f.getTargetEntity().key.getLlaves().get(0).type(), f.fieldName());
					else
						return P(GlobalTypes.ARRAY.STRING, f.fieldName());
				});
				
				params = Stream.concat(params, entidad.onConstructMethods.stream().flatMap(f->f.params.stream()).map(f->P(f.type, f.name)));
				
				List<JVariable> ps = params.collect(Collectors.toList());
					
				$M(Modifier.PUBLIC, "", entidad.getTipo(), ps.stream(), ()->{
					if(entidad.padre != null) {
						$("super(" + entidad.padre.getFinalFields() + ");");
						if(entidad.entidad.useParentName())
							$("this.rawKey[Key.Size - 2] = ENTITY_NAME;");
						if(entidad.padre.campoSelector != null && entidad.clase.isAnnotationPresent(Selector.class)){
							$("this." + entidad.padre.campoSelector.fieldName() + "(" + entidad.clase.getAnnotation(Selector.class).valor()+");");
						}
					}else{
						if(!entidad.key.isAutogeneratedKey())
							$("rawKey = Key.createRawKey(" + ps.stream().map(f->f.name()).collect(Collectors.joining(", "))+");");
						else if(ps.isEmpty())
							$("rawKey = Key.createRawKey(null);");
						else
							$("rawKey = Key.createRawKey(" + ps.stream().map(f->f.name()).collect(Collectors.joining(", "))+", null);");
						$("rawEntity = new java.util.TreeMap<>();");
					}
					
					TreeSet<String> procesados = new TreeSet<>();
					entidad.properties.stream().forEach(f->{
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
				Stream<JVariable> params = entidad.streamFinalFields().map(f->{
					if(f.keyData != null && f.getTargetEntity() != null && !isExposed(f.getTargetEntity()))
						return P(f.getTargetEntity().key.getLlaves().get(0).type(), f.fieldName());					
					return P(f.type(), f.fieldName());					
				});
				params = Stream.concat(params, entidad.onConstructMethods.stream().flatMap(f->f.params.stream()).map(f->P(f.type, f.name)));
				$M(Modifier.PUBLIC, "", entidad.getTipo(), params, ()->{
					Stream<String> ps = entidad.streamFinalFields().map(f->{
						if(f.keyData == null || f.getTargetEntity() == null || !isExposed(f.getTargetEntity()))
							return f.fieldName();
						else if(f.getTargetEntity().key.isSimple())
							return f.fieldName()+"."+f.getTargetEntity().key.getLlaves().get(0).fieldName()+"()";
						else
							return f.fieldName()+".getRawKey()";
					});
					ps = Stream.concat(ps, entidad.onConstructMethods.stream().flatMap(f->f.params.stream()).map(f->f.name));
					$("this("+ps.collect(Collectors.joining(", "))+");");
				});
			}
			if(entidad.streamFinalFields().anyMatch(f-> f.keyData != null && f.getTargetEntity() != null && !f.getTargetEntity().key.isSimple())){
				Stream<JVariable> params = entidad.streamFinalFields().map(f->{
					if(f.keyData != null && f.getTargetEntity() != null)
						return P(f.getTargetEntity().key.getSingleKeyType(), f.fieldName());					
					return P(f.type(), f.fieldName());					
				});
				params = Stream.concat(params, entidad.onConstructMethods.stream().flatMap(f->f.params.stream()).map(f->P(f.type, f.name)));
				$M(Modifier.PUBLIC, "", entidad.getTipo(), params, ()->{
					Stream<String> ps = entidad.streamFinalFields().map(f->{
						if(f.keyData == null || f.getTargetEntity() == null || !isExposed(f.getTargetEntity()))
							return f.fieldName();
						else if(f.getTargetEntity().key.isSimple())
							return f.fieldName()+"."+f.getTargetEntity().key.getLlaves().get(0).fieldName()+"()";
						else
							return f.fieldName()+".getRawKey()";
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
			if(entidad.key == null)
				return;
			if(entidad.key.getLlaves().size() == 1) {
				EntityField f = entidad.key.getLlaves().get(0);
				$("public " + $($convert(f.type())) + " "+f.fieldName()+"()",()->{
					$("return Key." + f.fieldName() + "(rawKey, 0);");
				});
				if(context.input.SERVER.WEB.isEnableJSF()){
					$("public " + $($convert(f.type())) + " get"+capitalize(f.fieldName())+"()",()->{
						$("return this." + f.fieldName()+"();");
					});
				}
			}else{
				StreamUtils.forEachWithIndex(entidad.key.getLlaves(), (i,key)->{
					if(!key.isEntityProperty){
						if(key.getTargetEntity() != null) {
							$("public final " + $($convert(key.getTargetEntity().key.getSingleKeyType())) + " " + key.fieldName() + "$Key()", ()->{
								$("return Key." + key.fieldName() + "(rawKey, 0);");
							});
						}else
							$("public final " + $($convert(key.type())) + " " + key.fieldName() + "()", ()->{
								$("return Key." + key.fieldName() + "(rawKey, 0);");
							});
					}
				});
			}
		}
		@Override
		protected void generateSaveMethods() {
			if(entidad.entidad.internal())
				return;
			$("public " + entidad.getTipo() + " put()throws InterruptedException, java.util.concurrent.ExecutionException", ()->{
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
				$if("!Key.isCompleteKey(rawKey)",()->{
					$("Key.completeKey(rawKey, $ctx."+entidad.mainDBType.getDBName()+"().create($ctx."+entidad.mainDBType.getDBName()+"().service.collection(getRawKeyPath()), rawEntity).get().getPath());");
				}).$else(()->{
					$("$ctx."+entidad.mainDBType.getDBName()+"().create($ctx."+entidad.mainDBType.getDBName()+"().service.document(getRawKeyPath()), rawEntity).get();");
				});
				if(context.input.SERVER.DEBUG.ENTITY_CHECKS)
					$("DEBUG_CHANGES = false;");
				$("return this;");
			});
			$("public " + entidad.getTipo() + " update()throws InterruptedException, java.util.concurrent.ExecutionException", ()->{
				for(JMethod m : entidad.preWriteMethods)
					$("this."+m.name()+"();");
				$("jcrystal.context.CrystalContext $ctx = jcrystal.context.CrystalContext.get();");
				$if("!Key.isCompleteKey(rawKey)",()->{
					$("throw new jcrystal.errors.ErrorException(\"Can't update an entity that has not been saved (call put before update)\");");
				}).$else(()->{
					$("$ctx."+entidad.mainDBType.getDBName()+"().update($ctx."+entidad.mainDBType.getDBName()+"().service.document(getRawKeyPath()), rawEntity).get();");
				});
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
			$("public static com.google.api.core.ApiFuture<com.google.cloud.firestore.DocumentSnapshot> rawGetAsync(String[] $key)", ()->{
				$if("$key == null", "return com.google.api.core.ApiFutures.immediateFuture(null);");
				$("return jcrystal.context.CrystalContext.get()."+entidad.mainDBType.getDBName()+"().service.document(Key.keyToPath($key, 0)).get();");
			});
			$("public static com.google.cloud.firestore.DocumentSnapshot rawGet(String[] $key)throws InterruptedException, java.util.concurrent.ExecutionException", ()->{
				$if("$key == null", "return null;");
				$("return rawGetAsync($key).get();");
			});
			get:{
				$("public static com.google.api.core.ApiFuture<" + entidad.getTipo() + "> getFromKeyAsync(String[] $key)", ()->{
					$("return new jcrystal.db.firestore.ApiFutureTransform<com.google.cloud.firestore.DocumentSnapshot, "+entidad.getTipo()+">(rawGetAsync($key), data->",()->{
						$("if(data == null)return null;");
						$("return new " + entidad.getTipo() + "(data);");
					},");");
				});
				$("public static " + entidad.getTipo() + " getFromKey(String[] $key)throws InterruptedException, java.util.concurrent.ExecutionException", ()->{
					$("com.google.cloud.firestore.DocumentSnapshot ent = rawGet($key);");
					$("if(ent == null)return null;");
					$("return new " + entidad.getTipo() + "(ent);");
				});
				if(entidad.key.isSimple() || KeyConverters.isComplexKey())
					$("public static " + entidad.getTipo() + " get(" + KeyConverters.keyType() + ")throws InterruptedException, java.util.concurrent.ExecutionException", ()->{
						$("return getFromKey(Key.createRawKey(" + entidad.key.getKeyValues() + "));");
					});
				if(!entidad.key.isSimple()) {
					$("public static " + entidad.getTipo() + " get(" + KeyConverters.rawKeyType() + ")throws InterruptedException, java.util.concurrent.ExecutionException", ()->{
						$("return getFromKey(Key.createRawKey(" + entidad.key.getKeyValues() + "));");
					});
					$("public static " + entidad.getTipo() + " get(" + entidad.getTipo() + ".Key $key)throws InterruptedException, java.util.concurrent.ExecutionException", ()->{
						$("return $key==null ? null : getFromKey($key.getRawKey());");
					});
				}
			}
			$("public static boolean exist(" + KeyConverters.keyType() + ")throws InterruptedException, java.util.concurrent.ExecutionException", ()->{
				$("return rawGet(Key.createRawKey(" + entidad.key.getKeyValues() + ")) != null;");
			});
			tryGet:{
				$("public static " + entidad.getTipo() + " tryGetFromKey(String[] $key)throws InterruptedException, java.util.concurrent.ExecutionException", ()->{
					$("com.google.cloud.firestore.DocumentSnapshot ent = rawGet($key);");
					$("if(ent == null)throw new jcrystal.utils.InternalException(17, \"Invalid identifier\");");
					$("return new " + entidad.getTipo() + "(ent);");
				});
				if(entidad.key.isSimple() || KeyConverters.isComplexKey())
					$("public static " + entidad.getTipo() + " tryGet(" + KeyConverters.keyType() + ")throws InterruptedException, java.util.concurrent.ExecutionException", ()->{
						$("return tryGetFromKey(Key.createRawKey(" + entidad.key.getKeyValues() + "));");
					});
				if(!entidad.key.isSimple()) {
					$("public static " + entidad.getTipo() + " tryGet(" + KeyConverters.rawKeyType() + ")throws InterruptedException, java.util.concurrent.ExecutionException", ()->{
						$("return tryGetFromKey(Key.createRawKey(" + entidad.key.getKeyValues() + "));");
					});
					$("public static " + entidad.getTipo() + " tryGet(" + entidad.getTipo() + ".Key $key)throws InterruptedException, java.util.concurrent.ExecutionException", ()->{
						$("return tryGetFromKey($key.getRawKey());");
					});
				}
				$("public static " + entidad.getTipo() + " tryGet(" + KeyConverters.keyType() + ", "+entidad.getTipo()+" $defValue)throws InterruptedException, java.util.concurrent.ExecutionException", ()->{
					$(entidad.getTipo() + " ent = get(" + entidad.key.getKeyValues() + ");");
					$("if(ent == null)return $defValue;");
					$("return ent;");
				});
			}
		}
		@Override
		protected void generatePropertyGetters() {
			for(final EntityField f : entidad.properties){
				if(f.type().is(Password.class)) {
					if(f.hashSalt == null)
						throw new NullPointerException("Todo tipo Password debe tener HashSalt");
					$("private com.google.cloud.firestore.Blob " + f.fieldName() + "()", ()->{
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
							$("return jcrystal.db.firestore.EntityUtils.getLong(rawEntity, \"" + f.dbName + "\");");
						});
						$("public " + $(f.type().firstInnerType()) + "[] " + f.fieldName() + "Array()", ()->{
							$("final long mask = jcrystal.db.firestore.EntityUtils.getLong(rawEntity, \"" + f.dbName + "\");");
							$("if(mask > 0)",()->{
								$("return " + f.type().firstInnerType().getSimpleName() + ".getFromMask(mask);");
							});
							$("return new " + f.type().firstInnerType().getSimpleName() + "[0];");
						});
					}else{
						$("public " + f.type().firstInnerType().getSimpleName() + "[] " + f.fieldName() + "()", ()->{
							$("final int size = jcrystal.db.firestore.EntityUtils.getInt(rawEntity, \"" + f.dbName + ".len\", -1);");
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
				}else {
					String returnType = $($convert(f.type()));
					if(f.isText())
						returnType = "String";
					else if(f.type().isSubclassOf(List.class))
						returnType = "java.util.List<"+f.type().getInnerTypes().get(0).getSimpleName()+">";
						
					$("public " + returnType + " " + f.fieldName() + "()", ()->{
						if(f.type().is(Map.class))
							$if("!rawEntity.hasProperty(\""+f.dbName+"\")",()->{
								$("rawEntity.put(\""+f.dbName+"\", new com.google.appengine.api.datastore.EmbeddedEntity());");
							});
						$("return " + getProperty(f.type(), "\"" + f.dbName + "\"")+";");
					});
					if(context.input.SERVER.WEB.isEnableJSF()){
						$("public " + $($convert(f.type())) + " get" + capitalize(f.fieldName()) + "()",()->{
							$("return this." + f.fieldName()+"();");
						});
					}
				}
			}
			Stream.concat(entidad.manyToOneRelations.stream(), entidad.ownedOneToOneRelations.stream())
				.filter(f->!f.field.isAnnotationPresent(EntityKey.class))
				.filter(f->isExposed(f.getTarget()))
				.forEach(f->{
				String keyType = $($convert(f.getTarget().key.getSingleKeyType()));
				$("public "+keyType+" " + f.fieldName() + "$Key()", ()->{
					if(f.getTarget().key.isSimple())
						$("return ("+keyType+")rawEntity.get(\"" + f.dbName  + "\");");
					else
						$("return "+keyType+".fromPath((String)rawEntity.get(\"" + f.dbName  + "\"));");
				});
				$("public " + $(f.getTarget().clase) + " " + f.fieldName() + "()throws InterruptedException, java.util.concurrent.ExecutionException", ()->{
					if(f.getTarget().key.isSimple())
						$("return "+$(f.getTarget().clase)+".get(" +f.fieldName() + "$Key());");
					else
						$("return "+$(f.getTarget().clase)+".getFromKey("+$(f.getTarget().clase)+".Key.pathToKey((String)rawEntity.get(\"" + f.dbName  + "\")));");
				});
			});
		}
		@Override
		protected void generatePropertySetters() {
			entidad.properties.stream().filter(f-> f.keyData == null).forEach(f->{
				String VISIBILITY = f.isFinal ? "private " : "public ";
				int VISIBILITY_mod = f.isFinal ? Modifier.PRIVATE : Modifier.PUBLIC;
				Runnable doReturn = ()->{
					if(context.input.SERVER.DEBUG.ENTITY_CHECKS)
						$("DEBUG_CHANGES = true;");
					$("return this;");
				};
				if(f.type().is(String.class)) {
					$M(VISIBILITY_mod, entidad.getTipo(), f.fieldName(), $(P($convert(f.type()), f.fieldName())), ()->{
						if(f.hashSalt == null)
							$("rawEntity.put(\"" + f.dbName + "\", " + f.fieldName() + ");");
						else
							$("rawEntity.put(\"" + f.dbName + "\", hash"+capitalize(f.fieldName())+"Base64(" + f.fieldName() + "));");
						doReturn.run();
					});
				}else if (f.isAutoNow && (f.type().is(Date.class) || f.type().isJAnnotationPresent(CrystalDate.class))) {
					$(VISIBILITY + entidad.getTipo() + " " + f.fieldName() + "(java.util.Date " + f.fieldName() + ")",()->{
						if (f.isFinal)
							$("rawEntity.put(\"" + f.dbName + "\", " + f.fieldName() + ");");
						else
							$("rawEntity.put(\"" + f.dbName + "\", " + f.fieldName() + " = new java.util.Date());");
						doReturn.run();
					});
				} else if(f.type().is(Password.class)) {
					if(f.hashSalt == null)
						throw new NullPointerException("Todo tipo Password debe tener HashSalt");
					$("public "+entidad.getTipo()+" " + f.fieldName() + "(String __password)", ()->{
						$("rawEntity.put(\"" + f.dbName + "\", com.google.cloud.firestore.Blob.fromBytes(hash" + capitalize(f.fieldName())+"(__password)));");
						doReturn.run();
					});
				} else if(f.type().is(JSONObject.class, JSONArray.class)) { //Este debe ir antes de isIterable porque el JSONArray es iterable  
					$M(VISIBILITY_mod, entidad.getTipo(), f.fieldName(), $(P(f.type(), f.fieldName())), ()->{
						$if(f.fieldName()+" == null",()-> $("rawEntity.put(\"" + f.dbName + "\", null);"))
						.$else(()->$("rawEntity.put(\"" + f.dbName + "\", new com.google.appengine.api.datastore.Text(" + f.fieldName() + ".toString(0)));"));
						doReturn.run();
					});
				} else if (f.type().isIterable()) {
					IJType innerType = f.type().getInnerTypes().get(0);
					$(VISIBILITY + entidad.getTipo() + " " + f.fieldName() + "("+$(f.type()) + " " + f.fieldName() + ")", ()->{
						if(innerType.isPrimitiveObjectType() || innerType.is(String.class))
							$("rawEntity.put(\"" + f.dbName + "\", " + f.fieldName() + ");");
						else if(innerType.isEnum()) {
							$("rawEntity.put(\"" + f.dbName + "\", " + innerType.prefixName("Utils")+".toIds("+f.fieldName() + "));");
						}else if(f.getTargetEntity() != null) {
							$if(f.fieldName()+" == null", ()->{
								$("rawEntity.put(\"" + f.dbName + "\", null);");
							}).$else(()->{
								$("rawEntity.put(\"" + f.dbName + "\", " + f.fieldName() + ".stream().map(f->f.getRawEntity()).collect(java.util.stream.Collectors.toList()));");	
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
						$("rawEntity.put(\"" + f.dbName + "\", " + f.fieldName() + ");");
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
								$("rawEntity.put(\"" + f.dbName + ".len\", " + f.fieldName() + ".length);");
								$("for(int indx = 0; indx < " + f.fieldName() + ".length; indx++)");
								$("\trawEntity.put(\"" + f.dbName + ".\"+indx, " + f.fieldName() + "[indx]);");
							}).$else(()->{
								$("final int size = jcrystal.db.firestore.EntityUtils.getInt(rawEntity, \"" + f.dbName + ".len\", -1);");
								$("for(int indx = 0; indx < size; indx++)");
								$("	rawEntity.put(\"" + f.dbName + ".\"+indx, null);");
								$("rawEntity.put(\"" + f.dbName + ".len\", 0);");
							});
						}
						doReturn.run();
					});
				} else if(f.isText()) {
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
						else if (f.isEnum()) {
							String idName = f.type().resolve(c->c.enumData.propiedades.get("id")) == null ? ".name()" : ".id";
							$if(f.fieldName()+" == null",()-> $("rawEntity.put(\"" + f.dbName + "\", null);"))
							.$else(()->$("rawEntity.put(\"" + f.dbName + "\", " + f.fieldName() + idName + ");"));
						}else
							$("rawEntity.put(\"" + f.dbName + "\", " + f.fieldName() + ");");
						doReturn.run();
					});
					if(context.input.SERVER.WEB.isEnableJSF()){
						$(VISIBILITY + " void set" + capitalize(f.fieldName()) + "(" + $(f.type()) + " " + f.fieldName() + ")",()->{
							$("this." + f.fieldName()+"(" + f.fieldName() + ");");
						});
					}
				}
			});
			entidad.fields.stream().filter(f-> f.keyData != null && f.keyData.indexAsProperty()).forEach(f->{
				EntityClass target = context.data.entidades.get(f.type());
				String keyName = target.key.isSimple() ? target.key.getKeyValues()+"()":"getRawKeyPath()";
				if(isExposed(target))
					$("private " + entidad.getTipo() + " " + f.fieldName() + "("+$(target.clase)+" val)", ()->{
						$if("val != null",()->{
							$("rawEntity.put(\""+f.dbName+"\", val."+keyName+");");
						}).$else(()->{
							$("rawEntity.put(\""+f.dbName+"\", null);");
						});
						$("return this;");
					});
				$("private  " + entidad.getTipo() + " " + f.fieldName() + "(" + KeyConverters.keyType(target.key.getLlaves()) + ")", ()->{
					if(target.key.isSimple())
						$("rawEntity.put(\""+f.dbName+"\", " + target.key.getKeyValues()+");");
					else
						$("rawEntity.put(\""+f.dbName+"\", " + $(target.clase)+".Key.createRawPath("+target.key.getKeyValues()+"));");
					$("return this;");
				});
			});
			Stream.concat(entidad.manyToOneRelations.stream(), entidad.ownedOneToOneRelations.stream())
				.filter(f->!f.field.isAnnotationPresent(EntityKey.class))
				.filter(f->isExposed(f.getTarget()))
				.forEach(f->{
				String keyName = f.getTarget().key.isSimple()?f.getTarget().key.getKeyValues()+"()":"getRawKeyPath()";
				$("public " + entidad.getTipo() + " " + f.fieldName() + "("+$(f.getTarget().clase)+" val)", ()->{
					$if("val != null",()->{
						$("rawEntity.put(\""+f.dbName+"\", val."+keyName+");");
					}).$else(()->{
						$("rawEntity.put(\""+f.dbName+"\", null);");
					});
					$("return this;");
				});
				$("public " + entidad.getTipo() + " " + f.fieldName() + "("+KeyConverters.keyType(f.getTarget().key.getLlaves()) + ")", ()->{
					if(f.getTarget().key.isSimple())
						$("rawEntity.put(\""+f.dbName+"\", "+f.getTarget().key.getKeyValues()+");");
					else
						$("rawEntity.put(\""+f.dbName+"\", "+$(f.getTarget().clase)+".Key.createRawPath("+f.getTarget().key.getKeyValues()+"));");
					$("return this;");
				});
				if(!f.getTarget().key.isSimple()){
					$("public " + entidad.getTipo() + " " + f.fieldName() + "(" + $($convert(f.getTarget().key.getSingleKeyType())) + " val)", ()->{
						$("rawEntity.put(\"" + f.dbName + "\", val.toPath());");
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
						$("return "+$(f.getOwner().clase)+".Query."+f.fieldName() + "("+f.getTarget().key.getKeyValues()+"());");
					else
						$("return "+$(f.getOwner().clase)+".Query."+f.fieldName() + "(getKey());");
				});
			});
			entidad.targetedOneToOneRelations.stream()
				.filter(f->f.targetName != null)
				.filter(f->isExposed(f.getTarget()))
				.forEach(f->{
					$("public "+$(f.getOwner().clase)+" " + f.targetName + "()", ()->{
					if(f.getTarget().key.isSimple())
						$("return "+$(f.getOwner().clase)+".Query."+f.fieldName()+"("+f.getTarget().key.getKeyValues()+"());");
					else
						$("return "+$(f.getOwner().clase)+".Query."+f.fieldName()+"(getKey());");
				});
			});
			entidad.manyToManyRelations.stream()
				.filter(f->isExposed(f.getTarget()))
				.forEach(f->{
				if(f.smallCardinality) {
					String keyType = f.getTarget().key.isSimple()?f.getTarget().key.getKeyTypes(this):"String";
					if(f.getOwner() == entidad) {
						$("@SuppressWarnings(\"unchecked\")");
						$("public java.util.List<"+keyType+"> " + f.fieldName() + "$Key()", ()->{
							$("return (java.util.List<"+keyType+">)rawEntity.get(\"" + f.dbName  + "\");");
						});
						$("public  "+ f.getOwner().getTipo()+" " + f.fieldName() + "$Key(java.util.List<"+keyType+"> $vals)", ()->{
							$("rawEntity.put(\""+f.dbName+"\", $vals);");
							$("return this;");
						});
						$("public "+f.getOwner().getTipo()+" " + f.fieldName() + "(java.util.function.Consumer<java.util.List<"+keyType+">> editor)", ()->{
							$("java.util.List<"+keyType+"> $temp = "+f.fieldName()+"$Key();");
							$("if($temp==null)$temp = new java.util.ArrayList<>();");
							$("editor.accept($temp);");
							$("rawEntity.put(\""+f.dbName+"\", $temp);");
							$("return this;");
						});
						$("public "+f.getOwner().getTipo()+" " + f.fieldName() + "("+$(f.getTarget().clase)+" val)", ()->{
							if(f.getTarget().key.isSimple())
								$("return this."+f.fieldName()+"(list->list.add(val."+f.getTarget().key.getLlaves().get(0).fieldName()+"()));");
							else
								$("return this."+f.fieldName()+"(list->list.add(val.getRawKeyPath()));");
						});
						$("public "+f.getOwner().getTipo()+" " + f.fieldName() + "(java.util.List<"+$(f.getTarget().clase)+"> $val)", ()->{
							if(f.getTarget().key.isSimple())
								$("rawEntity.put(\""+f.dbName+"\", $val==null ? null : $val.stream().map($v -> $v."+f.getTarget().key.getLlaves().get(0).fieldName()+").collect(java.util.stream.Collectors.toList()));");
							else
								$("rawEntity.put(\""+f.dbName+"\", $val==null ? null : $val.stream().map($v -> $v.getRawKeyPath()).collect(java.util.stream.Collectors.toList()));");
							$("return this;");
						});
						$("public "+f.getOwner().getTipo()+" " + f.fieldName() + "("+KeyConverters.keyType(f.getTarget().key.getLlaves())+")", ()->{
							if(f.getTarget().key.isSimple())
								$("return this."+f.fieldName()+"(list->list.add("+f.getTarget().key.getKeyValues()+"));");
							else
								$("return this."+f.fieldName()+"(list->list.add("+$(f.getTarget().clase)+".Key.createRawPath("+f.getTarget().key.getKeyValues()+")));");
						});
						$("public com.google.api.core.ApiFuture<java.util.List<" + $(f.getTarget().clase) + ">> " + f.fieldName() + "Async()", ()->{
							$("return "+$(f.getTarget().clase)+".Batch.getAsync(" +f.fieldName() + "$Key());");
						});
						$("public java.util.List<" + $(f.getTarget().clase) + "> " + f.fieldName() + "() throws InterruptedException, java.util.concurrent.ExecutionException", ()->{
							$("return "+$(f.getTarget().clase)+".Batch.get(" +f.fieldName() + "$Key());");
						});
					}else if(f.getTarget() == entidad && f.targetName != null) {
						$("public java.util.List<"+$(f.getOwner().clase)+"> " + f.targetName + "()", ()->{
							if(f.getTarget().key.isSimple())
								$("return "+$(f.getOwner().clase)+".Query." + f.fieldName() + "("+f.getTarget().key.getKeyValues()+"());");
							else
								$("return "+$(f.getOwner().clase)+".Query." + f.fieldName() + "(getKey());");
						});
					}
				}
			});
		}
		@Override
		protected void generateQueries() {
			if(entidad.padre != null)
				$("public static class Q{");
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
			if(entidad.padre != null)
				$("}");
			cachedGetter:if(entidad.key != null){
				if(entidad.key.getLlaves().size() == 1) {
					$("public static class CachedGetter", ()->{
						final String keyTypes = entidad.key.getKeyTypes(this);
						$("private java.util.TreeMap<"+keyTypes+", "+entidad.getTipo()+"> cache = new java.util.TreeMap<>();");
						$("public " + entidad.getTipo() + " get(" + KeyConverters.keyType() + ")throws InterruptedException, java.util.concurrent.ExecutionException", ()->{
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
							$("byte[] digest = hash" + capitalize(f.fieldName()) + "(password);");
							$("return java.util.Arrays.equals("+f.fieldName()+"().toBytes(), digest);");
						});
						$("public static String hash" + capitalize(f.fieldName()) + "Base64(String value)", ()->{
							$("return java.util.Base64.getEncoder().encodeToString(hash" + capitalize(f.fieldName()) + "(value));");
						});								
					});
				if(entidad.isSecurityToken()){
					$("public static "+entidad.getTipo()+" generateTokenBase64(String value)", ()->{
						$("try",()->{
							$("java.security.MessageDigest md = java.security.MessageDigest.getInstance(\"SHA-256\");");
							Random r = new Random();
							$("byte[] hash = md.digest((value + \"KFsWV4g" + Long.toString(r.nextLong(), 32) + Long.toString(r.nextLong(), 32) + Long.toString(r.nextLong(), 32) + "\" + System.currentTimeMillis()).getBytes(\"UTF-8\"));");
							$("return new "+entidad.getTipo()+"(java.util.Base64.getEncoder().encodeToString(hash));");
						});
						$("catch(java.io.UnsupportedEncodingException | java.security.NoSuchAlgorithmException ex)",()->{
							$("throw new jcrystal.http.responses.HttpInternalServerError500(\"Internal server error generating authentication token\");");
						});
					});
				}
			}
		}
		
		@Override public void generateExtras() {
			if(back.id == null) {
				if(!entidad.isSecurityToken())
					$("public static class Post extends Post"+entidad.clase.getSimpleName()+"{}");
				$("public static class Serializer extends Serializer"+entidad.clase.getSimpleName()+"{}");
			}
			if(!entidad.entidad.internal()) {
				if(entidad.key.isSimple())
					$("public static Query"+entidad.clase.getSimpleName()+" Query() { return new Query"+entidad.clase.getSimpleName()+"(); }");
				else {
					$("public static Query" + entidad.getTipo() + " Query(" + KeyConverters.keyType(entidad.key.parent()) + ")",()->{
						$("return new Query"+entidad.clase.getSimpleName()+"(" + KeyConverters.keyNames(entidad.key.parent()) + ");");
					});
					if(!entidad.key.isSimple() && (entidad.key.parent().count() != 1 || entidad.key.parent().noneMatch(f->f.getTargetEntity().key.isSimple())))
						$("public static Query" + entidad.getTipo() + " Query(" + KeyConverters.entityType(entidad.key.parent()) + ")",()->{
							$("return new Query" + entidad.clase.getSimpleName()+"(" + KeyConverters.keyNames(entidad.key.parent()) + ");");
						});
				}
				$("public static class Batch extends Batch"+entidad.clase.getSimpleName()+"{}");
			}
			$("public static class " + context.data.entidades.MetaClassName + " extends Meta"+entidad.clase.getSimpleName()+"{}");
			
			$("public static java.util.List<" + entidad.getTipo()+"> convertRawList(java.util.List<com.google.cloud.firestore.DocumentSnapshot> rawData)", ()->{
				$if("rawData == null", "return null;");
				$else(()->{
					$("java.util.ArrayList<"+entidad.getTipo()+"> ret = new java.util.ArrayList<>();");
					$("for(com.google.cloud.firestore.DocumentSnapshot data : rawData)",()->{
						$("ret.add(new "+entidad.getTipo()+"(data));");
					});
					$("return ret;");
				});
			});
			if(context.input.SERVER.DEBUG.ENTITY_CHECKS) {
				$("//DEBUG");
				$("public boolean DEBUG_CHANGES = false;");
				$("@Override public boolean DEBUG_CHANGES(){return DEBUG_CHANGES;}");
			}
		}
		
		private String getProperty(IJType tipo, String nombrePropiedad){
			tipo = GlobalTypeConverter.INSTANCE.convert(tipo);
			if (tipo.is(String.class, Text.class, LongText.class)) 
				return "jcrystal.db.firestore.EntityUtils.getString(rawEntity, " + nombrePropiedad + ")";
			else if (tipo.is(JSONObject.class))
				return "jcrystal.db.firestore.EntityUtils.getJsonObject(rawEntity, " + nombrePropiedad + ")";
			else if (tipo.is(JSONArray.class))
				return "jcrystal.db.firestore.EntityUtils.getJsonArray(rawEntity, " + nombrePropiedad + ")";
			else if (tipo.is(int.class, double.class, boolean.class, long.class)) {
				String name = StringUtils.capitalize(tipo.getSimpleName());
				return "jcrystal.db.firestore.EntityUtils.get"+name+"(rawEntity, " + nombrePropiedad + ", " + GlobalTypes.defaultValues.get(tipo) + ")";
			}else if (tipo.is(char.class)){
				return "(char)jcrystal.db.firestore.EntityUtils.getInt(rawEntity, " + nombrePropiedad + ")";
			}
			else if (tipo.is(Integer.class))
				return "jcrystal.db.firestore.EntityUtils.getInteger(rawEntity, " + nombrePropiedad + ")";
			else if (tipo.is(Long.class))
				return "jcrystal.db.firestore.EntityUtils.getLongObj(rawEntity, " + nombrePropiedad + ")";
			else if (tipo.is(Date.class) || tipo.isJAnnotationPresent(CrystalDate.class))
				return "(java.util.Date)rawEntity.get(" + nombrePropiedad + ")";
			else if (tipo.name().startsWith("com.google.appengine.api.datastore."))
				return "(" + tipo.getSimpleName() + ")rawEntity.get(" + nombrePropiedad + ")";
			else if (tipo.is(Password.class))
				return "(com.google.cloud.firestore.Blob)rawEntity.get(" + nombrePropiedad + ")";
			else if (tipo.isEnum()) {
				if(tipo.resolve(c->c.enumData.propiedades.get("id")) == null)
					return tipo.prefixName("Utils") + ".fromId(jcrystal.db.firestore.EntityUtils.getString(rawEntity, " + nombrePropiedad + "))";
				else
					return tipo.prefixName("Utils") + ".fromId(jcrystal.db.firestore.EntityUtils.getInt(rawEntity, " + nombrePropiedad + "))";
			}else if (tipo.is(Map.class)) {
				IJType keyType = tipo.getInnerTypes().get(0);
				IJType valueType = tipo.getInnerTypes().get(1);
				return "new jcrystal.db.datastore.EntityMap<"+valueType.name()+">(rawEntity, " + nombrePropiedad + ")";
			}
			else if (tipo.isSubclassOf(List.class)) {
				if (tipo.getInnerTypes().get(0).isPrimitiveObjectType())
					return "(java.util.List<"+tipo.getInnerTypes().get(0).getSimpleName()+">)rawEntity.get(" + nombrePropiedad + ")";
				else if (tipo.getInnerTypes().get(0).isEnum()) {
					IJType typeId = ((JClass)tipo.getInnerTypes().get(0)).enumData.propiedades.get("id");
					if(typeId == null || typeId.is(String.class))
						return tipo.getInnerTypes().get(0).prefixName("Utils") + ".fromString((java.util.List<String>)rawEntity.get(" + nombrePropiedad + "))";
					else
						throw new NullPointerException(tipo.name() + ": " + tipo);
				}
				else{
					EntityClass targetEntity = context.data.entidades.get(tipo.getInnerTypes().get(0));
					if(targetEntity != null) {
						return targetEntity.getTipo() + ".convertRawList((java.util.List<com.google.appengine.api.datastore.EmbeddedEntity>)rawEntity.get(" + nombrePropiedad + "))";
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
						$("private int offset = 0;");
						$("private String[] rawKey;");
						$("public Key(" + KeyConverters.keyType() + ")",()->{
							$("this.rawKey = new String[KEY_SIZE];");
							$("fillKeyData(this.rawKey, 0, "+KeyConverters.keyNames()+");");
						});
						$("public Key(String[] rawKey, int offset)",()->{
							$("this.rawKey = rawKey;");
							$("this.offset = offset;");
						});
						$("public Key(String rawKey)",()->{
							$("this.rawKey = pathToKey(rawKey);");
						});
						$("public boolean isCompleteKey()",()->{
							$("return isCompleteKey(this.rawKey);");
						});
						$("public static Key fromPath(String path)",()->{
							$("if(path == null)return null;");
							$("return new Key(path);");
						});
						$("public String toPath()",()->{
							$("return keyToPath(rawKey, offset);");
						});
						$("public int getOffset(){ return offset;}");
						$("public String[] getRawKey(){ return rawKey;}");
					}else {
						$("private Key(){}");
					}
					$("public static String keyToPath(String[] rawKey, int offset)",()->{
						$("String ret = rawKey[offset];");
						$("for(int pos = 1, limit = rawKey[offset + KEY_SIZE - 1] == null ? KEY_SIZE - 1 : KEY_SIZE; pos < limit; pos++)",()->{
							$("ret += \"/\";");
							$("ret += rawKey[offset + pos];");
						});
						$("return ret;");
					});
					$("public static String[] pathToKey(String path)",()->{
						$("String[] rawKey = new String[KEY_SIZE];");
						$("java.util.StringTokenizer tokenizer = new java.util.StringTokenizer(path, \"/\");");
						$("for(int pos = 0; pos < KEY_SIZE && tokenizer.hasMoreTokens(); pos++)rawKey[pos] = tokenizer.nextToken();");
						$("return rawKey;");
					});
					$("public static boolean isCompleteKey(String[] rawKey)",()->{
						$("return rawKey[KEY_SIZE - 1] != null;");
					});
					$("private static void completeKey(String[] rawKey, String path)",()->{
						$("rawKey[KEY_SIZE - 1] = path.substring(1 + path.lastIndexOf('/'));");
					});
					{
						StringBuilder size = new StringBuilder("offset");
						StreamUtils.forEachWithIndex(entidad.key.getLlaves(), (i,key)->{
							IJType type = key.getTargetEntity() != null ? key.getTargetEntity().key.getSingleKeyType() : key.type();
							if(!entidad.key.isSimple())
								$("public final " + $($convert(type)) + " " + key.fieldName() + "()", ()->{
									$("return " + key.fieldName() + "(rawKey, offset);");
								});
							$("public static " + $($convert(type)) + " " + key.fieldName() + "(String[] rawKey, int offset)", ()->{
								String keyExp = "rawKey[" + size + " + 1]";
								if (type.is(String.class))
									$("return "+keyExp+";");
								else if (type.is(long.class,Long.class))
									$("return Long.parseLong(" + keyExp + ");");
								else if (type.isJAnnotationPresent(CrystalDate.class)) {
									$("try", ()->{
										$("return "+type.getSimpleName() + ".SDF.parse("+keyExp+");");
									});
									$("catch(ParseException e)", ()->{});
									$("return new " + type.getSimpleName()+ "("+keyExp+");");
								} else if (type.is(Date.class)) {
									DateType tipo = key.f.isJAnnotationPresent(CrystalDate.class)?key.f.getJAnnotation(CrystalDate.class).value():DateType.DATE_MILIS;
									$("try", ()->{
										$("return DateType." + tipo + ".FORMAT.parse("+keyExp+");");
									});
									$("catch(ParseException e)", ()->{});
									$("return null;");
								} else
									$("return new " + type.name()+"(rawKey, " + size + ");");
							});
							if(key.getTargetEntity() != null)
								size.append(" + " + $($convert(key.getTargetEntity().clase))+".Key.KEY_SIZE");
							else
								size.append(" + 2");
						});
						$("public static final int KEY_SIZE = " + size.substring(9) + ";");
					}
					$("public static String cloneKey(String rawKey)",()->{
						$("return rawKey;");
					});
					if(!entidad.key.isSimple()) {
						if(entidad.key.stream().anyMatch(f->f.getTargetEntity() != null && isExposed(f.getTargetEntity())))
							$("public static String[] createRawKey(" + KeyConverters.entityType() + ")", ()->{
								$("return createRawKey(" + KeyConverters.entityToRawKey() + ");");
							});
						if(entidad.key.isAutogeneratedKey()) {
							if(entidad.key.parent().anyMatch(f->f.getTargetEntity() != null && !f.getTargetEntity().key.isSimple()))
								$("private static String[] createRawParentKey(" + KeyConverters.keyType(entidad.key.parent()) + ")", ()->{
									$("return createRawParentKey(" + entidad.key.parent().map(f->{
										if(f.getTargetEntity() != null) {
											if(!f.getTargetEntity().key.isSimple())
												return f.fieldName()+".getRawKey()";
											else
												return $(f.getTargetEntity().clase)+".Key.createRawKey("+f.fieldName()+")";
										}
										return f.fieldName();
									}).collect(Collectors.joining(", "))+");");
								});
							$("public static String[] createRawParentKey(" + KeyConverters.rawKeyType(entidad.key.size() - 1) + ")", ()->{
								$("return createRawKey(" + KeyConverters.getRawKeyExpresion("ENTITY_NAME", entidad.key.size() - 1)+", null);");
							});
						}
					}
					
					for(boolean raw : new boolean[] {false, true})
						if(raw || KeyConverters.isComplexKey()) {
							$("public static String[] createRawKey(" + (raw ? KeyConverters.rawKeyType() : KeyConverters.keyType()) + ")", ()->{
								String cond = entidad.key.getLlaves().stream().filter(f->{
									if(f.isAutogenerated())
										return false;
									if(f.keyData == null || f.getTargetEntity() == null)
										return $convert(f.type()).nullable();
									else
										return true;
								}).map(f->f.fieldName()+" == null").collect(Collectors.joining(" || "));
								if(!cond.isEmpty())
									$if(cond,()->{
										$("return null;");
									});
								$("String[] rawKey = new String[KEY_SIZE];");
								$("fillKeyData(rawKey, 0, "+KeyConverters.keyNames()+");");
								$("return rawKey;");
							});
							$("public static void fillKeyData(String[] rawKey, int offset, " + (raw ? KeyConverters.rawKeyType() : KeyConverters.keyType()) + ")",()->{
								StringBuilder size = new StringBuilder("offset");
								StreamUtils.forEachWithIndex(entidad.key.getLlaves(), (i,key)->{
									IJType type = key.getTargetEntity() != null ? key.getTargetEntity().key.getSingleKeyType() : key.type();
									if(entidad.key.size() - 1 == i)
										$("rawKey[" + size + "] = " + $($convert(entidad.clase)) + ".ENTITY_NAME;");
									else if(key.getTargetEntity() == null)
										$("rawKey[" + size + "] = \"" + key.fieldName() + "\";");
									else
										$("rawKey[" + size + "] = " + $($convert(key.getTargetEntity().clase)) + ".ENTITY_NAME;");
									if (type.is(String.class))
										$("rawKey[" + size + " + 1] = " + key.fieldName() + ";");
									else if (type.is(long.class))
										$("rawKey[" + size + " + 1] = Long.toString(" + key.fieldName() + ");");
									else if (type.is(Long.class))
										$("rawKey[" + size + " + 1] = " + key.fieldName() + ".toString();");
									else if (type.isJAnnotationPresent(CrystalDate.class)) {
										$("rawKey[" + size + " + 1] = "+type.getSimpleName() + ".format("+key.fieldName()+");");
									} else if (type.is(Date.class)) {
										DateType tipo = key.f.isJAnnotationPresent(CrystalDate.class)?key.f.getJAnnotation(CrystalDate.class).value():DateType.DATE_MILIS;
										$("rawKey[" + size + " + 1] = "+tipo + ".FORMAT.format("+key.fieldName()+");");
									} 
									else if(key.getTargetEntity().key.isSimple())
										$("rawKey[" + size + " + 1] = " + key.fieldName() + ";");
									else if(raw)
										$("System.arraycopy(" + key.fieldName() + ", 0, rawKey, " + size + ", " + $($convert(key.getTargetEntity().clase))+".Key.KEY_SIZE);");
									else
										$("System.arraycopy(" + key.fieldName() + ".getRawKey(), " + key.fieldName() + ".getOffset(), rawKey, " + size + ", " + $($convert(key.getTargetEntity().clase))+".Key.KEY_SIZE);");
									
									if(key.getTargetEntity() != null)
										size.append(" + " + $($convert(key.getTargetEntity().clase))+".Key.KEY_SIZE");
									else
										size.append(" + 2");
								});
							});
						}
					for(boolean raw : new boolean[] {true, false})
						if(raw || KeyConverters.isComplexKey())
							$("public static String createRawPath(" + (raw?KeyConverters.rawKeyType():KeyConverters.keyType()) + ")",()->{
								String cond = entidad.key.getLlaves().stream().filter(f->{
									if(f.isAutogenerated())
										return false;
									if(f.keyData == null || f.getTargetEntity() == null)
										return $convert(f.type()).nullable();
									else
										return true;
								}).map(f->f.fieldName()+" == null").collect(Collectors.joining(" || "));
								if(!cond.isEmpty())
									$if(cond,"return null;");
								
								String ret = "";
								for(EntityField key : entidad.key.getLlaves()){
									IJType type = key.getTargetEntity() != null ? key.getTargetEntity().key.getSingleKeyType() : key.type();
									if(key == entidad.key.getLast())
										ret += " + \"/\" + "+$($convert(entidad.clase)) + ".ENTITY_NAME";
									else if(key.getTargetEntity() == null)
										ret += " + \"/" + key.fieldName() + "\"";
									else
										ret += " + \"/\" + "+$($convert(key.getTargetEntity().clase)) + ".ENTITY_NAME";
		
									if(key == entidad.key.getLast()) {
										String last = ret;
										$if(entidad.key.getLast().fieldName() + " == null",()->{
											$("return " + last.substring(3).replaceFirst("/", "").replaceFirst("\"\" \\+ ", "")+";");							
										});
									}
								
									if (type.is(String.class,long.class,Long.class))
										ret += " + \"/\" + " + key.fieldName();
									else if (type.isJAnnotationPresent(CrystalDate.class)) {
										ret += " + \"/\" + " + type.getSimpleName() + ".format("+key.fieldName()+")";
									} else if (type.is(Date.class)) {
										DateType tipo = key.f.isJAnnotationPresent(CrystalDate.class)?key.f.getJAnnotation(CrystalDate.class).value():DateType.DATE_MILIS;
										ret += " + \"/\" + " + tipo + ".FORMAT.format("+key.fieldName()+")";
									} 
									else if(key.getTargetEntity().key.isSimple())
										ret += " + \"/\" + " + key.fieldName();
									else if(raw)
										ret += " + \"/\" + " + $($convert(key.getTargetEntity().clase))+".Key.keyToPath("+key.fieldName()+", 0)";
									else
										ret += " + \"/\" + " + key.fieldName()+".toPath()";
								};
								
								$("return " + ret.substring(3).replaceFirst("/", "").replaceFirst("\"\" \\+ ", "")+";");
							});
				});
			}
		}
	}
	
	
}

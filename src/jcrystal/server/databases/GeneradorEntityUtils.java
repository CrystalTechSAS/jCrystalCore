package jcrystal.server.databases;

import java.io.IOException;
import java.util.Arrays;

import com.google.appengine.api.datastore.Query.FilterOperator;

import jcrystal.json.JsonLevel;
import jcrystal.main.data.ClientContext;
import jcrystal.main.data.DataBackends.BackendWrapper;
import jcrystal.model.server.db.EntityClass;
import jcrystal.reflection.MaskedEnum;
import jcrystal.reflection.annotations.CrystalDate;
import jcrystal.reflection.annotations.FullJsonImport;
import jcrystal.utils.SerializeLevelUtils;
import jcrystal.utils.langAndPlats.JavaCode;

public class GeneradorEntityUtils {
	
	private ClientContext context;
	
	public GeneradorEntityUtils(ClientContext context) {
		this.context = context;
	}
	public void generateComparisonOperator(BackendWrapper back) {
		new JavaCode(){{
			$("package jcrystal.server.db.query;");
			$("public enum Op", ()->{
				Arrays.stream(FilterOperator.values()).forEach(operator->{
					switch (operator) {
						case GREATER_THAN:
						case GREATER_THAN_OR_EQUAL:
						case EQUAL:
						case LESS_THAN:
						case LESS_THAN_OR_EQUAL:
							$(getOperatorName(operator) + "(com.google.appengine.api.datastore.Query.FilterOperator."+operator.name()+"),");
							break;
						default:break;
					}
				});
				$(";");
				$("public final com.google.appengine.api.datastore.Query.FilterOperator datastoreOp;");
				$("private Op(com.google.appengine.api.datastore.Query.FilterOperator datastoreOp)",()->{
					$("this.datastoreOp = datastoreOp;");
				});
			});
			context.output.exportFile(back, this, "jcrystal/server/db/query/Op.java");
		}};
	}
	public static String getOperatorName(FilterOperator operator) {
		switch (operator) {
		case GREATER_THAN:
			return "greaterThan";
		case GREATER_THAN_OR_EQUAL:
			return "greaterThanEq";
		case EQUAL:
			return "is";
		case LESS_THAN:
			return "lessThan";
		case LESS_THAN_OR_EQUAL:
			return "lessThanEq";
		default:break;
		}
		return null;
	}
	public void generateInterface() {
		new JavaCode(){{
			$("package jcrystal.server;");
			$("public class Entity", ()->{
				$("private Entity(){}");
				context.data.databases.forEach((name, db)->{
					$("public interface " + db.getDBName(),()->{
						switch (db.type) {
							case GOOGLE_REALTIMEDB:
								$("public java.util.Map<String, Object> getRawEntity();");
								break;
							case GOOGLE_DATASTORE:
									$("public com.google.appengine.api.datastore.Entity getRawEntity();");
								delete:{
									$("default void deleteTxn()", ()->{
										$("jcrystal.context.CrystalContext $ctx = jcrystal.context.CrystalContext.get();");
										$("$ctx."+db.getDBName()+"().service.delete($ctx."+db.getDBName()+"().getTxn(), getRawEntity().getKey());");
									});
									$("default void delete()", ()->{
										$("jcrystal.context.CrystalContext.get()."+db.getDBName()+"().service.delete((com.google.appengine.api.datastore.Transaction)null, getRawEntity().getKey());");
									});
								}
								$("default com.google.appengine.api.datastore.Key getKey()",()->{
									$("return getRawEntity().getKey();");
								});
								break;
							case GOOGLE_FIRESTORE:
								$("public java.util.Map<String, Object> getRawEntity();");
								$("public String[] getRawKey();");
								$("public String getRawKeyPath();");
								$("default void delete()", ()->{
									$("jcrystal.context.CrystalContext.get().DefaultDB().delete(jcrystal.context.CrystalContext.get().DefaultDB().service.document(getRawKeyPath()));");
								});
								break;
							default:
								break;
						}
						if(context.input.SERVER.DEBUG.ENTITY_CHECKS) {
							$("//DEBUG");
							$("public boolean DEBUG_CHANGES();");
						}
					});
				});
			});
			context.output.exportFile(this, "jcrystal/server/Entity.java");
		}};
	}
	public void generateJsonObject(final EntityClass entidad) throws IOException{
		if(entidad.clase.isAnnotationPresent(FullJsonImport.class))
			new JavaCode(){{
				$("package "+entidad.clase.getPackageName()+";");
				$("import jcrystal.datetime.*;");
				$("public class JsonObject" + entidad.getTipo(), ()->{
					context.utils.converters.relationsToKeys.extending(()->{
						entidad.iterateKeysAndProperties().forEach(f->{
							if(SerializeLevelUtils.getAnnotedJsonLevel(f) != JsonLevel.NONE) {
								if(f.type().isJAnnotationPresent(CrystalDate.class))
									$("public java.util.Date " + f.fieldName()+";");
								else
									$("public "+$(f.type())+" " + f.fieldName()+";");
							}
						});
						entidad.manyToOneRelations.stream().filter(f-> f.editable && f.getTarget().key.isSimple()).forEach(r->
							$("public " + $(r.getTarget().key.getSingleKeyType()) + " " + r.fieldName+";")
						);
						$("public JsonObject"+entidad.getTipo()+"(org.json.JSONObject json)", ()->{
							entidad.iterateKeysAndProperties().forEach((f)->{
								if(SerializeLevelUtils.getAnnotedJsonLevel(f) != JsonLevel.NONE)
									context.utils.extrators.jsonObject.procesarCampo(this, entidad.clase, f.fieldAccessor().prefix("this."));
							});
							entidad.manyToOneRelations.stream().filter(f-> f.editable && f.getTarget().key.isSimple()).forEach(r->{
								context.utils.extrators.jsonObject.procesarCampo(this, entidad.clase, r.field.fieldAccessor().prefix("this."));
							});
						});
						$("public JsonObject"+entidad.getTipo()+"("+entidad.clase.name()+" entidad)", ()->{
							entidad.iterateKeysAndProperties().filter(f->SerializeLevelUtils.getAnnotedJsonLevel(f) != JsonLevel.NONE).forEach(f->{
								if(f.isArray() && f.type().firstInnerType().isSubclassOf(MaskedEnum.class))
									$("this."+f.fieldName()+" = entidad." + f.fieldName()+"Array();");
								else if(f.isText())
									$("this."+f.fieldName()+" = entidad." + f.fieldName()+"();");
								else if(f.f.type().isJAnnotationPresent(CrystalDate.class))
									$("this."+f.fieldName()+" = entidad." + f.fieldName()+"();");
								else
									$("this."+f.fieldName()+" = entidad." + f.fieldName()+"();");
							});
						});						
					});
				});
				context.output.exportFile(this, entidad.clase.getPackageName().replace(".", "/")+"/JsonObject"+entidad.getTipo()+".java");
			}};
	}
	
}

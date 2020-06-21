package jcrystal.server.databases.google.bigtable;

import java.util.Date;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.appengine.api.datastore.GeoPt;

import jcrystal.model.server.db.EntityField;
import jcrystal.server.databases.AbsEntityGenerator;
import jcrystal.server.databases.AbsUtilsEntityGenerator;
import jcrystal.types.IJType;

public class UtilsEntityGenerator  extends AbsUtilsEntityGenerator{
	
	public UtilsEntityGenerator(AbsEntityGenerator parent) {
		super(parent);
		// TODO Auto-generated constructor stub
	}
	@Override
	protected void putUtils() {
		generarBatchClass();
	}
	@Override
	protected String[] getAdditionalImports() {
		return null;
	}
	private void generarBatchClass(){
		$("class Batch" + entidad.getTipo(), ()->{
			$("public static com.google.cloud.bigquery.InsertAllResponse put(java.lang.Iterable<"+entidad.getTipo()+"> it)",()->{
				$("jcrystal.context.CrystalContext $ctx = jcrystal.context.CrystalContext.get();");
				$("com.google.cloud.bigquery.InsertAllRequest.Builder $insertBuilder = $ctx."+entidad.mainDBType.getDBName()+"().createBuilder("+entidad.getTipo()+".ENTITY_NAME);");
				$("it.forEach($value->",()->{
					if(entidad.key == null)
						$("$insertBuilder.addRow($value.rawEntity);");
					else {
						EntityField k = entidad.key.getLlaves().get(0); 
						if (k.type().is(String.class))
							$("$insertBuilder.addRow((String)$value."+k.fieldName()+"(), $value.rawEntity);");
						else if (k.type().isPrimitive())
							$("$insertBuilder.addRow("+k.type().getObjectType().getSimpleName()+".toString($value."+k.fieldName()+"()), $value.rawEntity);");
						else if (k.type().isPrimitiveObjectType())
							$("$insertBuilder.addRow("+k.type().getSimpleName()+".toString($value."+k.fieldName()+"()), $value.rawEntity);");
					}
					
				},");");
				$("try",()->{
					$("return $ctx."+entidad.mainDBType.getDBName()+"().service.insertAll($insertBuilder.build());");
				});
				$catch("com.google.cloud.bigquery.BigQueryException ex",()->{
					$if("ex.getMessage().startsWith(\"Not found: Dataset \")",()->{
						$("$ctx."+entidad.mainDBType.getDBName()+"().createDataset();");
					}).$else("ex.getMessage().startsWith(\"Not found: Table \")", ()->{
						Function<IJType, String> transform = type->{
							if(type.is(long.class, int.class, short.class, byte.class))
								return "NUMERIC";
							else if(type.is(boolean.class))
								return "BOOLEAN";
							else if(type.is(Date.class))
								return "DATETIME";
							else if(type.is(String.class))
								return "STRING";
							else if(type.is(GeoPt.class))
								return "STRING";//GEOGRAPHY?
							else
								throw new NullPointerException("Unssuporte type on BigQuery : " + type);
						};
						String fields = entidad.properties.stream().map(field->{
							return "com.google.cloud.bigquery.Field.of(\""+field.dbName+"\", com.google.cloud.bigquery.LegacySQLTypeName."+transform.apply(field.type())+")";
						}).collect(Collectors.joining(", "));
						$("com.google.cloud.bigquery.StandardTableDefinition tableDefinition = com.google.cloud.bigquery.StandardTableDefinition.of(com.google.cloud.bigquery.Schema.of("+fields+"));");
						$("$ctx."+entidad.mainDBType.getDBName()+"().createTable(\""+entidad.name()+"\", tableDefinition);");
					});
					$("throw ex;");
				});
			});
		});
	}		
}
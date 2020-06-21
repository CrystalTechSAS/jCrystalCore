package jcrystal.server.databases.google.firebase.firestore;

import static java.lang.reflect.Modifier.PUBLIC;
import static jcrystal.utils.StringUtils.capitalize;

import java.util.function.Function;

import jcrystal.annotations.internal.model.db.InternalEntityKey;
import jcrystal.model.server.db.EntityClass;
import jcrystal.model.server.db.IndexableField;
import jcrystal.model.server.db.EntityIndexModel;
import jcrystal.reflection.annotations.IndexType;
import jcrystal.server.databases.ComparisonOperator;
import jcrystal.utils.StringSeparator;
import jcrystal.utils.langAndPlats.JavaCode;

public class QueryClassGenerator extends JavaCode{
	MainEntityGenerator parent;
	EntityClass entidad;
	public QueryClassGenerator(MainEntityGenerator parent) {
		this.parent = parent;
		this.entidad = parent.entidad;
	}
	public static void generateUtils(MainEntityGenerator parent) {
		new JavaCode() {{
			$("package jcrystal.db.firestore;");
			addStream(jcrystal.server.databases.google.firebase.firestore.MainEntityGenerator.class.getResourceAsStream("AbsQueryHelper"));
			parent.context.output.exportFile(parent.back, this, "jcrystal/db/firestore/AbsQueryHelper.java");
		}};
		new JavaCode() {{
			$("package jcrystal.db.firestore;");
			addStream(jcrystal.server.databases.google.firebase.firestore.MainEntityGenerator.class.getResourceAsStream("ApiFutureTransform"));
			parent.context.output.exportFile(parent.back, this, "jcrystal/db/firestore/ApiFutureTransform.java");
		}};
	}
	public void generate() {
		if(parent.entidad.entidad.internal())
			return;
		$("package "+entidad.clase.getPackageName()+";");
		$("import jcrystal.db.firestore.ApiFutureTransform;");
		$("import java.util.concurrent.ExecutionException;");
		$("public class Query" + entidad.getTipo() + " extends jcrystal.db.firestore.AbsQueryHelper", ()->{
			$("protected Query" + entidad.getTipo() + "(" + parent.KeyConverters.keyType(entidad.key.parent()) + ")",()->{
				String parentArgs = parent.KeyConverters.keyToRawKey(entidad.key.parent());
				if(!parentArgs.isEmpty())
					parentArgs += ", null";
				else
					parentArgs = "null";
				$("super(jcrystal.context.CrystalContext.get()."+entidad.mainDBType.getDBName()+"().service.collection(" +entidad.getTipo()+ ".Key.createRawPath(" + parentArgs + ")));");
			});
			if(!entidad.key.isSimple() && (entidad.key.parent().count() != 1 || entidad.key.parent().noneMatch(f->f.getTargetEntity().key.isSimple())))
				$("protected Query" + entidad.getTipo() + "(" + parent.KeyConverters.entityType(entidad.key.parent()) + ")",()->{
					String parentArgs = parent.KeyConverters.entityToRawKey(entidad.key.parent());
					if(!parentArgs.isEmpty())
						parentArgs += ", null";
					else
						parentArgs = "null";
					$("super(jcrystal.context.CrystalContext.get()."+entidad.mainDBType.getDBName()+"().service.collection(" +entidad.getTipo()+ ".Key.createRawPath(" + parentArgs + ")));");
				});
			
			getAllBy:{
				for(EntityClass c = entidad; c != null; c = c.padre) {
					final EntityClass finalC = c;
					c.fields.stream().filter(f->f.indexType() != IndexType.NONE).forEach(f->craerIndiceCampo(false, f));
					c.manyToOneRelations.stream().forEach(f->craerIndiceCampo(false, f.getOwnerIndexableField()));
					c.ownedOneToOneRelations.stream().forEach(f->craerIndiceCampo(false, f.getOwnerIndexableField()));
					c.manyToManyRelations.stream().filter(f->f.smallCardinality && finalC == f.getOwner()).forEach(f->craerIndiceCampo(false, f.getOwnerIndexableField()));
				}
			}
			$("//indexes:");
			indices:{
				for (final EntityIndexModel indice : entidad.indices) {
					StringSeparator params = new StringSeparator(", ");
					for(IndexableField f : indice.camposIndice)
						params.add($($convert(f.getIndexType())) + " " + f.fieldName());
					$("public java.util.List<" + entidad.getTipo() + "> " + indice.name() + "(" + params + ")", ()->{
						$("java.util.List<com.google.appengine.api.datastore.Query.Filter> subFilters = new java.util.ArrayList<>(" + indice.camposIndice.length + ");");
						for(final IndexableField f : indice.camposIndice){
							if(f.getIndexType().isPrimitive())
								$("subFilters.add(new com.google.appengine.api.datastore.Query.FilterPredicate(\"" + f.getDBName()  + "\", com.google.appengine.api.datastore.Query.FilterOperator.EQUAL, " + f.fieldName() + "));");
							else
								$if(f.fieldName() + " != null", ()->{
								if(f.getIndexType().isEnum())
									$("subFilters.add(new com.google.appengine.api.datastore.Query.FilterPredicate(\"" + f.getDBName()  + "\", com.google.appengine.api.datastore.Query.FilterOperator.EQUAL, " + f.fieldName() + ".id));");
								else
									$("subFilters.add(new com.google.appengine.api.datastore.Query.FilterPredicate(\"" + f.getDBName()  + "\", com.google.appengine.api.datastore.Query.FilterOperator.EQUAL, " + f.fieldName() + "));");
							});
						}
						$("com.google.appengine.api.datastore.Query.Filter f = com.google.appengine.api.datastore.Query.CompositeFilterOperator.AND.of(subFilters);");
						$("com.google.appengine.api.datastore.Query q = new com.google.appengine.api.datastore.Query(ENTITY_NAME).setFilter(f);");
						$("return processQuery(q);");
					});
					
					final IndexableField lastField = indice.camposIndice[indice.camposIndice.length - 1];
					params.add($($convert(lastField.getIndexType())) + " upper" + capitalize(lastField.fieldName()));
					$("public java.util.List<" + entidad.getTipo() + "> " + indice.name() + "(" + params + ")", ()->{
						$("java.util.List<com.google.appengine.api.datastore.Query.Filter> subFilters = new java.util.ArrayList<>(" + indice.camposIndice.length + ");");
						for(final IndexableField f : indice.camposIndice){
							if(lastField == f){
								if(f.getIndexType().isPrimitive()) {
									$("subFilters.add(new com.google.appengine.api.datastore.Query.FilterPredicate(\"" + f.getDBName() + "\", com.google.appengine.api.datastore.Query.FilterOperator.GREATER_THAN_OR_EQUAL, " + f.fieldName() + "));");
									$("subFilters.add(new com.google.appengine.api.datastore.Query.FilterPredicate(\"" + f.getDBName()  + "\", com.google.appengine.api.datastore.Query.FilterOperator.LESS_THAN, upper" + capitalize(f.fieldName()) + "));");
								}else{
									$if(f.fieldName() + " != null", ()->{
										if(f.getIndexType().isEnum())
											$("subFilters.add(new com.google.appengine.api.datastore.Query.FilterPredicate(\"" + f.getDBName() + "\", com.google.appengine.api.datastore.Query.FilterOperator.GREATER_THAN_OR_EQUAL, " + f.fieldName() + ".id));");
										else
											$("subFilters.add(new com.google.appengine.api.datastore.Query.FilterPredicate(\"" + f.getDBName() + "\", com.google.appengine.api.datastore.Query.FilterOperator.GREATER_THAN_OR_EQUAL, " + f.fieldName() + "));");
									});
									$if("upper" + capitalize(f.fieldName()) + " != null", ()->{
										if(f.getIndexType().isEnum())
											$("subFilters.add(new com.google.appengine.api.datastore.Query.FilterPredicate(\"" + f.getDBName()  + "\", com.google.appengine.api.datastore.Query.FilterOperator.LESS_THAN, upper" + capitalize(f.fieldName()) + ".id));");
										else
											$("subFilters.add(new com.google.appengine.api.datastore.Query.FilterPredicate(\"" + f.getDBName()  + "\", com.google.appengine.api.datastore.Query.FilterOperator.LESS_THAN, upper" + capitalize(f.fieldName()) + "));");
									});
								}
							}else if(f.getIndexType().isPrimitive())
								$("subFilters.add(new com.google.appengine.api.datastore.Query.FilterPredicate(\"" + f.getDBName()  + "\", com.google.appengine.api.datastore.Query.FilterOperator.EQUAL, " + f.fieldName() + "));");
							else {
								$if(f.fieldName() + " != null", ()->{
									if(f.getIndexType().isEnum())
										$("subFilters.add(new com.google.appengine.api.datastore.Query.FilterPredicate(\"" + f.getDBName() + "\", com.google.appengine.api.datastore.Query.FilterOperator.EQUAL, " + f.fieldName() + ".id));");
									else
										$("subFilters.add(new com.google.appengine.api.datastore.Query.FilterPredicate(\"" + f.getDBName() + "\", com.google.appengine.api.datastore.Query.FilterOperator.EQUAL, " + f.fieldName() + "));");
								});
							}
						}
						$("com.google.appengine.api.datastore.Query.Filter f = com.google.appengine.api.datastore.Query.CompositeFilterOperator.AND.of(subFilters);");
						$("com.google.appengine.api.datastore.Query q = new com.google.appengine.api.datastore.Query(ENTITY_NAME).setFilter(f);");
						$("return processQuery(q);");
					});
				}
			}
			$("//utils:");
			getAll:if(entidad.key != null){
				$("public com.google.api.core.ApiFuture<java.util.List<" + entidad.getTipo() + ">> getAllAsync()", ()->{
					$("return ApiFutureTransform.forQuery(jcrystal.context.CrystalContext.get()."+entidad.mainDBType.getDBName()+"().doQuery(query), " + entidad.getTipo()+"::new);");
				});
				$("public com.google.api.core.ApiFuture<java.util.List<" + entidad.getTipo() + ">> getAllKeysAsync()", ()->{
					$("return ApiFutureTransform.forQuery(jcrystal.context.CrystalContext.get()."+entidad.mainDBType.getDBName()+"().doQuery(query.select(new String[0])), " + entidad.getTipo()+"::new);");
				});
				$("public java.util.List<" + entidad.getTipo() + "> getAll() throws InterruptedException, ExecutionException", ()->{
					$("return getAllAsync().get();");
				});
				$("public java.util.List<" + entidad.getTipo() + "> getAllKeys() throws InterruptedException, ExecutionException", ()->{
					$("return getAllKeysAsync().get();");
				});
			}
		});
		parent.context.output.exportFile(parent.back, this, entidad.clase.getPackageName().replace(".", "/")+"/Query"+entidad.clase.getSimpleName()+".java");
	}
	private void craerIndiceCampo(boolean paged, IndexableField field) {
		boolean isSortable = !field.type().isEnum() || parent.context.data.entidades.contains(field.type());
		Function<ComparisonOperator, String> buildQuery = operator->{
			String query = "";
			if(operator == ComparisonOperator.in)
				return query + ".whereGreaterThanOrEqualTo(\"" + field.getDBName() + "\", " + field.fieldName() + ").whereLessThan(\"" + field.getDBName() + "\", upper" + capitalize(field.fieldName()) + ")";
			String accessor = field.fieldName();
			if(field.getIndexType().isEnum())
				accessor += ".id";
			else if(field.getIndexType().isJAnnotationPresent(InternalEntityKey.class))
				accessor += ".getRawKey()";
			
			return query + "." + operator.firebaseMethod() + "(\"" + field.getDBName() + "\", " + accessor + ")";
		};
		if(paged) {
			if(!field.indexType().isUnique())
				$M(PUBLIC, "jcrystal.db.query.Page<" + entidad.getTipo() + ">", field.fieldName(), $($convert(field.getIndexType())) + " "+field.fieldName(), ()->{
					$("return new jcrystal.db.query.Page<>(Query" + entidad.getTipo() + ".this, " + buildQuery.apply(ComparisonOperator.equal) + "));");
				});
		}else {
			$M(PUBLIC, "com.google.api.core.ApiFuture<" + (field.indexType().isUnique() ? entidad.getTipo() : ("java.util.List<" + entidad.getTipo() + ">")) + ">", field.fieldName()+"Async", $($convert(field.getIndexType())) + " "+field.fieldName(), ()->{
				$("return ApiFutureTransform."+(field.indexType().isUnique()?"first":"forQuery")+"(jcrystal.context.CrystalContext.get()."+entidad.mainDBType.getDBName()+"().doQuery(query"+buildQuery.apply(ComparisonOperator.equal) + "), " + entidad.getTipo()+"::new);");
			});
			$M(PUBLIC, (field.indexType().isUnique() ? entidad.getTipo() : ("java.util.List<" + entidad.getTipo() + ">")), field.fieldName(), $($convert(field.getIndexType())) + " "+field.fieldName(), "throws InterruptedException, ExecutionException", ()->{
				$("return " + field.fieldName() + "Async(" + field.fieldName() + ").get();");
			});
			if(!paged && !field.indexType().isUnique()) {
				$M(PUBLIC, "com.google.api.core.ApiFuture<" + entidad.getTipo() + ">", field.fieldName()+"FirstAsync", $($convert(field.getIndexType()))+" "+field.fieldName(), ()->{
					$("return ApiFutureTransform.first(jcrystal.context.CrystalContext.get()."+entidad.mainDBType.getDBName()+"().doQuery(query.limit(1)"+buildQuery.apply(ComparisonOperator.equal) + "), " + entidad.getTipo()+"::new);");
				});
				$M(PUBLIC, entidad.getTipo(), field.fieldName()+"First", $($convert(field.getIndexType()))+" "+field.fieldName(), "throws InterruptedException, ExecutionException", ()->{
					$("return " + field.fieldName() + "FirstAsync(" + field.fieldName() + ").get();");
				});
			}
			if(isSortable) {
				String args = $($convert(field.getIndexType())) + " "+field.fieldName();
				$M(PUBLIC, "com.google.api.core.ApiFuture<java.util.List<" + entidad.getTipo() + ">>", field.fieldName()+"Async", "jcrystal.server.db.query.Op operator, " + args, ()->{
					$("switch(operator)",()->{
						for(ComparisonOperator operator : ComparisonOperator.valuesSingle){
							$("case " + operator.getUserName() + ":");
							$("	return ApiFutureTransform.forQuery(jcrystal.context.CrystalContext.get()."+entidad.mainDBType.getDBName()+"().doQuery(query" + buildQuery.apply(operator) + "), " + entidad.getTipo()+"::new);");
						}	
						$("default:throw new jcrystal.errors.ErrorException(\"Invalid operator for jCrystal query: \" + operator);");
					});
				});
				$M(PUBLIC, "java.util.List<" + entidad.getTipo() + ">", field.fieldName(), "jcrystal.server.db.query.Op operator, " + args, "throws InterruptedException, ExecutionException", ()->{
					$("return " + field.fieldName() + "Async(operator, " + field.fieldName() + ").get();");
				});
				args += ", "+$($convert(field.getIndexType())) + " upper"+capitalize(field.fieldName());
				$M(PUBLIC, "com.google.api.core.ApiFuture<java.util.List<" + entidad.getTipo() + ">>", field.fieldName() + "Async", args, ()->{
					$("return ApiFutureTransform.forQuery(jcrystal.context.CrystalContext.get()."+entidad.mainDBType.getDBName()+"().doQuery(query" + buildQuery.apply(ComparisonOperator.in) + "), " + entidad.getTipo()+"::new);");
				});
				$M(PUBLIC, "java.util.List<" + entidad.getTipo() + ">", field.fieldName(), args, "throws InterruptedException, ExecutionException", ()->{
					$("return " + field.fieldName() + "Async(" + field.fieldName() + ", upper" + capitalize(field.fieldName()) + ").get();");
				});
			}
		}
	}
}

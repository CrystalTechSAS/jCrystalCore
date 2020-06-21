package jcrystal.server.databases.google.datastore;

import static java.lang.reflect.Modifier.PUBLIC;
import static jcrystal.utils.StringUtils.capitalize;

import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.SortDirection;

import jcrystal.annotations.internal.model.db.InternalEntityKey;
import jcrystal.entity.types.Email;
import jcrystal.model.server.db.EntityClass;
import jcrystal.model.server.db.IndexableField;
import jcrystal.model.server.db.EntityIndexModel;
import jcrystal.reflection.annotations.IndexType;
import jcrystal.types.IJType;
import jcrystal.types.JClass;
import jcrystal.types.utils.GlobalTypes;
import jcrystal.utils.StringSeparator;
import jcrystal.utils.StringUtils;
import jcrystal.utils.langAndPlats.JavaCode;

public class QueryClassGenerator extends JavaCode{
	MainEntityGenerator parent;
	EntityClass entidad;
	public QueryClassGenerator(MainEntityGenerator parent) {
		this.parent = parent;
		this.entidad = parent.entidad;
	}
	private String getAccessor(String prefix, IndexableField f) {
		String fieldName = prefix != null ? prefix + StringUtils.capitalize(f.fieldName()) : f.fieldName();
		if(f.getIndexType().isEnum()) {
			IJType idType = ((JClass)f.getIndexType()).enumData.propiedades.get("id");
			if(idType == null)
				return fieldName + ".name()";
			else
				return fieldName + ".id";
		}
		else if(f.isJAnnotationPresent(Email.class))
			return "new com.google.appengine.api.datastore.Email(" + fieldName + ")";
		else
			return fieldName;
	}
	private String getAccessor(IndexableField f) {
		return getAccessor(null, f);
	}
	public void generate() {
		if(parent.entidad.entidad.internal())
			return;
		$("package "+entidad.clase.getPackageName()+";");
		$("import static " + entidad.clase.name()+".ENTITY_NAME;");
		$("public class Query" + entidad.getTipo() + " extends jcrystal.db.datastore.query.AbsBaseHelper<Query" + entidad.getTipo() + ", "+entidad.getTipo()+">", ()->{
			$("//indexed fields:");
			$("protected Query" + entidad.getTipo() +"()",()->{
				$("super(jcrystal.context.CrystalContext.get()."+entidad.mainDBType.getDBName()+"());");
				$("this.ancestor = null;");
			});
			$("protected Query" + entidad.getTipo() +"(com.google.appengine.api.datastore.Key ancestor)",()->{
				$("super(jcrystal.context.CrystalContext.get()."+entidad.mainDBType.getDBName()+"(), ancestor);");
			});
			entidad.iterateAncestorKeys(keys->{
				$("protected Query" + entidad.getTipo() + "(" + parent.KeyConverters.keyType(keys) + ")",()->{
					$("this(" + entidad.getTipo()+".Key.createRawParentKey(" + parent.KeyConverters.keyNames(keys) + "));");
				});
			});
			$("@Override public Query" + entidad.getTipo() + " create(){return new Query"+entidad.getTipo()+"(ancestor);}");
			$("@Override public "+entidad.getTipo()+" create(com.google.appengine.api.datastore.Entity entidad)",()->{
				if(entidad.campoSelector != null)
					$("return getBySelector(entidad);");
				else
					$("return new " + entidad.getTipo() + "(entidad);");
			});
			getAllBy:{
				for(EntityClass c = entidad; c != null; c = c.padre) {
					final EntityClass finalC = c;
					c.key.getLlaves().stream().filter(f->f.keyData.indexAsProperty() && f.indexType() != IndexType.NONE).forEach(f->craerIndiceCampo(false, f));
					c.properties.stream().filter(f->f.indexType() != IndexType.NONE).forEach(f->craerIndiceCampo(false, f));
					c.manyToOneRelations.stream().forEach(f->craerIndiceCampo(false, f.getOwnerIndexableField()));
					c.ownedOneToOneRelations.stream().forEach(f->craerIndiceCampo(false, f.getOwnerIndexableField()));
					c.manyToManyRelations.stream().filter(f->f.smallCardinality && finalC == f.getOwner()).forEach(f->craerIndiceCampo(false, f.getOwnerIndexableField()));
				}
			}
			$("//indexes:");
			indices:{
				for (final EntityIndexModel indice : entidad.indices)
					craerIndice(false, indice);
			}
			$("//utils:");
			getAll:if(entidad.key != null){
				$("public java.util.List<" + entidad.getTipo() + "> getAll()", ()->{
					$("return processQuery(createQuery(ENTITY_NAME));");
				});
				$("public java.util.List<" + entidad.getTipo() + "> getAllKeys()", ()->{
					$("return processQuery(createQuery(ENTITY_NAME).setKeysOnly());");
				});
			}
			$("public Page" + entidad.getTipo() + " paged(int size)",()->{
				$("return limit(size).new Page" + entidad.getTipo()+"();");
			});
			$("public class Page" + entidad.getTipo(),()->{
				$("public jcrystal.db.query.Page<" + entidad.getTipo() + "> getAll()", ()->{
					$("return new jcrystal.db.query.Page<>(Query" + entidad.getTipo() + ".this, createQuery(ENTITY_NAME));");
				});
				for (final EntityIndexModel indice : entidad.indices)
					craerIndice(true, indice);
				for(EntityClass c = entidad; c != null; c = c.padre) {
					final EntityClass finalC = c;
					c.key.getLlaves().stream().filter(f->!f.keyData.indexAsProperty()).filter(f->f.indexType() != IndexType.NONE).forEach(f->craerIndiceCampo(true, f));
					c.properties.stream().filter(f->f.indexType() != IndexType.NONE).forEach(f->craerIndiceCampo(true, f));
					c.manyToOneRelations.stream().forEach(f->craerIndiceCampo(true, f.getOwnerIndexableField()));
					c.ownedOneToOneRelations.stream().forEach(f->craerIndiceCampo(true, f.getOwnerIndexableField()));
					c.manyToManyRelations.stream().filter(f->f.smallCardinality && finalC == f.getOwner()).forEach(f->craerIndiceCampo(true, f.getOwnerIndexableField()));
				}
			});
		});
		parent.context.output.exportFile(parent.back, this, entidad.clase.getPackageName().replace(".", "/")+"/Query"+entidad.clase.getSimpleName()+".java");
	}
	private IJType $convertType(IJType type) {
		if(type.isJAnnotationPresent(InternalEntityKey.class))
			return GlobalTypes.Google.DataStore.KEY;
		return $convert(type);
	}
	private void craerIndice(boolean paged, EntityIndexModel indice) {
		StringSeparator params = new StringSeparator(", ");
		StringSeparator values = new StringSeparator(", ");
		final IndexableField lastField = indice.camposIndice[indice.camposIndice.length - 1];
		boolean isSortable = !lastField.type().isEnum() || parent.context.data.entidades.contains(lastField.type());
		if(paged && !isSortable)
			return;
		String returnType = (paged ? "jcrystal.db.query.Page<" : "java.util.List<") + entidad.getTipo() + ">";
		for(IndexableField f : indice.camposIndice) {
			if(lastField != f) {
				params.add($($convert(f.getIndexType())) + " " + f.fieldName());
				values.add(f.fieldName());
			}
		}
		if(isSortable) {
			$("public " + returnType + " " + indice.name() + "Sorted(" + params + ")", ()->{
				$("return " + indice.name() + "Sorted(" + values + ", com.google.appengine.api.datastore.Query.SortDirection.ASCENDING);");
			});
			$("public " + returnType + " " + indice.name() + "Sorted(" + params + ", com.google.appengine.api.datastore.Query.SortDirection direction)", ()->{
				if(indice.camposIndice.length > 2) {
					$("java.util.List<com.google.appengine.api.datastore.Query.Filter> subFilters = new java.util.ArrayList<>(" + indice.camposIndice.length + ");");
					for(final IndexableField f : indice.camposIndice){
						if(lastField != f)
							$if(!f.getIndexType().isPrimitive(), f.fieldName() + " != null", ()->{
								$("subFilters.add(new com.google.appengine.api.datastore.Query.FilterPredicate(\"" + f.getDBName() + "\", com.google.appengine.api.datastore.Query.FilterOperator.EQUAL, " + getAccessor(f) + "));");
							});
					}
					$("com.google.appengine.api.datastore.Query.Filter f = com.google.appengine.api.datastore.Query.CompositeFilterOperator.AND.of(subFilters);");
					$("com.google.appengine.api.datastore.Query q = new com.google.appengine.api.datastore.Query(ENTITY_NAME).setFilter(f).addSort(\""+lastField.getDBName()+"\", direction);");
				}else {
					IndexableField f = indice.camposIndice[0];
					String filter = "new com.google.appengine.api.datastore.Query.FilterPredicate(\"" + f.getDBName() + "\", com.google.appengine.api.datastore.Query.FilterOperator.EQUAL, " + getAccessor(f) + ")";
					$("com.google.appengine.api.datastore.Query q = new com.google.appengine.api.datastore.Query(ENTITY_NAME).setFilter(" + filter + ").addSort(\""+lastField.getDBName()+"\", direction);");
				}
				if(paged)
					$("return new jcrystal.db.query.Page<>(Query" + entidad.getTipo() + ".this, q);");
				else
					$("return processQuery(q);");
			});
		}
		params.add($($convert(lastField.getIndexType())) + " " + lastField.fieldName());
		values.add(lastField.fieldName());
		$("public " + returnType + " " + indice.name() + "(" + params + ", "+FilterOperator.class.getName().replace("$", ".")+" operator)", ()->{
			crearCuerpoIndice(indice, null, paged);
		});
		$("public " + returnType + " " + indice.name() + "(" + params + ")", ()->{
			$("return " + indice.name() + "(" + values.add(FilterOperator.class.getName().replace("$", ".")+".EQUAL") + ");");
		});
		if(isSortable) {
			params.add($($convert(lastField.getIndexType())) + " upper" + capitalize(lastField.fieldName()));
			$("public " + returnType + " " + indice.name() + "(" + params + ")", ()->{
				crearCuerpoIndice(indice, FilterOperator.IN, paged);
			}); 
		}
	}
	private void crearCuerpoIndice(EntityIndexModel indice, FilterOperator lastOp, boolean paged) {
		final IndexableField lastField = indice.camposIndice[indice.camposIndice.length - 1];
		$("java.util.List<com.google.appengine.api.datastore.Query.Filter> subFilters = new java.util.ArrayList<>(" + indice.camposIndice.length + ");");
		for(final IndexableField f : indice.camposIndice){
			if(lastField == f){
				if(lastOp == FilterOperator.IN) {
					$if(!f.getIndexType().isPrimitive(), f.fieldName() + " != null", ()->{
						$("subFilters.add(new com.google.appengine.api.datastore.Query.FilterPredicate(\"" + f.getDBName() + "\", com.google.appengine.api.datastore.Query.FilterOperator.GREATER_THAN_OR_EQUAL, " + getAccessor(f) + "));");
					});
					$if(!f.getIndexType().isPrimitive(), "upper" + capitalize(f.fieldName()) + " != null", ()->{
						$("subFilters.add(new com.google.appengine.api.datastore.Query.FilterPredicate(\"" + f.getDBName()  + "\", com.google.appengine.api.datastore.Query.FilterOperator.LESS_THAN, " + getAccessor("upper", f) + "));");
					});
				}else if(lastOp == null){
					$if(!f.getIndexType().isPrimitive(), f.fieldName() + " != null", ()->{
						$("subFilters.add(new com.google.appengine.api.datastore.Query.FilterPredicate(\"" + f.getDBName() + "\", operator, " + getAccessor(f) + "));");
					});
				}
			}else
				$if(!f.getIndexType().isPrimitive(), f.fieldName() + " != null", ()->{
					$("subFilters.add(new com.google.appengine.api.datastore.Query.FilterPredicate(\"" + f.getDBName() + "\", com.google.appengine.api.datastore.Query.FilterOperator.EQUAL, " + getAccessor(f) + "));");
				});
		}
		$("com.google.appengine.api.datastore.Query.Filter f = com.google.appengine.api.datastore.Query.CompositeFilterOperator.AND.of(subFilters);");
		$("com.google.appengine.api.datastore.Query q = new com.google.appengine.api.datastore.Query(ENTITY_NAME).setFilter(f);");
		if(lastOp == null) {
			$if("operator == com.google.appengine.api.datastore.Query.FilterOperator.GREATER_THAN || operator == com.google.appengine.api.datastore.Query.FilterOperator.GREATER_THAN_OR_EQUAL", ()->{
				$("q.addSort(\"" + lastField.getDBName() + "\", com.google.appengine.api.datastore.Query.SortDirection.ASCENDING);");
			});
			$if("operator == com.google.appengine.api.datastore.Query.FilterOperator.LESS_THAN || operator == com.google.appengine.api.datastore.Query.FilterOperator.LESS_THAN_OR_EQUAL", ()->{
				$("q.addSort(\"" + lastField.getDBName() + "\", com.google.appengine.api.datastore.Query.SortDirection.DESCENDING);");
			});
		}
		if(paged)
			$("return new jcrystal.db.query.Page<>(Query" + entidad.getTipo() + ".this, q);");
		else
			$("return processQuery(q);");
	}
	private void craerIndiceCampo(boolean paged, IndexableField field) {
		boolean isSortable = !field.type().isEnum() || parent.context.data.entidades.contains(field.type());
		Function<FilterOperator, String> buildQuery = operator->{
			String query = "createQuery(ENTITY_NAME)";
			if(operator == null)
				return query + ".setFilter(new com.google.appengine.api.datastore.Query.FilterPredicate(\"" + field.getDBName() + "\", operator.datastoreOp, " + getAccessor(field) + "))";
			if(operator == FilterOperator.IN)
				return query + ".setFilter(new com.google.appengine.api.datastore.Query.CompositeFilter(com.google.appengine.api.datastore.Query.CompositeFilterOperator.AND, java.util.Arrays.asList((com.google.appengine.api.datastore.Query.Filter)new com.google.appengine.api.datastore.Query.FilterPredicate(\"" + field.getDBName() + "\", com.google.appengine.api.datastore.Query.FilterOperator.GREATER_THAN_OR_EQUAL, " + field.fieldName() + "),new com.google.appengine.api.datastore.Query.FilterPredicate(\"" + getAccessor(field) + "\", com.google.appengine.api.datastore.Query.FilterOperator.LESS_THAN, " +getAccessor("upper", field) + "))))";
			else
				return query + ".setFilter(new com.google.appengine.api.datastore.Query.FilterPredicate(\"" + field.getDBName() + "\", com.google.appengine.api.datastore.Query.FilterOperator." + operator.name() + ", " + getAccessor(field) + "))";
		};
		if(paged) {
			if(!field.indexType().isUnique())
				$M(PUBLIC, "jcrystal.db.query.Page<" + entidad.getTipo() + ">", field.fieldName(), $($convert(field.getIndexType())) + " "+field.fieldName(), ()->{
					$("return new jcrystal.db.query.Page<>(Query" + entidad.getTipo() + ".this, " + buildQuery.apply(FilterOperator.EQUAL) + ");");
				});
		}else {
			$M(PUBLIC, field.indexType().isUnique() ? entidad.getTipo() : ("java.util.List<" + entidad.getTipo() + ">"), field.fieldName(), $($convertType(field.getIndexType())) + " "+field.fieldName(), ()->{
				String methodName = field.indexType().isUnique() ? "processQueryUnique" : "processQuery";
				$("return "+methodName+"("+buildQuery.apply(FilterOperator.EQUAL)+");");
			});			
			if(!paged && !field.indexType().isUnique())
				$M(PUBLIC, entidad.getTipo(), field.fieldName()+"First", $($convertType(field.getIndexType()))+" "+field.fieldName(), ()->{
					$("return processQueryUnique(" + buildQuery.apply(FilterOperator.EQUAL) + ");");
				});
			if(isSortable) {
				String args = $($convertType(field.getIndexType())) + " "+field.fieldName();
				$M(PUBLIC, "java.util.List<" + entidad.getTipo() + ">", field.fieldName(), "jcrystal.server.db.query.Op operator, " + args, ()->{
					$("return processQuery(" + buildQuery.apply(null) + ");");
				});
				args += ", "+$($convertType(field.getIndexType())) + " upper"+capitalize(field.fieldName());
				$M(PUBLIC, "java.util.List<" + entidad.getTipo() + ">", field.fieldName(), args, ()->{
					$("return processQuery(" + buildQuery.apply(FilterOperator.IN) + ");");
				});
			}
		}
	}
}

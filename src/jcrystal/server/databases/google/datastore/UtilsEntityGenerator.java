package jcrystal.server.databases.google.datastore;

import jcrystal.server.databases.AbsUtilsEntityGenerator;

public class UtilsEntityGenerator extends AbsUtilsEntityGenerator{
	MainEntityGenerator parent;
	public UtilsEntityGenerator(MainEntityGenerator parent) {
		super(parent);
		this.parent = parent;
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
		if(entidad.key != null)
			$("class Batch" + entidad.getTipo(), ()->{
				if(entidad.key.isSimple()) {
					String keyType = entidad.key.getKeyTypes(this);
					$("public static <T> java.util.List<"+entidad.getTipo()+"> get(java.util.stream.Stream<T> it, java.util.function.Function<T, "+keyType+"> mapper)",()->{
						$("return get(it.map(mapper).filter(f->f != null).collect(java.util.stream.Collectors.toList()));");
					});
					$("public static <T> java.util.List<"+entidad.getTipo()+"> get(java.util.List<T> it, java.util.function.Function<T, "+keyType+"> mapper)",()->{
						$("return get(it.stream(), mapper);");
					});
					$("public static java.util.List<"+entidad.getTipo()+"> get(java.util.stream.Stream<"+keyType+"> it)",()->{
						$if("it == null","return new java.util.ArrayList<>();");
						$("return get(it.filter(f->f != null).collect(java.util.stream.Collectors.toList()));");
					});
					$("public static java.util.List<"+entidad.getTipo()+"> get(java.util.Collection<"+keyType+"> it)",()->{
						$if("it == null || it.isEmpty()", "return new java.util.ArrayList<>();");
						$("jcrystal.context.CrystalContext $ctx = jcrystal.context.CrystalContext.get();");
						$("return $ctx."+entidad.mainDBType.getDBName()+"().service.get($ctx."+entidad.mainDBType.getDBName()+"().getTxn(), new jcrystal.utils.IterableTransform<"+keyType+", com.google.appengine.api.datastore.Key>(it){ @Override public com.google.appengine.api.datastore.Key transform("+keyType+" v) { return "+entidad.getTipo()+".Key.createRawKey(v); }}).values().stream().map(ent->new "+entidad.getTipo()+"(ent)).collect(java.util.stream.Collectors.toList());");
					});
					$("public static java.util.List<" + entidad.getTipo() + "> getFromKeys(Iterable<com.google.appengine.api.datastore.Key> keys)", ()->{
						$("java.util.List<" + entidad.getTipo() + "> ret = new java.util.ArrayList<>();");
						$("for(com.google.appengine.api.datastore.Entity ent : jcrystal.context.CrystalContext.get()."+entidad.mainDBType.getDBName()+"().service.get(null, keys).values())ret.add(new " + entidad.getTipo()+"(ent));");
						$("return ret;");
					});
					
				}else {
					$("public static java.util.List<"+entidad.getTipo()+"> get(java.lang.Iterable<com.google.appengine.api.datastore.Key> it)",()->{
						$("jcrystal.context.CrystalContext $ctx = jcrystal.context.CrystalContext.get();");
						$("return $ctx."+entidad.mainDBType.getDBName()+"().service.get($ctx."+entidad.mainDBType.getDBName()+"().getTxn(), it).values().stream().map(ent->new "+entidad.getTipo()+"(ent)).collect(java.util.stream.Collectors.toList());");
					});
				}
				$("public static void put(java.lang.Iterable<"+entidad.getTipo()+"> it)",()->{
					$("jcrystal.context.CrystalContext $ctx = jcrystal.context.CrystalContext.get();");
					if(context.input.SERVER.DEBUG.ENTITY_CHECKS)
						$("$ctx."+entidad.mainDBType.getDBName()+"().service.put(null, new jcrystal.utils.IterableTransform<"+entidad.getTipo()+", com.google.appengine.api.datastore.Entity>(it){ @Override public com.google.appengine.api.datastore.Entity transform("+entidad.getTipo()+" v) { v.DEBUG_CHANGES = false; return v.rawEntity; }});");
					else
						$("$ctx."+entidad.mainDBType.getDBName()+"().service.put(null, new jcrystal.utils.IterableTransform<"+entidad.getTipo()+", com.google.appengine.api.datastore.Entity>(it){ @Override public com.google.appengine.api.datastore.Entity transform("+entidad.getTipo()+" v) { return v.rawEntity; }});");
					});
				$("public static void delete(java.lang.Iterable<"+entidad.getTipo()+"> it)",()->{
					$("jcrystal.context.CrystalContext $ctx = jcrystal.context.CrystalContext.get();");
					$("$ctx."+entidad.mainDBType.getDBName()+"().service.delete(null, new jcrystal.utils.IterableTransform<"+entidad.getTipo()+", com.google.appengine.api.datastore.Key>(it){ @Override public com.google.appengine.api.datastore.Key transform("+entidad.getTipo()+" v) { return v.rawEntity.getKey(); }});");
				});
			});
	}
}


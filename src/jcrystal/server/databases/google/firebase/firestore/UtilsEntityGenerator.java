package jcrystal.server.databases.google.firebase.firestore;

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
		return new String[] {"jcrystal.db.firestore.ApiFutureTransform", "java.util.concurrent.ExecutionException"};
	}
	private void generarBatchClass(){
		if(entidad.key != null)
			$("class Batch" + entidad.getTipo(), ()->{
				if(entidad.key.isSimple()) {
					String keyType = entidad.key.getKeyTypes(this);
					$("public static <T> com.google.api.core.ApiFuture<java.util.List<"+entidad.getTipo()+">> getAsync(java.util.stream.Stream<T> it, java.util.function.Function<T, "+keyType+"> mapper)",()->{
						$("return getAsync(it.map(mapper).filter(f->f != null));");
					});
					$("public static <T> com.google.api.core.ApiFuture<java.util.List<"+entidad.getTipo()+">> getAsync(java.util.List<T> it, java.util.function.Function<T, "+keyType+"> mapper)",()->{
						$("return getAsync(it.stream(), mapper);");
					});
					$("public static com.google.api.core.ApiFuture<java.util.List<"+entidad.getTipo()+">> getAsync(java.util.stream.Stream<"+keyType+"> it)",()->{
						$if("it == null", "return com.google.api.core.ApiFutures.immediateFuture(new java.util.ArrayList<>());");
						$("return getAsync(it.map(key->"+entidad.getTipo()+".Key.createRawPath(key)).collect(java.util.stream.Collectors.toList()));");
					});
					$("public static com.google.api.core.ApiFuture<java.util.List<"+entidad.getTipo()+">> getFromKeysAsync(java.util.Collection<"+keyType+"> it)",()->{
						$if("it == null || it.isEmpty()", "return com.google.api.core.ApiFutures.immediateFuture(new java.util.ArrayList<>());");
						$("return getAsync(it.stream().map(key->"+entidad.getTipo()+".Key.createRawPath(key)).collect(java.util.stream.Collectors.toList()));");
					});
					$("public static <T> java.util.List<"+entidad.getTipo()+"> get(java.util.stream.Stream<T> it, java.util.function.Function<T, "+keyType+"> mapper)throws InterruptedException, ExecutionException",()->{
						$("return getAsync(it, mapper).get();");
					});
					$("public static <T> java.util.List<"+entidad.getTipo()+"> get(java.util.List<T> it, java.util.function.Function<T, "+keyType+"> mapper)throws InterruptedException, ExecutionException",()->{
						$("return getAsync(it, mapper).get();");
					});
					$("public static java.util.List<"+entidad.getTipo()+"> get(java.util.stream.Stream<"+keyType+"> it)throws InterruptedException, ExecutionException",()->{
						$("return getAsync(it).get();");
					});
					$("public static java.util.List<"+entidad.getTipo()+"> getFromKeys(java.util.Collection<"+keyType+"> it)throws InterruptedException, ExecutionException",()->{
						$("return getFromKeysAsync(it).get();");
					});
				}
				$("public static com.google.api.core.ApiFuture<java.util.List<"+entidad.getTipo()+">> getAsync(java.util.List<String> it)",()->{
					$("jcrystal.context.CrystalContext $ctx = jcrystal.context.CrystalContext.get();");
					$("return ApiFutureTransform.forList($ctx."+entidad.mainDBType.getDBName()+"().getAll(it), "+entidad.getTipo()+"::new);");
				});
				$("public static java.util.List<"+entidad.getTipo()+"> get(java.util.List<String> it)throws InterruptedException, ExecutionException",()->{
					$("return getAsync(it).get();");
				});
				$("public static com.google.api.core.ApiFuture<java.util.List<com.google.cloud.firestore.WriteResult>> putAsync(java.util.List<"+entidad.getTipo()+"> it)",()->{
					$("jcrystal.context.CrystalContext $ctx = jcrystal.context.CrystalContext.get();");
					$("return $ctx."+entidad.mainDBType.getDBName()+"().createBatch(it.stream().map(f->(jcrystal.server.Entity."+entidad.mainDBType.getDBName()+")f));");
				});
				$("public static void put(java.util.List<"+entidad.getTipo()+"> it)throws InterruptedException, ExecutionException",()->{
					$("putAsync(it).get();");
				});
				$("public static com.google.api.core.ApiFuture<java.util.List<com.google.cloud.firestore.WriteResult>> deleteAsync(java.util.List<"+entidad.getTipo()+"> it)",()->{
					$("jcrystal.context.CrystalContext $ctx = jcrystal.context.CrystalContext.get();");
					$("return $ctx."+entidad.mainDBType.getDBName()+"().deleteBatch(it.stream().map(f->"+entidad.getTipo()+".Key.keyToPath(f.getRawKey(), 0)));");
				});
				$("public static void delete(java.util.List<"+entidad.getTipo()+"> it)throws InterruptedException, ExecutionException",()->{
					$("deleteAsync(it).get();");
				});
			});
	}
}


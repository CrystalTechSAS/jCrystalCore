package jcrystal.clients.android;

import java.io.File;
import java.util.TreeSet;

import jcrystal.clients.AbsClientGenerator;
import jcrystal.json.JsonLevel;
import jcrystal.types.JClass;
import jcrystal.utils.langAndPlats.AbsCodeBlock;
import jcrystal.utils.langAndPlats.JavaCode;
import jcrystal.utils.langAndPlats.AbsCodeBlock.B;

public class GeneradorStorage {
	public void crearCodigoAlmacenamiento(AbsClientGenerator<?> c, String paquete, final JClass clase, final TreeSet<JsonLevel> levels){
		new JavaCode() {
			{
				$("package " + paquete + ";");
				$("import " + AndroidClient.paqueteEntidades + ".enums.*;");
				$("import " + AndroidClient.paqueteMobile + ".*;");
				$("import " + AndroidClient.netPackage+".DBUtils;");
				$("public class DB" + clase.getSimpleName(), () -> {
					$("public static boolean store(String key, " + clase.getSimpleName() + " value)", () -> {
						$("return store(null, key, value);");
					});
					$("public static boolean store(String key, java.util.List<" + clase.getSimpleName() + "> values)", () -> {
						$("return store(null, key, values);");
					});
					$("public static " + clase.getSimpleName() + " retrieve(String key)", () -> {
						$("return retrieve(null, key);");
					});
					crearCodigoStore(this, clase, null);
					if (levels != null)
						for (JsonLevel level : levels)
							crearCodigoStore(this, clase, level);

					$("public static boolean appendToList(String partKey, String key, " + clase.getSimpleName() + " value)", () -> {
						$("return DBUtils.appendToList(partKey, key, value);");
					});
					$("public static " + clase.getSimpleName() + " retrieve(String partKey, String key)", () -> {
						$("return DBUtils.retrieve(partKey, key, "+clase.getSimpleName()+"::new);");
					});
					$("public static <T> java.util.List<T> retrieveList(String key){return retrieveList(null, key);}");
					$("public static <T> java.util.List<T> retrieveList(String partKey, String key)", () -> {
						$("return DBUtils.retrieveList(partKey, key, "+clase.getSimpleName()+"::new);");
					});
					$("public static void delete(String partKey, String key)", () -> {
						$("DBUtils.delete(partKey, key);");
					});
					$("public static void deleteList(String partKey, String key)", () -> {
						$("DBUtils.deleteList(partKey, key);");
					});
					$("public static void delete(String key)", () -> {
						$("DBUtils.delete(key);");
					});
				});
				c.exportFile(this, paquete.replace(".", File.separator) + File.separator + "DB" + clase.getSimpleName() + ".java");
			}
		};
		// CREAR LA CLASE
		
	}
	private void crearCodigoStore(AbsCodeBlock code, final JClass clase, final JsonLevel level) {
		code.new B() {
			{
				if (level == null)
					$("public static boolean store(String partKey, String key, " + clase.getSimpleName() + " value)", () -> {
						$("return DBUtils.store(partKey, key, value);");
					});
				else
					$("public static boolean store(String partKey, String key, " + clase.getSimpleName() + level.baseName() + " value)", () -> {
						$("return store(partKey, key, (" + clase.getSimpleName() + ")value);");
					});
				$("public static boolean store" + (level == null ? "" : level.baseName()) + "(String partKey, String key, java.util.List<" + clase.getSimpleName() + (level == null ? "" : level.baseName()) + "> values)", () -> {
					$("return DBUtils.store(partKey, key, values);");
				});
			}
		};
	}

	
}

package jcrystal.clients.flutter;

import java.io.File;
import java.util.TreeSet;

import jcrystal.clients.AbsClientGenerator;
import jcrystal.json.JsonLevel;
import jcrystal.types.JClass;
import jcrystal.utils.langAndPlats.AbsCodeBlock;
import jcrystal.utils.langAndPlats.JavaCode;
import jcrystal.utils.langAndPlats.AbsCodeBlock.B;
import jcrystal.utils.langAndPlats.DartCode;

public class GeneradorStorage {
	public void crearCodigoAlmacenamiento(AbsClientGenerator<?> c, String paquete, final JClass clase, final TreeSet<JsonLevel> levels){
		new DartCode() {{
			$("import './"+clase.getSimpleName()+".dart';");
			$("import '../../DBUtils.dart';");
			$("class DB" + clase.getSimpleName(), () -> {
				
				crearCodigoStore(this, clase, null);
				if (levels != null)
					for (JsonLevel level : levels)
						crearCodigoStore(this, clase, level);

				$("static Future<bool> appendToList(String partKey, String key, " + clase.getSimpleName() + " value)async", () -> {
					$("return DBUtils.appendToList(partKey, key, value);");
				});
				$("static Future<" + clase.getSimpleName() + "> retrieve(String partKey, String key)async", () -> {
					$("return DBUtils.retrieve(partKey, key, (k){return "+clase.getSimpleName()+".fromJson(k);});");
				});
				$("static Future<List<"+clase.getSimpleName()+">> retrieveList(String partKey, String key)async", () -> {
					$("return DBUtils.retrieveList(partKey, key, (k){return "+clase.getSimpleName()+".fromJson(k);});");
				});
				$("static void delete(String partKey, String key)async", () -> {
					$("DBUtils.delete(partKey, 'V'+key);");
				});
				$("static void deleteList(String partKey, String key)async", () -> {
					$("DBUtils.delete(partKey, 'L'+key);");
				});
			});
			c.exportFile(this, paquete.replace(".", File.separator) + File.separator + "DB" + clase.getSimpleName() + ".dart");
		}};
		// CREAR LA CLASE
		
	}
	private void crearCodigoStore(AbsCodeBlock code, final JClass clase, final JsonLevel level) {
		code.new B() {
			{
				if (level == null)
					$("static Future<bool> store(String key, " + clase.getSimpleName() + " value, [String partKey])async", () -> {
						$("return DBUtils.store(partKey, key, value);");
					});
				else
					$("static Future<bool> store(String key, " + clase.getSimpleName() + level.baseName() + " value, [String partKey])async", () -> {
						$("return store(partKey, key, (" + clase.getSimpleName() + ")value);");
					});
				$("static Future<bool> storeList" + (level == null ? "" : level.baseName()) + "(String key, List<" + clase.getSimpleName() + (level == null ? "" : level.baseName()) + "> values, [String partKey])async", () -> {
					$("return DBUtils.storeList(partKey, key, values);");
				});
			}
		};
	}

	
}

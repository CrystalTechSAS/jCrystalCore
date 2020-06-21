package jcrystal.clients.jsweet;

import static jcrystal.clients.jsweet.WebClientJSweet.paqueteDates;
import static jcrystal.clients.jsweet.WebClientJSweet.paqueteEntidades;
import static jcrystal.clients.jsweet.WebClientJSweet.paqueteJS;
import static jcrystal.clients.jsweet.WebClientJSweet.paquetePadre;

import java.io.File;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import jcrystal.clients.AbsEntityGenerator;
import jcrystal.clients.ClientGeneratorDescriptor;
import jcrystal.json.JsonLevel;
import jcrystal.model.server.db.EntityField;
import jcrystal.model.server.db.EntityClass;
import jcrystal.types.JVariable;
import jcrystal.reflection.annotations.LoginResultClass;
import jcrystal.reflection.annotations.Selector;
import jcrystal.utils.ListUtils;
import jcrystal.utils.StringSeparator;
import jcrystal.utils.langAndPlats.JavaCode;
public class GeneradorEntidad extends AbsEntityGenerator<WebClientJSweet>{
	private ClientGeneratorDescriptor<?> descriptor;
	public GeneradorEntidad(WebClientJSweet client) {
		super(client);
		this.descriptor = client.descriptor;
	}
	private void crearInterfaz(final EntityClass entidad, JsonLevel level){
		final JavaCode cliente = new JavaCode(){{
				$("package "+paqueteEntidades+";");
				$("import "+paqueteJS+".*;");
				$("import "+paqueteDates+".*;");
				$("public interface " + entidad.clase.getSimpleName()+level.baseName(), ()->{
					for(EntityField f : descriptor.getFields(entidad).get(level))
						$($convert(f.type())+" " + f.name() + "();");
				});
		}};
		
		client.exportFile(cliente, paqueteEntidades.replace(".", File.separator) + File.separator + entidad.clase.getSimpleName()+level.baseName() + ".java");
	}
	@Override
	public void generateEntity(final EntityClass entidad, final TreeSet<JsonLevel> levels){
		StringSeparator interfaces = new StringSeparator(',');
		for(JsonLevel level : levels) {
			crearInterfaz(entidad, level);
			interfaces.add(entidad.clase.getSimpleName()+level.baseName());
		}
		//CREAR LA CLASE
		final JavaCode cliente = new JavaCode(){{
				$("package "+paqueteEntidades+";");
				$("import "+paqueteJS+".*;");
				$("import "+paqueteEntidades+".enums.*;");
				$("import "+WebClientJSweet.paqueteDates+".*;");
				$("import static "+paquetePadre+".JSONUtils.*;");
				String extras = interfaces.isEmpty()?"":(" implements " + interfaces);
				if(entidad.padre != null && entidad.padre.campoSelector != null)
					extras = " extends " + entidad.padre.name() + extras;
				$("public class " + entidad.clase.getSimpleName() + extras, ()->{
					final List<EntityField> definitiveFields = ListUtils.join(descriptor.getFields(entidad).get(null), new ArrayList<EntityField>(descriptor.getFields(entidad).get(levels.last())));
					for(final EntityField f : definitiveFields){
						$("public "+$convert(f.type())+" " + f.name() + ";");
					}
					
					if(entidad.campoSelector != null){
						$("private static " + entidad.getTipo() + " getBySelector(org.json.JSONObject json)throws org.json.JSONException", ()->{
							String nombre =  entidad.campoSelector.dbName;
							
							$($convert(entidad.campoSelector.type()) + " selector = " + $convert(entidad.campoSelector.type())+".fromId(json.getInt(\"" + nombre + "\"));");
							for(EntityClass h : entidad.hijos)if(h.clase.isAnnotationPresent(Selector.class)){
								$("if(selector == " + h.clase.getAnnotation(Selector.class).valor()+")return new " + h.clase.getSimpleName() + "(json);");
							}
							$("return new " + entidad.clase.getSimpleName()+"(json);");
						});
					}
					if(entidad.clase.isAnnotationPresent(LoginResultClass.class))
						throw new NullPointerException("Unssuported on jsweet");
				});
		}};
		
		client.exportFile(cliente, paqueteEntidades.replace(".", File.separator) + File.separator + entidad.clase.getSimpleName() + ".java");
	}
	@Override
	public void generateEntityKey(EntityClass entidad) {
		// TODO Generate entity key
	}
}

package jcrystal.clients.ios;

import static jcrystal.clients.ios.SwiftClient.paqueteEntidades;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jcrystal.clients.AbsEntityGenerator;
import jcrystal.clients.ClientGeneratorDescriptor;
import jcrystal.datetime.DateType;
import jcrystal.json.JsonLevel;
import jcrystal.model.server.db.EntityField;
import jcrystal.model.server.db.EntityClass;
import jcrystal.reflection.annotations.EntityProperty;
import jcrystal.reflection.annotations.LoginResultClass;
import jcrystal.reflection.annotations.entities.RelMto1;
import jcrystal.types.IJType;
import jcrystal.types.JVariable;
import jcrystal.utils.ListUtils;
import jcrystal.utils.StringSeparator;
import jcrystal.utils.StringUtils;
import jcrystal.utils.langAndPlats.AbsCodeBlock;
import jcrystal.utils.langAndPlats.SwiftCode;

/**
* Created by gasotelo on 2/2/17.
*/
public class GeneradorEntidad extends AbsEntityGenerator<SwiftClient>{
	private ClientGeneratorDescriptor<?> descriptor;
	
	public GeneradorEntidad(final SwiftClient client, ClientGeneratorDescriptor<?> descriptor) {
		super(client);
		this.descriptor = descriptor;
		this.entityValidatorGenerator = new IosValidators(client);
		
	}
	public void crearInterfaz(final EntityClass entidad, final JsonLevel level, final JsonLevel prev){
		final SwiftCode cliente = new SwiftCode(){{
				$("import Foundation");
				$("import " + SwiftClient.jCrystalPackage);
				$("public protocol " + entidad.clase.getSimpleName()+level.baseName() + (prev != null ? " : "+entidad.clase.getSimpleName()+prev.baseName() : ""), ()->{
					for(EntityField f : descriptor.getFields(entidad).get(level))
						$("func " + f.name() + "() -> " + $($convert(client.entityUtils.convertToRawType(f.f))));
				});
				$L("public let CREATOR"+entidad.clase.getSimpleName() + level.baseName()+" = ", new AbsCodeBlock.Lambda("json: [String : AnyObject]", entidad.clase.getSimpleName() + level.baseName()) {
					@Override public void run() {
						$("return Serializer"+entidad.clase.getSimpleName()+".fromJson(json)");
					}
				}, "");
		}};
		client.exportFile(cliente, paqueteEntidades.replace(".", File.separator) + File.separator + entidad.clase.getSimpleName()+level.baseName() + ".swift");
	}
	public void generateEntity(final EntityClass entidad, final TreeSet<JsonLevel> levels){
		
		StringSeparator interfaces = new StringSeparator(',');
		JsonLevel prev = null;
		for(JsonLevel level : levels) {
			crearInterfaz(entidad, level, prev);
			interfaces.add(entidad.clase.getSimpleName()+level.baseName());
			prev = level;
		}
		//CREAR LA CLASE
		final List<EntityField> definitiveFields = ListUtils.join(descriptor.getFields(entidad).get(null), new ArrayList<EntityField>(descriptor.getFields(entidad).get(levels.last())));
		
		new SwiftCode(){{
			$("import Foundation");
			$("import " + SwiftClient.jCrystalPackage);
			for(final EntityField f : definitiveFields){
				if (f.type().is(Date.class) && f.isAnnotationPresent(EntityProperty.class)) {
					$("private let SDF_" + f.name() + " : DateFormatter = createDateFormatter(\"" + DateType.DATE_MILIS.format + "\", \"" + "UTC" + "\")");
				}
			}
			$("public class " + entidad.clase.getSimpleName()+": "+interfaces, ()->{
				for(final EntityField f : definitiveFields) {
					if(f.type().isEnum())
						client.requiredClasses.add(f.type());
					if(f.type().isPrimitive()) {
						if(f.type().is(boolean.class))
							$("fileprivate var _" + f.name() + " : " + $($convert(f.type()))+ " = false");
						else
							$("fileprivate var _" + f.name() + " : " + $($convert(f.type()))+ " = 0");
					}else {
						IJType tipo = $convert(client.entityUtils.convertToRawType(f.f));
						if(tipo.nullable())
							$("fileprivate var _" + f.name() + " : " + $(tipo));
						else
							$("fileprivate var _" + f.name() + " : " + $(tipo) + " = " + $(tipo)+"()");
					}
				}
				$("");
				for(final EntityField f : definitiveFields){
					$("public func " + f.name() + "()->" + $($convert(client.entityUtils.convertToRawType(f.f))) + " {return self._"+f.name()+"}");
					$("public func " + f.name() + "(val : "+ $($convert(client.entityUtils.convertToRawType(f.f)))+"){self._"+f.name()+" = val}");
				}
				$("public init()", ()->{});
				
				if(entidad.clase.isAnnotationPresent(LoginResultClass.class))
					SwiftClient.crearCodigoTokenSeguridad(client.context, this, entidad.clase);
				$("public class MapList",()->{
					//TreeSet<String> procesado = new TreeSet<>();
					descriptor.getFields(entidad).fields.forEach((level, list)->{
						if(level != null && levels.contains(level))
							list.stream()/*.filter(f->!procesado.contains(f.name()))*/.forEach(f->{
								//procesado.add(f.name());
								String className =entidad.clase.getSimpleName()+level.baseName();
								if(f.type().is(Long.class, long.class) || f.isAnnotationPresent(RelMto1.class)) {
									$("static func By"+StringUtils.capitalize(f.name())+"(_ a : ["+className+"]) -> Dictionary<Int64, "+className+">",()->{
										$("return Dictionary(a.map({($0." + f.name() + "()"+(f.type().isPrimitive()?"":"!")+", $0)}), uniquingKeysWith : { (first, _) in first })");
									});
								}else if(f.type().isEnum()) {
									$("static func By"+StringUtils.capitalize(f.name())+"(_ a : ["+className+"]) -> Dictionary<Int, "+className+">",()->{
										$("return Dictionary(a.map({($0." + f.name() + "()!.id, $0)}), uniquingKeysWith : { (first, _) in first })");
									});
								}
							});
					});
				});
				$("public class Group",()->{
					TreeSet<String> procesado = new TreeSet<>();
					descriptor.getFields(entidad).fields.forEach((level, list)->{
						if(level != null && levels.contains(level))
							list.stream().filter(f->!procesado.contains(f.name())).forEach(f->{
								procesado.add(f.name());
								String className =entidad.clase.getSimpleName()+level.baseName();
								if(f.type().is(Long.class, long.class)) {
									$("static func By"+StringUtils.capitalize(f.name())+"(_ a : ["+className+"]) -> Dictionary<Int64, ["+className+"]>",()->{
										$("return Dictionary(grouping: a, by: {$0." + f.name()+"()"+(f.type().isPrimitive()?"":"!")+"})");
									});
								}	
								if(f.type().isEnum()) {
									$("static func By"+StringUtils.capitalize(f.name())+"(_ a : ["+className+"]) -> Dictionary<Int, ["+className+"]>",()->{
										$("return Dictionary(grouping: a, by: {$0." + f.name()+"()!.id})");
									});
								}	
							});
					});
				});
			});
			new GeneradorSerializer(client).generateEntidad(entidad, this, definitiveFields, levels);
			client.exportFile(this, paqueteEntidades.replace(".", File.separator) + File.separator + entidad.clase.getSimpleName() + ".swift");
		}};
		
		//TODO: crear codigo almacenamiento
		//crearCodigoAlmacenamiento(paqueteEntidades, entidad.clase.clase, entidad);
		client.crearCodigoAlmacenamiento(paqueteEntidades, entidad.clase, levels);
	}

	@Override
	public void generateEntityKey(EntityClass entidad) {
		// TODO Generate entity key
	}
}

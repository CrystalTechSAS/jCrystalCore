package jcrystal.clients.ios;

import static java.lang.reflect.Modifier.PUBLIC;
import static java.lang.reflect.Modifier.STATIC;

import java.util.Date;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.google.appengine.api.datastore.Text;

import jcrystal.datetime.DateType;
import jcrystal.json.JsonLevel;
import jcrystal.model.server.db.EntityClass;
import jcrystal.model.server.db.EntityField;
import jcrystal.reflection.annotations.CrystalDate;
import jcrystal.reflection.annotations.jEntity;
import jcrystal.reflection.annotations.com.jSerializable;
import jcrystal.reflection.annotations.entities.Rel1to1;
import jcrystal.reflection.annotations.entities.RelMto1;
import jcrystal.types.IJType;
import jcrystal.types.JClass;
import jcrystal.types.JVariable;
import jcrystal.types.utils.GlobalTypes;
import jcrystal.utils.StringUtils;
import jcrystal.utils.langAndPlats.SwiftCode;

public class GeneradorSerializer{
	SwiftClient client;
	public GeneradorSerializer(SwiftClient client) {
		this.client = client;
	}
	public void generateResultClass(JClass clase, SwiftCode code){
		SwiftCode nuevo = new SwiftCode() {{
			$("public class Serializer" + clase.getSimpleName(), ()->{
				generateFromJson(this, clase, null, clase.attributes);
				
				client.context.utils.generadorToJson.generateJsonify(clase, this, null, clase.attributes.stream().map(f -> f.accessor().prefix("objeto._")).collect(Collectors.toList()));
				
				$L("public static let CREATOR = ", new Lambda("_ json: [String: AnyObject]", clase.getSimpleName()) {
					@Override public void run() {
						$("return Serializer"+clase.getSimpleName()+".fromJson(json)");
					}
				},"");
			});
		}};
		code.$append(nuevo);
	}
	public void generateEntidad(EntityClass entidad, SwiftCode code, final List<EntityField> definitiveFields, final TreeSet<JsonLevel> levels){
		SwiftCode nuevo = new SwiftCode() {{
			$("class Serializer" + entidad.clase.getSimpleName(), ()->{
				generateFromJson(this, entidad.clase, null, definitiveFields.stream().map(f->f.f).collect(Collectors.toList()));
				client.context.utils.generadorToJson.generateJsonify(entidad.clase, this, null, definitiveFields.stream().map(f -> {
					return f.fieldKeyAccessor().prefix("objeto.");
				}).collect(Collectors.toList()));
				for(final JsonLevel level : levels){
					generateFromJson(this, entidad.clase, level, client.descriptor.getFields(entidad).get(level).stream().map(f->f.f).collect(Collectors.toList()));
				}
				$L("static let CREATOR = ", new Lambda("_ json: [String: AnyObject]", entidad.clase.getSimpleName()) {
					@Override public void run() {
						$("return Serializer"+entidad.clase.getSimpleName()+".fromJson(json)");
					}
				},"");
				/*generateFromJson(this, clase, null, clase.attributes);
				
				GeneradorToJson.generateJsonify(clase, this, null, clase.attributes.stream().map(f -> new PrefixAccessor("objeto._", new CrystalField(f))).collect(Collectors.toList()));
				*/
			});
		}};
		code.$append(nuevo);
	}
	public void generateFromJson(SwiftCode bloque, final JClass clase, final JsonLevel level, final List<JVariable> campos){
		bloque.new B() {{
				$M(PUBLIC | STATIC, clase.getSimpleName(), "fromJson" + (level == null ? "" : level.baseName()), $(P(GlobalTypes.Json.JSONObject, "_ json")), ()->{
					$("let ret = "+clase.getSimpleName()+"()");
					for(final JVariable f : campos)
						procesarCampo(bloque, f);
					$("return ret");
				});
				
				$M(PUBLIC | STATIC, "["+clase.getSimpleName()+(level==null?"":level.baseName())+"]", "listFromJson"+(level==null?"":level.baseName()), $(P(GlobalTypes.Json.JSONArray, "_ json")), ()->{
					$("var ret = ["+clase.getSimpleName()+(level==null?"":level.baseName())+"]()");
					$("for e in 0 ..< json.count", ()->{
						$("ret.append(fromJson"+(level==null?"":level.baseName())+"(json[e]))");
					});
					$("return ret");
				});
		}};
	}
	private void procesarCampo(final SwiftCode code, final JVariable f){
		code.new B() {{
				if(f.type().name().equals("com.google.appengine.api.datastore.GeoPt")) {
					$if("let point = json[\"" + f.name() + "\"] as? [AnyObject]", ()->{
						$("ret._" + f.name() + " = [point[0] as! Double, point[1] as! Double]");
					});
				}else if(f.type().isEnum()) {
					client.requiredClasses.add(f.type());
					$if_let("Int", "_"+f.name(), "json[\"" + f.name() + "\"] as? Int", null, ()->{
						$("ret._" + f.name() + " = " + f.type().getSimpleName() + ".fromId(_"+f.name()+")");
					});
				}else if(f.type().is(Date.class)){
					DateType tipo = f.isJAnnotationPresent(CrystalDate.class)?f.getJAnnotation(CrystalDate.class).value():DateType.DATE_MILIS;
					$if("let fecha = json[\"" + f.name() + "\"] as? String", ()->{
						$("ret._" + f.name() + " = Crystal" + StringUtils.camelizar(tipo.name()) + ".SDF.date(from: fecha)");
					});
				}else if(f.type().isJAnnotationPresent(CrystalDate.class)) {
					$if_let("String", "fecha", "json[\"" + f.name() + "\"] as? String", null, ()->{
						$("ret._" + f.name() + " = " + f.type().getSimpleName() + "(fecha)");
					});
				}else if(f.type().isAnnotationPresent(jSerializable.class))
					$("if let _data" + f.name() + " = json[\"" + f.name() + "\"] as? [String: AnyObject]",()->{
						$("ret._" + f.name() + " = Serializer"+f.type().getSimpleName()+".fromJson(_data" + f.name()+")");
					});
				else if(f.type().is(int.class, Integer.class))
					$if_let("Int", "numberVal", "json[\"" + f.name() + "\"] as? Int", null, ()->{
						$("ret._" + f.name() + " = numberVal");
					}).$else(()->{
						if(f.type().is(int.class))
							$("ret._" + f.name() + " = Int(json[\"" + f.name() + "\"] as? String ?? \"0\") ?? 0");
						else
							$("ret._" + f.name() + " = Int(json[\"" + f.name() + "\"] as? String ?? \"0\")");
					});
				else if(f.type().is(long.class, Long.class))
					$if_let("", "numberVal", "json[\"" + f.name() + "\"] as? NSNumber", null, ()->{
						$("ret._" + f.name() + " = numberVal.int64Value");
					}).$else(()->{
						if(f.type().is(long.class))
							$("ret._" + f.name() + " = Int64(json[\"" + f.name() + "\"] as? String ?? \"0\") ?? 0");
						else
							$("ret._" + f.name() + " = Int64(json[\"" + f.name() + "\"] as? String ?? \"0\")");
					});
				else if(f.type().is(boolean.class))
					$("ret._" + f.name() + " = json[\"" + f.name() + "\"] as? Bool ?? false");
				else if(f.type().is(double.class, Double.class))
					$("ret._" + f.name() + " = json[\"" + f.name() + "\"] as? Double ?? 0.0");
				else if(f.type().is(float.class))
					$("ret._" + f.name() + " = json[\"" + f.name() + "\"] as? Float ?? 0.0");
				else if(f.type().is(String.class, Text.class)) {
					$("ret._" + f.name() + " = json[\"" + f.name() + "\"] as? String");
				}else if(f.type().isAnnotationPresent(jEntity.class)) {
					if(f.isAnyAnnotationPresent(RelMto1.class, Rel1to1.class))
						$("ret._" + f.name() + " = (json[\"" + f.name() + "\"] as? NSNumber)?.int64Value");
					else
						$("if let _data" + f.name() + " = json[\"" + f.name() + "\"] as? [String: AnyObject]",()->{
							$("ret._" + f.name() + " = Serializer"+f.type.getSimpleName()+".fromJson(_data" + f.name() + ")");
						});
						
				}else if(f.type().isAnnotationPresent(jSerializable.class)) {
					$if("let _object = json[\"" + f.name() + "\"] as? [String : AnyObject]",()->{
						$("ret._" + f.name() + " = " + f.type().getSimpleName() + "(_object)");
					});
				}else if(f.type().isArray()){
					$if("let _Array" + f.name() + " = json[\"" + f.name() + "\"] as? [AnyObject]",()->{
						IJType arrayType = f.type().getInnerTypes().get(0);
						$("ret._" + f.name() + " = " + "[" + $($convert(arrayType)) + "]()");
						$("for i in 0 ..< _Array" + f.name() + ".count", ()->{
							if(arrayType.is(int.class))
								$("ret._" + f.name() + "?.append(_Array" + f.name() + "[i] as! Int)");
							else if(arrayType.is(long.class))
								$("ret._" + f.name() + "?.append(_Array" + f.name() + "[i] as! Int64)");
							else if(arrayType.is(double.class))
								$("ret._" + f.name() + "?.append(_Array" + f.name() + "[i] as! Double)");
							else if(arrayType.is(float.class))
								$("ret._" + f.name() + "?.append(_Array" + f.name() + "[i] as! Float)");
							else if(arrayType.is(boolean.class))
								$("ret._" + f.name() + "?.append(_Array" + f.name() + "[i] as! Bool)");
							else
								throw new NullPointerException(arrayType.name());
						});
					});
				}else if(f.type().isIterable()) {
					IJType tipoParamero = f.type().getInnerTypes().get(0);
					if (tipoParamero.name().equals("com.google.appengine.api.datastore.GeoPt")) {
						$("let _Array" + f.name() + " = json[\"" + f.name() + "\"] as? [[AnyObject]]");
						$("ret._" + f.name() + " = [[double]](repeating: Double[](count: 2, repeating: 0), count: _Array" + f.name() + ".length())");
						$("for(int i = 0; i < ret._" + f.name() + ".length; i++)", ()->{
							$("let _temp = _Array" + f.name() + "[i]");
							$("ret._" + f.name() + "[i] = [_temp[0] as! Double, _temp[1] as! Double]");
						});
					}else if(tipoParamero.isAnnotationPresent(jSerializable.class)){
						$("if let _Array" + f.name() + " = json[\"" + f.name() + "\"] as? [[String: AnyObject]]",()->{
							$("ret._" + f.name() + " = Serializer"+tipoParamero.getSimpleName()+".listFromJson(_Array"+f.name()+")");
						});
						$("else",()->{
							$("ret._" + f.name() + " = ["+tipoParamero.getSimpleName()+"]()");
						});
					}else if(tipoParamero.is(String.class)){
						$("if let _Array" + f.name() + " = json[\"" + f.name() + "\"] as? [String]",()->{
							$("ret._" + f.name() + " = _Array" + f.name());
						});
						$("else",()->{
							$("ret._" + f.name() + " = ["+tipoParamero.getSimpleName()+"]()");
						});
					}else if(tipoParamero.isAnnotationPresent(jEntity.class)) {
						$("if let _Array" + f.name() + " = json[\"" + f.name() + "\"] as? [Int64]",()->{
							$("ret._" + f.name() + " = _Array" + f.name());
						});
						$("else",()->{
							$("ret._" + f.name() + " = [Int64]()");
						});
					}else{
						throw new NullPointerException("Unsupported ios field on "+f.type() + ":"+f.name()+":");
					}
				}else{
					throw new NullPointerException("Unsupported ios field on "+$(f.type()) + ":"+f.name()+":"+f.type().name());
				}
		}};
	}
}

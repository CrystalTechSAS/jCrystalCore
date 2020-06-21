package jcrystal.utils.extractors;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.appengine.api.datastore.Text;

import jcrystal.reflection.annotations.com.jSerializable;
import jcrystal.types.IJType;
import jcrystal.types.JClass;
import jcrystal.types.vars.IAccessor;
import jcrystal.annotations.internal.model.db.InternalEntityKey;
import jcrystal.datetime.DateType;
import jcrystal.entity.types.ConstantId;
import jcrystal.entity.types.LongText;
import jcrystal.json.JsonLevel;
import jcrystal.lang.Language;
import jcrystal.reflection.MaskedEnum;
import jcrystal.reflection.annotations.CrystalDate;
import jcrystal.reflection.annotations.jEntity;
import jcrystal.utils.SerializeLevelUtils;
import jcrystal.utils.StringUtils;
import jcrystal.utils.context.CodeGeneratorContext;
import jcrystal.utils.context.CodeGeneratorContext.ContextType;
import jcrystal.utils.langAndPlats.AbsCodeBlock;

public class GeneratorFromJsonObject extends AbsExtractor{
	
	public void procesarCampo(AbsCodeBlock code, final JClass definingClass, final IAccessor f){
		setDelegate(code);
		if(f.type().is(ConstantId.class));
		else if(f.type().is(Text.class, LongText.class)){
			if(ContextType.MOBILE.is())
				$(f.write("json.has(\"" + f.name() + "\")?json.getString(\"" + f.name() + "\"):null")+";");
			else
				$(f.write("json.has(\"" + f.name() + "\")?json.getString(\"" + f.name() + "\"):null")+";");
		}else if(f.type().name().equals("com.google.appengine.api.datastore.GeoPt")){
			if(ContextType.MOBILE.is())
				$(f.write("json.has(\"" + f.name() + "\")?new double[]{json.getJSONArray(\"" + f.name() + "\").getDouble(0), json.getJSONArray(\"" + f.name() + "\").getDouble(1)}:null")+";");
			else
				$(f.write("json.has(\"" + f.name() + "\")?new com.google.appengine.api.datastore.GeoPt((float)json.getJSONArray(\"" + f.name() + "\").getDouble(0), (float)json.getJSONArray(\"" + f.name() + "\").getDouble(1)):null")+";");
		}else if(f.type().isEnum()) {
			if(f.type().resolve(c->c.enumData.propiedades.get("id")) == null)
				$(f.write("json.has(\"" + f.name() + "\")&&!json.isNull(\""+f.name()+"\")?" + $(f.type()) + ".valueOf(json.getString(\"" + f.name() + "\")):null")+";");
			else if(ContextType.SERVER.is())
				$(f.write("json.has(\"" + f.name() + "\")&&!json.isNull(\""+f.name()+"\")?" + f.type().prefixName("Utils") + ".fromId(json.getInt(\"" + f.name() + "\")):null")+";");
			else
				$(f.write("json.has(\"" + f.name() + "\")&&!json.isNull(\""+f.name()+"\")?" + f.type().getSimpleName() + ".fromId(json.getInt(\"" + f.name() + "\")):null")+";");
		}else if(f.type().is(Date.class)) {
			final DateType tipo = f.isJAnnotationPresent(CrystalDate.class)?f.getJAnnotation(CrystalDate.class).value():DateType.DATE_MILIS;
			$("try", ()->{
				$(f.write("json.has(\"" + f.name() + "\")&&!json.isNull(\""+f.name()+"\")?DateType." + tipo + ".FORMAT.parse(json.getString(\"" + f.name() + "\")):null")+";");
			});
			$("catch(java.text.ParseException ex)", ()->{
				$("throw new org.json.JSONException(ex.getMessage());");
			});
		}else if(f.type().isJAnnotationPresent(CrystalDate.class)) {
			$("try", ()->{
				if(ContextType.SERVER.is() && definingClass.isAnnotationPresent(jEntity.class))
					$(f.write("json.has(\"" + f.name() + "\")&&!json.isNull(\""+f.name()+"\")?new " + f.type().getSimpleName() + "(json.getString(\"" + f.name() + "\")).toDate():null")+";");
				else
					$(f.write("json.has(\"" + f.name() + "\")&&!json.isNull(\""+f.name()+"\")?new " + $(f.type()) + "(json.getString(\"" + f.name() + "\")):null")+";");
			});
			$("catch(java.text.ParseException ex)", ()->{
				$("throw new org.json.JSONException(ex.getMessage());");
			});
		}else if(f.type().isAnnotationPresent(jSerializable.class)) {
			if(ContextType.SERVER.is())
				$if("json.has(\""+f.name()+"\") && !json.isNull(\""+f.name()+"\")",()->{
					$(f.write("new " + $($convert(f.type())) + "(json.optJSONObject(\"" + f.name() + "\"))")+";");
				});
			else
				$if("json.has(\""+f.name()+"\")  && !json.isNull(\""+f.name()+"\")",()->{
					$(f.write("new " + $($convert(f.type())) + "(json.optJSONObject(\"" + f.name() + "\"))")+";");
				});
		}
		//Tipos Json
		else if(f.type().is(JSONArray.class))
			$(f.write("json.optJSONArray(\"" + f.name() + "\")")+";");
		else if(f.type().is(JSONObject.class))
			$(f.write("json.optJSONObject(\"" + f.name() + "\")")+";");
		//Tipos b�sicos
		else if(f.type().is(char.class))
			$(f.write("(char)json.optInt(\"" + f.name() + "\")")+";");
		else if(f.type().is(int.class))
			$(f.write("json.optInt(\"" + f.name() + "\")")+";");
		else if(f.type().is(Integer.class))
			$(f.write("json.has(\"" + f.name() + "\")?json.getInt(\"" + f.name() + "\"):null")+";");
		else if(f.type().is(long.class))
			$(f.write("json.optLong(\"" + f.name() + "\", 0)")+";");
		else if(f.type().is(Long.class))
			$(f.write("json.has(\"" + f.name() + "\")&&!json.isNull(\""+f.name()+"\")?json.getLong(\"" + f.name() + "\"):null")+";");
		else if(f.type().is(boolean.class))
			$(f.write("json.optBoolean(\"" + f.name() + "\")")+";");
		else if(f.type().is(double.class))
			$(f.write("json.optDouble(\"" + f.name() + "\")")+";");
		else if (f.type().is(Double.class))
			$(f.write("json.has(\"" + f.name() + "\")&&!json.isNull(\""+f.name()+"\")??json.getDouble(\"" + f.name() + "\"):null")+";");
		else if(f.type().is(float.class))
			$(f.write("json.optFloat(\"" + f.name() + "\")")+";");
		else if(f.type().is(String.class))
			$(f.write("json.has(\"" + f.name() + "\")&&!json.isNull(\""+f.name()+"\")?json.getString(\"" + f.name() + "\"):null")+";");
		//Entidades
		else if(f.type().isAnnotationPresent(jEntity.class)) {
			JsonLevel level = SerializeLevelUtils.getAnnotedJsonLevel(f);
			$(f.write("json.has(\"" + f.name() + "\")&&!json.isNull(\""+f.name()+"\")?new " + $(f.type())+".Post."+level.baseName()+"(json.getJSONObject(\"" + f.name() + "\")).create():null")+";");
			//$(f.accessor()+ " = json.has(\"" + f.name() + "\")&&!json.isNull(\""+f.name()+"\")?json.getLong(\"" + f.name() + "\"):null;");
		}else if(f.type().isJAnnotationPresent(InternalEntityKey.class)) {
			if(ContextType.SERVER.is())
				$(f.write("json.has(\"" + f.name() + "\")&&!json.isNull(\""+f.name()+"\")?new " + $(f.type().getJAnnotation(InternalEntityKey.class).parentEntity())+".Post.Key(json.getJSONObject(\"" + f.name() + "\")).create():null")+";");
			else
				$(f.write("json.has(\"" + f.name() + "\")&&!json.isNull(\""+f.name()+"\")?" + $(f.type())+".fromJson(json.getJSONObject(\"" + f.name() + "\")):null")+";");
			//$(f.accessor()+ " = json.has(\"" + f.name() + "\")&&!json.isNull(\""+f.name()+"\")?json.getLong(\"" + f.name() + "\"):null;");
		}else if(f.type().isArray()){
			$("", ()->{
				IJType arrayType = f.type().getInnerTypes().get(0);
				if(arrayType.isSubclassOf(MaskedEnum.class)){
					$(f.write("" + (ContextType.SERVER.is()?arrayType.name():arrayType.getSimpleName()) + ".getFromMask(json.optLong(\"" + f.name() + "\",0))")+";");
				}else{
					$("org.json.JSONArray $Array" + f.name() + " = json.optJSONArray(\"" + f.name() + "\");");
					$if("$Array" + f.name()+" != null",()->{
						$(f.write("new " + arrayType.name() + "[$Array" + f.name() + ".length()]")+";");
						$("for(int i = 0; i < " + f.read()+ ".length; i++)", ()->{
							if(arrayType.is(String.class))
								$(f.read()+ "[i] = $Array" + f.name() + ".optString(i);");
							else if(arrayType.is(int.class))
								$(f.read()+ "[i] = $Array" + f.name() + ".getInt(i);");
							else if(arrayType.is(long.class))
								$(f.read()+ "[i] = $Array" + f.name() + ".getLong(i);");
							else if(arrayType.is(double.class))
								$(f.read()+ "[i] = $Array" + f.name() + ".getDouble(i);");
							else if(arrayType.is(boolean.class))
								$(f.read()+ "[i] = $Array" + f.name() + ".getBoolean(i);");
							else
								throw new NullPointerException("Array type not supported: "+arrayType.name()+" (String, int, long, double, boolean)");
						});
					});
					
				}
			});
		}
		//Maps
		else if(f.type().isSubclassOf(Map.class)) {
			final IJType tipoKey = f.type().getInnerTypes().get(0);
			final IJType tipoVal = f.type().getInnerTypes().get(1);
			$("org.json.JSONObject $Map" + f.name() + " = json.optJSONObject(\"" + f.name() + "\");");
			$if("$Map" + f.name()+" != null",()->{
				$(f.write("new java.util.TreeMap<>()")+";");
				$("for(String $key : $Map"+f.name()+".keySet())",()->{
					$(f.read() + ".put($key, $Map"+f.name()+".optString($key));");
				});
			});
		}
		//Listas
		else if(f.type().isSubclassOf(List.class)) {
			$("", ()->{
				final IJType tipoParamero = f.type().getInnerTypes().get(0);
				$("org.json.JSONArray $Array" + f.name() + " = json.optJSONArray(\"" + f.name() + "\");");
				$if("$Array" + f.name()+" != null",()->{
					if (tipoParamero.name().equals("com.google.appengine.api.datastore.GeoPt")) {
						if(ContextType.SERVER.is())
							$(f.write("new java.util.ArrayList<>()")+";");
						else
							$(f.write("new double[$Array" + f.name() + ".length()][2]")+";");
						$("for(int i = 0; i < $Array" + f.name() + ".length(); i++)", ()->{
							$("org.json.JSONArray $temp = $Array" + f.name() + ".getJSONArray(i);");
							if(ContextType.SERVER.is()) {
								$(f.read()+ ".add(new com.google.appengine.api.datastore.GeoPt((float)$temp.getDouble(0), (float)$temp.getDouble(1)));");
							}else{
								$(f.read()+ "[i][0] = $temp.getDouble(0);");
								$(f.read()+ "[i][1] = $temp.getDouble(1);");
							}
						});
					}else if(tipoParamero.is(Long.class, Integer.class, String.class)){
						String jsonName = tipoParamero.is(Integer.class) ? "Int" : StringUtils.capitalize(tipoParamero.getSimpleName());
						if(CodeGeneratorContext.is(Language.JAVA)) {
							$(f.write("new java.util.ArrayList<>()")+";");
							$("for(int i = 0; i < $Array" + f.name() + ".length(); i++)", ()->{
								$if("!$Array" + f.name() + ".isNull(i)",()->{
									$(f.read()+ ".add($Array" + f.name() + ".get" + jsonName + "(i));");
								});
							});
						}else {
							$(f.write("new "+tipoParamero.name()+"[$Array" + f.name() + ".length()]")+";");
							$("for(int i = 0; i < $Array" + f.name() + ".length(); i++)", ()->{
								$if("!$Array" + f.name() + ".isNull(i)",()->{
									$(f.read()+ "[i] = $Array" + f.name() + ".get" + jsonName + "(i);");
								});
							});
						}
						
					}else if(tipoParamero.isEnum()){
						if(ContextType.SERVER.is() || ContextType.isAndroid())
							$(f.write(""+tipoParamero.prefixName("Utils")+".from($Array"+f.name()+")")+";");
						else {
							$(f.write("new "+tipoParamero.name()+"[$Array" + f.name() + ".length()]")+";");
							$("for(int i = 0; i < $Array" + f.name() + ".length(); i++)", ()->{
								$(f.read()+ "[i] = "+tipoParamero.getSimpleName()+".fromId($Array" + f.name() + ".getLong(i));");
							});
						}
					}else if(tipoParamero.isAnnotationPresent(jSerializable.class)){
						if(ContextType.SERVER.is())
							$(f.write("" + tipoParamero.name() + ".listFromJson($Array" + f.name() + ")")+";");
						else
							$(f.write("" + $($convert(tipoParamero)) + ".listFromJson($Array" + f.name() + ")")+";");
					}else if(tipoParamero.isAnnotationPresent(jEntity.class)){
						JsonLevel level = SerializeLevelUtils.getAnnotedJsonLevel(f);
						if(ContextType.SERVER.is() || ContextType.isAndroid()) {
							$(f.write("new java.util.ArrayList<>()")+";");
							$("for(int i = 0; i < $Array" + f.name() + ".length(); i++)", ()->{
								$(f.read()+ ".add(new "+tipoParamero.name()+".Post."+level.baseName()+"($Array"+f.name()+".getJSONObject(i)).create());");
								//$(f.accessor()+ ".add($Array" + f.name() + ".getLong(i));");
							});
						}else {
							$(f.write("new "+tipoParamero.name()+"[$Array" + f.name() + ".length()]")+";");
							$("for(int i = 0; i < $Array" + f.name() + ".length(); i++)", ()->{
								$(f.read()+ "[i] = $Array" + f.name() + ".getLong(i);");
							});
						}
					}else
						throw new NullPointerException(f.type().name());
				});
			});
		}else{
			throw new NullPointerException(definingClass.name + " " + f.name()+":"+f.type());
		}
	}
}

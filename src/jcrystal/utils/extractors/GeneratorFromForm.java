package jcrystal.utils.extractors;

import java.util.Date;
import java.util.List;

import com.google.appengine.api.datastore.GeoPt;
import com.google.appengine.api.datastore.Text;

import jcrystal.reflection.annotations.com.jSerializable;
import jcrystal.types.JClass;
import jcrystal.types.vars.IAccessor;
import jcrystal.datetime.DateType;
import jcrystal.entity.types.ConstantId;
import jcrystal.reflection.annotations.CrystalDate;
import jcrystal.reflection.annotations.jEntity;
import jcrystal.utils.context.CodeGeneratorContext.ContextType;
import jcrystal.utils.langAndPlats.AbsCodeBlock;

public class GeneratorFromForm extends AbsExtractor{
	
	public void procesarCampo(AbsCodeBlock code, final JClass definingClass, final IAccessor f){
		setDelegate(code);
		if(f.type().is(ConstantId.class));
		else if(f.type().is(Text.class)){
			$(f.read()+ " = req.getParameter(\"" + f.name() + "\");");
		}else if(f.type().is(GeoPt.class)){
			$(f.read()+ " = req.getParameter(\"" + f.name() + "\") != null ? new com.google.appengine.api.datastore.GeoPt(Float.parseFloat(req.getParameterValues(\"" + f.name() + "\")[0]), Float.parseFloat(req.getParameterValues(\"" + f.name() + "\")[1])):null;");
		}else if(f.type().isEnum()) {
			if(f.type().resolve(c->c.enumData.propiedades.get("id")) == null)
				$(f.read()+ " = req.getParameter(\"" + f.name() + "\")!=null?"+$(f.type())+".valueOf(req.getParameter(\"" + f.name() + "\")):null;");
			else
				$(f.read()+ " = req.getParameter(\"" + f.name() + "\")!=null?"+f.type().prefixName("Utils")+".fromId(Integer.parseInt(req.getParameter(\"" + f.name() + "\"))):null;");
		}else if(f.type().is(Date.class)) {
			$(f.write("req.getParameter(\"" + f.name() + "\") != null ? new java.util.Date(Long.parseLong(req.getParameter(\"" + f.name() + "\"))) : null")+";");
		}else if(f.type().isJAnnotationPresent(CrystalDate.class)) {
			$("try", ()->{
				if(ContextType.SERVER.is() && definingClass.isAnnotationPresent(jEntity.class))
					$(f.write("req.getParameter(\"" + f.name() + "\") != null ? new " + f.type().getSimpleName() + "(req.getParameter(\"" + f.name() + "\")).toDate() : null")+";");
				else
					$(f.write("req.getParameter(\"" + f.name() + "\") != null ? new jcrystal.datetime." + f.type().getSimpleName() + "(req.getParameter(\"" + f.name() + "\")) : null")+";");
			});
			$("catch(java.text.ParseException ex)", ()->{
				$("throw new org.json.JSONException(ex.getMessage());");
			});
		}else if(f.type().isAnnotationPresent(jSerializable.class)) {
			$(f.read()+ " = new " + f.type().name() + "(json.optJSONObject(\"" + f.name() + "\"));");
			throw new NullPointerException();
		}else if(f.type().is(char.class))
			$(f.read()+ " = req.getParameter(\"" + f.name() + "\").charAt(0);");
		else if(f.type().is(int.class))
			$(f.read()+ " = req.getParameter(\"" + f.name() + "\")!=null?Integer.parseInt(req.getParameter(\"" + f.name() + "\")): 0;");
		else if(f.type().is(Integer.class))
			$(f.read()+ " = req.getParameter(\"" + f.name() + "\")!=null?Integer.parseInt(req.getParameter(\"" + f.name() + "\")) : null;");
		else if(f.type().is(long.class))
			$(f.read()+ " = req.getParameter(\"" + f.name() + "\")!=null?Long.parseLong(req.getParameter(\"" + f.name() + "\")) : 0;");
		else if(f.type().is(Long.class))
			$(f.read()+ " = req.getParameter(\"" + f.name() + "\")!=null?Long.parseLong(req.getParameter(\"" + f.name() + "\")) : null;");
		else if(f.type().is(boolean.class))
			$(f.read()+ " = req.getParameter(\"" + f.name() + "\")!=null?Boolean.parseBoolean(req.getParameter(\"" + f.name() + "\")) : false;");
		else if(f.type().is(double.class))
			$(f.read()+ " = req.getParameter(\"" + f.name() + "\")!=null?Double.parseDouble(req.getParameter(\"" + f.name() + "\")) : 0;");
		else if (f.type().is(Double.class))
			$(f.read()+ " = req.getParameter(\"" + f.name() + "\")!=null?Double.parseDouble(req.getParameter(\"" + f.name() + "\")) : null;");
		else if(f.type().is(float.class))
			$(f.read()+ " = req.getParameter(\"" + f.name() + "\")!=null?Float.parseFloat(req.getParameter(\"" + f.name() + "\")) : null;");
		else if(f.type().is(String.class))
			$(f.read()+ " = req.getParameter(\"" + f.name() + "\");");
		else if(f.type().isArray()){
			$("//Omitted: " + f.read());
			//TODO:throw new NullPointerException();
			/*$("", ()->{
				Class<?> arrayType = f.type().getComponentType();
				if(MaskedEnum.class.isAssignableFrom(arrayType)){
					$(f.accessor()+ " = " + (is_Server()?arrayType.name():arrayType.getSimpleName()) + ".getFromMask(json.optLong(\"" + f.name() + "\",0));");
				}else{
					$("org.json.JSONArray $Array" + f.name() + " = json.optJSONArray(\"" + f.name() + "\");");
					$if("$Array" + f.name()+" != null",()->{
						$(f.accessor()+ " = new " + arrayType.name() + "[$Array" + f.name() + ".length()];");
						$("for(int i = 0; i < " + f.accessor()+ ".length; i++)", ()->{
							if(arrayType.equals(int.class))
								$(f.accessor()+ "[i] = $Array" + f.name() + ".getInt(i);");
							else if(arrayType.equals(long.class))
								$(f.accessor()+ "[i] = $Array" + f.name() + ".getLong(i);");
							else
								throw new NullPointerException(arrayType.name());
						});
					});
					
				}
			});*/
		}else if(f.type().isSubclassOf(List.class)) {
			$("//Omitted: " + f.read());
			//TODO:throw new NullPointerException();
			/*
			$("", ()->{
				final Class<?> tipoParamero = (Class<?>)((ParameterizedType) f.genericType()).getActualTypeArguments()[0];
				$("org.json.JSONArray $Array" + f.name() + " = json.optJSONArray(\"" + f.name() + "\");");
				$if("$Array" + f.name()+" != null",()->{
					if (tipoParamero.name().equals("com.google.appengine.api.datastore.GeoPt")) {
						if(CodeGeneratorContext.is_Server())
							$(f.accessor()+ " = new java.util.ArrayList<>();");
						else
							$(f.accessor()+ " = new double[$Array" + f.name() + ".length()][2];");
						$("for(int i = 0; i < $Array" + f.name() + ".length(); i++)", ()->{
							$("org.json.JSONArray $temp = $Array" + f.name() + ".getJSONArray(i);");
							if(CodeGeneratorContext.is_Server()){
								$(f.accessor()+ ".add(new com.google.appengine.api.datastore.GeoPt((float)$temp.getDouble(0), (float)$temp.getDouble(1)));");
							}else{
								$(f.accessor()+ "[i][0] = $temp.getDouble(0);");
								$(f.accessor()+ "[i][1] = $temp.getDouble(1);");
							}
						});
					}else if(tipoParamero == Long.class || tipoParamero == Integer.class || tipoParamero == String.class){
						if(CodeGeneratorContext.is_Server() || CodeGeneratorContext.is_Android())
							$(f.accessor()+ " = new java.util.ArrayList<>();");
						else
							$(f.accessor()+ " = new "+tipoParamero.name()+"[$Array" + f.name() + ".length()];");
						String jsonName = tipoParamero == Integer.class ? "Int" : StringUtils.capitalize(tipoParamero.getSimpleName());
						$("for(int i = 0; i < $Array" + f.name() + ".length(); i++)", ()->{
							if(CodeGeneratorContext.is_Server() || CodeGeneratorContext.is_Android()){
								$(f.accessor()+ ".add($Array" + f.name() + ".get" + jsonName + "(i));");
							}else{
								$(f.accessor()+ "[i] = $Array" + f.name() + ".get" + jsonName + "(i);");
							}
						});
					}else if(tipoParamero.isEnum()){
						if(CodeGeneratorContext.is_Server() || CodeGeneratorContext.is_Android())
							$(f.accessor()+ " = new java.util.ArrayList<>();");
						else
							$(f.accessor()+ " = new "+tipoParamero.name()+"[$Array" + f.name() + ".length()];");
						$("for(int i = 0; i < $Array" + f.name() + ".length(); i++)", ()->{
							if(CodeGeneratorContext.is_Server() || CodeGeneratorContext.is_Android()){
								$(f.accessor()+ ".add("+tipoParamero.getSimpleName()+".fromId($Array" + f.name() + ".getLong(i)));");
							}else{
								$(f.accessor()+ "[i] = "+tipoParamero.getSimpleName()+".fromId($Array" + f.name() + ".getLong(i));");
							}
						});
					}else if(tipoParamero.isAnnotationPresent(JsonSerializable.class)){
						if (CodeGeneratorContext.is_Server())
							$(f.accessor()+ " = " + tipoParamero.name() + ".listFromJson($Array" + f.name() + ");");
						else
							$(f.accessor()+ " = " + paqueteResultados + "." + tipoParamero.getSimpleName() + ".listFromJson($Array" + f.name() + ");");
					}else if(tipoParamero.isAnnotationPresent(jEntity.class)){
						System.out.println("TODO_______________________");
					}else
						throw new NullPointerException(f.type().name());
				});
			});*/
		}else{
			//TODO: remove comment
			//throw new NullPointerException(f.type().name());
		}
	}
}

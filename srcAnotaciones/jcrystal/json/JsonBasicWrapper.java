package jcrystal.json;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.json.JsonBasic;
@SuppressWarnings("all")
public class JsonBasicWrapper implements JsonBasic{
	private final JAnnotation anotacion;
	public JsonBasicWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	public String[] value(){
		return (String[])anotacion.values.get("value");
	}
	public void value(String[] value){
		anotacion.values.put("value", value);
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.json.JsonBasic.class;
	}
	@Override public String toString(){
		return "@jcrystal.json.JsonBasic("+"value="+java.util.Arrays.toString(value())+")";
	}
}

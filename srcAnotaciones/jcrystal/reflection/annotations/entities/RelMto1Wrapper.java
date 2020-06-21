package jcrystal.reflection.annotations.entities;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.annotations.entities.RelMto1;
import jcrystal.json.JsonLevel;
@SuppressWarnings("all")
public class RelMto1Wrapper implements RelMto1{
	private final JAnnotation anotacion;
	public RelMto1Wrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	public String name(){
		return (String)anotacion.values.get("name");
	}
	public void name(String value){
		anotacion.values.put("name", value);
	}
	public String target(){
		return (String)anotacion.values.get("target");
	}
	public void target(String value){
		anotacion.values.put("target", value);
	}
	public JsonLevel keyLevel(){
		return JsonLevel.valueOf((String)anotacion.values.get("keyLevel"));
	}
	public void keyLevel(JsonLevel value){
		anotacion.values.put("keyLevel", value == null ? null : value.name());
	}
	public boolean editable(){
		return Boolean.parseBoolean((String)anotacion.values.get("editable"));
	}
	public void editable(boolean value){
		anotacion.values.put("editable", Boolean.toString(value));
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.annotations.entities.RelMto1.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.annotations.entities.RelMto1("+"editable="+editable()+", "+"keyLevel="+keyLevel()+", "+"name="+name()+", "+"target="+target()+")";
	}
}

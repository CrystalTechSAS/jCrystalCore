package jcrystal.reflection.annotations.entities;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.annotations.entities.RelMtoM;
import jcrystal.json.JsonLevel;
@SuppressWarnings("all")
public class RelMtoMWrapper implements RelMtoM{
	private final JAnnotation anotacion;
	public RelMtoMWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	public String value(){
		return (String)anotacion.values.get("value");
	}
	public void value(String value){
		anotacion.values.put("value", value);
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
	public boolean small(){
		return Boolean.parseBoolean((String)anotacion.values.get("small"));
	}
	public void small(boolean value){
		anotacion.values.put("small", Boolean.toString(value));
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.annotations.entities.RelMtoM.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.annotations.entities.RelMtoM("+"keyLevel="+keyLevel()+", "+"small="+small()+", "+"target="+target()+", "+"value="+value()+")";
	}
}

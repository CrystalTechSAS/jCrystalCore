package jcrystal.reflection.docs;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.docs.Param;
@SuppressWarnings("all")
public class ParamWrapper implements Param{
	private final JAnnotation anotacion;
	public ParamWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	public String name(){
		return (String)anotacion.values.get("name");
	}
	public void name(String value){
		anotacion.values.put("name", value);
	}
	public String value(){
		return (String)anotacion.values.get("value");
	}
	public void value(String value){
		anotacion.values.put("value", value);
	}
	public String test(){
		return (String)anotacion.values.get("test");
	}
	public void test(String value){
		anotacion.values.put("test", value);
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.docs.Param.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.docs.Param("+"name="+name()+", "+"test="+test()+", "+"value="+value()+")";
	}
}

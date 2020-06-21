package jcrystal.reflection.docs;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.docs.Doc;
@SuppressWarnings("all")
public class DocWrapper implements Doc{
	private final JAnnotation anotacion;
	public DocWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	public String value(){
		return (String)anotacion.values.get("value");
	}
	public void value(String value){
		anotacion.values.put("value", value);
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.docs.Doc.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.docs.Doc("+"value="+value()+")";
	}
}

package jcrystal.reflection.docs;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.docs.Params;
@SuppressWarnings("all")
public class ParamsWrapper implements Params{
	private final JAnnotation anotacion;
	public ParamsWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	public Param[] value(){
		return java.util.Arrays.stream((JAnnotation[])anotacion.values.get("value")).map(f->new jcrystal.reflection.docs.ParamWrapper(f)).toArray(f->new jcrystal.reflection.docs.Param[f]);
	}
	public void value(Param[] value){
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.docs.Params.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.docs.Params("+"value="+java.util.Arrays.toString(value())+")";
	}
}

package jcrystal.reflection.annotations.ws;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.annotations.ws.DataSource;
@SuppressWarnings("all")
public class DataSourceWrapper implements DataSource{
	private final JAnnotation anotacion;
	public DataSourceWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	public String[] value(){
		return (String[])anotacion.values.get("value");
	}
	public void value(String[] value){
		anotacion.values.put("value", value);
	}
	public String mapBy(){
		return (String)anotacion.values.get("mapBy");
	}
	public void mapBy(String value){
		anotacion.values.put("mapBy", value);
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.annotations.ws.DataSource.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.annotations.ws.DataSource("+"mapBy="+mapBy()+", "+"value="+java.util.Arrays.toString(value())+")";
	}
}

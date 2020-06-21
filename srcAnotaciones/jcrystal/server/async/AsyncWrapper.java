package jcrystal.server.async;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.server.async.Async;
@SuppressWarnings("all")
public class AsyncWrapper implements Async{
	private final JAnnotation anotacion;
	public AsyncWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	public String name(){
		return (String)anotacion.values.get("name");
	}
	public void name(String value){
		anotacion.values.put("name", value);
	}
	public boolean timeable(){
		return Boolean.parseBoolean((String)anotacion.values.get("timeable"));
	}
	public void timeable(boolean value){
		anotacion.values.put("timeable", Boolean.toString(value));
	}
	public boolean namabled(){
		return Boolean.parseBoolean((String)anotacion.values.get("namabled"));
	}
	public void namabled(boolean value){
		anotacion.values.put("namabled", Boolean.toString(value));
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.server.async.Async.class;
	}
	@Override public String toString(){
		return "@jcrystal.server.async.Async("+"namabled="+namabled()+", "+"name="+name()+", "+"timeable="+timeable()+")";
	}
}

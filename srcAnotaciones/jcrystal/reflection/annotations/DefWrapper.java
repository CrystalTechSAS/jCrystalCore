package jcrystal.reflection.annotations;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.annotations.Def;
@SuppressWarnings("all")
public class DefWrapper implements Def{
	private final JAnnotation anotacion;
	public DefWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	public boolean B(){
		return Boolean.parseBoolean((String)anotacion.values.get("B"));
	}
	public void B(boolean value){
		anotacion.values.put("B", Boolean.toString(value));
	}
	public double D(){
		return Double.parseDouble((String)anotacion.values.get("D"));
	}
	public void D(double value){
		anotacion.values.put("D", Double.toString(value));
	}
	public int I(){
		return Integer.parseInt((String)anotacion.values.get("I"));
	}
	public void I(int value){
		anotacion.values.put("I", Integer.toString(value));
	}
	public String str(){
		return (String)anotacion.values.get("str");
	}
	public void str(String value){
		anotacion.values.put("str", value);
	}
	public long L(){
		return Long.parseLong((String)anotacion.values.get("L"));
	}
	public void L(long value){
		anotacion.values.put("L", Long.toString(value));
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.annotations.Def.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.annotations.Def("+"B="+B()+", "+"D="+D()+", "+"I="+I()+", "+"L="+L()+", "+"str="+str()+")";
	}
}

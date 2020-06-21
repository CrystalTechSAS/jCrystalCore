package jcrystal.reflection.annotations;
import java.lang.annotation.Annotation;
import jcrystal.types.JAnnotation;
import jcrystal.reflection.annotations.FullJsonImport;
@SuppressWarnings("all")
public class FullJsonImportWrapper implements FullJsonImport{
	private final JAnnotation anotacion;
	public FullJsonImportWrapper(JAnnotation anotacion){
		this.anotacion = anotacion;
	}
	@Override public Class<? extends Annotation> annotationType(){
		return jcrystal.reflection.annotations.FullJsonImport.class;
	}
	@Override public String toString(){
		return "@jcrystal.reflection.annotations.FullJsonImport()";
	}
}

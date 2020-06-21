package jcrystal.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.TreeMap;

import jcrystal.types.IJType;
import jcrystal.types.JPackage;
import jcrystal.types.JType;
import jcrystal.types.loaders.IJClassLoader;

public class IJServerClassLoader implements IJClassLoader{

	@Override
	public IJType load(Class<?> clase, Type genericType) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TreeMap<String, IJType> getLoadedClasses() {
		throw new UnsupportedOperationException();
	}

	@Override
	public TreeMap<String, JPackage> getLoadedPackages() {
		throw new UnsupportedOperationException();
	}

	@Override
	public IJType forName(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public JPackage packageForName(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void load(IJType type) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void load(JPackage jPackage) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IJClassLoader getParentClassLoader() {
		return null;
	}
	
	@Override
	public boolean subclassOf(IJType jtype, Class<?> clase) {
		if(jtype instanceof JType && ((JType)jtype).isClientType)
			return false;
		try {
			Class<?> c = Class.forName(jtype.name());
			return clase.isAssignableFrom(c);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			//TODO: Hacer mas eficiente esta operacion
		}
		return false;
	}
	
	@Override
	public boolean isAnnotationPresent(JType jtype, Class<? extends Annotation> annotation) {
		if(jtype instanceof JType && ((JType)jtype).isClientType)
			return false;
		try {
			Class<?> c = Class.forName(jtype.name);
			return c.isAnnotationPresent(annotation);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return false;
	}
}

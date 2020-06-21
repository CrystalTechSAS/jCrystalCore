package jcrystal.model.security;

import java.util.Map;
import java.util.TreeMap;

import jcrystal.types.IJType;
import jcrystal.types.JClass;
import jcrystal.types.JMethod;

public class SecurityTokenClass {
	JClass tokenType;
	public final Map<IJType, JMethod> validators = new TreeMap<>();
	public SecurityTokenClass(JClass tokenType) {
		this.tokenType = tokenType;
		tokenType.methods.stream().filter(f->f.name.equals("validate")).forEach(m->{
			if(m.params.size() != 1)
				throw new NullPointerException("Invalid number of params on validate method for class " + tokenType+". Token validators can only have one paramer");
			validators.put(m.params.get(0).type, m);
		});
	}
}

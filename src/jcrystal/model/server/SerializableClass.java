package jcrystal.model.server;

import java.util.ArrayList;
import java.util.List;

import jcrystal.model.server.db.EntityClass;
import jcrystal.types.JClass;
import jcrystal.types.JVariable;

public class SerializableClass {
	public final JClass jclass;
	private final List<JVariable> fields = new ArrayList<>();
	public SerializableClass(JClass jclass) {
		this.jclass = jclass;
		jclass.attributes.stream().forEach(fields::add);
	}
	public SerializableClass(EntityClass entity) {
		this.jclass = entity.clase;
	}
	public List<JVariable> fields() {
		return fields;
	}
}

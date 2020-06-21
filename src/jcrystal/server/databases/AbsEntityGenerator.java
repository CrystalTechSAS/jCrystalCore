package jcrystal.server.databases;

import java.io.IOException;
import java.util.TreeSet;

import jcrystal.main.data.ClientContext;
import jcrystal.main.data.DataBackends.BackendWrapper;
import jcrystal.model.server.db.EntityClass;
import jcrystal.types.IJType;
import jcrystal.utils.langAndPlats.JavaCode;

public abstract class AbsEntityGenerator {
	public final ClientContext context;
	public EntityClass entidad;
	public BackendWrapper back;
	protected TreeSet<IJType> requiredEnums = new TreeSet<>();

	public AbsEntityGenerator(ClientContext context) {
		this.context = context;
	}
	public final void generate(BackendWrapper back, EntityClass entidad, boolean keysOnly){
		this.entidad = entidad;
		this.back = back;
		generateEntity(keysOnly);
	}
	
	protected abstract void generateEntity(boolean keysOnly);
	
	protected abstract class InternalEntityGenerator extends JavaCode{
		public InternalEntityGenerator(boolean keysOnly) {
			super(1);
			initialize();
			generate(keysOnly);
		}
		private void generate(boolean keysOnly) {
			if(keysOnly) {
				generateKeyClass();
			}else {
				generateClassHeader();
				generateConstructors();
				generateKeyMethods();
				generateSaveMethods();
				generateRetriveMethods();
				generatePropertySetters();
				generateRelations();
				generatePropertyGetters();
				generateQueries();
				generateExtras();
				generateKeyClass();
			}
		}
		protected abstract void initialize();
		protected abstract void generateClassHeader();
		protected abstract void generateConstructors();
		protected abstract void generateKeyMethods();
		protected abstract void generateSaveMethods();
		protected abstract void generateRetriveMethods();
		protected abstract void generatePropertyGetters();
		protected abstract void generateRelations();
		protected abstract void generatePropertySetters();
		protected abstract void generateQueries();
		protected abstract void generateExtras();
		protected abstract void generateKeyClass();
	}
	
	public final boolean isExposed(EntityClass entidad) {
		return back.checkExposed(entidad);
	}
}

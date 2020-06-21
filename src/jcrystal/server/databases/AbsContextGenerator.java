package jcrystal.server.databases;

import jcrystal.configs.server.dbs.DBType;
import jcrystal.main.data.ClientContext;
import jcrystal.utils.StringUtils;
import jcrystal.utils.langAndPlats.JavaCode;

public abstract class AbsContextGenerator extends JavaCode {
	protected final DBType type;
	protected ClientContext context;
	protected AbsContextGenerator(DBType type, ClientContext context) {
		this.type = type;
		this.context = context;
	}
	protected String className() {
		return StringUtils.camelizar(type.name());
	}
	public void generate() {
		$("package jcrystal.context;");
		String extension = type == DBType.GOOGLE_DATASTORE ? " extends jcrystal.context.DataStoreContext" : ""; 
		$("public class " + className() + extension, ()->{
			generateContent();
		});
		context.output.exportFile(this, "jcrystal/context/"+StringUtils.camelizar(type.name())+".java");
	}
	protected abstract void generateContent();
}

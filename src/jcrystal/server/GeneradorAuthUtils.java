package jcrystal.server;

import jcrystal.entity.types.security.FacebookAccount;
import jcrystal.entity.types.security.FirebaseAccount;
import jcrystal.entity.types.security.GoogleAccount;
import jcrystal.main.data.ClientContext;
import jcrystal.model.server.db.EntityClass;
import jcrystal.utils.langAndPlats.JavaCode;

public class GeneradorAuthUtils {
	private ClientContext context;
	public GeneradorAuthUtils(ClientContext context) {
		this.context = context;
	}
	
	public void generate(EntityClass entidad) {
		if(entidad.fields.stream().anyMatch(f->f.isAccountField())) {
			new JavaCode(){{
				$("package "+entidad.clase.getPackageName()+";");
				$("import " + entidad.clase.name()+";");
				$("import static " + entidad.clase.name()+".ENTITY_NAME;");
				$("public interface " + entidad.getTipo()+"AuthBuilder", ()->{
					entidad.fields.stream().filter(f->f.isAccountField()).forEach(campo->{
						//TODO agregar parametros del login?
						if(campo.type().is(FacebookAccount.class))
							$("public "+entidad.getTipo()+" create(final jcrystal.apis.FBUser data);");
						if(campo.type().is(GoogleAccount.class))
							$("public "+entidad.getTipo()+" create(final com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload data);");
						if(campo.type().is(FirebaseAccount.class))
							$("public "+entidad.getTipo()+" create(final com.google.firebase.auth.FirebaseToken decodedToken);");
					});
				});
				context.output.exportFile(this, entidad.clase.getPackageName().replace(".", "/")+"/"+entidad.clase.getSimpleName()+"AuthBuilder.java");
			}};
			
		}
	}
}

package jcrystal.server.web.servlet;

import jcrystal.entity.types.security.EmailAccount;
import jcrystal.entity.types.security.FacebookAccount;
import jcrystal.entity.types.security.FirebaseAccount;
import jcrystal.entity.types.security.GoogleAccount;
import jcrystal.entity.types.security.GoogleAccountKeys;
import jcrystal.entity.types.security.Password;
import jcrystal.entity.types.security.PhoneAccount;
import jcrystal.entity.types.security.UsernameAccount;
import jcrystal.main.data.ClientContext;
import jcrystal.model.server.db.EntityField;
import jcrystal.model.server.db.EntityClass;
import jcrystal.model.web.JCrystalWebService;
import jcrystal.model.web.JCrystalWebServiceParam;
import jcrystal.security.SignInInfo;
import jcrystal.types.JClass;
import jcrystal.utils.StringUtils;
import jcrystal.utils.langAndPlats.AbsCodeBlock;

public class UtilsGeneradorAuth {
	public static void generateLoginUser(AbsCodeBlock code, JCrystalWebService metodo, JCrystalWebServiceParam param) {
		ClientContext context = metodo.context;
		EntityClass entidad = context.data.entidades.get(param.p.type());
		String tipo = entidad.clase.name();
		EntityField password = entidad.fields.stream().filter(f->f.type().is(Password.class)).findFirst().orElse(null);
		JCrystalWebServiceParam signInInfo = metodo.getParameters().stream().filter(f->f.tipoRuta==null && f.type().is(SignInInfo.class)).findFirst().orElse(null);
		code.new B() {{
			$(tipo+" " + param.nombre + " = null;");
			entidad.iterateKeysAndProperties().filter(f->f.isAccountField()).forEach(campo->{
				$if("$body.has(\"" + campo.fieldName() + "\")",()->{
					$("String " + campo.fieldName() + " = $body.getString(\"" + campo.fieldName() + "\");");
					if(campo.type().is(FacebookAccount.class)) {
						$("final jcrystal.apis.FBUser $data = jcrystal.apis.FBApi.getUserInfo(" + campo.fieldName() + ");");
						$(param.nombre + " = " + entidad.queryClassName() + "." + campo.fieldName()+"($data.id);");
						JClass builder = context.data.auth_builders.get(entidad.clase.getSimpleName());
						if(builder != null)
							$if(param.nombre + " == null",()->{
								if(signInInfo != null) {
									$(signInInfo.name() + ".isSignUp = true;");
								}
								$(param.nombre + " = new "+builder.name+"().create($data);");
							});
						$if(param.nombre + " == null",()->{
							$("throw new jcrystal.http.responses.HttpPreconditionFailed412();");
						});
					}
					else if(campo.type().is(FirebaseAccount.class)) {
						$("com.google.firebase.auth.FirebaseToken $data = com.google.firebase.auth.FirebaseAuth.getInstance().verifyIdToken(" + campo.fieldName() + ");");
						$(param.nombre + " = " + entidad.queryClassName() + "."+campo.fieldName()+"($data.getUid());");
						JClass builder = context.data.auth_builders.get(entidad.clase.getSimpleName());
						if(builder != null)
							$if(param.nombre + " == null",()->{
								if(signInInfo != null) {
									$(signInInfo.name() + ".isSignUp = true;");
									$(signInInfo.name() + ".isEmailVerified = $data.isEmailVerified();");
								}
								$(param.nombre + " = new "+builder.name+"().create($data);");
							});
						$if(param.nombre + " == null",()->{
							$("throw new jcrystal.http.responses.HttpPreconditionFailed412();");
						});
					}
					else if(campo.type().is(GoogleAccount.class)) {
						GoogleAccountKeys keys = campo.f.getAnnotation(GoogleAccountKeys.class);
						if(keys == null)
							throw new NullPointerException("GoogleAccountKeys not defined for field " + campo.fieldName()+" on entity " + entidad.name());
						$("com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier $verifier = new com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier.Builder(com.google.api.client.extensions.appengine.http.UrlFetchTransport.getDefaultInstance(), com.google.api.client.json.jackson2.JacksonFactory.getDefaultInstance()).setAudience(java.util.Collections.singletonList(\"" + keys.tokenId() + "\")).build();");
						$("com.google.api.client.googleapis.auth.oauth2.GoogleIdToken $idToken = $verifier.verify("+campo.fieldName()+");");
						$if("$idToken == null",()->{
							$("throw new jcrystal.http.responses.HttpUnauthorized401();");
						});
						$("com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload $data = $idToken.getPayload();");
						$(param.nombre + " = " + entidad.queryClassName() + "."+campo.fieldName()+"($data.getSubject());");
						JClass builder = context.data.auth_builders.get(entidad.clase.getSimpleName());
						if(builder != null)
							$if(param.nombre + " == null",()->{
								if(signInInfo != null) {
									$(signInInfo.name() + ".isSignUp = true;");
								}
								$(param.nombre + " = new "+builder.name+"().create($data);");
							});
						$if(param.nombre + " == null",()->{
							$("throw new jcrystal.http.responses.HttpPreconditionFailed412();");
						});
					}
					else if(password != null && campo.type().is(UsernameAccount.class, EmailAccount.class)) {
						$("String " + password.fieldName() + " = $body.optString(\"" + password.fieldName() + "\");");
						$(param.nombre + " = " + entidad.queryClassName() + "."+campo.fieldName()+"(" + campo.fieldName() + ");");
						$if(param.nombre + " == null",()->{
							$("throw new jcrystal.http.responses.HttpPreconditionFailed412();");
						});
						$if(param.nombre + " == null || " + password.fieldName() + " == null || !"+param.nombre+".validate"+StringUtils.capitalize(password.fieldName())+"(" + password.fieldName() + ")",()->{
							$("throw new jcrystal.http.responses.HttpUnauthorized401();");
						});
					}
					else if(campo.type().is(PhoneAccount.class)) {
						throw new NullPointerException("No se ha implementado el login con telefono, se debe enviar un sms para confirmar");
					}
					
				});
				
			});
			
		}};
	}
}

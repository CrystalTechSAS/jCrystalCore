package jcrystal.clients;

import java.util.stream.Collectors;

import jcrystal.configs.clients.ClientType;
import jcrystal.main.data.ClientContext;
import jcrystal.utils.StringUtils;
import jcrystal.utils.langAndPlats.JavaCode;

public class ClientTagGenerator {

	public static void generate(ClientContext context) {
		context.input.CLIENT.list.stream().filter(f->f.type != ClientType.ADMIN).forEach(client->{
			new JavaCode(){{
				$("package jcrystal.clients;");
				$("import java.lang.annotation.Retention;");
				$("import java.lang.annotation.RetentionPolicy;");
				$("@Retention(RetentionPolicy.RUNTIME)");
				
				$("public @interface Client" + StringUtils.capitalize(client.id), ()->{
					$("public String id() default \""+client.id+"\";");
					if(client.configs.size() > 1) {
						$("public ClientType" + StringUtils.capitalize(client.id)+" type() default ClientType"+StringUtils.capitalize(client.id)+".Default;");
					}
				});
				context.output.exportFile(this, "jcrystal/clients/Client"+StringUtils.capitalize(client.id)+".java");
			}};
			if(client.configs.size() > 1) {
				new JavaCode(){{
					$("package jcrystal.clients;");
					$("public enum ClientType" + StringUtils.capitalize(client.id), ()->{
						$("Default, "+client.configs.stream().filter(f->f.id()!=null).distinct().map(f->f.id()).collect(Collectors.joining(", "))+";");
					});
					context.output.exportFile(this, "jcrystal/clients/ClientType"+StringUtils.capitalize(client.id)+".java");
				}};
			}
		});
		new JavaCode(){{
			$("package jcrystal.clients;");
			$("import java.lang.annotation.Retention;");
			$("import java.lang.annotation.RetentionPolicy;");
			$("import jcrystal.json.JsonLevel;");
			$("@Retention(RetentionPolicy.RUNTIME)");
			
			$("public @interface ClientLevel", ()->{
				context.input.CLIENT.list.stream().map(client->client.id).distinct().forEach(client->{
					$("public JsonLevel "+client+"() default JsonLevel.DEFAULT;");					
				});
				
			});
			context.output.exportFile(this, "jcrystal/clients/ClientLevel.java");
		}};
		new JavaCode(){{
			$("package jcrystal.clients;");
			$("public enum ClientId", ()->{
				$(context.input.CLIENT.list.stream().map(client->client.id).distinct().collect(Collectors.joining(", ")) + ";");
			});
			context.output.exportFile(this, "jcrystal/clients/ClientId.java");
		}};
	}
}

package jcrystal.clients.android;

import jcrystal.clients.AbsClientGenerator;
import jcrystal.types.IJType;
import jcrystal.types.JClass;
import jcrystal.types.JType;
import jcrystal.types.utils.GlobalTypes;
import jcrystal.types.JVariable;
import jcrystal.reflection.MainGenerator;
import jcrystal.reflection.annotations.Push;
import jcrystal.utils.StringUtils;
import jcrystal.utils.langAndPlats.JavaCode;

import static jcrystal.clients.android.AndroidClient.paquetePush;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
* Created by gasotelo on 2/10/17.
*/
public class GeneradorPushs {
	public static void generarPushClasses(final AbsClientGenerator<?> c) throws IOException {
		for(JClass push : c.context.data.clases_push_entities){
			generarPushClass(c, push);
		}
		if(!c.context.data.clases_push_entities.isEmpty()){
			final JavaCode cliente = new JavaCode(){{
					$("package "+AndroidClient.paquetePush+";");
					$(  "import android.app.NotificationManager;\n" +
					"import android.app.PendingIntent;\n" +
					"import android.content.Context;\n" +
					"import android.content.Intent;\n" +
					"import android.media.RingtoneManager;\n" +
					"import android.net.Uri;\n" +
					"import android.support.v4.app.NotificationCompat;\n" +
					"import android.util.Log;\n" +
					"import com.google.firebase.messaging.FirebaseMessagingService;\n" +
					"import com.google.firebase.messaging.RemoteMessage;\n" +
					"import android.support.annotation.DrawableRes;");
					$("public abstract class JCrystalMessagingService extends FirebaseMessagingService", ()->{
						$("@Override");
						$("public final void onMessageReceived(RemoteMessage message)", ()->{
							$("java.util.Map<String,String> data= message.getData();");
							$("String tipo = data.get(\"t\");");
							$if("tipo==null", ()->{ });
							for(JClass push : c.context.data.clases_push_entities){
								$else_if("tipo.equals(\"" + push.getSimpleName() + "\")", ()->{
									$("String title = message.getNotification().getTitle();");
									$("String body = message.getNotification().getBody();");
									$("this.on"+StringUtils.capitalize(push.getSimpleName())+"(new "+push.getSimpleName()+"(title, body, data));");
								});
							}
						});
						for(JClass push : c.context.data.clases_push_entities){
							$("public abstract void on" + StringUtils.capitalize(push.getSimpleName())+"(" + push.getSimpleName() + " event);");
						}
						$("protected void showForegroundNotification(String title, String message, @DrawableRes int icon) {\n" +
						"       Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);\n" +
						"       NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)\n" +
						"           .setContentTitle(title)\n" +
						"           .setContentText(message)\n" +
						"           .setAutoCancel(true)\n" +
						"           .setSound(defaultSoundUri);\n" +
						"       if(icon != 0)notificationBuilder.setSmallIcon(icon);\n" +
						"       NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);\n" +
						"       notificationManager.notify(0, notificationBuilder.build());\n" +
						"   }");
					});
			}};
			c.exportFile(cliente, paquetePush.replace(".", File.separator) + File.separator + "JCrystalMessagingService.java");
		}
	}
	private static void generarPushClass(final AbsClientGenerator<?> c, JClass push) throws IOException {
		final JavaCode cliente = new JavaCode(){{
				$("package "+AndroidClient.paquetePush+";");
				$("public final class " + push.getSimpleName(), ()->{
					for(JVariable f : push.attributes){
						$("public final " + f.type().name() + " " + f.name()+";");
					}
					$("public " + push.getSimpleName() + "(String title, String body, java.util.Map<String, String> data)", ()->{
						for(JVariable f : push.attributes){
							if(f.isAnnotationPresent(Push.PushTitle.class))
								$("this." + f.name()+" = title;");
							else if(f.isAnnotationPresent(Push.PushBody.class))
								$("this." + f.name()+" = body;");
							else if(f.type().is(String.class))
								$("this." + f.name()+" = data.get(\""+f.name()+"\");");
							else if(f.type().is(int.class))
								$("this." + f.name()+" = data.containsKey(\"" + f.name() + "\") ? Integer.parseInt(data.get(\""+f.name()+"\")) : 0;");
							else if(f.type().isPrimitive())
								$("this." + f.name()+" = data.containsKey(\"" + f.name() + "\") ? "+StringUtils.capitalize(f.type().getSimpleName())+".parse"+f.type().getSimpleName()+"(data.get(\""+f.name()+"\")) : "+GlobalTypes.defaultValues.get(f.type())+";");
							else
								throw new NullPointerException("Unssuported type");
						}
					});
				});
		}};
		c.exportFile(cliente, paquetePush.replace(".", File.separator) + File.separator + push.getSimpleName() + ".java");
	}
}

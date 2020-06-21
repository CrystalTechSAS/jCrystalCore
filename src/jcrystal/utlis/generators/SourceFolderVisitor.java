package jcrystal.utlis.generators;

import java.io.File;
import java.util.function.BiConsumer;


public class SourceFolderVisitor {

	public static void preCargarClases(File srcFolder, BiConsumer<String, File> consumer){
		preCargarClases(srcFolder, "", false, consumer);
	}
	private static void preCargarClases(File f, String paquete, boolean include, BiConsumer<String, File> consumer){
		if(f.getName().equals("gen") || f.getName().startsWith("AbsManager"))
			return;
		else if (f.isDirectory()) {
			for (File h : f.listFiles()){
				if(include)
					preCargarClases(h, (paquete.isEmpty()?"":(paquete+".")) + f.getName(), true, consumer);
				else
					preCargarClases(h, paquete, true, consumer);
			}
		} else if (f.getName().endsWith(".java") && !paquete.isEmpty()) {
			paquete += "." + f.getName().replace(".java", "");
			consumer.accept(paquete, f);
		}
	}
	
	
	
}

package jcrystal.clients.webadmin;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jcrystal.model.web.JCrystalWebService;
import jcrystal.types.JType;

public class WebAdminUtils {

	public static String getReturnFromReturnType(JCrystalWebService ws) {
		String returnType = ws.getReturnType().getSimpleName();
		
		if(returnType.startsWith("void"))
			return "";
		else if(returnType.startsWith("Tupla"))
			return IntStream.range(0, Integer.parseInt(returnType.replace("Tupla", ""))).mapToObj(f->"$"+f).collect(Collectors.joining(", "));
		else
			return "$0";
	}
}

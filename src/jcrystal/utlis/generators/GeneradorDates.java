package jcrystal.utlis.generators;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import jcrystal.datetime.DateType;
import jcrystal.utils.StringUtils;
import jcrystal.utils.langAndPlats.JavaCode;

public class GeneradorDates {
	public static void main(String[] args) {
		Arrays.stream(DateType.values()).forEach(date->{
			List<String> code = new JavaCode() {{
				$("package jcrystal.datetime;");
				//$("@jcrystal.reflection.annotations.CrystalDate(DateType."+date.name()+")");
				$("public class Crystal"+StringUtils.camelizar(date.name()) + " extends AbsCrystalDate<Crystal"+StringUtils.camelizar(date.name())+">", ()->{
					$("public Crystal"+StringUtils.camelizar(date.name())+"(String text)throws java.text.ParseException",()->{
						$("super(DateType."+date.name()+".FORMAT.parse(text));");
					});
					$("public Crystal"+StringUtils.camelizar(date.name())+"(long time)",()->{
						$("super(new java.util.Date(time));");
					});
					$("@Override public Crystal"+StringUtils.camelizar(date.name())+" create(long time)",()->{
						$("return new Crystal"+StringUtils.camelizar(date.name())+"(time);");
					});
					$("public Crystal"+StringUtils.camelizar(date.name())+"()",()->{
						$("super(new java.util.Date());");
					});
					$("@Override public String format()",()->{
						$("return DateType."+date.name()+".FORMAT.format(date);");
					});
					$("public static String format(java.util.Date date)",()->{
						$("return DateType."+date.name()+".FORMAT.format(date);");
					});
					$("@Override public Crystal" + StringUtils.camelizar(date.name()) + " next()",()->{
						switch (date) {
							case DATE_MILIS:
							case TIME_MILIS:
								$("return add("+1l+"l);");
								break;
							case DATE_SECONDS:
							case TIME_SECONDS:
								$("return add(java.util.GregorianCalendar.SECOND, 1);");
								break;
							case DATE_TIME:
							case TIME:
								$("return add(java.util.GregorianCalendar.MINUTE, 1);");
								break;
							case DATE:
								$("return add(java.util.GregorianCalendar.DAY_OF_YEAR, 1);");
								break;
							case MONTH:
								$("return add(java.util.GregorianCalendar.MONTH, 1);");
								break;
							case YEAR:
								$("return add(java.util.GregorianCalendar.YEAR, 1);");
								break;
							default:
								break;
						}
					});
					$("@Override public Crystal" + StringUtils.camelizar(date.name()) + " prev()",()->{
						switch (date) {
						case DATE_MILIS:
						case TIME_MILIS:
							$("return add(-"+1l+"l);");
							break;
						case DATE_SECONDS:
						case TIME_SECONDS:
							$("return add(java.util.GregorianCalendar.SECOND, -1);");
							break;
						case DATE_TIME:
						case TIME:
							$("return add(java.util.GregorianCalendar.MINUTE, -1);");
							break;
						case DATE:
							$("return add(java.util.GregorianCalendar.DAY_OF_YEAR, -1);");
							break;
						case MONTH:
							$("return add(java.util.GregorianCalendar.MONTH, -1);");
							break;
						case YEAR:
							$("return add(java.util.GregorianCalendar.YEAR, -1);");
							break;
						default:
							break;
					}
					});
					if(date.format.contains("m")) {
						$("public static Crystal"+StringUtils.camelizar(date.name())+" now()",()->{
							$("return new Crystal"+StringUtils.camelizar(date.name())+"();");
						});
					}
					if(date.format.contains("d")) {
						$("public static Crystal"+StringUtils.camelizar(date.name())+" today()",()->{
							$("return new Crystal"+StringUtils.camelizar(date.name())+"(CrystalDateUtils.today().getTimeInMillis());");
						});
						$("public static Crystal"+StringUtils.camelizar(date.name())+" currentWeek()",()->{
							$("return new Crystal"+StringUtils.camelizar(date.name())+"(CrystalDateUtils.currentWeek().getTimeInMillis());");
						});
					}
					if(date.format.contains("M")) {
						$("public static Crystal"+StringUtils.camelizar(date.name())+" currentMonth()",()->{
							$("return new Crystal"+StringUtils.camelizar(date.name())+"(CrystalDateUtils.currentMonth().getTimeInMillis());");
						});
					}
					if(date.format.contains("y")) {
						$("public static Crystal"+StringUtils.camelizar(date.name())+" currentYear()",()->{
							$("return new Crystal"+StringUtils.camelizar(date.name())+"(CrystalDateUtils.currentYear().getTimeInMillis());");
						});
					}
				});
			}}.getCode();
			try {
				Files.write(Paths.get("C:\\Users\\G\\Documents\\GitHub\\jcrystal\\jCrystalJavaUtils\\src\\jcrystal\\datetime\\Crystal"+StringUtils.camelizar(date.name())+".java"), code);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
	}
	
}

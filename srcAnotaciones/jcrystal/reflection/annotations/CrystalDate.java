/* Copyright (C) Germán Augusto Sotelo Arévalo - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Germán Augusto Sotelo Arévalo <gasotelo@crystaltech.co>, December 2018
 */
package jcrystal.reflection.annotations;

import jcrystal.datetime.DateType;
import jcrystal.types.JAnnotation;

public class CrystalDate extends JAnnotation{

	private static final long serialVersionUID = 8833400153451852833L;
	
	private DateType value;
	
	public CrystalDate(DateType value) {
		super(CrystalDate.class);
		this.value = value;
	}
	public DateType value() {
		return value;
	}
}

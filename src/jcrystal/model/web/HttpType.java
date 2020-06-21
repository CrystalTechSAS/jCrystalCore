/* Copyright (C) Germán Augusto Sotelo Arévalo - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Germán Augusto Sotelo Arévalo <gasotelo@crystaltech.co>, December 2018
 */
package jcrystal.model.web;

/**
* Created by gasotelo on 2/11/17.
*/
public enum HttpType {
	GET, POST, PATH, HEADER, SESSION, SERVER;
	public boolean isGetLike() {
		return this == GET; 
	}
	public boolean isPostLike() {
		return this == POST; 
	}
	public boolean isSentByClient() {
		return this.isGetLike() || this.isPostLike(); 
	}
}

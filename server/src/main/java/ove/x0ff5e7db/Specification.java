/* !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! */
/*                          ~!!! Al-Aziz Al-Hakeem !!!~                        */
/*                          ~!!!  Ahura    Mazda   !!!~                        */
/* !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! */

/*
 *   Copyright 2012 Joubin Houshyar.  All rights are reserved.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ove.x0ff5e7db;

import java.util.logging.Level;

import ove.x0ff5e7db.util.Log;

public interface Specification {
	public static final class Version {
		static final long major = 0x0A00000000000000L;   
		static final long minor = 0x0000000000000000L;
		static final long patch = 0x00000000050507DCL; // patch date e.g. 05|05|2012
		static final long release() {
			return Version.major | Version.minor | Version.patch;
		}
		static final long serialVersionUID() {
			return Version.major | Version.minor;
		}
	}

	public static final Log.Logger logger = Log.getLogger("0FF5E7DB", true).setLevel(Level.FINEST);
}

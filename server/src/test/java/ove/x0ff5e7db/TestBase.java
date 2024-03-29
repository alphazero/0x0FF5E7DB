/* !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! */
/*                          ~!!! Al-Aziz Al-Hakeem !!!~                        */
/*                          ~!!!  Ahura    Mazda   !!!~                        */
/* !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! */

/*
 *   Copyright 2012 Joubin Houshyar
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

import java.util.Random;

import ove.x0ff5e7db.util.Log;
import ove.x0ff5e7db.util.Log.Logger;

/**
 */
public class TestBase {
	// ------------------------------------------------------------------------
	// properties
	// ------------------------------------------------------------------------
	protected static final Logger logger = Log.getLogger("test-run");
	protected static final Random rand = new Random(System.currentTimeMillis());
	
	// ------------------------------------------------------------------------
	// utilities - benchmarks
	// ------------------------------------------------------------------------
	protected static final long comprpcycle(long iters, long delta){
		return (long) ((float)delta/iters);
	}
}

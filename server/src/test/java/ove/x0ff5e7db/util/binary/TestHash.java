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

package ove.x0ff5e7db.util.binary;

import java.util.logging.Level;

import org.testng.Assert;
import org.testng.annotations.Test;

import ove.x0ff5e7db.TestBase;

@Test(groups={"server:util", "capability:id"})
public class TestHash extends TestBase {

	// ------------------------------------------------------------------------
	// properties
	// ------------------------------------------------------------------------
	static final int n_bench_cnt = 10000; // TODO: spec in pom


	// TODO: need a meaningful test for hashes that doesn't take forever

	// ------------------------------------------------------------------------
	// test Hash.Providers
	// ------------------------------------------------------------------------
	@Test()
	public void testHashProviders () {
		for(Hash.Provider p : Hash.Provider.values()){
			if(p.provider.bitsize() == 32)
				testHashProvider32bit(p.provider, p.id);
			else if(p.provider.bitsize() == 31)
				testHashProvider31bit(p.provider, p.id);
		}
	}
	public void testHashProvider32bit (Hash provider, String provider_id) {
		doTestHashProvider32bit(provider, provider_id);
	}

	public void testHashProvider31bit (Hash provider, String provider_id) {
		final String t_feature = "hash";
		int h = doTestHashProvider32bit(provider, provider_id);
		Assert.assertTrue((h | 0x7fffffff) == 0x7fffffff, String.format("hi bit should not set : provider:%s.%s", provider_id, t_feature));
	}
	public int doTestHashProvider32bit (Hash provider, String provider_id) {
		final int s_buff = 4096;
		final byte[] b = new byte[s_buff];
		rand.nextBytes(b);
		return provider.hash(b);
	}

	
	// ------------------------------------------------------------------------
	// bench Hash.Providers
	// ------------------------------------------------------------------------
	
	/* 
	 * REVU: generalize this - extract to bench util - low priority
	 * TODO: see above.
	 */
	
	@Test()
	public void testBenchHashProviders () {
		for(Hash.Provider p : Hash.Provider.values()){
			testBenchHashProvider(p.provider, p.id);
		}
	}
	
	public void testBenchHashProvider (Hash provider, String provider_id) {
		final String t_feature = "hash";
		final int s_buff = 4096;
		final byte[] b = new byte[s_buff];
		
		final long t0 = System.nanoTime();
		for(int i=0;i<n_bench_cnt;i++) {
			rand.nextBytes(b);
			@SuppressWarnings("unused")
			int h = provider.hash(b);
		}
		final long tn = System.nanoTime();
		final long delta = tn - t0;
		long rpusec = comprpcycle(n_bench_cnt, delta);

		logger.log(Level.INFO, "delta:%011d nsecs - iters:%09d - opt-lat:%09d nsec : %s.%s( rand( b[%d] ) )", delta, n_bench_cnt, rpusec, provider_id, t_feature, s_buff);
	}

	@Test()
	public void testBenchHashProvidersOnly () {
		for(Hash.Provider p : Hash.Provider.values()){
			testBenchHashProviderOnly(p.provider, p.id);
		}
	}
	public void testBenchHashProviderOnly (Hash provider, String provider_id) {
		final String t_feature = "hash";
		final int s_buff = 4096;
		final byte[] b = new byte[s_buff];
		rand.nextBytes(b);
		
		final long t0 = System.nanoTime();
		for(int i=0;i<n_bench_cnt;i++) {
			provider.hash(b);
		}
		final long tn = System.nanoTime();
		final long delta = tn - t0;
		long rpusec = comprpcycle(n_bench_cnt, delta);

		logger.log(Level.INFO, "delta:%011d nsecs - iters:%09d - opt-lat:%09d nsec : %s.%s( b[%d] )", delta, n_bench_cnt, rpusec, provider_id, t_feature, s_buff);
	}
}

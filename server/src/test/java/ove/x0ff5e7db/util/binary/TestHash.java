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

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.testng.Assert;
import org.testng.annotations.Test;

import ove.x0ff5e7db.util.Log;
import ove.x0ff5e7db.util.Log.Logger;

@Test(groups={"server:util", "capability:id"})
public class TestHash {

	static final Logger logger = Log.getLogger("test-run");
	static final int n_bench_cnt = 10000; // TODO: spec in pom

	Random rand = new Random(System.currentTimeMillis());
	public long comprptu(long iters, long delta, TimeUnit tu_delta, TimeUnit rptu){
		return rptu.convert((long)comprpcycle(iters, delta), tu_delta);
	}
	public long comprpcycle(long iters, long delta){
		return (long) ((float)delta/iters);
	}
	
	// TODO: need a meaningful test for hashes that doesn't take forever
	public void testHashProvider (Hash provider, String provider_id) {
		final int s_buff = 4096;
		final byte[] b = new byte[s_buff];
		rand.nextBytes(b);
		int h = provider.hash(b);
		Assert.assertTrue((h | 0x7fffffff) == 0x7fffffff, String.format("hi bit should not set : provider:%s", provider_id));
	}

	public void testBenchHashProvider (Hash provider, String provider_id) {
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

		logger.log(Level.INFO, "delta:%011d nsecs - iters:%09d - opt-lat:%09d nsec : %s( rand( b[%d] ) )", delta, n_bench_cnt, rpusec, provider_id, s_buff);
	}

	public void testBenchHashProviderOnly (Hash provider, String provider_id) {
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

		logger.log(Level.INFO, "delta:%011d nsecs - iters:%09d - opt-lat:%09d nsec : %s( b[%d] )", delta, n_bench_cnt, rpusec, provider_id, s_buff);
	}
	interface Testing {
		interface HashFn {
			enum Provider {
				MBInt32 (Hash.MBInt32),
				MBUint32 (Hash.MBUint32);
				final public String id;
				final public Hash provider;
				Provider(Hash provider){
					this.provider = provider;
					this.id = String.format("%s.hash", provider.getClass().getSimpleName());
				}
			}
		}
	}
	
	@Test()
	public void testHashProviders () {
		for(Testing.HashFn.Provider p : Testing.HashFn.Provider.values()){
			testHashProvider(p.provider, p.id);
		}
	}
	
	@Test()
	public void testBenchHashProviders () {
		for(Testing.HashFn.Provider p : Testing.HashFn.Provider.values()){
			testBenchHashProvider(p.provider, p.id);
		}
	}
	
	@Test()
	public void testBenchHashProvidersOnly () {
		for(Testing.HashFn.Provider p : Testing.HashFn.Provider.values()){
			testBenchHashProviderOnly(p.provider, p.id);
		}
	}
}

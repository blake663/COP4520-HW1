# COP4520-HW1


usage: <br>
javac SingleThreadedPrimeFinder.java 

java SingleThreadedPrimeFinder



I tried several ways of parallelizing the Sieve of Erotosthenes Algorithm, but was not able to achieve a performance increase. 
In my first attempt, I tried to relax the precedence constraints of the algorithm, without allowing it to mark multiples of composite number. This could be a desireable property, because it could prevent extra writing to the array. To do this, it maintains a window
(k, k^2), of uncheck numbers of known primality, where k is the last number checked. (Any number in (k, k^] must either be prime, or contain a factor in [2,k], whose multiples have been marked) My approach to this is somewhat expensive, but even without any checks, it performed worse than the single threaded solution. There may have also been too much contention on the atomic 
index variables. <br>

For the next attempt, I used a thread pool hoping for an inherent speed boost and also deciced to drop the synchronization window to save the overhead. I didn't notice any performance difference. This approach is still correct because the array is traversed a second time, after the multiples of each number have been marked. 

In my last attempt, I gave each thread its own equal-sized subarray to mangage. This required growing a list of primes in the shared memory which the receiving threads would read from. The benefit to this approach is that the threads wouldn't have to share their memory with other threads and so there should be less latency for each read and write. The growing list of primes is still shared of course, but it's a smaller list at least, and one thread performs all of its writes during an interval where other threads are only reading. 

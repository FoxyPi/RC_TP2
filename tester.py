import os
import math

n_iter = 5

kbyte = 1024

def blockSizeGenerator():
    blocksizelist = range(8*kbyte, 1024*kbyte,8*kbyte)
    for i in blocksizelist:
        yield i
#block_sizes = [20 * kbyte, 50 * kbyte, 100 * kbyte, 200 * kbyte, 500 * kbyte, 1024 * kbyte]

best_size_time = math.inf

bsgenerator = blockSizeGenerator()

for size in bsgenerator:
    current_time = 0
    for x in range(n_iter):
        os.system("java GetFile " + str(size) + " http://localhost:8080/earth.jpg http://localhost:8081/earth.jpg http://localhost:8082/earth.jpg http://localhost:8083/earth.jpg copy > stat.txt")
        with open("stat.txt", "r") as f:
            next(f)
            next(f)
            line = next(f)
            current_time += float(line[len("Total time elapsed (s):\t\t"):])
        os.remove("stat.txt")
    current_time = current_time/n_iter
    if current_time < best_size_time:
        best_size_time = current_time
        best_size = size

print("Best block_size was " + str(best_size) + " with " + str(best_size_time) + "s as the average download time")
      
            
            


import os
import math
import random

n_iter = 3

kbyte = 1024

#def blockSizeGenerator():
 #   blocksizelist = range(8*kbyte, 1024*kbyte,8*kbyte)
  #  for i in blocksizelist:
   #     yield i
block_sizes =[n * kbyte for n in range(15, 20)]
random.shuffle(block_sizes)
print(block_sizes) 

results = {}

best_size_time = math.inf

#bsgenerator = blockSizeGenerator()

def progress_bar(percent):
    max_sharps = 50
    scalePercent = percent * max_sharps
    sharpList = ['#' for x in range(0, int(scalePercent))]
    spaceList = [' ' for x in range(int(scalePercent), max_sharps + 1)]
    bar = "".join(sharpList + spaceList)
    os.system("echo -ne '{}  ({}%)\r'".format(bar, format(percent * 100, ".2f")))

progress_bar(0)

os.system("javac *.java")

current_iter = 0

for size in block_sizes:
    max_iter = len(block_sizes) * n_iter
    current_time = 0
    for x in range(n_iter):
        os.system("java GetFile " + str(size) + " http://localhost:8080/earth.jpg http://localhost:8081/earth.jpg http://localhost:8082/earth.jpg http://localhost:8083/earth.jpg copy > stat.txt")
        with open("stat.txt", "r") as f:
            next(f)
            next(f)
            line = next(f)
            current_time += float(line[len("Total time elapsed (s):\t\t"):])
        os.remove("stat.txt")
        current_iter += 1
        progress_bar(current_iter / max_iter)
    current_time = current_time/n_iter
    results[size] = current_time
    if current_time < best_size_time:
        best_size_time = current_time
        best_size = size

print("Best block_size was " + str(best_size) + " with " + str(best_size_time) + "s as the average download time")
for result in results:
    print(str(result) + " : " + str(results[result]))
      
            
            


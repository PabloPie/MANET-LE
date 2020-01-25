import matplotlib.pyplot as plt
import pandas as pd
import numpy as np
import os
import subprocess

def execute(batches):
    for i, batch in enumerate(batches):
        print("Batch nº"+str(i)+"...")
        processes = [subprocess.Popen(exec, shell=True, stdout=subprocess.DEVNULL) for exec in batch]
        for proc in processes:
            proc.wait()


###########################
    
print("Generating configuration files...\n")
gen = os.system("config/genconfigs.sh")

print("Compiling sources...\n")
os.system("ant jar")

print("Executing simulation with configuration files:")
print("==============================================")
config_files = filter(lambda x: x.endswith(".txt"), os.listdir("config")) # Get config file names ending with .txt
execs = ["ant run -Dconf="+s for s in config_files] # Concat with ant command to run
batches = [execs[x:x+5] for x in range(0, len(execs), 5)] # Divide in batches of 5
execute(batches) ## Execute simulation

###########################

stat_files = os.listdir("stats")
gv_stats = list(filter(lambda x: "globalviewelection" in x, stat_files))
vkt_stats = list(filter(lambda x: "vkt" in x, stat_files))

aggregated_gv = pd.concat((pd.read_csv("stats/" + f) for f in gv_stats))
aggregated_vkt = pd.concat((pd.read_csv("stats/" + f) for f in vkt_stats))

aggregated_gv.to_csv("gv.csv")
aggregated_vkt.to_csv("vkt.csv")

print("Data exported as csv!")
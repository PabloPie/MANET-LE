# MANET-LE

Implementation of two leader election algorithms on top of Peersim(P2P simulator) in the context of Mobile Ad Hoc Networks(MANET): VKT04 and GlobalView Election.

## Requirements

* Ant for building and executing the project
* Python with Pandas for generating stats from a list of configuration files

## Execution

1. Clone the repo
2. Build and generate the jar file
```bash
$ ant jar
 ```
3. Run the simulation with the default config (config.txt) or change the config path
```bash
$ ant run [-Dconf=<yourconfigfile>]
```

Alternatively you can just execute the python script that will generate configurations and execute simulations for every configuration file. A script to generate configuration files and a template can be found in the "configs" folder.

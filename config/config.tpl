network.size 75
random.seed 1
simulation.endtime 1000000

init.i util.Initialization # Ou InitializationStatique

protocol.position PositionProtocolImpl
protocol.position.maxspeed 20
protocol.position.minspeed 5
protocol.position.width 1200
protocol.position.height 1200
protocol.position.pause 20

#control.gmonitor GraphicalMonitor
#control.gmonitor.time_slow 1.002
#control.gmonitor.step 1
#control.gmonitor.from 0
#control.gmonitor.positionprotocol position
#control.gmonitor.neighborprotocol neighbors
#control.gmonitor.monitorableprotocol election
#control.gmonitor.emitter emitter

control.cmonitor ConnexionMonitor
control.cmonitor.from 0
control.cmonitor.step 1
control.cmonitor.positionprotocol position
control.cmonitor.electionprotocol election

protocol.ewatcher EmitterWatcher
protocol.ewatcher.emitter emitter

protocol.emitter EmitterAlt
protocol.emitter.latency 90
protocol.emitter.variance 0
protocol.emitter.scope $SCOPE

protocol.neighbors NeighborProtocolImpl
protocol.neighbors.heartbeat_period 300
protocol.neighbors.neighbor_timer 400
protocol.neighbors.neighborListener election
protocol.neighbors.emitter ewatcher

# à choisir entre : GlobalViewElection, VKT04Dynamique ou VKT04Statique
protocol.election $ALGO
protocol.election.emitter ewatcher
protocol.election.position position
protocol.election.neighbors neighbors

# Pour VKT04 Dynamique
protocol.election.beacon_interval 50
protocol.election.beacon_max_loss 6
protocol.election.beacon_leader_broadcast 10

# Pour GlobalView
protocol.election.seed_value 0

################# Strategies Initial #######################

initial_position_strategy FullRandom
initial_position_strategy.positionprotocol position

#initial_position_strategy ConnectedRandom
#initial_position_strategy.positionprotocol position
#initial_position_strategy.emitter emitter

#initial_position_strategy InitialPositionRandomConnected
#initial_position_strategy.positionprotocol position
#initial_position_strategy.emitter emitter
#initial_position_strategy.distance_init_min 25
#initial_position_strategy.distance_init_max 75

#initial_position_strategy InitialPositionConnectedRing2
#initial_position_strategy InitialPositionConnectedRing
#initial_position_strategy.positionprotocol position
#initial_position_strategy.emitter emitter

################# Strategies Next #######################

next_destination_strategy FullRandom
next_destination_strategy.positionprotocol position

#next_destination_strategy ConnectedRandom
#next_destination_strategy.positionprotocol position
#next_destination_strategy.emitter emitter

#next_destination_strategy NextDestinationConnectedOneMove
#next_destination_strategy.positionprotocol position
#next_destination_strategy.emitter emitter
#next_destination_strategy.distance_min 15
#next_destination_strategy.distance_max 45

#next_destination_strategy NextDestinationImmobility
#next_destination_strategy.positionprotocol position

#next_destination_strategy NextDestinationRandomPeriodicInitial
#next_destination_strategy.positionprotocol position
#next_destination_strategy.random_dest_period 10

#next_destination_strategy NextPingPongBlock
#next_destination_strategy.positionprotocol position
#next_destination_strategy.emitter emitter


# MANET-LE

## Exercice 1
### Question 6
*FullRandom*
FullRandom attribue une position aléatoire à chaque noeud si c'est la stratégie initiale choisie et réalise un mouvement aléatoire si choisie comme stratégie pour le next.
*ConnectedRandom*
ConnectedRandom place les noeuds et les déplace de manière aléatoire mais de manière à ce qu'ils restent dans le même réseau.
*InitalPositionRandomConnected*
InitalPositionRandomConnected positionne initialement les noeuds de manière aléatoire mais de manière à ce qu'ils soient dans le même réseaux.
*InitalPositionConnectedRing*
InitalPositionConnectedRing positionne les noeuds en forme d'anneau où tous les noeuds sont dans le même réseau.
*NextDestinationConnectedOneMove*
NextDestinationConnectedOneMove déplace un seul noeud de manière à ce que celui-ci reste co
*NextDestinationRandomPeriodicInitial*
Les noeuds bougent de façon aléatoire mais revienne périodiquement (grâce au paramètre random_dest_period) à leur poisition initiale.
*NextDestinationImmobility*
Les noeuds ne bougent pas.
## Premier Algorithme
### Question 1
1. On a la fonction getValue de l'interface ElectionProtocol qui nous retourne la valeure aléatoire calculée préalablement pour chaque node
2. Le simulateur attribue des identifiants entiers uniques à chaque node, et donc ordonnables
3. Les liens entre les nodes sont FIFO grâce aux queues d'évenements, tous les noeuds pouvant communiquer entre eux.
4. Dans le simulateur on peut implémenter la stratégie de mouvement que l'on souhaite, pour arriver à des topologies arbitraires. Le simulateur permet de faire crash des nodes précis.
5. La stratégie d'emission qu'on implémente sur le simulateur ne délivre que les messages des nodes qui se trouvent dans le rayon de communication.
6. Pas de limite sur la taille des buffer des nodes.


## Deuxième algorithme
### Question 5
*L’algorithme utilise des horloges logiques. A quoi servent-elles ? Pourquoi chaque noeud ne peut incrémenter uniquement sa propre horloge ?*

Pour préserver l'ordre partiel et causale des edit

*Pourquoi le knowledge est émis dans sa totalité à la détection de l’arrivée d’un noeud dans le voisinage ?*

Parce que edit est relatif à l'état d'avant, un nouveau node peut ne rien connaitre et doit donc recevoir toute l'information de l'état des noeuds, knowledge est le cas où il se met à jour. Edit ne contient pas suffisamment d'information pour qu'un node soit à jour

*Quel est l’intérêt de créer des edits lors de la déconnexion d’un voisin ou de la réceptiond’un knowledge, au lieu d’envoyer le knowledge dans son ensemble ?*

On réduit la quantité de traffic à faire passer dans un réseau mobile, qui est peut être à débit faible et plus prone à fautes (not reliable)

*Quel est le contenu d’un edit ?*

DISCONNECT:
 <i,-,<j, j.value>, i.clock, i.clock+1 >

KNOWLEDGE:
 i knowledge of p is empty(i ne connait rien à p):
 <p, p.neighbors, -, 0, p.clock>
 s'il connait quelque chose à p:
 <p, added, removed, i.knowledge[p].clock, p.clock>

Pourquoi clock et clock+1?

*Qu’implique l’adjectif reachable ligne 46 ?*

Il existe un chemin entre le noeud et le leader choisi, même si celui-ci est indirect (i.e à travers un autre noeud du réseau)

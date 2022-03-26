# OnionSocket

[![Java CI with Maven](https://github.com/markuSolli/OnionSocket/actions/workflows/main.yml/badge.svg)](https://github.com/markuSolli/OnionSocket/actions/workflows/main.yml)

## Introduction
OnionSocket is a Java library connecting your application to a internet address through onion routers, enabling anonymous interaction. Your network packets gets encrypted in mulitple layers and sent through multiple Onion Nodes, each peeling off one layer of encryption, before reaching your intended destination. By using at least three nodes it becomes difficult to identify the actual sender and reciever of packets.
This library uses no external libraries.

## Implemented functionality
- Layered encryption
- Send and recieve messages up to 512 bytes
- Rndom node assignment for each connection

## Future work
- Client class for messaging with other clients
- Enable larger packet sizes by sending fragments
- Client class for requesting HTTP sites
- Increase key size from 1024b to 2048b

## Limitations
- The keys are only 1024b in size, it should preferably be 2048b
- Messages can only be 512b long
- No automatic reconnection if something fails

## Install instructions
- Run 'mvn package' to get the .jar file
- Either use this as a library, or run the .jar file with one of the main classes as argument to try the programs that are included
  - router/Distributor: Host a distributor server

## How to use
- Import the OnionSocket library
- Construct an OnionSocket with the address you want to communicate with, a chain of random OnionNodes gets set up automatically
- Use send() to send data to the destination address, and read() to read incoming data
- Close the connection by calling close()

### How to test
- Run 'mvn test' to run through all unit tests

### How to use - locally
Without an established network you will need to provide distribution and onion nodes yourself before starting.
- Run an instance of the NodeDistributor
- Run at least three instances of OnionNode, with the Distributor-address specified

### How to contribute
If you want to contribute to an established network, you can run an Onion Node to further increase traffic capacity and network anonimity.
- Run an instance of OnionNode, with the address of the established networks Distributor-address specified

## External code and information
- CI .yml code from [Githubs Starter Workflows](https://github.com/actions/starter-workflows/blob/main/ci/maven.yml)
- Diffie-Hellman Key Exchange from [Java Cryptography Architecture (JCA) Reference Guide, Appendix D: Sample programs](https://docs.oracle.com/javase/7/docs/technotes/guides/security/crypto/CryptoSpec.html#DH2Ex)
- Information on onion routing from [Computerphile, How TOR Works](https://www.youtube.com/watch?v=QRYzre4bf7I)
- Information on Diffie-Hellman Key Exchange from [Computerphile, Secret Key Exchange (Diffie-Hellman)](https://www.youtube.com/watch?v=NmM9HA2MQGI)

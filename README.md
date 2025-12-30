ABSTRACT    


HopeNet is an infrastructure-less offline communication system designed to enable reliable messaging during disaster and emergency situations where conventional communication networks such as cellular services and the internet become unavailable. The system focuses on peer-to-peer device communication using short-range wireless technologies, allowing users to exchange text and voice messages without relying on centralized servers or external infrastructure.

This project is particularly useful for civilians, rescue teams, and emergency responders operating in network-disrupted environments, as it ensures continuity of communication when traditional systems fail. HopeNet employs device discovery, peer-to-peer connectivity, message encryption, and multi-hop routing to form a resilient mesh network capable of extending communication beyond direct device range. Store-and-forward mechanisms are incorporated to support message delivery even when end-to-end connectivity is temporarily unavailable.

The core functionality includes offline user registration, automatic device discovery, secure message encryption and decryption, multi-hop message forwarding, temporary message storage, and Time-To-Live (TTL) based message expiry to maintain privacy and storage efficiency. Message notifications and chat history management enhance usability while preserving the lightweight nature of the system.

HopeNet is implemented using mobile-oriented peer-to-peer communication techniques, local storage (SQLite), and cryptographic methods to ensure security and reliability. By integrating concepts from Mobile Ad Hoc Networks (MANETs), Wireless Mesh Networks, and Delay Tolerant Networking, the system provides a practical and scalable solution for emergency communication. This project demonstrates how decentralized, offline communication platforms can significantly improve information exchange, coordination, and safety during critical situations.

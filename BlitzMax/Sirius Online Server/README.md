Sirius Online Server
=======================

This project was supposed to be the official server software for Sirius Online, before the idea of multiplayer was dumped completely. Talk about sticking to the project plan. Oh wait, there was none. Why did I join that trainwreck of a team. Anyway, the server was designed to be somewhat efficient at its job of calculating and transferring. It supports up to 65535 players and will re-use Ids where possible instead of constantly incrementing. This makes memory management really easy, but join times are sometimes higher than normal. The project uses Xaymar.IOQueue, something which no longer exists. All it did was store packets to be sent off at a later point in time. Networking was done using UDP and TCP, i think. No idea anymore, don't care either, I'm now using Unreal Engine 4 for game development.

License
=======
Sirius Online Server by Michael Fabian Dirks is licensed under the Creative Commons Attribution-ShareAlike 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by-sa/4.0/.
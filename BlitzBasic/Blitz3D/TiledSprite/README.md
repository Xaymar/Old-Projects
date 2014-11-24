TiledSprite
=======================

Blitz3D didn't have a UI solution that worked well. There was Draw3D (& Draw3D2) which claimed to solve it, but in turn caused more issues than I had before it. Mainly the stupid Z-Ordering in Draw3D was annoying, as it didn't solve anything except add more data into RAM and VRAM. And it didn't work well for many things, such as skinned windows with stretched or repeating background, etc.
This library adds some neat little functions that allow you to create a "Tiled Sprite". It does that by cutting up the original texture using new vertices instead of adding more textures to memory or using a fancy shader.

License
=======
TiledSprite by Michael Fabian Dirks is licensed under the Creative Commons Attribution-ShareAlike 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by-sa/4.0/.
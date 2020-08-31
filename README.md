_This release is part of the source code release as outlined in [this announcement](https://www.spigotmc.org/threads/deathmessagesprime.48322/page-53#post-3933244)._

# DMPInventoryPlayerInfo for DeathMessagesPrime

This plugin adds the %inventory% and %inventorykiller% tags to DeathMessagesPrime, after which they can be added to death messages. The text they appear as can be configured within the configuration of this plugin.

Clicking %inventory% will show the player's inventory at the time of death, including the hotbar (bottom), armor slots (top left), offhand slot (top middle), list of potion effects active at the time (top right, potion) and the amount of damage that caused the death (top right, redstone).

%inventorykiller% does the same action, but for the inventory of the player that acted as the killer (if the player wasn't killed by a player, the tag will be replaced with nothing). Instead of the damage being shown in the top-right, it shows the amount of health the killer had at the time of the kill.

There are also the tags/placeholders %plrtaginv%, %victiminv%, %killerinv% and %killer2inv%. These show up as the vanilla DMP %plrtag%, %victim%, %killer% and %killer2% respectively, but can be clicked to open the inventory of that player.

There is some extra customizability: hunger and XP items exist, but are hidden by default. See the configuration on details how to enable them as well as how to customize the other items and how they appear.

Here is an example, with a sample inventory:

![Inventory example](https://i.imgur.com/Y8Lzgnu.png)

...which will appear like this:

![Inventory example when viewed through link](https://i.imgur.com/avaNhmQ.png)

with the potion list provided in the tooltip of the potion in the top right and the amount of damage (or the killer's health) in the name of the redstone item in the top right. 

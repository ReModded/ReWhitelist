# ReWhitelist
A simple, but still very powerful plugin that allows you to restrict access to the particular server or an entire network. Allows you to add many different lists, with ability to turn them on and off as needed. 
For example, you can have a list with all administrators who can access the server at all times, and a list with testers who have access only for testing. 
You no longer need to add or remove users from one whitelist, you just need to enable or disable whitelist with testers in it.

## Download
You can download the latest version of the plugin from the [Modrinth](https://modrinth.com/plugin/rewhitelist) or [Hangar](https://hangar.papermc.io/ReModded/ReWhitelist).

## Issues & suggestions
If you have any issues or suggestions, please report them in the [issues](https://github.com/ReModded/ReWhitelist/issues) section.

## Description
This plugin allows you to create and manage multiple whitelists.

By default all whitelists are disabled.<br>
When the default whitelist is enabled, all players are forbidden to enter, unless any other enabled whitelist allows entrance.<br>
When whitelist's servers are empty, whitelist protects all servers, otherwise whitelist protects only specified servers.<br>

Plugin has a built-in integration with [Floodgate](https://github.com/GeyserMC/Floodgate) to allow players to join the server with their Floodgate accounts.

## Commands & permissions
Main command is `/whitelist` which allows you to manage whitelists.

| Argument                                                                    | Description                             | Permission                                                                                                                    |
|-----------------------------------------------------------------------------|-----------------------------------------|-------------------------------------------------------------------------------------------------------------------------------|
| /whitelist reload                                                           | Reload a whitelist configuration.       | rewhitelist.command.whitelist.reload                                                                                          |
| /whitelist create `<group>`                                                 | Create new whitelist group.             | rewhitelist.command.whitelist.create                                                                                          |
| /whitelist `[group]` add `<uuid/nick/regex/group/permission/...>` `<value>` | Add player entry to the whitelist.      | rewhitelist.command.whitelist.add                                                                                             |
| /whitelist `[group]` remove `<value>`                                       | Remove player from the whitelist.       | rewhitelist.command.whitelist.remove                                                                                          |
| /whitelist `[group]` list                                                   | Show list of all whitelisted players.   | rewhitelist.command.whitelist.list                                                                                            |
| /whitelist `[group]` on                                                     | Enable whitelist.                       | rewhitelist.command.whitelist.on                                                                                              |
| /whitelist `[group]` off                                                    | Disable whitelist.                      | rewhitelist.command.whitelist.off                                                                                             |
| /whitelist `[group]` settings `<servers/...>`                               | Additional settings of whitelist.       | rewhitelist.command.whitelist.settings                                                                                        |
| /whitelist `[group]` settings servers `<add/remove/clear/list/...>`         | Manage servers controlled by whitelist. | rewhitelist.command.whitelist.settings.servers<br/>rewhitelist.command.whitelist.settings.servers.edit (for add/remove/clear) |

`[group]` - is an optional argument, that can be omited. Defaults to: `default`<br>
`<...>` - has multiple options. If omited, available options will be displayed.

#### Example
```
/whitelist on                      # enables whitelist blocking

/whitelist create Stuff            # creates a new group "Stuff"
/whitelist Stuff on                # enables a whitelist to allow entries to passthorugh players
/whitelist Stuff add nick Test123  # adds new entry allowing player with nick "Test123" to enter

/whitelist create Testers                 # creates a new group "Testers"
/whitelist Testers add permission Tester  # adds new entry allowing player with permission "Tester" to enter

# due to fact that "Testers" group wasn't enabled, players with permission "Tester" sill won't be able to enter
/whitelist Testers on                     # enables "Testers" group
```

## Configuration file
Example config file:
[config.toml](/src/main/resources/config.toml)

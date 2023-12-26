# ReWhitelist
A simple, but still very powerful plugin that allows you to restrict access to the server. Allows you to add many different lists, with the option to turn them on and off as needed. For example, you can have a list with all administrators who have access to the server at all times, and a list with testers who have access only for testing. You no longer need to add or remove users from one whitelist, you just need to enable or disable the whitelist with testers on it.
## Commands & permissions
Main command is `/whitelist` which display all arguments or you can find them below.

| Argument                                                                | Description                           | Permission                           |
|-------------------------------------------------------------------------|---------------------------------------|--------------------------------------|
| /whitelist reload                                                       | Reload a whitelist configuration.     | rewhitelist.command.whitelist.reload |
| /whitelist create `<group>`                                             | Create new whitelist group.           | rewhitelist.command.whitelist.create |
| /whitelist `[group]` add `<uuid/nick/regex/group/permission>` `<value>` | Add player to the whitelist.          | rewhitelist.command.whitelist.add    |
| /whitelist `[group]` remove `<value>`                                   | Remove player from the whitelist.     | rewhitelist.command.whitelist.remove |
| /whitelist `[group]` list                                               | Show list of all whitelisted players. | rewhitelist.command.whitelist.list   |
| /whitelist `[group]` on                                                 | Enable whitelist.                     | rewhitelist.command.whitelist.on     |
| /whitelist `[group]` off                                                | Disable whitelist.                    | rewhitelist.command.whitelist.off    |

`[group]` - is an optional argument, that can be omited. Defaults to: `default`

#### Example
```
/whitelist on                      # enables whitelist blocking

/whitelist create Stuff            # creates a new group "Stuff"
/whitelist Stuff on                # enables a whitelist to allow entries to passthorugh players
/whitelist Stuff add nick Test123  # adds new entry allowing player with nick "Test123" to enter
```

## Configuration file
Example config file:
config.toml
```toml
[messages]
deny = "&cYou're not invited to the party..."
```
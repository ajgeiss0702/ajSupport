# ajSupport
Discord bot for my plugin support discord.

## Running a test bot

### Windows
No scripts are available for windows
(unless you use something like git bash. then follow the mac/linux instructions)

### Mac/linux
Run:
```
./start-dev <token>
```
Make sure to replace `<token>` with the token of your test bot.

Make sure to ***NEVER*** commit your token to the repository.
If you accidently do, make sure to regen the token immedietly.


## Building
Note that if you use the start script, it will build it for you.
### Windows
In cmd
```
gradlew.bat clean shadowJar
```
### Mac/linux
In a command line
```
./gradlew clean shadowJar
```

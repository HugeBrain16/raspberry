# Raspberry

![screens](https://i.imgur.com/sbK6k7y.jpeg)

A tiny TCP chat server (made for use with netcat/ncat/socat and other similar tools)

> [!TIP]
> It's recommended to run any client through **rlwrap** for the best experience. For example with netcat `rlwrap nc <host> <port>`

Features:
- Basic chat functionality (multi-threaded)
- Choose username (supports user mention highlight f.e. @user)
- Join any channel (`/join <channel>`)
- Color customization (`/color <color>`)
- Whisper to a user (`/whisper <user>`)

## How to connect

- netcat: `nc <host> <port>`
- socat: `socat - TCP:<host>:<port>`
- ncat: `ncat <host> <port>`
- telnet: `telnet <host> <port>`
- curl: `curl --no-progress-meter -N -T . telnet://<host>:<port>`
- websocat: `websocat --exit-on-eof - tcp:<host>:<port>`

## How to run/build

Requires Java 8+

### Run in project environment
```sh
./gradlew run --no-daemon
```

### Build & run jar
```sh
./gradlew shadowJar
java -jar app/build/libs/raspberry.jar
```

## How to connect to RCON

> [!IMPORTANT]
> RCON login is disabled if RASPBERRY_RCON_PASSWORD is not set.

Connect to the server normally. When asked for username, type "rcon" (case insensitive). Enter RCON password.

## How to setup SSL server

You need to convert whatever certificate files you have to a .p12 or .pfx file (PKCS#12)  
for example with .pem:  
```sh
openssl pkcs12 -export -in cert.pem -inkey privkey.pem -out key.p12
```

Then set the following env vars:
- RASPBERRY_SSL_KEYSTORE to the .p12/.pfx file path  
- RASPBERRY_SSL_PASSWORD if password is required to import the keystore  
- RASPBERRY_SSL_PORT (optional, see Configurations)  
- RASPBERRY_SSL_ONLY to true if you want to only start the ssl server  

## Configurations

Environment variables:
- `RASPBERRY_HOST` default: `0.0.0.0`
- `RASPBERRY_PORT` default: 5555
- `RASPBERRY_SERVER_NAME` default: "Raspberry chat server"
- `RASPBERRY_DEFAULT_CHANNEL` default: "general"
- `RASPBERRY_MAX_USERNAME_LENGTH` default: 16
- `RASPBERRY_MAX_MESSAGE_LENGTH` default: 256
- `RASPBERRY_MAX_MESSAGE_RATE` default: 100 (ms)
- `RASPBERRY_MESSAGE_FREE_THRESHOLD` default: 50
- `RASPBERRY_TIMESTAMP_FORMAT` default: "dd-MM-yyyy HH:mm:ss"
- `RASPBERRY_MOTD` path to motd file
- `RASPBERRY_RCON_PASSWORD` set this to allow RCON login
- `RASPBERRY_SSL_KEYSTORE` path to ssl keystore file
- `RASPBERRY_SSL_PASSWORD` password for the keystore
- `RASPBERRY_SSL_PORT` default: RASPBERRY_PORT + 1
- `RASPBERRY_SSL_ONLY` only start ssl server, non-secure server won't start.

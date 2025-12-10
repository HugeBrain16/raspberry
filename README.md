# Raspberry

![screens](https://i.imgur.com/sbK6k7y.jpeg)

A tiny raw socket chat server (made for netcat)

> [!TIP]
> Run netcat (nc) with **rlwrap** for the best experience `rlwrap nc <host> <port>`

Features:
- Basic chat functionality (multi-threaded)
- Choose username (supports user mention highlight f.e. @user)
- Join any channel (`/join channel>`)
- Color customization (`/color <color>`)
- Whisper to a user (`/whisper <user>`)

## How to run/build

Requires Java 9+

### Run in project environment
```sh
gradle run
```

### Build & run jar
```sh
./gradlew shadowJar
java -jar app/build/libs/raspberry.jar
```

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
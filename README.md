# NightbotCommandsWebServer

An HTTP server coded in Java to provide and serve Twitch APIs to use in Nightbot or other compatible chatbots

[Live Server](https://nightbot.logicism.tv/)

## Nightbot APIs
- Quotes (Add, Remove, List, Grab)
- Followage
- More to come later on...

## Features
- HTTP Server (to be used in conjuction with a reverse proxy for HTTP & HTTPS)
  - Static HTML Resources Server
  - HTML Resources Server

## How to Self-Host

Download the [latest release](https://github.com/LogicismDev/NightbotCommandsWebServer/releases), edit the configuration (config.yml), and use the provided .bat or .sh file to run the server.

Please note that this requires the usage of Java 8 or higher to use the server.

## Modifying HTML Files

You are more than welcome to add your own html files or modify the current ones in the pages directory as specified.

The only mandatory files required in the pages directory are the following below:
- index.html
- callback.html
- callbackError.html
- 404.html
- privacy.html
- terms.html
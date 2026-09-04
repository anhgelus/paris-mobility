# Paris Mobilité

A Jetpack compose Android app replacing *Bonjour RATP*.
Aims to be fast, native and easy to use.

## Features

- Track lines' disruptions
- Monitor next trains for a specific stop
- Display network's map
- Widget displaying lines state and next trains

## Build

This repository doesn't contain the data of the IDF Mobilité network.
Gradle downloads data and generates corresponding Kotlin files at compile time.
Thus, you must have to use your own [PRIM](https://prim.iledefrance-mobilites.fr/en) token during the compilation.
It is read from the environment variable `PRIM_TOKEN`.
You can also set it in a new file called `keys.properties` placed at the root of your project.
This token is never bundled into the application.

The application requires a backend to collect information about the network.
The address and the port of the server must be set in the environment variables `SERVER_HOSTNAME` and `SERVER_PORT`.
You can also set it in `keys.properties`.

## Architecture

The application is located in the module [`app`](app).
The custom Gradle plugin generating the Kotlin files is in [`build-logic/plugin`](build-logic/plugin).
The backend is placed in [`backend`](backend).

The application communicate with the server with a client-Server model.
The protocol is a custom binary protocol based on CBOR.
It reduces the overhead of HTTP and of JSON, which is useful if the network coverage is bad (like in a tunnel).
The formal format is described in [`backend/proto/proto.abnf`](backend/proto/proto.abnf).

## Privacy

Because the application is not fully offline, it sends personnal information like your IP address to the backend.
The default implementation doesn't log anything.

If you are using a third-party packaged application using another backend, it can log your IP address, but the
first-party is not and will never collect personnal data.

## License

The application itself is distributed under AGPL-3.0-only.
The data provided by IDF Mobilité is under multiple [open licenses](https://prim.iledefrance-mobilites.fr/en/licences).

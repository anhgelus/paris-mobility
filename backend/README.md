# Paris Mobilité - backend

Backend application communicating with [PRIM](https://prim.iledefrance-mobilites.fr/marketplace) from IDF Mobilité to
retrieve the data.
It caches the result to reduce API usage and use a custom protocol to limit the overhead.

## Usage

The protocol is described in [proto/proto.abnf](./proto/proto.abnf) and the default port is 1900 (opening's year of the
first metro line in Paris) for encrypted TCP with at least TLS v1.2.
The header contains basic information, such as the kind of request (response, data  requested...), various flags (body
gzipped) and the length of the body.
If not modified by flags, the body is encoded with CBOR, a binary format that works like JSON, but without its huge
overhead.
One connection can be used to send multiple requests to the server.

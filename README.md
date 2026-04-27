## GeyserVoice - Minecraft Proximity Voice Chat Plugin

GeyserVoice is a Java-side bridge for VoiceCraft built around `McApi TCP`. It now ships dedicated runtimes for `Paper`, `Velocity`, and `BungeeCord`, with direct Paper mode and proxy-relay mode for multi-server networks.

### Features

- `McApi TCP` transport only for VoiceCraft communication
- managed VoiceCraft runtime download/startup on direct Paper servers
- proxy relay mode for `Velocity` and `BungeeCord`
- server-side positioning updates from each Paper backend through the proxy

### How It Works

Direct Paper mode:
- install GeyserVoice on the Paper server
- let the plugin download/start VoiceCraft locally if desired
- the Paper plugin connects to VoiceCraft over `McApi TCP`

Proxy mode:
- install GeyserVoice on each Paper backend and on the proxy
- enable `config.server-behind-proxy: true` on the Paper backends
- the proxy plugin owns the VoiceCraft connection
- backend Paper servers stream player snapshots to the proxy through plugin messages

### Getting Started

For detailed instructions on installing and configuring GeyserVoice, please refer to the [Wiki](https://github.com/AvionBlock/GeyserVoice/wiki/) section of this repository.

### Contributing

We welcome contributions from the community to improve and expand the functionality of GeyserVoice. If you have ideas, bug reports, or would like to contribute code, please check out our [Contribution](https://github.com/AvionBlock/GeyserVoice/wiki/Contribution) Guidelines.

### License

GeyserVoice is licensed under the MIT License. Feel free to use, modify, and distribute the plugin in accordance with the terms of the license.

### Status

Current supported runtime paths:
- `Paper -> McApi TCP -> VoiceCraft`
- `Paper -> Proxy relay -> McApi TCP -> VoiceCraft`

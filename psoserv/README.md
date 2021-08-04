# Phantasmal PSO Server

## Configuration

Put a psoserv.conf file in the directory where psoserv will run or pass
the `--config=/path/to/file.conf` parameter to specify a configuration file.
The [HOCON](https://github.com/lightbend/config#using-hocon-the-json-superset) format is used to
describe configurations.

## Proxy

Phantasmal PSO server can proxy any other PSO server. Below is a sample configuration for proxying a
locally running Tethealla server using the standard Tethealla client. Be sure to modify
tethealla.ini and set server port to 22000.

```hocon
proxy: {
  # Default local address used by all proxies, can be overwritten per proxy.
  bindAddress: localhost
  # Default address of the remote server used by all proxies, can be overwritten per proxy.
  remoteAddress: localhost
  # One server configuration per address/port pair that needs to be proxied.
  servers: [
    {
      # Name used for e.g. the logs.
      name: patch_proxy
      # PC or BB, determines the message format encryption cipher used.
      version: PC
      # Local port the proxy will listen on.
      bindPort: 11000
      # Remote port the proxy will connect to.
      remotePort: 21000
    }
    {
      name: patch_data_proxy
      version: PC
      bindPort: 11001
      remotePort: 21001
    }
    {
      name: login_proxy
      version: BB
      bindPort: 12000
      remotePort: 22000
    }
    {
      name: character_proxy
      version: BB
      bindPort: 12001
      remotePort: 22001
    }
    {
      name: ship_proxy
      version: BB
      bindPort: 13000
      remotePort: 5278
    }
    {
      name: block_1_proxy
      version: BB
      bindPort: 13001
      remotePort: 5279
    }
    {
      name: block_2_proxy
      version: BB
      bindPort: 13002
      remotePort: 5280
    }
  ]
}
```

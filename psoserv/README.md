# Phantasmal PSO Server

## Configuration

Put a config.json file in the directory where psoserv will run or pass
the `--config=/path/to/config.json` parameter to specify a configuration file.

## Proxy

Phantasmal PSO server can proxy any other PSO server. Below is a sample configuration for proxying a
locally running Tethealla server using a Tethealla client. Be sure to modify tethealla.ini and set
server port to 22000.

```json
{
    "proxy": {
        "bindAddress": "localhost",
        "remoteAddress": "localhost",
        "servers": [
            {
                "name": "patch_proxy",
                "version": "PC",
                "bindPort": 11000,
                "remotePort": 21000
            },
            {
                "name": "patch_data_proxy",
                "version": "PC",
                "bindPort": 11001,
                "remotePort": 21001
            },
            {
                "name": "login_proxy",
                "version": "BB",
                "bindPort": 12000,
                "remotePort": 22000
            },
            {
                "name": "login_2_proxy",
                "version": "BB",
                "bindPort": 12001,
                "remotePort": 22001
            }
        ]
    }
}
```

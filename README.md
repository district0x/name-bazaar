[![Build Status](https://travis-ci.org/district0x/name-bazaar.svg?branch=master)](https://travis-ci.org/district0x/name-bazaar)

# Name Bazaar

A peer-to-peer marketplace for the exchange of names registered via the Ethereum Name Service.

See at [https://namebazaar.io](https://namebazaar.io)

Smart-contracts can be found [here](https://github.com/district0x/name-bazaar/tree/master/resources/public/contracts/src).

## Start dev server
```bash
# In separete terminal start Ganache blockchain
ganache-cli -p 8549

# In separate terminal start autocompiling smart-contracts
lein auto compile-solidity

# In separate terminal start server REPL
git clone https://github.com/district0x/name-bazaar
lein deps
lein repl
(start-server!)

# In separate terminal run server
node dev-server/name-bazaar.js
```
#### Redeploy smart-contracts and generate mock data
```bash
# In server REPL run:
(name-bazaar.server.dev/redeploy)
```
#### Start server with custom config
Namebazaar uses [district-server-config](https://github.com/district0x/district-server-config) for loading configuration. So you can create `config.json` somewhat like this:
```json
{
  "emailer": {
    "private-key": "25677d268904ea651f84e37cfd580696c5c793dcd9730c415bf03b96003c09e9ef8",
    "print-mode?": true
  },
  "ui": {
    "public-key": "2564e15aaf9593acfdc633bd08f1fc5c089aa43972dd7e8a36d67825cd0154602da47d02f30e1f74e7e72c81ba5f0b3dd20d4d4f0cc6652a2e719a0e9d4c7f10943",
    "use-instant-registrar?": true
  },
  "logging": {
    "level": "info",
    "console?": true
  },
  "web3": {
    "port": 8549
  },
  "endpoints": {
    "port": 6200
  }
}
```
## Start dev UI
```bash
lein repl
(start-ui!)

# In separate terminal run
docker-compose build nginx
docker-compose up nginx

# Visit website at http://localhost:3001
```

## Backend (server) tests:

```
lein doo node "server-tests"
```

The doo runner will autobuild the test and re-run them as the watched files change.
Alternatively:

```
lein cljsbuild once server-tests
node server-tests/server-tests.js
```

## Frontend (browser) tests:

```
lein doo chrome browser-tests
```

It will autobuild browser-test and re-run the tests as the watched files change.
More info: [https://github.com/bensu/doo](doo).

## Development env through nginx:

```
docker-compose build nginx
docker-compose up nginx
```
and start (start-ui!), (start-server!) as usual, but open the site on http://localhost:3001

## Build for production
Following commands are used to build system for production
```bash
lein build-prod-server
lein build-prod-ui
lein build-css

# To build all 3 in parallel use
lein build-prod
```

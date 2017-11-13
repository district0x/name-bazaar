[![Build Status](https://travis-ci.org/district0x/name-bazaar.svg?branch=master)](https://travis-ci.org/district0x/name-bazaar)

# Name Bazaar

A peer-to-peer marketplace for the exchange of names registered via the Ethereum Name Service.

See at [https://namebazaar.io](https://namebazaar.io)

Smart-contracts can be found [here](https://github.com/district0x/name-bazaar/tree/master/resources/public/contracts/src).

## Overriding default-config variables

Config variables are picked up from a JSON file specified as process.ENV variable.
Any key set in config.json overrides the variable with matching key in default-config.
Hierarchical keys are deep-merged.

Example config file:

```
{"sendgrid-api-key" : "SG.uJM-W5OCNkxhyXx0XNTOZY",
          "logging" : {"level"    : "debug"
                        "console" : true}}
```

Setting CONFIG process.ENV variable:

```
CONFIG='/etc/config/config.json' node dev-server/name-bazaar.js
```

Any key is then accessible as:

```
(config/get-config :sendgrid-api-key)
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
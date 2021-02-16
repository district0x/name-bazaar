# Name Bazaar

[![Build Status](https://travis-ci.org/district0x/name-bazaar.svg?branch=master)](https://travis-ci.org/district0x/name-bazaar)

A peer-to-peer marketplace for the exchange of names registered via the Ethereum Name Service.

See at [https://namebazaar.io](https://namebazaar.io)

Smart-contracts can be found [here](https://github.com/district0x/name-bazaar/tree/master/resources/public/contracts/src).

## Starting a dev server with an Ethereum Testnet

In a terminal, start a ganache blockchain

```bash
./run-ganache.sh
```

_(note that this uses docker and will try to pull `trufflesuite/ganache-cli:v6.12.1` image if you don't have it)_

Open another terminal, start autocompiling smart-contracts

```bash
lein auto compile-solidity
```

_(You need to have `solc` with compatible version installed. See `contracts` folder to determine the
correct version)_

Open another terminal, start a repl and build the dev server (with
figwheel repl)

```bash
lein repl
(start-server!)
```

Figwheel will prompt for a connection for the repl instance.

Open another terminal, run the compiled server script, which should
connect to the figwheel repl.

```bash
node dev-server/name-bazaar.js
```
_(If you have problems re-running this command, try removing `dev-server`
folder and try to start the server again)_

#### Redeploy smart-contracts and generate mock data

```clojure
# In the figwheel server REPL run:
(redeploy)
;; and optionally generate sample dev data
(generate-data)
```

Redeployment / Generation can take a long time, please be patient.

#### Start server with custom config

Namebazaar uses [district-server-config](https://github.com/district0x/district-server-config) for loading configuration. So you can create `config.edn` somewhat like this:

```clojure
{:emailer {:private-key "25677d268904ea651f84e37cfd580696c5c793dcd9730c415bf03b96003c09e9ef8"
           :print-mode? true}
 :ui {:public-key "2564e15aaf9593acfdc633bd08f1fc5c089aa43972dd7e8a36d67825cd0154602da47d02f30e1f74e7e72c81ba5f0b3dd20d4d4f0cc6652a2e719a0e9d4c7f10943"
      :use-instant-registrar? true}
 :logging {:level :info
           :console? true}
 :web3 {:port 8549}
 :endpoints {:port 6200}}
```

## Start dev UI

If you wish to connect to the dev server discussed above, open a
separate terminal, and build the client-side ui

```bash
lein repl
(start-ui!)
```

You can then connect to the server through a web browser at http://localhost:4541

### Semantic UI

To build the Semantic UI pieces of the app you need to have `gulp` installed.
Note that `gulp 4.x` does not work, you need a `3.x` version.

`npm install gulp@^3.9.0 --save`

Then use our handy script:

`./semantic.sh build`
or
`./semantic.sh watch`

Depending upon how you'd like to work.

## Start a development UI for client-side development only

If you're only focusing on working with the UI, you can start a UI
interface which connects to the production server using mainnet.

```bash
lein repl
(start-ui! :ui-only? true)
```

In separate terminal, start the supplied docker nginx server

```bash
docker-compose build nginx
docker-compose up nginx

# Visit website at http://localhost:3001
```

**Note: using this client is using the main ethereum network, it is
ill-advised to carry out transactions unless you know what you are doing!**

## Backend (server) tests:

```
lein doo node "server-tests"
```
_(If you have problems running the tests, try to remove `server-tests` directory
and try re-running the tests again)_

The doo runner will autobuild the test and re-run them as the watched files change.
Alternatively:

```
lein cljsbuild once server-tests
node server-tests/server-tests.js
```

## Frontend (browser) tests:

To run browser tests use the following command:
```
lein npm run cypress-open
```

Tests connect to a running app on `http://localhost:4541`. It is recommended to run the
tests with clear ganache network, otherwise the tests will be slower or fail.

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

# To run prod server
node server/name-bazaar.js
```

## Linting and formatting

The purpose of linting and formatting is such that the code is readable and consistent.

We use [ethlint](https://github.com/duaraghav8/Ethlint) for linting solidity files. You
can use `lein npm run ethlint` and `lein npm run ethlint-fix` to run the linter.
<div align="left">
    <a href="https://discord.com/invite/sS2AWYm"><img alt="District0x Discord server" src="https://img.shields.io/discord/356854079022039062?label=district0x&logo=discord"></a>
    <a href="LICENSE"><img alt="LICENSE" src="https://img.shields.io/github/license/district0x/name-bazaar"></a>
    <a href="http://makeapullrequest.com"><img alt="pull requests welcome" src="https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat"></a>
    <a href="(https://travis-ci.org/district0x/name-bazaar"><img alt="Build Status" src="https://travis-ci.org/district0x/name-bazaar.svg?branch=master"></a></p>
</div>

# Name Bazaar

A peer-to-peer marketplace for the exchange of names registered via the Ethereum Name Service.

See at [https://namebazaar.io](https://namebazaar.io)

Smart-contracts can be found [here](https://github.com/district0x/name-bazaar/tree/master/resources/public/contracts/src).

## Starting a dev server

In a terminal, start a ganache blockchain

```bash
./run-ganache.sh
```

Note that this uses docker and will try to pull `trufflesuite/ganache-cli:v6.12.1` image if you don't have it.

Alternatively, you can connect directly to one of ethereum testnet networks - e.g. ropsten. In order to do this, specify correct smart contract addresses and `:web3` properties in the `./config.edn` file. Example config file can be found in `docker-builds/server/config.example.edn`.

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

You can (re)deploy contracts with

```bash
truffle migrate --reset
```

and (optionally) generate some samle dev data from the clojurescript REPL by running the following command:

```clojure
(generate-data)
```

Redeployment / generation can take a long time, please be patient.

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

## Testnet deploy

To run server in docker container use image `district0x/namebazaar-server`, e.g.:

```bash
docker run --name=namebazaar-server \
    --net=host \
    -v /path/to/config.edn:/configs/namebazaar.config.edn \
    district0x/namebazaar-server:latest
```

You can choose between tags `dev`, `latest` (intended for QA deploys) and `release` (intended for production deploy).  As for the config file, you can find an example in `docker-builds/server/config.example.edn`. Of particular interest is providing correct addresses of smart contracts on the blockchain you'll link the app to.

For UI use the `district0x/namebazaar-ui` image:

```bash
docker run --name=namebazaar-ui \
    --net=host \
    district0x/namebazaar-ui:latest
```

Note that there is no passing of config file for UI: currently for any change of UI config you need to build a new image (see the next section). The hardcoded configuration is at `src/name_bazaar/ui/config.cljs`.

### Updating docker images

If you want to build new docker images locally and push them to district0x dockerhub (if you're authorised to do so), run:

```bash
./docker-push.sh env sshkey
```

where

* `env` is `dev`,`qa` or `prod` (the only difference is in how the images will be tagged)
* `sshkey` is path to your private github ssh key, which will be used to download dependencies in a secure manner, not persisting in any build layer

### Deploying Name Bazaar smart contracts

First, you need to specify deployments secrets in `config.edn`. For example:

```clojure
{:truffle {:ropsten {:infuraKey "0ff2cb560e864d078290597a29e2505d"
                     :privateKeys ["0508d5f96e139a0c18ee97a92d890c55707c77b90916395ff7849efafffbd810"]
                     :ensAddress "0x00000000000C2E074eC69A0dFb2997BA6C7d2e1e"
                     :registrarAddress "0x57f1887a8BF19b14fC0dF6Fd9B2acc9Af147eA85"
                     :publicResolverAddress "0x42D63ae25990889E35F215bC95884039Ba354115"
                     :reverseRegistrarAddress "0x6F628b68b30Dc3c17f345c9dbBb1E483c2b7aE5c"}}}
```

Then, you can use `truffle` to deploy the contracts just by running the following command in bash:
```bash
# you can also use `--network mainnet` - see truffle-config.js for deployment details for more information.
truffle migrate --network ropsten
```

## Linting and formatting smart contracts

We use [ethlint](https://github.com/duaraghav8/Ethlint) for linting solidity files. You
can use `lein npm run ethlint` and `lein npm run ethlint-fix` to run the linter.

You can use `lein run-slither` to run [slither](https://github.com/crytic/slither) to
statically analyze the smart contracts. _However, this tool reports many false positives_.

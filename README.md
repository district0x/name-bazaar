[![Build Status](https://travis-ci.org/district0x/name-bazaar.svg?branch=master)](https://travis-ci.org/district0x/name-bazaar)

# Name Bazaar

A peer-to-peer marketplace for the exchange of names registered via the Ethereum Name Service.

Coming Soon to [https://namebazaar.io](https://namebazaar.io)

Smart-contracts can be found [here](https://github.com/district0x/name-bazaar/tree/master/resources/public/contracts/src).

## Overriding default-config variables

Any variable set in process.ENV overrides the variable with matching key in default-config.
Example:

```
PUBLIC_KEY='bla' node dev-server/name-bazaar.js
```

is then accessible as:

```
(config/get-config :public-key)
```

## Frontend (browser) tests: 

```
lein doo chrome browser-tests
```

It will autobuild browser-test and re-run the tests as the watched files change.
More info: [https://github.com/bensu/doo](doo).
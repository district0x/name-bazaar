/**
 * Use this file to configure your truffle project. It's seeded with some
 * common settings for different networks and features like migrations,
 * compilation and testing. Uncomment the ones you need or modify
 * them to suit your project as necessary.
 *
 * More information about configuration can be found at:
 *
 * trufflesuite.com/docs/advanced/configuration
 *
 * To deploy via Infura you'll need a wallet provider (like @truffle/hdwallet-provider)
 * to sign your transactions before they're sent to a remote public node. Infura accounts
 * are available for free at: infura.io/register.
 *
 * You'll also need a mnemonic - the twelve word phrase the wallet uses to generate
 * public/private key pairs. If you're publishing your code to GitHub make sure you load this
 * phrase from a file you've .gitignored so it doesn't accidentally become public.
 *
 */

const HDWalletProvider = require("@truffle/hdwallet-provider")
const edn = require("jsedn")
const fs = require('fs')
const path = require('path')

const CONTRACTS_BUILD_DIR = './resources/public/contracts-build'
const ednConfig = edn.parse(fs.readFileSync('config.edn').toString())

const fromConfig = (path) => {
  try {
    return edn.toJS(edn.atPath(ednConfig, path))
  } catch {
    return undefined
  }
}

const removeColonFromConfigKeys = (value) => {
  if (typeof value === 'object' && !Array.isArray(value)) {
    const res = {}
    Object.keys(value).forEach(key => {
      res[key.replace(':', '')] = removeColonFromConfigKeys(value[key])
    })

    return res
  }

  return value
}

const deleteFolder = function (directoryPath) {
  if (fs.existsSync(directoryPath)) {
    fs.readdirSync(directoryPath).forEach((file) => {
      const curPath = path.join(directoryPath, file)
      if (fs.lstatSync(curPath).isDirectory()) {
        deleteFolder(curPath)
      } else {
        fs.unlinkSync(curPath)
      }
    })
    fs.rmdirSync(directoryPath)
  }
}

// This hack will make sure contracts are always recompiled!
//
// It is necessary because in migrations we are changing the
// contracts bytecode (which is persisted to file system by truffle)
// and will not be recompiled until the source file change.
deleteFolder(CONTRACTS_BUILD_DIR)

module.exports = {
  contracts_directory: './resources/public/contracts',
  contracts_build_directory: CONTRACTS_BUILD_DIR,
  migrations_directory: './resources/public/contracts-migration',
  /**
   * Networks define how you connect to your ethereum client and let you set the
   * defaults web3 uses to send transactions. If you don't specify one truffle
   * will spin up a development blockchain for you on port 9545 when you
   * run `develop` or `test`. You can ask a truffle command to use a specific
   * network from the command line, e.g
   *
   * $ truffle test --network <network-name>
   */
  networks: {
    // Useful for testing. The `development` name is special - truffle uses it by default
    // if it's defined here and no other network is specified at the command line.
    // You should run a client (like ganache-cli, geth or parity) in a separate terminal
    // tab if you use this network and you must also set the `host`, `port` and `network_id`
    // options below to some value.
    development: {
     host: "127.0.0.1",     // Localhost (default: none)
     port: 8549,            // Standard Ethereum port (default: none)
     network_id: "*",       // Any network (default: none)
    },
    goerli: {
      provider: () =>
        new HDWalletProvider({
          privateKeys: fromConfig(":truffle :goerli :privateKeys"),
          providerOrUrl: `wss://goerli.infura.io/ws/v3/${fromConfig(":truffle :goerli :infuraKey")}`
        }),
      network_id: '5',
      networkCheckTimeout: 999999,
      confirmations: 2,
      timeoutBlocks: 200,
      skipDryRun: true,
      deploymentConfig: removeColonFromConfigKeys(fromConfig(":truffle :goerli"))
    },
    mainnet: {
      provider: () =>
        new HDWalletProvider({
          privateKeys: fromConfig(":truffle :mainnet :privateKeys"),
          providerOrUrl: `wss://mainnet.infura.io/ws/v3/${fromConfig(":truffle :mainnet :infuraKey")}`
        }),
      network_id: '1',
      networkCheckTimeout: 10000, // https://github.com/trufflesuite/truffle/issues/3356
      confirmations: 2,    // # of confs to wait between deployments. (default: 0)
      timeoutBlocks: 200,  // # of blocks before a deployment times out  (minimum/default: 50)
      skipDryRun: true,
      deploymentConfig: removeColonFromConfigKeys(fromConfig(":truffle :mainnet"))
    }
  },
  compilers: {
    solc: {
      version: "0.8.4",
    }
  },
}

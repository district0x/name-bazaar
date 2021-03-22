const namehash = require('eth-ens-namehash').hash
const keccak256 = require('js-sha3').keccak_256
const edn = require("jsedn")
const fs = require('fs')
const { Promise } = require('@ungap/global-this')

// flatMap polyfill in nodejs
if (!Array.prototype.flatMap) {
  function flatMap (f, ctx) {
    return this.reduce
    ( (r, x, i, a) =>
        r.concat(f.call(ctx, x, i, a))
      , []
    )
  }
  Array.prototype.flatMap = flatMap
}

const ensLabel = (label) => '0x' + keccak256(label)

const parallel = (...promises) => Promise.all(promises)

const smartContractsTemplate = (contracts) => {
  return `(ns name-bazaar.shared.smart-contracts)
    (def smart-contracts
      ${contracts})`;
}

const linkBytecode = (Contract, placeholder, replacement) => {
  // if (Contract.bytecode.split(placeholder).length <= 1) {
  //   console.log('wtf', Contract.bytecode, placeholder, replacement, Contract.bytecode.split(placeholder).length, Contract.contractName)
  //   throw new Error('stop')
  // }
  placeholder = placeholder.replace('0x', '');
  replacement = replacement.replace('0x', '');
  Contract.bytecode = Contract.bytecode.split(placeholder).join(replacement);
}

const loadArtifacts = (entries) => entries.reduce(
  (acc, {kw, name}) => {
    const artifact = artifacts.require(name)

    // persist the clojurescript keyword and contract name
    artifact.kw = kw;
    artifact.contractName = name

    return {...acc, [name]: artifact}
  }, {}
)

const writeSmartContracts = (artifacts) => {
  // verify that every artifact has been deployed
  Object.values(artifacts).forEach((artifact) => {
    if (!artifact.isDeployed() && !artifact.skippedDeploy) {
      throw new Error(`Artifact ${artifact.contractName} has not been deployed! If this is intended, call 'skipDeploy' on the contract.`)
    }
  })

  const smartContracts = smartContractsTemplate(
      edn.encode(
      new edn.Map(
        Object.values(artifacts).flatMap((artifact) => [
          edn.kw(artifact.kw),
          new edn.Map([
            edn.kw(':name'),
            artifact.contractName,
            edn.kw(':address'),
            artifact.address,
          ])]
        )
      )
    )
  )

  const path = 'src/name_bazaar/shared/smart_contracts.cljs'
  fs.writeFileSync(path, smartContracts)
  console.log(`Successfully modified deployment file on path: ${path}`)
}

const validateDeploymentConfig = (config = {}) => {
  const errorDetails = 'Properties must me specified in config.edn! (See: truffle-config.js)'
  const configJSON = JSON.stringify(config)

  const {privateKeys, infuraKey} = config
  if (typeof infuraKey !== 'string' || !Array.isArray(privateKeys) || privateKeys.length <= 0) {
    throw new Error(`Invalid deployment secrets. Configuration: ${configJSON}. ` + errorDetails)
  }

  const {ensAddress, registrarAddress, publicResolverAddress, reverseRegistrarAddress} = config
  if (typeof ensAddress !== 'string' ||
    typeof registrarAddress !== 'string' ||
    typeof publicResolverAddress !== 'string' ||
    typeof reverseRegistrarAddress !== 'string'
  ) {
    throw new Error(`Missing (at least) one of required smart contract addresses. Config: ${configJSON}.` + errorDetails)
  }
}

const loadedArtifacts = loadArtifacts([
  {kw: ':ens', name: 'ENSRegistry'},
  {kw: ':name-bazaar-registrar', name: 'NameBazaarDevRegistrar'},
  {kw: ':offering-registry', name: 'OfferingRegistry'},
  {kw: ':offering-requests', name: 'OfferingRequests'},
  {kw: ':buy-now-offering', name: 'BuyNowOffering'},
  {kw: ':buy-now-offering-factory', name: 'BuyNowOfferingFactory'},
  {kw: ':auction-offering', name: 'AuctionOffering'},
  {kw: ':auction-offering-factory', name: 'AuctionOfferingFactory'},
  {kw: ':district0x-emails', name: 'District0xEmails'},
  {kw: ':reverse-name-resolver', name: 'NamebazaarDevNameResolver'},
  {kw: ':public-resolver', name: 'NamebazaarDevPublicResolver'},
  {kw: ':reverse-registrar', name: 'NamebazaarDevReverseRegistrar'},
])

const {
  ENSRegistry,
  NameBazaarDevRegistrar,
  OfferingRegistry,
  OfferingRequests,
  BuyNowOffering,
  BuyNowOfferingFactory,
  AuctionOffering,
  AuctionOfferingFactory,
  District0xEmails,
  NamebazaarDevNameResolver,
  NamebazaarDevPublicResolver,
  NamebazaarDevReverseRegistrar
} = loadedArtifacts

const emergencyMultisigPlaceholder = "DeEDdeeDDEeDDEEdDEedDEEdDEeDdEeDDEEDDeed".toLowerCase()
const offeringPlaceholder = "beefbeefbeefbeefbeefbeefbeefbeefbeefbeef".toLowerCase()
const ensPlaceholder = "314159265dD8dbb310642f98f50C066173C1259b".toLowerCase()
const offeringRegistryPlaceholder = "fEEDFEEDfeEDFEedFEEdFEEDFeEdfEEdFeEdFEEd".toLowerCase()

module.exports = async function (deployer, network, accounts) {
  const deploy = async (Contract, ...args) => {
    await deployer.deploy(Contract, ...args)
    return await Contract.deployed()
  }

  const skipDeploy = (Contract, address) => {
    Contract.address = address
    Contract.skippedDeploy = true
  }

  if (network === 'development') {
    const [ens, namebazaarDevNameResolver] = await parallel(
      deploy(ENSRegistry),
      deploy(NamebazaarDevNameResolver)
    )

    const [nameBazaarDevRegistrar] = await parallel(
      deploy(NameBazaarDevRegistrar, ens.address, namehash('eth')),
      deploy(NamebazaarDevPublicResolver, ens.address),
      deploy(NamebazaarDevReverseRegistrar, ens.address, namebazaarDevNameResolver.address)
    )
    await ens.setSubnodeOwner(namehash(''), ensLabel('eth'), nameBazaarDevRegistrar.address)
  } else {
    const config = deployer.networks[network].deploymentConfig
    validateDeploymentConfig(config)

    skipDeploy(ENSRegistry, config.ensAddress)
    skipDeploy(NamebazaarDevNameResolver, "0x0000000000000000000000000000000000000000") // this address is not important
    skipDeploy(NameBazaarDevRegistrar, config.registrarAddress)
    skipDeploy(NamebazaarDevPublicResolver, config.publicResolverAddress)
    skipDeploy(NamebazaarDevReverseRegistrar, config.reverseRegistrarAddress)
  }

  const emergencyMultisigAccount = accounts[0]
  const offeringRegistry = await deploy(OfferingRegistry, emergencyMultisigAccount)
  linkBytecode(BuyNowOffering, emergencyMultisigPlaceholder, emergencyMultisigAccount)
  linkBytecode(BuyNowOffering, ensPlaceholder, ENSRegistry.address)
  linkBytecode(BuyNowOffering, offeringRegistryPlaceholder, offeringRegistry.address)
  linkBytecode(AuctionOffering, emergencyMultisigPlaceholder, emergencyMultisigAccount)
  linkBytecode(AuctionOffering, ensPlaceholder, ENSRegistry.address)
  linkBytecode(AuctionOffering, offeringRegistryPlaceholder, offeringRegistry.address)
  const [offeringRequests, buyNowOffering, auctionOffering] = await parallel(
    deploy(OfferingRequests),
    deploy(BuyNowOffering, emergencyMultisigAccount),
    deploy(AuctionOffering, emergencyMultisigAccount),
    deploy(District0xEmails)
  )

  linkBytecode(BuyNowOfferingFactory, offeringPlaceholder, buyNowOffering.address)
  linkBytecode(AuctionOfferingFactory, offeringPlaceholder, auctionOffering.address)
  const [buyNowOfferingFactory, auctionOfferingFactory] = await parallel(
    deploy(
      BuyNowOfferingFactory,
      ENSRegistry.address,
      offeringRegistry.address,
      offeringRequests.address
    ),
    deploy(
      AuctionOfferingFactory,
      ENSRegistry.address,
      offeringRegistry.address,
      offeringRequests.address
    )
  )

  await parallel(
    offeringRegistry.setFactories([buyNowOfferingFactory.address, auctionOfferingFactory.address], true),
    offeringRequests.setFactories([buyNowOfferingFactory.address, auctionOfferingFactory.address], true)
  )

  writeSmartContracts(loadedArtifacts)
};

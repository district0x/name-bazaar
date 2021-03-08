const namehash = require('eth-ens-namehash').hash
const keccak256 = require('js-sha3').keccak_256
const edn = require("jsedn")
const fs = require('fs')

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

const smartContractsTemplate = (contracts) => {
  return `(ns name-bazaar.shared.smart-contracts)
    (def smart-contracts
      ${contracts})`;
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
    if (!artifact.isDeployed()) throw new Error(`Artifact ${artifact.contractName} has not been deployed!`)
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

const loadedArtifacts = loadArtifacts([
  {kw: ':ens', name: 'ENSRegistry'},
  {kw: ':name-bazaar-registrar', name: 'NameBazaarRegistrar'},
  {kw: ':offering-registry', name: 'OfferingRegistry'},
  {kw: ':offering-requests', name: 'OfferingRequests'},
  {kw: ':buy-now-offering', name: 'BuyNowOffering'},
  {kw: ':buy-now-offering-factory', name: 'BuyNowOfferingFactory'},
  {kw: ':auction-offering', name: 'AuctionOffering'},
  {kw: ':auction-offering-factory', name: 'AuctionOfferingFactory'},
  {kw: ':district0x-emails', name: 'District0xEmails'},
  {kw: ':reverse-name-resolver', name: 'NamebazaarDevNameResolver'},
  {kw: ':public-resolver', name: 'NamebazaarDevPublicResolver'},
  {kw: ':reverse-registrar', name: 'NamebazaarDevReverseResolver'},
])

const {
  ENSRegistry,
  NameBazaarRegistrar,
  OfferingRegistry,
  OfferingRequests,
  BuyNowOffering,
  BuyNowOfferingFactory,
  AuctionOffering,
  AuctionOfferingFactory,
  District0xEmails,
  NamebazaarDevNameResolver,
  NamebazaarDevPublicResolver,
  NamebazaarDevReverseResolver
} = loadedArtifacts

module.exports = async function (deployer, network, accounts) {
  await deployer.deploy(ENSRegistry);
  await deployer.deploy(NameBazaarRegistrar, ENSRegistry.address, namehash('eth'))

  const ens = await ENSRegistry.deployed()
  await ens.setSubnodeOwner(namehash(''), ensLabel('eth'), NameBazaarRegistrar.address)

  const emergencyMultisigAccount = accounts[0]
  await deployer.deploy(OfferingRegistry, emergencyMultisigAccount)
  await deployer.deploy(OfferingRequests)

  await deployer.deploy(BuyNowOffering, emergencyMultisigAccount)
  await deployer.deploy(
    BuyNowOfferingFactory,
    ENSRegistry.address,
    OfferingRegistry.address,
    OfferingRequests.address
  )

  await deployer.deploy(AuctionOffering, emergencyMultisigAccount)
  await deployer.deploy(
    AuctionOfferingFactory,
    ENSRegistry.address,
    OfferingRegistry.address,
    OfferingRequests.address
  )

  await deployer.deploy(District0xEmails)

  const offeringRegistry = await OfferingRegistry.deployed()
  await offeringRegistry.setFactories([BuyNowOfferingFactory.address, AuctionOfferingFactory.address], true)
  const offeringRequests = await OfferingRequests.deployed()
  await offeringRequests.setFactories([BuyNowOfferingFactory.address, AuctionOfferingFactory.address], true)

  await deployer.deploy(NamebazaarDevNameResolver)
  await deployer.deploy(NamebazaarDevPublicResolver, ENSRegistry.address)
  await deployer.deploy(NamebazaarDevReverseResolver, ENSRegistry.address, NamebazaarDevNameResolver.address)

  writeSmartContracts(loadedArtifacts)
};

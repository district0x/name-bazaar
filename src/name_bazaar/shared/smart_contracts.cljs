(ns name-bazaar.shared.smart-contracts)

(def smart-contracts
{:auction-offering-factory
 {:name "AuctionOfferingFactory",
  :address "0xf6562dccfd2a135bf7eb8fe1a3279001ba33e84f"},
 :buy-now-offering-factory
 {:name "BuyNowOfferingFactory",
  :address "0x3525181bf2ef29f0f86d6e049c2b5e331aa86754"},
 :name-bazaar-registrar
 {:name "NameBazaarRegistrar",
  :address "0x02a4859792ff18bc611b7e34ef2eb8276070d544"},
 :buy-now-offering
 {:name "BuyNowOffering",
  :address "0x8fc046ec87ee3fdc84836e98983561172b129416"},
 :reverse-registrar
 {:name "ReverseRegistrar",
  :address "0x9062C0A6Dbd6108336BcBe4593a3D1cE05512069"},
 :deed
 {:name "Deed", :address "0x0000000000000000000000000000000000000000"},
 :public-resolver
 {:name "PublicResolver",
  :address "0x5FfC014343cd971B7eb70732021E26C35B744cc4"},
 :ens
 {:name "ENSRegistry",
  :address "0x468edc874e1fead4611b276608392dfb06f3f1c1"},
 :offering-registry
 {:name "OfferingRegistry",
  :address "0x2f3b45084c8ca68b9ce175950f65ec2e67daa0cb"},
 :district0x-emails
 {:name "District0xEmails",
  :address "0xeeb8c09e789ce3e63c7367af239bd8a17da058c4"},
 :offering-requests
 {:name "OfferingRequests",
  :address "0x1a3bc2f94657a9dd364e8a01945e1c1c6db895aa"},
 :auction-offering
 {:name "AuctionOffering",
  :address "0x450778e29c1cd2df801598d1e26cd77c1725af55"}})